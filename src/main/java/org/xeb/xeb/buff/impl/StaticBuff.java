package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class StaticBuff extends EliteBuff {
    private static final String PREV_X = "xebPrevX";
    private static final String PREV_Y = "xebPrevY";
    private static final String PREV_Z = "xebPrevZ";

    public StaticBuff() {
        super("static", "Static", BuffType.UNIVERSAL, 0xFFFF00, 2.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        tag.putDouble(PREV_X, entity.getX());
        tag.putDouble(PREV_Y, entity.getY());
        tag.putDouble(PREV_Z, entity.getZ());
    }

    @Override
    public void onDetach(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        tag.remove(PREV_X);
        tag.remove(PREV_Y);
        tag.remove(PREV_Z);
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        CompoundTag tag = entity.getPersistentData();
        if (tag.contains(PREV_X)) {
            double prevX = tag.getDouble(PREV_X);
            double prevY = tag.getDouble(PREV_Y);
            double prevZ = tag.getDouble(PREV_Z);

            double distSq = entity.distanceToSqr(prevX, prevY, prevZ);
            if (distSq > 0.04D) { // Moved more than 0.2 blocks
                // Shock nearby
                AABB aabb = entity.getBoundingBox().inflate(2.0D);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aabb);
                boolean shockedAny = false;
                for (LivingEntity target : targets) {
                    if (target != entity) {
                        target.hurt(entity.damageSources().lightningBolt(), 1.0F);
                        shockedAny = true;
                    }
                }
                
                if (shockedAny) {
                    BuffParticlePacket packet = new BuffParticlePacket(entity.getX(), entity.getY(), entity.getZ(), "static", 5);
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
                }
            }
        }

        // Save current position
        tag.putDouble(PREV_X, entity.getX());
        tag.putDouble(PREV_Y, entity.getY());
        tag.putDouble(PREV_Z, entity.getZ());
    }
}
