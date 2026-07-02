package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.util.BuffParticleHelper;
import org.xeb.xeb.util.MultiAttributeModifierHelper;
import org.xeb.xeb.util.MultiAttributeModifierHelper.ModifierEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.UUID;

public class MadBuff extends EliteBuff {
    private static final String STACKS_KEY = "xebMadStacks";
    private static final UUID MAD_SPEED_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab40");

    private static final ModifierEntry[] STACK_ENTRIES = {
            new ModifierEntry(Attributes.ATTACK_DAMAGE, UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab41"), "Mad Damage Stack", 1.0D),
            new ModifierEntry(Attributes.ARMOR, UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab42"), "Mad Armor Stack", 1.0D),
            new ModifierEntry(Attributes.MAX_HEALTH, UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab43"), "Mad Health Stack", 1.0D),
            new ModifierEntry(Attributes.MOVEMENT_SPEED, UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab44"), "Mad Speed Stack", 0.02D),
    };

    public MadBuff() {
        super("mad", "Mad", BuffType.ENEMY_ONLY, 0xB22222, 1.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.addTransientModifier(new AttributeModifier(MAD_SPEED_UUID, "Mad Attack Speed modifier", 1.0D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        entity.addEffect(new MobEffectInstance(ModEffects.MADNESS.get(), -1, 0, false, false, true));
    }

    @Override
    public void onDetach(LivingEntity entity) {
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.removeModifier(MAD_SPEED_UUID);
        entity.removeEffect(ModEffects.MADNESS.get());
        entity.getPersistentData().remove(STACKS_KEY);
        MultiAttributeModifierHelper.removeAll(entity, STACK_ENTRIES);
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        if (entity.tickCount % 20 == 0) {
            if (!entity.hasEffect(ModEffects.MADNESS.get())) {
                entity.addEffect(new MobEffectInstance(ModEffects.MADNESS.get(), -1, 0, false, false, true));
            }
        }
    }

    @Override
    public void onKill(LivingEntity entity, LivingDeathEvent event) {
        entity.heal(4.0F);

        CompoundTag tag = entity.getPersistentData();
        int stacks = tag.getInt(STACKS_KEY) + 1;
        tag.putInt(STACKS_KEY, stacks);

        MultiAttributeModifierHelper.updateAll(entity, stacks, STACK_ENTRIES);
        BuffParticleHelper.sendParticles(entity, "creepy", 12);
    }
}
