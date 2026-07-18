package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BrimstoneBeamPacket {
    public final int ownerId;
    public final boolean active;
    public final int imbueType;
    public final List<Vec3> points;
    public final float beamWidth;

    public BrimstoneBeamPacket(int ownerId, boolean active, int imbueType, List<Vec3> points) {
        this(ownerId, active, imbueType, points, 1.0F);
    }

    public BrimstoneBeamPacket(int ownerId, boolean active, int imbueType, List<Vec3> points, float beamWidth) {
        this.ownerId = ownerId;
        this.active = active;
        this.imbueType = imbueType;
        this.points = points;
        this.beamWidth = beamWidth;
    }

    public static void encode(BrimstoneBeamPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.ownerId);
        buf.writeBoolean(msg.active);
        buf.writeInt(msg.imbueType);
        buf.writeFloat(msg.beamWidth);
        buf.writeInt(msg.points.size());
        for (Vec3 p : msg.points) {
            buf.writeDouble(p.x);
            buf.writeDouble(p.y);
            buf.writeDouble(p.z);
        }
    }

    public static BrimstoneBeamPacket decode(FriendlyByteBuf buf) {
        int ownerId = buf.readInt();
        boolean active = buf.readBoolean();
        int imbueType = buf.readInt();
        float beamWidth = buf.readFloat();
        int size = buf.readInt();
        List<Vec3> points = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            points.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        return new BrimstoneBeamPacket(ownerId, active, imbueType, points, beamWidth);
    }

    public static void handle(BrimstoneBeamPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            org.xeb.xeb.client.renderer.BrimstoneBeamRenderer.handleBeamPacket(
                    msg.ownerId, msg.active, msg.imbueType, msg.points, msg.beamWidth
            );
        });
        ctx.setPacketHandled(true);
    }
}
