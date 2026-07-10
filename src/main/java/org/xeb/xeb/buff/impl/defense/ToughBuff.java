package org.xeb.xeb.buff.impl.defense;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class ToughBuff extends EliteBuff {
    public ToughBuff() {
        super("tough", "Tough", BuffType.UNIVERSAL, 0x696969, 1.0D, true);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onAttach(LivingEntity entity, UUID medallionId) {
        AttributeInstance instance = entity.getAttribute(Attributes.ARMOR);
        if (instance != null) {
            if (instance.getModifier(medallionId) == null) {
                double amount = MedallionManager.isBoss(entity) ? 1.0D : 2.0D;
                AttributeModifier modifier = new AttributeModifier(medallionId, "Tough Buff Modifier", amount, AttributeModifier.Operation.ADDITION);
                instance.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity, UUID medallionId) {
        AttributeInstance instance = entity.getAttribute(Attributes.ARMOR);
        if (instance != null) {
            instance.removeModifier(medallionId);
        }
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}
}
