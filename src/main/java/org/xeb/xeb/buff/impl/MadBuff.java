package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraft.world.effect.MobEffectInstance;
import org.xeb.xeb.effect.ModEffects;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class MadBuff extends EliteBuff {
    private static final String STACKS_KEY = "xebMadStacks";
    private static final UUID MAD_SPEED_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab40");
    private static final UUID STACK_DAMAGE_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab41");
    private static final UUID STACK_ARMOR_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab42");
    private static final UUID STACK_HEALTH_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab43");
    private static final UUID STACK_SPEED_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab44");

    public MadBuff() {
        super("mad", "Mad", BuffType.ENEMY_ONLY, 0xB22222, 10.0D, false);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        // Double Attack Speed
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.addTransientModifier(new AttributeModifier(MAD_SPEED_UUID, "Mad Attack Speed modifier", 1.0D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        // Apply Madness Effect
        entity.addEffect(new MobEffectInstance(ModEffects.MADNESS.get(), -1, 0, false, false, true));
    }

    @Override
    public void onDetach(LivingEntity entity) {
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.removeModifier(MAD_SPEED_UUID);
        
        // Remove Madness Effect
        entity.removeEffect(ModEffects.MADNESS.get());
        
        CompoundTag tag = entity.getPersistentData();
        tag.remove(STACKS_KEY);
        removeStackModifiers(entity);
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

        // Update modifiers
        updateStackModifiers(entity, stacks);

        // Spawn particles
        if (!entity.level().isClientSide()) {
            BuffParticlePacket packet = new BuffParticlePacket(entity.getX(), entity.getY(), entity.getZ(), "creepy", 12); // villager happy green/anger particles
            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
        }
    }

    private void removeStackModifiers(LivingEntity entity) {
        AttributeInstance dmg = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.removeModifier(STACK_DAMAGE_UUID);

        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.removeModifier(STACK_ARMOR_UUID);

        AttributeInstance hp = entity.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.removeModifier(STACK_HEALTH_UUID);

        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(STACK_SPEED_UUID);
    }

    private void updateStackModifiers(LivingEntity entity, int stacks) {
        removeStackModifiers(entity);

        AttributeInstance dmg = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.addTransientModifier(new AttributeModifier(STACK_DAMAGE_UUID, "Mad Damage Stack", stacks * 1.0D, AttributeModifier.Operation.ADDITION));
        }

        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.addTransientModifier(new AttributeModifier(STACK_ARMOR_UUID, "Mad Armor Stack", stacks * 1.0D, AttributeModifier.Operation.ADDITION));
        }

        AttributeInstance hp = entity.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.addTransientModifier(new AttributeModifier(STACK_HEALTH_UUID, "Mad Health Stack", stacks * 1.0D, AttributeModifier.Operation.ADDITION));
        }

        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addTransientModifier(new AttributeModifier(STACK_SPEED_UUID, "Mad Speed Stack", stacks * 0.02D, AttributeModifier.Operation.ADDITION));
        }
    }
}
