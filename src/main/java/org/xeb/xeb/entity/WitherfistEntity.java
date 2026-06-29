package org.xeb.xeb.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
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

public class WitherfistEntity extends WitherSkeleton {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("Witherfist"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_6
    );

    // Ability cooldown trackers (in ticks)
    private int chargedPunchCooldown = 0;   // 8s = 160 ticks
    private int risingUppercutCooldown = 0; // 6s = 120 ticks
    private int seismicSlamCooldown = 0;    // 9s = 180 ticks

    // State tracking
    private int dashingTimer = 0;    // active dash ticks remaining
    private Vec3 dashDir = Vec3.ZERO;
    private boolean isChargingPunch = false;
    private int chargeTicks = 0;     // how long it has been charging (max 40 for full charge)

    private int uppercutTimer = 0;   // frames player floats
    private boolean isSeismicDiving = false;
    private Vec3 seismicTarget = Vec3.ZERO;
    private boolean awakenSoundPlayed = false;

    public WitherfistEntity(EntityType<? extends WitherSkeleton> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("Witherfist"));
        this.setCustomNameVisible(false); // shown via bossbar instead
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
        this.goalSelector.addGoal(3, new RisingUppercutGoal(this));
        this.goalSelector.addGoal(4, new SeismicSlamGoal(this));
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

