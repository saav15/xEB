package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import org.xeb.xeb.item.ModItems;

import java.util.function.Supplier;

public class FlourishPacket {
    private final int itemId;

    public FlourishPacket(Item item) {
        this.itemId = Item.getId(item);
    }

    public FlourishPacket(int itemId) {
        this.itemId = itemId;
    }

    public static void encode(FlourishPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.itemId);
    }

    public static FlourishPacket decode(FriendlyByteBuf buf) {
        return new FlourishPacket(buf.readInt());
    }

    public static void handle(FlourishPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.isAlive()) {
                Item item = Item.byId(msg.itemId);
                if (item == ModItems.THE_TEARS.get()) {
                    org.xeb.xeb.item.TheTearsItem.triggerAbilityAnim(player);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 1.0F, 1.4F);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.6F);
                } else if (item == ModItems.SMART_HALBERD.get()) {
                    // Track glow stacks in player persistent data for server sound pitch
                    long now = System.currentTimeMillis();
                    long lastTime = player.getPersistentData().getLong("xebHalberdGlowTime");
                    int level = player.getPersistentData().getInt("xebHalberdGlowLevel");

                    if (now - lastTime > 1000L) {
                        level = 1;
                    } else {
                        level = Math.min(4, level + 1);
                    }
                    player.getPersistentData().putLong("xebHalberdGlowTime", now);
                    player.getPersistentData().putInt("xebHalberdGlowLevel", level);

                    // Flag forced eye animation change
                    player.getPersistentData().putBoolean("xebHalberdEyeForced", true);

                    // Play cybernetic beacon/amethyst sound with pitch scaling per level
                    float pitch = 1.0F + (level * 0.22F);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, pitch);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, pitch);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
