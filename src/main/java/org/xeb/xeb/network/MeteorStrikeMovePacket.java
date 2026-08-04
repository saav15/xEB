package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MeteorStrikeMovePacket {
    private final double targetX;
    private final double targetY;
    private final double targetZ;

    public MeteorStrikeMovePacket(double targetX, double targetY, double targetZ) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public static void encode(MeteorStrikeMovePacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.targetX);
        buf.writeDouble(msg.targetY);
        buf.writeDouble(msg.targetZ);
    }

    public static MeteorStrikeMovePacket decode(FriendlyByteBuf buf) {
        return new MeteorStrikeMovePacket(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(MeteorStrikeMovePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.getPersistentData().getInt("xebMeteorStrikeState") == 2) {
                player.getPersistentData().putDouble("xebMeteorStrikeTargetX", msg.targetX);
                player.getPersistentData().putDouble("xebMeteorStrikeTargetY", msg.targetY);
                player.getPersistentData().putDouble("xebMeteorStrikeTargetZ", msg.targetZ);
            }
        });
        ctx.setPacketHandled(true);
    }
}
