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
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.function.Consumer;

public class TheTearsItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TheTearsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, event -> {
            net.minecraft.world.entity.Entity entity = event.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
            LivingEntity wielder = resolveWielder(entity);
            if (wielder instanceof Player player) {
                int charge = player.getPersistentData().getInt("xebBrimstoneCharge");
                
                // Si está cargando el brimstone, dejar en loop la animación de carga
                if (charge > 0) {
                    String loopAnim = player.getPersistentData().getString("xebTearsBrimstoneLoopAnim");
                    if (loopAnim.isEmpty()) {
                        String[] anims = {"rolly", "rollx", "rollz"};
                        loopAnim = anims[player.getRandom().nextInt(anims.length)];
                        player.getPersistentData().putString("xebTearsBrimstoneLoopAnim", loopAnim);
                    }
                    return event.setAndContinue(RawAnimation.begin().thenLoop(loopAnim));
                } else {
                    player.getPersistentData().remove("xebTearsBrimstoneLoopAnim");
                }
                
                // Si casteó una habilidad recientemente, reproducir la animación
                int animTicks = player.getPersistentData().getInt("xebTearsAbilityAnimTicks");
                if (animTicks > 0) {
                    String animName = player.getPersistentData().getString("xebTearsAbilityAnimName");
                    if (!animName.isEmpty()) {
                        return event.setAndContinue(RawAnimation.begin().thenPlay(animName));
                    }
                }
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("rolly")); // Default looping anim
        }));
    }

    @Nullable
    private LivingEntity resolveWielder(net.minecraft.world.entity.Entity entityFromTicket) {
        if (entityFromTicket instanceof LivingEntity living && isHolding(living)) {
            return living;
        }
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && isHolding(mc.player)) {
                return mc.player;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private boolean isHolding(LivingEntity entity) {
        return entity.getMainHandItem().getItem() == this
                || entity.getOffhandItem().getItem() == this;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private org.xeb.xeb.client.renderer.TheTearsGeoRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new org.xeb.xeb.client.renderer.TheTearsGeoRenderer();
                }
                return this.renderer;
            }
        });
    }

    public static void triggerAbilityAnim(Player player) {
        String[] anims = {"rolly", "rollx", "rollz"};
        String selected = anims[player.getRandom().nextInt(anims.length)];
        player.getPersistentData().putInt("xebTearsAbilityAnimTicks", 15);
        player.getPersistentData().putString("xebTearsAbilityAnimName", selected);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || stack.isEnchanted();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return enchantment == org.xeb.xeb.enchantment.ModEnchantments.MEDALLERO.get() 
            || enchantment == org.xeb.xeb.enchantment.ModEnchantments.REFRACTION.get() 
            || super.canApplyAtEnchantingTable(stack, enchantment);
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
        CompoundTag pData = player.getPersistentData();

        // Bloquear si el láser ya está disparándose activamente
        if (pData.getInt("xebBrimstoneFiringTicks") > 0) {
            return InteractionResultHolder.fail(stack);
        }

        CompoundTag stackTag = stack.getOrCreateTag();

        // Disparo instantáneo (otorgado por Activa 2)
        if (pData.getBoolean("xebNextBrimstoneInstant")) {
            pData.putBoolean("xebNextBrimstoneInstant", false);
            pData.putInt("xebBrimstoneCharge", 0);
            pData.putInt("xebBrimstoneFiringTicks", 40); // 2s de duración
            if (!level.isClientSide()) {
                level.playSound(null, player, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.8F);
                level.playSound(null, player, SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            return InteractionResultHolder.success(stack);
        }

        // Iniciar la carga (el jugador mantiene presionado click derecho)
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        CompoundTag pData = player.getPersistentData();
        int charge = pData.getInt("xebBrimstoneCharge");
        if (charge >= 80) {
            pData.putInt("xebBrimstoneCharge", 0);
            pData.putInt("xebBrimstoneFiringTicks", 40); // 2s de duración
            if (!level.isClientSide()) {
                level.playSound(null, player, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.8F);
                level.playSound(null, player, SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
        } else {
            // Carga insuficiente: resetear la barra
            pData.putInt("xebBrimstoneCharge", 0);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) return;

        boolean holdsMain = player.getMainHandItem().is(this);
        boolean holdsOff = player.getOffhandItem().is(this);
        boolean isEquipped = holdsMain || holdsOff;

        // Tick solo una vez por jugador aunque haya múltiples slots con el item
        if (isEquipped && ((holdsMain && stack == player.getMainHandItem()) || (!holdsMain && holdsOff && stack == player.getOffhandItem()))) {
            long currentTick = level.getGameTime();

            // Estado TRANSIENT (compartido con ActuarKeyPacket, TearsSyncPacket y el HUD)
            CompoundTag pData = player.getPersistentData();

            // Estado DURABLE ligado al item
            CompoundTag stackTag = stack.getOrCreateTag();

            // --- 1. Regeneración Pasiva ---
            if (!level.isClientSide()) {
                long lastHeal = stackTag.getLong("xebLastHealTick");
                if (currentTick - lastHeal >= 1200L) { // 60 segundos
                    player.heal(2.0F);
                    stackTag.putLong("xebLastHealTick", currentTick);
                }
            }

            // --- 2. Decrementar cooldowns y animaciones ---
            int prevA1 = pData.getInt("xebTearsA1Cooldown");
            int prevA2 = pData.getInt("xebTearsA2Cooldown");
            int prevAnimTicks = pData.getInt("xebTearsAbilityAnimTicks");
            String prevAnimName = pData.getString("xebTearsAbilityAnimName");

            if (prevA1 > 0) pData.putInt("xebTearsA1Cooldown", prevA1 - 1);
            if (prevA2 > 0) pData.putInt("xebTearsA2Cooldown", prevA2 - 1);
            if (prevAnimTicks > 0) {
                pData.putInt("xebTearsAbilityAnimTicks", prevAnimTicks - 1);
                if (prevAnimTicks - 1 <= 0) {
                    pData.remove("xebTearsAbilityAnimName");
                }
            }

            // --- 3. Decrementar duración del imbue ---
            int prevImbueType = stackTag.getInt("xebTearsImbueType");
            int prevImbueDur  = stackTag.getInt("xebTearsImbueDuration");
            if (prevImbueDur > 0) {
                stackTag.putInt("xebTearsImbueDuration", prevImbueDur - 1);
                if (prevImbueDur - 1 <= 0) {
                    stackTag.putInt("xebTearsImbueType", TearsProjectileEntity.IMBUE_NONE);
                }
            }

            // --- 4. Cola de burst ---
            int burstCount = pData.getInt("xebTearsBurstCount");
            if (burstCount > 0) {
                int burstTimer = pData.getInt("xebTearsBurstTimer") + 1;
                if (burstTimer >= 2) {
                    pData.putInt("xebTearsBurstCount", burstCount - 1);
                    pData.putInt("xebTearsBurstTimer", 0);
                    if (!level.isClientSide()) {
                        fireSingleTear(player, level, true);
                    }
                } else {
                    pData.putInt("xebTearsBurstTimer", burstTimer);
                }
            }

            // --- 5. Carga de Brimstone (click derecho sostenido) ---
            boolean isUsing = player.isUsingItem() && player.getUseItem().is(this);
            int prevCharge   = pData.getInt("xebBrimstoneCharge");
            int prevFiring   = pData.getInt("xebBrimstoneFiringTicks");
            boolean prevInstant = pData.getBoolean("xebNextBrimstoneInstant");

            if (isUsing) {
                if (prevCharge < 80) {
                    int newCharge = prevCharge + 1;
                    pData.putInt("xebBrimstoneCharge", newCharge);
                }
            } else {
                if (prevCharge > 0) {
                    pData.putInt("xebBrimstoneCharge", 0);
                }
            }

            // --- 6. Sincronizar al cliente solo si algo cambió ---
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                int curA1     = pData.getInt("xebTearsA1Cooldown");
                int curA2     = pData.getInt("xebTearsA2Cooldown");
                int curImbue  = stackTag.getInt("xebTearsImbueType");
                int curDur    = stackTag.getInt("xebTearsImbueDuration");
                int curCharge = pData.getInt("xebBrimstoneCharge");
                int curFiring = pData.getInt("xebBrimstoneFiringTicks");
                boolean curInstant = pData.getBoolean("xebNextBrimstoneInstant");
                int curAnimTicks = pData.getInt("xebTearsAbilityAnimTicks");
                String curAnimName = pData.getString("xebTearsAbilityAnimName");

                if (curA1 != prevA1 || curA2 != prevA2
                        || curImbue != prevImbueType || curDur != prevImbueDur
                        || curCharge != prevCharge || curFiring != prevFiring
                        || curInstant != prevInstant
                        || curAnimTicks != prevAnimTicks || !curAnimName.equals(prevAnimName)) {
                    XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new TearsSyncPacket(curA1, curA2, curImbue, curDur, curCharge, curFiring, curInstant, curAnimTicks, curAnimName));
                }
            }

            // --- 7. Tick del láser Brimstone activo ---
            int firingTicks = pData.getInt("xebBrimstoneFiringTicks");
            if (firingTicks > 0) {
                if (!org.xeb.xeb.beamstruggle.BeamStruggleManager.isInActiveStruggle(player.getUUID())) {
                    pData.putInt("xebBrimstoneFiringTicks", firingTicks - 1);
                }
                if (!level.isClientSide()) {
                    tickBrimstoneLaser(player, level, currentTick);
                }
            }
        }
    }

    private void fireSingleTear(Player player, Level level, boolean isBurst) {
        ItemStack heldStack = player.getMainHandItem().is(this) ? player.getMainHandItem() : player.getOffhandItem();
        CompoundTag tag = heldStack.getOrCreateTag();
        boolean leftEye = tag.getBoolean("xebTearsLeftEye");
        tag.putBoolean("xebTearsLeftEye", !leftEye);

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 upVec = new Vec3(0, 1, 0);
        Vec3 rightVec = lookVec.cross(upVec).normalize();

        // Calculate offset (0.3 blocks left/right)
        Vec3 eyeOffset = rightVec.scale(leftEye ? -0.3D : 0.3D);
        Vec3 spawnPos = eyePos.add(eyeOffset);

        int imbue = tag.getInt("xebTearsImbueType"); // read from ItemStack tag
        
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
        UUID playerUUID = player.getUUID();
        ItemStack stack = player.getMainHandItem().is(this) ? player.getMainHandItem() : player.getOffhandItem();
        int imbue = stack.getOrCreateTag().getInt("xebTearsImbueType");

        Vec3 lookDir = player.getLookAngle();
        Vec3 mouthPos = player.getEyePosition(1.0F).add(lookDir.scale(0.5D));

        List<Vec3> points = new ArrayList<>();
        points.add(mouthPos);

        Set<LivingEntity> hitEntities = new LinkedHashSet<>();
        Vec3 finalCollisionPoint = null;
        Vec3 struggleCollision = null;

        if (!level.isClientSide()) {
            if (!org.xeb.xeb.beamstruggle.BeamStruggleManager.isInActiveStruggle(player.getUUID())) {
                double reach = 40.0D;
                Vec3 beamEnd = mouthPos.add(lookDir.scale(reach));

                BlockHitResult blockHit = level.clip(new ClipContext(
                        mouthPos, beamEnd,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                ));

                Vec3 effectiveEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : beamEnd;

                AABB sweepBox = new AABB(mouthPos, effectiveEnd).inflate(0.75D);
                List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                        e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable() && !(e instanceof CrazyDiamondEntity));

                LivingEntity closestTarget = null;
                double closestDist = Double.MAX_VALUE;
                for (LivingEntity target : list) {
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
        }

        if (!org.xeb.xeb.beamstruggle.BeamStruggleManager.isInActiveStruggle(player.getUUID())) {
            for (LivingEntity target : hitEntities) {
                float baseDmg = 5.0F;
                boolean isBack = isBackstab(player, target);
                if (isBack) {
                    baseDmg *= 1.05F;
                    playBackstabSound(player, target, currentTick);
                }

                boolean isBlocked = target.isBlocking();
                int refractionLvl = player.getMainHandItem().getEnchantmentLevel(org.xeb.xeb.enchantment.ModEnchantments.REFRACTION.get());
                if (refractionLvl == 0) {
                    refractionLvl = player.getOffhandItem().getEnchantmentLevel(org.xeb.xeb.enchantment.ModEnchantments.REFRACTION.get());
                }
                if (isBlocked && refractionLvl > 0) {
                    List<LivingEntity> nearbyTargets = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(8.0D),
                            e -> e instanceof LivingEntity && e.isAlive() && e != player && e != target && !e.isSpectator());
                    int shots = Math.min(3, nearbyTargets.size());
                    for (int j = 0; j < shots; j++) {
                        LivingEntity targetNearby = nearbyTargets.get(j);
                        org.xeb.xeb.entity.MiniLaserProjectileEntity miniLaser = new org.xeb.xeb.entity.MiniLaserProjectileEntity(level, player);
                        miniLaser.moveTo(target.getX(), target.getEyeY() - 0.2D, target.getZ(), 0.0F, 0.0F);
                        Vec3 dir = targetNearby.getEyePosition(1.0F).subtract(target.getEyePosition(1.0F)).normalize();
                        miniLaser.setDeltaMovement(dir.scale(2.5D));
                        level.addFreshEntity(miniLaser);
                    }
                    if (shots > 0) {
                        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0F, 1.6F);
                    }
                }

                target.getPersistentData().putString("xebLastAttackWeapon", "the_tears");
                target.getPersistentData().putString("xebLastAttackType", "right_click");
                target.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
                target.hurt(player.damageSources().playerAttack(player), baseDmg);

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
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                }
            }
        }

        Vec3 renderEnd = points.get(points.size() - 1);

        struggleCollision = org.xeb.xeb.beamstruggle.BeamStruggleManager.getCollisionPointFor(playerUUID);
        if (struggleCollision != null) {
            renderEnd = struggleCollision;
            points.set(points.size() - 1, renderEnd);
        } else {
            Vec3 beamCollision = ActiveBeamManager.get().checkBeamVsBeamCollision(playerUUID, mouthPos, renderEnd, 1.2D);
            if (beamCollision != null) {
                double collisionDist = mouthPos.distanceToSqr(beamCollision);
                double currentEndDist = mouthPos.distanceToSqr(renderEnd);
                if (collisionDist < currentEndDist + 4.0D) {
                    UUID otherOwnerUUID = null;
                    Vec3 otherStart = null;
                    Vec3 otherEnd = null;

                    for (BeamData otherBeam : ActiveBeamManager.get().getActiveBeams()) {
                        if (!otherBeam.getOwnerUUID().equals(playerUUID)) {
                            otherOwnerUUID = otherBeam.getOwnerUUID();
                            otherStart = otherBeam.getStart();
                            otherEnd = otherBeam.getEnd();
                            break;
                        }
                    }

                    if (otherOwnerUUID != null && otherStart != null && otherEnd != null && level instanceof ServerLevel serverLevel) {
                        Vec3 myDir = renderEnd.subtract(mouthPos).normalize();
                        Vec3 otherDir = otherEnd.subtract(otherStart).normalize();
                        double dotProduct = myDir.dot(otherDir);

                        if (dotProduct < -0.2D) {
                            renderEnd = beamCollision;
                            points.set(points.size() - 1, renderEnd);
                            org.xeb.xeb.beamstruggle.BeamStruggleManager.onBeamCollision(
                                    playerUUID, otherOwnerUUID, mouthPos, otherStart, beamCollision, currentTick, serverLevel);
                        } else {
                            renderEnd = beamCollision;
                            points.set(points.size() - 1, renderEnd);

                            net.minecraft.nbt.CompoundTag pData = player.getPersistentData();
                            pData.putInt("xebBrimstoneFiringTicks", 0);
                            pData.remove("xebBrimstoneFiringTicks");
                            pData.putInt("xebBrimstoneCharge", 0);

                            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                    new BrimstoneBeamPacket(player.getId(), false, TearsProjectileEntity.IMBUE_NONE, Collections.emptyList()));

                            serverLevel.sendParticles(ParticleTypes.FLASH, beamCollision.x, beamCollision.y, beamCollision.z,
                                    3, 0.0D, 0.0D, 0.0D, 0.0D);
                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, beamCollision.x, beamCollision.y, beamCollision.z,
                                    15, 0.5D, 0.5D, 0.5D, 0.1D);
                            level.playSound(null, beamCollision.x, beamCollision.y, beamCollision.z,
                                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.5F);
                        }
                    }
                }
            }
        }

        int color = 0xFF0000;
        if (imbue == TearsProjectileEntity.IMBUE_PURPLE) color = 0xB000FF;
        else if (imbue == TearsProjectileEntity.IMBUE_WHITE) color = 0xFFFFFF;
        else if (imbue == TearsProjectileEntity.IMBUE_DARK) color = 0x222222;
        else if (imbue == TearsProjectileEntity.IMBUE_COLD) color = 0x90E0FF;

        BeamData beamData = new BeamData(
                playerUUID,
                player.getId(),
                mouthPos,
                renderEnd,
                color,
                currentTick,
                currentTick + 1,
                "brimstone"
        );
        ActiveBeamManager.get().putBeam(playerUUID, beamData);

        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BrimstoneBeamPacket(player.getId(), true, imbue, points));

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, mouthPos.x, mouthPos.y, mouthPos.z,
                    2, 0.1D, 0.1D, 0.1D, 0.02D);
        }
    }

    public static boolean isBackstab(LivingEntity attacker, LivingEntity target) {
        Vec3 attackDir = attacker.position().subtract(target.position()).normalize();
        Vec3 targetLook = target.getLookAngle();
        return attackDir.dot(targetLook) < -0.5D;
    }

    private static void playBackstabSound(Player player, LivingEntity target, long currentTick) {
        CompoundTag targetData = target.getPersistentData();
        long lastSound = targetData.getLong("xebLastBackstabSound");
        if (currentTick - lastSound >= 20) {
            target.level().playSound(null, target, SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 1.0F, 1.5F);
            targetData.putLong("xebLastBackstabSound", currentTick);
        }
    }

    public void triggerLeftClick(Player player, Level level) {
        if (!level.isClientSide()) {
            ItemStack heldStack = player.getMainHandItem().is(this) ? player.getMainHandItem() : player.getOffhandItem();
            CompoundTag tag = heldStack.getOrCreateTag();
            long lastTearTime = tag.getLong("xebLastTearShootTime");
            long currentTick = level.getGameTime();
            if (currentTick - lastTearTime >= 8) { // 0.4 seconds (8 ticks)
                tag.putLong("xebLastTearShootTime", currentTick);
                
                triggerAbilityAnim(player);
                
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
