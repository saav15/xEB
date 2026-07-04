package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server → Client packet for the Gene Splice chain beam.
 * Sends a list of points (entity positions) that form the chain laser path.
 * The renderer draws laser segments between each consecutive pair of points.
 */
public class OpticBlastChainBeamPacket {
    private final int ownerEntityId;
    private final boolean active;
    private final List<double[]> chainPoints; // Each element: [x, y, z]

    public OpticBlastChainBeamPacket(int ownerEntityId, boolean active, List<double[]> chainPoints) {
        this.ownerEntityId = ownerEntityId;
        this.active = active;
        this.chainPoints = chainPoints;
    }

    public static void encode(OpticBlastChainBeamPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.ownerEntityId);
        buf.writeBoolean(msg.active);
        buf.writeInt(msg.chainPoints.size());
        for (double[] point : msg.chainPoints) {
            buf.writeDouble(point[0]);
            buf.writeDouble(point[1]);
            buf.writeDouble(point[2]);
        }
    }

    public static OpticBlastChainBeamPacket decode(FriendlyByteBuf buf) {
        int ownerId = buf.readInt();
        boolean active = buf.readBoolean();
        int count = buf.readInt();
        List<double[]> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(new double[]{ buf.readDouble(), buf.readDouble(), buf.readDouble() });
        }
        return new OpticBlastChainBeamPacket(ownerId, active, points);
    }

    public static void handle(OpticBlastChainBeamPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    org.xeb.xeb.client.renderer.OpticBlastBeamRenderer.handleChainBeamPacket(
                            msg.ownerEntityId, msg.active, msg.chainPoints
                    )
            );
        });
        ctx.setPacketHandled(true);
    }
}
