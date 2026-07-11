package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sync packet for an active Beam Struggle.
 * Sent to ALL players so spectators see the struggle.
 */
public class BeamStrugglePacket {
    private final boolean isStart;
    private final UUID struggleId;
    private final int ownerAEntityId;
    private final int ownerBEntityId;
    private final Vec3 startPosA;
    private final Vec3 startPosB;
    private final Vec3 collisionPoint;
    private final float pointsA;
    private final float pointsB;
    private final int ticksElapsed;

    public BeamStrugglePacket(boolean isStart, UUID struggleId, int a, int b,
                              Vec3 startA, Vec3 startB, Vec3 collision,
                              float pointsA, float pointsB, int ticks) {
        this.isStart = isStart;
        this.struggleId = struggleId;
        this.ownerAEntityId = a;
        this.ownerBEntityId = b;
        this.startPosA = startA;
        this.startPosB = startB;
        this.collisionPoint = collision;
        this.pointsA = pointsA;
        this.pointsB = pointsB;
        this.ticksElapsed = ticks;
    }

    public static void encode(BeamStrugglePacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.isStart);
        buf.writeUUID(m.struggleId);
        buf.writeInt(m.ownerAEntityId);
        buf.writeInt(m.ownerBEntityId);
        buf.writeDouble(m.startPosA.x); buf.writeDouble(m.startPosA.y); buf.writeDouble(m.startPosA.z);
        buf.writeDouble(m.startPosB.x); buf.writeDouble(m.startPosB.y); buf.writeDouble(m.startPosB.z);
        buf.writeDouble(m.collisionPoint.x); buf.writeDouble(m.collisionPoint.y); buf.writeDouble(m.collisionPoint.z);
        buf.writeFloat(m.pointsA);
        buf.writeFloat(m.pointsB);
        buf.writeInt(m.ticksElapsed);
    }

    public static BeamStrugglePacket decode(FriendlyByteBuf buf) {
        return new BeamStrugglePacket(
                buf.readBoolean(), buf.readUUID(), buf.readInt(), buf.readInt(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readFloat(), buf.readFloat(), buf.readInt()
        );
    }

    public static void handle(BeamStrugglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.xeb.xeb.client.renderer.BeamStruggleRenderer.handleStrugglePacket(msg)));
        ctx.get().setPacketHandled(true);
    }

    // Getters
    public boolean isStart() { return isStart; }
    public UUID getStruggleId() { return struggleId; }
    public int getOwnerAEntityId() { return ownerAEntityId; }
    public int getOwnerBEntityId() { return ownerBEntityId; }
    public Vec3 getStartPosA() { return startPosA; }
    public Vec3 getStartPosB() { return startPosB; }
    public Vec3 getCollisionPoint() { return collisionPoint; }
    public float getPointsA() { return pointsA; }
    public float getPointsB() { return pointsB; }
    public int getTicksElapsed() { return ticksElapsed; }
}
