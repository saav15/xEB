package org.xeb.xeb.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class MechaVulcanProjectileEntity extends ThrowableProjectile implements net.minecraft.world.entity.projectile.ItemSupplier {
    private int lifeTicks = 0;

    @Override
    public net.minecraft.world.item.ItemStack getItem() {
        return new net.minecraft.world.item.ItemStack(org.xeb.xeb.item.ModItems.MECHA_OVERDRIVE.get());
    }

    public MechaVulcanProjectileEntity(EntityType<? extends MechaVulcanProjectileEntity> type, Level level) {
        super(type, level);
    }

    public MechaVulcanProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.MECHA_VULCAN_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;
        if (lifeTicks > 60) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide()) {
            if (result.getEntity() instanceof LivingEntity target) {
                double dmg = org.xeb.xeb.Config.mechaVulcanDamage;
                target.hurt(this.damageSources().thrown(this, this.getOwner()), (float) dmg);
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
        return 0.0F; // Fired without gravity
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
