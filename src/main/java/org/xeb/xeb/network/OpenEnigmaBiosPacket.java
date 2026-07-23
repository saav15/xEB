package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.xeb.xeb.item.EnigmaBiosItem;

import java.util.function.Supplier;

public class OpenEnigmaBiosPacket {

    public OpenEnigmaBiosPacket() {}

    public static void encode(OpenEnigmaBiosPacket msg, FriendlyByteBuf buf) {}

    public static OpenEnigmaBiosPacket decode(FriendlyByteBuf buf) {
        return new OpenEnigmaBiosPacket();
    }

    public static void handle(OpenEnigmaBiosPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && EnigmaBiosItem.hasEnigmaBiosEquippedOrInInventory(player)) {
                org.xeb.xeb.event.EnigmaBiosHandler.syncBitacoras(player, -1);
            }
        });
        ctx.setPacketHandled(true);
    }
}
