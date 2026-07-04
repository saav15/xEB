package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class CreepyBuff extends EliteBuff {
    public CreepyBuff() {
        super("creepy", "Creepy", BuffType.UNIVERSAL, 0x32CD32, 2.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        // Apply poison to nearby entities standing near feet
        if (entity.tickCount % 10 == 0) {
            AABB aabb = entity.getBoundingBox().inflate(1.0D, 0.5D, 1.0D);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aabb);
            for (LivingEntity target : targets) {
                if (target != entity) {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
                }
            }
        }

        // Spawn particles
        if (entity.tickCount % 5 == 0) {
            BuffParticlePacket packet = new BuffParticlePacket(entity.getX(), entity.getY(), entity.getZ(), "creepy", 1);
            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
        }
    }

    @Override
    public void onDamageDealt(LivingEntity entity, LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1)); // Poison II
        }
    }
}
