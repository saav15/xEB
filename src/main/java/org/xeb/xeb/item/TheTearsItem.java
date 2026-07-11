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

public class TheTearsItem extends Item {

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
            // → player.getPersistentData(). No es fuga de memoria: es 1 entrada por jugador,
            //   no un Map estático con UUID como clave.
            CompoundTag pData = player.getPersistentData();

            // Estado DURABLE ligado al item (viaja con él si el jugador lo tira)
            // → ItemStack NBT. Solo para valores que DEBEN seguir al item.
            CompoundTag stackTag = stack.getOrCreateTag();

            // --- 1. Regeneración Pasiva ---
            // Timestamp en el ItemStack para no interferir con use() ni startUsingItem()
            if (!level.isClientSide()) {
                long lastHeal = stackTag.getLong("xebLastHealTick");
                if (currentTick - lastHeal >= 1200L) { // 60 segundos
                    player.heal(2.0F);
                    stackTag.putLong("xebLastHealTick", currentTick);
                }
            }

            // --- 2. Decrementar cooldowns de habilidades (en pData, igual que ActuarKeyPacket) ---
            int prevA1 = pData.getInt("xebTearsA1Cooldown");
            int prevA2 = pData.getInt("xebTearsA2Cooldown");
            if (prevA1 > 0) pData.putInt("xebTearsA1Cooldown", prevA1 - 1);
            if (prevA2 > 0) pData.putInt("xebTearsA2Cooldown", prevA2 - 1);

            // --- 3. Decrementar duración del imbue (en ItemStack: es estado del item) ---
            int prevImbueType = stackTag.getInt("xebTearsImbueType");
            int prevImbueDur  = stackTag.getInt("xebTearsImbueDuration");
            if (prevImbueDur > 0) {
                stackTag.putInt("xebTearsImbueDuration", prevImbueDur - 1);
                if (prevImbueDur - 1 <= 0) {
                    stackTag.putInt("xebTearsImbueType", TearsProjectileEntity.IMBUE_NONE);
                }
            }

