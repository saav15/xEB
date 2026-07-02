package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import org.xeb.xeb.util.BuffParticleHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EvolvingBuff extends EliteBuff {
    private static final String TICKS_KEY = "xebEvolvingTicks";

    public EvolvingBuff() {
        super("evolving", "Evolving", BuffType.ENEMY_ONLY, 0x7B68EE, 1.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        entity.getPersistentData().putInt(TICKS_KEY, 0);
    }

    @Override
    public void onDetach(LivingEntity entity) {
        entity.getPersistentData().remove(TICKS_KEY);
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        CompoundTag tag = entity.getPersistentData();
        int ticks = tag.getInt(TICKS_KEY) + 1;
        
        if (ticks >= 600) { // 30 seconds (600 ticks)
            ticks = 0;

            List<MedallionData> current = MedallionManager.getMedallions(entity);
            if (current.size() < 5) { // Cap at 5 total medallions
                List<String> excludeIds = new ArrayList<>();
                excludeIds.add(this.getId());
                for (MedallionData m : current) {
                    excludeIds.add(m.getBuff().getId());
                }

                EliteBuff newBuff = EliteBuffRegistry.getRandomByWeight(entity.getRandom(), MedallionManager.isBoss(entity), excludeIds);
                if (newBuff != null) {
                    MedallionManager.attachMedallion(entity, new MedallionData(newBuff, MedallionType.COMMON, UUID.randomUUID()));

                    BuffParticleHelper.sendParticles(entity, "evolve", 15);
                }
            }
        }
        tag.putInt(TICKS_KEY, ticks);
    }
}
