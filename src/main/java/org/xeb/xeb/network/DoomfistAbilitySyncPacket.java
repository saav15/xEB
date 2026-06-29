package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DoomfistAbilitySyncPacket {
    private final int entityId;
    private final int uppercutFloatTicks;
    private final int slamState;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final int uppercutCooldown;
    private final int slamCooldown;
    private final boolean flashHUD;

    public DoomfistAbilitySyncPacket(int entityId, int uppercutFloatTicks, int slamState) {
        this(entityId, uppercutFloatTicks, slamState, 0.0D, 0.0D, 0.0D, 0, 0, false);
    }

    public DoomfistAbilitySyncPacket(int entityId, int uppercutFloatTicks, int slamState, double targetX, double targetY, double targetZ) {
        this(entityId, uppercutFloatTicks, slamState, targetX, targetY, targetZ, 0, 0, false);
    }

    public DoomfistAbilitySyncPacket(int entityId, int uppercutFloatTicks, int slamState, double targetX, double targetY, double targetZ, int uppercutCooldown, int slamCooldown) {
        this(entityId, uppercutFloatTicks, slamState, targetX, targetY, targetZ, uppercutCooldown, slamCooldown, false);
    }

    public DoomfistAbilitySyncPacket(int entityId, int uppercutFloatTicks, int slamState, double targetX, double targetY, double targetZ, int uppercutCooldown, int slamCooldown, boolean flashHUD) {
        this.entityId = entityId;
        this.uppercutFloatTicks = uppercutFloatTicks;
        this.slamState = slamState;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.uppercutCooldown = uppercutCooldown;
        this.slamCooldown = slamCooldown;
        this.flashHUD = flashHUD;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getUppercutFloatTicks() {
        return uppercutFloatTicks;
    }

    public int getSlamState() {
        return slamState;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public double getTargetZ() {
        return targetZ;
    }

    public int getUppercutCooldown() {
        return uppercutCooldown;
    }

    public int getSlamCooldown() {
        return slamCooldown;
    }

    public boolean isFlashHUD() {
        return flashHUD;
    }

    public static void encode(DoomfistAbilitySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.uppercutFloatTicks);
        buf.writeInt(msg.slamState);
        buf.writeDouble(msg.targetX);
        buf.writeDouble(msg.targetY);
        buf.writeDouble(msg.targetZ);
        buf.writeInt(msg.uppercutCooldown);
        buf.writeInt(msg.slamCooldown);
        buf.writeBoolean(msg.flashHUD);
    }

    public static DoomfistAbilitySyncPacket decode(FriendlyByteBuf buf) {
        return new DoomfistAbilitySyncPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public static void handle(DoomfistAbilitySyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDoomfistAbility(msg));
        });
        ctx.setPacketHandled(true);
    }
}
