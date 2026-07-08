package org.xeb.xeb.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import java.util.UUID;

public class HotPokerEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING = SynchedEntityData.defineId(HotPokerEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int attackTicks = 0;
    private boolean isAttacking = false;
    private int walkTimer = 0;

    public HotPokerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_WALKING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D) // 8 hearts
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D) // 2 hearts
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new HotPokerMeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data, dataTag);

        // Equip Trident in right hand
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));

        if (level instanceof ServerLevel serverLevel) {
            int targetLevel = 1;
            double radius = org.xeb.xeb.Config.playerScanRadius;
            net.minecraft.world.phys.AABB searchBox = this.getBoundingBox().inflate(radius);
            java.util.List<Player> players = serverLevel.getEntitiesOfClass(Player.class, searchBox, p -> !p.isSpectator() && !p.isCreative());
            if (!players.isEmpty()) {
                int maxLvl = 1;
                for (Player p : players) {
                    int pLvl = MedallionManager.getEliteMeterLevel(p);
                    if (pLvl > maxLvl) {
                        maxLvl = pLvl;
                    }
                }
                targetLevel = maxLvl;
            }

            MedallionType tier = MedallionType.COMMON;
            if (targetLevel >= 7) {
                tier = MedallionType.LEGENDARY;
            } else if (targetLevel >= 4) {
                tier = MedallionType.RARE;
            }

            // Assign standard random medallions first
            MedallionManager.assignRandomMedallions(this, serverLevel);

            // Guarantee a flaming medallion matching the scaled tier
            java.util.List<MedallionData> meds = new java.util.ArrayList<>(MedallionManager.getMedallions(this));
            boolean found = false;
            for (int i = 0; i < meds.size(); i++) {
                MedallionData m = meds.get(i);
                if (m.getBuff().getId().equals("flaming")) {
                    meds.set(i, new MedallionData(m.getBuff(), tier, m.getUniqueId()));
                    found = true;
                    break;
                }
            }
            if (!found) {
                var flamingBuff = EliteBuffRegistry.getById("flaming");
                if (flamingBuff != null) {
                    meds.add(new MedallionData(flamingBuff, tier, UUID.randomUUID()));
                }
            }
            MedallionManager.saveMedallions(this, meds);
            MedallionManager.refreshDimensionsIfNeeded(this, meds);
            MedallionManager.syncToTracking(this);
        }

        return result;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.isAttacking) {
            this.attackTicks++;
            if (this.attackTicks == 10) { // Deal damage at exactly 0.5s
                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive() && this.distanceToSqr(target) <= 9.0D) {
                    this.doHurtTarget(target);
                }
            }
            if (this.attackTicks >= 20) {
                this.isAttacking = false;
                this.attackTicks = 0;
            }
        }

        // Handle step sounds manually synced to walking animation
        if (!this.level().isClientSide()) {
            boolean isAttemptingToMove = this.getNavigation().isInProgress() || Math.abs(this.zza) > 0.001F || Math.abs(this.xxa) > 0.001F;
            this.entityData.set(DATA_IS_WALKING, isAttemptingToMove);

            if (isAttemptingToMove) {
                this.walkTimer++;
                if (this.walkTimer >= 20) {
                    this.walkTimer = 0;
                }
                // Step at 0.21s (tick 4) and 0.42s (tick 8)
                if (this.onGround() && (this.walkTimer == 4 || this.walkTimer == 8)) {
                    BlockPos stepPos = this.blockPosition().below();
                    this.playStepSound(stepPos, this.level().getBlockState(stepPos));
                }
            } else {
                this.walkTimer = 0;
            }
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        // Suppressed vanilla step sounds as we play them manually
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
            boolean isAttemptingToMove = this.getNavigation().isInProgress() || Math.abs(this.zza) > 0.001F || Math.abs(this.xxa) > 0.001F;
            if (this.onGround() && isAttemptingToMove) {
                int tickInCycle = this.walkTimer % 20;
                boolean canMove = (tickInCycle >= 4 && tickInCycle <= 5) || (tickInCycle >= 8 && tickInCycle <= 9);
                if (!canMove) {
                    super.travel(new net.minecraft.world.phys.Vec3(0, travelVector.y, 0));
                    this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                    return;
                } else if (tickInCycle == 4 || tickInCycle == 8) {
                    // Micro jump: push the mob slightly upward and forward to gain more step distance
                    net.minecraft.world.phys.Vec3 currentVel = this.getDeltaMovement();
                    this.setDeltaMovement(currentVel.x * 1.8D, 0.18D, currentVel.z * 1.8D);
                }
            }
        }
        super.travel(travelVector);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean flag = super.doHurtTarget(target);
        if (flag && target instanceof LivingEntity livingTarget) {
            // Apply Charring Burn for 2 seconds (40 ticks)
            livingTarget.addEffect(new MobEffectInstance(ModEffects.BURN.get(), 40, 0));
        }
        return flag;
    }

    public void startAttackAnimation() {
        if (!this.level().isClientSide()) {
            this.triggerAnim("attack_controller", "attack");
            this.isAttacking = true;
            this.attackTicks = 0;
        }
    }

    public static boolean checkHotPokerSpawnRules(EntityType<HotPokerEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (level instanceof ServerLevel serverLevel) {
            Holder<Biome> biomeHolder = serverLevel.getBiome(pos);
            if (biomeHolder.is(Biomes.CRIMSON_FOREST) || biomeHolder.is(Biomes.WARPED_FOREST)) {
                return Monster.checkMonsterSpawnRules(type, serverLevel, spawnType, pos, random);
            }
            if (serverLevel.dimension() == Level.OVERWORLD) {
                var ruinedPortalKey = net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, new net.minecraft.resources.ResourceLocation("minecraft", "ruined_portal"));
                for (int x = -16; x <= 16; x += 8) {
                    for (int z = -16; z <= 16; z += 8) {
                        for (int y = -8; y <= 8; y += 4) {
                            if (serverLevel.structureManager().getStructureWithPieceAt(pos.offset(x, y, z), ruinedPortalKey).isValid()) {
                                return Monster.checkMonsterSpawnRules(type, serverLevel, spawnType, pos, random);
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Idle is base layer, always active and never deactivated
        controllers.add(new AnimationController<>(this, "idle_controller", 0, event -> {
            return event.setAndContinue(RawAnimation.begin().thenLoop("Idle"));
        }));

        controllers.add(new AnimationController<>(this, "walk_controller", 5, event -> {
            if (this.entityData.get(DATA_IS_WALKING)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "attack_controller", 5, event -> {
            return PlayState.STOP;
        }).triggerableAnim("attack", RawAnimation.begin().thenPlay("attack")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    static class HotPokerMeleeAttackGoal extends MeleeAttackGoal {
        private final HotPokerEntity poker;

        public HotPokerMeleeAttackGoal(HotPokerEntity mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(mob, speedModifier, followingTargetEvenIfNotSeen);
            this.poker = mob;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
            double reach = this.getAttackReachSqr(enemy);
            if (distToEnemySqr <= reach && this.isTimeToAttack()) {
                this.resetAttackCooldown();
                this.poker.startAttackAnimation();
            }
        }
    }
}
