package org.xeb.xeb.beamstruggle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.network.BeamStruggleEndPacket;
import org.xeb.xeb.network.BeamStrugglePacket;
import org.xeb.xeb.network.XEBNetwork;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative Beam Struggle manager.
 */
public class BeamStruggleManager {

    public enum StrugglePhase {
        PREP,       // 2 seconds prep phase, golden HUD, no movement
        ACTIVE,     // Active mashing and moving collision point
        RESOLVED    // Finished, waiting for cleanup
    }

    private static final Map<UUID, BeamStruggle> ACTIVE_STRUGGLES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> OWNER_TO_STRUGGLE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RECENT_STRUGGLE_COOLDOWN = new ConcurrentHashMap<>();

    public static final int MAX_STRUGGLE_TICKS = 320; // 16 seconds (was 160)
    public static final double MASH_POINT_PER_CLICK = 1.0;
    public static final double MASH_DECAY_PER_TICK = 0.05;
    public static final double COLLISION_MOVE_PER_POINT = 0.15;

    public static final int PREP_DURATION_TICKS = 40; // 2 seconds
    public static final double POINT_BLANK_DISTANCE = 4.0;
    public static final double FAR_DISTANCE = 30.0;
    public static final double WIN_DISTANCE_NORMAL = 6.0;
    public static final double WIN_DISTANCE_POINT_BLANK = 2.0;

    public static final int STRUGGLE_COOLDOWN_TICKS = 20; // 1 second cooldown

    public static class BeamStruggle {
        public final UUID struggleId;
        public final UUID ownerA;
        public final UUID ownerB;
        public final int ownerAEntityId;
        public final int ownerBEntityId;
        public final Vec3 startPosA;
        public final Vec3 startPosB;
        public Vec3 midpoint; // non-final to update tick-by-tick
        public Vec3 currentCollision;
        public double pointsA;
        public double pointsB;
        public int ticksElapsed;
        public final long startTick;
        public StrugglePhase phase;
        public final double initialDistance;
        public final double winDistance;
        public final double mashMultiplier;

        public BeamStruggle(UUID id, UUID a, UUID b, int idA, int idB, Vec3 startA, Vec3 startB,
                            Vec3 collision, long tick, StrugglePhase phase, double initialDistance,
                            double winDistance, double mashMultiplier) {
            this.struggleId = id;
            this.ownerA = a;
            this.ownerB = b;
            this.ownerAEntityId = idA;
            this.ownerBEntityId = idB;
            this.startPosA = startA;
            this.startPosB = startB;
            this.midpoint = collision;
            this.currentCollision = new Vec3(collision.x, collision.y, collision.z);
            this.startTick = tick;
            this.phase = phase;
            this.initialDistance = initialDistance;
            this.winDistance = winDistance;
            this.mashMultiplier = mashMultiplier;
            this.ticksElapsed = 0;
        }
    }

    public static boolean onBeamCollision(UUID ownerA, UUID ownerB, Vec3 startA, Vec3 startB,
                                          Vec3 collisionPoint, long currentTick, ServerLevel level) {
        // BUG 3 fix: cooldown check to avoid instant re-triggers
        Long cooldownA = RECENT_STRUGGLE_COOLDOWN.get(ownerA);
        Long cooldownB = RECENT_STRUGGLE_COOLDOWN.get(ownerB);
        if (cooldownA != null && currentTick - cooldownA < STRUGGLE_COOLDOWN_TICKS) return false;
        if (cooldownB != null && currentTick - cooldownB < STRUGGLE_COOLDOWN_TICKS) return false;

        BeamStruggle struggle = findActiveStruggle(ownerA, ownerB);

        if (struggle == null) {
            UUID id = UUID.randomUUID();
            double distance = startA.distanceTo(startB);
            double winDist = distance < POINT_BLANK_DISTANCE ? WIN_DISTANCE_POINT_BLANK : WIN_DISTANCE_NORMAL;
            double mashMult = 1.0 + (distance / 40.0);
            if (distance < POINT_BLANK_DISTANCE) {
                mashMult = 0.5; // point blank needs less mash = resolves extremely fast
            }

            StrugglePhase initialPhase = (distance < POINT_BLANK_DISTANCE) ? StrugglePhase.ACTIVE : StrugglePhase.PREP;
            int initialTicks = (distance < POINT_BLANK_DISTANCE) ? PREP_DURATION_TICKS : 0;

            int idA = getEntityIdFromUUID(level, ownerA);
            int idB = getEntityIdFromUUID(level, ownerB);

            struggle = new BeamStruggle(id, ownerA, ownerB, idA, idB, startA, startB, collisionPoint,
                    currentTick, initialPhase, distance, winDist, mashMult);
            struggle.ticksElapsed = initialTicks;
            ACTIVE_STRUGGLES.put(id, struggle);
            OWNER_TO_STRUGGLE.put(ownerA, id);
            OWNER_TO_STRUGGLE.put(ownerB, id);

            broadcastStruggleStart(struggle, level);

            // Start sound
            level.playSound(null, collisionPoint.x, collisionPoint.y, collisionPoint.z,
                    net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.5F);
        }

        struggle.ticksElapsed++;

        if (struggle.phase == StrugglePhase.PREP) {
            if (struggle.ticksElapsed >= PREP_DURATION_TICKS) {
                struggle.phase = StrugglePhase.ACTIVE;
                struggle.ticksElapsed = 0;
                broadcastStruggleUpdate(struggle, level);
            }
            return true;
        }

        if (struggle.phase == StrugglePhase.ACTIVE) {
            double timeMultiplier = 1.0 + (struggle.ticksElapsed / 80.0); // BUG 6: mash escalation
            double effectiveWinDistance = struggle.winDistance * (1.0 + struggle.ticksElapsed / 120.0);

            double advantage = struggle.pointsA - struggle.pointsB;
            Vec3 dirAtoB = struggle.startPosB.subtract(struggle.startPosA).normalize();
            Vec3 moveVector = dirAtoB.scale(advantage * COLLISION_MOVE_PER_POINT);
            struggle.currentCollision = struggle.currentCollision.add(moveVector);

            double decay = MASH_DECAY_PER_TICK * struggle.mashMultiplier * timeMultiplier;
            struggle.pointsA = Math.max(0, struggle.pointsA - decay);
            struggle.pointsB = Math.max(0, struggle.pointsB - decay);

            double distFromMid = struggle.currentCollision.distanceTo(struggle.midpoint);
            if (distFromMid >= effectiveWinDistance || struggle.ticksElapsed >= MAX_STRUGGLE_TICKS) {
                resolveStruggle(struggle, level, currentTick);
                return false;
            }

            if (struggle.ticksElapsed % 20 == 0) {
                level.playSound(null, struggle.currentCollision.x, struggle.currentCollision.y, struggle.currentCollision.z,
                        net.minecraft.sounds.SoundEvents.BEACON_AMBIENT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 2.0F);
            }

            broadcastStruggleUpdate(struggle, level);
            return true;
        }

        return false;
    }

