package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class BouncyBuff extends EliteBuff {
    public BouncyBuff() {
        super("bouncy", "Bouncy", BuffType.UNIVERSAL, 0xFF69B4, 1.0D);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {}

    @Override
    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity livingAttacker && attacker != entity) {
            Vec3 dir = entity.position().subtract(livingAttacker.position());
            if (dir.lengthSqr() > 0) {
                dir = dir.normalize();
                
                // Knockback entity away from attacker
                entity.knockback(1.2D, -dir.x, -dir.z);
                entity.hurtMarked = true;
                
                // Knockback attacker away from entity
                livingAttacker.knockback(1.2D, dir.x, dir.z);
                livingAttacker.hurtMarked = true;
            }
        }
    }
}
