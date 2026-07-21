package org.xeb.xeb.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class HomingMissileEntity extends ThrowableProjectile implements net.minecraft.world.entity.projectile.ItemSupplier {
    private int lifeTicks = 0;
    private LivingEntity target;

    @Override
    public net.minecraft.world.item.ItemStack getItem() {
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET);
    }

    public HomingMissileEntity(EntityType<? extends HomingMissileEntity> type, Level level) {
        super(type, level);
    }

    public HomingMissileEntity(Level level, LivingEntity shooter, LivingEntity target) {
        super(ModEntities.HOMING_MISSILE.get(), shooter, level);
        this.target = target;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        if (lifeTicks > 100) {
            this.discard();
            return;
        }

        // Homing behavior
        if (target != null && target.isAlive()) {
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
            Vec3 dir = targetCenter.subtract(this.position()).normalize();
            double speed = 0.8D;
            this.setDeltaMovement(dir.scale(speed));
            
            // Check distance
            if (this.position().distanceToSqr(targetCenter) < 1.0D) {
                explode(target);
                return;
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, this.getX(), this.getY(), this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity living) {
            explode(living);
        } else {
            explode(null);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        explode(null);
    }

    private void explode(LivingEntity hitTarget) {
        if (!this.level().isClientSide()) {
            Level level = this.level();
            double dmg = org.xeb.xeb.Config.homingMissileDamage;
            
            if (hitTarget != null) {
                hitTarget.hurt(this.damageSources().thrown(this, this.getOwner()), (float) dmg);
            } else if (target != null && target.isAlive() && this.position().distanceToSqr(target.position()) < 4.0D) {
                target.hurt(this.damageSources().thrown(this, this.getOwner()), (float) dmg);
            }

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.4F);
            this.discard();
        }
    }

    @Override
    protected float getGravity() {
        return 0.0F; // Fired straight (using manual vectors)
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
    }
}
