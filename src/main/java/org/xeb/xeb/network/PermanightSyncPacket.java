package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.function.Supplier;

public class PermanightSyncPacket {
    private final boolean active;
    private final int ticksRemaining;

    public PermanightSyncPacket(boolean active) {
        this(active, active ? 24000 : 0);
    }

    public PermanightSyncPacket(boolean active, int ticksRemaining) {
        this.active = active;
        this.ticksRemaining = ticksRemaining;
    }

    public boolean isActive() {
        return active;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public static void encode(PermanightSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeInt(msg.ticksRemaining);
    }

    public static PermanightSyncPacket decode(FriendlyByteBuf buf) {
        return new PermanightSyncPacket(buf.readBoolean(), buf.readInt());
    }

    public static void handle(PermanightSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePermanightSync(msg));
        });
        ctx.setPacketHandled(true);
    }
}
