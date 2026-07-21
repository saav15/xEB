package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HolySyncPacket {
    private final int entityId;
    private final boolean shieldActive;
    private final boolean annihilationActive;
    private final boolean blessedActive;
    private final int blessedTicks;
    private final int blastCharge;
    private final double blastRadius;
    private final int a1Cooldown;
    private final int a2Cooldown;
    private final int comboStage;
    private final int attackTicks;
    private final int playingCombo;

    public HolySyncPacket(int entityId, boolean shieldActive, boolean annihilationActive,
                           boolean blessedActive, int blessedTicks, int blastCharge, double blastRadius,
                           int a1Cooldown, int a2Cooldown, int comboStage, int attackTicks, int playingCombo) {
        this.entityId = entityId;
        this.shieldActive = shieldActive;
        this.annihilationActive = annihilationActive;
        this.blessedActive = blessedActive;
        this.blessedTicks = blessedTicks;
        this.blastCharge = blastCharge;
        this.blastRadius = blastRadius;
        this.a1Cooldown = a1Cooldown;
        this.a2Cooldown = a2Cooldown;
        this.comboStage = comboStage;
        this.attackTicks = attackTicks;
        this.playingCombo = playingCombo;
    }

    public int getEntityId() { return entityId; }
    public boolean isShieldActive() { return shieldActive; }
    public boolean isAnnihilationActive() { return annihilationActive; }
    public boolean isBlessedActive() { return blessedActive; }
    public int getBlessedTicks() { return blessedTicks; }
    public int getBlastCharge() { return blastCharge; }
    public double getBlastRadius() { return blastRadius; }
    public int getA1Cooldown() { return a1Cooldown; }
    public int getA2Cooldown() { return a2Cooldown; }
    public int getComboStage() { return comboStage; }
    public int getAttackTicks() { return attackTicks; }
    public int getPlayingCombo() { return playingCombo; }

    public static void encode(HolySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.shieldActive);
        buf.writeBoolean(msg.annihilationActive);
        buf.writeBoolean(msg.blessedActive);
        buf.writeInt(msg.blessedTicks);
        buf.writeInt(msg.blastCharge);
        buf.writeDouble(msg.blastRadius);
        buf.writeInt(msg.a1Cooldown);
        buf.writeInt(msg.a2Cooldown);
        buf.writeInt(msg.comboStage);
        buf.writeInt(msg.attackTicks);
        buf.writeInt(msg.playingCombo);
    }

    public static HolySyncPacket decode(FriendlyByteBuf buf) {
        return new HolySyncPacket(
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readDouble(),
                buf.readInt(),
                buf.readInt(),
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

