package org.xeb.xeb.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class MechaEggmanMissileEntity extends ThrowableProjectile implements net.minecraft.world.entity.projectile.ItemSupplier {
    private int lifeTicks = 0;
    private boolean isLeftSide = false;

    public MechaEggmanMissileEntity(EntityType<? extends MechaEggmanMissileEntity> type, Level level) {
        super(type, level);
    }

    public MechaEggmanMissileEntity(Level level, LivingEntity shooter, boolean isLeftSide) {
        super(ModEntities.MECHA_EGGMAN_MISSILE.get(), shooter, level);
        this.isLeftSide = isLeftSide;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIREWORK_ROCKET);
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

        if (this.level() instanceof ServerLevel serverLevel) {
            // Sonic 3 Eggman missile trail: Dense black smoke and flame trail
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(),
                    2, 0.05D, 0.05D, 0.05D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(),
                    1, 0.02D, 0.02D, 0.02D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        explode();
    }

    private void explode() {
        if (!this.level().isClientSide()) {
            Level level = this.level();
            float dmg = 14.0F; // High impact Sonic 3 Eggman rocket damage

            level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.5D),
                    e -> e.isAlive() && e != this.getOwner()).forEach(target -> {
                target.hurt(this.damageSources().explosion(this, this.getOwner()), dmg);
            });

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.9F);
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putBoolean("IsLeftSide", isLeftSide);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        isLeftSide = tag.getBoolean("IsLeftSide");
    }
}
