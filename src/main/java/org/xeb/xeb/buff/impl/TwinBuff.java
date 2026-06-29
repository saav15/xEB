package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class TwinBuff extends EliteBuff {
    private static final String IS_TWIN_KEY = "xebIsTwin";
    private static final UUID TWIN_HEALTH_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08ab50");

    public TwinBuff() {
        super("twin", "Twin", BuffType.UNIVERSAL, 0x9400D3, 1.0D, false);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        // Halve max health
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            if (maxHealth.getModifier(TWIN_HEALTH_UUID) == null) {
                maxHealth.addTransientModifier(new AttributeModifier(TWIN_HEALTH_UUID, "Twin Health Penalty", -0.5D, AttributeModifier.Operation.MULTIPLY_BASE));
            }
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }

        CompoundTag tag = entity.getPersistentData();
        if (!tag.getBoolean(IS_TWIN_KEY)) {
            // This is the original mob. Spawn the twin!
            if (!entity.level().isClientSide()) {
                LivingEntity twin = (LivingEntity) entity.getType().create(entity.level());
                if (twin != null) {
                    twin.getPersistentData().putBoolean(IS_TWIN_KEY, true);
                    twin.moveTo(entity.getX() + 1.5D, entity.getY(), entity.getZ() + 1.5D, entity.getYRot(), entity.getXRot());
                    // Pre-set empty medallions list so EntityJoinLevelEvent doesn't roll random ones
                    MedallionManager.saveMedallions(twin, java.util.Collections.emptyList());

                    if (twin instanceof net.minecraft.world.entity.Mob mobTwin) {
                        mobTwin.finalizeSpawn((net.minecraft.world.level.ServerLevelAccessor) entity.level(), entity.level().getCurrentDifficultyAt(twin.blockPosition()), net.minecraft.world.entity.MobSpawnType.TRIGGERED, null, null);
                    }

                    entity.level().addFreshEntity(twin);
                    
                    // Copy medallions (this triggers copyMedallions which copies NBT and invokes attach on all copied buffs)
                    MedallionManager.copyMedallions(entity, twin);
                }
            }
        }
    }

    @Override
    public void onDetach(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(TWIN_HEALTH_UUID);
        }
        entity.getPersistentData().remove(IS_TWIN_KEY);
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}
}
