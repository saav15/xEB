package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DoomfistPowerBlockSyncPacket {
    private final int entityId;
    private final boolean powerBlocking;

    public DoomfistPowerBlockSyncPacket(int entityId, boolean powerBlocking) {
        this.entityId = entityId;
        this.powerBlocking = powerBlocking;
    }

    public int getEntityId() {
        return entityId;
    }

    public boolean isPowerBlocking() {
        return powerBlocking;
    }

    public static void encode(DoomfistPowerBlockSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.powerBlocking);
    }

    public static DoomfistPowerBlockSyncPacket decode(FriendlyByteBuf buf) {
        return new DoomfistPowerBlockSyncPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(DoomfistPowerBlockSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDoomfistPowerBlock(msg));
        });
        ctx.setPacketHandled(true);
    }
}