    public static void updateCollisionPoint(UUID owner, Vec3 realCollisionPoint) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(owner);
        if (struggleId != null) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(struggleId);
            if (s != null && s.phase != StrugglePhase.RESOLVED) {
                // BUG 3 & BUG 4 fix: update midpoint and currentCollision with advantage offset
                Vec3 advantageOffset = s.currentCollision.subtract(s.midpoint);
                s.midpoint = realCollisionPoint;
                s.currentCollision = realCollisionPoint.add(advantageOffset);
            }
        }
    }

    public static void handleFlourishPress(ServerPlayer player) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(player.getUUID());
        if (struggleId == null) return;

        BeamStruggle struggle = ACTIVE_STRUGGLES.get(struggleId);
        if (struggle == null) return;
        if (struggle.phase != StrugglePhase.ACTIVE) return;

        double timeMultiplier = 1.0 + (struggle.ticksElapsed / 80.0); // BUG 6: mash escalation
        double points = MASH_POINT_PER_CLICK * struggle.mashMultiplier * timeMultiplier;
        if (player.getUUID().equals(struggle.ownerA)) {
            struggle.pointsA += points;
        } else if (player.getUUID().equals(struggle.ownerB)) {
            struggle.pointsB += points;
        }
    }

    public static boolean isInActiveStruggle(UUID playerUUID) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(playerUUID);
        if (struggleId == null) return false;
        BeamStruggle s = ACTIVE_STRUGGLES.get(struggleId);
        return s != null && s.phase == StrugglePhase.ACTIVE;
    }

    public static boolean isInAnyStruggle(UUID playerUUID) {
        return OWNER_TO_STRUGGLE.containsKey(playerUUID);
    }

    public static Vec3 getCollisionPointFor(UUID owner) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(owner);
        if (struggleId != null) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(struggleId);
            // BUG 1 fix: only return collision point if struggle is not RESOLVED
            if (s != null && s.phase != StrugglePhase.RESOLVED) {
                return s.currentCollision;
            }
        }
        return null;
    }

    public static void clearOwnerMapping(UUID owner) {
        OWNER_TO_STRUGGLE.remove(owner);
    }

    private static BeamStruggle findActiveStruggle(UUID a, UUID b) {
        UUID idA = OWNER_TO_STRUGGLE.get(a);
        if (idA != null) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(idA);
            if (s != null && (s.ownerA.equals(b) || s.ownerB.equals(b))) return s;
        }
        UUID idB = OWNER_TO_STRUGGLE.get(b);
        if (idB != null) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(idB);
            if (s != null && (s.ownerA.equals(a) || s.ownerB.equals(a))) return s;
        }
        return null;
    }

    private static void resolveStruggle(BeamStruggle struggle, ServerLevel level, long currentTick) {
        double advantage = struggle.pointsA - struggle.pointsB;
        UUID winner, loser;
        Vec3 loserStart;

        if (advantage > 0.1) {
            winner = struggle.ownerA; loser = struggle.ownerB;
            loserStart = struggle.startPosB;
        } else if (advantage < -0.1) {
            winner = struggle.ownerB; loser = struggle.ownerA;
            loserStart = struggle.startPosA;
        } else {
            // Draw
            applyBlastImpact(level, struggle.ownerA, struggle.currentCollision, 1.0F, false);
            applyBlastImpact(level, struggle.ownerB, struggle.currentCollision, 1.0F, false);
            cleanupStruggle(struggle, level);
            return;
        }

        applyBlastImpact(level, loser, struggle.currentCollision, 2.0F, true);
        applyBlastImpact(level, winner, struggle.currentCollision, 0.0F, false); // Play victory sound on winner

        cleanupStruggle(struggle, level);

        broadcastStruggleEnd(struggle, level, winner, loser);
    }

    private static void applyBlastImpact(ServerLevel level, UUID targetId, Vec3 explosionPos, float multiplier, boolean loser) {
        net.minecraft.world.entity.Entity target = level.getEntity(targetId);
        if (target instanceof LivingEntity living) {
            if (multiplier > 0) {
                living.hurt(level.damageSources().explosion(null, null), 30.0F * multiplier);
                Vec3 knockback = living.position().subtract(explosionPos).normalize().scale(1.5 * multiplier);
                living.setDeltaMovement(knockback.x, knockback.y + 0.5, knockback.z);
                living.hurtMarked = true;
            }
        }

        if (multiplier > 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    explosionPos.x, explosionPos.y, explosionPos.z, 3, 0.5, 0.5, 0.5, 0.0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    explosionPos.x, explosionPos.y, explosionPos.z, 30, 1.0, 1.0, 1.0, 0.2);

            if (loser) {
                level.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                        net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.8F);
                level.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                        net.minecraft.sounds.SoundEvents.WITHER_HURT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.8F);
            }
        } else {
            // Victory sounds
            level.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.0F);
            level.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                    net.minecraft.sounds.SoundEvents.WITHER_DEATH,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 2.0F);
        }
    }

    public static void onServerTick(ServerLevel level, long currentTick) {
        List<UUID> toRemove = new ArrayList<>();
        for (BeamStruggle s : ACTIVE_STRUGGLES.values()) {
            if (s.phase == StrugglePhase.ACTIVE && s.ticksElapsed >= MAX_STRUGGLE_TICKS) {
                toRemove.add(s.struggleId);
            }
        }
        for (UUID id : toRemove) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(id);
            if (s != null) resolveStruggle(s, level, currentTick);
        }
    }

    private static void broadcastStruggleStart(BeamStruggle s, ServerLevel level) {
        byte phaseByte = (byte) s.phase.ordinal();
        XEBNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new BeamStrugglePacket(
                        true, s.struggleId,
                        s.ownerAEntityId, s.ownerBEntityId,
                        s.startPosA, s.startPosB, s.currentCollision,
                        (float) s.pointsA, (float) s.pointsB, s.ticksElapsed,
                        phaseByte, s.initialDistance
                )
        );
    }

    private static void broadcastStruggleUpdate(BeamStruggle s, ServerLevel level) {
        byte phaseByte = (byte) s.phase.ordinal();
        XEBNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new BeamStrugglePacket(
                        false, s.struggleId,
                        s.ownerAEntityId, s.ownerBEntityId,
                        s.startPosA, s.startPosB, s.currentCollision,
                        (float) s.pointsA, (float) s.pointsB, s.ticksElapsed,
                        phaseByte, s.initialDistance
                )
        );
    }

    private static void broadcastStruggleEnd(BeamStruggle s, ServerLevel level, UUID winner, UUID loser) {
        int idWinner = -1;
        int idLoser = -1;
        net.minecraft.world.entity.Entity entWinner = level.getEntity(winner);
        if (entWinner != null) idWinner = entWinner.getId();
        net.minecraft.world.entity.Entity entLoser = level.getEntity(loser);
        if (entLoser != null) idLoser = entLoser.getId();

        XEBNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new BeamStruggleEndPacket(idWinner, idLoser)
        );
    }

    private static void cleanupStruggle(BeamStruggle struggle, ServerLevel level) {
        struggle.phase = StrugglePhase.RESOLVED;
        ACTIVE_STRUGGLES.remove(struggle.struggleId);
        OWNER_TO_STRUGGLE.remove(struggle.ownerA);
        OWNER_TO_STRUGGLE.remove(struggle.ownerB);
        long endTick = level.getGameTime();
        RECENT_STRUGGLE_COOLDOWN.put(struggle.ownerA, endTick);
        RECENT_STRUGGLE_COOLDOWN.put(struggle.ownerB, endTick);
    }

    private static int getEntityIdFromUUID(ServerLevel level, UUID uuid) {
        net.minecraft.world.entity.Entity e = level.getEntity(uuid.hashCode());
        if (e == null) {
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                if (p.getUUID().equals(uuid)) return p.getId();
            }
            for (net.minecraft.world.entity.Entity ent : level.getAllEntities()) {
                if (ent.getUUID().equals(uuid)) return ent.getId();
            }
        }
        return e != null ? e.getId() : -1;
    }
}
