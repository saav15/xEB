package org.xeb.xeb.util;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

/**
 * Shared dodge logic used by Lucky and Adrenaline effects.
 * Fully cancels a hurt event (or projectile impact) and plays a
 * subtle wave-ring particle + dodge sound on the dodging entity.
 */
public class DodgeHelper {

    /**
     * Cancels a LivingHurtEvent as a "dodge": zeroes damage, marks the event
     * cancelled, spawns dodge_wave particles and plays a dodge sound.
     */
    public static void triggerDodge(LivingEntity entity, LivingHurtEvent event) {
        event.setAmount(0.0F);
        event.setCanceled(true);
        sendDodgeEffects(entity);
    }

    /**
     * Cancels a ProjectileImpactEvent as a "dodge": cancels the impact
     * so no damage or secondary effects are applied, then plays effects.
     */
    public static void triggerDodge(LivingEntity entity, ProjectileImpactEvent event) {
        event.setCanceled(true);
        sendDodgeEffects(entity);
    }

    private static void sendDodgeEffects(LivingEntity entity) {
        if (entity.level().isClientSide()) return;

        BuffParticleHelper.sendParticles(entity, "dodge_wave", 12);
        BuffParticleHelper.sendParticles(entity, "dodge", 5);

        entity.level().playSound(null,
                entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS,
                0.6F,
                1.8F + entity.getRandom().nextFloat() * 0.2F);
    }
}
