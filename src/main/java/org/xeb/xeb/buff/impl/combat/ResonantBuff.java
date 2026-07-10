package org.xeb.xeb.buff.impl.combat;
import java.util.Set;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.UUID;

public class ResonantBuff extends EliteBuff {
    private static final String STACKS_KEY = "xebResonantStacks";
    private static final UUID RESONANT_DAMAGE_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08aa00");
    private static final UUID RESONANT_ARMOR_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08aa01");
    private static final UUID RESONANT_HEALTH_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08aa02");
    private static final UUID RESONANT_SPEED_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08aa03");

    public ResonantBuff() {
        super("resonant", "Resonant", BuffType.ENEMY_ONLY, 0x9370DB, 5.0D);
    }

    
    @Override
    public Set<String> getConflicts() {
        return Set.of("mirror");
    }
    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        tag.remove(STACKS_KEY);
        removeModifiers(entity);
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}

    public void handleNearbyItemUse(LivingEntity entity, LivingEntityUseItemEvent.Finish event) {
        if (entity.level() == event.getEntity().level()) {
            double distanceSq = entity.distanceToSqr(event.getEntity());
            if (distanceSq <= 64.0D) { // Within 8 blocks (8^2 = 64)
                entity.heal(1.0F);

                CompoundTag tag = entity.getPersistentData();
                int stacks = tag.getInt(STACKS_KEY) + 1;
                tag.putInt(STACKS_KEY, stacks);

                // Update attributes
                updateModifiers(entity, stacks);
            }
        }
    }

    private void removeModifiers(LivingEntity entity) {
        AttributeInstance dmg = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.removeModifier(RESONANT_DAMAGE_UUID);

        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.removeModifier(RESONANT_ARMOR_UUID);

        AttributeInstance hp = entity.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.removeModifier(RESONANT_HEALTH_UUID);

        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(RESONANT_SPEED_UUID);
    }

    private void updateModifiers(LivingEntity entity, int stacks) {
        removeModifiers(entity);

        AttributeInstance dmg = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.addTransientModifier(new AttributeModifier(RESONANT_DAMAGE_UUID, "Resonant Damage Boost", stacks * 1.0D, AttributeModifier.Operation.ADDITION));
        }

        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.addTransientModifier(new AttributeModifier(RESONANT_ARMOR_UUID, "Resonant Armor Boost", stacks * 1.0D, AttributeModifier.Operation.ADDITION));
        }

        AttributeInstance hp = entity.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.addTransientModifier(new AttributeModifier(RESONANT_HEALTH_UUID, "Resonant Health Boost", stacks * 1.0D, AttributeModifier.Operation.ADDITION));
        }

        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addTransientModifier(new AttributeModifier(RESONANT_SPEED_UUID, "Resonant Speed Boost", stacks * 0.02D, AttributeModifier.Operation.ADDITION));
        }
    }
}
