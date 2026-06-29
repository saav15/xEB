package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DoomfistUltraChargeSyncPacket {
    private final int entityId;
    private final boolean ultraCharged;

    public DoomfistUltraChargeSyncPacket(int entityId, boolean ultraCharged) {
        this.entityId = entityId;
        this.ultraCharged = ultraCharged;
    }

    public int getEntityId() {
        return entityId;
    }

    public boolean isUltraCharged() {
        return ultraCharged;
    }

    public static void encode(DoomfistUltraChargeSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.ultraCharged);
    }

    public static DoomfistUltraChargeSyncPacket decode(FriendlyByteBuf buf) {
        return new DoomfistUltraChargeSyncPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(DoomfistUltraChargeSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDoomfistUltraCharge(msg));
        });
        ctx.setPacketHandled(true);
    }
}
