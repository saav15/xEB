package org.xeb.xeb.buff.impl.combat;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

public class PlowBuff extends EliteBuff {
    public PlowBuff() {
        super("plow", "Plow", BuffType.ENEMY_ONLY, 0xD2691E, 1.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        // If moving, deal 1 trample damage to colliding non-allied entities
        Vec3 vel = entity.getDeltaMovement();
        if (vel.horizontalDistanceSqr() > 0.005D) {
            AABB aabb = entity.getBoundingBox();
            List<LivingEntity> collided = level.getEntitiesOfClass(LivingEntity.class, aabb);
            for (LivingEntity target : collided) {
                if (target != entity && target.getType() != entity.getType()) {
                    target.hurt(entity.damageSources().mobAttack(entity), 1.0F);
                }
            }
        }
    }

    @Override
    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity && attacker != entity) {
            Vec3 dir = attacker.position().subtract(entity.position());
            if (dir.lengthSqr() > 0) {
                dir = dir.normalize();
                
                // Move 1 block towards attacker
                double targetX = entity.getX() + dir.x;
                double targetY = entity.getY();
                double targetZ = entity.getZ() + dir.z;
                
                entity.setPos(targetX, targetY, targetZ);
                entity.hurtMarked = true;
            }
        }
    }
}
