package org.xeb.xeb.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * A small, fast laser projectile fired by left-clicking with the Optic Blast.
 * Spawns at eye level, travels at high speed, deals 3 damage on entity hit.
 */
public class MiniLaserProjectileEntity extends ThrowableProjectile {

    private int lifeTicks = 0;

    public MiniLaserProjectileEntity(EntityType<? extends MiniLaserProjectileEntity> type, Level level) {
        super(type, level);
    }

    public MiniLaserProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.MINI_LASER.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {
        // No extra synched data needed
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        // Max lifetime 40 ticks (2 seconds)
        if (lifeTicks > 40) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide()) {
            if (result.getEntity() instanceof LivingEntity target) {
                target.hurt(this.damageSources().thrown(this, this.getOwner()), 3.0F);
            }
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    protected float getGravity() {
        return 0.0F; // No gravity — laser travels straight
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
