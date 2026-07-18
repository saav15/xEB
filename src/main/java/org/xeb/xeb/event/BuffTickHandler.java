package org.xeb.xeb.event;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.effect.ManaLeechEffect;
import org.xeb.xeb.mana.ManaManager;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.network.CrazyDiamondSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BuffTickHandler {
    public static final java.util.Map<UUID, net.minecraft.server.level.ServerBossEvent> ACTIVE_BOSS_BARS = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!org.xeb.xeb.Config.enabled) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        // === Spore Cloud ticking (Golden Flower retorno) ===
        if (entity instanceof ServerPlayer sp) {
            int sporeTicks = sp.getPersistentData().getInt("xebSporeCloudTicks");
            if (sporeTicks > 0) {
                sp.getPersistentData().putInt("xebSporeCloudTicks", sporeTicks - 1);
                
                double sx = sp.getX();
                double sy = sp.getY();
                double sz = sp.getZ();
                sp.getPersistentData().putDouble("xebSporeCloudX", sx);
                sp.getPersistentData().putDouble("xebSporeCloudY", sy);
                sp.getPersistentData().putDouble("xebSporeCloudZ", sz);
                
                ServerLevel sl = sp.serverLevel();
                
                // Partículas de esporas
                if (sp.level().getGameTime() % 3 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = sl.random.nextDouble() * Math.PI * 2.0;
                        double radius = org.xeb.xeb.item.GoldenFlowerItem.SPORE_RADIUS * sl.random.nextDouble();
                        double px = sx + Math.cos(angle) * radius;
                        double pz = sz + Math.sin(angle) * radius;
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, px, sy + 0.5, pz,
                                1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                
                // Dañar mobs hostiles en el área cada 20 ticks
                if (sp.level().getGameTime() % 20 == 0) {
                    double radius = org.xeb.xeb.item.GoldenFlowerItem.SPORE_RADIUS;
                    net.minecraft.world.phys.AABB sporeBox = new net.minecraft.world.phys.AABB(sx - radius, sy - 1, sz - radius,
                                             sx + radius, sy + 3, sz + radius);
                    List<LivingEntity> hostileTargets = sl.getEntitiesOfClass(LivingEntity.class, sporeBox,
                            e -> e instanceof net.minecraft.world.entity.Mob mob
                                    && mob.getTarget() == sp
                                    && e.isAlive()
                                    && e != sp);
                    
                    for (LivingEntity hostile : hostileTargets) {
                        hostile.hurt(sp.damageSources().magic(), org.xeb.xeb.item.GoldenFlowerItem.SPORE_DAMAGE_PER_TICK);
                        hostile.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, false));
                    }
                }
            }
        }

        // ── Madness / Mega Boss Bar (N19) ──
        boolean qualifies = false;
        boolean hasMadness = false;
        int madStacks = 0;
        boolean hasMultipleMega = false;

        if (entity.isAlive() && !entity.isRemoved()) {
            hasMadness = MedallionManager.hasBuff(entity, "mad");
            madStacks = entity.getPersistentData().getInt("xebMadStacks");
            boolean escalatedMadness = hasMadness && madStacks > 0;

            int megaCount = 0;
            for (MedallionData m : MedallionManager.getMedallions(entity)) {
                if (m.getBuff().getId().equals("mega")) {
                    megaCount++;
                }
            }
            hasMultipleMega = megaCount >= 2;

            qualifies = escalatedMadness || hasMultipleMega;
        }

        if (qualifies) {
            boolean finalHasMultipleMega = hasMultipleMega;
            int finalMadStacks = madStacks;
            net.minecraft.server.level.ServerBossEvent bossEvent = ACTIVE_BOSS_BARS.computeIfAbsent(entity.getUUID(), uuid -> {
                net.minecraft.world.BossEvent.BossBarColor color = finalHasMultipleMega ? net.minecraft.world.BossEvent.BossBarColor.PURPLE : net.minecraft.world.BossEvent.BossBarColor.RED;
                net.minecraft.server.level.ServerBossEvent bar = new net.minecraft.server.level.ServerBossEvent(
                        entity.getDisplayName(),
                        color,
                        net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS
                );
                bar.setVisible(true);
                return bar;
            });

            bossEvent.setProgress(entity.getHealth() / entity.getMaxHealth());
            if (hasMadness) {
                bossEvent.setName(net.minecraft.network.chat.Component.literal(entity.getDisplayName().getString() + " (Madness x" + finalMadStacks + ")"));
            } else {
                bossEvent.setName(entity.getDisplayName());
            }

            if (entity.level() instanceof ServerLevel serverLevel) {
                java.util.Set<ServerPlayer> currentPlayers = new java.util.HashSet<>();
                for (ServerPlayer player : serverLevel.players()) {
                    if (player.distanceToSqr(entity) <= 32.0D * 32.0D) {
                        currentPlayers.add(player);
                    }
                }
                java.util.Set<ServerPlayer> trackingPlayers = new java.util.HashSet<>(bossEvent.getPlayers());
                for (ServerPlayer player : currentPlayers) {
                    if (!trackingPlayers.contains(player)) {
                        bossEvent.addPlayer(player);
                    }
                }
                for (ServerPlayer player : trackingPlayers) {
                    if (!currentPlayers.contains(player)) {
                        bossEvent.removePlayer(player);
                    }
                }
            }
        } else {
            net.minecraft.server.level.ServerBossEvent bossEvent = ACTIVE_BOSS_BARS.remove(entity.getUUID());
            if (bossEvent != null) {
                bossEvent.removeAllPlayers();
            }
        }


        // ── Golden Flower Weapon Server Ticks ──
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            net.minecraft.nbt.CompoundTag pData = player.getPersistentData();

            // Elite Mastery Sync (N24)
            if (player instanceof ServerPlayer serverPlayer) {
                int baseLvl = MedallionManager.getEliteMeterLevel(serverPlayer, false);
                int lastSyncedLvl = pData.getInt("xebLastSyncedEliteLvl");
                if (baseLvl != lastSyncedLvl || serverPlayer.tickCount % 40 == 0) {
                    pData.putInt("xebLastSyncedEliteLvl", baseLvl);
                    XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new XEBNetwork.EliteMasterySyncPacket(baseLvl));
                }
            }

            boolean holdsFlower = player.getMainHandItem().is(org.xeb.xeb.item.ModItems.GOLDEN_FLOWER.get())
                    || player.getOffhandItem().is(org.xeb.xeb.item.ModItems.GOLDEN_FLOWER.get());
            
            // Handle Seismic Slam absorption decay
            if (pData.contains("xebSeismicAbsorptionTimer")) {
                int timer = pData.getInt("xebSeismicAbsorptionTimer") - 1;
                if (timer <= 0) {
                    float amt = pData.getFloat("xebSeismicAbsorptionAmount");
                    player.setAbsorptionAmount(Math.max(0.0F, player.getAbsorptionAmount() - amt));
                    pData.remove("xebSeismicAbsorptionTimer");
                    pData.remove("xebSeismicAbsorptionAmount");
                } else {
                    pData.putInt("xebSeismicAbsorptionTimer", timer);
                }
            }

            if (!pData.contains("xebGoldenFlowerCharges")) pData.putInt("xebGoldenFlowerCharges", 6);
            if (!pData.contains("xebGoldenFlowerRechargeTimer")) pData.putInt("xebGoldenFlowerRechargeTimer", 0);
            if (!pData.contains("xebJaronaCharges")) pData.putInt("xebJaronaCharges", 3);
            if (!pData.contains("xebJaronaRechargeTimer")) pData.putInt("xebJaronaRechargeTimer", 0);
            if (!pData.contains("xebGoldenFlowerDanceCooldown")) pData.putInt("xebGoldenFlowerDanceCooldown", 0);
            if (!pData.contains("xebFlowerDanceActive")) pData.putBoolean("xebFlowerDanceActive", false);
            if (!pData.contains("xebFlowerDanceTicksRemaining")) pData.putInt("xebFlowerDanceTicksRemaining", 0);
            if (!pData.contains("xebJaronaDashTicks")) pData.putInt("xebJaronaDashTicks", 0);

            int charges = pData.getInt("xebGoldenFlowerCharges");
            int rechargeTimer = pData.getInt("xebGoldenFlowerRechargeTimer");
            int jaronaCharges = pData.getInt("xebJaronaCharges");
            int jaronaTimer = pData.getInt("xebJaronaRechargeTimer");
            int danceCD = pData.getInt("xebGoldenFlowerDanceCooldown");
            boolean danceActive = pData.getBoolean("xebFlowerDanceActive");
            int danceTicks = pData.getInt("xebFlowerDanceTicksRemaining");
            int jaronaDashTicks = pData.getInt("xebJaronaDashTicks");

            int oldCharges = charges;
            int oldJaronaCharges = jaronaCharges;
            int oldDanceCD = danceCD;

            // Omega Flowery active: cooldowns tick down 2× per tick (50% shorter)
            int cdDecrement = pData.getBoolean("xebOmegaFloweryActive")
                    ? org.xeb.xeb.extremeburst.OmegaFloweryHandler.COOLDOWN_TICKS_PER_TICK_OMEGA
                    : org.xeb.xeb.extremeburst.OmegaFloweryHandler.COOLDOWN_TICKS_PER_TICK_NORMAL;

            // Tick Dance active state
            if (danceActive) {
                danceTicks--;
                pData.putInt("xebFlowerDanceTicksRemaining", danceTicks);
                if (danceTicks <= 0) {
                    pData.putBoolean("xebFlowerDanceActive", false);
                    danceActive = false;
                }
            }
            if (danceCD > 0) {
                danceCD = Math.max(0, danceCD - cdDecrement);
                pData.putInt("xebGoldenFlowerDanceCooldown", danceCD);
            }

            // Passive Kinetic Spikes 1 + loadedCount (Only active when actually holding the weapon)
            if (holdsFlower && !danceActive) {
                int loaded = pData.getInt("xebGoldenFlowerLoadedCount");
                int amp = Math.max(0, loaded);
                MobEffectInstance activeSpikes = player.getEffect(ModEffects.KINETIC_SPIKES.get());
                if (activeSpikes == null || activeSpikes.getDuration() <= 20 || activeSpikes.getAmplifier() != amp) {
                    player.addEffect(new MobEffectInstance(ModEffects.KINETIC_SPIKES.get(), 40, amp, false, false, true));
                }
            } else {
                player.removeEffect(ModEffects.KINETIC_SPIKES.get());
            }

            // Passive flower recharge (every 1.5 s / 30 ticks; 50% faster = 20 ticks with Omega Flowery)
            int rechargeInterval = pData.getBoolean("xebOmegaFloweryActive")
                    ? org.xeb.xeb.extremeburst.OmegaFloweryHandler.CHARGE_RECHARGE_INTERVAL_OMEGA
                    : org.xeb.xeb.extremeburst.OmegaFloweryHandler.CHARGE_RECHARGE_INTERVAL_NORMAL;
            if (charges < 6) {
                rechargeTimer++;
                if (rechargeTimer >= rechargeInterval) {
                    charges++;
                    rechargeTimer = 0;
                }
                pData.putInt("xebGoldenFlowerCharges", charges);
                pData.putInt("xebGoldenFlowerRechargeTimer", rechargeTimer);
            }

            // Passive Jarona recharge (every 6 s / 120 ticks) — runs in background
            if (jaronaCharges < 3) {
                jaronaTimer++;
                if (jaronaTimer >= 120) {
                    jaronaCharges++;
                    jaronaTimer = 0;
                }
                pData.putInt("xebJaronaCharges", jaronaCharges);
                pData.putInt("xebJaronaRechargeTimer", jaronaTimer);
            }

            // Jarona dash ticks - AABB sweeps for damage
            if (jaronaDashTicks > 0) {
                jaronaDashTicks--;
                pData.putInt("xebJaronaDashTicks", jaronaDashTicks);

                int comboStep = pData.getInt("xebJaronaComboStep");
                float damage = 8.0F;
                if (comboStep == 2) damage = 14.0F;
                else if (comboStep == 3) damage = 20.0F;

                // Omega Flowery: Final Jarona — apply to each target hit below
                final boolean finalJaronaActive = player instanceof net.minecraft.server.level.ServerPlayer sp
                        && org.xeb.xeb.extremeburst.OmegaFloweryHandler.isOmegaFloweryActive(sp);
                final float finalJaronaBaseDmg = damage;

                // AABB sweep in front of player
                Vec3 eye = player.getEyePosition(1.0F);
                Vec3 look = player.getLookAngle();
                Vec3 center = eye.add(look.scale(1.8D));
                net.minecraft.world.phys.AABB sweepBox = new net.minecraft.world.phys.AABB(
                        center.x - 1.5D, center.y - 1.2D, center.z - 1.5D,
                        center.x + 1.5D, center.y + 1.2D, center.z + 1.5D
                );

                List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, sweepBox,
                        e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator() && e.isPickable());

                if (!targets.isEmpty()) {
                    String hitKey = "xebJaronaHitEntityIdsStep" + comboStep + "_" + pData.getLong("xebLastJaronaTime");
                    for (LivingEntity target : targets) {
                        String idStr = String.valueOf(target.getId());
                        String hitList = pData.getString(hitKey);
                        if (!hitList.contains(idStr)) {
                            pData.putString(hitKey, hitList + "," + idStr);

                            // Apply Final Jarona multiplier if Omega Flowery is active
                            float effectiveDamage = finalJaronaBaseDmg;
                            if (finalJaronaActive && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                                effectiveDamage = org.xeb.xeb.extremeburst.OmegaFloweryHandler
                                        .applyFinalJaronaDamage(sp, target, finalJaronaBaseDmg);
                            }

                            // Deal damage
                            net.minecraft.world.damagesource.DamageSource src = player.damageSources().mobAttack(player);
                            target.hurt(src, effectiveDamage);

                            // Knockback
                            target.knockback(0.4D + (comboStep * 0.2D), -look.x, -look.z);

                            // Impact sound
                            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.7F + (comboStep * 0.2F));
                        }
                    }
                }
            }

            // Sync final state conditionally (to minimize network traffic when not holding)
            boolean shouldSync = holdsFlower;
            if (!holdsFlower) {
                if (charges != oldCharges || jaronaCharges != oldJaronaCharges || (danceCD == 0 && oldDanceCD > 0)) {
                    shouldSync = true;
                }
            }

            if (shouldSync && player instanceof ServerPlayer serverPlayer) {
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new org.xeb.xeb.network.GoldenFlowerSyncPacket(
                                player.getId(),
                                charges,
                                rechargeTimer,
                                pData.getInt("xebGoldenFlowerLoadedCount"),
                                jaronaCharges,
                                jaronaTimer,
                                danceCD
                        ));
            }
        }

        // ── Extreme Burst ticking ─────────────────────────────────────────────
        if (entity instanceof ServerPlayer serverPlayer) {
            // Dogma brimstone beam
            if (serverPlayer.getPersistentData().contains("xebDogmaBrimstoneTicks")) {
                org.xeb.xeb.extremeburst.DogmaBurstHandler.tick(serverPlayer);
            }

            // Omega Flowery instance
            if (serverPlayer.getPersistentData().contains("xebOmegaFloweryTicks")) {
                org.xeb.xeb.extremeburst.OmegaFloweryHandler.tick(serverPlayer);
            }

            // General Instance duration countdown
            int instanceTicks = serverPlayer.getPersistentData().getInt("xebExtremeBurstInstanceTicks");
            if (instanceTicks > 0) {
                serverPlayer.getPersistentData().putInt("xebExtremeBurstInstanceTicks", instanceTicks - 1);
                if (instanceTicks <= 1) {
                    serverPlayer.getPersistentData().putBoolean("xebExtremeBurstActive", false);
                }
            }
        }

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
                            e -> e != player && e.isAlive() && !e.isAlliedTo(player) && !(e instanceof org.xeb.xeb.entity.CrazyDiamondEntity));

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
                        tag.putDouble("xebSlamStartY", player.getY()); // Store starting Y for height damage calculations
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

            // --- Crazy Diamond Cooldowns ---
            if (tag.contains("xebCDA1CooldownTicks")) {
                int cd = tag.getInt("xebCDA1CooldownTicks");
                if (cd > 0) tag.putInt("xebCDA1CooldownTicks", cd - 1);
            }
            if (tag.contains("xebCDA2CooldownTicks")) {
                int cd = tag.getInt("xebCDA2CooldownTicks");
                if (cd > 0) tag.putInt("xebCDA2CooldownTicks", cd - 1);
            }

            // --- Crazy Diamond Levitation/Slow Falling ---
            if (tag.contains("xebCDLevitateTicks")) {
                int levTicks = tag.getInt("xebCDLevitateTicks");
                if (levTicks > 0) {
                    tag.putInt("xebCDLevitateTicks", levTicks - 1);
                    net.minecraft.world.phys.Vec3 motion = player.getDeltaMovement();
                    if (motion.y < -0.05D) {
                        player.setDeltaMovement(motion.x, -0.05D, motion.z);
                        player.hurtMarked = true;
                    }
                    player.fallDistance = 0.0F;
                } else {
                    tag.remove("xebCDLevitateTicks");
                }
            }

            // --- Crazy Diamond Barrage Ticking (Dorarara!) ---
            if (tag.contains("xebCDBarrageTimer")) {
                int timer = tag.getInt("xebCDBarrageTimer");
                if (timer > 0) {
                    tag.putInt("xebCDBarrageTimer", timer - 1);
                    
                    // Slow fall force
                    Vec3 motion = player.getDeltaMovement();
                    if (motion.y < -0.05D) {
                        player.setDeltaMovement(motion.x, -0.05D, motion.z);
                        player.hurtMarked = true;
                    }
                    player.fallDistance = 0.0F;

                    // Tick damage & healing every 10 ticks (0.5s)
                    if (timer % 10 == 0) {
                        int N = tag.getInt("xebCDActiveBarrages");
                        if (N > 0) {
                            List<LivingEntity> allies = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10.0D),
                                    (e) -> e != player && e.isAlive() && (player.isAlliedTo(e) || (e instanceof net.minecraft.world.entity.OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID()))));
                            
                            double searchRange = N == 1 ? 3.0D : (N == 2 ? 5.0D : 8.0D);
                            List<LivingEntity> enemies = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(searchRange),
                                    (e) -> e != player && e.isAlive() && !e.isAlliedTo(player) && !(e instanceof org.xeb.xeb.entity.CrazyDiamondEntity));
                            
                            if (!enemies.isEmpty()) {
                                LivingEntity firstEnemy = enemies.get(0);
                                if (player.level() instanceof ServerLevel serverLevel) {
                                    for (Entity e : serverLevel.getAllEntities()) {
                                        if (e instanceof org.xeb.xeb.entity.CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                                            cds.setPos(firstEnemy.getX(), firstEnemy.getY(), firstEnemy.getZ());
                                            cds.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, firstEnemy.getEyePosition());
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            int H = 0;
                            int A = 0;
                            if (!allies.isEmpty()) {
                                if (!enemies.isEmpty()) {
                                    H = 1;
                                    A = N - 1;
                                } else {
                                    H = N;
                                    A = 0;
                                }
                            } else {
                                H = 0;
                                A = N;
                            }
                            
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.4F);
                            
                            if (H > 0) {
                                allies.sort((a, b) -> Float.compare(a.getHealth() / a.getMaxHealth(), b.getHealth() / b.getMaxHealth()));
                                LivingEntity allyToHeal = allies.get(0);
                                float healAmt = (4.5F * 0.3F * 0.5F) * H; // 30% healing of 4.5 damage/sec
                                allyToHeal.heal(healAmt);
                                if (player.level() instanceof ServerLevel serverLevel) {
                                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, 
                                            allyToHeal.getX(), allyToHeal.getY(0.5D), allyToHeal.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.05D);
                                }
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.3F, 1.8F);
                            }
                            
                            if (A > 0 && !enemies.isEmpty()) {
                                float damageAmt = 4.5F; // Flat 4.5 damage
                                for (LivingEntity enemy : enemies) {
                                    Vec3 enemyMotion = enemy.getDeltaMovement();
                                    enemy.getPersistentData().putString("xebLastAttackWeapon", "doomfist");
                                    enemy.getPersistentData().putString("xebLastAttackType", "ultimate");
                                    enemy.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
                                    enemy.hurt(player.damageSources().playerAttack(player), damageAmt);
                                    enemy.setDeltaMovement(enemyMotion);
                                    enemy.hurtMarked = true;
                                    if (player.level() instanceof ServerLevel serverLevel) {
                                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, 
                                                enemy.getX(), enemy.getY(0.5D), enemy.getZ(), 3, 0.1D, 0.1D, 0.1D, 0.1D);
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (timer - 1 <= 0) {
                    tag.remove("xebCDBarrageTimer");
                    tag.remove("xebCDActiveBarrages");
                    tag.remove("xebCDLevitateTicks");
                    
                    if (player.level() instanceof ServerLevel serverLevel) {
                        for (Entity e : serverLevel.getAllEntities()) {
                            if (e instanceof org.xeb.xeb.entity.CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                                cds.setAnimState(org.xeb.xeb.entity.CrazyDiamondEntity.STATE_EXHAUSTION, 15);
                                break;
                            }
                        }
                    }
                }
            }

            // --- Crazy Diamond Active 1 State Machine ---
            if (tag.contains("xebCDA1State")) {
                int state = tag.getInt("xebCDA1State");
                int timer = tag.getInt("xebCDA1Timer");
                
                if (timer > 0) {
                    tag.putInt("xebCDA1Timer", timer - 1);
                    
                    if (state == 1) { // Phase 1: Dashing & Searching target within 8 blocks
                        Vec3 look = player.getLookAngle();
                        player.setDeltaMovement(look.x * 2.0D, 0.1D, look.z * 2.0D);
                        player.hurtMarked = true;
                        
                        LivingEntity target = null;
                        double minDist = 8.0D;
                        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0D))) {
                            if (e != player && e.isAlive() && !e.isAlliedTo(player) && !(e instanceof org.xeb.xeb.entity.CrazyDiamondEntity)) {
                                double dist = player.distanceTo(e);
                                if (dist < minDist) {
                                    minDist = dist;
                                    target = e;
                                }
                            }
                        }
                        
                        if (target != null) {
                            tag.putInt("xebCDA1TargetId", target.getId());
                            tag.putInt("xebCDA1State", 2);
                            tag.putInt("xebCDA1Timer", 20); // 1s to approach target
                            
                            // Stop player dash
                            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                            player.hurtMarked = true;
                            
                            // Deal 8.0 damage
                            target.getPersistentData().putString("xebLastAttackWeapon", "crazy_diamond");
                            target.getPersistentData().putString("xebLastAttackType", "active1");
                            target.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
                            target.hurt(player.damageSources().playerAttack(player), 8.0F);
                            
                            // Pull target to player
                            Vec3 toPlayer = player.position().subtract(target.position()).normalize();
                            target.setDeltaMovement(toPlayer.x * 1.5D, 0.35D, toPlayer.z * 1.5D);
                            target.hurtMarked = true;
                            
                            // Move Stand to target
                            if (player.level() instanceof ServerLevel serverLevel) {
                                for (Entity e : serverLevel.getAllEntities()) {
                                    if (e instanceof org.xeb.xeb.entity.CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                                        cds.setPos(target.getX(), target.getY(), target.getZ());
                                        cds.setAnimState(org.xeb.xeb.entity.CrazyDiamondEntity.STATE_LOW_PUNCH, 6, target.getId());
                                        break;
                                    }
                                }
                            }
                            
                            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                                    net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.1F);
                        } else if (timer - 1 <= 0) { // Dashed but hit nothing: 4s cooldown
                            tag.remove("xebCDA1State");
                            tag.remove("xebCDA1Timer");
                            tag.remove("xebCDLevitateTicks");
                            tag.putInt("xebCDA1CooldownTicks", 80);
                            XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                                    new CrazyDiamondSyncPacket(80, tag.getInt("xebCDA2CooldownTicks"), tag.getInt("xebCDPunches"), tag.getInt("xebCDChargeTimer")));
                        }
                    } else if (state == 2) { // Phase 2: Approach target within 5 blocks
                        int targetId = tag.getInt("xebCDA1TargetId");
                        Entity targetEntity = level.getEntity(targetId);
                        if (targetEntity instanceof LivingEntity target && target.isAlive()) {
                            if (player.distanceTo(target) <= 5.0D) {
                                // Hit 9.0 damage
                                target.getPersistentData().putString("xebLastAttackWeapon", "crazy_diamond");
                                target.getPersistentData().putString("xebLastAttackType", "active2");
                                target.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
                                target.hurt(player.damageSources().playerAttack(player), 9.0F);
                                
                                // Petrify target (3s normally, 1s if Boss or Golden Medallion)
                                boolean hasGoldMedallion = false;
                                for (MedallionData m : MedallionManager.getMedallions(target)) {
                                    if (m.getTier() == org.xeb.xeb.medallion.MedallionType.LEGENDARY) {
                                        hasGoldMedallion = true;
                                        break;
                                    }
                                }
                                boolean isBoss = !target.canChangeDimensions() || target.getMaxHealth() > 40.0F || target instanceof net.minecraft.world.entity.player.Player;
                                int duration = (isBoss || hasGoldMedallion) ? 20 : 60;
                                
                                target.addEffect(new MobEffectInstance(org.xeb.xeb.effect.ModEffects.PETRIFY.get(), duration, 0));
                                
                                // Move Stand to target and play face punch
                                if (player.level() instanceof ServerLevel serverLevel) {
                                    for (Entity e : serverLevel.getAllEntities()) {
                                        if (e instanceof org.xeb.xeb.entity.CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                                            cds.setPos(target.getX(), target.getY(), target.getZ());
                                            cds.setAnimState(org.xeb.xeb.entity.CrazyDiamondEntity.STATE_FACE_PUNCH, 25, target.getId());
                                            break;
                                        }
                                    }
                                }
                                
                                // Transition to Phase 3: 2-second window to press DORA! again
                                tag.putInt("xebCDA1State", 3);
                                tag.putInt("xebCDA1Timer", 40); // 2 seconds (40 ticks) window
                                
                                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                                        net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.3F);
                            } else if (timer - 1 <= 0) { // Timed out before target got close: 9s cooldown
                                tag.remove("xebCDA1State");
                                tag.remove("xebCDA1Timer");
                                tag.remove("xebCDA1TargetId");
                                tag.remove("xebCDLevitateTicks");
                                tag.putInt("xebCDA1CooldownTicks", 180);
                                XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                                        new CrazyDiamondSyncPacket(180, tag.getInt("xebCDA2CooldownTicks"), tag.getInt("xebCDPunches"), tag.getInt("xebCDChargeTimer")));
                            }
                        } else { // Target died or is missing: 9s cooldown
                            tag.remove("xebCDA1State");
                            tag.remove("xebCDA1Timer");
                            tag.remove("xebCDA1TargetId");
                            tag.remove("xebCDLevitateTicks");
                            tag.putInt("xebCDA1CooldownTicks", 180);
                            XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                                    new CrazyDiamondSyncPacket(180, tag.getInt("xebCDA2CooldownTicks"), tag.getInt("xebCDPunches"), tag.getInt("xebCDChargeTimer")));
                        }
                    } else if (state == 3) { // Phase 3: Waiting for second DORA! key press
                        if (timer - 1 <= 0) { // Key window expired: 12s cooldown
                            tag.remove("xebCDA1State");
                            tag.remove("xebCDA1Timer");
                            tag.remove("xebCDA1TargetId");
                            tag.remove("xebCDLevitateTicks");
                            tag.putInt("xebCDA1CooldownTicks", 240);
                            XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                                    new CrazyDiamondSyncPacket(240, tag.getInt("xebCDA2CooldownTicks"), tag.getInt("xebCDPunches"), tag.getInt("xebCDChargeTimer")));
                        }
                    } else if (state == 4) { // Phase 4: Waiting for enhanced basic left click kick
                        if (timer - 1 <= 0) { // Kick window expired: 12s cooldown
                            tag.remove("xebCDA1State");
                            tag.remove("xebCDA1Timer");
                            tag.remove("xebCDA1TargetId");
                            tag.remove("xebCDLevitateTicks");
                            tag.putInt("xebCDA1CooldownTicks", 240);
                            XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                                    new CrazyDiamondSyncPacket(240, tag.getInt("xebCDA2CooldownTicks"), tag.getInt("xebCDPunches"), tag.getInt("xebCDChargeTimer")));
                        }
                    }
                }
            }

            // --- Crazy Diamond Active 2 State Machine ---
            if (tag.contains("xebCDA2State")) {
                int state = tag.getInt("xebCDA2State");
                int timer = tag.getInt("xebCDA2Timer");
                
                if (timer > 0) {
                    tag.putInt("xebCDA2Timer", timer - 1);
                } else {
                    org.xeb.xeb.entity.CrazyDiamondEntity stand = null;
                    if (player.level() instanceof ServerLevel serverLevel) {
                        for (Entity e : serverLevel.getAllEntities()) {
                            if (e instanceof org.xeb.xeb.entity.CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                                stand = cds;
                                break;
                            }
                        }
                    }

                    if (state == 1) {
                        tag.putInt("xebCDA2State", 2);
                        tag.putInt("xebCDA2Timer", 6);
                        if (stand != null) {
                            stand.setAnimState(org.xeb.xeb.entity.CrazyDiamondEntity.STATE_WIND_UP, 6);
                        }
                    } else if (state == 2) {
                        net.minecraft.world.level.block.state.BlockState blockState = player.level().getBlockState(player.blockPosition().below());
                        if (blockState.isAir()) {
                            blockState = net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState();
                        }
                        
                        org.xeb.xeb.entity.RestoreProjectileEntity proj = new org.xeb.xeb.entity.RestoreProjectileEntity(player.level(), player, blockState);
                        proj.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.0F, 0.0F); // 1.0F speed, 0.0F divergence (straight)
                        player.level().addFreshEntity(proj);
                        player.getPersistentData().putInt("xebCDA2ProjectileId", proj.getId());
                        
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.SNOWBALL_THROW, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                                
                        tag.putInt("xebCDA2State", 3);
                        tag.putInt("xebCDA2Timer", 6);
                        if (stand != null) {
                            stand.setAnimState(org.xeb.xeb.entity.CrazyDiamondEntity.STATE_THROWING, 6);
                        }
                    } else if (state == 3) {
                        tag.remove("xebCDA2State");
                        tag.remove("xebCDA2Timer");
                        if (stand != null) {
                            stand.setAnimState(org.xeb.xeb.entity.CrazyDiamondEntity.STATE_IDLE, 0);
                        }
                    }
                }
            }
        }

        // --- Crazy Diamond general wall slam detection ---
        if (entity.getPersistentData().contains("xebCDA1SlamTimer")) {
            int slamTimer = entity.getPersistentData().getInt("xebCDA1SlamTimer");
            if (slamTimer > 0) {
                entity.getPersistentData().putInt("xebCDA1SlamTimer", slamTimer - 1);

                if (entity.horizontalCollision) {
                    entity.getPersistentData().remove("xebCDA1SlamTimer");
                    float initialDamage = 6.0F;

                    net.minecraft.world.damagesource.DamageSource slamSource = entity.damageSources().generic();
                    if (entity.getPersistentData().contains("xebCDA1SlamAttacker")) {
                        UUID attackerUuid = entity.getPersistentData().getUUID("xebCDA1SlamAttacker");
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
                entity.getPersistentData().remove("xebCDA1SlamTimer");
                entity.getPersistentData().remove("xebCDA1SlamAttacker");
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

                    entity.getPersistentData().putString("xebLastAttackWeapon", "doomfist");
                    entity.getPersistentData().putString("xebLastAttackType", "right_click");
                    entity.getPersistentData().putLong("xebLastAttackTime", entity.level().getGameTime());
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
                if (m.getBuff() instanceof org.xeb.xeb.buff.impl.combat.ResonantBuff resonantBuff) {
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
                
        double startY = player.getPersistentData().contains("xebSlamStartY") ? player.getPersistentData().getDouble("xebSlamStartY") : pos.y;
        player.getPersistentData().remove("xebSlamStartY");
        double heightFallen = Math.max(0.0D, startY - pos.y);
        
        double baseDamage = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float damage = (float) (baseDamage * 0.6D + heightFallen * 1.5F); // Height-proportional extra damage
        
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 horizLook = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z).normalize();
        
        int hitCount = 0;
        for (LivingEntity target : targets) {
            net.minecraft.world.phys.Vec3 offset = target.position().subtract(pos);
            double dist = offset.horizontalDistance();
            
            if (dist <= 6.0D && Math.abs(offset.y) <= 2.0D) {
                net.minecraft.world.phys.Vec3 horizOffsetDir = new net.minecraft.world.phys.Vec3(offset.x, 0.0D, offset.z).normalize();
                double dot = horizLook.dot(horizOffsetDir);
                
                // Cone check: 60-degree angle (cos(30) ~ 0.866)
                if (dot >= 0.866D) {
                    target.getPersistentData().putString("xebLastAttackWeapon", "doomfist");
                    target.getPersistentData().putString("xebLastAttackType", "active2");
                    target.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
                    target.hurt(player.damageSources().playerAttack(player), damage);
                    
                    // Pull target slightly towards player and pop them up
                    net.minecraft.world.phys.Vec3 pullDir = pos.subtract(target.position()).normalize();
                    target.setDeltaMovement(pullDir.x * 0.3D, 0.4D, pullDir.z * 0.3D);
                    target.hurtMarked = true;
                    
                    hitCount++;
                }
            }
        }
        
        if (hitCount > 0) {
            float absorptionAmount = hitCount * 1.0F; // 0.5 hearts (1.0 absorption point) per mob hit
            player.setAbsorptionAmount(player.getAbsorptionAmount() + absorptionAmount);
            player.getPersistentData().putInt("xebSeismicAbsorptionTimer", 80); // 4 seconds
            player.getPersistentData().putFloat("xebSeismicAbsorptionAmount", absorptionAmount);
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
            net.minecraft.world.phys.Vec3 downEnd = new net.minecraft.world.phys.Vec3(end.x, end.y - 1000.0D, end.z);
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
                e -> e != player && e.isAlive() && !e.isAlliedTo(player) && !(e instanceof org.xeb.xeb.entity.CrazyDiamondEntity));
                
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

    public static void executeCrazyDiamondBarrageHit(net.minecraft.world.entity.player.Player player) {
        net.minecraft.world.level.Level level = player.level();
        net.minecraft.world.phys.HitResult hit = getCDPOVHitResult(player, 6.0D);
        net.minecraft.world.entity.LivingEntity target = null;
        if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
            net.minecraft.world.phys.EntityHitResult entityHit = (net.minecraft.world.phys.EntityHitResult) hit;
            if (entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
                target = living;
            }
        }
        
        triggerStandSwing(player);
        
        if (target == null) return;
        
        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float damage = baseDamage * 0.4F;
        
        java.util.List<net.minecraft.world.entity.LivingEntity> allies = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(10.0D),
                (entity) -> {
                    if (entity == player || !entity.isAlive()) return false;
                    if (entity instanceof net.minecraft.world.entity.player.Player otherPlayer) {
                        return player.isAlliedTo(otherPlayer);
                    }
                    if (entity instanceof net.minecraft.world.entity.OwnableEntity ownable) {
                        return player.getUUID().equals(ownable.getOwnerUUID());
                    }
                    return false;
                });
        
        if (!allies.isEmpty()) {
            allies.sort((a, b) -> Float.compare(a.getHealth() / a.getMaxHealth(), b.getHealth() / b.getMaxHealth()));
            net.minecraft.world.entity.LivingEntity allyToHeal = allies.get(0);
            allyToHeal.heal(damage);
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, 
                        allyToHeal.getX(), allyToHeal.getY(0.5D), allyToHeal.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.05D);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.8F);
        } else {
            target.hurt(player.damageSources().playerAttack(player), damage);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, 
                        target.getX(), target.getY(0.5D), target.getZ(), 3, 0.1D, 0.1D, 0.1D, 0.1D);
            }
        }
        
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    public static void executeCDA1Phase1(net.minecraft.world.entity.player.Player player, int targetId) {
        net.minecraft.world.level.Level level = player.level();
        net.minecraft.world.entity.LivingEntity target = targetId != -1 && level.getEntity(targetId) instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
        if (target != null && target.isAlive()) {
            float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            target.hurt(player.damageSources().playerAttack(player), baseDamage * 0.8F);
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, target.getX(), target.getY(), target.getZ(), 8, 0.3D, 0.1D, 0.3D, 0.1D);
            }
            level.playSound(null, target.getX(), target.getY(), target.getZ(), net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.1F);
        }
        triggerStandSwing(player);
    }
    
    public static void executeCDA1Phase3(net.minecraft.world.entity.player.Player player, int targetId) {
        net.minecraft.world.level.Level level = player.level();
        net.minecraft.world.entity.LivingEntity target = targetId != -1 && level.getEntity(targetId) instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
        if (target != null && target.isAlive()) {
            target.hurt(player.damageSources().playerAttack(player), 4.0F);
            
            // Set wallslam data on target (6.0 damage, 40 ticks slam window)
            target.getPersistentData().putInt("xebCDA1SlamTimer", 40);
            target.getPersistentData().putUUID("xebCDA1SlamAttacker", player.getUUID());
            
            double yawRad = Math.toRadians(player.getYRot());
            target.knockback(2.5D, -Math.sin(yawRad), Math.cos(yawRad));
            target.hurtMarked = true;
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, target.getX(), target.getY(0.5D), target.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.1D);
            }
            level.playSound(null, target.getX(), target.getY(), target.getZ(), net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.0F);
        }
        triggerStandSwing(player);
    }
    
    private static void triggerStandSwing(net.minecraft.world.entity.player.Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            for (net.minecraft.world.entity.Entity e : serverLevel.getAllEntities()) {
                if (e instanceof org.xeb.xeb.entity.CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                    cds.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    break;
                }
            }
        }
    }

    private static net.minecraft.world.phys.HitResult getCDPOVHitResult(net.minecraft.world.entity.player.Player player, double reach) {
        net.minecraft.world.phys.Vec3 start = player.getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = start.add(look.scale(reach));
        
        net.minecraft.world.phys.BlockHitResult blockHit = player.level().clip(new net.minecraft.world.level.ClipContext(
                start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        
        double maxDist = blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? blockHit.getLocation().distanceToSqr(start) : reach * reach;
        
        net.minecraft.world.phys.AABB area = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        net.minecraft.world.phys.EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, start, end, area,
                (entity) -> entity instanceof net.minecraft.world.entity.LivingEntity && entity.isAlive() && entity != player && !(entity instanceof org.xeb.xeb.entity.CrazyDiamondEntity),
                maxDist);
                
        if (entityHit != null) {
            return entityHit;
        }
        return blockHit;
    }
}
