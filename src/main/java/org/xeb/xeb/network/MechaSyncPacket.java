package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MechaSyncPacket {
    private final int entityId;
    private final boolean jetActive;
    private final boolean vulcanFiring;
    private final int spindashState;
    private final boolean dashing;
    private final int a1Cooldown;
    private final int a2Cooldown;
    private final double kineticSpeed;

    public MechaSyncPacket(int entityId, boolean jetActive, boolean vulcanFiring, int spindashState, boolean dashing, int a1Cooldown, int a2Cooldown, double kineticSpeed) {
        this.entityId = entityId;
        this.jetActive = jetActive;
        this.vulcanFiring = vulcanFiring;
        this.spindashState = spindashState;
        this.dashing = dashing;
        this.a1Cooldown = a1Cooldown;
        this.a2Cooldown = a2Cooldown;
        this.kineticSpeed = kineticSpeed;
    }

    public int getEntityId() { return entityId; }
    public boolean isJetActive() { return jetActive; }
    public boolean isVulcanFiring() { return vulcanFiring; }
    public int getSpindashState() { return spindashState; }
    public boolean isDashing() { return dashing; }
    public int getA1Cooldown() { return a1Cooldown; }
    public int getA2Cooldown() { return a2Cooldown; }
    public double getKineticSpeed() { return kineticSpeed; }

    public static void encode(MechaSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.jetActive);
        buf.writeBoolean(msg.vulcanFiring);
        buf.writeInt(msg.spindashState);
        buf.writeBoolean(msg.dashing);
        buf.writeInt(msg.a1Cooldown);
        buf.writeInt(msg.a2Cooldown);
        buf.writeDouble(msg.kineticSpeed);
    }

    public static MechaSyncPacket decode(FriendlyByteBuf buf) {
        return new MechaSyncPacket(
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readDouble()
        );
    }

    public static void handle(MechaSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMechaSync(msg));
        });
        ctx.setPacketHandled(true);
    }
}
