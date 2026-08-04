package org.xeb.xeb.extremeburst;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.network.OpticBlastBeamPacket;
import org.xeb.xeb.network.XEBNetwork;

import java.util.List;

/**
 * Manejador del servidor para el Extreme Burst: Full-Aperture Supernova (Optic Blast).
 *
 * <p>Secuencia Cinemática Auténtica (Marvel Rivals):
 * 1. Etapa 1 (3x3 Láser Óptico - 1.0s / Ticks 80 a 60): Desbloqueo del visor y emisión inicial de haz rojo de 3x3.
 * 2. Etapa 2 (4x4 Láser Óptico + Recoil de Cámara - 2.0s / Ticks 60 a 20): El rayo crece a 4x4 y el retroceso óptico
 *    fuerza la cámara del jugador elevando la inclinación hacia el cielo.
 * 3. Etapa 3 (6x6 MEGA-LÁSER Clímax - 1.0s / Ticks 20 a 0): El rayo se expande a 6x6 con sacudida de pantalla masiva.
 * 4. Etapa 4 (Detonación Supernova): Anillo XebWaves (10m) Carmesí Mítico, +70.0 daño remate, Knockup y Wall Slam (+30.0),
 *    aplicando 300s (6000 ticks) de enfriamiento al curio.</p>
 */
public class OpticBlastBurstHandler {

    public static final int TOTAL_BURST_TICKS = 80; // 4.0 segundos totales

