package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.Set;
import java.util.function.Supplier;

public class BuffParticlePacket {
    private static final int MAX_PARTICLE_COUNT = 128;
    private static final Set<String> VALID_PARTICLES = Set.of(
            "sonic_boom", "flame", "creepy", "dodge", "crit", "revival",
            "sandstorm", "evolve", "mega", "static", "tarred",
            "dodge_wave", "blind", "mana_leech", "marked", "doomed"
    );
    private final double x;
    private final double y;
    private final double z;
    private final String particleName;
    private final int count;

    public BuffParticlePacket(double x, double y, double z, String particleName, int count) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.particleName = particleName;
        this.count = count;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getParticleName() {
        return particleName;
    }

    public int getCount() {
        return count;
    }

    public static void encode(BuffParticlePacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeUtf(msg.particleName);
        buf.writeInt(msg.count);
    }

    public static BuffParticlePacket decode(FriendlyByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        String particleName = buf.readUtf(256);
        if (!VALID_PARTICLES.contains(particleName)) {
            particleName = "";
        }
        int count = buf.readInt();
        if (count < 0 || count > MAX_PARTICLE_COUNT) {
            count = 0;
        }
        return new BuffParticlePacket(x, y, z, particleName, count);
    }

    public static void handle(BuffParticlePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleBuffParticle(msg));
        });
        ctx.setPacketHandled(true);
    }
}
