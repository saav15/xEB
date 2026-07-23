package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.xeb.xeb.client.renderer.StevenLaserBeamRenderer;

import java.util.function.Supplier;

public class StevenLaserSyncPacket {
    private final int ownerId;
    private final boolean active;
    private final Vec3 start;
    private final Vec3 end;

    public StevenLaserSyncPacket(int ownerId, boolean active, Vec3 start, Vec3 end) {
        this.ownerId = ownerId;
        this.active = active;
        this.start = start != null ? start : Vec3.ZERO;
        this.end = end != null ? end : Vec3.ZERO;
    }

    public static void encode(StevenLaserSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.ownerId);
        buf.writeBoolean(msg.active);
        buf.writeDouble(msg.start.x);
        buf.writeDouble(msg.start.y);
        buf.writeDouble(msg.start.z);
        buf.writeDouble(msg.end.x);
        buf.writeDouble(msg.end.y);
        buf.writeDouble(msg.end.z);
    }

    public static StevenLaserSyncPacket decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        boolean active = buf.readBoolean();
        Vec3 start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 end = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new StevenLaserSyncPacket(id, active, start, end);
    }

    public static void handle(StevenLaserSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            StevenLaserBeamRenderer.updateLaser(msg.ownerId, msg.active, msg.start, msg.end);
        });
        ctx.get().setPacketHandled(true);
    }
}
