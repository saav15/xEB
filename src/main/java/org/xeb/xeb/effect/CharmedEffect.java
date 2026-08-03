package org.xeb.xeb.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class CharmedEffect extends MobEffect {
    public CharmedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF69B4); // Hot Pink
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        // Clean up NBT owner when Charmed effect expires or is removed
        if (!entity.level().isClientSide()) {
            entity.getPersistentData().remove("xebCharmedOwner");
            entity.getPersistentData().remove("xebCharmedOwnerName");
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
