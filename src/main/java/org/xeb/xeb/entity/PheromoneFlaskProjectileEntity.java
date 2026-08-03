package org.xeb.xeb.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.item.ModItems;

import java.util.List;

public class PheromoneFlaskProjectileEntity extends ThrowableItemProjectile {
    public PheromoneFlaskProjectileEntity(EntityType<? extends PheromoneFlaskProjectileEntity> type, Level level) {
        super(type, level);
    }

    public PheromoneFlaskProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.PHEROMONE_FLASK_PROJECTILE.get(), shooter, level);
    }

    public PheromoneFlaskProjectileEntity(Level level, double x, double y, double z) {
        super(ModEntities.PHEROMONE_FLASK_PROJECTILE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PHEROMONE_FLASK.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            Level level = this.level();
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            // Play glass splash sound
            level.playSound(null, x, y, z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);

            // Spawn Heart and Pink Potion particles in 3x3 area
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, x, y + 0.5, z, 35, 1.2, 0.8, 1.2, 0.1);
                serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT, x, y + 0.5, z, 25, 1.2, 0.8, 1.2, 0.5);
            }

            // 3x3 Area (1.5 radius) search
            AABB area = new AABB(x - 1.5, y - 1.5, z - 1.5, x + 1.5, y + 1.5, z + 1.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);
            LivingEntity shooter = this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null;

            for (LivingEntity target : targets) {
                // Self-Charm Restriction: Skip thrower
                if (shooter != null && target.getUUID().equals(shooter.getUUID())) {
                    continue;
                }

                // Apply Charmed effect for 15 seconds (300 ticks)
                target.addEffect(new MobEffectInstance(ModEffects.CHARMED.get(), 300, 0, false, true, true));

                // Save Owner NBT if shooter is valid
                if (shooter != null) {
                    target.getPersistentData().putUUID("xebCharmedOwner", shooter.getUUID());
                    target.getPersistentData().putString("xebCharmedOwnerName", shooter.getName().getString());
                }
            }

            this.discard();
        }
    }
}
