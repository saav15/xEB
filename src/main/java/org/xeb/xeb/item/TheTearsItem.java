package org.xeb.xeb.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.xeb.xeb.entity.CrazyDiamondEntity;
import org.xeb.xeb.entity.ModEntities;
import org.xeb.xeb.entity.TearsProjectileEntity;
import org.xeb.xeb.network.BrimstoneBeamPacket;
import org.xeb.xeb.network.TearsSyncPacket;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.opticblast.ActiveBeamManager;
import org.xeb.xeb.opticblast.BeamData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TheTearsItem extends Item {
    private static final Map<UUID, Long> lastHealTicks = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastBackstabSoundTicks = new ConcurrentHashMap<>();

    public TheTearsItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(ChatFormatting.RED);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // standard holding duration
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Cooldown or constraints
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        CompoundTag tag = player.getPersistentData();
        if (tag.getBoolean("xebNextBrimstoneInstant")) {
            tag.putBoolean("xebNextBrimstoneInstant", false);
            tag.putInt("xebBrimstoneCharge", 0);
            tag.putInt("xebBrimstoneFiringTicks", 40); // 2s duration
            if (!level.isClientSide()) {
                level.playSound(null, player, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.8F);
                level.playSound(null, player, SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            return InteractionResultHolder.success(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        CompoundTag tag = player.getPersistentData();
        int charge = tag.getInt("xebBrimstoneCharge");
        if (charge >= 80) {
            tag.putInt("xebBrimstoneCharge", 0);
            tag.putInt("xebBrimstoneFiringTicks", 40); // 2s duration
            if (!level.isClientSide()) {
                level.playSound(null, player, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.8F);
                level.playSound(null, player, SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
        } else {
            tag.putInt("xebBrimstoneCharge", 0);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) return;

        boolean holdsMain = player.getMainHandItem().is(this);
        boolean holdsOff = player.getOffhandItem().is(this);
        boolean isEquipped = holdsMain || holdsOff;

        // Ensure we only tick once per player even if multiple slots hold the item
        if (isEquipped && ((holdsMain && stack == player.getMainHandItem()) || (!holdsMain && holdsOff && stack == player.getOffhandItem()))) {
            long currentTick = level.getGameTime();

            // --- 1. Passive 1 (Passive Regeneration) ---
            if (!level.isClientSide()) {
                long lastHeal = lastHealTicks.getOrDefault(player.getUUID(), 0L);
                if (currentTick - lastHeal >= 1200) { // 60 seconds
                    player.heal(2.0F); // 1 heart
                    lastHealTicks.put(player.getUUID(), currentTick);
                }
            }

            // --- 2. Decr cooldowns & element timers ---
            CompoundTag tag = player.getPersistentData();
            if (tag.getInt("xebTearsA1Cooldown") > 0) {
                tag.putInt("xebTearsA1Cooldown", tag.getInt("xebTearsA1Cooldown") - 1);
            }
            if (tag.getInt("xebTearsA2Cooldown") > 0) {
                tag.putInt("xebTearsA2Cooldown", tag.getInt("xebTearsA2Cooldown") - 1);
            }

            int imbueDur = tag.getInt("xebTearsImbueDuration");
            if (imbueDur > 0) {
                tag.putInt("xebTearsImbueDuration", imbueDur - 1);
                if (imbueDur - 1 <= 0) {
                    tag.putInt("xebTearsImbueType", TearsProjectileEntity.IMBUE_NONE);
                }
            }

            // --- 3. Active 2 (Camo Undies burst spawn queue) ---
            int burstCount = tag.getInt("xebTearsBurstCount");
            if (burstCount > 0) {
                int burstTimer = tag.getInt("xebTearsBurstTimer") + 1;
                if (burstTimer >= 2) {
                    tag.putInt("xebTearsBurstCount", burstCount - 1);
                    tag.putInt("xebTearsBurstTimer", 0);
                    if (!level.isClientSide()) {
                        fireSingleTear(player, level, true);
                    }
                } else {
                    tag.putInt("xebTearsBurstTimer", burstTimer);
                }
            }

            // --- 4. Right-Click Brimstone charge ---
            boolean isUsing = player.isUsingItem() && player.getUseItem().is(this);
            int charge = tag.getInt("xebBrimstoneCharge");

            if (isUsing) {
                if (charge < 80) {
                    charge++;
                    tag.putInt("xebBrimstoneCharge", charge);
                }
            } else {
                // If not using and not firing, reset charge
                if (tag.getInt("xebBrimstoneFiringTicks") <= 0 && charge > 0) {
                    tag.putInt("xebBrimstoneCharge", 0);
                }
            }

            // Sync all states to client
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new TearsSyncPacket(
                                tag.getInt("xebTearsA1Cooldown"),
                                tag.getInt("xebTearsA2Cooldown"),
                                tag.getInt("xebTearsImbueType"),
                                tag.getInt("xebTearsImbueDuration"),
                                tag.getInt("xebBrimstoneCharge"),
                                tag.getInt("xebBrimstoneFiringTicks"),
                                tag.getBoolean("xebNextBrimstoneInstant")
                        )
                );
            }

            // --- 5. Ticking active Brimstone Laser ---
            int firingTicks = tag.getInt("xebBrimstoneFiringTicks");
            if (firingTicks > 0) {
                tag.putInt("xebBrimstoneFiringTicks", firingTicks - 1);
                if (!level.isClientSide()) {
                    tickBrimstoneLaser(player, level, currentTick);
                }
            } else {
                if (firingTicks == 0 && tag.contains("xebBrimstoneFiringTicks")) {
                    tag.remove("xebBrimstoneFiringTicks");
                    if (!level.isClientSide()) {
                        // Send stop packet
                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new BrimstoneBeamPacket(player.getId(), false, TearsProjectileEntity.IMBUE_NONE, Collections.emptyList()));
                    }
                }
            }
        }
    }

    private void fireSingleTear(Player player, Level level, boolean isBurst) {
        CompoundTag tag = player.getPersistentData();
        boolean leftEye = tag.getBoolean("xebTearsLeftEye");
        tag.putBoolean("xebTearsLeftEye", !leftEye);

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 upVec = new Vec3(0, 1, 0);
        Vec3 rightVec = lookVec.cross(upVec).normalize();

        // Calculate offset (0.3 blocks left/right)
        Vec3 eyeOffset = rightVec.scale(leftEye ? -0.3D : 0.3D);
        Vec3 spawnPos = eyePos.add(eyeOffset);

        int imbue = tag.getInt("xebTearsImbueType");
        
        // Spawn TearsProjectileEntity
        TearsProjectileEntity tear = new TearsProjectileEntity(level, player, imbue, false);
        tear.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
        
        // Fire vector
        Vec3 motion = lookVec.scale(1.5D);
        if (isBurst) {
            // Add a small horizontal angle spread for rapid fire chain look
            double spread = (player.getRandom().nextDouble() - 0.5D) * 0.15D;
            motion = motion.add(rightVec.scale(spread));
        }
        tear.setDeltaMovement(motion);
        
        level.addFreshEntity(tear);

        level.playSound(null, player, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    private void tickBrimstoneLaser(Player player, Level level, long currentTick) {
        CompoundTag tag = player.getPersistentData();
        int imbue = tag.getInt("xebTearsImbueType");

        // Firing from the mouth
        Vec3 mouthPos = player.getEyePosition(1.0F).subtract(0, 0.15D, 0);
        Vec3 lookDir = player.getLookAngle();

        List<Vec3> points = new ArrayList<>();
        points.add(mouthPos);

        Set<LivingEntity> hitEntities = new HashSet<>();
        Vec3 finalCollisionPoint = null;

        // Brimstone Morado (Purple): curves and loops targets
        if (imbue == TearsProjectileEntity.IMBUE_PURPLE) {
            Vec3 currentPos = mouthPos;
            Vec3 currentDir = lookDir;
            double segmentLength = 2.0D;
            int maxSegments = 20;

            for (int i = 0; i < maxSegments; i++) {
                Vec3 nextPos = currentPos.add(currentDir.scale(segmentLength));

                BlockHitResult blockHit = level.clip(new ClipContext(
                        currentPos, nextPos,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                ));

                Vec3 segmentEnd = (blockHit.getType() != HitResult.Type.MISS) ? blockHit.getLocation() : nextPos;

                // Find entities near segmentEnd
                AABB sweepBox = new AABB(currentPos, segmentEnd).inflate(1.5D);
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                        e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable() 
                             && !hitEntities.contains(e) && !(e instanceof CrazyDiamondEntity));

                if (!entities.isEmpty() && hitEntities.size() < 5) {
                    LivingEntity closest = null;
                    double closestDist = Double.MAX_VALUE;
                    for (LivingEntity e : entities) {
                        double d = currentPos.distanceToSqr(e.position());
                        if (d < closestDist) {
                            closestDist = d;
                            closest = e;
                        }
                    }

                    if (closest != null) {
                        hitEntities.add(closest);
                        // Bend next segment towards entity center
                        Vec3 targetCenter = closest.getBoundingBox().getCenter();
                        currentDir = targetCenter.subtract(currentPos).normalize();
                        
                        nextPos = currentPos.add(currentDir.scale(segmentLength));
                        blockHit = level.clip(new ClipContext(
                                currentPos, nextPos,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                player
                        ));
                        segmentEnd = (blockHit.getType() != HitResult.Type.MISS) ? blockHit.getLocation() : nextPos;
                    }
                }

                points.add(segmentEnd);
                currentPos = segmentEnd;

                if (blockHit.getType() != HitResult.Type.MISS) {
                    finalCollisionPoint = segmentEnd;
                    break;
                }
            }
        } else {
            // Straight Beam
            double reach = 40.0D;
            Vec3 beamEnd = mouthPos.add(lookDir.scale(reach));

            BlockHitResult blockHit = level.clip(new ClipContext(
                    mouthPos, beamEnd,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            Vec3 effectiveEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : beamEnd;

            // Entity sweep along the beam (hitbox scaled up to 1.5x1.5 blocks = 0.75F inflation)
            AABB sweepBox = new AABB(mouthPos, effectiveEnd).inflate(0.75D);
            List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                    e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable() && !(e instanceof CrazyDiamondEntity));

            LivingEntity closestTarget = null;
            double closestDist = Double.MAX_VALUE;
            for (LivingEntity target : list) {
                // Ray-to-entity-aabb check
                AABB aabb = target.getBoundingBox().inflate(0.75D);
                Optional<Vec3> clip = aabb.clip(mouthPos, effectiveEnd);
                if (clip.isPresent()) {
                    double dist = mouthPos.distanceToSqr(clip.get());
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestTarget = target;
                        effectiveEnd = clip.get();
                    }
                }
            }

            if (closestTarget != null) {
                hitEntities.add(closestTarget);
            }

            points.add(effectiveEnd);
            finalCollisionPoint = effectiveEnd;
        }

        // --- 1. Apply Damage & Special element ticks ---
        for (LivingEntity target : hitEntities) {
            float baseDmg = 10.0F; // Brimstone deals double Optic Blast damage (5.0F * 2)
            boolean isBack = isBackstab(player, target);
            if (isBack) {
                baseDmg *= 1.05F; // +5% damage multiplier
                playBackstabSound(player, target, currentTick);
            }

            target.hurt(player.damageSources().playerAttack(player), baseDmg);

            // White imbuement: tick lightning strike
            if (imbue == TearsProjectileEntity.IMBUE_WHITE) {
                if (player.getRandom().nextFloat() <= 0.08F) {
                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                    if (lightning != null) {
                        lightning.moveTo(target.position());
                        level.addFreshEntity(lightning);
                    }
                }
            } else if (imbue == TearsProjectileEntity.IMBUE_DARK) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            } else if (imbue == TearsProjectileEntity.IMBUE_COLD) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3)); // Slowness IV
            }
        }

        // --- 2. Check Beam vs Beam Collision in ActiveBeamManager ---
        UUID playerUUID = player.getUUID();
        // Register unique key for the player's Brimstone beam
        UUID brimstoneUUID = new UUID(playerUUID.getMostSignificantBits(), playerUUID.getLeastSignificantBits() ^ 9999);
        
        Vec3 renderEnd = points.get(points.size() - 1);
        Vec3 beamCollision = ActiveBeamManager.get().checkBeamVsBeamCollision(playerUUID, mouthPos, renderEnd);

        if (beamCollision != null) {
            double collisionDist = mouthPos.distanceToSqr(beamCollision);
            double currentEndDist = mouthPos.distanceToSqr(renderEnd);
            if (collisionDist < currentEndDist) {
                // Truncate rendering
                points.set(points.size() - 1, beamCollision);
                
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLASH, beamCollision.x, beamCollision.y, beamCollision.z,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, beamCollision.x, beamCollision.y, beamCollision.z,
                            8, 0.3D, 0.3D, 0.3D, 0.05D);
                }
            }
        }

        // --- 3. Register Beam data ---
        int color = 0xFF0000;
        if (imbue == TearsProjectileEntity.IMBUE_PURPLE) color = 0xB000FF;
        else if (imbue == TearsProjectileEntity.IMBUE_WHITE) color = 0xFFFFFF;
        else if (imbue == TearsProjectileEntity.IMBUE_DARK) color = 0x222222;
        else if (imbue == TearsProjectileEntity.IMBUE_COLD) color = 0x90E0FF;

        BeamData beamData = new BeamData(
                playerUUID,
                player.getId(),
                mouthPos,
                points.get(points.size() - 1),
                color,
                currentTick,
                currentTick + 1
        );
        ActiveBeamManager.get().putBeam(brimstoneUUID, beamData);

        // --- 4. Send network sync packet to client ---
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BrimstoneBeamPacket(player.getId(), true, imbue, points));

        // --- 5. Spawn mouth particles ---
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, mouthPos.x, mouthPos.y, mouthPos.z,
                    2, 0.1D, 0.1D, 0.1D, 0.02D);
        }
    }

    public static boolean isBackstab(LivingEntity attacker, LivingEntity target) {
        Vec3 attackerLook = attacker.getLookAngle();
        Vec3 targetLook = target.getLookAngle();
        return attackerLook.dot(targetLook) > 0.5D; // victim facing away
    }

    private static void playBackstabSound(Player player, LivingEntity target, long currentTick) {
        long lastSound = lastBackstabSoundTicks.getOrDefault(target.getUUID(), 0L);
        if (currentTick - lastSound >= 20) { // 1 second cooldown per target
            target.level().playSound(null, target, SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 1.0F, 1.5F);
            lastBackstabSoundTicks.put(target.getUUID(), currentTick);
        }
    }

    public void triggerLeftClick(Player player, Level level) {
        if (!level.isClientSide()) {
            long lastTearTime = player.getPersistentData().getLong("xebLastTearShootTime");
            long currentTick = level.getGameTime();
            if (currentTick - lastTearTime >= 8) { // 0.4 seconds (8 ticks)
                player.getPersistentData().putLong("xebLastTearShootTime", currentTick);
                fireSingleTear(player, level, false);
                player.swing(InteractionHand.MAIN_HAND, true);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.the_tears.desc1"));
        tooltip.add(Component.translatable("item.xeb.the_tears.desc2"));
        tooltip.add(Component.translatable("item.xeb.the_tears.activa1", Component.keybind("key.xeb.activa_1")));
        tooltip.add(Component.translatable("item.xeb.the_tears.activa2", Component.keybind("key.xeb.activa_2")));
        tooltip.add(Component.translatable("item.xeb.the_tears.lore"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
