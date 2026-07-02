package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.mana.ManaManager;
import org.xeb.xeb.util.BuffParticleHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class AbsorbentBuff extends EliteBuff {
    public AbsorbentBuff() {
        super("absorbent", "Absorbent", BuffType.UNIVERSAL, 0x483D8B, 2.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        if (entity.tickCount % 20 == 0) { // check every second
            AABB aabb = entity.getBoundingBox().inflate(6.0D);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aabb);
            for (LivingEntity target : targets) {
                if (target != entity) {
                    // Check if target is not moving (velocity squared is very small)
                    boolean notMoving = target.getDeltaMovement().lengthSqr() < 0.01D;
                    boolean notUsingItem = !target.isUsingItem();

                    if (notMoving && notUsingItem) {
                        double toDrain = 1.0D + entity.getRandom().nextInt(2); // 1-2 mana
                        double current = ManaManager.getMana(target);
                        if (current > 0) {
                            double drained = Math.min(toDrain, current);
                            boolean success = ManaManager.drainMana(target, drained);
                            if (success || drained > 0) {
                                target.hurt(entity.damageSources().magic(), (float) drained);
                                
                                BuffParticleHelper.sendParticles(target.getX(), target.getY() + 0.5, target.getZ(), target, "tarred", 4);
                            }
                        }
                    }
                }
            }
        }
    }
}
