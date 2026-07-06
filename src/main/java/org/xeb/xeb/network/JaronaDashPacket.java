package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class JaronaDashPacket {
    private final int playerId;
    private final int comboStep;

    public JaronaDashPacket(int playerId, int comboStep) {
        this.playerId = playerId;
        this.comboStep = comboStep;
    }

    public static void encode(JaronaDashPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.playerId);
        buf.writeInt(msg.comboStep);
    }

    public static JaronaDashPacket decode(FriendlyByteBuf buf) {
        return new JaronaDashPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(JaronaDashPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null) {
                    net.minecraft.world.entity.Entity entity = mc.level.getEntity(msg.playerId);
                    if (entity instanceof net.minecraft.world.entity.player.Player player) {
                        org.xeb.xeb.client.JaronaAttack.startKick(player, msg.comboStep);
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
