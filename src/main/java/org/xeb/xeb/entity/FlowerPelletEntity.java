package org.xeb.xeb.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class FlowerPelletEntity extends ThrowableProjectile {
    private int lifeTicks = 0;

    public FlowerPelletEntity(EntityType<? extends FlowerPelletEntity> type, Level level) {
        super(type, level);
    }

    public FlowerPelletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FLOWER_PELLET.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        if (lifeTicks > 100) { // 5 seconds lifetime
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide()) {
            if (result.getEntity() instanceof LivingEntity target) {
                target.hurt(this.damageSources().thrown(this, this.getOwner()), 2.0F); // 1 heart
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
        return 0.0F; // Travels straight, no gravity
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
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
