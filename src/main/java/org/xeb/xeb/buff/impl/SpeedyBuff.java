package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SpeedyBuff extends SimpleAttributeModifierBuff {
    public SpeedyBuff() {
        super("speedy", "Speedy", BuffType.UNIVERSAL, 0x00CED1, 10.0D,
                Attributes.MOVEMENT_SPEED, 0.2D, AttributeModifier.Operation.ADDITION,
                "Speedy Buff Modifier");
    }
}
