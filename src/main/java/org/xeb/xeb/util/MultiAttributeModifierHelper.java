package org.xeb.xeb.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * Shared helper for buffs that add/remove a fixed set of attribute modifiers
 * scaled by a stack count (e.g. MadBuff, ResonantBuff).
 */
public final class MultiAttributeModifierHelper {

    private MultiAttributeModifierHelper() {}

    /**
     * Describes a single attribute modifier entry: attribute, UUID, name, and
     * per-stack scaling amount.
     */
    public record ModifierEntry(Attribute attribute, UUID uuid, String name, double perStackAmount,
                                AttributeModifier.Operation operation) {

        public ModifierEntry(Attribute attribute, UUID uuid, String name, double perStackAmount) {
            this(attribute, uuid, name, perStackAmount, AttributeModifier.Operation.ADDITION);
        }
    }

    /**
     * Removes all modifiers in the given entries from the entity's attributes.
     */
    public static void removeAll(LivingEntity entity, ModifierEntry... entries) {
        for (ModifierEntry entry : entries) {
            AttributeInstance inst = entity.getAttribute(entry.attribute());
            if (inst != null) {
                inst.removeModifier(entry.uuid());
            }
        }
    }

    /**
     * Removes then re-applies all modifiers, scaled by the given stack count.
     */
    public static void updateAll(LivingEntity entity, int stacks, ModifierEntry... entries) {
        removeAll(entity, entries);
        for (ModifierEntry entry : entries) {
            AttributeInstance inst = entity.getAttribute(entry.attribute());
            if (inst != null) {
                inst.addTransientModifier(new AttributeModifier(
                        entry.uuid(), entry.name(), stacks * entry.perStackAmount(), entry.operation()));
            }
        }
    }
}