        // Equip Doomfist v1 in main hand
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DOOMFIST.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 1.0F); // guaranteed drop

        // Clear armour slots (no visible armour)
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);

        // Medallion setup
        if (level instanceof ServerLevel serverLevel) {
            MedallionManager.assignRandomMedallions(this, serverLevel);
            // Guarantee Speedy LEGENDARY medallion
            if (!MedallionManager.hasBuff(this, "speedy")) {
                EliteBuffRegistry.getById("speedy");
                var speedyBuff = EliteBuffRegistry.getById("speedy");
                if (speedyBuff != null) {
                    MedallionManager.attachMedallion(this, new MedallionData(speedyBuff, MedallionType.LEGENDARY, UUID.randomUUID()));
                }
            }
        }

        return result;
    }

    // ── Main tick ──────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Tick down cooldowns (halved at <= 50% HP)
        int cdTick = (this.getHealth() <= this.getMaxHealth() / 2.0F) ? 2 : 1;
        if (chargedPunchCooldown > 0) chargedPunchCooldown = Math.max(0, chargedPunchCooldown - cdTick);
        if (risingUppercutCooldown > 0) risingUppercutCooldown = Math.max(0, risingUppercutCooldown - cdTick);
        if (seismicSlamCooldown > 0) seismicSlamCooldown = Math.max(0, seismicSlamCooldown - cdTick);

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
                float damage = 7.0F + chargeRatio * 15.0F;
                for (LivingEntity target : hit) {
                    target.hurt(this.damageSources().mobAttack(this), damage);
                    Vec3 look = this.getLookAngle();
                    target.knockback(1.5D + chargeRatio * 2.0D, -look.x, -look.z);
                    target.hurtMarked = true;
                }
                dashingTimer = 0; // stop dash on first hit
            }
        }

        // Uppercut float tick (for self)
        if (uppercutTimer > 0) {
            uppercutTimer--;
            Vec3 vel = this.getDeltaMovement();
            this.setDeltaMovement(vel.x, Math.max(-0.08D, vel.y), vel.z);
            this.hurtMarked = true;
        }

        // Seismic slam diagonal dive
        if (isSeismicDiving) {
            Vec3 slamVec = seismicTarget.subtract(this.position());
            if (slamVec.lengthSqr() > 0.01D) {
                Vec3 dir = slamVec.normalize();
                this.setDeltaMovement(dir.scale(1.5D));
                this.hurtMarked = true;
            }

            double distToTargetSq = this.position().distanceToSqr(seismicTarget);
            boolean hitGround = this.onGround();
            if (!hitGround) {
                AABB check = this.getBoundingBox().move(0, -0.3D, 0);
                if (!this.level().noCollision(this, check)) hitGround = true;
            }

            if (hitGround || distToTargetSq <= 2.25D) {
                isSeismicDiving = false;
                executeSeismicImpact();
                seismicSlamCooldown = 180;
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

    // ── Ability execution helpers ─────────────────────────────────────────────

    /** Rises upward, launches nearest player up, both float for 1.5s */
    private void executeRisingUppercut() {
        this.setDeltaMovement(this.getDeltaMovement().x, 1.1D, this.getDeltaMovement().z);
        this.hurtMarked = true;
        uppercutTimer = 30; // 1.5s float

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.6F);

        AABB area = this.getBoundingBox().inflate(2.5D, 1.0D, 2.5D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this && e.isAlive() && !e.isAlliedTo(this));
        for (LivingEntity target : targets) {
            target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            target.setDeltaMovement(target.getDeltaMovement().x, 1.0D, target.getDeltaMovement().z);
            target.hurtMarked = true;
            
            // Set floating tag on target just like player's rising uppercut!
            target.getPersistentData().putInt("xebUppercutFloatTicks", 40);
        }

        risingUppercutCooldown = 120;

        // INSTANTLY prepare a fully-charged punch for follow-up combo!
        this.chargedPunchCooldown = 0;
        this.isChargingPunch = true;
        this.chargeTicks = 40;
        this.addEffect(new MobEffectInstance(org.xeb.xeb.effect.ModEffects.CHARGED_FIST.get(), 600, 0, false, false));
    }

    /** Impact from above — cone damage in front */
    private void executeSeismicImpact() {
        Vec3 look = this.getLookAngle();
        Vec3 horizLook = new Vec3(look.x, 0, look.z).normalize();
        Vec3 pos = this.position();

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.7F);

        // Shockwave particles
        if (this.level() instanceof ServerLevel sLevel) {
            sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY(), this.getZ(), 3, 0, 0, 0, 0);
            var redDust = new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.5F, 0.0F, 0.0F), 2.0F);
            for (int step = 1; step <= 5; step++) {
                double sx = pos.x + horizLook.x * step;
                double sz = pos.z + horizLook.z * step;
                sLevel.sendParticles(redDust, sx, pos.y + 0.2D, sz, 8, 0.5, 0.1, 0.5, 0.0);
            }

            // Circular POOF dust shockwave particles expanding outward
            for (int i = 0; i < 16; i++) {
                double angle = (i * Math.PI * 2.0D) / 16.0D;
                double dx = Math.cos(angle) * 1.5D;
                double dz = Math.sin(angle) * 1.5D;
                sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, this.getX() + dx, this.getY() + 0.1D, this.getZ() + dz, 1, 0.1D, 0.0D, 0.1D, 0.02D);
            }
        }

        AABB area = this.getBoundingBox().inflate(6.0D, 3.0D, 6.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this && e.isAlive() && !e.isAlliedTo(this));
        for (LivingEntity target : targets) {
            Vec3 offset = target.position().subtract(pos);
            double dist = offset.horizontalDistance();
            if (dist > 6.0D) continue;
            Vec3 dir = new Vec3(offset.x, 0, offset.z).normalize();
            double dot = horizLook.dot(dir);
            if (dot < 0.7D) continue; // ~45° cone
            target.hurt(this.damageSources().mobAttack(this),
                    (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2D));
            Vec3 pull = pos.subtract(target.position()).normalize();
            target.setDeltaMovement(pull.x * 0.4D, 0.5D, pull.z * 0.4D);
            target.hurtMarked = true;
        }
    }

    // ── Drop logic ────────────────────────────────────────────────────────────

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        // Doomfist v1 drop is handled by drop chance 1.0 set in finalizeSpawn
    }

    // ── Save/Load ─────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("xwfChargedPunchCD", chargedPunchCooldown);
        tag.putInt("xwfUppercutCD", risingUppercutCooldown);
        tag.putInt("xwfSeismicCD", seismicSlamCooldown);
        tag.putBoolean("xwfAwakenSoundPlayed", awakenSoundPlayed);
        tag.putBoolean("xwfSeismicDiving", isSeismicDiving);
        if (isSeismicDiving) {
            tag.putDouble("xwfSeismicTargetX", seismicTarget.x);
            tag.putDouble("xwfSeismicTargetY", seismicTarget.y);
            tag.putDouble("xwfSeismicTargetZ", seismicTarget.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        chargedPunchCooldown = tag.getInt("xwfChargedPunchCD");
        risingUppercutCooldown = tag.getInt("xwfUppercutCD");
        seismicSlamCooldown = tag.getInt("xwfSeismicCD");
        awakenSoundPlayed = tag.getBoolean("xwfAwakenSoundPlayed");
        isSeismicDiving = tag.getBoolean("xwfSeismicDiving");
        if (isSeismicDiving) {
            seismicTarget = new Vec3(
                tag.getDouble("xwfSeismicTargetX"),
                tag.getDouble("xwfSeismicTargetY"),
                tag.getDouble("xwfSeismicTargetZ")
            );
        }
    }

    // ── Protected helpers for goals ───────────────────────────────────────────

    boolean canUseChargedPunch() { return chargedPunchCooldown == 0 && dashingTimer == 0 && !isSeismicDiving; }
    boolean canUseRisingUppercut() { return risingUppercutCooldown == 0 && dashingTimer == 0 && !isSeismicDiving; }
    boolean canUseSeismicSlam() { return seismicSlamCooldown == 0 && dashingTimer == 0 && uppercutTimer == 0 && !isSeismicDiving; }

    void startChargingPunch() { isChargingPunch = true; }
    void releasePunch(Vec3 direction) {
        isChargingPunch = false;
        dashDir = new Vec3(direction.x * 1.5D, 0.15D, direction.z * 1.5D);
        dashingTimer = 12;
        chargedPunchCooldown = 160; // 8s
        this.removeEffect(org.xeb.xeb.effect.ModEffects.CHARGED_FIST.get());
        this.addEffect(new MobEffectInstance(org.xeb.xeb.effect.ModEffects.CHARGED_FIST.get(), 100, 0, false, false));
    }
    void triggerRisingUppercut() { executeRisingUppercut(); }


    int getChargeTicks() { return chargeTicks; }

    // ══════════════════════════════════════════════════════════════════════════
    // INNER GOAL CLASSES
    // ══════════════════════════════════════════════════════════════════════════

    /** Goal: charge up Doomfist, then dash at the player */
    static class ChargedPunchGoal extends Goal {
        private final WitherfistEntity mob;
        private LivingEntity target;
        private int chargeTimer = 0;
        private static final int CHARGE_DURATION = 40; // 2s charge
        private enum Phase { IDLE, CHARGING, DASHING }
        private Phase phase = Phase.IDLE;

        ChargedPunchGoal(WitherfistEntity mob) {
            this.mob = mob;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!mob.canUseChargedPunch()) return false;
            LivingEntity t = mob.getTarget();
            if (t == null || !t.isAlive()) return false;
            target = t;
            double dist = mob.distanceToSqr(target);
            if (mob.isChargingPunch && mob.chargeTicks >= 40) {
                return dist <= 225.0D;
            }
            return dist <= 225.0D && dist >= 4.0D; // 4–15 blocks
        }

        @Override
        public boolean canContinueToUse() {
            return phase != Phase.IDLE && target != null && target.isAlive();
        }

        @Override
        public void start() {
            if (mob.isChargingPunch && mob.chargeTicks >= 40) {
                phase = Phase.CHARGING;
                chargeTimer = CHARGE_DURATION;
            } else {
                phase = Phase.CHARGING;
                chargeTimer = 0;
                mob.startChargingPunch();
                mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_CHARGE,
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.3F);
            }
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
            mob.chargeTicks = 0;
        }
    }

    /** Goal: Rising Uppercut — jump + launch nearby player */
    static class RisingUppercutGoal extends Goal {
        private final WitherfistEntity mob;
        private LivingEntity target;
        private int windupTimer = 0;
        private boolean triggered = false;

        RisingUppercutGoal(WitherfistEntity mob) {
            this.mob = mob;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!mob.canUseRisingUppercut()) return false;
            LivingEntity t = mob.getTarget();
            if (t == null || !t.isAlive()) return false;
            target = t;
            // Use when target is within 3 blocks (close quarters combo finisher)
            return mob.distanceToSqr(target) <= 16.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return !triggered && target != null && target.isAlive();
        }

        @Override
        public void start() {
            windupTimer = 0;
            triggered = false;
        }

        @Override
        public void tick() {
            if (target == null) { stop(); return; }
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            mob.getNavigation().stop();
            windupTimer++;
            // Short windup of 10 ticks then execute
            if (windupTimer >= 10 && !triggered) {
                mob.triggerRisingUppercut();
                triggered = true;
            }
        }

        @Override
        public void stop() {
            triggered = true;
        }
    }

    /** Goal: Seismic Slam — dive diagonally at player and drop with cone impact */
    static class SeismicSlamGoal extends Goal {
        private final WitherfistEntity mob;
        private LivingEntity target;
        private int windupTimer = 0;
        private boolean dived = false;

        SeismicSlamGoal(WitherfistEntity mob) {
            this.mob = mob;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!mob.canUseSeismicSlam()) return false;
            LivingEntity t = mob.getTarget();
            if (t == null || !t.isAlive()) return false;
            target = t;
            double dist = mob.distanceToSqr(target);
            // 4 to 15 blocks range (16 to 225 distSqr)
            return dist >= 16.0D && dist <= 225.0D && mob.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return !dived || mob.isSeismicDiving;
        }

        @Override
        public void start() {
            windupTimer = 0;
            dived = false;
            mob.getNavigation().stop();
            mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.5F);
        }

        @Override
        public void tick() {
            if (target == null) { stop(); return; }
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            mob.getNavigation().stop();

            if (!dived) {
                windupTimer++;
                if (windupTimer >= 10) { // 0.5s windup
                    // Launch diagonal dive towards player target
                    mob.seismicTarget = target.position();
                    mob.isSeismicDiving = true;
                    dived = true;
                }
            }
        }

        @Override
        public void stop() {
            dived = true;
        }
    }
}
