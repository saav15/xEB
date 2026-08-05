package org.xeb.xeb.extremeburst;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.network.MeteorStrikeSyncPacket;
import org.xeb.xeb.network.XEBNetwork;

import java.util.List;

/**
 * Server-side handler for the authentic Overwatch Meteor Strike (Doomfist v1 & v2).
 *
 * <p>Sequence:
 * 1. Launch Phase (State 1): Catapults player high into the atmosphere. Sets player invisible & invulnerable.
 * 2. Overhead Targeting Phase (State 2): Player hovers in atmosphere for up to 10 seconds (200 ticks).
 *    Raycast calculates ground telegraph target position. Live target count is synced to all nearby players.
 * 3. Plunge Phase (State 3): Rapid warning flash telegraph. Player crashes downward at high speed.
 * 4. Impact & Wave Phase (State 4): 4x4 Epicenter hit dealing 4x Doomfist max charged damage,
 *    flying crater block debris, Overhealth (+30 Absorption per target), and vertical Knockup.</p>
 */
public class MeteorStrikeHandler {

    public static final int TARGETING_MAX_TICKS = 200; // 10 seconds max

    public static void activate(ServerPlayer player, ExtremeBurstRegistry.ExtremeBurstEntry entry) {
        CompoundTag nbt = player.getPersistentData();

        boolean holdsV2 = player.getMainHandItem().is(ModItems.DOOMFIST_V2.get())
                       || player.getOffhandItem().is(ModItems.DOOMFIST_V2.get());

        boolean isUltra = nbt.getBoolean("xebDashIsUltraCharged");

        nbt.putInt("xebMeteorStrikeState", 1); // 1 = Launching
        nbt.putInt("xebMeteorStrikeLaunchTimer", 25); // 25 ticks of vertical rise
        nbt.putInt("xebMeteorStrikeTargetingTicks", TARGETING_MAX_TICKS);
        nbt.putBoolean("xebMeteorStrikeIsV2", holdsV2);
        nbt.putBoolean("xebMeteorStrikeIsUltra", isUltra);
        nbt.putBoolean("xebDoomfistFallProtect", true);
        nbt.putBoolean("xebExtremeBurstActive", true);

        // Store launch origin coordinates to clamp distance to loaded chunk radius (48m max)
        nbt.putDouble("xebMeteorStrikeStartX", player.getX());
        nbt.putDouble("xebMeteorStrikeStartY", player.getY());
        nbt.putDouble("xebMeteorStrikeStartZ", player.getZ());

        // Make player invisible and invulnerable while in atmosphere

        player.setInvisible(true);
        player.setInvulnerable(true);

        // Sound & Launch Explosion
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 1.2F);

