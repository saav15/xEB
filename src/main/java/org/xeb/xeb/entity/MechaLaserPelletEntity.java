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

public class MechaLaserPelletEntity extends ThrowableProjectile implements net.minecraft.world.entity.projectile.ItemSupplier {
    private int lifeTicks = 0;
    private int bounces = 0;
    private static final int MAX_BOUNCES = 1;
    private Vec3 spawnPos = Vec3.ZERO;

    @Override
    public net.minecraft.world.item.ItemStack getItem() {
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIRE_CHARGE);
    }

    public MechaLaserPelletEntity(EntityType<? extends MechaLaserPelletEntity> type, Level level) {
        super(type, level);
    }

    public MechaLaserPelletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.MECHA_LASER_PELLET.get(), shooter, level);
        this.spawnPos = shooter.position();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        if (spawnPos.equals(Vec3.ZERO)) {
            spawnPos = this.position();
        }

        if (lifeTicks > 60) { // Discard after 3 seconds
            this.discard();
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            // Bright red/cyan laser particle trail
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(),
                    1, 0.02D, 0.02D, 0.02D, 0.05D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide()) {
            if (result.getEntity() instanceof LivingEntity living && living != this.getOwner()) {
                double distFromSpawn = this.position().distanceTo(spawnPos);
                // Damage effectiveness drops off past 3 blocks
                float damage = 8.0F;
                if (distFromSpawn > 3.0D) {
                    float dropoff = (float) Math.max(0.2D, 1.0D - ((distFromSpawn - 3.0D) * 0.15D));
                    damage *= dropoff;
                }
                
                living.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
                living.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.8F);
                this.discard();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            if (bounces < MAX_BOUNCES) {
                bounces++;
                // Calculate bounce vector off the hit normal face
                Vec3 motion = this.getDeltaMovement();
                Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
                Vec3 bounced = motion.subtract(normal.scale(2.0D * motion.dot(normal))).scale(0.85D);
                this.setDeltaMovement(bounced);
                
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.4F, 2.0F);
            } else {
                this.discard();
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putInt("Bounces", bounces);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        bounces = tag.getInt("Bounces");
    }
}
