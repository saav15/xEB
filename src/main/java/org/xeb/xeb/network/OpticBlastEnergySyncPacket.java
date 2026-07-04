package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client packet syncing the Optic Blast energy resource state.
 * Sent every tick while a player holds the Optic Blast, so the HUD can display
 * energy level, overheat status, and ability cooldowns.
 */
public class OpticBlastEnergySyncPacket {
    private final int entityId;
    private final float energy;
    private final boolean isOverheated;
    private final int cyclonePushCooldown;
    private final int geneSpliceCooldown;
    private final int cyclonePushTicks;
    private final int geneSpliceTicks;

    public OpticBlastEnergySyncPacket(int entityId, float energy, boolean isOverheated,
                                       int cyclonePushCooldown, int geneSpliceCooldown,
                                       int cyclonePushTicks, int geneSpliceTicks) {
        this.entityId = entityId;
        this.energy = energy;
        this.isOverheated = isOverheated;
        this.cyclonePushCooldown = cyclonePushCooldown;
        this.geneSpliceCooldown = geneSpliceCooldown;
        this.cyclonePushTicks = cyclonePushTicks;
        this.geneSpliceTicks = geneSpliceTicks;
    }

    public static void encode(OpticBlastEnergySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeFloat(msg.energy);
        buf.writeBoolean(msg.isOverheated);
        buf.writeInt(msg.cyclonePushCooldown);
        buf.writeInt(msg.geneSpliceCooldown);
        buf.writeInt(msg.cyclonePushTicks);
        buf.writeInt(msg.geneSpliceTicks);
    }

    public static OpticBlastEnergySyncPacket decode(FriendlyByteBuf buf) {
        return new OpticBlastEnergySyncPacket(
                buf.readInt(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(OpticBlastEnergySyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null) {
                    net.minecraft.world.entity.Entity entity = mc.level.getEntity(msg.entityId);
                    if (entity instanceof net.minecraft.world.entity.player.Player player) {
                        player.getPersistentData().putFloat("xebOpticEnergy", msg.energy);
                        player.getPersistentData().putBoolean("xebOpticOverheated", msg.isOverheated);
                        player.getPersistentData().putInt("xebCyclonePushCooldown", msg.cyclonePushCooldown);
                        player.getPersistentData().putInt("xebGeneSpliceCooldown", msg.geneSpliceCooldown);
                        player.getPersistentData().putInt("xebCyclonePushTicks", msg.cyclonePushTicks);
                        player.getPersistentData().putInt("xebGeneSpliceTicks", msg.geneSpliceTicks);
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
