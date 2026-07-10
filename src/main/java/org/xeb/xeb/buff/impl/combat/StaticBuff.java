package org.xeb.xeb.buff.impl.combat;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

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
        // N6 — throttle position-change detection to every 5 ticks (250 ms).
        // The shock effect triggers on movement which is a continuous signal;
        // checking every tick produced 20 getEntitiesOfClass scans/s per Static mob
        // with no perceptible gameplay benefit. 5 ticks is still responsive enough
        // for the chain lightning effect to feel immediate.
        if (entity.tickCount % 5 != 0) return;

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
                    // N5 — use vanilla sendParticles instead of a custom network packet.
                    level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        5, 0.3, 0.3, 0.3, 0.05
                    );
                }
            }
        }

        // Save current position
        tag.putDouble(PREV_X, entity.getX());
        tag.putDouble(PREV_Y, entity.getY());
        tag.putDouble(PREV_Z, entity.getZ());
    }
}
