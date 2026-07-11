package org.xeb.xeb.opticblast;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Immutable data class representing a single active laser beam in the world.
 */
public class BeamData {
    private final UUID ownerUUID;
    private final int ownerEntityId;
    private final Vec3 start;
    private Vec3 end;
    private final int color;
    private final long tickCreated;
    private long tickExpires;

    private final String beamSource; // "optic_blast", "brimstone", "external"

    public BeamData(UUID ownerUUID, int ownerEntityId, Vec3 start, Vec3 end, int color, long tickCreated, long tickExpires) {
        this(ownerUUID, ownerEntityId, start, end, color, tickCreated, tickExpires, "optic_blast");
    }

    public BeamData(UUID ownerUUID, int ownerEntityId, Vec3 start, Vec3 end, int color, long tickCreated, long tickExpires, String beamSource) {
        this.ownerUUID = ownerUUID;
        this.ownerEntityId = ownerEntityId;
        this.start = start;
        this.end = end;
        this.color = color;
        this.tickCreated = tickCreated;
        this.tickExpires = tickExpires;
        this.beamSource = beamSource;
    }

    public String getBeamSource() { return beamSource; }

    public UUID getOwnerUUID() { return ownerUUID; }
    public int getOwnerEntityId() { return ownerEntityId; }
    public Vec3 getStart() { return start; }
    public Vec3 getEnd() { return end; }
    public int getColor() { return color; }
    public long getTickCreated() { return tickCreated; }
    public long getTickExpires() { return tickExpires; }

    public void setEnd(Vec3 end) { this.end = end; }
    public void setTickExpires(long tickExpires) { this.tickExpires = tickExpires; }

    /** Returns the direction vector (normalized) from start to end. */
    public Vec3 getDirection() {
        return end.subtract(start).normalize();
    }

    /** Returns the length of this beam. */
    public double getLength() {
        return start.distanceTo(end);
    }
}
