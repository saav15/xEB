package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class BeamStruggleEndPacket {
    private final int winnerEntityId;
    private final int loserEntityId;

    public BeamStruggleEndPacket(int winner, int loser) {
        this.winnerEntityId = winner;
        this.loserEntityId = loser;
    }

    public static void encode(BeamStruggleEndPacket m, FriendlyByteBuf buf) {
        buf.writeInt(m.winnerEntityId); buf.writeInt(m.loserEntityId);
    }

    public static BeamStruggleEndPacket decode(FriendlyByteBuf buf) {
        return new BeamStruggleEndPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(BeamStruggleEndPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.xeb.xeb.client.renderer.BeamStruggleRenderer.handleStruggleEnd(msg)));
        ctx.get().setPacketHandled(true);
    }

    public int getWinnerEntityId() { return winnerEntityId; }
    public int getLoserEntityId() { return loserEntityId; }
}
