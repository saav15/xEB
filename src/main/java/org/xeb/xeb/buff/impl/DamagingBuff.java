package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class DamagingBuff extends SimpleAttributeModifierBuff {
    public DamagingBuff() {
        super("damaging", "Damaging", BuffType.UNIVERSAL, 0xDC143C, 10.0D,
                Attributes.ATTACK_DAMAGE, 2.0D, AttributeModifier.Operation.ADDITION,
                "Damaging Buff Modifier");
    }

    @Override
    protected double getAmount(LivingEntity entity) {
        return MedallionManager.isBoss(entity) ? 1.0D : 2.0D;
    }
}
