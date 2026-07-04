package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.network.PacketDistributor;

public class FlamingBuff extends EliteBuff {
    public FlamingBuff() {
        super("flaming", "Flaming", BuffType.UNIVERSAL, 0xFF4500, 2.0D, false);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false, false));
    }

    @Override
    public void onDetach(LivingEntity entity) {
        entity.removeEffect(MobEffects.FIRE_RESISTANCE);
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        // Refresh Fire Resistance
        if (!entity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false, false));
        }

        // Leave fire trail
        if (entity.tickCount % 5 == 0) {
            BlockPos pos = entity.blockPosition();
            if (level.isEmptyBlock(pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }

        // Spawn particles
        if (entity.tickCount % 3 == 0) {
            BuffParticlePacket packet = new BuffParticlePacket(entity.getX(), entity.getY(), entity.getZ(), "flame", 1);
            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
        }
    }

    @Override
    public void onDamageDealt(LivingEntity entity, LivingHurtEvent event) {
        if (MedallionManager.isBoss(entity)) {
            LivingEntity target = event.getEntity();
            if (target != null) {
                target.addEffect(new MobEffectInstance(ModEffects.BURN.get(), 60, 0));
            }
        }
    }
}
