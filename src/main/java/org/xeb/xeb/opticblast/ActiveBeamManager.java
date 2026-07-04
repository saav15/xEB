package org.xeb.xeb.opticblast;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active laser beams in the world.
 * Provides beam-vs-beam collision checking by discretizing beams into 1-block AABB segments.
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
     * Checks if a beam from the given owner intersects with any other active beam.
     * Discretizes both beams into 1-block segments and checks AABB overlaps.
     *
     * @param ownerUUID  The UUID of the beam owner to check
     * @param beamStart  Start position of the beam
     * @param beamEnd    End position of the beam
     * @return The collision point (closest intersection), or null if no collision.
     */
    public Vec3 checkBeamVsBeamCollision(UUID ownerUUID, Vec3 beamStart, Vec3 beamEnd) {
        double halfWidth = 0.5D; // half the 1-block-wide hitbox
        Vec3 closestCollision = null;
        double closestDist = Double.MAX_VALUE;

        for (Map.Entry<UUID, BeamData> entry : activeBeams.entrySet()) {
            if (entry.getKey().equals(ownerUUID)) continue; // don't collide with self

            BeamData other = entry.getValue();
            Vec3 otherStart = other.getStart();
            Vec3 otherEnd = other.getEnd();

            // Discretize both beams into segments
            List<AABB> mySegments = discretizeBeam(beamStart, beamEnd, halfWidth);
            List<AABB> otherSegments = discretizeBeam(otherStart, otherEnd, halfWidth);

            for (int i = 0; i < mySegments.size(); i++) {
                AABB myBox = mySegments.get(i);
                for (AABB otherBox : otherSegments) {
                    if (myBox.intersects(otherBox)) {
                        // Calculate approximate collision point (center of my segment)
                        Vec3 dir = beamEnd.subtract(beamStart).normalize();
                        Vec3 collisionPoint = beamStart.add(dir.scale(i + 0.5D));
                        double dist = beamStart.distanceToSqr(collisionPoint);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closestCollision = collisionPoint;
                        }
                    }
                }
            }
        }

        return closestCollision;
    }

    /**
     * Discretizes a beam into 1-block-length AABB segments.
     */
    private static List<AABB> discretizeBeam(Vec3 start, Vec3 end, double halfWidth) {
        List<AABB> segments = new ArrayList<>();
        double length = start.distanceTo(end);
        if (length < 0.01D) return segments;

        Vec3 dir = end.subtract(start).normalize();
        int segmentCount = Math.max(1, (int) Math.ceil(length));

        for (int i = 0; i < segmentCount; i++) {
            Vec3 segCenter = start.add(dir.scale(i + 0.5D));
            AABB box = new AABB(
                    segCenter.x - halfWidth, segCenter.y - halfWidth, segCenter.z - halfWidth,
                    segCenter.x + halfWidth, segCenter.y + halfWidth, segCenter.z + halfWidth
            );
            segments.add(box);
        }

        return segments;
    }

    /**
     * Clears all beams (e.g., on server stop or dimension change).
     */
    public void clearAll() {
        activeBeams.clear();
    }
}
