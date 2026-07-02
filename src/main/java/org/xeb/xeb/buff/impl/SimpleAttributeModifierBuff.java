package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * Base class for buffs that simply add a single transient attribute modifier on
 * attach and remove it on detach. Eliminates the identical boilerplate found in
 * DamagingBuff, ToughBuff, SpeedyBuff, and LuckyBuff.
 */
public abstract class SimpleAttributeModifierBuff extends EliteBuff {

    private final Attribute attribute;
    private final double amount;
    private final AttributeModifier.Operation operation;
    private final String modifierName;

    /**
     * @param id            buff id
     * @param displayName   display name
     * @param buffType      buff type
     * @param color         color
     * @param weight        weight
     * @param attribute     the attribute to modify
     * @param amount        the modifier amount (used directly; boss vs non-boss
     *                      scaling is not applied here — override
     *                      {@link #getAmount} if needed)
     * @param operation     the modifier operation
     * @param modifierName  name stored in the AttributeModifier
     */
    protected SimpleAttributeModifierBuff(String id, String displayName, BuffType buffType,
                                          int color, double weight,
                                          Attribute attribute, double amount,
                                          AttributeModifier.Operation operation,
                                          String modifierName) {
        super(id, displayName, buffType, color, weight, true);
        this.attribute = attribute;
        this.amount = amount;
        this.operation = operation;
        this.modifierName = modifierName;
    }

    /**
     * Returns the modifier amount for the given entity. Override to provide
     * boss-dependent scaling.
     */
    protected double getAmount(LivingEntity entity) {
        return amount;
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onAttach(LivingEntity entity, UUID medallionId) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            if (instance.getModifier(medallionId) == null) {
                double amt = getAmount(entity);
                AttributeModifier modifier = new AttributeModifier(medallionId, modifierName, amt, operation);
                instance.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity, UUID medallionId) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(medallionId);
        }
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}
}
