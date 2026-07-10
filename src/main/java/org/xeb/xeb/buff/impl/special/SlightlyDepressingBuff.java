package org.xeb.xeb.buff.impl.special;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SlightlyDepressingBuff extends EliteBuff {
    public SlightlyDepressingBuff() {
        super("slightly_depressing", "Slightly Depressing", BuffType.UNIVERSAL, 0x708090, 1.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        if (entity.tickCount % 10 != 0) return;

        // Find amplifier based on medallion quality
        int amplifier = 0;
        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        for (MedallionData m : medallions) {
            if (m.getBuff().getId().equals("slightly_depressing")) {
                amplifier = switch (m.getTier()) {
                    case COMMON -> 0;
                    case RARE -> 1;
                    case LEGENDARY -> 2;
                };
                break;
            }
        }

        AABB aabb = entity.getBoundingBox().inflate(2.0D); // Slightly Depressing applies at 2 blocks range
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aabb);

        for (LivingEntity target : targets) {
            if (target != entity) {
                // Apply/refresh All Stats Down for 220 ticks (11 seconds) to prevent GUI flashing (Minecraft threshold is 10s)
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        ModEffects.ALL_STATS_DOWN.get(), 220, amplifier, false, true, true
                ));
            }
        }
    }
}
