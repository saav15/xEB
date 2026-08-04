package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MeteorStrikeSyncPacket {
    private final int entityId;
    private final int state;
    private final boolean isV2;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final int targetCount;

    public MeteorStrikeSyncPacket(int entityId, int state, boolean isV2, double targetX, double targetY, double targetZ, int targetCount) {
        this.entityId = entityId;
        this.state = state;
        this.isV2 = isV2;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.targetCount = targetCount;
    }

    public int getEntityId() { return entityId; }
    public int getState() { return state; }
    public boolean isV2() { return isV2; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTargetZ() { return targetZ; }
    public int getTargetCount() { return targetCount; }

    public static void encode(MeteorStrikeSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.state);
        buf.writeBoolean(msg.isV2);
        buf.writeDouble(msg.targetX);
        buf.writeDouble(msg.targetY);
        buf.writeDouble(msg.targetZ);
        buf.writeInt(msg.targetCount);
    }

    public static MeteorStrikeSyncPacket decode(FriendlyByteBuf buf) {
        return new MeteorStrikeSyncPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt()
        );
    }

    public static void handle(MeteorStrikeSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMeteorStrikeSync(msg));
        });
        ctx.setPacketHandled(true);
    }
}
