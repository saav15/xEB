package org.xeb.xeb.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.NbtUtils;
import java.util.ArrayList;
import java.util.List;

public class RestoreProjectileEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<BlockState> BLOCK_STATE = 
            SynchedEntityData.defineId(RestoreProjectileEntity.class, EntityDataSerializers.BLOCK_STATE);
            
    private static final EntityDataAccessor<Boolean> RETURNING = 
            SynchedEntityData.defineId(RestoreProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    private int lifeTicks = 0;
    private final List<Integer> hitOutList = new ArrayList<>();
    private final List<Integer> hitReturnList = new ArrayList<>();

    public RestoreProjectileEntity(EntityType<? extends RestoreProjectileEntity> type, Level level) {
        super(type, level);
    }

    public RestoreProjectileEntity(Level level, LivingEntity shooter, BlockState state) {
        super(ModEntities.RESTORE_PROJECTILE.get(), shooter, level);
        this.setBlockState(state);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(BLOCK_STATE, Blocks.COBBLESTONE.defaultBlockState());
        this.entityData.define(RETURNING, false);
    }

    public BlockState getBlockState() {
        return this.entityData.get(BLOCK_STATE);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(BLOCK_STATE, state);
    }

    public boolean isReturning() {
        return this.entityData.get(RETURNING);
    }

    public void setReturning(boolean returning) {
        this.entityData.set(RETURNING, returning);
    }

    @Override
    public void tick() {
        // Base entity ticking (for tracking/updating positions, etc. on client)
        if (this.level().isClientSide()) {
            super.tick();
            return;
        }

        LivingEntity owner = (LivingEntity) this.getOwner();
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();

        if (this.isReturning()) {
            // Return Phase: Fly towards the owner
            Vec3 ownerPos = owner.getEyePosition(1.0F).subtract(0.0D, 0.5D, 0.0D);
            Vec3 toOwner = ownerPos.subtract(this.position());
            double dist = toOwner.length();
            
            if (dist < 1.5D) {
                this.discard();
                return;
            } else {
                Vec3 dir = toOwner.normalize();
                this.setDeltaMovement(dir.scale(1.2D));
                this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
                this.hurtMarked = true;
            }

            // Damage & drag targets towards a landing spot 1.5 blocks in front of the owner
            Vec3 targetLandingPos = owner.position().add(owner.getLookAngle().scale(1.5D));
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.0D))) {
                if (entity != owner && entity.isAlive() && !entity.isAlliedTo(owner)) {
                    if (!hitReturnList.contains(entity.getId())) {
                        hitReturnList.add(entity.getId());
                        
                        // Deal 4.0 damage (return damage)
                        entity.hurt(owner.damageSources().indirectMagic(this, owner), 4.0F);
                        
                        // Pull towards landing spot
                        Vec3 pullVec = targetLandingPos.subtract(entity.position());
                        double pullDist = pullVec.length();
                        if (pullDist > 0.1D) {
                            entity.setDeltaMovement(pullVec.x * 0.35D, 0.3D, pullVec.z * 0.35D);
                            entity.hurtMarked = true;
                        }
                    }
                }
            }
        } else {
            // Outward Phase: Fly straight (piercing blocks and entities)
            this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
            this.hurtMarked = true;
            
            lifeTicks++;
            if (lifeTicks >= 20) { // 20 blocks total movement (since speed is 1.0 block per tick)
                this.setReturning(true);
            }

            // Damage area of 1 block (piercing)
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.0D))) {
                if (entity != owner && entity.isAlive() && !entity.isAlliedTo(owner)) {
                    if (!hitOutList.contains(entity.getId())) {
                        hitOutList.add(entity.getId());
                        
                        // Deal 5.0 damage
                        entity.hurt(owner.damageSources().indirectMagic(this, owner), 5.0F);
                    }
                }
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Ignore block collision to allow piercing blocks
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Ignore direct entity collision since scanning is handled in tick()
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("BlockState", NbtUtils.writeBlockState(this.getBlockState()));
        tag.putBoolean("Returning", this.isReturning());
        tag.putInt("LifeTicks", lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BlockState")) {
            this.setBlockState(NbtUtils.readBlockState(this.level().holderLookup(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("BlockState")));
        }
        this.setReturning(tag.getBoolean("Returning"));
        this.lifeTicks = tag.getInt("LifeTicks");
    }
}
