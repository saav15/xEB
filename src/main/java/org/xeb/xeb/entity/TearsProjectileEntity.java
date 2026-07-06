package org.xeb.xeb.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TearsProjectileEntity extends ThrowableProjectile {
    public static final int IMBUE_NONE = 0;
    public static final int IMBUE_PURPLE = 1;
    public static final int IMBUE_WHITE = 2;
    public static final int IMBUE_DARK = 3;
    public static final int IMBUE_COLD = 4;

    private static final EntityDataAccessor<Integer> IMBUE_TYPE = SynchedEntityData.defineId(TearsProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_BACKSTAB = SynchedEntityData.defineId(TearsProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    private int lifeTicks = 0;

    public TearsProjectileEntity(EntityType<? extends TearsProjectileEntity> type, Level level) {
        super(type, level);
    }

    public TearsProjectileEntity(Level level, LivingEntity shooter, int imbueType, boolean isBackstab) {
        super(ModEntities.TEARS_PROJECTILE.get(), shooter, level);
        this.entityData.set(IMBUE_TYPE, imbueType);
        this.entityData.set(IS_BACKSTAB, isBackstab);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(IMBUE_TYPE, IMBUE_NONE);
        this.entityData.define(IS_BACKSTAB, false);
    }

    public int getImbueType() {
        return this.entityData.get(IMBUE_TYPE);
    }

    public boolean isBackstab() {
        return this.entityData.get(IS_BACKSTAB);
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        if (lifeTicks > 100) { // 5 seconds lifetime
            this.discard();
            return;
        }

        // Homing logic for purple element
        if (getImbueType() == IMBUE_PURPLE) {
            LivingEntity target = findNearestTarget();
            if (target != null) {
                Vec3 targetPos = target.getBoundingBox().getCenter();
                Vec3 dir = targetPos.subtract(this.position()).normalize();
                Vec3 currentVelocity = this.getDeltaMovement();
                double speed = currentVelocity.length();
                if (speed < 0.05D) speed = 0.8D;

                // snappier curve towards target
                Vec3 newVelocity = currentVelocity.scale(0.8D).add(dir.scale(speed * 0.2D)).normalize().scale(speed);
                this.setDeltaMovement(newVelocity);
            }
        }
    }

    private LivingEntity findNearestTarget() {
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(15.0D),
                e -> e instanceof LivingEntity && e.isAlive() && e != this.getOwner() && !e.isSpectator() && e.isPickable() && !(e instanceof CrazyDiamondEntity));
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double dist = this.distanceToSqr(e);
            if (dist < closestDist) {
                closestDist = dist;
                closest = e;
            }
        }
        return closest;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide()) {
            if (result.getEntity() instanceof LivingEntity target) {
                float damage = 6.0F;
                if (isBackstab()) {
                    damage *= 1.2F; // +20% damage for backstab
                }

                target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);

                // Apply element status effects
                int imbue = getImbueType();
                if (imbue == IMBUE_WHITE) {
                    if (this.random.nextFloat() <= 0.2F) {
                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.level());
                        if (lightning != null) {
                            lightning.moveTo(target.position());
                            this.level().addFreshEntity(lightning);
                        }
                    }
                } else if (imbue == IMBUE_DARK) {
                    // Tarred & Fear
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                } else if (imbue == IMBUE_COLD) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3)); // Slowness IV
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
        return 0.0F; // Tears don't drop in a parabola
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putInt("ImbueType", getImbueType());
        tag.putBoolean("IsBackstab", isBackstab());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lifeTicks = tag.getInt("LifeTicks");
        this.entityData.set(IMBUE_TYPE, tag.getInt("ImbueType"));
        this.entityData.set(IS_BACKSTAB, tag.getBoolean("IsBackstab"));
    }
}
