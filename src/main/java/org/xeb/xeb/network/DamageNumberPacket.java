package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.xeb.xeb.damagenumber.DamageNumberManager;

import java.util.function.Supplier;

/**
 * Paquete de red que sincroniza los eventos de daño desde el Servidor hacia el Cliente.
 */
public class DamageNumberPacket {
    private final int entityId;
    private final float amount;
    private final boolean isCrit;
    private final String sourceType;
    private final double posX;
    private final double posY;
    private final double posZ;

    public DamageNumberPacket(int entityId, float amount, boolean isCrit, String sourceType, double posX, double posY, double posZ) {
        this.entityId = entityId;
        this.amount = amount;
        this.isCrit = isCrit;
        this.sourceType = sourceType != null ? sourceType : "generic";
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public static void encode(DamageNumberPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeFloat(msg.amount);
        buf.writeBoolean(msg.isCrit);
        buf.writeUtf(msg.sourceType);
        buf.writeDouble(msg.posX);
        buf.writeDouble(msg.posY);
        buf.writeDouble(msg.posZ);
    }

    public static DamageNumberPacket decode(FriendlyByteBuf buf) {
        return new DamageNumberPacket(
                buf.readInt(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }

    public static void handle(DamageNumberPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                DamageNumberManager.getInstance().onDamageReceived(
                        msg.entityId,
                        msg.amount,
                        msg.isCrit,
                        msg.sourceType,
                        new Vec3(msg.posX, msg.posY, msg.posZ)
                );
            });
        });
        ctx.setPacketHandled(true);
    }
}
