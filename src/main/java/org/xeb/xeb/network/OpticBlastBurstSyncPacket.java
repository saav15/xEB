package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client packet syncing active Optic Blast Extreme Burst state, timer, and start pitch.
 */
public class OpticBlastBurstSyncPacket {
    private final int entityId;
    private final int state;
    private final int timer;
    private final float startPitch;

    public OpticBlastBurstSyncPacket(int entityId, int state, int timer, float startPitch) {
        this.entityId = entityId;
        this.state = state;
        this.timer = timer;
        this.startPitch = startPitch;
    }

    public static void encode(OpticBlastBurstSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.state);
        buf.writeInt(msg.timer);
        buf.writeFloat(msg.startPitch);
    }

    public static OpticBlastBurstSyncPacket decode(FriendlyByteBuf buf) {
        return new OpticBlastBurstSyncPacket(
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readFloat()
        );
    }

    public static void handle(OpticBlastBurstSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPacketHandler.handleOpticBlastBurstSync(msg)
        ));
        ctx.setPacketHandled(true);
    }

    public int getEntityId() { return entityId; }
    public int getState() { return state; }
    public int getTimer() { return timer; }
    public float getStartPitch() { return startPitch; }
}
