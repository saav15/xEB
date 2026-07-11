package org.xeb.xeb.beamstruggle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.opticblast.ActiveBeamManager;
import org.xeb.xeb.opticblast.BeamData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative Beam Struggle manager.
 *
 * <p>A Beam Struggle triggers when two beams from different owners collide.
 * Each side presses Flourish (B) to accumulate mash points; the side with more
 * points per tick pushes the collision point toward the loser. When the collision
 * point reaches one owner's beam start, that owner loses and takes a Blast Impact
 * (huge damage + knockback + explosion particles).
 *
 * <p>Struggles auto-expire after 8 seconds (160 ticks) if unresolved.
 */
public class BeamStruggleManager {

    private static final Map<UUID, BeamStruggle> ACTIVE_STRUGGLES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> OWNER_TO_STRUGGLE = new ConcurrentHashMap<>();

    /** Maximum duration of a struggle before it auto-resolves as a draw. */
    public static final int MAX_STRUGGLE_TICKS = 160;

    /** Mash points per Flourish click. */
    public static final double MASH_POINT_PER_CLICK = 1.0;

    /** Natural decay per tick (so idling loses to active mashing). */
    public static final double MASH_DECAY_PER_TICK = 0.05;

    /** Distance the collision point moves per point of advantage per tick. */
    public static final double COLLISION_MOVE_PER_POINT = 0.15;

    /** Maximum distance from midpoint before the struggle ends. */
    public static final double WIN_DISTANCE = 6.0;

    public static class BeamStruggle {
        public final UUID struggleId;
        public final UUID ownerA;
        public final UUID ownerB;
        public final Vec3 startPosA; // beam start of owner A
        public final Vec3 startPosB; // beam start of owner B
        public final Vec3 midpoint; // initial collision point
        public Vec3 currentCollision; // moves toward loser
        public double pointsA;
        public double pointsB;
        public int ticksElapsed;
        public final long startTick;

        public BeamStruggle(UUID id, UUID a, UUID b, Vec3 startA, Vec3 startB, Vec3 collision, long tick) {
            this.struggleId = id;
            this.ownerA = a; this.ownerB = b;
            this.startPosA = startA; this.startPosB = startB;
            this.midpoint = collision;
            this.currentCollision = new Vec3(collision.x, collision.y, collision.z);
            this.startTick = tick;
        }
    }

    /**
     * Called from OpticBlastTickHandler every tick when a beam-vs-beam collision is detected.
     * Returns true if the beam endpoint should be overridden to the struggle's collision point.
     */
    public static boolean onBeamCollision(UUID ownerA, UUID ownerB, Vec3 startA, Vec3 startB,
                                          Vec3 collisionPoint, long currentTick, ServerLevel level) {
        // Check if either owner is already in a struggle
        BeamStruggle struggle = findActiveStruggle(ownerA, ownerB);

        if (struggle == null) {
            // Create a new struggle
            UUID id = UUID.randomUUID();
            struggle = new BeamStruggle(id, ownerA, ownerB, startA, startB, collisionPoint, currentTick);
            ACTIVE_STRUGGLES.put(id, struggle);
            OWNER_TO_STRUGGLE.put(ownerA, id);
            OWNER_TO_STRUGGLE.put(ownerB, id);

            // Broadcast struggle start packet
            broadcastStruggleStart(struggle, level);
        }

        // Update struggle position based on mash points
        struggle.ticksElapsed++;
        double advantage = struggle.pointsA - struggle.pointsB; // positive = A wins

        Vec3 dirAtoB = struggle.startPosB.subtract(struggle.startPosA).normalize();
        // If A is winning (advantage > 0), collision moves toward B (loser)
        Vec3 moveVector = dirAtoB.scale(advantage * COLLISION_MOVE_PER_POINT);
        struggle.currentCollision = struggle.currentCollision.add(moveVector);

        // Decay mash points
        struggle.pointsA = Math.max(0, struggle.pointsA - MASH_DECAY_PER_TICK);
        struggle.pointsB = Math.max(0, struggle.pointsB - MASH_DECAY_PER_TICK);

        // Check win condition
        double distFromMid = struggle.currentCollision.distanceTo(struggle.midpoint);
        if (distFromMid >= WIN_DISTANCE || struggle.ticksElapsed >= MAX_STRUGGLE_TICKS) {
            resolveStruggle(struggle, level, currentTick);
            return false; // struggle ended, beam resumes normal collision
        }

        // Broadcast update packet
        broadcastStruggleUpdate(struggle, level);

        return true; // beam endpoint should be overridden
    }

