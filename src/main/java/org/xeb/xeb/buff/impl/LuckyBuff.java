package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.util.BuffParticleHelper;
import org.xeb.xeb.util.DodgeHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class LuckyBuff extends SimpleAttributeModifierBuff {
    public LuckyBuff() {
        super("lucky", "Lucky", BuffType.UNIVERSAL, 0xFFD700, 5.0D,
                Attributes.LUCK, 3.0D, AttributeModifier.Operation.ADDITION,
                "Lucky Buff Modifier");
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        if (entity.tickCount % 10 == 0) {
            BuffParticleHelper.sendParticles(entity, "crit", 1);
        }
    }

    @Override
    public void onDamageDealt(LivingEntity entity, LivingHurtEvent event) {
        if (entity.getRandom().nextFloat() < 0.10F) {
            event.setAmount(event.getAmount() * 2.0F);
            LivingEntity target = event.getEntity();
            if (target != null) {
                BuffParticleHelper.sendParticles(target, "crit", 8);
            }
        }
    }

    @Override
    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {
        if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                || event.getAmount() >= 1000.0F) {
            return;
        }
        if (entity.getRandom().nextFloat() < 0.10F) {
            DodgeHelper.triggerDodge(entity, event);
        }
    }

    @Override
    public void onProjectileImpact(LivingEntity entity, ProjectileImpactEvent event) {
        if (entity.getRandom().nextFloat() < 0.10F) {
            DodgeHelper.triggerDodge(entity, event);
        }
    }
}
