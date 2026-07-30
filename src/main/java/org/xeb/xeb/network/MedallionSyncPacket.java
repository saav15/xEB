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
    private final int madStacks;

    public MedallionSyncPacket(int entityId, List<String> buffIds, List<String> tiers, int madStacks) {
        this.entityId = entityId;
        this.buffIds = buffIds;
        this.tiers = tiers;
        this.madStacks = madStacks;
    }

    public MedallionSyncPacket(int entityId, List<String> buffIds, List<String> tiers) {
        this(entityId, buffIds, tiers, 0);
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

    public int getMadStacks() {
        return madStacks;
    }

    public static void encode(MedallionSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.madStacks);
        buf.writeInt(msg.buffIds.size());
        for (int i = 0; i < msg.buffIds.size(); i++) {
            buf.writeUtf(msg.buffIds.get(i));
            buf.writeUtf(msg.tiers.get(i));
        }
    }

    public static MedallionSyncPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int madStacks = buf.readInt();
        int size = buf.readInt();
        List<String> buffIds = new ArrayList<>();
        List<String> tiers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            buffIds.add(buf.readUtf());
            tiers.add(buf.readUtf());
        }
        return new MedallionSyncPacket(entityId, buffIds, tiers, madStacks);
    }

    public static void handle(MedallionSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMedallionSync(msg));
        });
        ctx.setPacketHandled(true);
    }

    public static void addPendingSync(int entityId, ListTag tag, int madStacks) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.addPendingSync(entityId, tag, madStacks));
    }

    public static void addPendingSync(int entityId, ListTag tag) {
        addPendingSync(entityId, tag, 0);
    }
}
