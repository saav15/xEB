package org.xeb.xeb.network;

import org.xeb.xeb.Xeb;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class XEBNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Xeb.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, MedallionSyncPacket.class, MedallionSyncPacket::encode, MedallionSyncPacket::decode, MedallionSyncPacket::handle);
        CHANNEL.registerMessage(id++, BuffParticlePacket.class, BuffParticlePacket::encode, BuffParticlePacket::decode, BuffParticlePacket::handle);
        CHANNEL.registerMessage(id++, AirSwingPacket.class, AirSwingPacket::encode, AirSwingPacket::decode, AirSwingPacket::handle);
        CHANNEL.registerMessage(id++, DoomfistDashPacket.class, DoomfistDashPacket::encode, DoomfistDashPacket::decode, DoomfistDashPacket::handle);
        CHANNEL.registerMessage(id++, ActuarKeyPacket.class, ActuarKeyPacket::encode, ActuarKeyPacket::decode, ActuarKeyPacket::handle);
        CHANNEL.registerMessage(id++, DoomfistAbilitySyncPacket.class, DoomfistAbilitySyncPacket::encode, DoomfistAbilitySyncPacket::decode, DoomfistAbilitySyncPacket::handle);
        CHANNEL.registerMessage(id++, DoomfistUltraChargeSyncPacket.class, DoomfistUltraChargeSyncPacket::encode, DoomfistUltraChargeSyncPacket::decode, DoomfistUltraChargeSyncPacket::handle);
        CHANNEL.registerMessage(id++, DoomfistPowerBlockSyncPacket.class, DoomfistPowerBlockSyncPacket::encode, DoomfistPowerBlockSyncPacket::decode, DoomfistPowerBlockSyncPacket::handle);
        CHANNEL.registerMessage(id++, PermanightSyncPacket.class, PermanightSyncPacket::encode, PermanightSyncPacket::decode, PermanightSyncPacket::handle);
        CHANNEL.registerMessage(id++, OpticBlastBeamPacket.class, OpticBlastBeamPacket::encode, OpticBlastBeamPacket::decode, OpticBlastBeamPacket::handle);
        CHANNEL.registerMessage(id++, OpticBlastEnergySyncPacket.class, OpticBlastEnergySyncPacket::encode, OpticBlastEnergySyncPacket::decode, OpticBlastEnergySyncPacket::handle);
        CHANNEL.registerMessage(id++, OpticBlastChainBeamPacket.class, OpticBlastChainBeamPacket::encode, OpticBlastChainBeamPacket::decode, OpticBlastChainBeamPacket::handle);
        CHANNEL.registerMessage(id++, GoldenFlowerSyncPacket.class, GoldenFlowerSyncPacket::encode, GoldenFlowerSyncPacket::decode, GoldenFlowerSyncPacket::handle);
        CHANNEL.registerMessage(id++, GoldenFlowerDanceStartPacket.class, GoldenFlowerDanceStartPacket::encode, GoldenFlowerDanceStartPacket::decode, GoldenFlowerDanceStartPacket::handle);
        CHANNEL.registerMessage(id++, GoldenFlowerDanceStrikePacket.class, GoldenFlowerDanceStrikePacket::encode, GoldenFlowerDanceStrikePacket::decode, GoldenFlowerDanceStrikePacket::handle);
        CHANNEL.registerMessage(id++, JaronaDashPacket.class, JaronaDashPacket::encode, JaronaDashPacket::decode, JaronaDashPacket::handle);
        CHANNEL.registerMessage(id++, GoldenFlowerDanceReturnPacket.class, GoldenFlowerDanceReturnPacket::encode, GoldenFlowerDanceReturnPacket::decode, GoldenFlowerDanceReturnPacket::handle);
        CHANNEL.registerMessage(id++, CrazyDiamondAttackPacket.class, CrazyDiamondAttackPacket::encode, CrazyDiamondAttackPacket::decode, CrazyDiamondAttackPacket::handle);
        CHANNEL.registerMessage(id++, CrazyDiamondSyncPacket.class, CrazyDiamondSyncPacket::encode, CrazyDiamondSyncPacket::decode, CrazyDiamondSyncPacket::handle);
        CHANNEL.registerMessage(id++, BrimstoneBeamPacket.class, BrimstoneBeamPacket::encode, BrimstoneBeamPacket::decode, BrimstoneBeamPacket::handle);
        CHANNEL.registerMessage(id++, TearsLeftClickPacket.class, TearsLeftClickPacket::encode, TearsLeftClickPacket::decode, TearsLeftClickPacket::handle);
        CHANNEL.registerMessage(id++, TearsSyncPacket.class, TearsSyncPacket::encode, TearsSyncPacket::decode, TearsSyncPacket::handle);
        CHANNEL.registerMessage(id++, EliteMasterySyncPacket.class, EliteMasterySyncPacket::encode, EliteMasterySyncPacket::decode, EliteMasterySyncPacket::handle);
        CHANNEL.registerMessage(id++, BeamStrugglePacket.class, BeamStrugglePacket::encode, BeamStrugglePacket::decode, BeamStrugglePacket::handle);
        CHANNEL.registerMessage(id++, BeamStruggleEndPacket.class, BeamStruggleEndPacket::encode, BeamStruggleEndPacket::decode, BeamStruggleEndPacket::handle);
        CHANNEL.registerMessage(id++, MechaSyncPacket.class, MechaSyncPacket::encode, MechaSyncPacket::decode, MechaSyncPacket::handle);
        CHANNEL.registerMessage(id++, HolySyncPacket.class, HolySyncPacket::encode, HolySyncPacket::decode, HolySyncPacket::handle);
        CHANNEL.registerMessage(id++, OmegaFlowerySyncPacket.class, OmegaFlowerySyncPacket::encode, OmegaFlowerySyncPacket::decode, OmegaFlowerySyncPacket::handle);
        CHANNEL.registerMessage(id++, EnigmaBiosSyncPacket.class, EnigmaBiosSyncPacket::encode, EnigmaBiosSyncPacket::decode, EnigmaBiosSyncPacket::handle);
        CHANNEL.registerMessage(id++, HalberdSpikeSyncPacket.class, HalberdSpikeSyncPacket::encode, HalberdSpikeSyncPacket::decode, HalberdSpikeSyncPacket::handle);
        CHANNEL.registerMessage(id++, StevenLaserSyncPacket.class, StevenLaserSyncPacket::encode, StevenLaserSyncPacket::decode, StevenLaserSyncPacket::handle);
        CHANNEL.registerMessage(id++, FlourishPacket.class, FlourishPacket::encode, FlourishPacket::decode, FlourishPacket::handle);
    }

    public static class EliteMasterySyncPacket {
        private final int baseLevel;

        public EliteMasterySyncPacket(int baseLevel) {
            this.baseLevel = baseLevel;
        }

        public static void encode(EliteMasterySyncPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeInt(msg.baseLevel);
        }

        public static EliteMasterySyncPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
            return new EliteMasterySyncPacket(buf.readInt());
        }

        public static void handle(EliteMasterySyncPacket msg, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctxSupplier) {
            net.minecraftforge.network.NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.getPersistentData().putInt("xebEliteMeterLevel", msg.baseLevel);
                    }
                });
            });
            ctx.setPacketHandled(true);
        }
    }
}
