package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client packet syncing active beam state for the Optic Blast.
 * <p>
 * Beam types:
 * <ul>
 *   <li>0 = PRIMARY (right-click beam)</li>
 *   <li>1 = CYCLONE_PUSH (Activa 1)</li>
 * </ul>
 */
public class OpticBlastBeamPacket {
    public static final byte BEAM_PRIMARY = 0;
    public static final byte BEAM_CYCLONE_PUSH = 1;

    private final int ownerEntityId;
    private final boolean active;
    private final double startX, startY, startZ;
    private final double endX, endY, endZ;
    private final byte beamType;

    public OpticBlastBeamPacket(int ownerEntityId, boolean active,
                                double startX, double startY, double startZ,
                                double endX, double endY, double endZ,
                                byte beamType) {
        this.ownerEntityId = ownerEntityId;
        this.active = active;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
        this.beamType = beamType;
    }

    public static void encode(OpticBlastBeamPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.ownerEntityId);
        buf.writeBoolean(msg.active);
        buf.writeDouble(msg.startX);
        buf.writeDouble(msg.startY);
        buf.writeDouble(msg.startZ);
        buf.writeDouble(msg.endX);
        buf.writeDouble(msg.endY);
        buf.writeDouble(msg.endZ);
        buf.writeByte(msg.beamType);
    }

    public static OpticBlastBeamPacket decode(FriendlyByteBuf buf) {
        return new OpticBlastBeamPacket(
                buf.readInt(), buf.readBoolean(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readByte()
        );
    }

    public static void handle(OpticBlastBeamPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    org.xeb.xeb.client.renderer.OpticBlastBeamRenderer.handleBeamPacket(
                            msg.ownerEntityId, msg.active,
                            msg.startX, msg.startY, msg.startZ,
                            msg.endX, msg.endY, msg.endZ,
                            msg.beamType
                    )
            );
        });
        ctx.setPacketHandled(true);
    }
}