            // --- 4. Cola de burst (Activa 2 — en pData para coherencia con ActuarKeyPacket) ---
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
                    // Feedback de audio cada 20 ticks mientras se carga (pitch creciente)
                    if (!level.isClientSide() && newCharge % 20 == 0) {
                        level.playSound(null, player, SoundEvents.NOTE_BLOCK_PLING.get(),
                                SoundSource.PLAYERS, 0.4F, 0.5F + (newCharge / 80.0F) * 1.5F);
                    }
                }
            } else {
                // Si no se está cargando y no se está disparando, resetear carga
                if (prevFiring <= 0 && prevCharge > 0) {
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

                if (curA1 != prevA1 || curA2 != prevA2
                        || curImbue != prevImbueType || curDur != prevImbueDur
                        || curCharge != prevCharge || curFiring != prevFiring
                        || curInstant != prevInstant) {
                    XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new TearsSyncPacket(curA1, curA2, curImbue, curDur, curCharge, curFiring, curInstant));
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
            } else {
                if (firingTicks == 0 && pData.contains("xebBrimstoneFiringTicks")) {
                    pData.remove("xebBrimstoneFiringTicks");
                    if (!level.isClientSide()) {
                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new BrimstoneBeamPacket(player.getId(), false, TearsProjectileEntity.IMBUE_NONE, Collections.emptyList()));
                    }
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
        // Imbue es estado del item → ItemStack NBT
        ItemStack heldStack = player.getMainHandItem().is(this) ? player.getMainHandItem() : player.getOffhandItem();
        int imbue = heldStack.getOrCreateTag().getInt("xebTearsImbueType");

        // Firing from the mouth
        Vec3 mouthPos = player.getEyePosition(1.0F).subtract(0, 0.15D, 0);
        Vec3 lookDir = player.getLookAngle();

        List<Vec3> points = new ArrayList<>();
        points.add(mouthPos);

        Set<LivingEntity> hitEntities = new HashSet<>();
        Vec3 finalCollisionPoint = null;
        UUID playerUUID = player.getUUID();

        Vec3 struggleCollision = org.xeb.xeb.beamstruggle.BeamStruggleManager.getCollisionPointFor(playerUUID);
        if (struggleCollision != null) {
            points.add(struggleCollision);
            finalCollisionPoint = struggleCollision;

            // Update struggle collision point using current raycast look direction
            Vec3 otherStart = null;
            UUID otherOwnerUUID = null;
            for (BeamData otherBeam : ActiveBeamManager.get().getActiveBeams()) {
                if (!otherBeam.getOwnerUUID().equals(playerUUID)) {
                    otherOwnerUUID = otherBeam.getOwnerUUID();
                    otherStart = otherBeam.getStart();
                    break;
                }
            }
            if (otherStart != null) {
                double reach = 40.0D;
                Vec3 lookEnd = mouthPos.add(lookDir.scale(reach));
                Vec3 closestCol = ActiveBeamManager.get().checkBeamVsBeamCollision(playerUUID, mouthPos, lookEnd, 0.75D);
                if (closestCol != null) {
                    org.xeb.xeb.beamstruggle.BeamStruggleManager.updateCollisionPoint(playerUUID, closestCol);
                    Vec3 updatedCol = org.xeb.xeb.beamstruggle.BeamStruggleManager.getCollisionPointFor(playerUUID);
                    if (updatedCol != null) {
                        finalCollisionPoint = updatedCol;
                        points.set(points.size() - 1, finalCollisionPoint);
                    }
                }
            }
        } else {
            // Brimstone Morado (Purple): curves and loops targets
            if (imbue == TearsProjectileEntity.IMBUE_PURPLE) {
                Vec3 currentPos = mouthPos;
                Vec3 currentDir = lookDir;
                double segmentLength = 2.0D;
                int maxSegments = 12;

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
                    AABB sweepBox = new AABB(currentPos, segmentEnd).inflate(0.8D);
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
        }

        // --- 1. Apply Damage & Special element ticks ---
        for (LivingEntity target : hitEntities) {
            float baseDmg = 10.0F; // Brimstone deals double Optic Blast damage (5.0F * 2)
            boolean isBack = isBackstab(player, target);
            if (isBack) {
                baseDmg *= 1.05F; // +5% damage multiplier
                playBackstabSound(player, target, currentTick);
            }

            target.getPersistentData().putString("xebLastAttackWeapon", "the_tears");
            target.getPersistentData().putString("xebLastAttackType", "right_click");
            target.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
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
        Vec3 renderEnd = points.get(points.size() - 1);

        if (struggleCollision == null) {
            Vec3 beamCollision = ActiveBeamManager.get().checkBeamVsBeamCollision(playerUUID, mouthPos, renderEnd, 0.75D);
            if (beamCollision != null) {
                double collisionDist = mouthPos.distanceToSqr(beamCollision);
                double currentEndDist = mouthPos.distanceToSqr(renderEnd);
                if (collisionDist < currentEndDist) {
                    // Buscar el otro beam
                    UUID otherOwnerUUID = null;
                    Vec3 otherStart = null, otherEnd = null;
                    for (BeamData otherBeam : ActiveBeamManager.get().getActiveBeams()) {
                        if (!otherBeam.getOwnerUUID().equals(playerUUID)) {
                            otherOwnerUUID = otherBeam.getOwnerUUID();
                            otherStart = otherBeam.getStart();
                            otherEnd = otherBeam.getEnd();
                            break;
                        }
                    }

                    if (otherOwnerUUID != null && otherStart != null && otherEnd != null) {
                        Vec3 myDir = renderEnd.subtract(mouthPos).normalize();
                        Vec3 otherDir = otherEnd.subtract(otherStart).normalize();
                        double dotProduct = myDir.dot(otherDir);

                        if (dotProduct < -0.5D) {
                            // Frontal — struggle
                            renderEnd = beamCollision;
                            points.set(points.size() - 1, renderEnd);
                            org.xeb.xeb.beamstruggle.BeamStruggleManager.onBeamCollision(
                                    playerUUID, otherOwnerUUID, mouthPos, otherStart, beamCollision, currentTick, (ServerLevel) level);
                            org.xeb.xeb.beamstruggle.BeamStruggleManager.updateCollisionPoint(playerUUID, beamCollision);

                            if (level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.FLASH, renderEnd.x, renderEnd.y, renderEnd.z,
                                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, renderEnd.x, renderEnd.y, renderEnd.z,
                                        8, 0.3D, 0.3D, 0.3D, 0.05D);
                            }
                        } else {
                            // No frontal — cortar beam y drenar
                            renderEnd = beamCollision;
                            points.set(points.size() - 1, renderEnd);

                            net.minecraft.nbt.CompoundTag pData = player.getPersistentData();
                            pData.putInt("xebBrimstoneFiringTicks", 0);
                            pData.remove("xebBrimstoneFiringTicks");
                            pData.putInt("xebBrimstoneCharge", 0);

                            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                    new BrimstoneBeamPacket(player.getId(), false, TearsProjectileEntity.IMBUE_NONE, Collections.emptyList()));

                            if (level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.FLASH, beamCollision.x, beamCollision.y, beamCollision.z,
                                        3, 0.0D, 0.0D, 0.0D, 0.0D);
                                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, beamCollision.x, beamCollision.y, beamCollision.z,
                                        15, 0.5D, 0.5D, 0.5D, 0.1D);
                            }
                            level.playSound(null, beamCollision.x, beamCollision.y, beamCollision.z,
                                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.5F);
                        }
                    }
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
                renderEnd,
                color,
                currentTick,
                currentTick + 1,
                "brimstone"
        );
        ActiveBeamManager.get().putBeam(playerUUID, beamData);

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
