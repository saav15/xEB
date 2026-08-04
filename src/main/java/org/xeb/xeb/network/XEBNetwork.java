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
        CHANNEL.registerMessage(id++, OpenEnigmaBiosPacket.class, OpenEnigmaBiosPacket::encode, OpenEnigmaBiosPacket::decode, OpenEnigmaBiosPacket::handle);
        CHANNEL.registerMessage(id++, DamageNumberPacket.class, DamageNumberPacket::encode, DamageNumberPacket::decode, DamageNumberPacket::handle);
        CHANNEL.registerMessage(id++, StaticPortalSyncPacket.class, StaticPortalSyncPacket::encode, StaticPortalSyncPacket::decode, StaticPortalSyncPacket::handle);
        CHANNEL.registerMessage(id++, EliteMasteryLevelUpPacket.class, EliteMasteryLevelUpPacket::encode, EliteMasteryLevelUpPacket::decode, EliteMasteryLevelUpPacket::handle);
        CHANNEL.registerMessage(id++, MeteorStrikeSyncPacket.class, MeteorStrikeSyncPacket::encode, MeteorStrikeSyncPacket::decode, MeteorStrikeSyncPacket::handle);
        CHANNEL.registerMessage(id++, OpticBlastBurstSyncPacket.class, OpticBlastBurstSyncPacket::encode, OpticBlastBurstSyncPacket::decode, OpticBlastBurstSyncPacket::handle);
        CHANNEL.registerMessage(id++, MeteorStrikeMovePacket.class, MeteorStrikeMovePacket::encode, MeteorStrikeMovePacket::decode, MeteorStrikeMovePacket::handle);
        CHANNEL.registerMessage(id++, JudgementCutSyncPacket.class, JudgementCutSyncPacket::encode, JudgementCutSyncPacket::decode, JudgementCutSyncPacket::handle);
        CHANNEL.registerMessage(id++, SovereignSyncPacket.class, SovereignSyncPacket::encode, SovereignSyncPacket::decode, SovereignSyncPacket::handle);
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

    // ── Elite Mastery Level-Up Toast Packet ────────────────────────────────────
    /** Sent server→client when the player's Elite Mastery level increases. Triggers the visual toast. */
    public static class EliteMasteryLevelUpPacket {
        private final int newLevel;

        public EliteMasteryLevelUpPacket(int newLevel) {
            this.newLevel = newLevel;
        }

        public static void encode(EliteMasteryLevelUpPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeInt(msg.newLevel);
        }

        public static EliteMasteryLevelUpPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
            return new EliteMasteryLevelUpPacket(buf.readInt());
        }

        public static void handle(EliteMasteryLevelUpPacket msg, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctxSupplier) {
            net.minecraftforge.network.NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                    org.xeb.xeb.client.ClientAccess.showEliteMasteryLevelUpToast(msg.newLevel);
                });
            });
            ctx.setPacketHandled(true);
        }
    }
}
