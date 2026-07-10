package org.xeb.xeb.buff.impl.combat;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public class ReactiveBuff extends EliteBuff {
    public ReactiveBuff() {
        super("reactive", "Reactive", BuffType.UNIVERSAL, 0xFF6347, 5.0D);
    }

    @Override
    public void onAttach(LivingEntity entity, UUID medallionId) {
        updateKineticSpikesEffect(entity);
    }

    @Override
    public void onDetach(LivingEntity entity, UUID medallionId) {
        updateKineticSpikesEffect(entity);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        updateKineticSpikesEffect(entity);
    }

    @Override
    public void onDetach(LivingEntity entity) {
        updateKineticSpikesEffect(entity);
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        if (entity.tickCount % 20 == 0) {
            updateKineticSpikesEffect(entity);
        }
    }

    private void updateKineticSpikesEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        int highestAmp = getHighestKineticSpikesAmplifier(entity);
        if (highestAmp >= 0) {
            net.minecraft.world.effect.MobEffectInstance current = entity.getEffect(org.xeb.xeb.effect.ModEffects.KINETIC_SPIKES.get());
            if (current == null || current.getAmplifier() != highestAmp) {
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        org.xeb.xeb.effect.ModEffects.KINETIC_SPIKES.get(), -1, highestAmp, false, false, true));
            }
        } else {
            entity.removeEffect(org.xeb.xeb.effect.ModEffects.KINETIC_SPIKES.get());
        }
    }

    private int getHighestKineticSpikesAmplifier(LivingEntity entity) {
        int highestAmp = -1;
        for (org.xeb.xeb.medallion.MedallionData m : org.xeb.xeb.medallion.MedallionManager.getMedallions(entity)) {
            if (m.getBuff().getId().equals("reactive")) {
                int amp = switch (m.getTier()) {
                    case COMMON -> 0;      // Bronze -> Kinetic Spikes 1 (amplifier 0)
                    case RARE -> 1;        // Silver -> Kinetic Spikes 2 (amplifier 1)
                    case LEGENDARY -> 3;   // Gold -> Kinetic Spikes 4 (amplifier 3)
                };
                if (amp > highestAmp) {
                    highestAmp = amp;
                }
            }
        }
        return highestAmp;
    }
}
