package org.xeb.xeb.extremeburst;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.xeb.xeb.item.ModItems;

import java.util.List;

/**
 * Dispatches Extreme Burst activation to the appropriate item-specific handler.
 *
 * <p>Called from {@link org.xeb.xeb.network.ActuarKeyPacket} when button==3 is pressed
 * and a registered burst curio is equipped — mirroring the existing Quantum Cat Barrage
 * activation pattern.</p>
 */
public class ExtremeBurstHandler {

    public static void handleActivation(ServerPlayer player, ExtremeBurstRegistry.ExtremeBurstEntry entry) {
        if (entry.curioItem == ModItems.QUANTUM_CAT_BARRAGE.get()) {
            handleQuantumCatBarrage(player);
        } else if (entry.curioItem == ModItems.DOGMA.get()) {
            handleDogma(player);
        } else if (entry.curioItem == ModItems.OMEGA_FLOWERY.get()) {
            handleOmegaFlowery(player);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // QUANTUM CAT BARRAGE — Universal / Instant
    // (logic moved from ActuarKeyPacket so the packet stays lean)
    // ═══════════════════════════════════════════════════════════════════════════
    static void handleQuantumCatBarrage(ServerPlayer player) {
        float totalDamage = org.xeb.xeb.item.QuantumCatBarrageItem.getDamageDealtLast60Seconds(player);
        AABB area = player.getBoundingBox().inflate(10.0D);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player));

        if (!targets.isEmpty()) {
            float damagePerTarget = totalDamage / targets.size();
            for (LivingEntity target : targets) {
                float finalDmg = Math.max(2.0F, damagePerTarget);
                target.hurt(player.damageSources().playerAttack(player), finalDmg);
                target.addEffect(new MobEffectInstance(
                        org.xeb.xeb.effect.ModEffects.NO_HEALTH_REGEN.get(), 200, 0));

                if (player.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 4; i++) {
                        double ox = (player.getRandom().nextDouble() - 0.5D) * 1.2D;
                        double oy = player.getRandom().nextDouble() * target.getBbHeight();
                        double oz = (player.getRandom().nextDouble() - 0.5D) * 1.2D;
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                target.getX() + ox, target.getY() + oy, target.getZ() + oz,
                                1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                            8, 0.3D, 0.3D, 0.3D, 0.2D);
                }
            }
            player.level().playSound(null, player, SoundEvents.CAT_HISS, SoundSource.PLAYERS, 1.2F, 1.2F);
            player.level().playSound(null, player, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
        }

        // Instant — burst already complete, clear active flag
        player.getPersistentData().putBoolean("xebExtremeBurstActive", false);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DOGMA — Limited (The Tears) / Instant
    // Growing brimstone beam; ticking handled by DogmaBurstHandler
    // ═══════════════════════════════════════════════════════════════════════════
    static void handleDogma(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        player.getPersistentData().putInt("xebDogmaBrimstoneTicks", DogmaBurstHandler.DOGMA_DURATION);
        player.getPersistentData().putInt("xebDogmaBrimstoneMaxTicks", DogmaBurstHandler.DOGMA_DURATION);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 0.5F);
        // Ticking handled per-tick by DogmaBurstHandler (registered in BuffTickHandler)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OMEGA FLOWERY — Limited (Golden Flower) / Instance
    // Transformed state; ticking handled by OmegaFloweryHandler
    // ═══════════════════════════════════════════════════════════════════════════
    static void handleOmegaFlowery(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        // Suspend in air
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;

        // Absorb any active Flower Dance clones
        player.getPersistentData().putBoolean("xebFlowerDanceActive", false);
        player.getPersistentData().putInt("xebFlowerDanceTicksRemaining", 0);

        // Set Omega Flowery state
        player.getPersistentData().putBoolean("xebOmegaFloweryActive", true);
        player.getPersistentData().putInt("xebOmegaFloweryTicks", OmegaFloweryHandler.OMEGA_DURATION);
        player.getPersistentData().putInt("xebOmegaFloweryMaxTicks", OmegaFloweryHandler.OMEGA_DURATION);

        // Apply transformation buffs
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, OmegaFloweryHandler.OMEGA_DURATION, 1));
        player.addEffect(new MobEffectInstance(org.xeb.xeb.effect.ModEffects.ALL_STATS_UP.get(), OmegaFloweryHandler.OMEGA_DURATION, 0));

        // Visual + sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0F, 1.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(),
                30, 1.0, 1.0, 1.0, 0.1);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 1, player.getZ(),
                20, 0.5, 0.5, 0.5, 0.05);
    }
}
