package org.xeb.xeb.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.item.OpticBlastItem;
import org.xeb.xeb.network.*;
import org.xeb.xeb.opticblast.ActiveBeamManager;
import org.xeb.xeb.opticblast.BeamData;

import java.util.*;

/**
 * Server-side tick handler for the Optic Blast weapon.
 * <p>
 * Handles:
 * <ol>
 *   <li>Primary beam (right-click hold) — raycast, damage, beam rendering sync</li>
 *   <li>Passive energy regeneration when not firing</li>
 *   <li>Cyclone Push (Activa 1) — beam with reverse thrust</li>
 *   <li>Gene Splice (Activa 2) — chain beam jumping between nearby entities</li>
 *   <li>Energy sync to clients every tick for HUD display</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OpticBlastTickHandler {

    /** Maximum beam reach distance in blocks. */
    private static final double MAX_BEAM_DISTANCE = 40.0D;

    /** Beam hit-box radius for entity sweeping. */
    private static final double BEAM_ENTITY_INFLATION = 0.5D;

    /** Cyclone Push: force magnitude applied opposite to look direction per tick. */
    private static final double CYCLONE_PUSH_FORCE = 0.1D;

    /** Gene Splice: max chain distance between entities. */
    private static final double GENE_SPLICE_CHAIN_RANGE = 3.0D;

    /** Gene Splice: max entities in the chain. */
    private static final int GENE_SPLICE_MAX_TARGETS = 10;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        long currentTick = server.overworld().getGameTime();

        // Clean up expired beams
        ActiveBeamManager.get().tickBeams(currentTick);

        // Collect external beams and tick struggle decay/timers
        net.minecraft.server.level.ServerLevel mainLevel = server.overworld();
        org.xeb.xeb.beamstruggle.ExternalBeamRegistry.collectBeams(mainLevel);
        org.xeb.xeb.beamstruggle.BeamStruggleManager.onServerTick(mainLevel, currentTick);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive()) continue;

            // Check if the player has the Optic Blast in either hand
            boolean holdsOpticBlast = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.OPTIC_BLAST.get())
                    || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.OPTIC_BLAST.get());

            if (!holdsOpticBlast) continue;

            boolean isFiringPrimary = player.isUsingItem()
                    && player.getUseItem().is(ModItems.OPTIC_BLAST.get());

            // ── Tick ability cooldowns (decrement client-sync counters) ──────────
            tickAbilityCooldowns(player, currentTick);

            // ── Cyclone Push tick ────────────────────────────────────────────────
            tickCyclonePush(player, currentTick);

            // ── Gene Splice tick ─────────────────────────────────────────────────
            tickGeneSplice(player, currentTick);

            // ── Mid-air slow fall and fall damage protection when using Optic Blast ────
            if (isFiringPrimary || player.getPersistentData().getBoolean("xebCyclonePushFiring") || player.getPersistentData().getBoolean("xebGeneSpliceFiring")) {
                BlockHitResult downHit = player.serverLevel().clip(new ClipContext(
                        player.position(),
                        player.position().add(0, -3.0D, 0),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                ));
                if (downHit.getType() == HitResult.Type.MISS) {
                    Vec3 motion = player.getDeltaMovement();
                    if (motion.y < -0.04D) {
                        player.setDeltaMovement(motion.x, -0.04D, motion.z);
                        player.hurtMarked = true;
                    }
                    player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);
                }
            }

            // ── Primary beam tick ───────────────────────────────────────────────
            if (isFiringPrimary) {
                tickPrimaryBeam(player, currentTick);
            }

            // ── Sync energy state to client every tick ──────────────────────────
            syncEnergyToClient(player, currentTick);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMARY BEAM
    // ═══════════════════════════════════════════════════════════════════════════

    private static void tickPrimaryBeam(ServerPlayer player, long currentTick) {
        float energy = OpticBlastItem.getEnergy(player);
        if (energy <= 0.0F) return; // Energy depleted, releaseUsing already handled

        ServerLevel level = player.serverLevel();

        // --- 1. Raycast from eye position ---
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle();
        Vec3 beamEnd = eyePos.add(lookDir.scale(MAX_BEAM_DISTANCE));

        // Block clip
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, beamEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 effectiveEnd;
        if (blockHit.getType() != HitResult.Type.MISS) {
            effectiveEnd = blockHit.getLocation();
        } else {
            effectiveEnd = beamEnd;
        }

        // --- 2. Entity sweep along the beam ---
        AABB sweepBox = new AABB(eyePos, effectiveEnd).inflate(BEAM_ENTITY_INFLATION);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eyePos, effectiveEnd, sweepBox,
                (entity) -> entity instanceof LivingEntity
                        && entity.isAlive()
                        && entity != player
                        && !entity.isSpectator()
                        && entity.isPickable(),
                MAX_BEAM_DISTANCE * MAX_BEAM_DISTANCE
        );

        // --- 3. Determine which hit is closer ---
        LivingEntity hitEntity = null;
        if (entityHit != null) {
            double entityDist = eyePos.distanceToSqr(entityHit.getLocation());
            double blockDist = eyePos.distanceToSqr(effectiveEnd);
            if (entityDist < blockDist) {
                effectiveEnd = entityHit.getLocation();
                if (entityHit.getEntity() instanceof LivingEntity living) {
                    hitEntity = living;
                }
            }
        }

        // --- 4. Check beam-vs-beam collision ---
        UUID ownerUUID = player.getUUID();
        Vec3 beamCollision = ActiveBeamManager.get().checkBeamVsBeamCollision(ownerUUID, eyePos, effectiveEnd);
        if (beamCollision != null) {
            double collisionDist = eyePos.distanceToSqr(beamCollision);
            double currentEndDist = eyePos.distanceToSqr(effectiveEnd);
            if (collisionDist < currentEndDist) {
                effectiveEnd = beamCollision;
                hitEntity = null;

                // Buscar el otro beam owner para iniciar/actualizar el Beam Struggle
                UUID otherOwnerUUID = null;
                Vec3 otherStart = null;

                for (BeamData otherBeam : ActiveBeamManager.get().getActiveBeams()) {
                    if (!otherBeam.getOwnerUUID().equals(ownerUUID)) {
                        otherOwnerUUID = otherBeam.getOwnerUUID();
                        otherStart = otherBeam.getStart();
                        break;
                    }
                }

                if (otherOwnerUUID == null) {
                    // Check external beams
                    for (org.xeb.xeb.beamstruggle.ExternalBeamRegistry.ExternalBeamData otherBeam : org.xeb.xeb.beamstruggle.ExternalBeamRegistry.getCurrentTickBeams()) {
                        if (!otherBeam.ownerUUID().equals(ownerUUID)) {
                            otherOwnerUUID = otherBeam.ownerUUID();
                            otherStart = otherBeam.start();
                            break;
                        }
                    }
                }

                if (otherOwnerUUID != null && otherStart != null) {
                    boolean struggleActive = org.xeb.xeb.beamstruggle.BeamStruggleManager.onBeamCollision(
                            ownerUUID, otherOwnerUUID,
                            eyePos, otherStart, beamCollision,
                            currentTick, level
                    );
                    if (struggleActive) {
                        Vec3 currentCollision = org.xeb.xeb.beamstruggle.BeamStruggleManager.getCollisionPointFor(ownerUUID);
                        if (currentCollision != null) {
                            effectiveEnd = currentCollision;
                        }
                    }
                }

                level.sendParticles(ParticleTypes.FLASH, effectiveEnd.x, effectiveEnd.y, effectiveEnd.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, effectiveEnd.x, effectiveEnd.y, effectiveEnd.z,
                        8, 0.3D, 0.3D, 0.3D, 0.05D);
            }
        }

        // --- 5. Deal damage to hit entity ---
        if (hitEntity != null) {
            hitEntity.getPersistentData().putString("xebLastAttackWeapon", "optic_blast");
            hitEntity.getPersistentData().putString("xebLastAttackType", "right_click");
            hitEntity.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
            hitEntity.hurt(player.damageSources().playerAttack(player), OpticBlastItem.BEAM_DAMAGE_PER_TICK);
        }

        // --- 6. Update beam manager ---
        BeamData beamData = new BeamData(
                ownerUUID, player.getId(),
                eyePos, effectiveEnd,
                0xFF0000, // Pure red
                currentTick, currentTick + 3
        );
        ActiveBeamManager.get().putBeam(ownerUUID, beamData);

        // --- 7. Send beam packet to clients ---
        XEBNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new OpticBlastBeamPacket(
                        player.getId(), true,
                        eyePos.x, eyePos.y, eyePos.z,
                        effectiveEnd.x, effectiveEnd.y, effectiveEnd.z,
                        OpticBlastBeamPacket.BEAM_PRIMARY
                )
        );

        // --- 8. Particles at beam endpoint ---
        int tickCount = (int) (currentTick % 100);
        if (tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, effectiveEnd.x, effectiveEnd.y, effectiveEnd.z,
                    2, 0.1D, 0.1D, 0.1D, 0.02D);
        }

        // Ambient beam hum sound every 20 ticks
        if (tickCount % 20 == 0) {
            level.playSound(null, player, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.4F, 2.0F);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CYCLONE PUSH (Activa 1)
    // ═══════════════════════════════════════════════════════════════════════════

    private static void tickCyclonePush(ServerPlayer player, long currentTick) {
        if (!player.getPersistentData().getBoolean("xebCyclonePushFiring")) return;

        int ticksRemaining = player.getPersistentData().getInt("xebCyclonePushTicks");
        ticksRemaining--;

        if (ticksRemaining <= 0) {
            // Duration expired — stop
            stopCyclonePush(player);
            return;
        }

        // Check if the key is still held (the key release sets this to false)
        if (!player.getPersistentData().getBoolean("xebCyclonePushKeyHeld")) {
            stopCyclonePush(player);
            return;
        }

        player.getPersistentData().putInt("xebCyclonePushTicks", ticksRemaining);

        ServerLevel level = player.serverLevel();

        // Raycast
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle();
        Vec3 beamEnd = eyePos.add(lookDir.scale(MAX_BEAM_DISTANCE));

        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));

        Vec3 effectiveEnd;
        if (blockHit.getType() != HitResult.Type.MISS) {
            effectiveEnd = blockHit.getLocation();
        } else {
            effectiveEnd = beamEnd;
        }

        // Entity sweep
        AABB sweepBox = new AABB(eyePos, effectiveEnd).inflate(BEAM_ENTITY_INFLATION);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eyePos, effectiveEnd, sweepBox,
                (entity) -> entity instanceof LivingEntity && entity.isAlive()
                        && entity != player && !entity.isSpectator() && entity.isPickable(),
                MAX_BEAM_DISTANCE * MAX_BEAM_DISTANCE
        );

        LivingEntity hitEntity = null;
        if (entityHit != null) {
            double entityDist = eyePos.distanceToSqr(entityHit.getLocation());
            double blockDist = eyePos.distanceToSqr(effectiveEnd);
            if (entityDist < blockDist) {
                effectiveEnd = entityHit.getLocation();
                if (entityHit.getEntity() instanceof LivingEntity living) {
                    hitEntity = living;
                }
            }
        }

        // Deal half damage
        if (hitEntity != null) {
            hitEntity.getPersistentData().putString("xebLastAttackWeapon", "optic_blast");
            hitEntity.getPersistentData().putString("xebLastAttackType", "active1");
            hitEntity.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
            hitEntity.hurt(player.damageSources().playerAttack(player),
                    OpticBlastItem.BEAM_DAMAGE_PER_TICK * 0.5F);
        }

        // --- Reverse thrust: push player OPPOSITE to look direction ---
        Vec3 pushForce = lookDir.scale(-CYCLONE_PUSH_FORCE);
        Vec3 currentMotion = player.getDeltaMovement();
        player.setDeltaMovement(currentMotion.add(pushForce.x, pushForce.y, pushForce.z));
        player.hurtMarked = true;

        // Prevent fall damage while being pushed upward
        player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);

        // Send beam packet (type = CYCLONE_PUSH)
        XEBNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new OpticBlastBeamPacket(
                        player.getId(), true,
                        eyePos.x, eyePos.y, eyePos.z,
                        effectiveEnd.x, effectiveEnd.y, effectiveEnd.z,
                        OpticBlastBeamPacket.BEAM_CYCLONE_PUSH
                )
        );

        // Particles at endpoint
        if (ticksRemaining % 2 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, effectiveEnd.x, effectiveEnd.y, effectiveEnd.z,
                    2, 0.1D, 0.1D, 0.1D, 0.02D);
        }
    }

    private static void stopCyclonePush(ServerPlayer player) {
        player.getPersistentData().putBoolean("xebCyclonePushFiring", false);
        player.getPersistentData().remove("xebCyclonePushTicks");
        player.getPersistentData().remove("xebCyclonePushKeyHeld");
        player.getPersistentData().putLong("xebCyclonePushLastTime", player.level().getGameTime());

        // Send stop packet
        XEBNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new OpticBlastBeamPacket(player.getId(), false, 0, 0, 0, 0, 0, 0,
                        OpticBlastBeamPacket.BEAM_CYCLONE_PUSH)
        );

        // Play stop sound
        player.serverLevel().playSound(null, player, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.8F);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GENE SPLICE (Activa 2)
    // ═══════════════════════════════════════════════════════════════════════════

    private static void tickGeneSplice(ServerPlayer player, long currentTick) {
        if (!player.getPersistentData().getBoolean("xebGeneSpliceFiring")) return;

        int ticksRemaining = player.getPersistentData().getInt("xebGeneSpliceTicks");
        ticksRemaining--;

        if (ticksRemaining <= 0) {
            stopGeneSplice(player);
            return;
        }

        if (!player.getPersistentData().getBoolean("xebGeneSpliceKeyHeld")) {
            stopGeneSplice(player);
            return;
        }

        player.getPersistentData().putInt("xebGeneSpliceTicks", ticksRemaining);

        ServerLevel level = player.serverLevel();

        // --- 1. Find the first entity with raycast ---
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle();
        Vec3 beamEnd = eyePos.add(lookDir.scale(MAX_BEAM_DISTANCE));

        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));

        Vec3 effectiveEnd;
        if (blockHit.getType() != HitResult.Type.MISS) {
            effectiveEnd = blockHit.getLocation();
        } else {
            effectiveEnd = beamEnd;
        }

        AABB sweepBox = new AABB(eyePos, effectiveEnd).inflate(BEAM_ENTITY_INFLATION);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eyePos, effectiveEnd, sweepBox,
                (entity) -> entity instanceof LivingEntity && entity.isAlive()
                        && entity != player && !entity.isSpectator() && entity.isPickable(),
                MAX_BEAM_DISTANCE * MAX_BEAM_DISTANCE
        );

        // Build the chain of points
        List<double[]> chainPoints = new ArrayList<>();
        chainPoints.add(new double[]{ eyePos.x, eyePos.y, eyePos.z });

        float chainDamage = OpticBlastItem.BEAM_DAMAGE_PER_TICK / 3.0F;

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity firstTarget) {
            double entityDist = eyePos.distanceToSqr(entityHit.getLocation());
            double blockDist = eyePos.distanceToSqr(effectiveEnd);
            if (entityDist < blockDist) {
                // Hit first entity — start chain
                Set<Integer> hitEntityIds = new HashSet<>();
                hitEntityIds.add(player.getId());

                LivingEntity currentTarget = firstTarget;
                Vec3 currentPos = entityHit.getLocation();

                for (int chainIndex = 0; chainIndex < GENE_SPLICE_MAX_TARGETS && currentTarget != null; chainIndex++) {
                    hitEntityIds.add(currentTarget.getId());

                    // Add chain point at entity center (eye height)
                    Vec3 targetCenter = currentTarget.position().add(0, currentTarget.getBbHeight() * 0.5, 0);
                    chainPoints.add(new double[]{ targetCenter.x, targetCenter.y, targetCenter.z });

                    // Deal damage
                    currentTarget.getPersistentData().putString("xebLastAttackWeapon", "optic_blast");
                    currentTarget.getPersistentData().putString("xebLastAttackType", "active2");
                    currentTarget.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
                    currentTarget.hurt(player.damageSources().playerAttack(player), chainDamage);
                    // Particles on hit: Burst of red sparks (ELECTRIC_SPARK), critical hit indicator (CRIT)
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, targetCenter.x, targetCenter.y, targetCenter.z,
                            16, 0.25D, 0.25D, 0.25D, 0.1D);
                    level.sendParticles(ParticleTypes.CRIT, targetCenter.x, targetCenter.y, targetCenter.z,
                            8, 0.2D, 0.2D, 0.2D, 0.15D);

                    // --- Find next closest entity within GENE_SPLICE_CHAIN_RANGE ---
                    LivingEntity nextTarget = null;
                    double closestDist = GENE_SPLICE_CHAIN_RANGE * GENE_SPLICE_CHAIN_RANGE;

                    AABB searchBox = currentTarget.getBoundingBox().inflate(GENE_SPLICE_CHAIN_RANGE);
                    List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                            e -> e.isAlive() && !e.isSpectator() && !hitEntityIds.contains(e.getId()) && e != player);

                    for (LivingEntity candidate : nearby) {
                        double dist = currentTarget.distanceToSqr(candidate);
                        if (dist < closestDist) {
                            closestDist = dist;
                            nextTarget = candidate;
                        }
                    }

                    currentTarget = nextTarget;
                }
            } else {
                // No entity hit, beam goes to block/max distance
                chainPoints.add(new double[]{ effectiveEnd.x, effectiveEnd.y, effectiveEnd.z });
            }
        } else {
            // No entity hit at all
            chainPoints.add(new double[]{ effectiveEnd.x, effectiveEnd.y, effectiveEnd.z });
        }

        // --- Send chain beam packet to clients ---
        XEBNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new OpticBlastChainBeamPacket(player.getId(), true, chainPoints)
        );
    }

    private static void stopGeneSplice(ServerPlayer player) {
        player.getPersistentData().putBoolean("xebGeneSpliceFiring", false);
        player.getPersistentData().remove("xebGeneSpliceTicks");
        player.getPersistentData().remove("xebGeneSpliceKeyHeld");
        player.getPersistentData().putLong("xebGeneSpliceLastTime", player.level().getGameTime());

        // Send stop packet
        XEBNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new OpticBlastChainBeamPacket(player.getId(), false, Collections.emptyList())
        );

        player.serverLevel().playSound(null, player, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.8F);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ABILITY COOLDOWN TICK
    // ═══════════════════════════════════════════════════════════════════════════

    private static void tickAbilityCooldowns(ServerPlayer player, long currentTick) {
        // These are tracked as remaining ticks for HUD display
        // They were set by ActuarKeyPacket when the ability was triggered
        // We don't tick them here — we compute remaining from last-use timestamps
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ENERGY SYNC
    // ═══════════════════════════════════════════════════════════════════════════

    private static void syncEnergyToClient(ServerPlayer player, long currentTick) {
        float energy = OpticBlastItem.getEnergy(player);
        boolean overheated = player.getCooldowns().isOnCooldown(ModItems.OPTIC_BLAST.get());

        // Compute ability cooldowns from timestamps
        long cyclonePushLast = player.getPersistentData().getLong("xebCyclonePushLastTime");
        long geneSpliceLast = player.getPersistentData().getLong("xebGeneSpliceLastTime");

        int cyclonePushCD = Math.max(0, (int) (OpticBlastItem.CYCLONE_PUSH_COOLDOWN - (currentTick - cyclonePushLast)));
        int geneSpliceCD = Math.max(0, (int) (OpticBlastItem.GENE_SPLICE_COOLDOWN - (currentTick - geneSpliceLast)));

        // Retrieve active firing ticks to sync duration
        int cyclonePushTicks = player.getPersistentData().getInt("xebCyclonePushTicks");
        int geneSpliceTicks = player.getPersistentData().getInt("xebGeneSpliceTicks");

        XEBNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpticBlastEnergySyncPacket(player.getId(), energy, overheated, cyclonePushCD, geneSpliceCD, cyclonePushTicks, geneSpliceTicks)
        );
    }
}
