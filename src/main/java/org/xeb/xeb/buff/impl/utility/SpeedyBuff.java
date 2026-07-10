package org.xeb.xeb.buff.impl.utility;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class SpeedyBuff extends EliteBuff {
    public SpeedyBuff() {
        super("speedy", "Speedy", BuffType.UNIVERSAL, 0x00CED1, 1.0D, true);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onAttach(LivingEntity entity, UUID medallionId) {
        AttributeInstance instance = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance != null) {
            if (instance.getModifier(medallionId) == null) {
                AttributeModifier modifier = new AttributeModifier(medallionId, "Speedy Buff Modifier", 0.2D, AttributeModifier.Operation.ADDITION);
                instance.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity, UUID medallionId) {
        AttributeInstance instance = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance != null) {
            instance.removeModifier(medallionId);
        }
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}
}
