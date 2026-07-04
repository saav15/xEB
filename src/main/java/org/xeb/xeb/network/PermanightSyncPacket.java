package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.function.Supplier;

public class PermanightSyncPacket {
    private final boolean active;

    public PermanightSyncPacket(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public static void encode(PermanightSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static PermanightSyncPacket decode(FriendlyByteBuf buf) {
        return new PermanightSyncPacket(buf.readBoolean());
    }

    public static void handle(PermanightSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePermanightSync(msg));
        });
        ctx.setPacketHandled(true);
    }
}
