package org.xeb.xeb.effect;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Miedo (Fear) — Unique level.
 *
 * Affected entities flee from:
 *  1. The entity that last dealt damage to them (if still alive), OR
 *  2. The position where they last received damage (if the attacker is gone/dead).
 *
 * Affects BOTH mobs and players:
 *  - Mobs:    a high-priority flee Goal is injected. The goal steers the mob smoothly
 *             away every tick using interpolated navigation.
 *  - Players: a tiny velocity nudge is applied EVERY server tick (via BuffTickHandler),
 *             resulting in a smooth, continuous push — not a jarring impulse.
 *
 * Source tracking (PersistentData):
 *  - "xebFearSourceId"      — runtime entity ID of the attacker (int, 0 = none)
 *  - "xebFearLastHurtX/Y/Z" — world coordinates of the last damage event (double)
 *
 * These tags are written / updated in BuffDamageHandler whenever a feared entity
 * receives damage, and cleaned up when the effect expires.
 */
public class FearEffect extends MobEffect {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** PersistentData key: runtime entity ID of the fear source (int). */
    public static final String FEAR_SOURCE_KEY = "xebFearSourceId";
    /** PersistentData keys: world position of the last received hit. */
    public static final String FEAR_LAST_X     = "xebFearLastHurtX";
    public static final String FEAR_LAST_Y     = "xebFearLastHurtY";
    public static final String FEAR_LAST_Z     = "xebFearLastHurtZ";
    /** Guard tag so the flee Goal is only injected once per fear application. */
    public static final String FEAR_GOAL_TAG   = "xebFearGoalActive";

    // ── Smooth player-push constants ──────────────────────────────────────────
    /**
     * Force applied per tick (every tick, not every N ticks).
     * Small value = smooth constant push, no jolts.
     * At 0 blocks distance: ~0.095 blocks/tick² → noticeable but not violent.
     * At 20 blocks distance: force → 0 (player is far enough, no more push).
     */
    private static final double PUSH_BASE      = 0.095;
    private static final double PUSH_FALLOFF   = 0.00024; // reduces with distSq
    /** Maximum horizontal speed the fear push can contribute (caps accumulation). */
    private static final double PUSH_MAX_SPEED = 0.55;

    public FearEffect() {
        super(MobEffectCategory.HARMFUL, 0x4B5320);
    }

