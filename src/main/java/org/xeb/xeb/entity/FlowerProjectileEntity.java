package org.xeb.xeb.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FlowerProjectileEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(FlowerProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR_INDEX = SynchedEntityData.defineId(FlowerProjectileEntity.class, EntityDataSerializers.INT);

    private int lifeTicks = 0;

    public FlowerProjectileEntity(EntityType<? extends FlowerProjectileEntity> type, Level level) {
        super(type, level);
    }

    public FlowerProjectileEntity(Level level, LivingEntity shooter, int colorIndex) {
        super(ModEntities.FLOWER_PROJECTILE.get(), shooter, level);
        this.entityData.set(COLOR_INDEX, colorIndex);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(COLOR_INDEX, 0);
    }

    public void setTarget(LivingEntity target) {
        this.entityData.set(TARGET_ID, target != null ? target.getId() : -1);
    }

    public int getColorIndex() {
        return this.entityData.get(COLOR_INDEX);
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        if (lifeTicks > 120) { // 6 seconds lifetime
            this.discard();
            return;
        }

        // Homing Logic
        int targetId = this.entityData.get(TARGET_ID);
        LivingEntity target = null;
        if (targetId != -1) {
            Entity ent = this.level().getEntity(targetId);
            if (ent instanceof LivingEntity living && living.isAlive()) {
                target = living;
            }
        }

        // If no target, try to find one nearby on the server side
        if (target == null && !this.level().isClientSide()) {
            List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(16.0D),
                    e -> e instanceof LivingEntity && e.isAlive() && e != this.getOwner() && !e.isSpectator() && e.isPickable());
            if (!list.isEmpty()) {
                // Find closest
                LivingEntity closest = null;
                double closestDist = Double.MAX_VALUE;
                for (LivingEntity e : list) {
                    double dist = this.distanceToSqr(e);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = e;
                    }
                }
                if (closest != null) {
                    this.setTarget(closest);
                    target = closest;
                }
            }
        }

        if (target != null) {
            Vec3 targetPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2.0D, target.getZ());
            Vec3 currentPos = this.position();
            Vec3 dir = targetPos.subtract(currentPos).normalize();
            Vec3 currentVelocity = this.getDeltaMovement();
            double speed = currentVelocity.length();
            if (speed < 0.05D) speed = 0.35D; // Initial speed fallback

            // Accelerate speed over time up to 1.2 blocks/tick (24 blocks/second) for snappy homing
            double newSpeed = Math.min(1.20D, speed + 0.035D);

            // Snappy interpolation towards target direction
            Vec3 newVelocity = currentVelocity.scale(0.70D).add(dir.scale(newSpeed * 0.30D)).normalize().scale(newSpeed);
            this.setDeltaMovement(newVelocity);
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide()) {
            if (result.getEntity() instanceof LivingEntity target) {
                target.hurt(this.damageSources().thrown(this, this.getOwner()), 6.0F);

                // If target dies and projectile lifetime is not over, search for another target in the vicinity
                if (!target.isAlive()) {
                    List<LivingEntity> potential = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(16.0D),
                            e -> e instanceof LivingEntity && e.isAlive() && e != this.getOwner() && e != target && !e.isSpectator() && e.isPickable());
                    if (!potential.isEmpty()) {
                        potential.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));
                        this.setTarget(potential.get(0));
                        return; // Do not discard projectile
                    }
                }
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
        return 0.0F; // Homing projectile, no gravity
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putInt("ColorIndex", getColorIndex());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        this.entityData.set(COLOR_INDEX, tag.getInt("ColorIndex"));
    }
}
