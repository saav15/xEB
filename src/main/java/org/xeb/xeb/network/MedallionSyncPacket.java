package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MedallionSyncPacket {
    private final int entityId;
    private final List<String> buffIds;
    private final List<String> tiers;

    public MedallionSyncPacket(int entityId, List<String> buffIds, List<String> tiers) {
        this.entityId = entityId;
        this.buffIds = buffIds;
        this.tiers = tiers;
    }

    public int getEntityId() {
        return entityId;
    }

    public List<String> getBuffIds() {
        return buffIds;
    }

    public List<String> getTiers() {
        return tiers;
    }

    public static void encode(MedallionSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.buffIds.size());
        for (int i = 0; i < msg.buffIds.size(); i++) {
            buf.writeUtf(msg.buffIds.get(i));
            buf.writeUtf(msg.tiers.get(i));
        }
    }

    private static final int MAX_MEDALLIONS = 64;

    public static MedallionSyncPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int size = buf.readInt();
        if (size < 0 || size > MAX_MEDALLIONS) {
            size = 0;
        }
        List<String> buffIds = new ArrayList<>(size);
        List<String> tiers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            buffIds.add(buf.readUtf(256));
            tiers.add(buf.readUtf(256));
        }
        return new MedallionSyncPacket(entityId, buffIds, tiers);
    }

    public static void handle(MedallionSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMedallionSync(msg));
        });
        ctx.setPacketHandled(true);
    }

    public static void addPendingSync(int entityId, ListTag tag) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.addPendingSync(entityId, tag));
    }
}
