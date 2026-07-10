package org.xeb.xeb.buff.impl.utility;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.util.DodgeHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class LuckyBuff extends EliteBuff {
    public LuckyBuff() {
        super("lucky", "Lucky", BuffType.UNIVERSAL, 0xFFD700, 1.0D, true);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onAttach(LivingEntity entity, UUID medallionId) {
        AttributeInstance instance = entity.getAttribute(Attributes.LUCK);
        if (instance != null) {
            AttributeModifier modifier = new AttributeModifier(medallionId, "Lucky Buff Modifier", 3.0D, AttributeModifier.Operation.ADDITION);
            instance.addTransientModifier(modifier);
        }
    }

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity, UUID medallionId) {
        AttributeInstance instance = entity.getAttribute(Attributes.LUCK);
        if (instance != null) {
            instance.removeModifier(medallionId);
        }
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        if (entity.tickCount % 10 == 0) {
            BuffParticlePacket packet = new BuffParticlePacket(entity.getX(), entity.getY(), entity.getZ(), "crit", 1);
            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
        }
    }

    @Override
    public void onDamageDealt(LivingEntity entity, LivingHurtEvent event) {
        if (entity.getRandom().nextFloat() < 0.10F) {
            event.setAmount(event.getAmount() * 2.0F);
            LivingEntity target = event.getEntity();
            if (target != null && !entity.level().isClientSide()) {
                BuffParticlePacket packet = new BuffParticlePacket(target.getX(), target.getY(), target.getZ(), "crit", 8);
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
            }
        }
    }

    @Override
    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {
        // Skip void / instakill damage
        if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                || event.getAmount() >= 1000.0F) {
            return;
        }
        // 10% flat dodge chance — cancels damage AND all secondary effects
        if (entity.getRandom().nextFloat() < 0.10F) {
            DodgeHelper.triggerDodge(entity, event);
        }
    }

    @Override
    public void onProjectileImpact(LivingEntity entity, ProjectileImpactEvent event) {
        // Same 10% dodge applies to projectiles — cancels the impact entirely
        if (entity.getRandom().nextFloat() < 0.10F) {
            DodgeHelper.triggerDodge(entity, event);
        }
    }
}
