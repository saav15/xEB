package org.xeb.xeb.buff.impl.combat;
import java.util.Set;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class MirrorBuff extends EliteBuff {
    public MirrorBuff() {
        super("mirror", "Mirror", BuffType.ENEMY_ONLY, 0xC0C0C0, 10.0D, false);
    }

    private int getReflectAmplifier(LivingEntity entity) {
        MedallionType tier = MedallionType.COMMON;
        for (MedallionData m : MedallionManager.getMedallions(entity)) {
            if (m.getBuff().getId().equals(this.getId())) {
                tier = m.getTier();
                break;
            }
        }
        return switch (tier) {
            case COMMON -> 1;     // Reflect II (amplifier = 1)
            case RARE -> 3;       // Reflect IV (amplifier = 3)
            case LEGENDARY -> 5;  // Reflect VI (amplifier = 5)
        };
    }

    
    @Override
    public Set<String> getConflicts() {
        return Set.of("spiky","resonant");
    }
    @Override
    public void onAttach(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            int amp = getReflectAmplifier(entity);
            entity.addEffect(new MobEffectInstance(ModEffects.REFLECT.get(), -1, amp, false, false, true));
        }
    }

    @Override
    public void onDetach(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.removeEffect(ModEffects.REFLECT.get());
        }
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        // Refresh effect if missing or has the wrong amplifier
        int expectedAmp = getReflectAmplifier(entity);
        MobEffectInstance current = entity.getEffect(ModEffects.REFLECT.get());
        if (current == null || current.getAmplifier() != expectedAmp) {
            entity.addEffect(new MobEffectInstance(ModEffects.REFLECT.get(), -1, expectedAmp, false, false, true));
        }
    }
}
