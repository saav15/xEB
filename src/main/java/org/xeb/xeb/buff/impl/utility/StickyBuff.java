package org.xeb.xeb.buff.impl.utility;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.effect.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

public class StickyBuff extends EliteBuff {
    public StickyBuff() {
        super("sticky", "Sticky", BuffType.UNIVERSAL, 0x2E8B57, 2.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        // Apply tarred stack on contact (overlapping bounding boxes)
        if (entity.tickCount % 20 == 0) {
            AABB aabb = entity.getBoundingBox().inflate(0.2D);
            List<LivingEntity> collided = level.getEntitiesOfClass(LivingEntity.class, aabb);
            for (LivingEntity target : collided) {
                if (target != entity && target.getType() != entity.getType()) {
                    applyTarred(target);
                }
            }
        }
    }

    @Override
    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity livingAttacker && attacker != entity) {
            applyTarred(livingAttacker);
        }
    }

    private void applyTarred(LivingEntity target) {
        MobEffectInstance current = target.getEffect(ModEffects.TARRED.get());
        int amp = 0;
        if (current != null) {
            amp = Math.min(4, current.getAmplifier() + 1); // Max amp 4 (5 stacks)
        }
        target.addEffect(new MobEffectInstance(ModEffects.TARRED.get(), 100, amp)); // 5 seconds
    }
}
