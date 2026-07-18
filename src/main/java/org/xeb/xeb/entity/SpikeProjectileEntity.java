package org.xeb.xeb.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.item.SmartHalberdItem;

import java.util.List;

/**
 * Big spike projectile for Smart Halberd right-click.
 */
public class SpikeProjectileEntity extends Projectile {
    private int spikeIndex = 0;
    private int ticksInAir = 0;
    private static final int MAX_LIFETIME = 60; // 3 seconds

    public SpikeProjectileEntity(EntityType<? extends SpikeProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public SpikeProjectileEntity(Level level, LivingEntity owner) {
        this(ModEntities.SPIKE_PROJECTILE.get(), level);
        this.setOwner(owner);
    }

    public void setSpikeIndex(int index) {
        this.spikeIndex = index;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        ticksInAir++;
        
        if (ticksInAir > MAX_LIFETIME || this.position().distanceTo(this.getOwner() != null ? this.getOwner().position() : this.position()) > SmartHalberdItem.SPIKE_MAX_REACH) {
            this.discard();
            return;
        }

        // Trail particles (Aqua green / white)
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.INSTANT_EFFECT,
                    this.getX(), this.getY(), this.getZ(),
                    0.0, 0.0, 0.0);
        }

        // Movement
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        // Entity collision
        if (!this.level().isClientSide()) {
            AABB checkBox = this.getBoundingBox().inflate(0.5D);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    this.level(), this, this.position(), this.position().add(motion), checkBox,
                    e -> e instanceof LivingEntity && e != this.getOwner() && e.isAlive() && e.isPickable()
            );

            if (entityHit != null) {
                onImpact(entityHit.getEntity());
                return;
            }

            // Block collision
            HitResult blockHit = this.level().clip(
                    new net.minecraft.world.level.ClipContext(
                            this.position(),
                            this.position().add(motion),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                            this
                    )
            );

            if (blockHit.getType() != HitResult.Type.MISS) {
                onImpact(null);
                return;
            }
        }
    }

    private void onImpact(Entity hitEntity) {
        if (!this.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            Vec3 impactPos = this.position();

            // Damage hit entity
            if (hitEntity instanceof LivingEntity living && this.getOwner() instanceof LivingEntity owner) {
                living.hurt(this.damageSources().mobAttack(owner), SmartHalberdItem.BIG_SPIKE_DAMAGE);
            }

            // Spawn small barbs in 3x3 area
            for (int i = 0; i < SmartHalberdItem.BARB_COUNT; i++) {
                double angle = (i / (double) SmartHalberdItem.BARB_COUNT) * Math.PI * 2.0;
                double radius = SmartHalberdItem.BARB_SPREAD_RADIUS * (0.5 + this.level().random.nextDouble() * 0.5);
                double bx = impactPos.x + Math.cos(angle) * radius;
                double bz = impactPos.z + Math.sin(angle) * radius;
                double by = impactPos.y + (this.level().random.nextDouble() - 0.5) * 1.0;

                // Damage entities near the barb position
                AABB barbBox = new AABB(bx - 0.5, by - 0.5, bz - 0.5, bx + 0.5, by + 0.5, bz + 0.5);
                List<LivingEntity> barbTargets = this.level().getEntitiesOfClass(LivingEntity.class, barbBox,
                        e -> e != this.getOwner() && e.isAlive() && e.isPickable());

                for (LivingEntity target : barbTargets) {
                    if (this.getOwner() instanceof LivingEntity owner) {
                        target.hurt(this.damageSources().mobAttack(owner), SmartHalberdItem.SMALL_BARB_DAMAGE);
                        // Small knockback away from impact
                        Vec3 knockDir = target.position().subtract(impactPos).normalize();
                        target.setDeltaMovement(target.getDeltaMovement().add(knockDir.x * 0.3, 0.1, knockDir.z * 0.3));
                        target.hurtMarked = true;
                    }
                }

                // Barb particles
                serverLevel.sendParticles(ParticleTypes.CRIT, bx, by, bz, 3, 0.2, 0.2, 0.2, 0.05);
            }

            // Impact particles
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, impactPos.x, impactPos.y, impactPos.z, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.END_ROD, impactPos.x, impactPos.y, impactPos.z, 10, 0.5, 0.5, 0.5, 0.1);

            // Impact sound
            this.level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.5F);
        }

        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("SpikeIndex", spikeIndex);
        nbt.putInt("TicksInAir", ticksInAir);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        spikeIndex = nbt.getInt("SpikeIndex");
        ticksInAir = nbt.getInt("TicksInAir");
    }
}
