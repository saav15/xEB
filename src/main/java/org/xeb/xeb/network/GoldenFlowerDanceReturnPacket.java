package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GoldenFlowerDanceReturnPacket {
    public GoldenFlowerDanceReturnPacket() {}

    public static void encode(GoldenFlowerDanceReturnPacket msg, FriendlyByteBuf buf) {
    }

    public static GoldenFlowerDanceReturnPacket decode(FriendlyByteBuf buf) {
        return new GoldenFlowerDanceReturnPacket();
    }

    public static void handle(GoldenFlowerDanceReturnPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.isAlive()) {
                net.minecraft.nbt.CompoundTag pData = player.getPersistentData();
                
                // 1. Lower Flower Dance Cooldown by 50 ticks (2.5 seconds)
                int danceCD = pData.getInt("xebGoldenFlowerDanceCooldown");
                if (danceCD > 0) {
                    pData.putInt("xebGoldenFlowerDanceCooldown", Math.max(0, danceCD - 50));
                }

                // 2. Add 50 ticks (2.5 seconds) recharge time to Jarona
                int jaronaCharges = pData.getInt("xebJaronaCharges");
                if (jaronaCharges < 3) {
                    int timer = pData.getInt("xebJaronaRechargeTimer") + 50;
                    if (timer >= 120) {
                        jaronaCharges = Math.min(3, jaronaCharges + 1);
                        timer = jaronaCharges < 3 ? timer - 120 : 0;
                    }
                    pData.putInt("xebJaronaCharges", jaronaCharges);
                    pData.putInt("xebJaronaRechargeTimer", timer);
                }

                // Play custom absorption sound
                player.level().playSound(null, player, org.xeb.xeb.sound.ModSounds.ABSORB_FLOWER_CLONE.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
            }
        });
        ctx.setPacketHandled(true);
    }
}
