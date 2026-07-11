package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HolySyncPacket {
    private final int entityId;
    private final boolean parrying;
    private final int crownState;
    private final int crownTicks;
    private final boolean crossSlashActive;
    private final int a1Cooldown;
    private final int a2Cooldown;
    private final int comboStage;

    public HolySyncPacket(int entityId, boolean parrying, int crownState, int crownTicks, boolean crossSlashActive, int a1Cooldown, int a2Cooldown, int comboStage) {
        this.entityId = entityId;
        this.parrying = parrying;
        this.crownState = crownState;
        this.crownTicks = crownTicks;
        this.crossSlashActive = crossSlashActive;
        this.a1Cooldown = a1Cooldown;
        this.a2Cooldown = a2Cooldown;
        this.comboStage = comboStage;
    }

    public int getEntityId() { return entityId; }
    public boolean isParrying() { return parrying; }
    public int getCrownState() { return crownState; }
    public int getCrownTicks() { return crownTicks; }
    public boolean isCrossSlashActive() { return crossSlashActive; }
    public int getA1Cooldown() { return a1Cooldown; }
    public int getA2Cooldown() { return a2Cooldown; }
    public int getComboStage() { return comboStage; }

    public static void encode(HolySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.parrying);
        buf.writeInt(msg.crownState);
        buf.writeInt(msg.crownTicks);
        buf.writeBoolean(msg.crossSlashActive);
        buf.writeInt(msg.a1Cooldown);
        buf.writeInt(msg.a2Cooldown);
        buf.writeInt(msg.comboStage);
    }

    public static HolySyncPacket decode(FriendlyByteBuf buf) {
        return new HolySyncPacket(
                buf.readInt(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(HolySyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleHolySync(msg));
        });
        ctx.setPacketHandled(true);
    }
}
