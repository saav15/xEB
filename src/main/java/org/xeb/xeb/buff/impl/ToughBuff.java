package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ToughBuff extends SimpleAttributeModifierBuff {
    public ToughBuff() {
        super("tough", "Tough", BuffType.UNIVERSAL, 0x696969, 10.0D,
                Attributes.ARMOR, 2.0D, AttributeModifier.Operation.ADDITION,
                "Tough Buff Modifier");
    }

    @Override
    protected double getAmount(LivingEntity entity) {
        return MedallionManager.isBoss(entity) ? 1.0D : 2.0D;
    }
}
