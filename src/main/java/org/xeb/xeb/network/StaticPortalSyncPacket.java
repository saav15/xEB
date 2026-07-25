package org.xeb.xeb.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.xeb.xeb.entity.StaticPortalEntity;

import java.util.function.Supplier;

public class StaticPortalSyncPacket {
    private final int entityId;
    private final int lifetime;
    private final int spawnInterval;
    private final int maxSpawns;
    private final int spawnsCount;
    private final int glitchTicks;
    private final String entityType;

    public StaticPortalSyncPacket(int entityId, int lifetime, int spawnInterval, int maxSpawns, int spawnsCount, int glitchTicks, String entityType) {
        this.entityId = entityId;
        this.lifetime = lifetime;
        this.spawnInterval = spawnInterval;
        this.maxSpawns = maxSpawns;
        this.spawnsCount = spawnsCount;
        this.glitchTicks = glitchTicks;
        this.entityType = entityType != null ? entityType : "xeb:steven_boss";
    }

    public static void encode(StaticPortalSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.lifetime);
        buf.writeInt(msg.spawnInterval);
        buf.writeInt(msg.maxSpawns);
        buf.writeInt(msg.spawnsCount);
        buf.writeInt(msg.glitchTicks);
        buf.writeUtf(msg.entityType);
    }

    public static StaticPortalSyncPacket decode(FriendlyByteBuf buf) {
        return new StaticPortalSyncPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf()
        );
    }

    public static void handle(StaticPortalSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    Entity entity = mc.level.getEntity(msg.entityId);
                    if (entity instanceof StaticPortalEntity portal) {
                        portal.setPortalLifetime(msg.lifetime);
                        portal.setSpawnInterval(msg.spawnInterval);
                        portal.setMaxSpawns(msg.maxSpawns);
                        portal.setSpawnsCount(msg.spawnsCount);
                        portal.setGlitchTicks(msg.glitchTicks);
                        portal.setPortalEntityType(msg.entityType);
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
