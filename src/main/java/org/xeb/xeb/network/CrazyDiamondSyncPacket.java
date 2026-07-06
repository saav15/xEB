package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class CrazyDiamondSyncPacket {
    private final int a1Cooldown;
    private final int a2Cooldown;
    private final int punches;
    private final int chargeTimer;

    public CrazyDiamondSyncPacket(int a1Cooldown, int a2Cooldown, int punches, int chargeTimer) {
        this.a1Cooldown = a1Cooldown;
        this.a2Cooldown = a2Cooldown;
        this.punches = punches;
        this.chargeTimer = chargeTimer;
    }

    public static void encode(CrazyDiamondSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.a1Cooldown);
        buf.writeInt(msg.a2Cooldown);
        buf.writeInt(msg.punches);
        buf.writeInt(msg.chargeTimer);
    }

    public static CrazyDiamondSyncPacket decode(FriendlyByteBuf buf) {
        return new CrazyDiamondSyncPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(CrazyDiamondSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                Player player = mc.player;
                player.getPersistentData().putInt("xebCDA1CooldownTicks", msg.a1Cooldown);
                player.getPersistentData().putInt("xebCDA2CooldownTicks", msg.a2Cooldown);
                player.getPersistentData().putInt("xebCDPunches", msg.punches);
                player.getPersistentData().putInt("xebCDChargeTimer", msg.chargeTimer);
            }
        });
        ctx.setPacketHandled(true);
    }
}