    /**
     * Handles a Flourish key press from a player.
     */
    public static void handleFlourishPress(ServerPlayer player) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(player.getUUID());
        if (struggleId == null) return;

        BeamStruggle struggle = ACTIVE_STRUGGLES.get(struggleId);
        if (struggle == null) return;

        if (player.getUUID().equals(struggle.ownerA)) {
            struggle.pointsA += MASH_POINT_PER_CLICK;
        } else if (player.getUUID().equals(struggle.ownerB)) {
            struggle.pointsB += MASH_POINT_PER_CLICK;
        }
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
            // Draw — both take damage
            applyBlastImpact(level, struggle.ownerA, struggle.currentCollision, 1.0F);
            applyBlastImpact(level, struggle.ownerB, struggle.currentCollision, 1.0F);
            cleanupStruggle(struggle);
            return;
        }

        applyBlastImpact(level, loser, struggle.currentCollision, 2.0F);

        // Cleanup
        cleanupStruggle(struggle);

        // Broadcast struggle end packet
        broadcastStruggleEnd(struggle, level, winner, loser);
    }

    private static void applyBlastImpact(ServerLevel level, UUID targetId, Vec3 explosionPos, float multiplier) {
        net.minecraft.world.entity.Entity target = level.getEntity(targetId);
        if (target instanceof LivingEntity living) {
            living.hurt(level.damageSources().explosion(null, null), 30.0F * multiplier);
            Vec3 knockback = living.position().subtract(explosionPos).normalize().scale(1.5 * multiplier);
            living.setDeltaMovement(knockback.x, knockback.y + 0.5, knockback.z);
            living.hurtMarked = true;
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                explosionPos.x, explosionPos.y, explosionPos.z, 3, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                explosionPos.x, explosionPos.y, explosionPos.z, 30, 1.0, 1.0, 1.0, 0.2);
        level.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.8F);
    }

    public static void onServerTick(ServerLevel level, long currentTick) {
        // Cleanup expired struggles
        List<UUID> toRemove = new ArrayList<>();
        for (BeamStruggle s : ACTIVE_STRUGGLES.values()) {
            if (s.ticksElapsed >= MAX_STRUGGLE_TICKS) {
                toRemove.add(s.struggleId);
            }
        }
        for (UUID id : toRemove) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(id);
            if (s != null) resolveStruggle(s, level, currentTick);
        }
    }

    // ── Packet broadcasting ──
    private static void broadcastStruggleStart(BeamStruggle s, ServerLevel level) {
        // Find owner entity IDs
        int idA = -1;
        int idB = -1;
        net.minecraft.world.entity.Entity entA = level.getEntity(s.ownerA);
        if (entA != null) idA = entA.getId();
        net.minecraft.world.entity.Entity entB = level.getEntity(s.ownerB);
        if (entB != null) idB = entB.getId();

        org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new org.xeb.xeb.network.BeamStrugglePacket(
                        true, s.struggleId,
                        idA, idB,
                        s.startPosA, s.startPosB, s.currentCollision,
                        (float) s.pointsA, (float) s.pointsB, 0
                )
        );
    }

    private static void broadcastStruggleUpdate(BeamStruggle s, ServerLevel level) {
        // Find owner entity IDs
        int idA = -1;
        int idB = -1;
        net.minecraft.world.entity.Entity entA = level.getEntity(s.ownerA);
        if (entA != null) idA = entA.getId();
        net.minecraft.world.entity.Entity entB = level.getEntity(s.ownerB);
        if (entB != null) idB = entB.getId();

        org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new org.xeb.xeb.network.BeamStrugglePacket(
                        false, s.struggleId,
                        idA, idB,
                        s.startPosA, s.startPosB, s.currentCollision,
                        (float) s.pointsA, (float) s.pointsB, s.ticksElapsed
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

        org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new org.xeb.xeb.network.BeamStruggleEndPacket(idWinner, idLoser)
        );
    }

    private static void cleanupStruggle(BeamStruggle struggle) {
        ACTIVE_STRUGGLES.remove(struggle.struggleId);
        OWNER_TO_STRUGGLE.remove(struggle.ownerA);
        OWNER_TO_STRUGGLE.remove(struggle.ownerB);
    }

    public static Vec3 getCollisionPointFor(UUID owner) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(owner);
        if (struggleId != null) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(struggleId);
            if (s != null) return s.currentCollision;
        }
        return null;
    }
}
