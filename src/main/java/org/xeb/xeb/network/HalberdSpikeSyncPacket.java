package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class HalberdSpikeSyncPacket {
    private final int playerId;
    private final List<Vec3> spikePositions;
    private final boolean isCharging;

    public HalberdSpikeSyncPacket(int playerId, List<Vec3> spikePositions, boolean isCharging) {
        this.playerId = playerId;
        this.spikePositions = spikePositions;
        this.isCharging = isCharging;
    }

    public static void encode(HalberdSpikeSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.playerId);
        buf.writeInt(msg.spikePositions.size());
        for (Vec3 pos : msg.spikePositions) {
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
        }
        buf.writeBoolean(msg.isCharging);
    }

    public static HalberdSpikeSyncPacket decode(FriendlyByteBuf buf) {
        int playerId = buf.readInt();
        int size = buf.readInt();
        List<Vec3> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        boolean isCharging = buf.readBoolean();
        return new HalberdSpikeSyncPacket(playerId, positions, isCharging);
    }

    public static void handle(HalberdSpikeSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    Player player = (Player) mc.level.getEntity(msg.playerId);
                    if (player != null) {
                        var data = player.getPersistentData();
                        if (msg.isCharging) {
                            data.putInt("xebHalberdChargeTicks", msg.spikePositions.size() * 5); // approximate
                            data.putInt("xebHalberdSpikeCount", msg.spikePositions.size());
                            for (int i = 0; i < msg.spikePositions.size(); i++) {
                                Vec3 pos = msg.spikePositions.get(i);
                                String key = "xebHalberdSpike" + i;
                                data.putDouble(key + "X", pos.x);
                                data.putDouble(key + "Y", pos.y);
                                data.putDouble(key + "Z", pos.z);
                            }
                        } else {
                            // Cleanup
                            data.remove("xebHalberdChargeTicks");
                            int count = data.getInt("xebHalberdSpikeCount");
                            for (int i = 0; i < count; i++) {
                                String key = "xebHalberdSpike" + i;
                                data.remove(key + "X");
                                data.remove(key + "Y");
                                data.remove(key + "Z");
                            }
                            data.remove("xebHalberdSpikeCount");
                        }
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
