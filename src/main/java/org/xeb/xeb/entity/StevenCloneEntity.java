package org.xeb.xeb.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class StevenCloneEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final EntityDataAccessor<Integer> COLOR_INDEX =
            SynchedEntityData.defineId(StevenCloneEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(StevenCloneEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> RETURNING =
            SynchedEntityData.defineId(StevenCloneEntity.class, EntityDataSerializers.BOOLEAN);

    private LivingEntity targetEntity;
    private StevenBossEntity ownerSteven;
    private int lifetimeTicks = 0;
    private int startDelayTicks = 0;
    private boolean struck = false;

    public StevenCloneEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COLOR_INDEX, 0);
        this.entityData.define(ATTACKING, false);
        this.entityData.define(RETURNING, false);
    }

    public int getColorIndex() {
        return this.entityData.get(COLOR_INDEX);
    }

    public void setColorIndex(int index) {
        this.entityData.set(COLOR_INDEX, index);
        this.startDelayTicks = index * 7;
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    public boolean isReturning() {
        return this.entityData.get(RETURNING);
    }

    public void setReturning(boolean returning) {
        this.entityData.set(RETURNING, returning);
    }

    public void initClone(StevenBossEntity owner, LivingEntity target, int colorIdx) {
        this.ownerSteven = owner;
        this.targetEntity = target;
        setColorIndex(colorIdx);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.55D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.8D, this.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.8F, 1.6F);
        }
        this.discard();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;

        this.lifetimeTicks++;

        // FIX DE ORIENTACIÓN: Forzar seguimiento directo del ángulo Y hacia donde se desplaza
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() > 0.001D) {
            float yaw = (float) (Math.atan2(movement.z, movement.x) * (180.0D / Math.PI)) - 90.0F;
            this.setYRot(yaw);
            this.setYHeadRot(yaw);
            this.yBodyRot = yaw;
            this.yRotO = yaw;
            this.yHeadRotO = yaw;
        }

        if (this.lifetimeTicks < this.startDelayTicks) {
            if (ownerSteven != null && ownerSteven.isAlive()) {
                double angle = (this.lifetimeTicks * 0.15D) + (getColorIndex() * 1.0D);
                Vec3 orbitPos = ownerSteven.position().add(Math.cos(angle) * 2.0D, 0.5D, Math.sin(angle) * 2.0D);
                this.setPos(orbitPos.x, orbitPos.y, orbitPos.z);
            }
            return;
        }

        if (!isReturning() && targetEntity != null && targetEntity.isAlive() && !struck) {
            Vec3 dir = targetEntity.position().add(0, 0.5D, 0).subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(0.75D));

            double dist = this.distanceToSqr(targetEntity);
            if (dist < 3.5D) {
                struck = true;
                setAttacking(true);
                targetEntity.hurt(this.damageSources().mobAttack(this), 14.0F);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0F, 1.3F);
                setReturning(true);
            }
        } else {
            setReturning(true);
            if (ownerSteven != null && ownerSteven.isAlive()) {
                Vec3 returnDir = ownerSteven.position().add(0, 1.0D, 0).subtract(this.position()).normalize();
                this.setDeltaMovement(returnDir.scale(0.80D));

                if (this.distanceToSqr(ownerSteven) < 2.0D) {
                    ownerSteven.addStevenCharge();
                    this.discard();
                }
            } else {
                this.discard();
            }
        }

        if (this.lifetimeTicks > 240) {
            this.discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            if (isAttacking()) {
                int variant = getColorIndex() % 4;
                switch (variant) {
                    case 1:
                        return event.setAndContinue(RawAnimation.begin().thenPlay("animation.steven.charged_punch"));
                    case 2:
                        return event.setAndContinue(RawAnimation.begin().thenPlay("animation.steven.chunli_kicks"));
                    case 3:
                        return event.setAndContinue(RawAnimation.begin().thenPlay("animation.steven.punch_barrage"));
                    default:
                        return event.setAndContinue(RawAnimation.begin().thenPlay("animation.steven.melee_attack"));
                }
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.steven.run"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
