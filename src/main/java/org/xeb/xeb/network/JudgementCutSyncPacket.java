package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class JudgementCutSyncPacket {

    private final int playerId;
    private final boolean active;
    private final Vec3 anchor;
    private final int totalTicks;

    public JudgementCutSyncPacket(int playerId, boolean active, Vec3 anchor, int totalTicks) {
        this.playerId = playerId;
        this.active = active;
        this.anchor = anchor;
        this.totalTicks = totalTicks;
    }

    public static void encode(JudgementCutSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.playerId);
        buf.writeBoolean(msg.active);
        buf.writeDouble(msg.anchor.x);
        buf.writeDouble(msg.anchor.y);
        buf.writeDouble(msg.anchor.z);
        buf.writeInt(msg.totalTicks);
    }

    public static JudgementCutSyncPacket decode(FriendlyByteBuf buf) {
        int playerId = buf.readInt();
        boolean active = buf.readBoolean();
        Vec3 anchor = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int totalTicks = buf.readInt();
        return new JudgementCutSyncPacket(playerId, active, anchor, totalTicks);
    }

    public static void handle(JudgementCutSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleJudgementCutSync(msg.playerId, msg.active, msg.anchor, msg.totalTicks));
        });
        ctx.setPacketHandled(true);
    }
}
