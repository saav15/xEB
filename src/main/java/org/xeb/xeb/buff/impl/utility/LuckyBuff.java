package org.xeb.xeb.buff.impl.utility;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.util.DodgeHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

public class LuckyBuff extends EliteBuff {
    public LuckyBuff() {
        super("lucky", "Lucky", BuffType.UNIVERSAL, 0xFFD700, 1.0D, true);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onAttach(LivingEntity entity, UUID medallionId) {
        AttributeInstance instance = entity.getAttribute(Attributes.LUCK);
        if (instance != null) {
            AttributeModifier modifier = new AttributeModifier(medallionId, "Lucky Buff Modifier", 3.0D, AttributeModifier.Operation.ADDITION);
            instance.addTransientModifier(modifier);
        }
    }

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity, UUID medallionId) {
        AttributeInstance instance = entity.getAttribute(Attributes.LUCK);
        if (instance != null) {
            instance.removeModifier(medallionId);
        }
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        // N5 — use vanilla sendParticles instead of a custom network packet.
        if (entity.tickCount % 10 == 0) {
            level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.CRIT,
                entity.getX(), entity.getY() + 0.5, entity.getZ(),
                1, 0.2, 0.3, 0.2, 0.0
            );
        }
    }

    @Override
    public void onDamageDealt(LivingEntity entity, LivingHurtEvent event) {
        if (entity.getRandom().nextFloat() < 0.10F) {
            event.setAmount(event.getAmount() * 2.0F);
            LivingEntity target = event.getEntity();
            if (target != null && !entity.level().isClientSide() && entity.level() instanceof ServerLevel sl) {
                sl.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CRIT,
                    target.getX(), target.getY() + 0.5, target.getZ(),
                    8, 0.5, 0.5, 0.5, 0.1
                );
            }
        }
    }

    @Override
    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {
        // Skip void / instakill damage
        if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                || event.getAmount() >= 1000.0F) {
            return;
        }
        // 10% flat dodge chance — cancels damage AND all secondary effects
        if (entity.getRandom().nextFloat() < 0.10F) {
            DodgeHelper.triggerDodge(entity, event);
        }
    }

    @Override
    public void onProjectileImpact(LivingEntity entity, ProjectileImpactEvent event) {
        // Same 10% dodge applies to projectiles — cancels the impact entirely
        if (entity.getRandom().nextFloat() < 0.10F) {
            DodgeHelper.triggerDodge(entity, event);
        }
    }
}
