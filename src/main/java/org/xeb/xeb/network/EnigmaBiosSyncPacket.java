package org.xeb.xeb.network;

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

    public EnigmaBiosSyncPacket(boolean[] unlocked, int newlyUnlocked) {
        this.unlocked = unlocked;
        this.newlyUnlocked = newlyUnlocked;
    }

    public static void encode(EnigmaBiosSyncPacket msg, FriendlyByteBuf buf) {
        for (int i = 0; i < 5; i++) {
            buf.writeBoolean(msg.unlocked[i]);
        }
        buf.writeInt(msg.newlyUnlocked);
    }

    public static EnigmaBiosSyncPacket decode(FriendlyByteBuf buf) {
        boolean[] unlocked = new boolean[5];
        for (int i = 0; i < 5; i++) {
            unlocked[i] = buf.readBoolean();
        }
        return new EnigmaBiosSyncPacket(unlocked, buf.readInt());
    }

    public static void handle(EnigmaBiosSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    for (int i = 0; i < 5; i++) {
                        mc.player.getPersistentData().putBoolean("xebUnlockedBitacora" + (i + 1), msg.unlocked[i]);
                    }
                    if (msg.newlyUnlocked >= 1 && msg.newlyUnlocked <= 5) {
                        XebCompletionToast.show(msg.newlyUnlocked);
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
