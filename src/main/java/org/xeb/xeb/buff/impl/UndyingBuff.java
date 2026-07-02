package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.util.BuffParticleHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;

public class UndyingBuff extends EliteBuff {
    private static final String REVIVED_KEY = "xebRevived";
    private static final String REMOVE_TIMER_KEY = "xebUndyingRemoveTimer";

    public UndyingBuff() {
        super("undying", "Undying", BuffType.ENEMY_ONLY, 0x8B0000, 1.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        entity.getPersistentData().putBoolean(REVIVED_KEY, false);
    }

    @Override
    public void onDetach(LivingEntity entity) {
        entity.getPersistentData().remove(REVIVED_KEY);
        entity.getPersistentData().remove(REMOVE_TIMER_KEY);
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        CompoundTag tag = entity.getPersistentData();
        if (tag.contains(REMOVE_TIMER_KEY)) {
            int ticks = tag.getInt(REMOVE_TIMER_KEY);
            if (ticks > 0) {
                tag.putInt(REMOVE_TIMER_KEY, ticks - 1);
            } else {
                tag.remove(REMOVE_TIMER_KEY);
                // Remove undying buff medallion
                removeUndyingMedallion(entity);
            }
        }
    }

    @Override
    public void onDeath(LivingEntity entity, LivingDeathEvent event) {
        if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            return;
        }
        CompoundTag tag = entity.getPersistentData();
        boolean revived = tag.getBoolean(REVIVED_KEY);
        if (!revived) {
            tag.putBoolean(REVIVED_KEY, true);
            event.setCanceled(true);

            entity.setHealth((float) (entity.getMaxHealth() * 0.5));
            tag.putInt(REMOVE_TIMER_KEY, 40); // 2 seconds

            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 1.0F);

            BuffParticleHelper.sendParticles(entity, "revival", 30);
        }
    }

    private void removeUndyingMedallion(LivingEntity entity) {
        List<MedallionData> list = MedallionManager.getMedallions(entity);
        List<MedallionData> toKeep = new ArrayList<>();
        for (MedallionData m : list) {
            if (!m.getBuff().getId().equals(this.getId())) {
                toKeep.add(m);
            } else {
                m.getBuff().onDetach(entity);
            }
        }
        MedallionManager.saveMedallions(entity, toKeep);
        MedallionManager.syncToTracking(entity);
    }
}
