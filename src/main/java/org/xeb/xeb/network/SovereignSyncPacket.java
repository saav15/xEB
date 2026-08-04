package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SovereignSyncPacket {

    private final int playerId;
    private final boolean active;
    private final Vec3 anchor;
    private final int totalTicks;
    private final ItemStack castItem;

    public SovereignSyncPacket(int playerId, boolean active, Vec3 anchor, int totalTicks, ItemStack castItem) {
        this.playerId = playerId;
        this.active = active;
        this.anchor = anchor;
        this.totalTicks = totalTicks;
        this.castItem = castItem != null ? castItem : ItemStack.EMPTY;
    }

    public static void encode(SovereignSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.playerId);
        buf.writeBoolean(msg.active);
        buf.writeDouble(msg.anchor.x);
        buf.writeDouble(msg.anchor.y);
        buf.writeDouble(msg.anchor.z);
        buf.writeInt(msg.totalTicks);
        buf.writeItem(msg.castItem);
    }

    public static SovereignSyncPacket decode(FriendlyByteBuf buf) {
        int playerId = buf.readInt();
        boolean active = buf.readBoolean();
        Vec3 anchor = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int totalTicks = buf.readInt();
        ItemStack castItem = buf.readItem();
        return new SovereignSyncPacket(playerId, active, anchor, totalTicks, castItem);
    }

    public static void handle(SovereignSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSovereignSync(msg.playerId, msg.active, msg.anchor, msg.totalTicks, msg.castItem));
        });
        ctx.setPacketHandled(true);
    }
}
