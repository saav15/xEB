package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs Omega Flowery instance state from server to clients.
 * Carries: entity ID, active flag, and remaining ticks.
 */
public class OmegaFlowerySyncPacket {

    private final int     entityId;
    private final boolean active;
    private final int     ticksRemaining;

    public OmegaFlowerySyncPacket(int entityId, boolean active, int ticksRemaining) {
        this.entityId       = entityId;
        this.active         = active;
        this.ticksRemaining = ticksRemaining;
    }

    public static void encode(OmegaFlowerySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.active);
        buf.writeInt(msg.ticksRemaining);
    }

    public static OmegaFlowerySyncPacket decode(FriendlyByteBuf buf) {
        return new OmegaFlowerySyncPacket(buf.readInt(), buf.readBoolean(), buf.readInt());
    }

    public static void handle(OmegaFlowerySyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientPacketHandler.handleOmegaFlowerySync(msg)));
        ctx.get().setPacketHandled(true);
    }

    public int     getEntityId()       { return entityId; }
    public boolean isActive()          { return active; }
    public int     getTicksRemaining() { return ticksRemaining; }
}
