package org.xeb.xeb.event;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.effect.ManaLeechEffect;
import org.xeb.xeb.mana.ManaManager;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BuffTickHandler {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!org.xeb.xeb.Config.enabled) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        // ── Tinfoil Hat: cure Madness, Fear, and Petrify ──
        if (org.xeb.xeb.compat.ModCompatManager.hasHelmetOrCurio(entity, org.xeb.xeb.item.ModItems.TINFOIL_HAT.get())) {
            if (entity.hasEffect(ModEffects.MADNESS.get())) {
                entity.removeEffect(ModEffects.MADNESS.get());
            }
            if (entity.hasEffect(ModEffects.FEAR.get())) {
                entity.removeEffect(ModEffects.FEAR.get());
            }
            if (entity.hasEffect(ModEffects.PETRIFY.get())) {
                entity.removeEffect(ModEffects.PETRIFY.get());
            }
        }

        // ── Hot Potato: apply Charred Burn to entities carrying it ──
        if (org.xeb.xeb.compat.ModCompatManager.hasHotPotato(entity) && !entity.isInWater()) {
            net.minecraft.world.effect.MobEffectInstance activeEffect = entity.getEffect(ModEffects.CHARRED_BURN.get());
            if (activeEffect == null || activeEffect.getDuration() <= 20) {
                entity.addEffect(new MobEffectInstance(ModEffects.CHARRED_BURN.get(), 40, 0, false, false, true));
            }
        }

        // ── Burn: clear effect if entity is in water ──
        if (entity.hasEffect(ModEffects.BURN.get()) && entity.isInWater()) {
            entity.removeEffect(ModEffects.BURN.get());
        }

        ServerLevel level = (ServerLevel) entity.level();
        List<MedallionData> medallions = MedallionManager.getMedallions(entity);

        // ── Medallion buff ticks ──────────────────────────────────────────────
        if (!medallions.isEmpty()) {
            for (MedallionData m : medallions) {
                m.getBuff().onServerTick(entity, level);
            }
        }

        // ── Mana regeneration (1 mana/s) — blocked by NMR ────────────────────
        if (entity.tickCount % 20 == 0) {
            if (!entity.hasEffect(ModEffects.NO_MANA_REGEN.get())) {
                ManaManager.regenMana(entity, 1.0D);
            }
        }

        // ── Mana Leeches — drain mana each second and give to applicator ─────
        if (entity.hasEffect(ModEffects.MANA_LEECH.get()) && entity.tickCount % 20 == 0) {
            MobEffectInstance leechEffect = entity.getEffect(ModEffects.MANA_LEECH.get());
            if (leechEffect != null) {
                float drainPercent = ManaLeechEffect.getDrainPercent(leechEffect.getAmplifier());
                double maxMana = ManaManager.getMaxMana(entity);
                double drainAmount = maxMana * drainPercent;

                if (drainAmount > 0) {
                    boolean drained = ManaManager.drainMana(entity, drainAmount);
                    if (drained) {
                        // Give drained mana to applicator if still alive and in range
                        int applicatorId = entity.getPersistentData().getInt(ManaLeechEffect.APPLICATOR_KEY);
                        if (applicatorId != 0) {
                            net.minecraft.world.entity.Entity applicatorEnt = level.getEntity(applicatorId);
                            if (applicatorEnt instanceof LivingEntity applicator && applicator.isAlive()) {
                                // Add mana to applicator (cap at their max)
                                double applicatorMax = ManaManager.getMaxMana(applicator);
                                double applicatorCurrent = ManaManager.getMana(applicator);
                                double give = Math.min(drainAmount, applicatorMax - applicatorCurrent);
                                if (give > 0) {
                                    // Forge back via custom attr — addMana helper inline
                                    net.minecraft.nbt.CompoundTag appTag = applicator.getPersistentData();
                                    appTag.putDouble("xebCurrentMana", Math.min(applicatorMax, applicatorCurrent + give));
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Doomed — kill entity when effect duration reaches 1 tick ─────────
        if (entity.hasEffect(ModEffects.DOOMED.get())) {
            MobEffectInstance doomedEffect = entity.getEffect(ModEffects.DOOMED.get());
            if (doomedEffect != null && doomedEffect.getDuration() <= 1) {
                // Instant death — use a high damage value that bypasses armour
                entity.hurt(entity.damageSources().magic(), Float.MAX_VALUE);
            }
        }

        // ── Delayed Pain — deal accumulated damage when effect expires ─────────
        if (entity.hasEffect(ModEffects.DELAYED_PAIN.get())) {
            MobEffectInstance effect = entity.getEffect(ModEffects.DELAYED_PAIN.get());
            if (effect != null && effect.getDuration() <= 1) {
                double accumulated = entity.getPersistentData().getDouble("xebDelayedPainAccumulated");
                entity.getPersistentData().remove("xebDelayedPainAccumulated");
                
                UUID attackerUuid = null;
                if (entity.getPersistentData().contains("xebDelayedPainAttacker")) {
                    attackerUuid = entity.getPersistentData().getUUID("xebDelayedPainAttacker");
                    entity.getPersistentData().remove("xebDelayedPainAttacker");
                }
                
                if (accumulated > 0) {
                    entity.getPersistentData().putBoolean("xebDelayedPainDied", true);
                    entity.getPersistentData().putBoolean("xebDelayedPainTriggering", true);
                    
                    net.minecraft.world.damagesource.DamageSource src = entity.damageSources().magic();
                    if (attackerUuid != null) {
                        Entity attacker = level.getEntity(attackerUuid);
                        if (attacker != null) {
                            src = entity.damageSources().indirectMagic(attacker, attacker);
                        }
                    }
                    
                    entity.hurt(src, (float) accumulated);
                    entity.getPersistentData().remove("xebDelayedPainTriggering");
                    if (entity.isAlive()) {
                        entity.getPersistentData().remove("xebDelayedPainDied");
                    }
                }
            }
        }

        // ── Adrenaline consistency — remove if Exhausted is gone ─────────────
        if (entity.hasEffect(ModEffects.ADRENALINE.get()) && !entity.hasEffect(ModEffects.EXHAUSTED.get())) {
            entity.removeEffect(ModEffects.ADRENALINE.get());
        }

        // ── Exhausted ↔ Adrenaline mutual link (polling, safe alternative to
        //    MobEffectEvent.Added which can silently fail) ─────────────────────
        if (entity.tickCount % 20 == 0) {
            if (entity.hasEffect(ModEffects.EXHAUSTED.get()) && !entity.hasEffect(ModEffects.ADRENALINE.get())) {
                MobEffectInstance ex = entity.getEffect(ModEffects.EXHAUSTED.get());
                int dur = ex != null ? ex.getDuration() : 600;
                entity.addEffect(new MobEffectInstance(
                        ModEffects.ADRENALINE.get(), dur, 0, false, true, true));
            }
        }

        // ── Fear (players) — smooth per-tick velocity nudge ───────────────────
        // Runs every tick with a tiny force so the push feels continuous, not jarring.
        if (entity.hasEffect(ModEffects.FEAR.get())
                && entity instanceof net.minecraft.world.entity.player.Player fearPlayer) {
            org.xeb.xeb.effect.FearEffect.applyPlayerFearTick(fearPlayer);
        }

        // ── Petrify: restore NoAI state when effect expires ──────────────────
        if (entity instanceof Mob mob) {
            if (mob.getPersistentData().getBoolean("xebRestoreAI") && !mob.hasEffect(ModEffects.PETRIFY.get())) {
                mob.getPersistentData().remove("xebRestoreAI");
                mob.setNoAi(false);
            }
        }

        // ── Player-specific Holy Shield & Doomfist gauntlet ticks ──────────────
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
            boolean hasMantle = org.xeb.xeb.compat.ModCompatManager.hasCurioOrOffhand(player, org.xeb.xeb.item.ModItems.HOLY_MANTLE.get());

            if (tag.contains("xebPlayerHolyShieldTimer")) {
                int timer = tag.getInt("xebPlayerHolyShieldTimer");
                if (timer > 0) {
                    tag.putInt("xebPlayerHolyShieldTimer", timer - 1);
                    if (timer % 20 == 0) {
                        com.mojang.logging.LogUtils.getLogger().info("Player {} Holy Shield cooldown: {} seconds remaining", player.getName().getString(), timer / 20);
                    }
                } else {
                    tag.remove("xebPlayerHolyShieldTimer");
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.5F);
                    // Send particle packet for restoration
                    BuffParticlePacket packet = new BuffParticlePacket(player.getX(), player.getY(), player.getZ(), "revival", 15);
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
                    
                    // Re-apply shield immediately now that cooldown has ended
                    if (hasMantle) {
                        player.addEffect(new MobEffectInstance(ModEffects.HOLY_SHIELD.get(), 40, 0, false, false, false));
                    }
                }
            } else {
                // Passively apply holy shield if holding/wearing Holy Mantle and not on cooldown
                if (hasMantle) {
                    MobEffectInstance active = player.getEffect(ModEffects.HOLY_SHIELD.get());
                    if (active == null || active.getDuration() <= 10) {
                        player.addEffect(new MobEffectInstance(ModEffects.HOLY_SHIELD.get(), 40, 0, false, false, false));
                    }
                }
            }

            // --- Doomfist gauntlet dash collision logic ---
            if (tag.getBoolean("xebDoomfistDashing")) {
                int dashTimer = tag.getInt("xebDoomfistDashTimer");
                if (dashTimer > 0) {
                    tag.putInt("xebDoomfistDashTimer", dashTimer - 1);

                    // Perform collision search around player
                    float chargeRatio = tag.getFloat("xebDoomfistChargeRatio");
                    net.minecraft.world.phys.AABB hitBox = player.getBoundingBox().inflate(1.2D);
                    List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                            e -> e != player && e.isAlive() && !e.isAlliedTo(player));

                    if (!targets.isEmpty()) {
                        LivingEntity target = targets.get(0);
                         
                         boolean holdsV2 = player.getMainHandItem().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get()) || player.getOffhandItem().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get());
                         float baseDamage = holdsV2 ? 7.0F : 10.0F;
                         float damage = baseDamage + chargeRatio * 15.0F; // Max 25 (or 22 on V2) damage
                         boolean isUltra = tag.getBoolean("xebDashIsUltraCharged");
                         if (isUltra) {
                             // Ultra Charge raises weapon base to 14, charge ratio still multiplies on top
                             damage = 14.0F + chargeRatio * 15.0F;
                             tag.remove("xebDashIsUltraCharged");
                             
                             level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY(0.5D), target.getZ(), 3, 0.5D, 0.5D, 0.5D, 0.0D);
                             level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, target.getX(), target.getY(0.5D), target.getZ(), 30, 1.0D, 1.0D, 1.0D, 0.2D);
                             
                             level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                     net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.8F);
                         }

                        // Deal initial damage
                        target.hurt(player.damageSources().playerAttack(player), damage);

                        // Push back
                        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                        double kb = 1.5D + chargeRatio * 2.5D; // Max 4.0 knockback
                        target.knockback(kb, -look.x, -look.z);
                        target.hurtMarked = true;

                        // Start tracking wall-slam target
                        net.minecraft.nbt.CompoundTag targetTag = target.getPersistentData();
                        targetTag.putInt("xebDoomfistSlamTimer", 15);
                        targetTag.putFloat("xebDoomfistSlamDamage", damage);
                        targetTag.putUUID("xebDoomfistSlamAttacker", player.getUUID());

                        // Sounds and particles
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.7F);
                        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                                net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);

                        // Stop player's dash
                        tag.remove("xebDoomfistDashing");
                        tag.remove("xebDoomfistDashTimer");
                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new org.xeb.xeb.network.DoomfistDashPacket(player.getId(), false, 0.0F));
                        player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);
                        player.hurtMarked = true;
                    }
                } else {
                    // Dash ended without collision
                    tag.remove("xebDoomfistDashing");
                    tag.remove("xebDoomfistDashTimer");
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new org.xeb.xeb.network.DoomfistDashPacket(player.getId(), false, 0.0F));
                }
            }

            // --- Doomfist Seismic Slam server ticking ---
            if (tag.contains("xebSlamState")) {
                int slamState = tag.getInt("xebSlamState");
                if (slamState == 1) { // Casting
                    int slamTimer = tag.getInt("xebSlamTimer");
                    if (slamTimer > 0) {
                        tag.putInt("xebSlamTimer", slamTimer - 1);
                        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                        player.hurtMarked = true;
                    } else {
                        // Cast complete: raycast along crosshair, projecting down to ground if it misses
                        tag.putInt("xebSlamState", 2);
                        net.minecraft.world.phys.Vec3 landPos = getSlamTarget(player, player.level());
                        
                        // Store target coordinates in NBT
                        tag.putDouble("xebSlamTargetX", landPos.x);
                        tag.putDouble("xebSlamTargetY", landPos.y);
                        tag.putDouble("xebSlamTargetZ", landPos.z);
                        
                        // Dive directly towards the landing target
                        net.minecraft.world.phys.Vec3 slamVec = landPos.subtract(player.position());
                        net.minecraft.world.phys.Vec3 dir = slamVec.lengthSqr() > 1E-4 ? slamVec.normalize() : player.getLookAngle();
                        player.setDeltaMovement(dir.scale(1.8D)); // 1.8D speed for swift diagonal dive
                        player.hurtMarked = true;
                        
                        // Sync state and target coordinates to clients
                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new org.xeb.xeb.network.DoomfistAbilitySyncPacket(player.getId(), 0, 2, landPos.x, landPos.y, landPos.z));
                    }
                } else if (slamState == 2) { // Slamming downward
                    double targetX = tag.getDouble("xebSlamTargetX");
                    double targetY = tag.getDouble("xebSlamTargetY");
                    double targetZ = tag.getDouble("xebSlamTargetZ");
                    
                    net.minecraft.world.phys.Vec3 targetPos = new net.minecraft.world.phys.Vec3(targetX, targetY, targetZ);
                    net.minecraft.world.phys.Vec3 slamVec = targetPos.subtract(player.position());
                    
                    // Override velocity every tick to guide player directly to targeted landing spot
                    if (slamVec.lengthSqr() > 0.01D) {
                        net.minecraft.world.phys.Vec3 dir = slamVec.normalize();
                        player.setDeltaMovement(dir.scale(1.8D));
                        player.hurtMarked = true;
                    }
                    
                    double distToTargetSq = player.position().distanceToSqr(targetX, targetY, targetZ);
                    if (player.onGround() || player.getDeltaMovement().y >= 0.0D || distToTargetSq <= 2.25D) {
                        tag.remove("xebSlamState");
                        tag.remove("xebSlamTimer");
                        tag.remove("xebSlamTargetX");
                        tag.remove("xebSlamTargetY");
                        tag.remove("xebSlamTargetZ");
                        
                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new org.xeb.xeb.network.DoomfistAbilitySyncPacket(player.getId(), 0, 0));
                        
                        executeSlamImpact(player);
                    }
                }
            }

            // --- Doomfist Earthquake Slam (v2) server ticking ---
            if (tag.contains("xebSlam2State")) {
                int slam2State = tag.getInt("xebSlam2State");
                int slam2Timer = tag.getInt("xebSlam2Timer");
                
                if (slam2Timer > 0) {
                    if (slam2Timer > 12) {
                        tag.putInt("xebSlam2Timer", slam2Timer - 1);
                    }
                    net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                    net.minecraft.world.phys.Vec3 forward = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z).normalize();
                    
                    if (slam2Timer > 12) { // Rising Phase (arc up and forward)
                        // Cancel/Float trigger if player presses Jump (player.jumping == true)
                        boolean isJumping = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(net.minecraft.world.entity.LivingEntity.class, player, "f_20899_");
                        if (isJumping) {
                            tag.remove("xebSlam2State");
                            tag.remove("xebSlam2Timer");
                            
                            // Float for 2.0 seconds (40 ticks)
                            tag.putInt("xebUppercutFloatTicks", 40);
                            
                            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                    new org.xeb.xeb.network.DoomfistAbilitySyncPacket(player.getId(), 40, 0));
                                    
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                        } else {
                            // Air steering: blend current look direction into horizontal velocity
                            net.minecraft.world.phys.Vec3 currentLook = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z).normalize();
                            net.minecraft.world.phys.Vec3 curVel = player.getDeltaMovement();
                            double blendedX = curVel.x * 0.7D + currentLook.x * 0.7D * 0.3D;
                            double blendedZ = curVel.z * 0.7D + currentLook.z * 0.7D * 0.3D;
                            double ySpeed = 0.4D + (slam2Timer - 12) * 0.05D;
                            player.setDeltaMovement(blendedX, ySpeed, blendedZ);
                            player.hurtMarked = true;
                        }
                    } else { // Falling Phase (slam down and forward)
                        // Air steering during fall: player can redirect horizontally
                        net.minecraft.world.phys.Vec3 currentLook = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z).normalize();
                        net.minecraft.world.phys.Vec3 curVel = player.getDeltaMovement();
                        double blendedX = curVel.x * 0.65D + currentLook.x * 0.8D * 0.35D;
                        double blendedZ = curVel.z * 0.65D + currentLook.z * 0.8D * 0.35D;
                        player.setDeltaMovement(blendedX, -0.9D, blendedZ);
                        player.hurtMarked = true;
                        
                        // If they hit the ground or are very close to hitting solid blocks downward, trigger impact!
                        boolean hitFloor = player.onGround();
                        if (!hitFloor) {
                            net.minecraft.world.phys.AABB checkAABB = player.getBoundingBox().move(0.0D, -0.3D, 0.0D);
                            if (!player.level().noCollision(player, checkAABB)) {
                                hitFloor = true;
                            }
                        }

                        if (hitFloor) {
                            tag.remove("xebSlam2State");
                            tag.remove("xebSlam2Timer");
                            
                            // Save impact time for bunny hop (up to 10 ticks window)
                            tag.putLong("xebSlam2ImpactTime", player.level().getGameTime());
                            
                            executeEarthquakeImpact((net.minecraft.server.level.ServerPlayer) player);
                        }
                    }
                } else {
                    // Timer expired - fire impact regardless of ground state
                    tag.remove("xebSlam2State");
                    tag.remove("xebSlam2Timer");
                    tag.putLong("xebSlam2ImpactTime", player.level().getGameTime());
                    executeEarthquakeImpact((net.minecraft.server.level.ServerPlayer) player);
                }
            }

            // --- Doomfist v2 Bunny Hop check ---
            if (tag.contains("xebSlam2ImpactTime")) {
                long impactTime = tag.getLong("xebSlam2ImpactTime");
                long gameTime = player.level().getGameTime();
                if (gameTime - impactTime <= 10) {
                    boolean isJumping = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(net.minecraft.world.entity.LivingEntity.class, player, "f_20899_");
                    if (isJumping) {
                        tag.remove("xebSlam2ImpactTime");
                        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                        net.minecraft.world.phys.Vec3 forward = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z).normalize();
                        
                        // Parabolic forward/up bunny hop boost!
                        player.setDeltaMovement(forward.x * 1.2D, 0.45D, forward.z * 1.2D);
                        player.hurtMarked = true;
                        
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.PLAYER_BREATH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
                    }
                } else {
                    tag.remove("xebSlam2ImpactTime");
                }
            }

            // --- Doomfist Power Block server ticking ---
            if (tag.getBoolean("xebPowerBlocking")) {
                int blockTimer = tag.getInt("xebBlockTimer");
                tag.putInt("xebBlockTimer", blockTimer + 1);
                
                boolean keyHeld = tag.getBoolean("xebBlockKeyHeld");
                
                // End blocking if held past 2.5s (50 ticks) OR (held at least 0.3s (6 ticks) and released key)
                if (blockTimer >= 50 || (blockTimer >= 6 && !keyHeld)) {
                    tag.remove("xebPowerBlocking");
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new org.xeb.xeb.network.DoomfistPowerBlockSyncPacket(player.getId(), false));
                    tag.remove("xebBlockTimer");
                    tag.remove("xebBlockKeyHeld");
                    
                    // Remove speed modifier
                    net.minecraft.world.entity.ai.attributes.AttributeInstance speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                    if (speed != null) {
                        speed.removeModifier(java.util.UUID.fromString("6a04870c-26d9-4828-98e9-44d4715f606e"));
                    }
                    
                    // Trigger cooldown (7s = 140 ticks)
                    tag.putLong("xebBlockLastTime", player.level().getGameTime());
                    
                    // Sync cooldown to client
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new org.xeb.xeb.network.DoomfistAbilitySyncPacket(player.getId(), 0, 0, 0.0D, 0.0D, 0.0D, 0, 140));
                            
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.PISTON_CONTRACT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
                }
            }

            // --- Doomfist Earthquake Slam (v2) propagating red shockwave visuals ---
            if (tag.contains("xebSlam2ShockwaveStep")) {
                int step = tag.getInt("xebSlam2ShockwaveStep");
                double lx = tag.getDouble("xebSlam2ShockwaveLookX");
                double lz = tag.getDouble("xebSlam2ShockwaveLookZ");
                
                net.minecraft.world.phys.Vec3 horizLook = new net.minecraft.world.phys.Vec3(lx, 0.0D, lz).normalize();
                net.minecraft.world.phys.Vec3 pos = player.position();
                
                double dist = step * 1.0D;
                net.minecraft.world.phys.Vec3 center = pos.add(horizLook.scale(dist));
                net.minecraft.world.phys.Vec3 perp = new net.minecraft.world.phys.Vec3(-horizLook.z, 0.0D, horizLook.x).normalize();
                
                double width = step * 0.6D;
                for (double offset = -width; offset <= width; offset += 0.4D) {
                    net.minecraft.world.phys.Vec3 point = center.add(perp.scale(offset));
                    net.minecraft.core.particles.DustParticleOptions redDust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.1F, 0.1F), 1.2F);
                    
                    level.sendParticles(redDust, point.x, point.y + 0.1D, point.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                    if (player.getRandom().nextFloat() < 0.3F) {
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, point.x, point.y + 0.1D, point.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                    }
                }
                
                if (step < 7) {
                    tag.putInt("xebSlam2ShockwaveStep", step + 1);
                } else {
                    tag.remove("xebSlam2ShockwaveStep");
                    tag.remove("xebSlam2ShockwaveLookX");
                    tag.remove("xebSlam2ShockwaveLookZ");
                }
            }

            // Clear fall protection on ground contact with a 20-tick buffer to ensure damage event has processed
            if (player.onGround()) {
                int gpTicks = tag.getInt("xebFallProtectGroundTicks");
                if (gpTicks >= 20) {
                    tag.remove("xebDoomfistFallProtect");
                    tag.remove("xebFallProtectGroundTicks");
                } else {
                    tag.putInt("xebFallProtectGroundTicks", gpTicks + 1);
                }
            } else {
                tag.remove("xebFallProtectGroundTicks");
            }
        }

        // --- Doomfist general wall slam detection ---
        net.minecraft.nbt.CompoundTag entityTag = entity.getPersistentData();
        if (entityTag.contains("xebDoomfistSlamTimer")) {
            int slamTimer = entityTag.getInt("xebDoomfistSlamTimer");
            if (slamTimer > 0) {
                entityTag.putInt("xebDoomfistSlamTimer", slamTimer - 1);

                if (entity.horizontalCollision) {
                    entityTag.remove("xebDoomfistSlamTimer");
                    float initialDamage = entityTag.getFloat("xebDoomfistSlamDamage");

                    // Deal double damage (apply the same damage amount again as extra damage!)
                    net.minecraft.world.damagesource.DamageSource slamSource = entity.damageSources().generic();
                    if (entityTag.contains("xebDoomfistSlamAttacker")) {
                        UUID attackerUuid = entityTag.getUUID("xebDoomfistSlamAttacker");
                        net.minecraft.world.entity.Entity attacker = level.getEntity(attackerUuid);
                        if (attacker instanceof net.minecraft.world.entity.player.Player pAttacker) {
                            slamSource = entity.damageSources().playerAttack(pAttacker);
                        }
                    }

                    entity.hurt(slamSource, initialDamage);

                    // Impact sounds
                    level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.9F);
                    level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);

                    // Explosion particles
                    if (entity.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                                entity.getX(), entity.getY(0.5D), entity.getZ(), 5, 0.3D, 0.3D, 0.3D, 0.0D);
                    }
                }
            } else {
                entityTag.remove("xebDoomfistSlamTimer");
                entityTag.remove("xebDoomfistSlamDamage");
                entityTag.remove("xebDoomfistSlamAttacker");
            }
        }

        // --- Doomfist general uppercut floating logic ---
        if (entityTag.contains("xebUppercutFloatTicks")) {
            int floatTicks = entityTag.getInt("xebUppercutFloatTicks");
            if (floatTicks > 0) {
                entityTag.putInt("xebUppercutFloatTicks", floatTicks - 1);
                if (!entity.onGround()) {
                    net.minecraft.world.phys.Vec3 motion = entity.getDeltaMovement();
                    // Only lock Y motion once they hit the peak of the jump (Y motion becomes non-positive)
                    if (motion.y <= 0.0D) {
                        entity.setDeltaMovement(motion.x, 0.0D, motion.z);
                    }
                } else {
                    // Only clear if they've been launched (e.g. floatTicks has ticked down a bit)
                    if (floatTicks < 35) {
                        entityTag.remove("xebUppercutFloatTicks");
                    }
                }
            } else {
                entityTag.remove("xebUppercutFloatTicks");
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (!org.xeb.xeb.Config.enabled) return;
        LivingEntity user = event.getEntity();
        if (user == null || user.level().isClientSide()) return;

        double searchRadius = 8.0D;
        net.minecraft.world.phys.AABB aabb = user.getBoundingBox().inflate(searchRadius);
        List<LivingEntity> nearby = user.level().getEntitiesOfClass(LivingEntity.class, aabb);

        for (LivingEntity nearbyEntity : nearby) {
            List<MedallionData> nearbyMedallions = MedallionManager.getMedallions(nearbyEntity);
            for (MedallionData m : nearbyMedallions) {
                if (m.getBuff() instanceof org.xeb.xeb.buff.impl.ResonantBuff resonantBuff) {
                    resonantBuff.handleNearbyItemUse(nearbyEntity, event);
                }
            }
        }
    }

    private static void executeSlamImpact(net.minecraft.world.entity.player.Player player) {
        net.minecraft.world.level.Level level = player.level();
        net.minecraft.world.phys.Vec3 pos = player.position();
        
        net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(7.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player));
                
        double baseDamage = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float damage = (float) (baseDamage * 0.6D);
        
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 horizLook = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z).normalize();
        
        for (LivingEntity target : targets) {
            net.minecraft.world.phys.Vec3 offset = target.position().subtract(pos);
            double dist = offset.horizontalDistance();
            
            if (dist <= 6.0D && Math.abs(offset.y) <= 2.0D) {
                net.minecraft.world.phys.Vec3 horizOffsetDir = new net.minecraft.world.phys.Vec3(offset.x, 0.0D, offset.z).normalize();
                double dot = horizLook.dot(horizOffsetDir);
                
                // Cone check: 60-degree angle (cos(30) ~ 0.866)
                if (dot >= 0.866D) {
                    target.hurt(player.damageSources().playerAttack(player), damage);
                    
                    // Pull target slightly towards player and pop them up
                    net.minecraft.world.phys.Vec3 pullDir = pos.subtract(target.position()).normalize();
                    target.setDeltaMovement(pullDir.x * 0.3D, 0.4D, pullDir.z * 0.3D);
                    target.hurtMarked = true;
                }
            }
        }
        
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.9F);
                
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, player.getX(), player.getY(0.5D), player.getZ(), 8, 0.5D, 0.2D, 0.5D, 0.1D);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY(0.5D), player.getZ(), 15, 1.0D, 0.2D, 1.0D, 0.2D);
        }
    }

    public static net.minecraft.world.phys.Vec3 getSlamTarget(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level) {
        net.minecraft.world.phys.Vec3 start = player.getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = start.add(look.scale(15.0D));
        
        net.minecraft.world.phys.BlockHitResult raycast = level.clip(new net.minecraft.world.level.ClipContext(
                start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        
        if (raycast.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            return raycast.getLocation();
        } else {
            // Missed block, project downward from the end point to stick to the floor
            net.minecraft.world.phys.Vec3 downEnd = new net.minecraft.world.phys.Vec3(end.x, end.y - 30.0D, end.z);
            net.minecraft.world.phys.BlockHitResult groundRay = level.clip(new net.minecraft.world.level.ClipContext(
                    end, downEnd, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            return groundRay.getLocation();
        }
    }

    private static void executeEarthquakeImpact(net.minecraft.server.level.ServerPlayer player) {
        net.minecraft.world.level.Level level = player.level();
        net.minecraft.world.phys.Vec3 pos = player.position();
        
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 horizLook = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z).normalize();
        
        // Trigger propagating red shockwave visuals
        player.getPersistentData().putInt("xebSlam2ShockwaveStep", 1);
        player.getPersistentData().putDouble("xebSlam2ShockwaveLookX", horizLook.x);
        player.getPersistentData().putDouble("xebSlam2ShockwaveLookZ", horizLook.z);
        
        net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(6.0D, 3.0D, 6.0D);
        java.util.List<net.minecraft.world.entity.LivingEntity> targets = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player));
                
        double baseDamage = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float damage = (float) (baseDamage * 0.8D);
        
        for (net.minecraft.world.entity.LivingEntity target : targets) {
            net.minecraft.world.phys.Vec3 offset = target.position().subtract(pos);
            double dist = offset.horizontalDistance();
            
            if (dist <= 6.0D && Math.abs(offset.y) <= 2.0D) {
                net.minecraft.world.phys.Vec3 horizOffsetDir = new net.minecraft.world.phys.Vec3(offset.x, 0.0D, offset.z).normalize();
                double dot = horizLook.dot(horizOffsetDir);
                
                if (dot >= 0.866D) {
                    target.hurt(player.damageSources().playerAttack(player), damage);
                    
                    net.minecraft.world.phys.Vec3 pullDir = pos.subtract(target.position()).normalize();
                    target.setDeltaMovement(pullDir.x * 0.3D, 0.4D, pullDir.z * 0.3D);
                    target.hurtMarked = true;
                }
            }
        }
        
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.7F);
                
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, player.getX(), player.getY(0.5D), player.getZ(), 8, 0.5D, 0.2D, 0.5D, 0.1D);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, player.getX(), player.getY(0.5D), player.getZ(), 25, 1.0D, 0.2D, 1.0D, 0.2D);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, player.getX(), player.getY(0.5D), player.getZ(), 12, 0.8D, 0.2D, 0.8D, 0.1D);

            // Circular POOF dust shockwave particles expanding outward
            for (int i = 0; i < 16; i++) {
                double angle = (i * Math.PI * 2.0D) / 16.0D;
                double dx = Math.cos(angle) * 1.5D;
                double dz = Math.sin(angle) * 1.5D;
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, player.getX() + dx, player.getY() + 0.1D, player.getZ() + dz, 1, 0.1D, 0.0D, 0.1D, 0.02D);
            }
        }
    }
}
