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
    }
}
