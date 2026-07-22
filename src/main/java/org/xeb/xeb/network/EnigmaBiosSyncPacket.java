package org.xeb.xeb.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import org.xeb.xeb.client.gui.XebCompletionToast;
import java.util.function.Supplier;

public class EnigmaBiosSyncPacket {
    private final boolean[] unlocked;
    private final int newlyUnlocked; // -1 if just syncing, 1 to 5 if newly unlocked
    private final CompoundTag killData;

    public EnigmaBiosSyncPacket(boolean[] unlocked, int newlyUnlocked, CompoundTag killData) {
        this.unlocked = unlocked != null ? unlocked : new boolean[0];
        this.newlyUnlocked = newlyUnlocked;
        this.killData = killData != null ? killData : new CompoundTag();
    }

    public EnigmaBiosSyncPacket(boolean[] unlocked, int newlyUnlocked) {
        this(unlocked, newlyUnlocked, new CompoundTag());
    }

    public static void encode(EnigmaBiosSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.unlocked.length);
        for (int i = 0; i < msg.unlocked.length; i++) {
            buf.writeBoolean(msg.unlocked[i]);
        }
        buf.writeInt(msg.newlyUnlocked);
        buf.writeNbt(msg.killData);
    }

    public static EnigmaBiosSyncPacket decode(FriendlyByteBuf buf) {
        int length = buf.readInt();
        boolean[] unlocked = new boolean[length];
        for (int i = 0; i < length; i++) {
            unlocked[i] = buf.readBoolean();
        }
        int newlyUnlocked = buf.readInt();
        CompoundTag killData = buf.readNbt();
        return new EnigmaBiosSyncPacket(unlocked, newlyUnlocked, killData);
    }

    public static void handle(EnigmaBiosSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    for (int i = 0; i < msg.unlocked.length; i++) {
                        mc.player.getPersistentData().putBoolean("xebUnlockedBitacora" + (i + 1), msg.unlocked[i]);
                    }
                    if (msg.killData != null) {
                        mc.player.getPersistentData().merge(msg.killData);
                    }
                    if (msg.newlyUnlocked >= 1) {
                        XebCompletionToast.show(msg.newlyUnlocked);
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
