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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

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
                boolean hasUltimate = org.xeb.xeb.item.QuantumCatBarrageItem.hasUltimateCurio(player);

                if (!holdsV1 && !holdsV2 && !holdsOpticBlast && !holdsGoldenFlower && !holdsCD && !holdsTears && !hasUltimate) return;

                long time = player.level().getGameTime();

                // Handle Ultimate Curio Activa 3 Key Press (Global, compatible with any weapon status)
                if (msg.button == 3 && msg.press && hasUltimate) {
                    if (!player.getCooldowns().isOnCooldown(ModItems.QUANTUM_CAT_BARRAGE.get())) {
                        float totalDamage = org.xeb.xeb.item.QuantumCatBarrageItem.getDamageDealtLast60Seconds(player);
                        AABB area = player.getBoundingBox().inflate(10.0D);
                        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                                e -> e != player && e.isAlive() && !e.isAlliedTo(player));

                        if (!targets.isEmpty()) {
                            float damagePerTarget = totalDamage / targets.size();
                            for (LivingEntity target : targets) {
                                float finalDmg = Math.max(2.0F, damagePerTarget);
                                target.hurt(player.damageSources().playerAttack(player), finalDmg);
                                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                        org.xeb.xeb.effect.ModEffects.NO_HEALTH_REGEN.get(), 200, 0
                                ));

                                if (player.level() instanceof ServerLevel serverLevel) {
                                    for (int i = 0; i < 4; i++) {
                                        double ox = (player.getRandom().nextDouble() - 0.5D) * 1.2D;
                                        double oy = player.getRandom().nextDouble() * target.getBbHeight();
                                        double oz = (player.getRandom().nextDouble() - 0.5D) * 1.2D;
                                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                                target.getX() + ox, target.getY() + oy, target.getZ() + oz,
                                                1, 0.0D, 0.0D, 0.0D, 0.0D);
                                    }
                                    serverLevel.sendParticles(ParticleTypes.CRIT,
                                            target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                                            8, 0.3D, 0.3D, 0.3D, 0.2D);
                                }
                            }

                            player.level().playSound(null, player, SoundEvents.CAT_HISS, SoundSource.PLAYERS, 1.2F, 1.2F);
                            player.level().playSound(null, player, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);

                            player.getCooldowns().addCooldown(ModItems.QUANTUM_CAT_BARRAGE.get(), 3600); // 180 seconds cooldown
                        }
                    }
                    return;
                }

                if (holdsGoldenFlower) {
                    if (msg.button == 1) {
                        // --- Flower Dance (Activa 1) ---
                        if (msg.press) {
                            int danceCD = player.getPersistentData().getInt("xebGoldenFlowerDanceCooldown");
                            if (danceCD > 0) return; // 20s cooldown

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
                            if (time - lastCyclone < org.xeb.xeb.item.OpticBlastItem.CYCLONE_PUSH_COOLDOWN) return;

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
                            if (time - lastSplice < org.xeb.xeb.item.OpticBlastItem.GENE_SPLICE_COOLDOWN) return;

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
                        if (time - lastUppercut < 100) return;
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
                        if (time - lastSlam < 120) return;
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
                        if (time - lastSlam2 < 140) return; // 7 seconds cooldown
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
                             if (time - lastBlock < 140) return; // 7 seconds cooldown

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
                        
                        if (player.getPersistentData().getInt("xebCDA1CooldownTicks") > 0) return;
                        
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
                        
                        if (player.getPersistentData().getInt("xebCDA2CooldownTicks") > 0) return;
                        
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
                    if (msg.button == 1 && msg.press) {
                            if (player.getPersistentData().getInt("xebTearsA1Cooldown") <= 0) {
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
                                
                                player.level().playSound(null, player, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.2F, 1.0F);
                                player.level().playSound(null, player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                            }
                        } else if (msg.button == 2 && msg.press) {
                            if (player.getPersistentData().getInt("xebTearsA2Cooldown") <= 0) {
                                player.getPersistentData().putInt("xebTearsA2Cooldown", 400); // 20s CD
                                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 400, 0)); // 20s
                                player.getPersistentData().putInt("xebTearsBurstCount", 6);
                                player.getPersistentData().putInt("xebTearsBurstTimer", 0);
                                player.getPersistentData().putBoolean("xebNextBrimstoneInstant", true);
                                
                                player.level().playSound(null, player, SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 1.0F, 1.5F);
                                player.level().playSound(null, player, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.6F, 1.6F);
                            }
                        }
                    }
                }
        });
        ctx.setPacketHandled(true);
    }
}
