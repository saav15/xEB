package org.xeb.xeb.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TearsSyncPacket {
    public final int a1CD;
    public final int a2CD;
    public final int imbueType;
    public final int imbueDur;
    public final int charge;
    public final int firing;
    public final boolean instant;
    public final int animTicks;
    public final String animName;

    public TearsSyncPacket(int a1CD, int a2CD, int imbueType, int imbueDur, int charge, int firing, boolean instant, int animTicks, String animName) {
        this.a1CD = a1CD;
        this.a2CD = a2CD;
        this.imbueType = imbueType;
        this.imbueDur = imbueDur;
        this.charge = charge;
        this.firing = firing;
        this.instant = instant;
        this.animTicks = animTicks;
        this.animName = animName != null ? animName : "";
    }

    public static void encode(TearsSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.a1CD);
        buf.writeInt(msg.a2CD);
        buf.writeInt(msg.imbueType);
        buf.writeInt(msg.imbueDur);
        buf.writeInt(msg.charge);
        buf.writeInt(msg.firing);
        buf.writeBoolean(msg.instant);
        buf.writeInt(msg.animTicks);
        buf.writeUtf(msg.animName);
    }

    public static TearsSyncPacket decode(FriendlyByteBuf buf) {
        return new TearsSyncPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readUtf()
        );
    }

    public static void handle(TearsSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Player player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                CompoundTag tag = player.getPersistentData();
                tag.putInt("xebTearsA1Cooldown", msg.a1CD);
                tag.putInt("xebTearsA2Cooldown", msg.a2CD);
                tag.putInt("xebTearsImbueType", msg.imbueType);
                tag.putInt("xebTearsImbueDuration", msg.imbueDur);
                tag.putInt("xebBrimstoneCharge", msg.charge);
                tag.putInt("xebBrimstoneFiringTicks", msg.firing);
                tag.putBoolean("xebNextBrimstoneInstant", msg.instant);
                tag.putInt("xebTearsAbilityAnimTicks", msg.animTicks);
                tag.putString("xebTearsAbilityAnimName", msg.animName);
            }
        });
        ctx.setPacketHandled(true);
    }
}
