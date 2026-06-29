package org.xeb.xeb.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class TankWitherfistEntity extends WitherSkeleton {

    private static final EntityDataAccessor<Boolean> POWER_BLOCKING = SynchedEntityData.defineId(TankWitherfistEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ULTRA_CHARGED = SynchedEntityData.defineId(TankWitherfistEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("Tank Witherfist"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    // Ability cooldown trackers (in ticks)
    private int chargedPunchCooldown = 0;   // 8s = 160 ticks
    private int earthquakeSlamCooldown = 0; // 7s = 140 ticks
    private int powerBlockCooldown = 0;     // 7s = 140 ticks

    // State tracking
    private int dashingTimer = 0;            // active dash ticks remaining
    private Vec3 dashDir = Vec3.ZERO;
    private boolean isChargingPunch = false;
    private int chargeTicks = 0;             // how long it has been charging (max 40 for full charge)

    private boolean isSlamming = false;
    private int slamTimer = 0;               // path ticks remaining (25 down to 0)

    private boolean isPowerBlocking = false;
    private int blockTimer = 0;              // how long it has been blocking (max 40)
    private float accumulatedBlockedDamage = 0.0F;

    private boolean forceChargedPunch = false;
    private boolean awakenSoundPlayed = false;

    public TankWitherfistEntity(EntityType<? extends WitherSkeleton> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("Tank Witherfist"));
        this.setCustomNameVisible(false); // shown via bossbar instead
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(POWER_BLOCKING, false);
        this.entityData.define(ULTRA_CHARGED, false);
    }

    // ── Attributes ───────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return WitherSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 350.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)           // faster than vanilla (0.25)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)             // base melee damage
                .add(Attributes.ARMOR, 20.0D)                    // netherite-level protection (hidden)
                .add(Attributes.ARMOR_TOUGHNESS, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ChargedPunchGoal(this));
        this.goalSelector.addGoal(3, new PowerBlockGoal(this));
        this.goalSelector.addGoal(4, new EarthquakeSlamGoal(this));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ── Spawn setup ────────────────────────────────────────────────────────────

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data, dataTag);

        // Equip Doomfist v2 in main hand
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DOOMFIST_V2.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 1.0F); // guaranteed drop

        // Clear armour slots (no visible armour)
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);

        // Medallion setup
        if (level instanceof ServerLevel serverLevel) {
            // Guarantee Mega LEGENDARY medallion
            var megaBuff = EliteBuffRegistry.getById("mega");
            if (megaBuff != null) {
                MedallionManager.attachMedallion(this, new MedallionData(megaBuff, MedallionType.LEGENDARY, UUID.randomUUID()));
            }
            this.refreshDimensions();
        }

        return result;
    }

    // ── Main tick ──────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Client-side synchronization: copy synced entity data to persistent data so that DoomfistRenderLayer sees it
        if (this.level().isClientSide()) {
            this.getPersistentData().putBoolean("xebPowerBlocking", this.entityData.get(POWER_BLOCKING));
            this.getPersistentData().putBoolean("xebUltraCharged", this.entityData.get(ULTRA_CHARGED));
        }

        // Tick down cooldowns (halved at <= 50% HP)
        int cdTick = (this.getHealth() <= this.getMaxHealth() / 2.0F) ? 2 : 1;
        if (chargedPunchCooldown > 0) chargedPunchCooldown = Math.max(0, chargedPunchCooldown - cdTick);
        if (earthquakeSlamCooldown > 0) earthquakeSlamCooldown = Math.max(0, earthquakeSlamCooldown - cdTick);
        if (powerBlockCooldown > 0) powerBlockCooldown = Math.max(0, powerBlockCooldown - cdTick);

        // Boss bar progress and server-side mechanics
        if (!this.level().isClientSide()) {
            bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

            // Phase 2 (health <= 50%)
            boolean atOrBelowHalf = this.getHealth() <= this.getMaxHealth() / 2.0F;
            if (atOrBelowHalf) {
                // Strength 1 permanently (DAMAGE_BOOST with amplifier 0)
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, false));

                // Awaken sound played once
                if (!awakenSoundPlayed) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                            net.minecraft.sounds.SoundSource.HOSTILE, 1.875F, 1.0F);
                    awakenSoundPlayed = true;
                }
            }

            // Ranged attack during Charged Fist
            if (this.hasEffect(org.xeb.xeb.effect.ModEffects.CHARGED_FIST.get())) {
                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive()) {
                    double distSqr = this.distanceToSqr(target);
                    // Ranged attack distance: 3 to 24 blocks (9 to 576 distSqr)
                    if (distSqr >= 9.0D && distSqr <= 576.0D) {
                        if (this.tickCount % 15 == 0) {
                            SparkleEntity sparkle = new SparkleEntity(this.level(), this, 6.0D, target);
                            sparkle.moveTo(this.getX(), this.getEyeY() - 0.2D, this.getZ());
                            this.level().addFreshEntity(sparkle);

                            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                    net.minecraft.sounds.SoundEvents.ILLUSIONER_CAST_SPELL,
                                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.2F);
                        }
                    }
                }
            }
        }

        // Doomfist charging animation — apply Charged Fist effect when charging
        if (isChargingPunch) {
            chargeTicks++;
            if (chargeTicks >= 20 && !this.hasEffect(org.xeb.xeb.effect.ModEffects.CHARGED_FIST.get())) {
                this.addEffect(new MobEffectInstance(org.xeb.xeb.effect.ModEffects.CHARGED_FIST.get(), 600, 0, false, false));
            }
        } else {
            if (chargeTicks > 0) chargeTicks = 0;
        }

        // Active dash tick
        if (dashingTimer > 0) {
            dashingTimer--;
            this.setDeltaMovement(dashDir.x, this.getDeltaMovement().y, dashDir.z);
            this.hurtMarked = true;

            // Hit detection during dash
            AABB hitBox = this.getBoundingBox().inflate(1.2D);
            List<LivingEntity> hit = this.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != this && e.isAlive() && !e.isAlliedTo(this));
            if (!hit.isEmpty()) {
                float chargeRatio = Math.min(1.0F, chargeTicks / 40.0F);
                boolean isUltra = this.entityData.get(ULTRA_CHARGED);
                float baseDamage = isUltra ? 14.0F : 7.0F;
                float damage = baseDamage + chargeRatio * 15.0F;

                if (isUltra && !this.level().isClientSide()) {
                    this.entityData.set(ULTRA_CHARGED, false);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.8F);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(0.5D), this.getZ(), 3, 0.5D, 0.5D, 0.5D, 0.0D);
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, this.getX(), this.getY(0.5D), this.getZ(), 30, 1.0D, 1.0D, 1.0D, 0.2D);
                    }
                }

                for (LivingEntity target : hit) {
                    target.hurt(this.damageSources().mobAttack(this), damage);
                    Vec3 look = this.getLookAngle();
                    double kb = isUltra ? (2.5D + chargeRatio * 3.5D) : (1.5D + chargeRatio * 2.0D);
                    target.knockback(kb, -look.x, -look.z);
                    target.hurtMarked = true;
                }
                dashingTimer = 0; // stop dash on first hit
            }
        }

        // Earthquake Slam ticking (steered on server)
        if (isSlamming && !this.level().isClientSide()) {
            if (slamTimer > 12) {
                slamTimer--;
            }
            LivingEntity target = this.getTarget();
            if (target == null || !target.isAlive()) {
                isSlamming = false;
                slamTimer = 0;
            } else {
                // Smart Cancel:
                // If in rising phase (timer > 12), and charged punch cooldown is 0,
                // and target is close (4 to 12 blocks), we have a 10% chance to cancel and punch!
                // Or if target is in the air/jumping, we cancel and punch!
                if (slamTimer > 12 && this.chargedPunchCooldown == 0) {
                    double distSqr = this.distanceToSqr(target);
                    boolean targetInAir = !target.onGround() && target.getDeltaMovement().y > 0.0D;
                    boolean shouldCancel = targetInAir || (distSqr >= 16.0D && distSqr <= 144.0D && this.random.nextFloat() < 0.10F);
                    if (shouldCancel) {
                        isSlamming = false;
                        slamTimer = 0;
                        // Give upward momentum bump like the player
                        this.setDeltaMovement(this.getDeltaMovement().x, 0.55D, this.getDeltaMovement().z);
                        this.hurtMarked = true;
                        
                        this.forceChargedPunch = true;
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_CHARGE,
                                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.3F);
                    }
                }

                if (isSlamming) { // Check again in case it was cancelled
                    Vec3 look = target.position().subtract(this.position()).normalize();
                    Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
                    
                    if (slamTimer > 12) { // Rising Phase
                        double blendedX = this.getDeltaMovement().x * 0.7D + forward.x * 0.8D * 0.3D;
                        double blendedZ = this.getDeltaMovement().z * 0.7D + forward.z * 0.8D * 0.3D;
                        double ySpeed = 0.4D + (slamTimer - 12) * 0.05D;
                        this.setDeltaMovement(blendedX, ySpeed, blendedZ);
                        this.hurtMarked = true;
                    } else { // Falling Phase
                        double blendedX = this.getDeltaMovement().x * 0.65D + forward.x * 0.8D * 0.35D;
                        double blendedZ = this.getDeltaMovement().z * 0.65D + forward.z * 0.8D * 0.35D;
                        this.setDeltaMovement(blendedX, -0.9D, blendedZ);
                        this.hurtMarked = true;
                        
                        boolean hitFloor = this.onGround();
                        if (!hitFloor) {
                            AABB checkAABB = this.getBoundingBox().move(0.0D, -0.3D, 0.0D);
                            if (!this.level().noCollision(this, checkAABB)) {
                                hitFloor = true;
                            }
                        }
                        
                        if (hitFloor) {
                            isSlamming = false;
                            slamTimer = 0;
                            executeEarthquakeImpact();
                            earthquakeSlamCooldown = 140;
                        }
                    }
                }
            }
        }

        // Power Block ticking
        if (isPowerBlocking) {
            blockTimer++;
            // Slow down movement speed by 35%
            this.setDeltaMovement(this.getDeltaMovement().x * 0.65D, this.getDeltaMovement().y, this.getDeltaMovement().z * 0.65D);
            this.hurtMarked = true;

            if (blockTimer >= 40) { // 2s duration
                stopPowerBlock();
            }
        }
    }

    // ── Boss bar player tracking ─────────────────────────────────────────────

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

    // ── Combat & Damage Block Logic ──────────────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isPowerBlocking && !this.level().isClientSide()) {
            Entity attacker = source.getEntity();
            Vec3 sourcePos = source.getSourcePosition();
            if (sourcePos == null && attacker != null) {
                sourcePos = attacker.position();
            }
            if (sourcePos != null) {
                Vec3 look = this.getLookAngle();
                Vec3 toSource = sourcePos.subtract(this.position()).normalize();
                Vec3 horizLook = new Vec3(look.x, 0.0D, look.z).normalize();
                Vec3 horizToSource = new Vec3(toSource.x, 0.0D, toSource.z).normalize();
                double dot = horizLook.dot(horizToSource);

                if (dot > 0.0D) { // Frontal damage
                    // First second: projectile absorption & mitigation completely
                    if (blockTimer <= 20 && source.getDirectEntity() instanceof Projectile proj) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
                        proj.discard();

                        accumulatedBlockedDamage += amount;
                        checkUltraChargeThreshold();
                        return false; // completely negate
                    }

                    // Otherwise mitigation by 75%
                    float blockedAmt = amount * 0.75F;
                    amount = amount * 0.25F;
                    accumulatedBlockedDamage += blockedAmt;
                    checkUltraChargeThreshold();
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,
                            net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
                }
            }
        }
        return super.hurt(source, amount);
    }

    private void checkUltraChargeThreshold() {
        if (accumulatedBlockedDamage >= this.getMaxHealth() * 0.10F && !this.entityData.get(ULTRA_CHARGED)) {
            this.entityData.set(ULTRA_CHARGED, true);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 1.0F);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.5F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, this.getX(), this.getY(0.5D), this.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.1D);
                net.minecraft.core.particles.DustParticleOptions redDust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.0F, 0.0F), 1.5F);
                serverLevel.sendParticles(redDust, this.getX(), this.getY(0.5D), this.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.1D);
            }
        }
    }

    // ── Ability helpers ────────────────────────────────────────────────────────

    private void executeEarthquakeImpact() {
        Vec3 pos = this.position();
        Vec3 look = this.getLookAngle();
        Vec3 horizLook = new Vec3(look.x, 0.0D, look.z).normalize();

        // Trigger propagating red shockwave visuals (same as player)
        this.getPersistentData().putInt("xebSlam2ShockwaveStep", 1);
        this.getPersistentData().putDouble("xebSlam2ShockwaveLookX", horizLook.x);
        this.getPersistentData().putDouble("xebSlam2ShockwaveLookZ", horizLook.z);

        AABB area = this.getBoundingBox().inflate(6.0D, 3.0D, 6.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this && e.isAlive() && !e.isAlliedTo(this));

        double baseDamage = this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) (baseDamage * 0.8D);

        for (LivingEntity target : targets) {
            Vec3 offset = target.position().subtract(pos);
            double dist = offset.horizontalDistance();

            if (dist <= 6.0D && Math.abs(offset.y) <= 2.0D) {
                Vec3 horizOffsetDir = new Vec3(offset.x, 0.0D, offset.z).normalize();
                double dot = horizLook.dot(horizOffsetDir);

                if (dot >= 0.866D) { // 60 degree cone
                    target.hurt(this.damageSources().mobAttack(this), damage);

                    Vec3 pullDir = pos.subtract(target.position()).normalize();
                    target.setDeltaMovement(pullDir.x * 0.3D, 0.4D, pullDir.z * 0.3D);
                    target.hurtMarked = true;
                }
            }
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.7F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5D), this.getZ(), 8, 0.5D, 0.2D, 0.5D, 0.1D);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, this.getX(), this.getY(0.5D), this.getZ(), 25, 1.0D, 0.2D, 1.0D, 0.2D);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, this.getX(), this.getY(0.5D), this.getZ(), 12, 0.8D, 0.2D, 0.8D, 0.1D);

            // Circular POOF dust shockwave particles expanding outward
            for (int i = 0; i < 16; i++) {
                double angle = (i * Math.PI * 2.0D) / 16.0D;
                double dx = Math.cos(angle) * 1.5D;
                double dz = Math.sin(angle) * 1.5D;
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, this.getX() + dx, this.getY() + 0.1D, this.getZ() + dz, 1, 0.1D, 0.0D, 0.1D, 0.02D);
            }
        }
    }

    // ── Save/Load ─────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("xwfChargedPunchCD", chargedPunchCooldown);
        tag.putInt("xwfEarthquakeSlamCD", earthquakeSlamCooldown);
        tag.putInt("xwfPowerBlockCD", powerBlockCooldown);
        tag.putBoolean("xwfAwakenSoundPlayed", awakenSoundPlayed);
        tag.putBoolean("xwfUltraCharged", this.entityData.get(ULTRA_CHARGED));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        chargedPunchCooldown = tag.getInt("xwfChargedPunchCD");
        earthquakeSlamCooldown = tag.getInt("xwfEarthquakeSlamCD");
        powerBlockCooldown = tag.getInt("xwfPowerBlockCD");
        awakenSoundPlayed = tag.getBoolean("xwfAwakenSoundPlayed");
        this.entityData.set(ULTRA_CHARGED, tag.getBoolean("xwfUltraCharged"));
    }

    // ── Protected helpers for goals ───────────────────────────────────────────

    boolean canUseChargedPunch() {
        return chargedPunchCooldown == 0 && dashingTimer == 0 && !isSlamming && !isPowerBlocking;
    }

    void startChargingPunch() {
        isChargingPunch = true;
        this.startUsingItem(InteractionHand.MAIN_HAND);
    }

    void releasePunch(Vec3 direction) {
        isChargingPunch = false;
        this.stopUsingItem();

        boolean isUltra = this.entityData.get(ULTRA_CHARGED);
        double speedMult = isUltra ? 2.5D : 1.5D;

        dashDir = new Vec3(direction.x * speedMult, 0.15D, direction.z * speedMult);
        dashingTimer = 12;
        chargedPunchCooldown = 160; // 8s
        this.removeEffect(org.xeb.xeb.effect.ModEffects.CHARGED_FIST.get());
        this.addEffect(new MobEffectInstance(org.xeb.xeb.effect.ModEffects.CHARGED_FIST.get(), 100, 0, false, false));
    }

    void startEarthquakeSlam(LivingEntity target) {
        isSlamming = true;
        slamTimer = 25;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.DRAGON_FIREBALL_EXPLODE, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.2F);

        Vec3 look = target.position().subtract(this.position()).normalize();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
        this.setDeltaMovement(forward.x * 0.8D, 0.5D, forward.z * 0.8D);
        this.hurtMarked = true;
    }

    void startPowerBlock() {
        isPowerBlocking = true;
        blockTimer = 0;
        accumulatedBlockedDamage = 0.0F;
        this.entityData.set(POWER_BLOCKING, true);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.PISTON_EXTEND, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.2F);
    }

    void stopPowerBlock() {
        isPowerBlocking = false;
        blockTimer = 0;
        this.entityData.set(POWER_BLOCKING, false);
        powerBlockCooldown = 140; // 7s
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.PISTON_CONTRACT, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.2F);
    }

    // ── Inner Goals ──────────────────────────────────────────────────────────

    /** Goal: charge up Doomfist, then dash at the target */
    static class ChargedPunchGoal extends Goal {
        private final TankWitherfistEntity mob;
        private LivingEntity target;
        private int chargeTimer = 0;
        private static final int CHARGE_DURATION = 40; // 2s charge
        private enum Phase { IDLE, CHARGING, DASHING }
        private Phase phase = Phase.IDLE;

        ChargedPunchGoal(TankWitherfistEntity mob) {
            this.mob = mob;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.forceChargedPunch) {
                mob.forceChargedPunch = false;
                LivingEntity t = mob.getTarget();
                if (t != null && t.isAlive()) {
                    target = t;
                    return true;
                }
            }
            if (!mob.canUseChargedPunch()) return false;
            LivingEntity t = mob.getTarget();
            if (t == null || !t.isAlive()) return false;
            target = t;
            double dist = mob.distanceToSqr(target);
            return dist <= 225.0D && dist >= 4.0D; // 4–15 blocks
        }

        @Override
        public boolean canContinueToUse() {
            return phase != Phase.IDLE && target != null && target.isAlive();
        }

        @Override
        public void start() {
            phase = Phase.CHARGING;
            chargeTimer = 0;
            mob.startChargingPunch();
            mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.3F);
        }

        @Override
        public void tick() {
            if (target == null) { stop(); return; }
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (phase == Phase.CHARGING) {
                chargeTimer++;
                // Stand still while charging
                mob.getNavigation().stop();

                if (chargeTimer >= CHARGE_DURATION) {
                    // Release punch toward target
                    Vec3 dir = target.position().subtract(mob.position()).normalize();
                    mob.releasePunch(dir);
                    phase = Phase.DASHING;
                }
            } else if (phase == Phase.DASHING) {
                // Dash is handled in mob.tick() — stop goal once dash ends
                if (mob.dashingTimer <= 0) {
                    phase = Phase.IDLE;
                }
            }
        }

        @Override
        public void stop() {
            phase = Phase.IDLE;
            mob.isChargingPunch = false;
            mob.stopUsingItem();
            mob.chargeTicks = 0;
        }
    }

    /** Goal: Earthquake Slam — jump + slide + slam target */
    static class EarthquakeSlamGoal extends Goal {
        private final TankWitherfistEntity mob;
        private LivingEntity target;

        EarthquakeSlamGoal(TankWitherfistEntity mob) {
            this.mob = mob;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (mob.earthquakeSlamCooldown > 0 || mob.isPowerBlocking || mob.isSlamming || mob.dashingTimer > 0 || mob.isChargingPunch) {
                return false;
            }
            LivingEntity t = mob.getTarget();
            if (t == null || !t.isAlive()) return false;
            target = t;
            double dist = mob.distanceToSqr(target);
            // Use when target is 5–20 blocks away, and we have line of sight
            return dist >= 25.0D && dist <= 400.0D && mob.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return mob.isSlamming && target != null && target.isAlive();
        }

        @Override
        public void start() {
            mob.startEarthquakeSlam(target);
        }

        @Override
        public void tick() {
            // Steering and tracking target logic is handled in mob.tick()
        }

        @Override
        public void stop() {
            mob.isSlamming = false;
            mob.slamTimer = 0;
        }
    }

    /** Goal: Power Block — mitigation and projectile absorption */
    static class PowerBlockGoal extends Goal {
        private final TankWitherfistEntity mob;
        private int blockTimer = 0;

        PowerBlockGoal(TankWitherfistEntity mob) {
            this.mob = mob;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.powerBlockCooldown > 0 || mob.isSlamming || mob.dashingTimer > 0 || mob.isChargingPunch) {
                return false;
            }
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) return false;

            boolean targetIsRanged = target.isUsingItem() && target.getUseItem().getUseAnimation() == net.minecraft.world.item.UseAnim.BOW;
            boolean underAttack = mob.getLastHurtByMob() == target && mob.tickCount - mob.getLastHurtByMobTimestamp() < 40;
            return targetIsRanged || underAttack || mob.getRandom().nextFloat() < 0.015F;
        }

        @Override
        public void start() {
            blockTimer = 0;
            mob.startPowerBlock();
        }

        @Override
        public void tick() {
            mob.getNavigation().stop();
            LivingEntity target = mob.getTarget();
            if (target != null) {
                mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            blockTimer++;
            if (blockTimer >= 40) { // 2s duration
                stop();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return mob.isPowerBlocking && blockTimer < 40 && mob.getTarget() != null && mob.getTarget().isAlive();
        }

        @Override
        public void stop() {
            mob.stopPowerBlock();
        }
    }
}
