package org.xeb.xeb.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.client.vfx.*;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.entity.FlowerPelletEntity;
import org.xeb.xeb.entity.FlowerProjectileEntity;
import org.xeb.xeb.entity.SpikeProjectileEntity;
import org.xeb.xeb.item.*;
import org.xeb.xeb.network.*;

import java.util.Collections;
import java.util.List;

/**
 * Universal Mob Weapon AI Execution Handler.
 *
 * <p>Enables ANY Minecraft Mob (Vanilla or Modded) holding an xEB weapon in its mainhand
 * to dynamically evaluate and execute its held weapon's EXACT Left Click, Right Click,
 * Activa 1, and Activa 2 abilities with proper cooldowns, visual passives, and sound effects.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobWeaponAIHandler {

    @SubscribeEvent
    public static void onMobTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Mob mob) || !mob.isAlive()) return;

        // Tick active mob Extreme Burst sequences
        if (mob.getPersistentData().getBoolean("xebJudgementCutActive")) {
            org.xeb.xeb.extremeburst.JudgementCutHandler.onServerTick(mob);
        }
        if (mob.getPersistentData().getBoolean("xebSovereignActive")) {
            org.xeb.xeb.extremeburst.SovereignArsenalHandler.onServerTick(mob);
        }
        if (mob.getPersistentData().getInt("xebDogmaBrimstoneTicks") > 0) {
            org.xeb.xeb.extremeburst.DogmaBurstHandler.tick(mob);
        }
        if (mob.getPersistentData().getInt("xebOmegaFloweryTicks") > 0) {
            org.xeb.xeb.extremeburst.OmegaFloweryHandler.tick(mob);
        }

        ItemStack mainhand = mob.getMainHandItem();
        ItemStack offhand = mob.getOffhandItem();

        Item mainItem = mainhand.getItem();
        Item offItem = offhand.getItem();
        Item item = mainItem;

        boolean hasXebWeapon = isXebWeapon(mainItem) || isXebWeapon(offItem);
        boolean hasXebCurio = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(offItem) != null
                || org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(mainItem) != null
                || org.xeb.xeb.extremeburst.ExtremeBurstRegistry.findActiveBurst(mob) != null;

        if (!hasXebWeapon && !hasXebCurio) return;

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || target.isSpectator()) return;

        double distSq = mob.distanceToSqr(target);
        if (distSq > 1024.0D) return; // Beyond 32 blocks

        boolean hasLOS = mob.getSensing().hasLineOfSight(target);
        if (!hasLOS) return;

        Level level = mob.level();
        long gameTime = level.getGameTime();

        // ── 0. EXTREME BURST MOB AI (EQUIPPED IN OFFHAND / MAINHAND / CURIOS) ────
        org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry =
                org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(offhand.getItem());
        if (burstEntry == null) {
            burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(mainhand.getItem());
        }
        if (burstEntry == null) {
            burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.findActiveBurst(mob);
        }

        if (burstEntry != null) {
            long lastBurstTime = mob.getPersistentData().getLong("xebMobExtremeBurstCD");
            if (gameTime - lastBurstTime >= burstEntry.cooldownTicks) {
                if (org.xeb.xeb.extremeburst.ExtremeBurstRegistry.canActivate(mob, burstEntry)) {
                    mob.getPersistentData().putLong("xebMobExtremeBurstCD", gameTime);
                    org.xeb.xeb.extremeburst.ExtremeBurstHandler.handleActivation(mob, burstEntry);
                    return;
                }
            }
        }

        // ── 0.5. DECLARATIVE MOB WEAPON CAPABILITY DISPATCH ───────────────
        if (item instanceof org.xeb.xeb.item.capability.IMobWeaponCapability capability) {
            if (capability.canMobUseWeapon(mob)) {
                capability.tickMobAI(mob, target, level, gameTime, distSq);
                return;
            }
        }

        // ── 1. GOLDEN FLOWER MOB AI ──────────────────────────────────────────
        if (item instanceof GoldenFlowerItem) {
            long cdPellets = mob.getPersistentData().getLong("xebMobCD_Pellets");
            long cdHoming = mob.getPersistentData().getLong("xebMobCD_Homing");
            long cdDance = mob.getPersistentData().getLong("xebMobCD_Dance");
            long cdJarona = mob.getPersistentData().getLong("xebMobCD_Jarona");

            // Activa 1: Flower Dance (12s cd) at 5-25 blocks distance
            if (distSq >= 25.0D && distSq <= 625.0D && gameTime - cdDance >= 240) {
                mob.getPersistentData().putLong("xebMobCD_Dance", gameTime);
                mob.getPersistentData().putBoolean("xebFlowerDanceActive", true);
                mob.getPersistentData().putInt("xebFlowerDanceTicksRemaining", 85);

                int[] targetIds = new int[5];
                for (int i = 0; i < 5; i++) targetIds[i] = target.getId();

                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                        new GoldenFlowerDanceStartPacket(mob.getId(), targetIds, 5));
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F, 1.0F);
                return;
            }

            // Activa 2: Jarona Flying Dash (8s cd) at 4-15 blocks distance
            if (distSq >= 16.0D && distSq <= 225.0D && gameTime - cdJarona >= 160) {
                mob.getPersistentData().putLong("xebMobCD_Jarona", gameTime);
                Vec3 dir = target.position().subtract(mob.position()).normalize();
                mob.setDeltaMovement(dir.x * 1.8D, 0.35D, dir.z * 1.8D);
                mob.hurtMarked = true;
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 1, false, false));
                mob.getPersistentData().putInt("xebJaronaDashTicks", 15);

                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                        new JaronaDashPacket(mob.getId(), 2));
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.0F, 1.2F);
                return;
            }

            // Click Derecho: Homing Spore Flowers (6s cd)
            if (distSq <= 400.0D && gameTime - cdHoming >= 120) {
                mob.getPersistentData().putLong("xebMobCD_Homing", gameTime);
                for (int i = 0; i < 5; i++) {
                    FlowerProjectileEntity flower = new FlowerProjectileEntity(level, mob, i);
                    double spreadX = (mob.getRandom().nextDouble() - 0.5D) * 1.5D;
                    double spreadZ = (mob.getRandom().nextDouble() - 0.5D) * 1.5D;
                    flower.setPos(mob.getX() + spreadX, mob.getEyeY() + 0.5D, mob.getZ() + spreadZ);
                    flower.setTarget(target);
                    level.addFreshEntity(flower);
                }
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.2F);
                return;
            }

            // Click Izquierdo: 3 Friendliness Pellets (2s cd)
            if (distSq <= 256.0D && gameTime - cdPellets >= 40) {
                mob.getPersistentData().putLong("xebMobCD_Pellets", gameTime);
                Vec3 eyePos = mob.getEyePosition();
                Vec3 targetPos = target.getEyePosition();
                Vec3 dir = targetPos.subtract(eyePos).normalize();

                for (int i = -1; i <= 1; i++) {
                    FlowerPelletEntity pellet = new FlowerPelletEntity(level, mob);
                    pellet.setPos(eyePos.x, eyePos.y - 0.2D, eyePos.z);
                    Vec3 spreadDir = dir.add((mob.getRandom().nextDouble() - 0.5D) * 0.1D, 0, (mob.getRandom().nextDouble() - 0.5D) * 0.1D);
                    pellet.shoot(spreadDir.x, spreadDir.y, spreadDir.z, 1.6F, 2.0F);
                    level.addFreshEntity(pellet);
                }
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.HOSTILE, 0.8F, 1.5F);
            }
        }



        // ── 3. OPTIC BLAST WEAPON MOB AI (ENERGY RESOURCE & MULTI-ATTACK AI) ──
        else if (item instanceof OpticBlastItem) {
            long cdOpticShot  = mob.getPersistentData().getLong("xebMobCD_OpticShot");
            long cdOpticBeam  = mob.getPersistentData().getLong("xebMobCD_OpticBeam");
            long cdCyclone    = mob.getPersistentData().getLong("xebMobCD_Cyclone");

            // Energy resource management
            float energy = mob.getPersistentData().contains("xebOpticEnergy")
                    ? mob.getPersistentData().getFloat("xebOpticEnergy") : OpticBlastItem.MAX_ENERGY;
            int overheatCD = mob.getPersistentData().getInt("xebOpticOverheatTicks");

            if (overheatCD > 0) {
                mob.getPersistentData().putInt("xebOpticOverheatTicks", overheatCD - 1);
            }

            // Passive energy regen when not active channeling
            int beamTicks = mob.getPersistentData().getInt("xebMobBeamTicks");
            int cycloneTicks = mob.getPersistentData().getInt("xebMobCycloneTicks");

            if (beamTicks <= 0 && cycloneTicks <= 0 && overheatCD <= 0) {
                if (energy < OpticBlastItem.MAX_ENERGY) {
                    energy = Math.min(OpticBlastItem.MAX_ENERGY, energy + OpticBlastItem.ENERGY_REGEN_PER_TICK);
                    mob.getPersistentData().putFloat("xebOpticEnergy", energy);
                }
            }

            // ── A. Tick active Primary Beam channeling ───────────────────────
            if (beamTicks > 0) {
                if (org.xeb.xeb.beamstruggle.BeamStruggleManager.isInActiveStruggle(mob.getUUID())) {
                    beamTicks = Math.max(beamTicks, 20);
                    energy = Math.max(10.0F, energy);
                } else {
                    beamTicks--;
                    energy -= OpticBlastItem.ENERGY_DRAIN_PER_TICK;
                }
                mob.getPersistentData().putInt("xebMobBeamTicks", beamTicks);
                mob.getPersistentData().putFloat("xebOpticEnergy", Math.max(0.0F, energy));

                if (energy <= 0.0F) {
                    // Overheat! Forced 6s cooldown
                    mob.getPersistentData().putInt("xebOpticOverheatTicks", OpticBlastItem.OVERHEAT_COOLDOWN);
                    mob.getPersistentData().putInt("xebMobBeamTicks", 0);
                    org.xeb.xeb.opticblast.ActiveBeamManager.get().removeBeam(mob.getUUID());
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                            new OpticBlastBeamPacket(mob.getId(), false, 0, 0, 0, 0, 0, 0, OpticBlastBeamPacket.BEAM_PRIMARY));
                    level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                            SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 0.8F, 1.5F);
                    return;
                }

                Vec3 eyePos  = mob.getEyePosition(1.0F);
                Vec3 lookDir = mob.getLookAngle();
                Vec3 beamEnd = eyePos.add(lookDir.scale(40.0D));

                Vec3 struggleCol = org.xeb.xeb.beamstruggle.BeamStruggleManager.getCollisionPointFor(mob.getUUID());
                Vec3 effectiveEnd;
                if (struggleCol != null) {
                    effectiveEnd = struggleCol;
                } else {
                    BlockHitResult blockHit = level.clip(new ClipContext(eyePos, beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
                    effectiveEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : beamEnd;
                }

                AABB sweepBox = new AABB(eyePos, effectiveEnd).inflate(0.5D);
                EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                        mob, eyePos, effectiveEnd, sweepBox,
                        e -> e instanceof LivingEntity && e.isAlive() && e != mob && !e.isSpectator() && e.isPickable(),
                        1600.0D
                );

                LivingEntity hitEntity = null;
                if (entityHit != null) {
                    double entityDist = eyePos.distanceToSqr(entityHit.getLocation());
                    double blockDist  = eyePos.distanceToSqr(effectiveEnd);
                    if (entityDist < blockDist) {
                        effectiveEnd = entityHit.getLocation();
                        if (entityHit.getEntity() instanceof LivingEntity living) hitEntity = living;
                    }
                }

                // Register for Beam Struggle collisions
                org.xeb.xeb.opticblast.ActiveBeamManager.get().putBeam(mob.getUUID(),
                        new org.xeb.xeb.opticblast.BeamData(mob.getUUID(), mob.getId(), eyePos, effectiveEnd, 0xFF0000, gameTime, gameTime + 2L, "optic_blast"));

                // Broadcast clean beam packet to clients
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                        new OpticBlastBeamPacket(mob.getId(), true, eyePos.x, eyePos.y, eyePos.z, effectiveEnd.x, effectiveEnd.y, effectiveEnd.z, OpticBlastBeamPacket.BEAM_PRIMARY));

                if (hitEntity != null && beamTicks % 2 == 0) {
                    hitEntity.hurt(level.damageSources().mobAttack(mob), OpticBlastItem.BEAM_DAMAGE_PER_TICK);
                    hitEntity.setSecondsOnFire(2);
                }

                if (beamTicks - 1 <= 0) {
                    org.xeb.xeb.opticblast.ActiveBeamManager.get().removeBeam(mob.getUUID());
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                            new OpticBlastBeamPacket(mob.getId(), false, 0, 0, 0, 0, 0, 0, OpticBlastBeamPacket.BEAM_PRIMARY));
                }
                return;
            }

            // ── B. Tick active Cyclone Push channeling ────────────────────────
            if (cycloneTicks > 0) {
                mob.getPersistentData().putInt("xebMobCycloneTicks", cycloneTicks - 1);
                Vec3 eyePos  = mob.getEyePosition(1.0F);
                Vec3 lookDir = mob.getLookAngle();
                Vec3 beamEnd = eyePos.add(lookDir.scale(40.0D));

                BlockHitResult blockHit = level.clip(new ClipContext(eyePos, beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
                Vec3 effectiveEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : beamEnd;

                // Push mob backwards (reverse thrust)
                Vec3 pushForce = lookDir.scale(-0.12D);
                mob.setDeltaMovement(mob.getDeltaMovement().add(pushForce.x, pushForce.y, pushForce.z));
                mob.hurtMarked = true;

                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                        new OpticBlastBeamPacket(mob.getId(), true, eyePos.x, eyePos.y, eyePos.z, effectiveEnd.x, effectiveEnd.y, effectiveEnd.z, OpticBlastBeamPacket.BEAM_CYCLONE_PUSH));

                if (cycloneTicks % 2 == 0 && target != null) {
                    target.hurt(level.damageSources().mobAttack(mob), OpticBlastItem.BEAM_DAMAGE_PER_TICK * 0.5F);
                }

                if (cycloneTicks - 1 <= 0) {
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                            new OpticBlastBeamPacket(mob.getId(), false, 0, 0, 0, 0, 0, 0, OpticBlastBeamPacket.BEAM_CYCLONE_PUSH));
                }
                return;
            }

            // ── C. AI Action Decision (When not active channeling) ───────────
            // 1. Activa 1: Cyclone Push if target is too close (< 5 blocks)
            if (distSq <= 25.0D && gameTime - cdCyclone >= 200 && overheatCD <= 0) {
                mob.getPersistentData().putLong("xebMobCD_Cyclone", gameTime);
                mob.getPersistentData().putInt("xebMobCycloneTicks", 40); // 2.0s
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 1.8F);
                return;
            }

            // 2. Right-Click: Continuous Plasma Beam if target at medium range (6-25 blocks) & energy > 25.0F
            if (distSq >= 36.0D && distSq <= 625.0D && gameTime - cdOpticBeam >= 120 && energy >= 25.0F && overheatCD <= 0) {
                mob.getPersistentData().putLong("xebMobCD_OpticBeam", gameTime);
                mob.getPersistentData().putInt("xebMobBeamTicks", 50); // 2.5s channel
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.2F, 1.8F);
                return;
            }

            // 3. Left-Click: Mini-Laser Pulse Shot (0.75s cd)
            if (distSq <= 400.0D && gameTime - cdOpticShot >= 15) {
                mob.getPersistentData().putLong("xebMobCD_OpticShot", gameTime);
                org.xeb.xeb.entity.MiniLaserProjectileEntity miniLaser = new org.xeb.xeb.entity.MiniLaserProjectileEntity(level, mob);
                Vec3 eyePos = mob.getEyePosition(1.0F);
                Vec3 look = target.getEyePosition().subtract(eyePos).normalize();
                miniLaser.moveTo(eyePos.x, eyePos.y - 0.1D, eyePos.z, 0.0F, 0.0F);
                miniLaser.setDeltaMovement(look.scale(2.5D));
                level.addFreshEntity(miniLaser);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.HOSTILE, 0.8F, 1.8F);
            }
        }

        // ── 4. SMART HALBERD MOB AI ─────────────────────────────────────────
        else if (item instanceof SmartHalberdItem) {
            long cdIceSpikes = mob.getPersistentData().getLong("xebMobCD_IceSpikes");

            // Click Derecho: Zigzag Ice Spikes (4s cd)
            if (distSq <= 225.0D && gameTime - cdIceSpikes >= 80) {
                mob.getPersistentData().putLong("xebMobCD_IceSpikes", gameTime);
                Vec3 dir = target.position().subtract(mob.position()).normalize();

                for (int i = 1; i <= 5; i++) {
                    SpikeProjectileEntity spike = new SpikeProjectileEntity(level, mob);
                    Vec3 spikePos = mob.position().add(dir.scale(i * 1.5D));
                    spike.moveTo(spikePos.x, spikePos.y + 0.1D, spikePos.z, mob.getYRot(), mob.getXRot());
                    spike.setDeltaMovement(dir.scale(1.2D));
                    spike.setSpikeIndex(i);
                    level.addFreshEntity(spike);
                }
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2F, 1.4F);
            }
        }

        // ── 5. HOLY DUALITY BLADE MOB AI ────────────────────────────────────
        else if (item instanceof HolyDualityBladeItem) {
            long cdHolySlash = mob.getPersistentData().getLong("xebMobCD_HolySlash");

            // Primary: Holy Energy Arc Slash (2.5s cd)
            if (distSq <= 64.0D && gameTime - cdHolySlash >= 50) {
                mob.getPersistentData().putLong("xebMobCD_HolySlash", gameTime);
                Vec3 dir = target.position().subtract(mob.position()).normalize();
                XebSlashes.spawnSlash(mob.position().add(0, 1.0D, 0), dir, 3.5D, 0.8D, 255, 215, 0, 15);
                target.hurt(level.damageSources().mobAttack(mob), 12.0F);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.8F);
            }
        }



    }

    private static boolean isXebWeapon(Item item) {
        return item instanceof GoldenFlowerItem ||
               item instanceof DoomfistItem ||
               item instanceof DoomfistV2Item ||
               item instanceof OpticBlastItem ||
               item instanceof SmartHalberdItem ||
               item instanceof HolyDualityBladeItem ||
               item instanceof MechaOverdriveItem ||
               item instanceof BrokenDiamondItem ||
               item instanceof TheTearsItem;
    }
}
