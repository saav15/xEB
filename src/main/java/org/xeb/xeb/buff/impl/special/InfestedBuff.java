package org.xeb.xeb.buff.impl.special;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.entity.EliteFlyEntity;
import org.xeb.xeb.entity.ModEntities;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InfestedBuff extends EliteBuff {
    public InfestedBuff() {
        super("infested", "Infested", BuffType.ENEMY_ONLY, 0x556B2F, 5.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}

    @Override
    public void onDeath(LivingEntity entity, LivingDeathEvent event) {
        if (entity.level().isClientSide()) return;

        double hostDamage = 2.0D;
        if (entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null) {
            hostDamage = entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        }
        double flyDamage = Math.max(1.0D, hostDamage * 0.5D);

        int count = 3 + entity.getRandom().nextInt(3); // 3 to 5
        for (int i = 0; i < count; i++) {
            EliteFlyEntity fly = ModEntities.ELITE_FLY.get().create(entity.level());
            if (fly != null) {
                double ox = (entity.getRandom().nextDouble() - 0.5D) * 1.0D;
                double oy = entity.getRandom().nextDouble() * 0.5D + 0.5D;
                double oz = (entity.getRandom().nextDouble() - 0.5D) * 1.0D;
                
                fly.moveTo(entity.getX() + ox, entity.getY() + oy, entity.getZ() + oz, entity.getRandom().nextFloat() * 360.0F, 0.0F);
                
                fly.setHostType(net.minecraft.world.entity.EntityType.getKey(entity.getType()).getPath());
                
                if (fly.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null) {
                    fly.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(flyDamage);
                }

                // Copy all medallions from host to the fly, excluding infested to prevent an infinite crash loop
                List<MedallionData> hostMedallions = MedallionManager.getMedallions(entity);
                List<MedallionData> copied = new ArrayList<>();
                for (MedallionData m : hostMedallions) {
                    if (!m.getBuff().getId().equals(this.getId())) {
                        copied.add(new MedallionData(m.getBuff(), m.getTier(), UUID.randomUUID()));
                    }
                }
                if (!copied.isEmpty()) {
                    MedallionManager.saveMedallions(fly, copied);
                    for (MedallionData m : copied) {
                        m.getBuff().onAttach(fly);
                    }
                    MedallionManager.syncToTracking(fly);
                }
                
                entity.level().addFreshEntity(fly);
            }
        }
    }
}