        // Launch particle ring
        // Launch ground block debris
        BlockPos gPos = player.blockPosition().below();
        BlockState gState = level.getBlockState(gPos);
        if (!gState.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, gState),
                    player.getX(), player.getY() + 0.2, player.getZ(), 36, 1.5, 0.2, 1.5, 0.15);
        }

        // Impulso vertical masivo hacia el cielo
        player.setDeltaMovement(0, 2.8, 0);
        player.hurtMarked = true;

        syncToClients(player, 1, holdsV2, player.getX(), player.getY(), player.getZ(), 0);
    }

    public static void tickServer(ServerPlayer player) {
        CompoundTag nbt = player.getPersistentData();
        int state = nbt.getInt("xebMeteorStrikeState");
        if (state == 0) return;

        ServerLevel level = player.serverLevel();
        boolean holdsV2 = nbt.getBoolean("xebMeteorStrikeIsV2");

        // Continuously protect from fall damage during Meteor Strike
        nbt.putBoolean("xebDoomfistFallProtect", true);

        if (state == 1) {
            // ── FASE 1: Despegue / Ascenso ────────────────────────────────────
            int launchTimer = nbt.getInt("xebMeteorStrikeLaunchTimer");
            if (launchTimer > 0) {
                nbt.putInt("xebMeteorStrikeLaunchTimer", launchTimer - 1);
                player.setDeltaMovement(player.getDeltaMovement().x, 2.0, player.getDeltaMovement().z);
                player.hurtMarked = true;
            } else {
                // Transition to State 2 (Overhead Targeting)
                nbt.putInt("xebMeteorStrikeState", 2);
            }
        } else if (state == 2) {
            // ── FASE 2: Selección y Apuntado Aéreo (10s Max) ──────────────────
            int targetingTicks = nbt.getInt("xebMeteorStrikeTargetingTicks");
            targetingTicks--;
            nbt.putInt("xebMeteorStrikeTargetingTicks", targetingTicks);

            player.setInvisible(true);
            player.setInvulnerable(true);

            double startX = nbt.getDouble("xebMeteorStrikeStartX");
            double startY = nbt.getDouble("xebMeteorStrikeStartY");
            double startZ = nbt.getDouble("xebMeteorStrikeStartZ");
            double safeAtmosphereY = Math.min((double) level.getMaxBuildHeight() - 2.0D, startY + 35.0D);

            player.teleportTo(startX, safeAtmosphereY, startZ);
            player.setDeltaMovement(0, 0, 0);
            player.fallDistance = 0.0F;
            player.hurtMarked = true;

            // Leer posición del suelo objetivo (X, Y, Z) enviada por el cliente
            double targetX = nbt.contains("xebMeteorStrikeTargetX") ? nbt.getDouble("xebMeteorStrikeTargetX") : startX;
            double targetY = nbt.contains("xebMeteorStrikeTargetY") ? nbt.getDouble("xebMeteorStrikeTargetY") : startY;
            double targetZ = nbt.contains("xebMeteorStrikeTargetZ") ? nbt.getDouble("xebMeteorStrikeTargetZ") : startZ;

            // Limitar la distancia máxima a 48m desde el despegue
            double maxDist = 48.0D;
            double dx = targetX - startX;
            double dz = targetZ - startZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > maxDist) {
                targetX = startX + (dx / dist) * maxDist;
                targetZ = startZ + (dz / dist) * maxDist;
            }

            Vec3 targetPos = new Vec3(targetX, targetY, targetZ);

            nbt.putDouble("xebMeteorStrikeTargetX", targetPos.x);
            nbt.putDouble("xebMeteorStrikeTargetY", targetPos.y);
            nbt.putDouble("xebMeteorStrikeTargetZ", targetPos.z);

            // Contar objetivos en el anillo de 12m en tiempo real
            AABB outerBox = new AABB(targetPos.x - 6.0D, targetPos.y - 3.0D, targetPos.z - 6.0D,
                                     targetPos.x + 6.0D, targetPos.y + 5.0D, targetPos.z + 6.0D);
            List<LivingEntity> targetsInRange = level.getEntitiesOfClass(LivingEntity.class, outerBox,
                    e -> e != player && e.isAlive() && !e.isAlliedTo(player));
            int targetCount = targetsInRange.size();
            nbt.putInt("xebMeteorStrikeTargetCount", targetCount);

            // Sincronizar posición del retículo a clientes
            syncToClients(player, 2, holdsV2, targetPos.x, targetPos.y, targetPos.z, targetCount);

            if (targetingTicks <= 0) {
                triggerPlunge(player);
            }
        } else if (state == 3) {
            // ── FASE 3: Ventana de Caída Vertical de 0.6s (-1.35b/t para reacción enemiga) ──
            double tx = nbt.getDouble("xebMeteorStrikeTargetX");
            double ty = nbt.getDouble("xebMeteorStrikeTargetY");
            double tz = nbt.getDouble("xebMeteorStrikeTargetZ");

            if (player.getY() <= ty + 1.2D || player.onGround()) {
                executeImpact(player, tx, ty, tz);
            } else {
                player.teleportTo(tx, player.getY() - 1.35D, tz);
                player.setDeltaMovement(0.0D, -1.35D, 0.0D);
                player.hurtMarked = true;
            }
        }
    }

    public static void triggerPlunge(ServerPlayer player) {
        CompoundTag nbt = player.getPersistentData();
        if (nbt.getInt("xebMeteorStrikeState") == 2) {
            nbt.putInt("xebMeteorStrikeState", 3);
            boolean holdsV2 = nbt.getBoolean("xebMeteorStrikeIsV2");
            double tx = nbt.getDouble("xebMeteorStrikeTargetX");
            double ty = nbt.getDouble("xebMeteorStrikeTargetY");
            double tz = nbt.getDouble("xebMeteorStrikeTargetZ");
            int targetCount = nbt.getInt("xebMeteorStrikeTargetCount");

            // Posiciona al jugador a 16m sobre el retículo para una caída vertical visible de 0.6s
            player.teleportTo(tx, ty + 16.0D, tz);
            player.setDeltaMovement(0.0D, -1.35D, 0.0D);

            syncToClients(player, 3, holdsV2, tx, ty, tz, targetCount);

            player.serverLevel().playSound(null, tx, ty, tz,
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.5F, 0.7F);
        }
    }

    private static void executeImpact(ServerPlayer player, double tx, double ty, double tz) {
        CompoundTag nbt = player.getPersistentData();
        ServerLevel level = player.serverLevel();

        boolean holdsV2 = nbt.getBoolean("xebMeteorStrikeIsV2");
        boolean isUltra = nbt.getBoolean("xebMeteorStrikeIsUltra");

        // Teleport & restore visibility & vulnerability
        player.teleportTo(tx, ty, tz);
        player.setDeltaMovement(0, 0, 0);
        player.setInvisible(false);
        player.setInvulnerable(false);

        // ── CALCULAR DAÑO BASE: 140 PARA DOOMFIST V1 Y 100 PARA DOOMFIST V2 ─────────
        float epicenterDamage = holdsV2 ? 100.0F : 140.0F;

        // Si viene empoderado por Uppercut o Charged Fist II, aplica +50% extra burst (210.0F para v1, 150.0F para v2!)
        if (nbt.getBoolean("xebUppercutEmpoweredPunch") || player.hasEffect(ModEffects.CHARGED_FIST.get())) {
            epicenterDamage *= 1.5F;
        }

        // ── FASE 4: FRACTURA Y EPICENTRO LETAL (4x4) ──────────────────────────
        AABB epicenterBox = new AABB(tx - 2.0D, ty - 1.0D, tz - 2.0D, tx + 2.0D, ty + 3.0D, tz + 2.0D);
        List<LivingEntity> epicenterTargets = level.getEntitiesOfClass(LivingEntity.class, epicenterBox,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player));

        for (LivingEntity target : epicenterTargets) {
            target.hurt(player.damageSources().playerAttack(player), epicenterDamage);
            // High radial push into walls + upward knockup
            target.setDeltaMovement((target.getX() - tx) * 0.8D, 1.45D, (target.getZ() - tz) * 0.8D);
            target.hurtMarked = true;

            // Daño de empuje (10/seg por 3s) + Wall Slam (+20 de daño al chocar pared)
            CompoundTag tTag = target.getPersistentData();
            tTag.putInt("xebMeteorStrikePushTimer", 60); // 3 segundos (60 ticks)
            tTag.putBoolean("xebMeteorStrikeCanWallSlam", true);
        }

        // ── ANILLO DE ONDA EXPANSIVA & KNOCKUP (12m) ───────────────────────────
        AABB waveBox = new AABB(tx - 6.0D, ty - 2.0D, tz - 6.0D, tx + 6.0D, ty + 5.0D, tz + 6.0D);
        List<LivingEntity> waveTargets = level.getEntitiesOfClass(LivingEntity.class, waveBox,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player) && !epicenterTargets.contains(e));

        for (LivingEntity target : waveTargets) {
            double dist = target.distanceToSqr(tx, ty, tz);
            float distRatio = (float) (1.0D - Math.min(1.0D, Math.sqrt(dist) / 6.0D));
            float waveDamage = epicenterDamage * 0.50F * distRatio; // 50% de daño epicentro
            target.hurt(player.damageSources().playerAttack(player), Math.max(15.0F, waveDamage));

            // Radial push outward into walls and upward knockup
            double dx = target.getX() - tx;
            double dz = target.getZ() - tz;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0.01) {
                dx /= len;
                dz /= len;
                target.setDeltaMovement(dx * 2.5D * distRatio, 1.2D * distRatio, dz * 2.5D * distRatio);
                target.hurtMarked = true;
            }

            // Daño de empuje (10/seg por 3s) + Wall Slam (+20 de daño al chocar pared)
            CompoundTag tTag = target.getPersistentData();
            tTag.putInt("xebMeteorStrikePushTimer", 60); // 3 segundos (60 ticks)
            tTag.putBoolean("xebMeteorStrikeCanWallSlam", true);
        }

        // ── OVERHEALTH PASIVA (ABSORCIÓN +30 POR ENEMIGO IMPACTADO) ────────────
        int totalHitCount = epicenterTargets.size() + waveTargets.size();
        if (totalHitCount > 0) {
            float overhealth = Math.min(150.0F, totalHitCount * 30.0F); // +30 absorption per enemy hit
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, (int) (overhealth / 4.0F)));
        }

        // ── EFECTOS VISUALES DE CRÁTER (BLOQUES DEL TERRENO ROTOS) ──────────────
        level.playSound(null, tx, ty, tz, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.5F, 0.5F);
        level.playSound(null, tx, ty, tz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 0.7F);
        level.playSound(null, tx, ty, tz, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.5F, 0.5F);

        BlockPos groundPos = new BlockPos((int) Math.floor(tx), (int) Math.floor(ty - 1.0), (int) Math.floor(tz));
        BlockState groundState = level.getBlockState(groundPos);
        if (!groundState.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                    tx, ty + 0.5, tz, 100, 1.8, 0.8, 1.8, 0.3);
        }

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tx, ty + 0.5, tz, 6, 1.2, 1.2, 1.2, 0.0);
        if (!holdsV2) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, tx, ty + 0.5, tz, 2, 0.0, 0.0, 0.0, 0.0);
        }

        for (double r = 1.0; r <= 8.0; r += 1.2) {
            int particleCount = (int) (r * 14);
            for (int i = 0; i < particleCount; i++) {
                double angle = (2 * Math.PI / particleCount) * i;
                double px = tx + r * Math.cos(angle);
                double pz = tz + r * Math.sin(angle);
                if (holdsV2) {
                    level.sendParticles(ParticleTypes.FLAME, px, ty + 0.2, pz, 2, 0.1, 0.3, 0.1, 0.06);
                    level.sendParticles(ParticleTypes.LAVA, px, ty + 0.2, pz, 1, 0.0, 0.1, 0.0, 0.0);
                } else {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, ty + 0.2, pz, 2, 0.1, 0.3, 0.1, 0.06);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, ty + 0.2, pz, 2, 0.1, 0.3, 0.1, 0.06);
                    level.sendParticles(ParticleTypes.END_ROD, px, ty + 0.2, pz, 1, 0.05, 0.2, 0.05, 0.04);
                }
            }
        }

        // Enforce 400s (8000 ticks) cooldown on the Meteor Strike curio item upon landing
        boolean devBypass = nbt.getBoolean("xebDevCooldownsDisabled");
        if (!devBypass) {
            player.getCooldowns().addCooldown(ModItems.METEOR_STRIKE.get(), 8000);
        }

        // Reset state & sync to clients
        nbt.putInt("xebMeteorStrikeState", 0);

        nbt.remove("xebMeteorStrikeLaunchTimer");
        nbt.remove("xebMeteorStrikeTargetingTicks");
        nbt.remove("xebMeteorStrikeIsV2");
        nbt.remove("xebMeteorStrikeIsUltra");
        nbt.remove("xebMeteorStrikeTargetX");
        nbt.remove("xebMeteorStrikeTargetY");
        nbt.remove("xebMeteorStrikeTargetZ");
        nbt.remove("xebMeteorStrikeTargetCount");
        nbt.putBoolean("xebExtremeBurstActive", false);
        nbt.remove("xebExtremeBurstId");

        syncToClients(player, 0, holdsV2, tx, ty, tz, 0);
    }

    private static void syncToClients(ServerPlayer player, int state, boolean isV2, double tx, double ty, double tz, int count) {
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new MeteorStrikeSyncPacket(player.getId(), state, isV2, tx, ty, tz, count));
    }
}
