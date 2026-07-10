package org.xeb.xeb.buff.impl.combat;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class SandyBuff extends EliteBuff {
    public SandyBuff() {
        super("sandy", "Sandy", BuffType.ENEMY_ONLY, 0xF4A460, 2.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}

    @Override
    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {
        if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD) || event.getAmount() >= 1000.0F) {
            return;
        }
        if (entity.getRandom().nextFloat() < 0.10F) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            
            if (!entity.level().isClientSide()) {
                BuffParticlePacket packet = new BuffParticlePacket(entity.getX(), entity.getY(), entity.getZ(), "dodge", 6);
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
            }
        }
    }

    @Override
    public void onDeath(LivingEntity entity, LivingDeathEvent event) {
        if (entity.level().isClientSide()) return;

        AABB aabb = entity.getBoundingBox().inflate(5.0D);
        List<AreaEffectCloud> existingClouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, aabb,
                cloud -> cloud.getParticle().getType() == ParticleTypes.CAMPFIRE_COSY_SMOKE);

        if (!existingClouds.isEmpty()) {
            // Increment existing cloud sandstorm damage
            AreaEffectCloud existing = existingClouds.get(0);
            // Re-apply effect with higher amplifier
            existing.addEffect(new MobEffectInstance(ModEffects.SANDSTORM.get(), existing.getDuration(), 1));
        } else {
            // Spawn new sandstorm cloud
            AreaEffectCloud cloud = new AreaEffectCloud(entity.level(), entity.getX(), entity.getY(), entity.getZ());
            cloud.setOwner(entity);
            cloud.setRadius(5.0F);
            cloud.setDuration(400); // 20 seconds
            cloud.setParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE);
            cloud.addEffect(new MobEffectInstance(ModEffects.SANDSTORM.get(), 400, 0));
            entity.level().addFreshEntity(cloud);
        }
    }
}
