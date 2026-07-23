package org.xeb.xeb.opticblast;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active laser beams in the world.
 * Provides optimized segment-to-segment collision checking.
 */
public class ActiveBeamManager {
    private static final ActiveBeamManager INSTANCE = new ActiveBeamManager();

    /** All active beams, keyed by owner UUID. */
    private final Map<UUID, BeamData> activeBeams = new ConcurrentHashMap<>();

    private ActiveBeamManager() {}

    public static ActiveBeamManager get() {
        return INSTANCE;
    }

    /**
     * Registers or updates a beam for the given owner.
     */
    public void putBeam(UUID ownerUUID, BeamData beam) {
        activeBeams.put(ownerUUID, beam);
    }

    /**
     * Removes the beam for the given owner.
     */
    public void removeBeam(UUID ownerUUID) {
        activeBeams.remove(ownerUUID);
    }

    /**
     * Returns all currently active beams.
     */
    public Collection<BeamData> getActiveBeams() {
        return Collections.unmodifiableCollection(activeBeams.values());
    }

    /**
     * Gets a specific beam by owner UUID.
     */
    public BeamData getBeam(UUID ownerUUID) {
        return activeBeams.get(ownerUUID);
    }

    /**
     * Cleans up expired beams based on the current game tick.
     */
    public void tickBeams(long currentTick) {
        activeBeams.entrySet().removeIf(entry -> entry.getValue().getTickExpires() <= currentTick);
    }

    /**
     * Default collision check for Optic Blast.
     */
    public Vec3 checkBeamVsBeamCollision(UUID ownerUUID, Vec3 beamStart, Vec3 beamEnd) {
        return checkBeamVsBeamCollision(ownerUUID, beamStart, beamEnd, 0.8D);
    }

    /**
     * Checks if a beam from the given owner intersects with any other active beam.
     * Uses optimized segment-to-segment math.
     *
     * @param ownerUUID    The UUID of the beam owner to check
     * @param beamStart    Start position of the beam
     * @param beamEnd      End position of the beam
     * @param myHalfWidth  Collision half-width for this beam
     * @return The collision point (closest intersection), or null if no collision.
     */
    public Vec3 checkBeamVsBeamCollision(UUID ownerUUID, Vec3 beamStart, Vec3 beamEnd, double myHalfWidth) {
        Vec3 closestCollision = null;
        double closestDist = Double.MAX_VALUE;

        for (Map.Entry<UUID, BeamData> entry : activeBeams.entrySet()) {
            if (entry.getValue().getOwnerUUID().equals(ownerUUID)) continue; // don't collide with self

            BeamData other = entry.getValue();
            double otherHalfWidth = getBeamHalfWidth(other.getBeamSource());
            Vec3 otherStart = other.getStart();
            Vec3 otherEnd = other.getEnd();

            Vec3 collision = checkSegmentIntersection(beamStart, beamEnd, otherStart, otherEnd, myHalfWidth, otherHalfWidth);
            if (collision != null) {
                double dist = beamStart.distanceToSqr(collision);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestCollision = collision;
                }
            }
        }

