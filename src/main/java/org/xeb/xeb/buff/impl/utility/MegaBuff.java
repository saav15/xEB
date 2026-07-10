package org.xeb.xeb.buff.impl.utility;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class MegaBuff extends EliteBuff {
    // Each Mega medallion uses its own UUID — stacking gives cumulative +50% per medallion
    public MegaBuff() {
        super("mega", "Mega", BuffType.ENEMY_ONLY, 0xFF1493, 10.0D, true);
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onAttach(LivingEntity entity, UUID medallionId) {
        // Derive unique UUIDs per-attribute from the medallion UUID
        UUID healthUUID = new UUID(medallionId.getMostSignificantBits() ^ 0x1L, medallionId.getLeastSignificantBits());
        UUID damageUUID = new UUID(medallionId.getMostSignificantBits() ^ 0x2L, medallionId.getLeastSignificantBits());
        UUID speedUUID  = new UUID(medallionId.getMostSignificantBits() ^ 0x3L, medallionId.getLeastSignificantBits());

        // +50% Max Health
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            if (maxHealth.getModifier(healthUUID) == null) {
                maxHealth.addTransientModifier(new AttributeModifier(healthUUID, "Mega Health modifier", 0.5D, AttributeModifier.Operation.MULTIPLY_BASE));
                entity.heal((float) (entity.getMaxHealth() * 0.5));
            }
        }

        // +50% Damage
        AttributeInstance attackDmg = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDmg != null) {
            if (attackDmg.getModifier(damageUUID) == null) {
                attackDmg.addTransientModifier(new AttributeModifier(damageUUID, "Mega Damage modifier", 0.5D, AttributeModifier.Operation.MULTIPLY_BASE));
            }
        }

        // +50% Attack Speed
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            if (attackSpeed.getModifier(speedUUID) == null) {
                attackSpeed.addTransientModifier(new AttributeModifier(speedUUID, "Mega Attack Speed modifier", 0.5D, AttributeModifier.Operation.MULTIPLY_BASE));
            }
        }

        // Force hitbox recalculation
        entity.refreshDimensions();
    }

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity, UUID medallionId) {
        UUID healthUUID = new UUID(medallionId.getMostSignificantBits() ^ 0x1L, medallionId.getLeastSignificantBits());
        UUID damageUUID = new UUID(medallionId.getMostSignificantBits() ^ 0x2L, medallionId.getLeastSignificantBits());
        UUID speedUUID  = new UUID(medallionId.getMostSignificantBits() ^ 0x3L, medallionId.getLeastSignificantBits());

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.removeModifier(healthUUID);

        AttributeInstance attackDmg = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDmg != null) attackDmg.removeModifier(damageUUID);

        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.removeModifier(speedUUID);

        // Force hitbox recalculation
        entity.refreshDimensions();
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        // N5 — use vanilla sendParticles (no custom packet overhead per tick per mob)
        if (entity.tickCount % 8 == 0) {
            level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                entity.getX(), entity.getY() + 0.5, entity.getZ(),
                1,
                0.25, 0.25, 0.25,
                0.02
            );
        }
    }
}
