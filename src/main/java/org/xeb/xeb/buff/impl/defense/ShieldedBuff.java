package org.xeb.xeb.buff.impl.defense;
import java.util.Set;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

public class ShieldedBuff extends EliteBuff {
    private static final String SHIELD_KEY = "xebShield";
    private static final String SHIELD_COUNT_KEY = "xebShieldCount";
    // Single health penalty UUID — the penalty is applied once and caps regardless of how many stacks
    private static final UUID HEALTH_PENALTY_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de089999");

    public ShieldedBuff() {
        super("shielded", "Shielded", BuffType.UNIVERSAL, 0x4169E1, 5.0D, true);
    }

    
    @Override
    public Set<String> getConflicts() {
        return Set.of("protected");
    }
    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onAttach(LivingEntity entity, UUID medallionId) {
        Difficulty difficulty = entity.level().getDifficulty();
        boolean isBoss = MedallionManager.isBoss(entity);
        int shield;

        if (isBoss) {
            shield = switch (difficulty) {
                case EASY -> 4;
                case NORMAL -> 8;
                case HARD -> 16;
                default -> 8;
            };
        } else {
            shield = switch (difficulty) {
                case EASY -> 3;
                case NORMAL -> 5;
                case HARD -> 7;
                default -> 5;
            };
        }

        CompoundTag tag = entity.getPersistentData();
        // Stack shield points additively
        int current = tag.contains(SHIELD_KEY) ? tag.getInt(SHIELD_KEY) : 0;
        tag.putInt(SHIELD_KEY, current + shield);

        // Track how many shielded buffs are active (for penalty management)
        int count = tag.contains(SHIELD_COUNT_KEY) ? tag.getInt(SHIELD_COUNT_KEY) : 0;
        tag.putInt(SHIELD_COUNT_KEY, count + 1);

        // Health penalty only applied once regardless of stack count
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getModifier(HEALTH_PENALTY_UUID) == null) {
            maxHealth.addTransientModifier(new AttributeModifier(HEALTH_PENALTY_UUID, "Shielded Health Penalty", -0.20D, AttributeModifier.Operation.MULTIPLY_BASE));
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }
    }

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity, UUID medallionId) {
        CompoundTag tag = entity.getPersistentData();
        int count = tag.contains(SHIELD_COUNT_KEY) ? tag.getInt(SHIELD_COUNT_KEY) - 1 : 0;
        if (count <= 0) {
            // Last shielded buff removed — clear everything
            tag.remove(SHIELD_KEY);
            tag.remove(SHIELD_COUNT_KEY);
            AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.removeModifier(HEALTH_PENALTY_UUID);
            }
        } else {
            tag.putInt(SHIELD_COUNT_KEY, count);
        }
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}

    @Override
    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {
        CompoundTag tag = entity.getPersistentData();
        if (tag.contains(SHIELD_KEY)) {
            int shield = tag.getInt(SHIELD_KEY);
            if (shield > 0) {
                float damage = event.getAmount();
                if (shield >= damage) {
                    shield -= damage;
                    tag.putInt(SHIELD_KEY, shield);
                    event.setAmount(0.0F);
                } else {
                    float remaining = damage - shield;
                    tag.putInt(SHIELD_KEY, 0);
                    event.setAmount(remaining);
                }
            }
        }
    }
}
