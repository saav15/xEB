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
import org.xeb.xeb.util.BuffParticleHelper;
import org.xeb.xeb.util.DodgeHelper;
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
                BuffParticleHelper.sendParticles(player, "revival", 15);
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
                    BuffParticleHelper.sendParticles(target, "blind", 5);
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

            BuffParticleHelper.sendParticles(target, "marked", 6);
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
        List<MedallionData> targetMedallions = MedallionManager.getMedallions(target);
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
                org.xeb.xeb.boss.FrozenBossRecoverySystem.registerDamageDealt(livingAttacker);
            }

            List<MedallionData> attackerMedallions = MedallionManager.getMedallions(livingAttacker);
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
                    BuffParticleHelper.sendParticles(target, "crit", 8);
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
                List<MedallionData> medallions = MedallionManager.getMedallions(target);
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
            boolean isUsingGauntlet = player.isUsingItem() && player.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST.get());
            if (tag.getBoolean("xebDoomfistFallProtect") || isUsingGauntlet) {
                event.setCanceled(true);
                event.setDamageMultiplier(0.0F);
            }
        }
    }
}

