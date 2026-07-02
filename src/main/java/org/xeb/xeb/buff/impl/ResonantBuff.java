package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.util.MultiAttributeModifierHelper;
import org.xeb.xeb.util.MultiAttributeModifierHelper.ModifierEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.UUID;

public class ResonantBuff extends EliteBuff {
    private static final String STACKS_KEY = "xebResonantStacks";

    private static final ModifierEntry[] MODIFIER_ENTRIES = {
            new ModifierEntry(Attributes.ATTACK_DAMAGE, UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08aa00"), "Resonant Damage Boost", 1.0D),
            new ModifierEntry(Attributes.ARMOR, UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08aa01"), "Resonant Armor Boost", 1.0D),
            new ModifierEntry(Attributes.MAX_HEALTH, UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08aa02"), "Resonant Health Boost", 1.0D),
            new ModifierEntry(Attributes.MOVEMENT_SPEED, UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08aa03"), "Resonant Speed Boost", 0.02D),
    };

    public ResonantBuff() {
        super("resonant", "Resonant", BuffType.ENEMY_ONLY, 0x9370DB, 1.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {
        entity.getPersistentData().remove(STACKS_KEY);
        MultiAttributeModifierHelper.removeAll(entity, MODIFIER_ENTRIES);
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}

    public void handleNearbyItemUse(LivingEntity entity, LivingEntityUseItemEvent.Finish event) {
        if (entity.level() == event.getEntity().level()) {
            double distanceSq = entity.distanceToSqr(event.getEntity());
            if (distanceSq <= 64.0D) {
                entity.heal(1.0F);

                CompoundTag tag = entity.getPersistentData();
                int stacks = tag.getInt(STACKS_KEY) + 1;
                tag.putInt(STACKS_KEY, stacks);

                MultiAttributeModifierHelper.updateAll(entity, stacks, MODIFIER_ENTRIES);
            }
        }
    }
}
