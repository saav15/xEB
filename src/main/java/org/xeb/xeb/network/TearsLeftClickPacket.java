package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.item.TheTearsItem;

import java.util.function.Supplier;

public class TearsLeftClickPacket {
    public TearsLeftClickPacket() {}

    public static void encode(TearsLeftClickPacket msg, FriendlyByteBuf buf) {}

    public static TearsLeftClickPacket decode(FriendlyByteBuf buf) {
        return new TearsLeftClickPacket();
    }

    public static void handle(TearsLeftClickPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.isAlive() && player.getMainHandItem().is(ModItems.THE_TEARS.get())) {
                if (player.getMainHandItem().getItem() instanceof TheTearsItem tearsItem) {
                    tearsItem.triggerLeftClick(player, player.serverLevel());
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
