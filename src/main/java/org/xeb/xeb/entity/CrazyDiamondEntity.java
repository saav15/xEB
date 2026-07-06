package org.xeb.xeb.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.xeb.xeb.item.ModItems;
import java.util.Optional;
import java.util.UUID;

public class CrazyDiamondEntity extends PathfinderMob {
    public static final int STATE_IDLE = 0;
    public static final int STATE_PUNCHING = 1;
    public static final int STATE_CHARGING = 2;
    public static final int STATE_BARRAGE = 3;
    public static final int STATE_EXHAUSTION = 4;
    public static final int STATE_DASHING = 5;
    public static final int STATE_LOW_PUNCH = 6;
    public static final int STATE_FACE_PUNCH = 7;
    public static final int STATE_KICKING = 8;
    public static final int STATE_DIGGING = 9;
    public static final int STATE_WIND_UP = 10;
    public static final int STATE_THROWING = 11;
    public static final int STATE_REELING_IN = 12;

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = 
            SynchedEntityData.defineId(CrazyDiamondEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final EntityDataAccessor<Integer> ANIM_STATE = 
            SynchedEntityData.defineId(CrazyDiamondEntity.class, EntityDataSerializers.INT);
            
    private static final EntityDataAccessor<Integer> ANIM_TICKS = 
            SynchedEntityData.defineId(CrazyDiamondEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> TARGET_ID = 
            SynchedEntityData.defineId(CrazyDiamondEntity.class, EntityDataSerializers.INT);

    private UUID ownerUUID = null;

    public CrazyDiamondEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(ANIM_STATE, 0);
        this.entityData.define(ANIM_TICKS, 0);
        this.entityData.define(TARGET_ID, -1);
    }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public UUID getOwnerUUID() {
        if (this.ownerUUID == null) {
            this.ownerUUID = this.entityData.get(OWNER_UUID).orElse(null);
        }
        return this.ownerUUID;
    }

    public void setAnimState(int state, int ticks) {
        this.setAnimState(state, ticks, -1);
    }

    public void setAnimState(int state, int ticks, int targetId) {
        this.entityData.set(ANIM_STATE, state);
        this.entityData.set(ANIM_TICKS, ticks);
        this.entityData.set(TARGET_ID, targetId);
    }

    public int getAnimState() {
        return this.entityData.get(ANIM_STATE);
    }

    public int getAnimTicks() {
        return this.entityData.get(ANIM_TICKS);
    }

    public void setAnimTicks(int ticks) {
        this.entityData.set(ANIM_TICKS, ticks);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity entity) {
        UUID ownerId = getOwnerUUID();
        if (ownerId != null && entity != null) {
            if (entity.getUUID().equals(ownerId)) return true;
        }
        return super.isAlliedTo(entity);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public void tick() {
        super.tick();
        
        int state = getAnimState();
        int ticks = getAnimTicks();
        if (ticks > 0) {
            setAnimTicks(ticks - 1);
            if (ticks - 1 == 0) {
                if (state == STATE_PUNCHING || state == STATE_EXHAUSTION || state == STATE_LOW_PUNCH || state == STATE_FACE_PUNCH || state == STATE_KICKING || state == STATE_DIGGING || state == STATE_WIND_UP || state == STATE_THROWING || state == STATE_REELING_IN) {
                    setAnimState(STATE_IDLE, 0, -1);
                }
            }
        }
        
        UUID uuid = getOwnerUUID();
        if (uuid != null) {
            Player owner = this.level().getPlayerByUUID(uuid);
            if (owner != null && owner.isAlive()) {
                boolean holdsCD = owner.getMainHandItem().is(ModItems.BROKEN_DIAMOND.get()) || 
                                 owner.getOffhandItem().is(ModItems.BROKEN_DIAMOND.get());
                if (!holdsCD) {
                    if (!this.level().isClientSide()) {
                        this.discard();
                    }
                    return;
                }
                
                // Self-cleaning: discard duplicate/ghost stands if not the player's active tracked ID
                if (owner.getPersistentData().contains("xebCrazyDiamondEntityId")) {
                    int activeId = owner.getPersistentData().getInt("xebCrazyDiamondEntityId");
                    if (activeId != this.getId()) {
                        if (!this.level().isClientSide()) {
                            this.discard();
                        }
                        return;
                    }
                }
                
                float rad = (float) Math.toRadians(owner.getYRot());
                net.minecraft.world.phys.Vec3 lookVec = owner.getLookAngle();
                net.minecraft.world.phys.Vec3 upVec = new net.minecraft.world.phys.Vec3(0, 1, 0);
                net.minecraft.world.phys.Vec3 leftVec = lookVec.cross(upVec); 

                double shoulderX = owner.getX() + leftVec.x * 0.6D - lookVec.x * 0.3D;
                double shoulderZ = owner.getZ() + leftVec.z * 0.6D - lookVec.z * 0.3D;
                
                double baseY = owner.getY() + owner.getEyeHeight() - 0.8D;
                if (owner.isCrouching()) {
                    baseY -= 0.3D;
                }
                
                double shoulderY = baseY;

                double targetX = shoulderX;
                double targetY = shoulderY;
                double targetZ = shoulderZ;
                
                if (state == STATE_DASHING) {
                    int targetId = getTargetId();
                    net.minecraft.world.entity.Entity target = targetId != -1 ? this.level().getEntity(targetId) : null;
                    double fwdX = -Math.sin(rad);
                    double fwdZ = Math.cos(rad);
                    double enemyX = target != null ? target.getX() : owner.getX() + fwdX * 6.0D;
                    double enemyY = target != null ? target.getY() : owner.getY();
                    double enemyZ = target != null ? target.getZ() : owner.getZ() + fwdZ * 6.0D;
                    
                    float progress = (8 - ticks) / 8.0F;
                    progress = net.minecraft.util.Mth.clamp(progress, 0.0F, 1.0F);
                    
                    targetX = net.minecraft.util.Mth.lerp(progress, shoulderX, enemyX);
                    targetY = net.minecraft.util.Mth.lerp(progress, shoulderY, enemyY);
                    targetZ = net.minecraft.util.Mth.lerp(progress, shoulderZ, enemyZ);
                } else if (state == STATE_REELING_IN) {
                    double fwdX = -Math.sin(rad);
                    double fwdZ = Math.cos(rad);
                    double pullOffset = 0.5D;
                    targetX -= fwdX * pullOffset;
                    targetZ -= fwdZ * pullOffset;
                }
                
                // Lerp spatial smoothing
                double curX = this.getX();
                double curY = this.getY();
                double curZ = this.getZ();
                
                if (this.tickCount <= 1 || this.distanceToSqr(owner) > 100.0D) {
                    curX = targetX;
                    curY = targetY;
                    curZ = targetZ;
                }
                
                double lerpX = net.minecraft.util.Mth.lerp(0.5D, curX, targetX);
                double lerpY = net.minecraft.util.Mth.lerp(0.5D, curY, targetY);
                double lerpZ = net.minecraft.util.Mth.lerp(0.5D, curZ, targetZ);
                
                this.setPos(lerpX, lerpY, lerpZ);
                
                // Rotacion del Stand (Body Yaw): owner.getYRot()
                this.setYRot(owner.getYRot());
                this.setXRot(owner.getXRot());
                this.yHeadRot = owner.getYRot();
                this.yBodyRot = owner.getYRot();
            } else if (!this.level().isClientSide()) {
                this.discard();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID uuid = getOwnerUUID();
        if (uuid != null) {
            tag.putUUID("OwnerUUID", uuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.setOwnerUUID(tag.getUUID("OwnerUUID"));
        }
    }
}
