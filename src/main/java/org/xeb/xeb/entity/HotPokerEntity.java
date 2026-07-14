package org.xeb.xeb.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;

import java.util.UUID;

public class HotPokerEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING = SynchedEntityData.defineId(HotPokerEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int attackTicks = 0;
    private boolean isAttacking = false;
    private int walkTimer = 0;
    public int teleportCooldown = 0;

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

        // Equip Trident in left hand
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TRIDENT));

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

            // Guarantee a flaming medallion matching the scaled tier.
            // N16 fix: use attachMedallion instead of directly mutating the list +
            // saveMedallions, so onAttach is always called (e.g. FlamingBuff grants
            // Fire Resistance).  We upgrade the tier if flaming is already present,
            // otherwise we attach a fresh one.
            java.util.List<MedallionData> meds = MedallionManager.getMedallions(this);
            boolean found = false;
            for (MedallionData m : meds) {
                if (m.getBuff().getId().equals("flaming")) {
                    if (m.getTier() != tier) {
                        // Re-attach with the correct tier by removing the old entry and adding new one
                        java.util.List<MedallionData> updated = new java.util.ArrayList<>(meds);
                        updated.removeIf(x -> x.getBuff().getId().equals("flaming"));
                        updated.add(new MedallionData(m.getBuff(), tier, m.getUniqueId()));
                        MedallionManager.saveMedallions(this, updated);
                        MedallionManager.refreshDimensionsIfNeeded(this, updated);
                        MedallionManager.syncToTracking(this);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                var flamingBuff = EliteBuffRegistry.getById("flaming");
                if (flamingBuff != null) {
                    // attachMedallion calls onAttach, saveMedallions, and syncToTracking
                    MedallionManager.attachMedallion(this,
                        new MedallionData(flamingBuff, tier, UUID.randomUUID()));
                }
            }

        }

        return result;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (this.teleportCooldown > 0) {
                this.teleportCooldown--;
            }
        }

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
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), org.xeb.xeb.sound.ModSounds.HOTPOKER_DMG.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos, Entity entity) {
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
            return true; // Don't replace air or indestructible blocks like bedrock
        }
        if (state.hasBlockEntity()) {
            return true; // Don't destroy chests, furnaces, or tile entities
        }
        try {
            net.minecraftforge.common.util.FakePlayer fakePlayer = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
            fakePlayer.setPos(pos.getX(), pos.getY(), pos.getZ());
            
            net.minecraftforge.event.level.BlockEvent.BreakEvent breakEvent = new net.minecraftforge.event.level.BlockEvent.BreakEvent(level, pos, state, fakePlayer);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(breakEvent);
            if (breakEvent.isCanceled()) {
                return true;
            }
        } catch (Exception e) {
            return true; // If anything fails, treat as protected
        }
        return false;
    }

    public boolean tryTeleportNearTarget(LivingEntity target) {
        if (this.level().isClientSide()) return false;
        
        double targetX = target.getX();
        double targetY = target.getY();
        double targetZ = target.getZ();
        
        RandomSource rand = this.getRandom();
        boolean success = false;
        BlockPos finalPos = null;
        
        for (int i = 0; i < 16; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double distance = 1.5D + rand.nextDouble() * 1.5D; // 1.5 to 3.0 blocks away
            double x = targetX + Math.cos(angle) * distance;
            double z = targetZ + Math.sin(angle) * distance;
            
            for (double yOffset = -3; yOffset <= 3; yOffset++) {
                double y = targetY + yOffset;
                BlockPos pos = BlockPos.containing(x, y, z);
                
                if (this.level().getBlockState(pos).isAir() && 
                    this.level().getBlockState(pos.above()).isAir() && 
                    this.level().getBlockState(pos.below()).isFaceSturdy(this.level(), pos.below(), Direction.UP)) {
                    
                    finalPos = pos;
                    success = true;
                    break;
                }
            }
            if (success) break;
        }
        
        if (success && finalPos != null) {
            double oldX = this.getX();
            double oldY = this.getY();
            double oldZ = this.getZ();
            
            this.level().playSound(null, oldX, oldY, oldZ, org.xeb.xeb.sound.ModSounds.HOTPOKER_TP.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            
            this.teleportTo(finalPos.getX() + 0.5D, finalPos.getY(), finalPos.getZ() + 0.5D);
            
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), org.xeb.xeb.sound.ModSounds.HOTPOKER_TP.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, oldX, oldY + 1.0D, oldZ, 10, 0.2, 0.5, 0.2, 0.0);
                serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.0D, this.getZ(), 15, 0.2, 0.5, 0.2, 0.0);
            }
            
            this.teleportCooldown = 100; // 5 seconds
            
            BlockPos groundPos = finalPos.below();
            if (this.level() instanceof ServerLevel serverLevel) {
                if (!isProtected(serverLevel, groundPos, this)) {
                    serverLevel.setBlockAndUpdate(groundPos, net.minecraft.world.level.block.Blocks.MAGMA_BLOCK.defaultBlockState());
                }
            }
            
            return true;
        }
        
        return false;
    }

    public static boolean checkHotPokerSpawnRules(EntityType<HotPokerEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (level instanceof ServerLevel serverLevel) {
            Holder<Biome> biomeHolder = serverLevel.getBiome(pos);
            if (biomeHolder.is(Biomes.CRIMSON_FOREST) || biomeHolder.is(Biomes.WARPED_FOREST)) {
                return Monster.checkMonsterSpawnRules(type, serverLevel, spawnType, pos, random);
            }
            if (serverLevel.dimension() == Level.OVERWORLD) {
                // N17 fix — the original 5×5×5 nested loop issued 125 getStructureWithPieceAt()
                // calls per spawn attempt, causing high overhead in chunk ticks near ruined portals.
                // A single check at the mob's exact position is sufficient because
                // getStructureWithPieceAt already queries the structure index efficiently.
                var ruinedPortalKey = net.minecraft.resources.ResourceKey.create(
                    Registries.STRUCTURE,
                    new net.minecraft.resources.ResourceLocation("minecraft", "ruined_portal"));
                if (serverLevel.structureManager()
                        .getStructureWithPieceAt(pos, ruinedPortalKey).isValid()) {
                    return Monster.checkMonsterSpawnRules(type, serverLevel, spawnType, pos, random);
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
        private boolean forceNextAttack = false;

        public HotPokerMeleeAttackGoal(HotPokerEntity mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(mob, speedModifier, followingTargetEvenIfNotSeen);
            this.poker = mob;
        }

        @Override
        public void tick() {
            LivingEntity enemy = this.poker.getTarget();
            if (enemy != null && enemy.isAlive()) {
                double distToEnemySqr = this.poker.distanceToSqr(enemy);
                double reach = this.getAttackReachSqr(enemy);
                if (distToEnemySqr > reach && this.poker.teleportCooldown == 0) {
                    boolean teleported = this.poker.tryTeleportNearTarget(enemy);
                    if (teleported) {
                        this.forceNextAttack = true;
                    }
                }
            }
            super.tick();
        }

        @Override
        protected boolean isTimeToAttack() {
            return this.forceNextAttack || super.isTimeToAttack();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
            double reach = this.getAttackReachSqr(enemy);
            if (distToEnemySqr <= reach && this.isTimeToAttack()) {
                this.forceNextAttack = false;
                this.resetAttackCooldown();
                this.poker.startAttackAnimation();
            }
        }
    }
}
