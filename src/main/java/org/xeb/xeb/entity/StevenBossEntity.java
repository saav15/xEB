package org.xeb.xeb.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.xeb.xeb.beamstruggle.BeamStruggleManager;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import org.xeb.xeb.network.StevenLaserSyncPacket;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.opticblast.ActiveBeamManager;
import org.xeb.xeb.opticblast.BeamData;
import org.xeb.xeb.util.StevenParticleHelper;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class StevenBossEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final EntityDataAccessor<String> ATTACK_STATE =
            SynchedEntityData.defineId(StevenBossEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> STAND_ACTIVE =
            SynchedEntityData.defineId(StevenBossEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> FLOWER_DANCE_ACTIVE =
            SynchedEntityData.defineId(StevenBossEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> STEVEN_CHARGES =
            SynchedEntityData.defineId(StevenBossEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("S T E V E N").withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE),
            BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarOverlay.PROGRESS
    );

    private int stateTicks = 0;
    private int totalLifetimeTicks = 0;
    private int maxDurationTicks = 300;
    private boolean isFullMode = false;
    private boolean despawning = false;
    private LivingEntity currentTarget = null;
    private int strafeDirection = 1;
    private boolean initialMedallionsSet = false;

    public StevenBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_STATE, "PORTAL_IN");
        this.entityData.define(STAND_ACTIVE, false);
        this.entityData.define(FLOWER_DANCE_ACTIVE, false);
        this.entityData.define(STEVEN_CHARGES, 0);
    }

    public String getAttackState() {
        return this.entityData.get(ATTACK_STATE);
    }

    public void setAttackState(String state) {
        this.entityData.set(ATTACK_STATE, state);
    }

    public boolean isStandActive() {
        return this.entityData.get(STAND_ACTIVE);
    }

    public void setStandActive(boolean active) {
        this.entityData.set(STAND_ACTIVE, active);
    }

    public boolean isFlowerDanceActive() {
        return this.entityData.get(FLOWER_DANCE_ACTIVE);
    }

    public void setFlowerDanceActive(boolean active) {
        this.entityData.set(FLOWER_DANCE_ACTIVE, active);
    }

    public int getStevenCharges() {
        return this.entityData.get(STEVEN_CHARGES);
    }

    public void setStevenCharges(int charges) {
        this.entityData.set(STEVEN_CHARGES, Math.max(0, charges));
    }

    public void addStevenCharge() {
        setStevenCharges(getStevenCharges() + 1);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.2D, this.getZ(), 15, 0.4, 0.6, 0.4, 0.08);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F, 1.6F);
        }
    }

    public double getStandDistanceToTarget() {
        if (currentTarget != null && currentTarget.isAlive()) {
            return Math.min(3.0D, this.distanceTo(currentTarget));
        }
        return 1.2D;
    }

    public void setFullMode(boolean full) {
        this.isFullMode = full;
    }

    public void setCustomDurationSeconds(int seconds) {
        this.maxDurationTicks = seconds * 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5000.0D)
                .add(Attributes.ATTACK_DAMAGE, 25.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FLYING_SPEED, 0.50D);
    }

    @Override
    public boolean fireImmune() {
        return true;
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
        // PARRY / COUNTER STANCE MECHANIC: Si está en postura de parry, anula el daño y contraataca
        if ("COUNTER_STANCE".equals(getAttackState()) && source.getEntity() != null) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.2D, this.getZ(), 20, 0.5, 0.5, 0.5, 0.2);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.5F, 0.5F);

                Vec3 behind = source.getEntity().position().subtract(source.getEntity().getLookAngle().scale(1.5D));
                this.setPos(behind.x, behind.y, behind.z);
                source.getEntity().hurt(this.damageSources().mobAttack(this), 35.0F);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.2F, 1.4F);
            }
            chooseNextAttack();
            return false;
        }

        float reduced = amount * 0.05F;
        return super.hurt(source, Math.min(reduced, 15.0F));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void tick() {
        super.tick();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (this.level().isClientSide()) return;

        this.clearFire(); // Prevenir auto-combustión de llamas propias

        this.totalLifetimeTicks++;
        this.stateTicks++;

        boolean isLowHpEnrage = (this.getHealth() / this.getMaxHealth()) < 0.25F;

        if (!initialMedallionsSet) {
            initialMedallionsSet = true;
            rotateMedallionsAdaptive();
        }

        if (this.totalLifetimeTicks % 350 == 0) {
            rotateMedallionsAdaptive();
        }

        int chargeInterval = isLowHpEnrage ? 300 : 600;
        if (this.totalLifetimeTicks % chargeInterval == 0) {
            addStevenCharge();
        }

        if (isLowHpEnrage && this.level() instanceof ServerLevel serverLevel) {
            if (this.totalLifetimeTicks % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0D, this.getZ(), 4, 0.4, 0.6, 0.4, 0.1);
            }
        }

        int charges = getStevenCharges();
        if (charges > 0 && this.level() instanceof ServerLevel serverLevel) {
            int circleCount = Math.min(8, charges);
            for (int i = 0; i < circleCount; i++) {
                double orbitAngle = (this.totalLifetimeTicks * 0.2D) + (i * (Math.PI * 2.0D / circleCount));
                double rx = this.getX() + Math.cos(orbitAngle) * 0.85D;
                double ry = this.getY() + 0.6D + Math.sin(orbitAngle * 2.0D) * 0.5D;
                double rz = this.getZ() + Math.sin(orbitAngle) * 0.85D;

                boolean isWhite = (i % 2 == 0);
                DustParticleOptions circleDust = new DustParticleOptions(
                        isWhite ? new Vector3f(1.0F, 1.0F, 1.0F) : new Vector3f(0.05F, 0.05F, 0.05F), 1.2F);
                serverLevel.sendParticles(circleDust, rx, ry, rz, 1, 0, 0, 0, 0);
            }

            if (this.totalLifetimeTicks % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0D, this.getZ(), 2, 0.3, 0.5, 0.3, 0.02);
            }
        }

        findOrUpdateTarget();

        if (currentTarget != null) {
            double dx = currentTarget.getX() - this.getX();
            double dz = currentTarget.getZ() - this.getZ();
            float yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

            this.setYRot(yaw);
            this.setYHeadRot(yaw);
            this.yBodyRot = yaw;
            this.yRotO = yaw;
            this.yHeadRotO = yaw;

            Vec3 dir = currentTarget.position().subtract(this.position()).normalize();
            double distSqr = this.distanceToSqr(currentTarget);
            double targetY = currentTarget.getY() + 0.5D;
            double dy = (targetY - this.getY()) * 0.15D;
            String currentState = getAttackState();

            if ("BEAM_CHARGE".equals(currentState) || "LASER".equals(currentState) || "FLOWER_DANCE".equals(currentState) || "ULTIMATE".equals(currentState)) {
                if (distSqr < 64.0D && !"ULTIMATE".equals(currentState)) {
                    this.setDeltaMovement(dir.scale(-0.25D).add(0, Math.max(-0.2D, Math.min(0.2D, dy)), 0));
                } else if (distSqr > 196.0D) {
                    this.setDeltaMovement(dir.scale(0.35D).add(0, Math.max(-0.2D, Math.min(0.2D, dy)), 0));
                } else {
                    this.setDeltaMovement(new Vec3(0, Math.max(-0.1D, Math.min(0.1D, dy)), 0));
                }
            } else if ("PUNCH_BARRAGE".equals(currentState)) {
                if (distSqr < 6.0D) {
                    this.setDeltaMovement(dir.scale(-0.2D).add(0, Math.max(-0.2D, Math.min(0.2D, dy)), 0));
                } else if (distSqr > 16.0D) {
                    this.setDeltaMovement(dir.scale(0.35D).add(0, Math.max(-0.2D, Math.min(0.2D, dy)), 0));
                }
            } else if (!"IDLE_STRAFE".equals(currentState) && distSqr > 2.5D) {
                this.setDeltaMovement(new Vec3(dir.x * 0.40D, Math.max(-0.35D, Math.min(0.35D, dy)), dir.z * 0.40D));
            }
        }

        String currentState = getAttackState();

        if ("PORTAL_IN".equals(currentState)) {
            if (this.stateTicks >= 20) {
                chooseNextAttack();
            }
        } else if ("IDLE_STRAFE".equals(currentState)) {
            executeIdleStrafe();
        } else if ("CHARGED_PUNCH".equals(currentState)) {
            executeChargedPunch();
        } else if ("BEAM_CHARGE".equals(currentState)) {
            executeBeamCharge();
        } else if ("LASER".equals(currentState)) {
            executeLaser();
        } else if ("PUNCH_BARRAGE".equals(currentState)) {
            executePunchBarrage();
        } else if ("CHUNLI_KICKS".equals(currentState)) {
            executeChunLiKicks();
        } else if ("FLOWER_DANCE".equals(currentState)) {
            executeFlowerDance();
        } else if ("TAP_HEAD".equals(currentState)) {
            executeTapHead();
        } else if ("COUNTER_STANCE".equals(currentState)) {
            executeCounterStance();
        } else if ("ULTIMATE".equals(currentState)) {
            executeUltimate();
        } else if ("PORTAL_OUT".equals(currentState)) {
            if (this.stateTicks >= 20) {
                this.bossEvent.removeAllPlayers();
                this.discard();
            }
        }

        if (!isFullMode && this.totalLifetimeTicks >= this.maxDurationTicks && !despawning) {
            triggerPortalOut();
        }
    }

    private void findOrUpdateTarget() {
        Player player = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 32.0D,
                p -> p instanceof Player pl && !pl.isSpectator() && !pl.isCreative() && pl.isAlive());
        if (player != null) {
            this.currentTarget = player;
        } else {
            AABB searchBox = this.getBoundingBox().inflate(32.0D);
            List<LivingEntity> living = this.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                    e -> e != this && !(e instanceof StevenCloneEntity) && e.isAlive() && (!(e instanceof Player p) || (!p.isSpectator() && !p.isCreative())));
            if (!living.isEmpty()) {
                living.sort(Comparator.comparingDouble(this::distanceToSqr));
                this.currentTarget = living.get(0);
            } else {
                this.currentTarget = null;
            }
        }
    }

    public void forceAttack(String attackType, LivingEntity target) {
        this.currentTarget = target;
        this.stateTicks = 0;
        if ("BEAM".equalsIgnoreCase(attackType) || "LASER".equalsIgnoreCase(attackType)) {
            this.setAttackState("BEAM_CHARGE");
        } else {
            this.setAttackState(attackType);
        }
        this.setStandActive("PUNCH_BARRAGE".equalsIgnoreCase(attackType));
        this.setFlowerDanceActive("FLOWER_DANCE".equalsIgnoreCase(attackType));
    }

    private void chooseNextAttack() {
        this.stateTicks = 0;

        if (getStevenCharges() >= 22) {
            forceAttack("ULTIMATE", this.currentTarget);
            return;
        }

        if (this.random.nextBoolean()) {
            this.strafeDirection = this.random.nextBoolean() ? 1 : -1;
            setAttackState("IDLE_STRAFE");
            return;
        }
        String[] attacks = {"CHARGED_PUNCH", "BEAM_CHARGE", "PUNCH_BARRAGE", "CHUNLI_KICKS", "FLOWER_DANCE", "TAP_HEAD", "COUNTER_STANCE"};
        String chosen = attacks[this.random.nextInt(attacks.length)];
        forceAttack(chosen, this.currentTarget);
    }

    private void executeIdleStrafe() {
        if (currentTarget != null && currentTarget.isAlive()) {
            Vec3 dir = currentTarget.position().subtract(this.position()).normalize();
            Vec3 strafeDir = new Vec3(-dir.z * strafeDirection, 0, dir.x * strafeDirection);
            this.setDeltaMovement(strafeDir.scale(0.32D));
        }

        if (this.stateTicks >= 20) {
            chooseNextAttack();
        }
    }

    private void executeChargedPunch() {
        boolean empowered = getStevenCharges() > 0;
        if (this.stateTicks <= 30) {
            Vec3 fistPos = this.position().add(this.getLookAngle().scale(0.8D)).add(0, 1.2D, 0);
            if (this.level() instanceof ServerLevel serverLevel) {
                StevenParticleHelper.spawnFistChargeParticles(serverLevel, fistPos);
            }
            if (this.stateTicks == 1) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0F, 1.8F);
                if (empowered) setStevenCharges(getStevenCharges() - 1);
            }
        } else if (this.stateTicks <= 50) {
            if (currentTarget != null) {
                Vec3 dir = currentTarget.position().subtract(this.position()).normalize();
                this.setDeltaMovement(dir.scale(empowered ? 1.8D : 1.4D));
            }
            if (this.level() instanceof ServerLevel serverLevel) {
                StevenParticleHelper.spawnDashTrail(serverLevel, this.position(), this.getDeltaMovement());
            }

            AABB box = this.getBoundingBox().inflate(1.6D);
            List<LivingEntity> hitList = this.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && !(e instanceof StevenCloneEntity));
            float damage = empowered ? 55.0F : 35.0F;
            for (LivingEntity victim : hitList) {
                victim.hurt(this.damageSources().mobAttack(this), damage);
                Vec3 knock = victim.position().subtract(this.position()).normalize().scale(3.5D).add(0, 0.8, 0);
                victim.setDeltaMovement(knock);
                victim.hasImpulse = true;
            }
        } else {
            chooseNextAttack();
        }
    }

    private void executeBeamCharge() {
        if (this.stateTicks <= 25) {
            if (this.stateTicks == 20 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                        this.getX(), this.getY() + 1.2D, this.getZ(), 1, 0, 0, 0, 0);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2F, 1.5F);
            }
        } else {
            this.stateTicks = 0;
            this.setAttackState("LASER");
        }
    }

    private void executeLaser() {
        boolean inStruggle = BeamStruggleManager.isEntityInStruggle(this.getUUID());
        boolean empowered = getStevenCharges() > 0;

        if (this.stateTicks == 1 && empowered) {
            setStevenCharges(getStevenCharges() - 1);
        }

        if (this.stateTicks <= 55 || inStruggle) {
            Vec3 origin = this.position().add(0, 1.8D, 0);
            Vec3 dir = this.getLookAngle().normalize();
            if (currentTarget != null) {
                dir = currentTarget.position().add(0, 1.0, 0).subtract(origin).normalize();
            }
            Vec3 beamEnd = origin.add(dir.scale(30.0D));

            if (this.level() instanceof ServerLevel serverLevel) {
                long gameTick = serverLevel.getGameTime();
                BeamData beam = new BeamData(this.getUUID(), this.getId(), origin, beamEnd, 0xFF050505, gameTick, gameTick + 10, "steven_laser");
                ActiveBeamManager.get().putBeam(this.getUUID(), beam);

                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                        new StevenLaserSyncPacket(this.getId(), true, origin, beamEnd));

                Vec3 collision = ActiveBeamManager.get().checkBeamVsBeamCollision(this.getUUID(), origin, beamEnd);
                if (collision != null && currentTarget != null) {
                    BeamStruggleManager.onBeamCollision(this.getUUID(), currentTarget.getUUID(), origin, currentTarget.getEyePosition(1.0F), collision, gameTick, serverLevel);
                }

                AABB beamBox = new AABB(origin, beamEnd).inflate(1.2D);
                List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, beamBox, e -> e != this && !(e instanceof StevenCloneEntity));
                float damage = empowered ? 11.0F : 7.0F;
                for (LivingEntity victim : victims) {
                    victim.hurt(this.damageSources().indirectMagic(this, this), damage);
                }
            }

            if (this.stateTicks % 5 == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.2F, 0.4F);
            }
        } else {
            if (this.level() instanceof ServerLevel serverLevel) {
                ActiveBeamManager.get().removeBeam(this.getUUID());
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                        new StevenLaserSyncPacket(this.getId(), false, Vec3.ZERO, Vec3.ZERO));
            }
            chooseNextAttack();
        }
    }

    private void executePunchBarrage() {
        this.setStandActive(true);
        if (this.stateTicks <= 65) {
            if (this.stateTicks % 2 == 0 && this.level() instanceof ServerLevel serverLevel) {
                double offsetDist = getStandDistanceToTarget();
                Vec3 standCenter = this.position().add(this.getLookAngle().scale(offsetDist)).add(0, 1.2D, 0);

                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, standCenter.x, standCenter.y, standCenter.z, 10, 0.4, 0.4, 0.4, 0.15);

                AABB punchBox = new AABB(standCenter, standCenter).inflate(2.0D);
                List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class, punchBox, e -> e != this && !(e instanceof StevenCloneEntity));
                for (LivingEntity victim : victims) {
                    victim.hurt(this.damageSources().mobAttack(this), 6.5F);
                    victim.invulnerableTime = 0;
                }
                this.level().playSound(null, standCenter.x, standCenter.y, standCenter.z,
                        SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 0.8F, 1.6F);
            }
        } else {
            this.setStandActive(false);
            chooseNextAttack();
        }
    }

    private void executeChunLiKicks() {
        if (currentTarget != null) {
            Vec3 dir = currentTarget.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(0.5D));
        }

        if (this.stateTicks <= 35) {
            if (this.stateTicks % 3 == 0 && this.level() instanceof ServerLevel serverLevel) {
                AABB box = this.getBoundingBox().inflate(1.8D);
                List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && !(e instanceof StevenCloneEntity));
                for (LivingEntity victim : victims) {
                    victim.hurt(this.damageSources().mobAttack(this), 7.0F);
                    victim.invulnerableTime = 0;
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.8F);
            }
        } else {
            chooseNextAttack();
        }
    }

    private void executeFlowerDance() {
        this.setFlowerDanceActive(true);
        if (this.stateTicks == 5 && this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 6; i++) {
                StevenCloneEntity clone = new StevenCloneEntity(ModEntities.STEVEN_CLONE.get(), serverLevel);
                double angle = i * (Math.PI * 2.0D / 6.0D);
                Vec3 offset = new Vec3(Math.cos(angle) * 2.0D, 0.2D, Math.sin(angle) * 2.0D);
                clone.setPos(this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
                clone.initClone(this, currentTarget, i);
                serverLevel.addFreshEntity(clone);
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.6F);
        } else if (this.stateTicks > 75) {
            this.setFlowerDanceActive(false);
            chooseNextAttack();
        }
    }

    private void executeTapHead() {
        if (this.stateTicks <= 25) {
            if (this.stateTicks == 15 && this.level() instanceof ServerLevel serverLevel) {
                StevenParticleHelper.spawnMetaTapParticles(serverLevel, this.position());
                rotateMedallionsAdaptive();
                this.level().playSound(null, this.getX(), this.getY() + 2.0D, this.getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.2F, 1.4F);
            }
        } else {
            chooseNextAttack();
        }
    }

    private void executeCounterStance() {
        if (this.stateTicks <= 30) {
            if (this.stateTicks % 5 == 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.2D, this.getZ(), 5, 0.4, 0.4, 0.4, 0.1);
            }
        } else {
            chooseNextAttack();
        }
    }

    private void executeUltimate() {
        if (this.stateTicks <= 50) {
            Vec3 riftCenter = this.position().add(this.getLookAngle().scale(2.5D)).add(0, 1.5D, 0);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, riftCenter.x, riftCenter.y, riftCenter.z, 20, 0.8, 0.8, 0.8, 0.2);
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, riftCenter.x, riftCenter.y, riftCenter.z, 15, 0.6, 0.6, 0.6, 0.1);

                AABB suckBox = new AABB(riftCenter, riftCenter).inflate(14.0D);
                List<LivingEntity> pulled = serverLevel.getEntitiesOfClass(LivingEntity.class, suckBox, e -> e != this && !(e instanceof StevenCloneEntity));
                for (LivingEntity victim : pulled) {
                    Vec3 pullDir = riftCenter.subtract(victim.position()).normalize().scale(0.40D);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(pullDir));
                    victim.hasImpulse = true;
                }
            }

            if (this.stateTicks == 1) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0F, 0.3F);
            }
        } else {
            if (this.level() instanceof ServerLevel serverLevel) {
                Vec3 riftCenter = this.position().add(this.getLookAngle().scale(2.5D)).add(0, 1.5D, 0);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, riftCenter.x, riftCenter.y, riftCenter.z, 5, 1.0, 1.0, 1.0, 0.0);
                serverLevel.sendParticles(ParticleTypes.FLASH, riftCenter.x, riftCenter.y, riftCenter.z, 10, 0.5, 0.5, 0.5, 0.0);

                AABB blastBox = new AABB(riftCenter, riftCenter).inflate(8.0D);
                List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, blastBox, e -> e != this && !(e instanceof StevenCloneEntity));
                for (LivingEntity victim : victims) {
                    float blastDamage = (victim instanceof Player) ? 70.0F : 150.0F;
                    victim.hurt(this.damageSources().magic(), blastDamage);
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                    Vec3 blastKnock = victim.position().subtract(riftCenter).normalize().scale(4.0D).add(0, 1.0, 0);
                    victim.setDeltaMovement(blastKnock);
                    victim.hasImpulse = true;
                }

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5F, 0.5F);
            }

            setStevenCharges(Math.max(0, getStevenCharges() - 22));
            chooseNextAttack();
        }
    }

    private void rotateMedallionsAdaptive() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // Desacoplar atributos/efectos de medallones viejos
        List<MedallionData> oldList = MedallionManager.getMedallions(this);
        for (MedallionData m : oldList) {
            try {
                m.getBuff().onDetach(this, m.getUniqueId());
            } catch (Exception ignored) {}
        }

        int targetLevel = 10;
        AABB searchBox = this.getBoundingBox().inflate(32.0D);
        List<Player> nearbyPlayers = serverLevel.getEntitiesOfClass(Player.class, searchBox,
                p -> !p.isSpectator() && !p.isCreative());

        if (!nearbyPlayers.isEmpty()) {
            targetLevel = 1;
            for (Player p : nearbyPlayers) {
                int pLvl = MedallionManager.getEliteMeterLevel(p);
                if (pLvl > targetLevel) targetLevel = pLvl;
            }
        }

        List<MedallionData> rolled = new ArrayList<>();
        Set<String> excludeSet = new HashSet<>();

        List<EliteBuff> availableBuffs = new ArrayList<>(EliteBuffRegistry.getAll());
        Collections.shuffle(availableBuffs, new java.util.Random());

        int count = 4;
        for (EliteBuff buff : availableBuffs) {
            if (rolled.size() >= count) break;
            if (excludeSet.contains(buff.getId())) continue;

            MedallionType tier;
            double roll = this.random.nextDouble();
            if (targetLevel >= 8) {
                tier = roll < 0.65 ? MedallionType.LEGENDARY : (roll < 0.90 ? MedallionType.RARE : MedallionType.COMMON);
            } else if (targetLevel >= 5) {
                tier = roll < 0.45 ? MedallionType.RARE : MedallionType.COMMON;
            } else {
                tier = MedallionType.COMMON;
            }

            rolled.add(new MedallionData(buff, tier, UUID.randomUUID()));
            excludeSet.addAll(buff.getConflicts());
            excludeSet.add(buff.getId());
        }

        MedallionManager.removeAllMedallions(this);
        MedallionManager.saveMedallions(this, rolled);

        // Acoplar en tiempo real atributos/efectos de nuevos medallones
        for (MedallionData m : rolled) {
            try {
                m.getBuff().onAttach(this, m.getUniqueId());
            } catch (Exception ignored) {}
        }

        MedallionManager.syncToTracking(this);
        StevenParticleHelper.spawnMetaTapParticles(serverLevel, this.position());
    }

    private void triggerPortalOut() {
        if (!despawning) {
            despawning = true;
            this.stateTicks = 0;
            this.setAttackState("PORTAL_OUT");
            this.setStandActive(false);
            this.setFlowerDanceActive(false);
            if (this.level() instanceof ServerLevel serverLevel) {
                StevenPortalEntity portal = new StevenPortalEntity(ModEntities.STEVEN_PORTAL.get(), serverLevel);
                portal.setPos(this.getX(), this.getY(), this.getZ());
                serverLevel.addFreshEntity(portal);
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PORTAL_TRAVEL, SoundSource.HOSTILE, 1.0F, 1.5F);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            String state = getAttackState();
            switch (state) {
                case "CHARGED_PUNCH":
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.steven.charged_punch"));
                case "BEAM_CHARGE":
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.steven.beam_charge"));
                case "LASER":
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.steven.laser"));
                case "PUNCH_BARRAGE":
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.steven.punch_barrage"));
                case "CHUNLI_KICKS":
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.steven.chunli_kicks"));
                case "FLOWER_DANCE":
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.steven.flower_dance"));
                case "TAP_HEAD":
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.steven.tap_head"));
                case "COUNTER_STANCE":
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.steven.counter_stance"));
                case "ULTIMATE":
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.steven.ultimate_charge"));
                case "PORTAL_IN":
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.steven.portal_in"));
                case "PORTAL_OUT":
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.steven.portal_out"));
                case "IDLE_STRAFE":
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.steven.run"));
                default:
                    if (this.getDeltaMovement().lengthSqr() > 0.05D) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.steven.run"));
                    }
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.steven.levitate"));
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