    // Partículas de polvo de energía de Rubí-Cuarzo carmesí brillante (RGB: 255, 10, 25)
    private static final DustParticleOptions RUBY_LASER_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.04F, 0.10F), 2.0F);

    public static void activate(ServerPlayer player, ExtremeBurstRegistry.ExtremeBurstEntry entry) {
        CompoundTag nbt = player.getPersistentData();

        // Verificar si el jugador sostiene Optic Blast en mano principal o secundaria
        boolean holdsOptic = player.getMainHandItem().is(ModItems.OPTIC_BLAST.get())
                || player.getOffhandItem().is(ModItems.OPTIC_BLAST.get());

        if (!holdsOptic) {
            player.displayClientMessage(Component.literal("§c¡Requiere Optic Blast equipado en mano!"), true);
            return;
        }

        // Verificar enfriamiento
        if (!nbt.getBoolean("xebDevCooldownsDisabled") && player.getCooldowns().isOnCooldown(ModItems.FULL_APERTURE_SUPERNOVA.get())) {
            return;
        }

        nbt.putInt("xebOpticBurstState", 1);
        nbt.putInt("xebOpticBurstTimer", TOTAL_BURST_TICKS);
        nbt.putFloat("xebOpticBurstStartPitch", player.getXRot());
        nbt.putFloat("xebOpticBurstStartYaw", player.getYRot());
        nbt.putBoolean("xebExtremeBurstActive", true);

        // Sincronizar estado al cliente mediante paquete de red
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new org.xeb.xeb.network.OpticBlastBurstSyncPacket(player.getId(), 1, TOTAL_BURST_TICKS, player.getXRot()));

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 0.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 1.4F);
    }

    public static void tickServer(ServerPlayer player) {
        CompoundTag nbt = player.getPersistentData();
        int state = nbt.getInt("xebOpticBurstState");
        if (state == 0) return;

        int timer = nbt.getInt("xebOpticBurstTimer");
        timer--;
        nbt.putInt("xebOpticBurstTimer", timer);

        ServerLevel level = player.serverLevel();
        float startPitch = nbt.getFloat("xebOpticBurstStartPitch");

        // Sincronizar tick actual al cliente
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new org.xeb.xeb.network.OpticBlastBurstSyncPacket(player.getId(), state, timer, startPitch));

        // Detener movimiento lateral del jugador durante el disparo de retroceso masivo
        player.setDeltaMovement(player.getDeltaMovement().x * 0.05D, player.getDeltaMovement().y * 0.05D, player.getDeltaMovement().z * 0.05D);
        // Servidor: Calcular inclinación de retroceso cinético hacia el cielo (-85°)
        if (timer <= 75 && timer > 0) {
            float progress = (75 - timer) / 75.0F; // 0.0F a 1.0F
            float accel = (float) Math.pow(progress, 1.4D);
            float targetPitch = startPitch - (accel * (startPitch + 85.0F));
            player.setXRot(targetPitch);
        }

        if (timer > 60) {
            // ── ETAPA 1: RAYO ÓPTICO INICIAL (3x3 DIÁMETRO - 1.0s) ───────────────────
            executeLaserBeam(player, level, 3.0D, 14.0F, false);
        } else if (timer > 20) {
            // ── ETAPA 2: CRECIMIENTO (4x4 A 6x6) + RECOIL DE CÁMARA HACIA EL CIELO (2.0s) ──
            executeLaserBeam(player, level, 6.0D, 20.0F, false);
        } else if (timer > 0) {
            // ── ETAPA 3: CLÍMAX 12x12 MEGA-LÁSER DE APERTURA MÁXIMA (1.0s) ───────────
            executeLaserBeam(player, level, 12.0D, 32.0F, true);
        } else {
            // ── ETAPA 4: DETONACIÓN SUPERNOVA Y ONDA EXPANSIVA ──────────────────────
            executeSupernovaDetonation(player, level);
        }
    }

    private static void executeLaserBeam(ServerPlayer player, ServerLevel level, double beamWidth, float damage, boolean isMegaBeam) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 rayEnd = eyePos.add(lookVec.scale(42.0D));

        BlockHitResult hit = level.clip(new ClipContext(
                eyePos, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 impactPos = (hit.getType() != HitResult.Type.MISS) ? hit.getLocation() : rayEnd;

        // Enviar paquete de renderizado 3D de rayo láser rojo a todos los clientes cercanos (Tipo 2 = Supernova Exponencial)
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new OpticBlastBeamPacket(player.getId(), true, eyePos.x, eyePos.y - 0.1D, eyePos.z,
                        impactPos.x, impactPos.y, impactPos.z, OpticBlastBeamPacket.BEAM_FULL_APERTURE_SUPERNOVA));

        // Dejar impacto de desintegración en el bloque golpeado (Sin partículas extras flotantes a lo largo del rayo)
        if (hit.getType() != HitResult.Type.MISS) {
            BlockPos hitBlock = hit.getBlockPos();
            BlockState bState = level.getBlockState(hitBlock);
            if (!bState.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, bState),
                        impactPos.x, impactPos.y + 0.2, impactPos.z, 10, 0.4, 0.2, 0.4, 0.1);
            }
            level.sendParticles(ParticleTypes.FLASH, impactPos.x, impactPos.y + 0.2, impactPos.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // Registrar rayo en ActiveBeamManager para detección de duelos de choque de rayos (Beam Struggle)
        org.xeb.xeb.opticblast.BeamData supernovaBeam = new org.xeb.xeb.opticblast.BeamData(
                player.getUUID(),
                player.getId(),
                eyePos,
                impactPos,
                0xFF0510, // Color rojo Rubí-Cuarzo
                level.getGameTime(),
                level.getGameTime() + 2,
                "optic_supernova"
        );
        org.xeb.xeb.opticblast.ActiveBeamManager.get().putBeam(player.getUUID(), supernovaBeam);

        // Escanear y infligir daño + empuje a enemigos en la trayectoria del rayo respetando su diámetro
        AABB beamBox = new AABB(eyePos, impactPos).inflate(beamWidth / 2.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, beamBox,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player));

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), damage);

            // Retroceso cinético violento impulsando hacia atrás y arriba
            target.setDeltaMovement(lookVec.x * 1.1D, lookVec.y * 0.6D + 0.35D, lookVec.z * 1.1D);
            target.hurtMarked = true;

            // Marcar para Wall Slam
            CompoundTag tTag = target.getPersistentData();
            tTag.putInt("xebOpticBurstPushTimer", 40);
            tTag.putBoolean("xebOpticBurstCanWallSlam", true);
        }

        if (isMegaBeam) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.8F, 1.1F);
        }
    }

    private static void executeSupernovaDetonation(ServerPlayer player, ServerLevel level) {
        CompoundTag nbt = player.getPersistentData();

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 rayEnd = eyePos.add(lookVec.scale(42.0D));

        BlockHitResult hit = level.clip(new ClipContext(
                eyePos, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 impactPos = (hit.getType() != HitResult.Type.MISS) ? hit.getLocation() : rayEnd;

        // Apagar renderizado del rayo 3D en clientes
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new OpticBlastBeamPacket(player.getId(), false, eyePos.x, eyePos.y, eyePos.z,
                        impactPos.x, impactPos.y, impactPos.z, OpticBlastBeamPacket.BEAM_FULL_APERTURE_SUPERNOVA));

        // Sonidos finales de explosión Supernova
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.5F, 0.5F);
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.5F, 1.5F);

        // Daño de área de detonación remate (70.0F) en radio de 10m
        AABB detBox = new AABB(impactPos.x - 5.0D, impactPos.y - 3.0D, impactPos.z - 5.0D,
                               impactPos.x + 5.0D, impactPos.y + 5.0D, impactPos.z + 5.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, detBox,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player));

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), 70.0F);

            // Gran impulso de elevación y dispersión radial
            double dx = target.getX() - impactPos.x;
            double dz = target.getZ() - impactPos.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.01) {
                dx /= dist;
                dz /= dist;
            }
            target.setDeltaMovement(dx * 1.8D, 1.4F, dz * 1.8D);
            target.hurtMarked = true;

            // Marcar para Wall Slam masivo (+30.0F daño extra al chocar pared)
            CompoundTag tTag = target.getPersistentData();
            tTag.putInt("xebOpticBurstPushTimer", 60);
            tTag.putBoolean("xebOpticBurstCanWallSlam", true);
        }

        // Aplicar enfriamiento de 300 segundos (6000 ticks) al ítem curio
        boolean devBypass = nbt.getBoolean("xebDevCooldownsDisabled");
        if (!devBypass) {
            player.getCooldowns().addCooldown(ModItems.FULL_APERTURE_SUPERNOVA.get(), 6000);
        }

        // Remover rayo de ActiveBeamManager al detonar
        org.xeb.xeb.opticblast.ActiveBeamManager.get().removeBeam(player.getUUID());

        // Reiniciar estado NBT
        nbt.putInt("xebOpticBurstState", 0);
        nbt.remove("xebOpticBurstTimer");
        nbt.remove("xebOpticBurstStartPitch");
        nbt.remove("xebOpticBurstStartYaw");

        // Notificar al cliente la finalización del estallido
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new org.xeb.xeb.network.OpticBlastBurstSyncPacket(player.getId(), 0, 0, 0.0F));
    }
}

