package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.effect.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class HardyBuff extends EliteBuff {
    public HardyBuff() {
        super("hardy", "Hardy", BuffType.UNIVERSAL, 0xB8860B, 2.0D, false);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        // Clear freeze state
        if (entity.getTicksFrozen() > 0) {
            entity.setTicksFrozen(0);
        }

        // Clear petrify effect
        if (entity.hasEffect(ModEffects.PETRIFY.get())) {
            entity.removeEffect(ModEffects.PETRIFY.get());
        }

        // Clear slowness/weakness if they count as crowd control
        // But mainly freeze and petrify are blocked.
    }
}
