package org.xeb.xeb.buff.impl.defense;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class HealthyBuff extends EliteBuff {
    public HealthyBuff() {
        super("healthy", "Healthy", BuffType.ENEMY_ONLY, 0x228B22, 1.0D, true);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onAttach(LivingEntity entity, UUID medallionId) {
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            AttributeModifier modifier = new AttributeModifier(medallionId, "Healthy Buff Max Health", 4.0D, AttributeModifier.Operation.ADDITION);
            maxHealth.addTransientModifier(modifier);
            entity.heal(4.0F);
        }
        // Refresh Regen (level 2 = Regen III in UI, doesn't stack but persists)
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 2, false, false, false));
    }

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity, UUID medallionId) {
        entity.removeEffect(MobEffects.REGENERATION);
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(medallionId);
        }
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        if (!entity.hasEffect(MobEffects.REGENERATION)) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 2, false, false, false));
        }
    }
}
