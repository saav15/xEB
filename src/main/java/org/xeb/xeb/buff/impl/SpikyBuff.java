package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import java.util.UUID;

public class SpikyBuff extends EliteBuff {
    private static final String THORNS_KEY = "xebThornsAmount";
    private static final String SPIKY_COUNT_KEY = "xebSpikyCount";

    public SpikyBuff() {
        super("spiky", "Spiky", BuffType.UNIVERSAL, 0x8B4513, 2.0D, true);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onAttach(LivingEntity entity, UUID medallionId) {
        CompoundTag tag = entity.getPersistentData();
        int count = tag.contains(SPIKY_COUNT_KEY) ? tag.getInt(SPIKY_COUNT_KEY) : 0;
        count++;
        tag.putInt(SPIKY_COUNT_KEY, count);
        
        int baseAmount = MedallionManager.isBoss(entity) ? 1 : 2;
        tag.putInt(THORNS_KEY, baseAmount * count);
    }

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity, UUID medallionId) {
        CompoundTag tag = entity.getPersistentData();
        int count = tag.contains(SPIKY_COUNT_KEY) ? tag.getInt(SPIKY_COUNT_KEY) - 1 : 0;
        if (count <= 0) {
            tag.remove(THORNS_KEY);
            tag.remove(SPIKY_COUNT_KEY);
        } else {
            tag.putInt(SPIKY_COUNT_KEY, count);
            int baseAmount = MedallionManager.isBoss(entity) ? 1 : 2;
            tag.putInt(THORNS_KEY, baseAmount * count);
        }
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}

    @Override
    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity livingAttacker && attacker != entity) {
            // Check if damage source is direct melee / physical (e.g. not thorns itself to avoid loops!)
            if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
                CompoundTag data = entity.getPersistentData();
                int thornsAmount = data.contains(THORNS_KEY) ? data.getInt(THORNS_KEY) : 1;
                livingAttacker.hurt(entity.damageSources().thorns(entity), thornsAmount);
            }
        }
    }
}
