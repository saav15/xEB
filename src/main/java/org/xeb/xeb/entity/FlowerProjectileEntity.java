package org.xeb.xeb.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.item.GoldenFlowerItem;

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

        boolean isReturning = this.getPersistentData().getBoolean("xebReturningToOwner");

        // Si la flor ha vivido demasiado sin target ni retorno, descartar
        if (!isReturning && lifeTicks > 120) {
            this.discard();
            return;
        }
        if (lifeTicks > 200) {
            this.discard();
            return;
        }

        // === RETORNO DE FLORES SIN TARGET ===
        if (lifeTicks > 100 && this.getOwner() != null) { // Después de 5 segundos sin target
            boolean canReturn = this.getPersistentData().getBoolean("xebCanReturnSpores");
            if (canReturn && !isReturning) {
                // Cambiar a modo retorno
                this.getPersistentData().putBoolean("xebReturningToOwner", true);
                isReturning = true;
            }
        }

        if (isReturning && this.getOwner() instanceof LivingEntity owner) {
            Vec3 ownerPos = owner.position().add(0, 1.0, 0);
            Vec3 currentPos = this.position();
            double distToOwner = currentPos.distanceTo(ownerPos);
            
            if (distToOwner < 1.5D) {
                // Llegó al player — explotar en esporas
                spawnSporeCloud(this.level(), owner, currentPos);
                this.discard();
                return;
            }
            
            // Homing hacia el owner
            Vec3 dir = ownerPos.subtract(currentPos).normalize();
            double speed = 0.5D;
            this.setDeltaMovement(dir.scale(speed));
        } else {
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
    }

    /**
     * Spawnea una nube de esporas defensivas alrededor del owner.
     * Las esporas dañan mobs hostiles y aplican slow.
     */
    private void spawnSporeCloud(Level level, LivingEntity owner, Vec3 pos) {
        if (level.isClientSide()) return;
        
        ServerLevel serverLevel = (ServerLevel) level;
        
        // Guardar estado de esporas en el owner
        CompoundTag ownerData = owner.getPersistentData();
        ownerData.putInt("xebSporeCloudTicks", GoldenFlowerItem.SPORE_DURATION);
        ownerData.putDouble("xebSporeCloudX", pos.x);
        ownerData.putDouble("xebSporeCloudY", pos.y);
        ownerData.putDouble("xebSporeCloudZ", pos.z);
        
        // Partículas de explosión de esporas
        for (int i = 0; i < 30; i++) {
            double angle = serverLevel.random.nextDouble() * Math.PI * 2.0;
            double radius = GoldenFlowerItem.SPORE_RADIUS * serverLevel.random.nextDouble();
            double px = pos.x + Math.cos(angle) * radius;
            double pz = pos.z + Math.sin(angle) * radius;
            serverLevel.sendParticles(ParticleTypes.END_ROD, px, pos.y + 0.5, pz,
                    2, 0.3, 0.3, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, pos.y + 0.5, pz,
                    1, 0.2, 0.2, 0.2, 0.02);
        }
        
        // Sonido
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.8F);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide()) {
            // Si está retornando, no dañar al owner
            boolean isReturning = this.getPersistentData().getBoolean("xebReturningToOwner");
            if (isReturning && result.getEntity() == this.getOwner()) {
                return; // atravesar al owner
            }
            
            if (result.getEntity() instanceof LivingEntity target) {
                target.invulnerableTime = 0;
                target.hurtTime = 0;
                target.hurt(this.damageSources().thrown(this, this.getOwner()), GoldenFlowerItem.FLOWER_PROJECTILE_DAMAGE);

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