    // -------------------------------------------------------------------------
    // isDurationEffectTick — only used for mob Goal injection, not player push
    // -------------------------------------------------------------------------

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Used only to periodically call applyEffectTick for mob AI injection.
        // Player smooth-push is handled directly in BuffTickHandler every tick.
        return duration % 10 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;
        // For mobs only — inject the flee goal if not already done.
        if (entity instanceof Mob mob) {
            FearAIManager.ensureFleeGoal(mob);
        }
        // Players are handled every tick in BuffTickHandler.applyPlayerFearTick()
    }

    // =========================================================================
    // Player smooth-push — called from BuffTickHandler every server tick
    // =========================================================================

    /**
     * Apply a tiny, continuous velocity nudge away from the flee origin.
     * Because this runs EVERY tick the force is tiny, producing a smooth
     * acceleration instead of jarring pulses.
     *
     * The horizontal velocity contribution from fear is capped at PUSH_MAX_SPEED
     * so it never feels like the player is being launched.
     */
    public static void applyPlayerFearTick(net.minecraft.world.entity.player.Player player) {
        Vec3 fleeFrom = resolveFleeOrigin(player);
        if (fleeFrom == null) return;

        Vec3 playerPos = player.position();
        Vec3 towardSource = fleeFrom.subtract(playerPos);
        double distSq = towardSource.lengthSqr();
        if (distSq < 0.01) return; // standing on top of source — skip

        Vec3 away = towardSource.scale(-1.0 / Math.sqrt(distSq)); // normalise

        // Force decreases smoothly with distance²; zero at ~20 blocks
        double force = Math.max(0.0, PUSH_BASE - distSq * PUSH_FALLOFF);
        if (force < 1e-5) return;

        Vec3 current = player.getDeltaMovement();

        // Only add to horizontal components; don't fight gravity
        double newX = current.x + away.x * force;
        double newZ = current.z + away.z * force;

        // Cap speed so the push never launches the player across the map
        double hSpeed = Math.sqrt(newX * newX + newZ * newZ);
        if (hSpeed > PUSH_MAX_SPEED) {
            double scale = PUSH_MAX_SPEED / hSpeed;
            newX *= scale;
            newZ *= scale;
        }

        player.setDeltaMovement(newX, current.y, newZ);
        player.hurtMarked = true; // sync velocity to client
    }

    // =========================================================================
    // Shared helper — resolves "flee-from" Vec3 for any entity
    // =========================================================================

    /**
     * Returns the world position the entity should flee from:
     *  1. The live attacker's current position (always up-to-date)
     *  2. The stored last-hurt position (fallback when attacker is dead/gone)
     *  3. null if neither is available
     */
    public static Vec3 resolveFleeOrigin(LivingEntity entity) {
        net.minecraft.nbt.CompoundTag tag = entity.getPersistentData();

        // Priority 1: live attacker
        int sourceId = tag.getInt(FEAR_SOURCE_KEY);
        if (sourceId != 0) {
            net.minecraft.world.entity.Entity attacker = entity.level().getEntity(sourceId);
            if (attacker != null && attacker.isAlive()) {
                return attacker.position();
            }
        }

        // Priority 2: last-hurt world position
        if (tag.contains(FEAR_LAST_X)) {
            return new Vec3(
                tag.getDouble(FEAR_LAST_X),
                tag.getDouble(FEAR_LAST_Y),
                tag.getDouble(FEAR_LAST_Z)
            );
        }

        return null;
    }

    // =========================================================================
    // PersistentData helpers — called from BuffDamageHandler
    // =========================================================================

    /**
     * Records attacker entity ID and hit position when a feared entity takes damage.
     * Keeps the flee origin always pointing to the most recent attacker / hit spot.
     */
    public static void recordDamageSource(LivingEntity target, LivingEntity attacker) {
        net.minecraft.nbt.CompoundTag tag = target.getPersistentData();
        if (attacker != null) {
            tag.putInt(FEAR_SOURCE_KEY, attacker.getId());
        }
        Vec3 pos = attacker != null ? attacker.position() : target.position();
        tag.putDouble(FEAR_LAST_X, pos.x);
        tag.putDouble(FEAR_LAST_Y, pos.y);
        tag.putDouble(FEAR_LAST_Z, pos.z);
    }

    /** Cleans up all fear-related PersistentData tags on effect expiry. */
    public static void cleanupTags(LivingEntity entity) {
        net.minecraft.nbt.CompoundTag tag = entity.getPersistentData();
        tag.remove(FEAR_SOURCE_KEY);
        tag.remove(FEAR_LAST_X);
        tag.remove(FEAR_LAST_Y);
        tag.remove(FEAR_LAST_Z);
        tag.remove(FEAR_GOAL_TAG);
    }

    // =========================================================================
    // Inner class — Mob AI Goal injection
    // =========================================================================

    public static class FearAIManager {

        /**
         * Injects a high-priority flee Goal if one has not been added yet.
         * The Goal continuously re-evaluates the flee origin every tick so it
         * adapts when the attacker moves or dies.
         */
        public static void ensureFleeGoal(Mob mob) {
            net.minecraft.nbt.CompoundTag tag = mob.getPersistentData();
            if (tag.getBoolean(FEAR_GOAL_TAG)) return; // already injected

            try {
                mob.goalSelector.addGoal(0, new FleeGoal(mob)); // priority 0 = highest
                tag.putBoolean(FEAR_GOAL_TAG, true);
            } catch (Exception e) {
                LOGGER.warn("[xEB] Failed to inject flee goal for mob {}: {}", mob.getId(), e.getMessage());
            }
        }

        /** Clears the guard tag so canUse() returns false on next evaluation. */
        public static void removeFleeGoal(Mob mob) {
            mob.getPersistentData().remove(FEAR_GOAL_TAG);
        }
    }

    // =========================================================================
    // Flee Goal — used by Mobs
    // =========================================================================

    /**
     * Steers the mob smoothly away from its fear origin every tick.
     *
     * Design:
     *  - Flee direction is recomputed every 4 ticks (more responsive than 8).
     *  - We keep a "smoothed direction" that lerps toward the raw away-vector
     *    each recompute step, preventing snappy 180° turns when the source moves.
     *  - Multiple candidate positions are tried at decreasing distances so the
     *    mob always finds a navigable escape route.
     */
    private static final class FleeGoal extends Goal {

        private static final double FLEE_SPEED  = 1.6;
        private static final double FLEE_REACH  = 14.0;
        private static final int    FLEE_TRIES  = 10;
        /** How much the smoothed direction lerps per recompute (0=frozen, 1=instant). */
        private static final double DIR_LERP    = 0.35;

        private final Mob mob;
        private net.minecraft.world.entity.ai.navigation.PathNavigation nav;
        private Vec3 smoothedDir = null; // lerped flee direction
        private Vec3 fleeTarget  = null;

        FleeGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mob.hasEffect(ModEffects.FEAR.get()) && resolveFleeOrigin(mob) != null;
        }

        @Override
        public boolean canContinueToUse() {
            return mob.hasEffect(ModEffects.FEAR.get());
        }

        @Override
        public void start() {
            this.nav = mob.getNavigation();
            smoothedDir = null;
            recomputeFleeTarget();
        }

        @Override
        public void tick() {
            // Recompute every 4 ticks — responsive but not thrashing
            if (mob.tickCount % 4 == 0) {
                recomputeFleeTarget();
            }
            if (fleeTarget != null) {
                nav.moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, FLEE_SPEED);
            }
        }

        @Override
        public void stop() {
            fleeTarget  = null;
            smoothedDir = null;
            if (nav != null) nav.stop();
        }

        private void recomputeFleeTarget() {
            Vec3 origin = resolveFleeOrigin(mob);
            if (origin == null) {
                fleeTarget = null;
                return;
            }

            // Raw away direction
            Vec3 rawAway = mob.position().subtract(origin);
            double len = rawAway.length();
            if (len < 0.01) {
                // Mob is on top of source — pick a random escape direction
                rawAway = new Vec3(
                    mob.getRandom().nextDouble() - 0.5, 0,
                    mob.getRandom().nextDouble() - 0.5
                ).normalize();
            } else {
                rawAway = rawAway.scale(1.0 / len); // normalise
            }

            // Lerp the smoothed direction toward the raw away-vector.
            // This prevents snappy 180° turns when the source moves quickly.
            if (smoothedDir == null) {
                smoothedDir = rawAway;
            } else {
                smoothedDir = new Vec3(
                    smoothedDir.x + (rawAway.x - smoothedDir.x) * DIR_LERP,
                    0,
                    smoothedDir.z + (rawAway.z - smoothedDir.z) * DIR_LERP
                ).normalize();
            }

            // Try decreasing reach distances until a navigable path is found
            for (int i = FLEE_TRIES; i >= 1; i--) {
                double reach = FLEE_REACH * (i / (double) FLEE_TRIES);
                Vec3 candidate = mob.position().add(smoothedDir.scale(reach));
                net.minecraft.world.level.pathfinder.Path path =
                        nav.createPath(candidate.x, candidate.y, candidate.z, 0);
                if (path != null) {
                    fleeTarget = candidate;
                    return;
                }
            }
            // No path found this tick — keep old target, retry next recompute
        }
    }
}