        // External beams
        for (org.xeb.xeb.beamstruggle.ExternalBeamRegistry.ExternalBeamData other : org.xeb.xeb.beamstruggle.ExternalBeamRegistry.getCurrentTickBeams()) {
            if (other.ownerUUID().equals(ownerUUID)) continue;

            double otherHalfWidth = 0.6D; // external beams default
            Vec3 collision = checkSegmentIntersection(beamStart, beamEnd, other.start(), other.end(), myHalfWidth, otherHalfWidth);
            if (collision != null) {
                double dist = beamStart.distanceToSqr(collision);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestCollision = collision;
                }
            }
        }

        return closestCollision;
    }

    public UUID findCollidingBeamOwner(UUID ownerUUID, Vec3 beamStart, Vec3 beamEnd, double myHalfWidth) {
        UUID closestOwner = null;
        double closestDist = Double.MAX_VALUE;

        for (Map.Entry<UUID, BeamData> entry : activeBeams.entrySet()) {
            if (entry.getValue().getOwnerUUID().equals(ownerUUID)) continue;

            BeamData other = entry.getValue();
            double otherHalfWidth = getBeamHalfWidth(other.getBeamSource());
            Vec3 otherStart = other.getStart();
            Vec3 otherEnd = other.getEnd();

            Vec3 collision = checkSegmentIntersection(beamStart, beamEnd, otherStart, otherEnd, myHalfWidth, otherHalfWidth);
            if (collision != null) {
                double dist = beamStart.distanceToSqr(collision);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestOwner = other.getOwnerUUID();
                }
            }
        }

        for (org.xeb.xeb.beamstruggle.ExternalBeamRegistry.ExternalBeamData other : org.xeb.xeb.beamstruggle.ExternalBeamRegistry.getCurrentTickBeams()) {
            if (other.ownerUUID().equals(ownerUUID)) continue;

            double otherHalfWidth = 0.6D;
            Vec3 collision = checkSegmentIntersection(beamStart, beamEnd, other.start(), other.end(), myHalfWidth, otherHalfWidth);
            if (collision != null) {
                double dist = beamStart.distanceToSqr(collision);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestOwner = other.ownerUUID();
                }
            }
        }

        return closestOwner;
    }

    private double getBeamHalfWidth(String beamSource) {
        if (beamSource == null) return 0.8D;
        return switch (beamSource) {
            case "brimstone" -> 1.2D; // Brimstone is thicker
            case "optic_blast" -> 0.8D;
            default -> 0.8D;
        };
    }

    /**
     * Calculates the real intersection point between two 3D segments using line closest approach math.
     * Returns the midpoint of closest approach if they are within threshold distance.
     */
    private Vec3 checkSegmentIntersection(Vec3 a1, Vec3 a2, Vec3 b1, Vec3 b2, double halfWidthA, double halfWidthB) {
        double threshold = halfWidthA + halfWidthB;
        double thresholdSq = threshold * threshold;

        Vec3 d1 = a2.subtract(a1); // direction of segment A
        Vec3 d2 = b2.subtract(b1); // direction of segment B
        Vec3 r = a1.subtract(b1);

        double a = d1.dot(d1);
        double e = d2.dot(d2);
        double f = d2.dot(r);

        double s, t;
        if (a <= 1e-6 && e <= 1e-6) {
            s = 0; t = 0;
        } else if (a <= 1e-6) {
            s = 0;
            t = Math.max(0, Math.min(1, f / e));
        } else {
            double c = d1.dot(r);
            if (e <= 1e-6) {
                t = 0;
                s = Math.max(0, Math.min(1, -c / a));
            } else {
                double b = d1.dot(d2);
                double denom = a * e - b * b;
                if (denom != 0) {
                    s = Math.max(0, Math.min(1, (b * f - c * e) / denom));
                } else {
                    s = 0;
                }
                t = (b * s + f) / e;
                if (t < 0) {
                    t = 0;
                    s = Math.max(0, Math.min(1, -c / a));
                } else if (t > 1) {
                    t = 1;
                    s = Math.max(0, Math.min(1, (b - c) / a));
                }
            }
        }

        Vec3 closestA = a1.add(d1.scale(s));
        Vec3 closestB = b1.add(d2.scale(t));
        double distSq = closestA.distanceToSqr(closestB);

        if (distSq < thresholdSq) {
            return new Vec3(
                    (closestA.x + closestB.x) / 2.0,
                    (closestA.y + closestB.y) / 2.0,
                    (closestA.z + closestB.z) / 2.0
            );
        }
        return null;
    }

    /**
     * Clears all beams.
     */
    public void clearAll() {
        activeBeams.clear();
    }
}
