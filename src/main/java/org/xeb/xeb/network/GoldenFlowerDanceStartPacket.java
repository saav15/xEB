package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GoldenFlowerDanceStartPacket {
    private final int[] targetEntityIds;
    private final int cloneCount;

    public GoldenFlowerDanceStartPacket(int[] targetEntityIds, int cloneCount) {
        this.targetEntityIds = targetEntityIds;
        this.cloneCount = cloneCount;
    }

    public static void encode(GoldenFlowerDanceStartPacket msg, FriendlyByteBuf buf) {
        buf.writeVarIntArray(msg.targetEntityIds);
        buf.writeInt(msg.cloneCount);
    }

    public static GoldenFlowerDanceStartPacket decode(FriendlyByteBuf buf) {
        return new GoldenFlowerDanceStartPacket(buf.readVarIntArray(), buf.readInt());
    }

    public static void handle(GoldenFlowerDanceStartPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null) {
                    org.xeb.xeb.client.PhantomAttackManager.startAttack(msg.targetEntityIds, msg.cloneCount);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
