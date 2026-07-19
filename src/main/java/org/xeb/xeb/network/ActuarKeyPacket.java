package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.item.MechaOverdriveItem;
import org.xeb.xeb.item.HolyDualityBladeItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.nbt.CompoundTag;
import org.xeb.xeb.entity.HomingMissileEntity;
import java.util.List;
import java.util.function.Supplier;

public class ActuarKeyPacket {
    private final int button;
    private final boolean press;

    public ActuarKeyPacket(int button) {
        this(button, true);
    }

    public ActuarKeyPacket(int button, boolean press) {
        this.button = button;
        this.press = press;
    }

    public static void encode(ActuarKeyPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.button);
        buf.writeBoolean(msg.press);
    }

    public static ActuarKeyPacket decode(FriendlyByteBuf buf) {
        return new ActuarKeyPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(ActuarKeyPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.isAlive()) {
                boolean holdsV1 = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.DOOMFIST.get())
                        || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.DOOMFIST.get());
                boolean holdsV2 = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.DOOMFIST_V2.get())
                        || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.DOOMFIST_V2.get());
                boolean holdsOpticBlast = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.OPTIC_BLAST.get())
                        || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.OPTIC_BLAST.get());
                boolean holdsGoldenFlower = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.GOLDEN_FLOWER.get())
                        || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.GOLDEN_FLOWER.get());
                boolean holdsCD = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.BROKEN_DIAMOND.get())
                        || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.BROKEN_DIAMOND.get());
                boolean holdsTears = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.THE_TEARS.get())
                        || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.THE_TEARS.get());
                boolean holdsMecha = player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof MechaOverdriveItem
                        || player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof MechaOverdriveItem;
                boolean holdsHoly = player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof HolyDualityBladeItem
                        || player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof HolyDualityBladeItem;
                // Check if any registered Extreme Burst curio is equipped (replaces old hasUltimate)
                org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry =
                        org.xeb.xeb.extremeburst.ExtremeBurstRegistry.findActiveBurst(player);
                boolean hasBurst = burstEntry != null;

                if (!holdsV1 && !holdsV2 && !holdsOpticBlast && !holdsGoldenFlower && !holdsCD && !holdsTears && !holdsMecha && !holdsHoly && !hasBurst) return;

                long time = player.level().getGameTime();
                // Dev mode: bypass all cooldown checks when xebDevCooldownsDisabled == true
                boolean devBypass = player.getPersistentData().getBoolean("xebDevCooldownsDisabled");

                // ── Extreme Burst (Activa 3 / tecla N) ─────────────────────────────────
                // burstEntry may be non-null from inventory fallback (for HUD display),
                // so we verify via isInCurioSlot() before allowing activation.
                if (msg.button == 3 && msg.press) {

                    if (!hasBurst) {
                        // No burst item at all — nothing to do
                        return;
                    }

                    // Server-side enforcement: item must be in a Curios slot
                    boolean inCurio = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.isInCurioSlot(player, burstEntry);
                    if (!inCurio) {
                        // Item is only in inventory/offhand — show guidance message
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "chat.xeb.extreme_burst.equip_curio"));
                        return;
                    }

                    // Verify weapon requirement (UNIVERSAL always passes; LIMITED needs specific weapon)
                    if (!org.xeb.xeb.extremeburst.ExtremeBurstRegistry.canActivate(player, burstEntry)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "chat.xeb.extreme_burst.requires_weapon"));
                        return;
                    }

                    if (!devBypass) {
                        // Cooldown check (keyed on the curio item itself, like QCB)
                        if (player.getCooldowns().isOnCooldown(burstEntry.curioItem)) return;

                        // Prevent stacking two bursts simultaneously
                        if (player.getPersistentData().getBoolean("xebExtremeBurstActive")) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                    "chat.xeb.extreme_burst.already_active"));
                            return;
                        }
                        // Also block if an Instance is still running
                        if (player.getPersistentData().getInt("xebExtremeBurstInstanceTicks") > 0) return;
                    }

                    // Mark as active
                    player.getPersistentData().putBoolean("xebExtremeBurstActive", true);
                    player.getPersistentData().putString("xebExtremeBurstId", burstEntry.curioItem.toString());

                    // For Instance bursts, set the duration timer
                    if (burstEntry.version == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstVersion.INSTANCE) {
                        player.getPersistentData().putInt("xebExtremeBurstInstanceTicks", burstEntry.durationTicks);
                        player.getPersistentData().putInt("xebExtremeBurstInstanceMaxTicks", burstEntry.durationTicks);
                    }

                    // Execute the item-specific burst logic
                    org.xeb.xeb.extremeburst.ExtremeBurstHandler.handleActivation(player, burstEntry);

                    // Apply cooldown (keyed on the curio item) — skipped in dev mode
                    if (!devBypass) {
                        player.getCooldowns().addCooldown(burstEntry.curioItem, burstEntry.cooldownTicks);
                    }
                    return;
                }



                // ── Flourish (button 4) — incrementa el mash counter del Beam Struggle activo ──
                if (msg.button == 4 && msg.press) {
                    org.xeb.xeb.beamstruggle.BeamStruggleManager.handleFlourishPress(player);
                    return;
                }

                if (holdsGoldenFlower) {
                    if (msg.button == 1) {
                        // --- Flower Dance (Activa 1) ---
                        if (msg.press) {
                            // Omega Flowery: Flower Dance heals 20% HP instead of spawning clones
                            if (player.getPersistentData().getBoolean("xebOmegaFloweryActive")) {
                                float healAmount = player.getMaxHealth() * org.xeb.xeb.extremeburst.OmegaFloweryHandler.FLOWER_DANCE_HEAL_PERCENT;
                                player.heal(healAmount);
                                player.level().playSound(null, player, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.5F);
                                return;
                            }

                            int danceCD = player.getPersistentData().getInt("xebGoldenFlowerDanceCooldown");
                            if (danceCD > 0 && !devBypass) return; // 20s cooldown (skipped in dev mode)


                            // Find target
                            LivingEntity target = null;
                            Vec3 eyePos = player.getEyePosition(1.0F);
                            Vec3 lookDir = player.getLookAngle();
                            Vec3 beamEnd = eyePos.add(lookDir.scale(25.0D));
                            net.minecraft.world.phys.BlockHitResult blockHit = player.level().clip(new net.minecraft.world.level.ClipContext(
                                    eyePos, beamEnd, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player
                            ));
                            Vec3 effectiveEnd = blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? blockHit.getLocation() : beamEnd;
                            AABB sweepBox = new AABB(eyePos, effectiveEnd).inflate(2.0D);
                            net.minecraft.world.phys.EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                                    player, eyePos, effectiveEnd, sweepBox,
                                    (e) -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator() && e.isPickable(),
                                    625.0D
                            );
                            if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
                                target = living;
                            }

                            // Fallback: If not looking at anything, target the closest enemy within 10 blocks
                            if (target == null) {
                                List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10.0D),
                                        e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator() && e.isPickable());
                                if (!nearby.isEmpty()) {
                                    nearby.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
                                    target = nearby.get(0);
                                }
                            }

                            if (target != null) {
                                player.getPersistentData().putLong("xebFlowerDanceLastTime", time);
                                player.getPersistentData().putInt("xebGoldenFlowerDanceCooldown", 400);
                                player.getPersistentData().putBoolean("xebFlowerDanceActive", true);
                                player.getPersistentData().putInt("xebFlowerDanceTicksRemaining", 85); // 5 clones * 15 ticks + buffer

                                int[] targetIds = new int[5];
                                boolean isBoss = !target.canChangeDimensions() || target.getMaxHealth() > 40.0F || target instanceof Player;

                                if (isBoss) {
                                    // Too strong! All clones attack this single boss target
                                    for (int i = 0; i < 5; i++) {
                                        targetIds[i] = target.getId();
                                    }
                                } else {
                                    // Spread clones among other nearby targets within 10 blocks
                                    List<LivingEntity> cohort = player.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(10.0D),
                                            e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator() && e.isPickable());
                                    
                                    cohort.remove(target);
                                    cohort.add(0, target); // main target is first

                                    for (int i = 0; i < 5; i++) {
                                        LivingEntity selectedTarget = cohort.get(i % cohort.size());
                                        targetIds[i] = selectedTarget.getId();
                                    }
                                }

                                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                        new GoldenFlowerDanceStartPacket(player.getId(), targetIds, 5));

                                // Custom Flower Dance Sound Selection (flowerdancefake has 5% chance, others share 95%)
                                net.minecraft.sounds.SoundEvent danceSound;
                                float prob = player.getRandom().nextFloat();
                                if (prob < 0.05F) {
                                    danceSound = org.xeb.xeb.sound.ModSounds.FLOWER_DANCE_FAKE.get();
                                } else {
                                    danceSound = player.getRandom().nextBoolean() ? 
                                            org.xeb.xeb.sound.ModSounds.FLOWER_DANCE1.get() : 
                                            org.xeb.xeb.sound.ModSounds.FLOWER_DANCE2.get();
                                }
                                player.level().playSound(null, player, danceSound, SoundSource.PLAYERS, 1.2F, 1.0F);
                            }
                        }
                    } else if (msg.button == 2) {
                        // --- Jarona (Activa 2) ---
                        if (msg.press) {
                            int charges = player.getPersistentData().getInt("xebJaronaCharges");
                            if (charges <= 0) return;

                            int comboStep = player.getPersistentData().getInt("xebJaronaComboStep");
                            long lastJarona = player.getPersistentData().getLong("xebLastJaronaTime");
                            if (time - lastJarona > 40) {
                                comboStep = 1;
                            } else {
                                comboStep = comboStep % 3 + 1;
                            }

                            // Consume charge
                            player.getPersistentData().putInt("xebJaronaCharges", charges - 1);
                            player.getPersistentData().putInt("xebJaronaComboStep", comboStep);
                            player.getPersistentData().putLong("xebLastJaronaTime", time);
                            if (charges == 3) {
                                player.getPersistentData().putInt("xebJaronaRechargeTimer", 0);
                            }

                            // Activa 2: flying kick motion
                            Vec3 lookVec = player.getLookAngle();
                            double force = 1.2D + (comboStep * 0.8D);
                            player.setDeltaMovement(lookVec.x * force, 0.25D, lookVec.z * force);
                            player.hurtMarked = true;

                            // Apply Resistance 1 effect to player until the attack ends (15 ticks)
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 15, 0, false, false, false
                            ));

                            // If combo step 3 is completed, grant Resistance 3 for 10 seconds (200 ticks)
                            if (comboStep == 3) {
                                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 2, false, false, true
                                ));
                                player.level().playSound(null, player, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.5F);
                            }

                            // Start AABB dash damage phase
                            player.getPersistentData().putInt("xebJaronaDashTicks", 15);

                            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                    new JaronaDashPacket(player.getId(), comboStep));

                            // Custom Jarona Sound Selection (jaronafake has 5% chance, others share 95%)
                            net.minecraft.sounds.SoundEvent jaronaSound;
                            float prob = player.getRandom().nextFloat();
                            if (prob < 0.05F) {
                                jaronaSound = org.xeb.xeb.sound.ModSounds.JARONA_FAKE.get();
                            } else {
                                int soundChoice = player.getRandom().nextInt(4);
                                if (soundChoice == 0) jaronaSound = org.xeb.xeb.sound.ModSounds.JARONA1.get();
                                else if (soundChoice == 1) jaronaSound = org.xeb.xeb.sound.ModSounds.JARONA2.get();
                                else if (soundChoice == 2) jaronaSound = org.xeb.xeb.sound.ModSounds.JARONA3.get();
                                else jaronaSound = org.xeb.xeb.sound.ModSounds.JARONA4.get();
                            }
                            player.level().playSound(null, player, jaronaSound, SoundSource.PLAYERS, 1.5F, 1.0F);
                        }
                    }
                } else if (holdsOpticBlast) {
                    // Exclusivity: Don't allow using active abilities if the player is using the primary laser
                    if (player.isUsingItem() && player.getUseItem().is(ModItems.OPTIC_BLAST.get())) {
                        return;
                    }

                    // --- The Optic Blast Abilities ---
                    if (msg.button == 1) {
                        // --- Cyclone Push (Activa 1) ---
                        if (msg.press) {
                            long lastCyclone = player.getPersistentData().getLong("xebCyclonePushLastTime");
                            if (time - lastCyclone < org.xeb.xeb.item.OpticBlastItem.CYCLONE_PUSH_COOLDOWN && !devBypass) return;

                            // Don't allow while already firing Gene Splice
                            if (player.getPersistentData().getBoolean("xebGeneSpliceFiring")) return;

                            player.getPersistentData().putBoolean("xebCyclonePushFiring", true);
                            player.getPersistentData().putBoolean("xebCyclonePushKeyHeld", true);
                            player.getPersistentData().putInt("xebCyclonePushTicks",
                                    org.xeb.xeb.item.OpticBlastItem.CYCLONE_PUSH_DURATION);

                            player.level().playSound(null, player, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 2.0F);
                        } else {
                            // Key released — stop Cyclone Push
                            player.getPersistentData().putBoolean("xebCyclonePushKeyHeld", false);
                        }
                    } else if (msg.button == 2) {
                        // --- Gene Splice (Activa 2) ---
                        if (msg.press) {
                            long lastSplice = player.getPersistentData().getLong("xebGeneSpliceLastTime");
                            if (time - lastSplice < org.xeb.xeb.item.OpticBlastItem.GENE_SPLICE_COOLDOWN && !devBypass) return;

                            // Don't allow while already firing Cyclone Push
                            if (player.getPersistentData().getBoolean("xebCyclonePushFiring")) return;

                            player.getPersistentData().putBoolean("xebGeneSpliceFiring", true);
                            player.getPersistentData().putBoolean("xebGeneSpliceKeyHeld", true);
                            player.getPersistentData().putInt("xebGeneSpliceTicks",
                                    org.xeb.xeb.item.OpticBlastItem.GENE_SPLICE_DURATION);

                            player.level().playSound(null, player, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.5F);
                        } else {
                            // Key released — stop Gene Splice
                            player.getPersistentData().putBoolean("xebGeneSpliceKeyHeld", false);
                        }
                    }
                } else if (holdsV1) {
                    // --- The Doomfist v1 Abilities ---
                    if (msg.button == 1) {
                        // --- Rising Uppercut ---
                        long lastUppercut = player.getPersistentData().getLong("xebUppercutLastTime");
                        if (time - lastUppercut < 100 && !devBypass) return;
                        player.getPersistentData().putLong("xebUppercutLastTime", time);

                        Vec3 look = player.getLookAngle();
                        Vec3 motion = new Vec3(look.x * 0.6D, 1.2D, look.z * 0.6D);
                        player.setDeltaMovement(motion);
                        player.hurtMarked = true;

                        player.getPersistentData().putInt("xebUppercutFloatTicks", 40);
                        player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);
                        player.getPersistentData().putBoolean("xebUppercutEmpoweredPunch", true); // Empower next Rocket Punch

                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new DoomfistAbilitySyncPacket(player.getId(), 40, 0, 0.0D, 0.0D, 0.0D, 100, 0));

                        AABB area = player.getBoundingBox().inflate(1.5D, 1.0D, 1.5D);
                        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                                e -> e != player && e.isAlive() && !e.isAlliedTo(player));

                        double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

                        for (LivingEntity target : targets) {
                            boolean isBoss = !target.canChangeDimensions() || target.getMaxHealth() > 40.0F || target instanceof Player;
                            float finalDamage = isBoss ? (float) (baseDamage * 4.0D) : (float) baseDamage; // 300% more damage against bosses (4x total)
                            target.hurt(player.damageSources().playerAttack(player), finalDamage);

                            Vec3 targetMotion = new Vec3(look.x * 0.6D, 1.2D, look.z * 0.6D);
                            target.setDeltaMovement(targetMotion);
                            target.hurtMarked = true;

                            target.getPersistentData().putInt("xebUppercutFloatTicks", 40);

                            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                                    new DoomfistAbilitySyncPacket(target.getId(), 40, 0, 0.0D, 0.0D, 0.0D, 0, 0));
                        }

                        player.level().playSound(null, player, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2F, 0.7F);
                        player.level().playSound(null, player, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);

                        if (player.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 10, 0.5D, 0.2D, 0.5D, 0.1D);
                        }

                    } else if (msg.button == 2) {
                        // --- Seismic Slam ---
                        if (player.onGround()) return;

                        long lastSlam = player.getPersistentData().getLong("xebSlamLastTime");
                        if (time - lastSlam < 120 && !devBypass) return;
                        player.getPersistentData().putLong("xebSlamLastTime", time);

                        player.getPersistentData().putInt("xebSlamState", 1);
                        player.getPersistentData().putInt("xebSlamTimer", 15);
                        player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);

                        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                        player.hurtMarked = true;

                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new DoomfistAbilitySyncPacket(player.getId(), 0, 1, 0.0D, 0.0D, 0.0D, 0, 120));

                        player.level().playSound(null, player, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                    }
                } else if (holdsV2) {
                    // --- The Doomfist v2 Abilities ---
                    if (msg.button == 1) {
                        // --- Earthquake Slam (V2) ---
                        long lastSlam2 = player.getPersistentData().getLong("xebSlam2LastTime");
                        if (time - lastSlam2 < 140 && !devBypass) return; // 7 seconds cooldown
                        player.getPersistentData().putLong("xebSlam2LastTime", time);

                        player.getPersistentData().putInt("xebSlam2State", 1); // Rising/momentum phase
                        player.getPersistentData().putInt("xebSlam2Timer", 25); // 25 ticks total path
                        player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);

                        Vec3 look = player.getLookAngle();
                        Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
                        
                        // Launch player in upward diagonal trajectory
                        player.setDeltaMovement(forward.x * 0.8D, 0.5D, forward.z * 0.8D);
                        player.hurtMarked = true;

                        // Sync to client with 140 tick cooldown
                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new DoomfistAbilitySyncPacket(player.getId(), 0, 0, 0.0D, 0.0D, 0.0D, 140, 0));

                        player.level().playSound(null, player, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.2F);
                                
                    } else if (msg.button == 2) {
                        // --- Power Block (V2) ---
                         if (msg.press) {
                             long lastBlock = player.getPersistentData().getLong("xebBlockLastTime");
                             if (time - lastBlock < 140 && !devBypass) return; // 7 seconds cooldown

                             // If blocking mid-Earthquake Slam, cancel slam and pop up with upward momentum
                             if (player.getPersistentData().contains("xebSlam2State")) {
                                 player.getPersistentData().remove("xebSlam2State");
                                 player.getPersistentData().remove("xebSlam2Timer");
                                 
                                 XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                         new DoomfistAbilitySyncPacket(player.getId(), 0, 0));
                                         
                                 player.setDeltaMovement(player.getDeltaMovement().x, 0.55D, player.getDeltaMovement().z);
                                 player.hurtMarked = true;
                             }

                             player.getPersistentData().putBoolean("xebPowerBlocking", true);
                             XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                     new DoomfistPowerBlockSyncPacket(player.getId(), true));
                             player.getPersistentData().putInt("xebBlockTimer", 0);
                             player.getPersistentData().putBoolean("xebBlockKeyHeld", true);
                             player.getPersistentData().putFloat("xebBlockDamageAccumulated", 0.0F);

                             // Apply speed penalty
                             net.minecraft.world.entity.ai.attributes.AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                             if (speed != null) {
                                 speed.removeModifier(java.util.UUID.fromString("6a04870c-26d9-4828-98e9-44d4715f606e"));
                                 speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                         java.util.UUID.fromString("6a04870c-26d9-4828-98e9-44d4715f606e"),
                                         "Power Block speed penalty",
                                         -0.35D,
                                         net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL
                                 ));
                             }

                             player.level().playSound(null, player, SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0F, 1.2F);
                         } else {
                             player.getPersistentData().putBoolean("xebBlockKeyHeld", false);
                         }
                    }
                } else if (holdsCD) {
                    if (msg.button == 1) {
                        // --- Dora! (Activa 1) ---
                        if (player.getPersistentData().contains("xebCDA1State")) {
                            int state = player.getPersistentData().getInt("xebCDA1State");
                            if (state == 3) {
                                // Second press within 2s window: transition to Phase 4 (unlock kick)
                                player.getPersistentData().putInt("xebCDA1State", 4);
                                player.getPersistentData().putInt("xebCDA1Timer", 60); // 3s window for left click
                                
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.5F);
                                
                                for (net.minecraft.world.entity.Entity e : ((ServerLevel) player.level()).getAllEntities()) {
                                    if (e instanceof org.xeb.xeb.entity.CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                                        cds.setAnimState(org.xeb.xeb.entity.CrazyDiamondEntity.STATE_WIND_UP, 10);
                                        break;
                                    }
                                }
                            }
                            return;
                        }
                        
                        if (player.getPersistentData().getInt("xebCDA1CooldownTicks") > 0 && !devBypass) return;
                        
                        player.getPersistentData().putInt("xebCDA1State", 1);
                        player.getPersistentData().putInt("xebCDA1Timer", 15); // dash duration (ticks)
                        player.getPersistentData().remove("xebCDA1TargetId");
                        
                        Vec3 look = player.getLookAngle();
                        Vec3 dashVec = new Vec3(look.x, 0.0D, look.z).normalize().scale(2.0D);
                        player.setDeltaMovement(dashVec.x, 0.15D, dashVec.z);
                        player.hurtMarked = true;
                        
                        if (!player.onGround()) {
                            player.getPersistentData().putInt("xebCDLevitateTicks", 20); // float during dash
                        }
                        
                        for (net.minecraft.world.entity.Entity e : ((ServerLevel) player.level()).getAllEntities()) {
                            if (e instanceof org.xeb.xeb.entity.CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                                cds.setAnimState(org.xeb.xeb.entity.CrazyDiamondEntity.STATE_DASHING, 15);
                                break;
                            }
                        }
                        
                        player.level().playSound(null, player, SoundEvents.PHANTOM_BITE, SoundSource.PLAYERS, 1.0F, 1.2F);
                                
                    } else if (msg.button == 2) {
                        // --- Restore! (Activa 2) ---
                        if (player.getPersistentData().contains("xebCDA2ProjectileId")) {
                            int projId = player.getPersistentData().getInt("xebCDA2ProjectileId");
                            net.minecraft.world.entity.Entity projEntity = player.level().getEntity(projId);
                            if (projEntity instanceof org.xeb.xeb.entity.RestoreProjectileEntity proj && !proj.isReturning()) {
                                proj.setReturning(true);
                                
                                for (net.minecraft.world.entity.Entity e : ((ServerLevel) player.level()).getAllEntities()) {
                                    if (e instanceof org.xeb.xeb.entity.CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                                        cds.setAnimState(org.xeb.xeb.entity.CrazyDiamondEntity.STATE_REELING_IN, 10);
                                        break;
                                    }
                                }
                                
                                player.level().playSound(null, player, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0F, 1.4F);
                                return;
                            }
                        }
                        
                        if (player.getPersistentData().getInt("xebCDA2CooldownTicks") > 0 && !devBypass) return;
                        
                        player.getPersistentData().putInt("xebCDA2CooldownTicks", 300); // 15s cooldown (300 ticks)
                        XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                new org.xeb.xeb.network.CrazyDiamondSyncPacket(
                                    player.getPersistentData().getInt("xebCDA1CooldownTicks"),
                                    300,
                                    player.getPersistentData().getInt("xebCDPunches"),
                                    player.getPersistentData().getInt("xebCDChargeTimer")
                                ));
                        
                        // Initialize Digging Phase of Activa 2 State Machine
                        player.getPersistentData().putInt("xebCDA2State", 1);
                        player.getPersistentData().putInt("xebCDA2Timer", 6);
                        
                        for (net.minecraft.world.entity.Entity e : ((ServerLevel) player.level()).getAllEntities()) {
                            if (e instanceof org.xeb.xeb.entity.CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                                cds.setAnimState(org.xeb.xeb.entity.CrazyDiamondEntity.STATE_DIGGING, 6);
                                break;
                            }
                        }
                        
                    }
                } else if (holdsTears) {
                    if (player.getPersistentData().getInt("xebBrimstoneFiringTicks") > 0
                            || player.getPersistentData().getInt("xebDogmaBrimstoneTicks") > 0) {
                        return;
                    }
                    if (msg.button == 1 && msg.press) {
                            if (player.getPersistentData().getInt("xebTearsA1Cooldown") <= 0 || devBypass) {
                                int element = 1 + player.getRandom().nextInt(4); // 1=Purple 2=White 3=Dark 4=Cold
                                // Imbue es estado del item → se escribe en el ItemStack NBT
                                net.minecraft.world.item.ItemStack tearsStack =
                        player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.TheTearsItem
                                        ? player.getMainHandItem() : player.getOffhandItem();
                                net.minecraft.nbt.CompoundTag stackTag = tearsStack.getOrCreateTag();
                                stackTag.putInt("xebTearsImbueType", element);
                                stackTag.putInt("xebTearsImbueDuration", 100); // 5s
                                // Cooldown de la habilidad → pData (se decrementa en inventoryTick)
                                player.getPersistentData().putInt("xebTearsA1Cooldown", 300); // 15s
                                
                                org.xeb.xeb.item.TheTearsItem.triggerAbilityAnim(player);

                                player.level().playSound(null, player, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.2F, 1.0F);
                                player.level().playSound(null, player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                            }
                        } else if (msg.button == 2 && msg.press) {
                            if (player.getPersistentData().getInt("xebTearsA2Cooldown") <= 0 || devBypass) {
                                player.getPersistentData().putInt("xebTearsA2Cooldown", 400); // 20s CD
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 400, 0)); // 20s
                                player.getPersistentData().putInt("xebTearsBurstCount", 6);
                                player.getPersistentData().putInt("xebTearsBurstTimer", 0);
                                player.getPersistentData().putBoolean("xebNextBrimstoneInstant", true);
                                
                                org.xeb.xeb.item.TheTearsItem.triggerAbilityAnim(player);

                                player.level().playSound(null, player, SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 1.0F, 1.5F);
                                player.level().playSound(null, player, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.6F, 1.6F);
                            }
                        }
                    } else if (holdsMecha) {
                        if (msg.button == 1) {
                            if (msg.press) {
                                if (player.getPersistentData().getInt("xebMechaA1Cooldown") <= 0) {
                                    // Jet Dash (Triggered instantly)
                                    player.getPersistentData().putBoolean("xebMechaOverdriveDashing", true);
                                    player.getPersistentData().putInt("xebMechaDashTicks", 15);
                                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                            SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0F, 1.2F);
                                    player.getPersistentData().putInt("xebMechaA1Cooldown", 160); // 8s
                                    
                                    // Consume overcharge on action
                                    player.getPersistentData().putBoolean("xebMechaOvercharged", false);
                                    
                                    org.xeb.xeb.item.MechaOverdriveItem.syncToClient((ServerPlayer) player);
                                }
                            }
                        } else if (msg.button == 2) {
                            if (msg.press) {
                                if (player.getPersistentData().getInt("xebMechaA2Cooldown") <= 0) {
                                    CompoundTag pData = player.getPersistentData();
                                    if (!pData.contains("xebSpindashCharges") || pData.getInt("xebSpindashCharges") <= 0) {
                                        pData.putInt("xebSpindashCharges", 3);
                                    }
                                    
                                    int charges = pData.getInt("xebSpindashCharges");
                                    if (charges > 0 && pData.getInt("xebMechaSpindashState") == 0) {
                                        // Start Spindash Charge
                                        pData.putInt("xebMechaSpindashState", 1);
                                        pData.putInt("xebMechaSpindashCharge", 0);
                                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                                SoundEvents.MINECART_RIDING, SoundSource.PLAYERS, 0.8F, 1.5F);
                                        
                                        org.xeb.xeb.item.MechaOverdriveItem.syncToClient((ServerPlayer) player);
                                    }
                                }
                            } else {
                                // Spindash Release
                                CompoundTag pData = player.getPersistentData();
                                if (pData.getInt("xebMechaSpindashState") == 1) {
                                    int charges = pData.getInt("xebSpindashCharges");
                                    
                                    // Floricultural Zeal refund check on Mecha Overdrive (The Mech II Core)
                                    int zealLvl = 0;
                                    net.minecraft.world.item.ItemStack mechaStack = player.getMainHandItem();
                                    if (mechaStack.getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem) {
                                        zealLvl = mechaStack.getEnchantmentLevel(org.xeb.xeb.enchantment.ModEnchantments.FLORICULTURAL_ZEAL.get());
                                    } else {
                                        mechaStack = player.getOffhandItem();
                                        if (mechaStack.getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem) {
                                            zealLvl = mechaStack.getEnchantmentLevel(org.xeb.xeb.enchantment.ModEnchantments.FLORICULTURAL_ZEAL.get());
                                        }
                                    }
                                    
                                    boolean refunded = false;
                                    if (zealLvl > 0 && player.getRandom().nextFloat() < zealLvl * 0.15F) {
                                        refunded = true;
                                    }
                                    
                                    if (!refunded) {
                                        charges--;
                                    } else {
                                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 2.0F);
                                    }
                                    pData.putInt("xebSpindashCharges", charges);
                                    Vec3 playerLook = player.getLookAngle().normalize();

                                    // 1. Resolve Locked-on Target or Fallback
                                    int targetId = pData.getInt("xebSpindashTargetId");
                                    net.minecraft.world.entity.Entity lockTarget = null;
                                    if (targetId != -1) {
                                        lockTarget = player.level().getEntity(targetId);
                                    }
                                    
                                    LivingEntity nearest = null;
                                    if (lockTarget instanceof LivingEntity living && living.isAlive() && player.distanceToSqr(living) <= 256.0D) {
                                        nearest = living;
                                    } else {
                                        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16.0D),
                                                e -> e != player && e.isAlive() && !(e instanceof Player p && p.isAlliedTo(player)));
                                        double minDistance = Double.MAX_VALUE;
                                        for (LivingEntity t : targets) {
                                            double dist = player.distanceToSqr(t);
                                            if (dist < minDistance) {
                                                Vec3 toTarget = t.position().subtract(player.position()).normalize();
                                                if (playerLook.dot(toTarget) > 0.3D) {
                                                    minDistance = dist;
                                                    nearest = t;
                                                }
                                            }
                                        }
                                    }
                                    
                                    Vec3 attackVector = playerLook;
                                    if (nearest != null) {
                                        Vec3 targetCenter = nearest.getBoundingBox().getCenter();
                                        Vec3 diff = targetCenter.subtract(player.position().add(0, player.getEyeHeight() / 2.0D, 0));
                                        attackVector = diff.normalize();
                                        
                                        // Face player towards targeted entity
                                        player.setYRot((float) Math.toDegrees(Math.atan2(-attackVector.x, attackVector.z)));
                                        player.setXRot((float) Math.toDegrees(-Math.atan2(attackVector.y, Math.sqrt(attackVector.x * attackVector.x + attackVector.z * attackVector.z))));
                                    }
                                    
                                    // Save the locked vector
                                    pData.putDouble("xebSpindashVectorX", attackVector.x);
                                    pData.putDouble("xebSpindashVectorY", attackVector.y);
                                    pData.putDouble("xebSpindashVectorZ", attackVector.z);
                                    
                                    int charge = pData.getInt("xebMechaSpindashCharge");
                                    int duration = 20 + charge;
                                    pData.putInt("xebMechaSpindashState", 3); // ATTACKING
                                    pData.putInt("xebMechaSpindashTicks", duration);
                                    pData.putInt("xebMechaSpindashHitCooldown", 0);
                                    pData.putInt("xebSpindashHitCount", 0);
                                    
                                    double mult = 1.0D + charge * 0.08D;
                                    player.setDeltaMovement(attackVector.x * mult * 1.5D, attackVector.y * mult * 1.5D, attackVector.z * mult * 1.5D);
                                    player.hurtMarked = true;
                                    
                                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 1.0F);
                                    
                                    // 2. Launch Homing Missile
                                    HomingMissileEntity missile = new HomingMissileEntity(player.level(), player, nearest);
                                    missile.moveTo(player.getX(), player.getY() + 1.2D, player.getZ());
                                    missile.setDeltaMovement(attackVector.scale(1.2D));
                                    player.level().addFreshEntity(missile);
                                    
                                    // If all 3 charges expended: start cooldown and consume overcharged state
                                    if (charges <= 0) {
                                        pData.putInt("xebMechaA2Cooldown", 300); // 15s
                                        pData.putBoolean("xebMechaOvercharged", false);
                                    }
                                    
                                    org.xeb.xeb.item.MechaOverdriveItem.syncToClient((ServerPlayer) player);
                                }
                            }
                        } else if (msg.button == 5 && msg.press) {
                            // Mecha Drill Punch (Left click)
                            Vec3 lookDir = player.getLookAngle();
                            Vec3 forwardVec = new Vec3(lookDir.x, 0.0D, lookDir.z).normalize();
                            AABB area = player.getBoundingBox().move(forwardVec.scale(1.5D)).inflate(1.5D, 1.0D, 1.5D);
                            List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
                            
                            double momentum = player.getPersistentData().getDouble("xebMechaMomentum");
                            double multiplier = 1.0D + momentum * 2.0D;
                            boolean overcharged = player.getPersistentData().getBoolean("xebMechaOvercharged");
                            
                            if (overcharged) {
                                multiplier *= 1.5D;
                            }
                            
                            double finalDmg = MechaOverdriveItem.DRILL_BASE_DAMAGE * multiplier;

                            for (LivingEntity target : targets) {
                                target.hurt(player.damageSources().playerAttack(player), (float) finalDmg);
                                
                                double kbMult = 1.0D + momentum * 2.0D;
                                if (overcharged || momentum > 0.7D) {
                                    // Extreme knockback + screen shake on targets
                                    Vec3 knock = lookDir.normalize().add(0, 0.6D, 0).scale(kbMult * 2.2D);
                                    target.setDeltaMovement(knock.x, knock.y, knock.z);
                                    target.hurtMarked = true;
                                } else {
                                    Vec3 knock = lookDir.normalize().add(0, 0.4D, 0).scale(kbMult * 1.5D);
                                    target.setDeltaMovement(knock.x, knock.y, knock.z);
                                    target.hurtMarked = true;
                                }
                            }
                            
                            if (overcharged) {
                                // Knockback applies to all enemies in 3 blocks radius
                                AABB radialArea = player.getBoundingBox().inflate(3.0D);
                                for (LivingEntity radialTarget : player.level().getEntitiesOfClass(LivingEntity.class, radialArea, e -> e != player && e.isAlive() && !targets.contains(e))) {
                                    Vec3 knockVec = radialTarget.position().subtract(player.position()).normalize().add(0, 0.5D, 0).scale(1.8D);
                                    radialTarget.setDeltaMovement(knockVec);
                                    radialTarget.hurtMarked = true;
                                }
                            }

                            Vec3 recoil = lookDir.normalize().scale(-0.3D * momentum);
                            player.setDeltaMovement(player.getDeltaMovement().add(recoil.x, 0.0D, recoil.z));
                            player.hurtMarked = true;

                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.8F);
                                    
                            // Consume momentum and overcharge
                            player.getPersistentData().putDouble("xebMechaMomentum", 0.0D);
                            player.getPersistentData().putBoolean("xebMechaOvercharged", false);
                            
                            org.xeb.xeb.item.MechaOverdriveItem.syncToClient((ServerPlayer) player);
                        } else if (msg.button == 6 && msg.press) {
                            // Air Jump Burst (Double Jump)
                            CompoundTag pData = player.getPersistentData();
                            int charges = pData.getInt("xebSpindashCharges");
                            if (charges > 0 && !player.onGround()) {
                                pData.putInt("xebSpindashCharges", charges - 1);
                                pData.putInt("xebMechaAirBurstTicks", 20); // 1 second speed boost
                                
                                // Push player up and preserve/boost horizontal momentum slightly
                                Vec3 mot = player.getDeltaMovement();
                                player.setDeltaMovement(mot.x * 1.25D, 0.58D, mot.z * 1.25D);
                                player.hurtMarked = true;
                                
                                // Sound effects
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 1.2F);
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.PLAYERS, 1.0F, 1.1F);
                                
                                // Spawn burst of flames behind the player
                                if (player.level() instanceof ServerLevel serverLevel) {
                                    Vec3 look = player.getLookAngle().normalize();
                                    Vec3 spawnPos = player.position().add(0, player.getBbHeight() * 0.5D, 0).subtract(look.scale(0.5D));
                                    Vec3 blastDir = look.scale(-0.4D);
                                    
                                    for (int i = 0; i < 20; i++) {
                                        double rx = (serverLevel.random.nextDouble() - 0.5D) * 0.25D;
                                        double ry = (serverLevel.random.nextDouble() - 0.5D) * 0.25D;
                                        double rz = (serverLevel.random.nextDouble() - 0.5D) * 0.25D;
                                        serverLevel.sendParticles(ParticleTypes.FLAME, 
                                                spawnPos.x + rx, spawnPos.y + ry, spawnPos.z + rz,
                                                1, blastDir.x, blastDir.y, blastDir.z, 0.25D);
                                        if (serverLevel.random.nextBoolean()) {
                                            serverLevel.sendParticles(ParticleTypes.LAVA,
                                                    spawnPos.x, spawnPos.y, spawnPos.z,
                                                    1, blastDir.x, blastDir.y, blastDir.z, 0.08D);
                                        }
                                    }
                                }
                                
                                org.xeb.xeb.item.MechaOverdriveItem.syncToClient((ServerPlayer) player);
                            }
                        }
                    } else if (holdsHoly) {
                        if (msg.button == 1 && msg.press) {
                            if (player.getPersistentData().getInt("xebHolyA1Cooldown") <= 0) {
                                player.getPersistentData().putBoolean("xebHolyBlessedActive", true);
                                player.getPersistentData().putInt("xebHolyBlessedTicks", 160); // 8s
                                player.getPersistentData().putBoolean("xebHolyShieldActive", true);
                                player.getPersistentData().putInt("xebHolyA1Cooldown", 320); // 16s (320 ticks)
                                
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.5F);
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8F, 1.5F);
                                        
                                org.xeb.xeb.item.HolyDualityBladeItem.syncToClient((ServerPlayer) player);
                            }
                        } else if (msg.button == 2 && msg.press) {
                            if (player.getPersistentData().getInt("xebHolyA2Cooldown") <= 0) {
                                double maxDist = 12.0D;
                                Vec3 eyePosition = player.getEyePosition(1.0F);
                                Vec3 lookAngle = player.getLookAngle();
                                Vec3 traceEnd = eyePosition.add(lookAngle.scale(maxDist));

                                // Raycast para bloques
                                net.minecraft.world.phys.HitResult blockHit = player.pick(maxDist, 1.0F, false);
                                Vec3 targetVec = blockHit.getLocation();

                                // Raycast para entidades
                                AABB searchArea = player.getBoundingBox().expandTowards(lookAngle.scale(maxDist)).inflate(2.0D);
                                LivingEntity targetEntity = null;
                                double closestDist = maxDist;

                                for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, searchArea, e -> e != player && e.isAlive())) {
                                    AABB entityBox = target.getBoundingBox().inflate(0.5D);
                                    java.util.Optional<Vec3> clip = entityBox.clip(eyePosition, traceEnd);
                                    if (clip.isPresent()) {
                                        double dist = eyePosition.distanceTo(clip.get());
                                        if (dist < closestDist) {
                                            closestDist = dist;
                                            targetEntity = target;
                                        }
                                    }
                                }

                                if (targetEntity != null) {
                                    // Teleportarse cerca de la entidad
                                    Vec3 targetPos = targetEntity.position();
                                    Vec3 offset = lookAngle.scale(-1.2D);
                                    targetVec = new Vec3(targetPos.x + offset.x, targetPos.y, targetPos.z + offset.z);
                                } else {
                                    // Si es bloque, retroceder un poco para no quedar atrapado
                                    Vec3 offset = lookAngle.scale(-1.0D);
                                    targetVec = targetVec.add(offset);
                                }

                                // Teleport al target
                                player.teleportTo(targetVec.x, targetVec.y, targetVec.z);

                                // NUEVO: Spin de 3 vueltas, 7 daño cada una, 3x3 AoE por vuelta
                                player.getPersistentData().putBoolean("xebHolyAnnihilationActive", true);
                                player.getPersistentData().putInt("xebHolyAnnihilationTicks", 30); // 30 ticks = 3 vueltas de 10 ticks
                                player.getPersistentData().putInt("xebHolyAnnihilationHits", 0); // contador de vueltas
                                player.getPersistentData().putDouble("xebHolyAnnihilationX", targetVec.x);
                                player.getPersistentData().putDouble("xebHolyAnnihilationY", targetVec.y);
                                player.getPersistentData().putDouble("xebHolyAnnihilationZ", targetVec.z);
                                player.getPersistentData().putInt("xebHolyA2Cooldown", 133); // 6.66s

                                // Sonido de teleport
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);

                                org.xeb.xeb.item.HolyDualityBladeItem.syncToClient((ServerPlayer) player);
                            }
                        } else if (msg.button == 5 && msg.press) {
                            // Holy Left Click combo
                            net.minecraft.world.item.ItemStack stack = player.getMainHandItem().is(ModItems.HOLY_DUALITY_BLADE.get())
                                     ? player.getMainHandItem() : player.getOffhandItem();
                            int combo = stack.getOrCreateTag().getInt("xebHolyComboStage"); // 0, 1, 2
                            
                            AABB box;
                            double baseDmg = HolyDualityBladeItem.BASE_DAMAGE;
                            
                            if (combo == 0) {
                                // Right slash
                                Vec3 cross = player.getLookAngle().normalize().cross(new Vec3(0, 1, 0)).normalize();
                                box = player.getBoundingBox().move(cross.scale(1.2D)).inflate(1.5D);
                            } else if (combo == 1) {
                                // Left slash
                                Vec3 cross = player.getLookAngle().normalize().cross(new Vec3(0, 1, 0)).normalize();
                                box = player.getBoundingBox().move(cross.scale(-1.2D)).inflate(1.5D);
                            } else {
                                // Double slash (X)
                                box = player.getBoundingBox().move(player.getLookAngle().normalize().scale(1.5D)).inflate(2.0D, 1.5D, 2.0D);
                                baseDmg = 14.0D;
                            }

                            // Advance combo stage
                            int nextCombo = (combo + 1) % 3;
                            stack.getOrCreateTag().putInt("xebHolyComboStage", nextCombo);

                            boolean isBlessed = player.getPersistentData().getBoolean("xebHolyBlessedActive");
                            if (isBlessed) {
                                baseDmg *= 3.0D;
                            }

                            boolean isCrit = isBlessed || (player.getRandom().nextFloat() < 0.25F);

                            List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
                            for (LivingEntity target : targets) {
                                float finalDmg = (float) baseDmg;
                                if (isCrit) {
                                    finalDmg *= 1.5F;
                                }
                                
                                target.hurt(player.damageSources().playerAttack(player), finalDmg);

                                if (isCrit) {
                                    if (player.level() instanceof ServerLevel sl) {
                                        sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(),
                                                8, 0.2D, 0.2D, 0.2D, 0.15D);
                                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0D, target.getZ(),
                                                5, 0.2D, 0.2D, 0.2D, 0.05D);
                                        sl.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + 1.0D, target.getZ(),
                                                3, 0.1D, 0.1D, 0.1D, 0.0D);
                                    }
                                    
                                    // Apply Charred Burn for 3 seconds (60 ticks)
                                    net.minecraft.nbt.CompoundTag targetNBT = target.getPersistentData();
                                    int currentTicks = targetNBT.getInt("xebCharredBurnTicks");
                                    int currentStack = targetNBT.getInt("xebCharredBurnStack");
                                    if (currentTicks > 0) {
                                        targetNBT.putInt("xebCharredBurnStack", currentStack + 1);
                                    } else {
                                        targetNBT.putInt("xebCharredBurnStack", 1);
                                    }
                                    targetNBT.putInt("xebCharredBurnTicks", 60);
                                    targetNBT.putUUID("xebCharredBurnAttacker", player.getUUID());
                                }

                                net.minecraft.nbt.CompoundTag targetNBT = target.getPersistentData();
                                long lastHitTime = targetNBT.getLong("xebLastHolyHitTime");
                                int lastCombo = targetNBT.getInt("xebLastHolyCombo");
                                long now = player.level().getGameTime();

                                if (now - lastHitTime <= 15 && lastCombo != combo) {
                                    // Stagger
                                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 9));
                                    player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.5F);
                                }

                                targetNBT.putLong("xebLastHolyHitTime", now);
                                targetNBT.putInt("xebLastHolyCombo", combo);
                            }

                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.2F);
                            if (combo == 2) {
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.5F);
                            }
                            player.swing(InteractionHand.MAIN_HAND, true);
                            org.xeb.xeb.item.HolyDualityBladeItem.syncToClient((ServerPlayer) player);
                        }
                    }
                }
        });
        ctx.setPacketHandled(true);
    }
}
