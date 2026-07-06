package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GoldenFlowerSyncPacket {
    private final int entityId;
    private final int flowerCharges;
    private final int flowerRechargeTimer;
    private final int loadedFlowerCount;
    private final int jaronaCharges;
    private final int jaronaRechargeTimer;
    private final int danceCooldown;

    public GoldenFlowerSyncPacket(int entityId, int flowerCharges, int flowerRechargeTimer,
                                  int loadedFlowerCount, int jaronaCharges, int jaronaRechargeTimer, int danceCooldown) {
        this.entityId = entityId;
        this.flowerCharges = flowerCharges;
        this.flowerRechargeTimer = flowerRechargeTimer;
        this.loadedFlowerCount = loadedFlowerCount;
        this.jaronaCharges = jaronaCharges;
        this.jaronaRechargeTimer = jaronaRechargeTimer;
        this.danceCooldown = danceCooldown;
    }

    public static void encode(GoldenFlowerSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.flowerCharges);
        buf.writeInt(msg.flowerRechargeTimer);
        buf.writeInt(msg.loadedFlowerCount);
        buf.writeInt(msg.jaronaCharges);
        buf.writeInt(msg.jaronaRechargeTimer);
        buf.writeInt(msg.danceCooldown);
    }

    public static GoldenFlowerSyncPacket decode(FriendlyByteBuf buf) {
        return new GoldenFlowerSyncPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(GoldenFlowerSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null) {
                    net.minecraft.world.entity.Entity entity = mc.level.getEntity(msg.entityId);
                    if (entity instanceof net.minecraft.world.entity.player.Player player) {
                        player.getPersistentData().putInt("xebGoldenFlowerCharges", msg.flowerCharges);
                        player.getPersistentData().putInt("xebGoldenFlowerRechargeTimer", msg.flowerRechargeTimer);
                        player.getPersistentData().putInt("xebGoldenFlowerLoadedCount", msg.loadedFlowerCount);
                        player.getPersistentData().putInt("xebJaronaCharges", msg.jaronaCharges);
                        player.getPersistentData().putInt("xebJaronaRechargeTimer", msg.jaronaRechargeTimer);
                        player.getPersistentData().putInt("xebGoldenFlowerDanceCooldown", msg.danceCooldown);
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
