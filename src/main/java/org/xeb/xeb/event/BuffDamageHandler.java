package org.xeb.xeb.event;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.effect.AdrenalineEffect;
import org.xeb.xeb.effect.BlindEffect;
import org.xeb.xeb.effect.FearEffect;
import org.xeb.xeb.effect.MagicWeaknessEffect;
import org.xeb.xeb.effect.ManaLeechEffect;
import org.xeb.xeb.effect.MarkedEffect;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.util.DodgeHelper;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.effect.ModEffects;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BuffDamageHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!org.xeb.xeb.Config.enabled) return;
        LivingEntity target = event.getEntity();
        if (target == null) return;

        if (target instanceof net.minecraft.world.entity.player.Player player && player.getPersistentData().getBoolean("xebHolyShieldActive")) {
            event.setAmount(event.getAmount() * 0.3F);
            Level level = player.level();
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(),
                        15, 0.5D, 0.5D, 0.5D, 0.05D);
                serverLevel.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0D, player.getZ(),
                        10, 0.5D, 0.5D, 0.5D, 0.02D);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.5F);
            }
        }

        // Custom weapon generic left-click tracker (N23)
        if (!target.level().isClientSide()) {
            if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player attackerPlayer) {
                net.minecraft.world.item.ItemStack held = attackerPlayer.getMainHandItem();
                String weaponName = null;
                if (held.is(org.xeb.xeb.item.ModItems.OPTIC_BLAST.get())) {
                    weaponName = "optic_blast";
                } else if (held.is(org.xeb.xeb.item.ModItems.DOOMFIST.get()) || held.is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get())) {
                    weaponName = "doomfist";
                } else if (held.is(org.xeb.xeb.item.ModItems.BROKEN_DIAMOND.get())) {
                    weaponName = "crazy_diamond";
                } else if (held.is(org.xeb.xeb.item.ModItems.GOLDEN_FLOWER.get())) {
                    weaponName = "golden_flower";
                } else if (held.is(org.xeb.xeb.item.ModItems.THE_TEARS.get())) {
                    weaponName = "the_tears";
                } else if (held.getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem) {
                    weaponName = "mecha";
                } else if (held.getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem) {
                    weaponName = "holy";
                }

                if (weaponName != null) {
                    long currentTick = target.level().getGameTime();
                    boolean alreadySet = target.getPersistentData().contains("xebLastAttackTime") &&
                                         target.getPersistentData().getLong("xebLastAttackTime") == currentTick;
                    if (!alreadySet) {
                        target.getPersistentData().putString("xebLastAttackWeapon", weaponName);
                        target.getPersistentData().putString("xebLastAttackType", "left_click");
                        target.getPersistentData().putLong("xebLastAttackTime", currentTick);
                    }
                }
            } else if (event.getSource().getEntity() instanceof org.xeb.xeb.entity.HotPokerEntity) {
                long currentTick = target.level().getGameTime();
                target.getPersistentData().putString("xebLastAttackWeapon", "hot_poker");
                target.getPersistentData().putString("xebLastAttackType", "left_click");
                target.getPersistentData().putLong("xebLastAttackTime", currentTick);
            }
        }

        // Decoherence enchantment check
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player attackerPlayer) {
            net.minecraft.world.item.ItemStack held = attackerPlayer.getMainHandItem();
            if (!held.isEmpty() && org.xeb.xeb.enchantment.ModEnchantments.isSpecialItem(held.getItem())) {
                int decoLvl = held.getEnchantmentLevel(org.xeb.xeb.enchantment.ModEnchantments.DECOHERENCE.get());
                if (decoLvl > 0) {
                    net.minecraft.nbt.CompoundTag aData = attackerPlayer.getPersistentData();
                    if (aData.getInt("xebDecoherencePlayerCooldown") <= 0) {
                        if (attackerPlayer.getRandom().nextFloat() < 0.15F) {
                            target.getPersistentData().putInt("xebDecoherenceActiveTicks", 120); // 6 seconds
                            aData.putInt("xebDecoherencePlayerCooldown", 3600); // 3 minutes
                            
                            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                    net.minecraft.sounds.SoundEvents.CONDUIT_DEACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                            
                            if (target.level() instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, target.getX(), target.getY() + 1.0D, target.getZ(),
                                        5, 0.3D, 0.3D, 0.3D, 0.05D);
                            }
                        }
                    }
                }
            }
        }

        // --- Ultra Charged left-click attack damage adjustment ---
        // Sets the weapon effective base to 14: scales current damage proportionally
        // so BetterCombat swing multipliers and combos still apply on top.
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player attackerPlayer && !attackerPlayer.level().isClientSide()) {
            org.xeb.xeb.item.QuantumCatBarrageItem.recordDamageDealt(attackerPlayer, event.getAmount());
            if (attackerPlayer.getMainHandItem().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get())) {
                net.minecraft.nbt.CompoundTag tag = attackerPlayer.getPersistentData();
                if (tag.getBoolean("xebUltraCharged")) {
                    // Get normal weapon base damage (7.0) and scale to 14 base
                    float normalBase = 7.0F;
                    float scaledDamage = event.getAmount() * (14.0F / normalBase);
                    event.setAmount(scaledDamage);
                    tag.remove("xebUltraCharged");
                    
                    // Sync consumed state to clients
                    org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> attackerPlayer),
                            new org.xeb.xeb.network.DoomfistUltraChargeSyncPacket(attackerPlayer.getId(), false)
                    );
                    
                    // Play thunder sound on impact
                    attackerPlayer.level().playSound(null, attackerPlayer.getX(), attackerPlayer.getY(), attackerPlayer.getZ(),
                            net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
                }
            }
        }

        // --- Power Block frontal damage mitigation ---
        if (target instanceof net.minecraft.world.entity.player.Player player && !player.level().isClientSide()) {
            net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
            if (tag.getBoolean("xebPowerBlocking")) {
                net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
                net.minecraft.world.phys.Vec3 sourcePos = event.getSource().getSourcePosition();
                if (sourcePos == null && attacker != null) {
                    sourcePos = attacker.position();
                }
                
                if (sourcePos != null) {
                    net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                    net.minecraft.world.phys.Vec3 toSource = sourcePos.subtract(player.position()).normalize();
                    net.minecraft.world.phys.Vec3 horizLook = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z).normalize();
                    net.minecraft.world.phys.Vec3 horizToSource = new net.minecraft.world.phys.Vec3(toSource.x, 0.0D, toSource.z).normalize();
                    
                    double dot = horizLook.dot(horizToSource);
                    if (dot > 0.0D) { // Frontal damage
                        float originalDamage = event.getAmount();
                        
                        // Accumulate blocked damage
                        float accum = tag.getFloat("xebBlockDamageAccumulated") + originalDamage;
                        tag.putFloat("xebBlockDamageAccumulated", accum);
                        
                        // Trigger Ultra Charge if accumulated blocked damage > 20% max health
                        if (accum >= player.getMaxHealth() * 0.20F && !tag.getBoolean("xebUltraCharged")) {
                            tag.putBoolean("xebUltraCharged", true);
                            
                            org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                                    net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                    new org.xeb.xeb.network.DoomfistUltraChargeSyncPacket(player.getId(), true)
                            );

                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.0F);
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.5F);
                                    
                            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, player.getX(), player.getY(0.5D), player.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.1D);
                                net.minecraft.core.particles.DustParticleOptions redDust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.0F, 0.0F), 1.5F);
                                serverLevel.sendParticles(redDust, player.getX(), player.getY(0.5D), player.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.1D);
                            }
                        }

                        event.setAmount(originalDamage * 0.25F);
                        
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F + player.getRandom().nextFloat() * 0.4F);
                        
                        // Sync HUD flash to client
                        org.xeb.xeb.network.XEBNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) player),
                                new org.xeb.xeb.network.DoomfistAbilitySyncPacket(player.getId(), 0, 0, 0.0D, 0.0D, 0.0D, 0, 0, true));
                    }
                }
            }
        }

        // ── Delayed Pain (target side) — accumulate damage to be dealt when effect expires ──
        if (!target.level().isClientSide() && target.hasEffect(ModEffects.DELAYED_PAIN.get())) {
            if (!target.getPersistentData().getBoolean("xebDelayedPainTriggering")) {
                double current = target.getPersistentData().getDouble("xebDelayedPainAccumulated");
                target.getPersistentData().putDouble("xebDelayedPainAccumulated", current + event.getAmount());
                
                Entity attacker = event.getSource().getEntity();
                if (attacker != null) {
                    target.getPersistentData().putUUID("xebDelayedPainAttacker", attacker.getUUID());
                } else {
                    target.getPersistentData().remove("xebDelayedPainAttacker");
                }
                
                event.setAmount(0.0F);
                event.setCanceled(true);
                return;
            }
        }

        // ── Bruise (target side) — extra damage equal to a percentage of max HP ──
        if (!target.level().isClientSide() && target.hasEffect(ModEffects.BRUISE.get())) {
            MobEffectInstance bruise = target.getEffect(ModEffects.BRUISE.get());
            if (bruise != null) {
                int amp = bruise.getAmplifier();
                float percent = 0.05F + 0.05F * amp;
                float extraDamage = target.getMaxHealth() * percent;
                event.setAmount(event.getAmount() + extraDamage);
            }
        }

        // ── Bounty (target side) — takes 20% more damage ──
        if (!target.level().isClientSide() && target.hasEffect(ModEffects.BOUNTY.get())) {
            event.setAmount(event.getAmount() * 1.20F);
        }

        Entity attacker = event.getSource().getEntity();

        // ── Brass Knuckles (attacker side) ──
        if (!target.level().isClientSide() && attacker instanceof LivingEntity livingAttacker) {
            if (event.getSource().getDirectEntity() == attacker
                    && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                    && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
                if (org.xeb.xeb.compat.ModCompatManager.hasCurioOrOffhand(livingAttacker, org.xeb.xeb.item.ModItems.BRASS_KNUCKLES.get())) {
                    long currentTime = livingAttacker.level().getGameTime();
                    long lastBruiseTime = livingAttacker.getPersistentData().getLong("xebLastBrassKnucklesBruiseTime");
                    if (lastBruiseTime == 0 || currentTime - lastBruiseTime >= 400) {
                        if (livingAttacker.getRandom().nextFloat() < 0.20F) {
                            MobEffectInstance existing = target.getEffect(ModEffects.BRUISE.get());
                            int amp = 0;
                            if (existing != null) {
                                amp = Math.min(2, existing.getAmplifier() + 1);
                            }
                            target.addEffect(new MobEffectInstance(ModEffects.BRUISE.get(), 40, amp));
                            livingAttacker.getPersistentData().putLong("xebLastBrassKnucklesBruiseTime", currentTime);
                        }
                    }
                }
            }
        }

        // ── Kinetic Spikes ───────────────────────────────────────────────────────
        if (!target.level().isClientSide() && target.hasEffect(ModEffects.KINETIC_SPIKES.get())) {
            MobEffectInstance effect = target.getEffect(ModEffects.KINETIC_SPIKES.get());
            if (effect != null) {
                int amplifier = effect.getAmplifier();
                double baseDamage = 1.0D;
                if (target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null) {
                    baseDamage = target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                }
                double projectileDamage = baseDamage * (0.10D + 0.15D * amplifier);
                int count = 2 + target.getRandom().nextInt(2);
                LivingEntity projectileTarget = findNearestEnemy(target, 16.0D);
                for (int i = 0; i < count; i++) {
                    org.xeb.xeb.entity.SparkleEntity sparkle = new org.xeb.xeb.entity.SparkleEntity(
                            target.level(), target, projectileDamage, projectileTarget);
                    sparkle.moveTo(target.getX(), target.getY(0.5D), target.getZ(), 0.0F, 0.0F);
                    target.level().addFreshEntity(sparkle);
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_STEP,
                            net.minecraft.sounds.SoundSource.NEUTRAL,
                            1.0F, 0.5F + target.getRandom().nextFloat() * 1.0F);
                }
            }
        }

        // ── Player Holy Shield ───────────────────────────────────────────────────
        if (target instanceof net.minecraft.world.entity.player.Player player && !player.level().isClientSide()) {
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                    || event.getAmount() >= 1000.0F
                    || player.getPersistentData().getBoolean("xebDelayedPainTriggering")) {
                return;
            }
            if (player.hasEffect(ModEffects.HOLY_SHIELD.get()) && !player.getPersistentData().contains("xebPlayerHolyShieldTimer")) {
                net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
                int cooldown = org.xeb.xeb.compat.ModCompatManager.hasCurioOrOffhand(player, org.xeb.xeb.item.ModItems.HOLY_MANTLE.get()) ? 2400 : 800;
                tag.putInt("xebPlayerHolyShieldTimer", cooldown);
                
                // Remove effect immediately so it doesn't try to block again during cooldown
                player.removeEffect(ModEffects.HOLY_SHIELD.get());
                
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                event.setAmount(0.0F);
                event.setCanceled(true);
                BuffParticlePacket packet = new BuffParticlePacket(player.getX(), player.getY(), player.getZ(), "revival", 15);
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
                return;
            }
        }

        // ── Blind (attacker side) — chance to miss entirely ─────────────────────
        if (attacker instanceof LivingEntity livingAttacker && !livingAttacker.level().isClientSide()) {
            // ── Burn (attacker side) — reduces physical melee damage ──
            if (livingAttacker.hasEffect(ModEffects.BURN.get())) {
                MobEffectInstance burnEffect = livingAttacker.getEffect(ModEffects.BURN.get());
                if (burnEffect != null) {
                    boolean isMelee = event.getSource().getDirectEntity() == livingAttacker
                            && !isMagicDamage(event.getSource())
                            && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ARROW)
                            && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_PROJECTILE);
                    if (isMelee) {
                        int amp = burnEffect.getAmplifier();
                        float reduction = 0.10F + 0.05F * amp;
                        event.setAmount(event.getAmount() * (1.0F - reduction));
                    }
                }
            }

            if (livingAttacker.hasEffect(ModEffects.BLIND.get())) {
                MobEffectInstance blindEffect = livingAttacker.getEffect(ModEffects.BLIND.get());
                float missChance = BlindEffect.getMissChance(blindEffect != null ? blindEffect.getAmplifier() : 0);
                if (livingAttacker.getRandom().nextFloat() < missChance) {
                    event.setAmount(0.0F);
                    event.setCanceled(true);
                    // Suppress the hurt animation (red flash) on the target
                    target.hurtTime = 0;
                    target.invulnerableTime = 0;
                    // Dark smoke on the target to signal the miss
                    BuffParticlePacket packet = new BuffParticlePacket(
                            target.getX(), target.getY(), target.getZ(), "blind", 5);
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
                    return;
                }
            }
        }

        // ── Marked (target side) — x2 damage + armour penetration ──────────────
        if (target.hasEffect(ModEffects.MARKED.get()) && !target.level().isClientSide()) {
            float originalAmount = event.getAmount();
            // Simulate armour penetration by boosting pre-mitigation damage.
            // We approximate 50% armour pen + 30% toughness pen by scaling damage
            // up so that after vanilla reduction the net effect ~= double with pen.
            float armor = (float) target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            float toughness = (float) target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
            // Effective armour/toughness after penetration
            float effectiveArmor    = armor    * (1.0F - MarkedEffect.ARMOR_PENETRATION);
            float effectiveToughness = toughness * (1.0F - MarkedEffect.TOUGHNESS_PENETRATION);
            // Damage after vanilla formula with full armour
            float reducedNormal = getDamageAfterArmor(originalAmount, armor, toughness);
            // Damage after vanilla formula with reduced armour
            float reducedPen    = getDamageAfterArmor(originalAmount, effectiveArmor, effectiveToughness);
            // Apply ×2 and compensate for the armour difference
            float newAmount = (reducedPen * MarkedEffect.DAMAGE_MULTIPLIER)
                    + Math.max(0, reducedPen - reducedNormal); // bonus for pen
            // Ensure at least the raw ×2 if pen calculation would give less
            newAmount = Math.max(originalAmount * MarkedEffect.DAMAGE_MULTIPLIER, newAmount);
            event.setAmount(newAmount);

            // Marked suppresses any dodge the attacker might have (Adrenaline, Lucky)
            // We tag the event so downstream handlers know to skip dodge rolls
            target.getPersistentData().putBoolean("xebMarkedHitIncoming", true);

            BuffParticlePacket pkt = new BuffParticlePacket(
                    target.getX(), target.getY(), target.getZ(), "marked", 6);
            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), pkt);
        }

        // ── Magic Weakness (target side) — amplify magic damage ─────────────────
        if (target.hasEffect(ModEffects.MAGIC_WEAKNESS.get()) && !target.level().isClientSide()) {
            if (isMagicDamage(event.getSource())) {
                MobEffectInstance mwEffect = target.getEffect(ModEffects.MAGIC_WEAKNESS.get());
                float mult = MagicWeaknessEffect.getDamageMultiplier(mwEffect != null ? mwEffect.getAmplifier() : 0);
                event.setAmount(event.getAmount() * mult);
            }
        }



        // ── Medallion buffs (target) ──────────────────────────────────────────────
        List<MedallionData> targetMedallions = MedallionManager.getActiveMedallions(target);
        if (!targetMedallions.isEmpty()) {
            for (MedallionData m : targetMedallions) {
                if (event.isCanceled()) break;
                m.getBuff().onDamageTaken(target, event);
            }
        }

        // Clear the marked hit flag after all handlers have run
        target.getPersistentData().remove("xebMarkedHitIncoming");

        // ── Medallion buffs (attacker) ────────────────────────────────────────────
        if (attacker instanceof LivingEntity livingAttacker) {
            if (livingAttacker instanceof net.minecraft.world.entity.Mob) {
                try {
                    org.xeb.xeb.boss.FrozenBossRecoverySystem.registerDamageDealt(livingAttacker);
                } catch (Throwable ignored) {}
            }

            List<MedallionData> attackerMedallions = MedallionManager.getActiveMedallions(livingAttacker);
            if (!attackerMedallions.isEmpty()) {
                for (MedallionData m : attackerMedallions) {
                    if (event.isCanceled()) break;
                    m.getBuff().onDamageDealt(livingAttacker, event);
                }
            }

            // ── Adrenaline (attacker) — crit + bonus damage ──────────────────────
            if (livingAttacker.hasEffect(ModEffects.ADRENALINE.get()) && !event.isCanceled() && !livingAttacker.level().isClientSide()) {
                float amount = event.getAmount();
                // ×2 base damage bonus
                amount *= AdrenalineEffect.DAMAGE_BONUS;
                // 75% crit: double damage again
                if (livingAttacker.getRandom().nextFloat() < AdrenalineEffect.CRIT_CHANCE) {
                    amount *= 2.0F;
                    BuffParticlePacket pkt = new BuffParticlePacket(
                            target.getX(), target.getY(), target.getZ(), "crit", 8);
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), pkt);
                }
                event.setAmount(amount);
            }

            // ── Charged Fist (attacker) — fire Sparkle projectiles on hit ────────
            // Same scaling as Kinetic Spikes; triggered on the ATTACKER side.
            if (!event.isCanceled() && !livingAttacker.level().isClientSide()
                    && livingAttacker.hasEffect(ModEffects.CHARGED_FIST.get())
                    && !(event.getSource().getDirectEntity() instanceof org.xeb.xeb.entity.SparkleEntity)) {
                net.minecraft.world.effect.MobEffectInstance cfEffect =
                        livingAttacker.getEffect(ModEffects.CHARGED_FIST.get());
                if (cfEffect != null) {
                    long currentTick = livingAttacker.level().getGameTime();
                    long lastTrigger = livingAttacker.getPersistentData().getLong("xebChargedFistLastTrigger");
                    int amplifier = cfEffect.getAmplifier();
                    int cooldown = Math.max(2, 16 - 4 * amplifier);
                    if (currentTick - lastTrigger >= cooldown) {
                        livingAttacker.getPersistentData().putLong("xebChargedFistLastTrigger", currentTick);

                        double baseDamage = 1.0D;
                        if (livingAttacker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null) {
                            baseDamage = livingAttacker.getAttributeValue(
                                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        }
                        double projectileDamage = baseDamage * (0.10D + 0.15D * amplifier);
                        int count = 2 + livingAttacker.getRandom().nextInt(2); // 2 or 3
                        LivingEntity sparkTarget = (target.isAlive()) ? target : findNearestEnemy(livingAttacker, 16.0D);
                        net.minecraft.world.phys.Vec3 eyePos = livingAttacker.getEyePosition(1.0F);

                        for (int i = 0; i < count; i++) {
                            org.xeb.xeb.entity.SparkleEntity sparkle = new org.xeb.xeb.entity.SparkleEntity(
                                    livingAttacker.level(), livingAttacker, projectileDamage, sparkTarget);
                            sparkle.moveTo(eyePos.x, eyePos.y - 0.1D, eyePos.z, 0.0F, 0.0F);
                            livingAttacker.level().addFreshEntity(sparkle);
                        }
                        livingAttacker.level().playSound(null,
                                livingAttacker.getX(), livingAttacker.getY(), livingAttacker.getZ(),
                                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_STEP,
                                net.minecraft.sounds.SoundSource.NEUTRAL,
                                0.8F, 1.2F + livingAttacker.getRandom().nextFloat() * 0.6F);
                    }
                }
            }
        }

        // ── Adrenaline (target) — 40% dodge (suppressed by Marked) ──────────────
        if (target.hasEffect(ModEffects.ADRENALINE.get()) && !event.isCanceled() && !target.level().isClientSide()) {
            boolean markedSuppressed = target.getPersistentData().getBoolean("xebMarkedHitIncoming");
            if (!markedSuppressed && target.getRandom().nextFloat() < AdrenalineEffect.DODGE_CHANCE) {
                DodgeHelper.triggerDodge(target, event);
            }
        }

        // ── Fear (target side) — update flee-source tags on each hit ─────────────
        // We update even if the event is cancelled so the position is always fresh.
        if (!target.level().isClientSide() && target.hasEffect(ModEffects.FEAR.get())) {
            LivingEntity fearAttacker = (attacker instanceof LivingEntity la) ? la : target.getLastHurtByMob();
            FearEffect.recordDamageSource(target, fearAttacker);
        }
    }

    // ── NHR: cancel ALL healing ───────────────────────────────────────────────
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!org.xeb.xeb.Config.enabled) return;
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide()) return;

        // No Health Regen
        if (entity.hasEffect(ModEffects.NO_HEALTH_REGEN.get())) {
            event.setCanceled(true);
            return;
        }
        // Exhausted also blocks passive regen
        if (entity.hasEffect(ModEffects.EXHAUSTED.get())) {
            event.setCanceled(true);
        }
    }

    // ── MobEffectEvent.Added — track applicators and link effects ─────────────
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!org.xeb.xeb.Config.enabled) return;
        LivingEntity target = event.getEntity();
        if (target == null || target.level().isClientSide()) return;

        MobEffectInstance added = event.getEffectInstance();
        if (added == null) return;

        // ── Mana Leeches: store applicator entity ID ─────────────────────────
        if (added.getEffect() == ModEffects.MANA_LEECH.get()) {
            // The applicator is the last entity that hurt the target, or we can check
            // via the effect's source. In Forge 1.20 MobEffectEvent.Added doesn't expose
            // the source entity directly, so we fall back to lastHurtByMob.
            LivingEntity applicator = target.getLastHurtByMob();
            if (applicator != null) {
                target.getPersistentData().putInt(ManaLeechEffect.APPLICATOR_KEY, applicator.getId());
            }
        }

        // ── Fear: store fear-source entity ID and last-hurt position ──────────
        if (added.getEffect() == ModEffects.FEAR.get()) {
            LivingEntity source = target.getLastHurtByMob();
            FearEffect.recordDamageSource(target, source);
        }

        // NOTE: Exhausted ↔ Adrenaline mutual linking is handled in BuffTickHandler
        // via polling, not here. Calling addEffect() inside MobEffectEvent.Added
        // can cause the secondary effect to be silently ignored or create event loops.
    }

    // ── MobEffectEvent.Expired — remove linked effects ────────────────────────
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (!org.xeb.xeb.Config.enabled) return;
        LivingEntity target = event.getEntity();
        if (target == null || target.level().isClientSide()) return;

        MobEffectInstance expired = event.getEffectInstance();
        if (expired == null) return;

        // Exhausted expires → remove Adrenaline
        if (expired.getEffect() == ModEffects.EXHAUSTED.get()) {
            target.removeEffect(ModEffects.ADRENALINE.get());
            // Clean up fear goal tag (in case Exhausted/Fear were combined)
        }

        // Fear expires → clean up all flee tags and goal guard
        if (expired.getEffect() == ModEffects.FEAR.get()) {
            FearEffect.cleanupTags(target);
            if (target instanceof net.minecraft.world.entity.Mob mob) {
                FearEffect.FearAIManager.removeFleeGoal(mob);
            }
        }

        // Mana Leeches expires → clean up applicator tag
        if (expired.getEffect() == ModEffects.MANA_LEECH.get()) {
            target.getPersistentData().remove(ManaLeechEffect.APPLICATOR_KEY);
        }
    }

    // ── Projectile impacts — route to medallion buffs ────────────────────────
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        HitResult hit = event.getRayTraceResult();
        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            if (entityHit.getEntity() instanceof LivingEntity target) {
                List<MedallionData> medallions = MedallionManager.getActiveMedallions(target);
                if (!medallions.isEmpty()) {
                    for (MedallionData m : medallions) {
                        m.getBuff().onProjectileImpact(target, event);
                    }
                }

                // ── Reflect Potion Effect ──
                if (!event.isCanceled() && target.hasEffect(ModEffects.REFLECT.get()) && !target.level().isClientSide()) {
                    MobEffectInstance reflectEffect = target.getEffect(ModEffects.REFLECT.get());
                    if (reflectEffect != null) {
                        int amp = reflectEffect.getAmplifier();
                        float chance = 0.20F + 0.10F * amp;
                        if (target.getRandom().nextFloat() < chance) {
                            net.minecraft.world.entity.projectile.Projectile proj = event.getProjectile();
                            net.minecraft.world.phys.Vec3 vel = proj.getDeltaMovement();
                            proj.setDeltaMovement(vel.scale(-1.2D));
                            proj.hurtMarked = true;
                            proj.setOwner(target);
                            if (proj instanceof org.xeb.xeb.entity.SparkleEntity sparkle) {
                                sparkle.onReflected(target);
                            }
                            event.setCanceled(true);
                        }
                    }
                }

                // Adrenaline projectile dodge (40%, suppressed by Marked)
                if (!event.isCanceled() && target.hasEffect(ModEffects.ADRENALINE.get()) && !target.level().isClientSide()) {
                    boolean markedSuppressed = target.getPersistentData().getBoolean("xebMarkedHitIncoming");
                    if (!markedSuppressed && target.getRandom().nextFloat() < AdrenalineEffect.DODGE_CHANCE) {
                        DodgeHelper.triggerDodge(target, event);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity != null && MedallionManager.hasBuff(entity, "hardy")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide() || !(entity instanceof net.minecraft.world.entity.Mob mob)) {
            return;
        }

        if (mob.hasEffect(ModEffects.MADNESS.get())) {
            if (org.xeb.xeb.boss.UniversalBossDetector.isBlacklisted(mob)) {
                return;
            }

            boolean isBoss = org.xeb.xeb.compat.ModCompatManager.isBoss(mob);
            LivingEntity newTargetChoice = event.getNewTarget();

            if (isBoss && newTargetChoice != null) {
                boolean isValidTarget = newTargetChoice instanceof net.minecraft.world.entity.player.Player ||
                                       org.xeb.xeb.compat.ModCompatManager.isBoss(newTargetChoice) ||
                                       !org.xeb.xeb.medallion.MedallionManager.getMedallions(newTargetChoice).isEmpty();
                if (isValidTarget && !org.xeb.xeb.boss.TargetRejectionBuffer.isRejected(mob, newTargetChoice.getId())) {
                    mob.getPersistentData().putInt("xebMadnessTargetId", newTargetChoice.getId());
                    return;
                }
            }

            MobEffectInstance effect = mob.getEffect(ModEffects.MADNESS.get());
            int amplifier = effect != null ? effect.getAmplifier() : 0;
            double range = 16.0D + amplifier * 4.0D;

            net.minecraft.nbt.CompoundTag tag = mob.getPersistentData();
            LivingEntity target = null;

            if (tag.contains("xebMadnessTargetId")) {
                int targetId = tag.getInt("xebMadnessTargetId");
                if (org.xeb.xeb.boss.TargetRejectionBuffer.isRejected(mob, targetId)) {
                    tag.remove("xebMadnessTargetId");
                } else {
                    Entity cached = mob.level().getEntity(targetId);
                    if (cached instanceof LivingEntity living && living.isAlive() && mob.distanceToSqr(living) <= range * range) {
                        boolean isValid = !(living instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator()))
                                && !org.xeb.xeb.boss.UniversalBossDetector.isBlacklisted(living);
                        if (isValid && isBoss && !org.xeb.xeb.boss.BossTargetCandidateExpander.shouldAttackAllMobs(mob)) {
                            isValid = living instanceof net.minecraft.world.entity.player.Player ||
                                      org.xeb.xeb.compat.ModCompatManager.isBoss(living) ||
                                      !org.xeb.xeb.medallion.MedallionManager.getMedallions(living).isEmpty();
                        }
                        if (isValid) {
                            target = living;
                        }
                    }
                }
            }

            if (target == null) {
                target = findNewMadnessTarget(mob, range);
                if (target != null) {
                    tag.putInt("xebMadnessTargetId", target.getId());
                } else {
                    tag.remove("xebMadnessTargetId");
                }
            }

            if (target != null && event.getNewTarget() != target) {
                event.setNewTarget(target);
                mob.setLastHurtByMob(target);
                mob.setLastHurtMob(target);
            }
        }
    }

    private static LivingEntity findNewMadnessTarget(net.minecraft.world.entity.Mob mob, double range) {
        boolean isBoss = org.xeb.xeb.compat.ModCompatManager.isBoss(mob);
        net.minecraft.world.phys.AABB searchBox = mob.getBoundingBox().inflate(range);
        List<LivingEntity> potentialTargets = mob.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> {
                    if (target == mob || !target.isAlive() || !mob.hasLineOfSight(target)) return false;
                    if (target instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) return false;
                    if (org.xeb.xeb.boss.UniversalBossDetector.isBlacklisted(target)) return false;
                    if (org.xeb.xeb.boss.TargetRejectionBuffer.isRejected(mob, target.getId())) return false;
                    if (isBoss) {
                        if (org.xeb.xeb.boss.BossTargetCandidateExpander.shouldAttackAllMobs(mob)) {
                            return true;
                        }
                        return target instanceof net.minecraft.world.entity.player.Player ||
                               org.xeb.xeb.compat.ModCompatManager.isBoss(target) ||
                               !org.xeb.xeb.medallion.MedallionManager.getMedallions(target).isEmpty();
                    }
                    return true;
                });
        if (!potentialTargets.isEmpty()) {
            return potentialTargets.get(mob.getRandom().nextInt(potentialTargets.size()));
        }
        return null;
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player != null) {
            if (player.hasEffect(ModEffects.ALL_STATS_DOWN.get())) {
                int amp = player.getEffect(ModEffects.ALL_STATS_DOWN.get()).getAmplifier();
                float reduction = 0.10F * (amp + 1);
                event.setNewSpeed(event.getNewSpeed() * (1.0F - reduction));
            }
            if (player.hasEffect(ModEffects.ALL_STATS_UP.get())) {
                int amp = player.getEffect(ModEffects.ALL_STATS_UP.get()).getAmplifier();
                float boost = 0.10F * (amp + 1);
                event.setNewSpeed(event.getNewSpeed() * (1.0F + boost));
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** True if the damage source is considered magic (indirect/spell/thorns/etc.) */
    private static boolean isMagicDamage(DamageSource source) {
        return source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)
                || source.getMsgId().equals("indirectMagic")
                || source.getMsgId().contains("magic")
                || source.getMsgId().contains("spell")
                || source.getMsgId().contains("projectile"); // Iron's Spells projectiles
    }

    /**
     * Approximate vanilla armour-damage reduction formula.
     * damage_after = damage * max(0.2, 1 - (armor / 5) / (2 + armor / 4) - toughness_factor)
     */
    private static float getDamageAfterArmor(float damage, float armor, float toughness) {
        float armorFactor = (float) Math.max(0.2, 1.0 - armor / 25.0 - toughness / 80.0);
        return damage * armorFactor;
    }

    public static LivingEntity findNearestEnemy(LivingEntity owner, double range) {
        net.minecraft.world.phys.AABB box = owner.getBoundingBox().inflate(range);
        List<LivingEntity> list = owner.level().getEntitiesOfClass(LivingEntity.class, box,
            entity -> {
                if (entity == owner || !entity.isAlive()) return false;
                if (entity instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) return false;
                if (owner.isAlliedTo(entity)) return false;
                if (entity instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isOwnedBy(owner)) return false;
                return true;
            });

        LivingEntity nearest = null;
        double minDist = Double.MAX_VALUE;

        if (owner instanceof net.minecraft.world.entity.player.Player) {
            for (LivingEntity e : list) {
                boolean isHostile = e instanceof net.minecraft.world.entity.monster.Enemy ||
                                    e instanceof net.minecraft.world.entity.player.Player ||
                                    org.xeb.xeb.compat.ModCompatManager.isBoss(e);
                if (isHostile) {
                    double dist = owner.distanceToSqr(e);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = e;
                    }
                }
            }
        }

        if (nearest == null) {
            minDist = Double.MAX_VALUE;
            for (LivingEntity e : list) {
                double dist = owner.distanceToSqr(e);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = e;
                }
            }
        }

        return nearest;
    }

    // ── Charged Fist (air swing) — fires when player swings and hits nothing ──
    // PlayerInteractEvent.LeftClickEmpty fires whenever the player left-clicks with
    // no entity or block in reach, i.e. a pure "air swing" of any weapon/hand.
    // We fire the same Sparkle burst, auto-targeting the nearest enemy.
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!org.xeb.xeb.Config.enabled) return;
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player == null || !player.level().isClientSide()) return;
        if (!player.hasEffect(ModEffects.CHARGED_FIST.get())) return;

        org.xeb.xeb.network.XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.AirSwingPacket());
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!org.xeb.xeb.Config.enabled) return;
        LivingEntity entity = event.getEntity();
        if (entity == null) return;
        if (org.xeb.xeb.compat.ModCompatManager.hasHelmetOrCurio(entity, org.xeb.xeb.item.ModItems.TINFOIL_HAT.get())) {
            net.minecraft.world.effect.MobEffect effect = event.getEffectInstance().getEffect();
            if (effect == ModEffects.MADNESS.get() || 
                effect == ModEffects.FEAR.get() || 
                effect == ModEffects.PETRIFY.get()) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
            boolean holdsMecha = player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem
                    || player.getOffhandItem().getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem;
            boolean isUsingGauntlet = player.isUsingItem() && (player.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST.get()) || player.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get()));
            double momentum = tag.getDouble("xebMechaMomentum");
            if (tag.getBoolean("xebDoomfistFallProtect") || isUsingGauntlet || tag.getBoolean("xebHolyAnnihilationActive") || (holdsMecha && momentum >= 0.5D)) {
                event.setCanceled(true);
                event.setDamageMultiplier(0.0F);
                // Immediately consume protection tags
                tag.remove("xebDoomfistFallProtect");
                tag.remove("xebFallProtectGroundTicks");
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide()) return;
        
        net.minecraft.nbt.CompoundTag tag = entity.getPersistentData();
        if (tag.contains("xebCharredBurnTicks")) {
            int ticks = tag.getInt("xebCharredBurnTicks");
            if (ticks > 0) {
                tag.putInt("xebCharredBurnTicks", ticks - 1);
                
                int stack = tag.getInt("xebCharredBurnStack");
                if (stack <= 0) stack = 1;
                
                if (entity.level() instanceof ServerLevel sl && entity.level().getGameTime() % 2 == 0) {
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + entity.getBbHeight() / 2.0D, entity.getZ(),
                            2 * stack, 0.3D, 0.3D, 0.3D, 0.01D);
                }
                
                if (ticks % 20 == 0) {
                    float dmg = 2.0F * stack;
                    LivingEntity attacker = null;
                    if (tag.hasUUID("xebCharredBurnAttacker")) {
                        Entity found = ((ServerLevel)entity.level()).getEntity(tag.getUUID("xebCharredBurnAttacker"));
                        if (found instanceof LivingEntity) {
                            attacker = (LivingEntity) found;
                        }
                    }
                    DamageSource source = attacker != null 
                            ? entity.level().damageSources().magic() 
                            : entity.level().damageSources().magic();
                    entity.hurt(source, dmg);
                }
            } else {
                tag.remove("xebCharredBurnTicks");
                tag.remove("xebCharredBurnStack");
                tag.remove("xebCharredBurnAttacker");
            }
        }
    }
}

