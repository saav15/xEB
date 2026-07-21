package org.xeb.xeb.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.Config;
import org.xeb.xeb.network.PermanightSyncPacket;
import org.xeb.xeb.network.XEBNetwork;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MoonTearVoteManager {
    private static boolean voteActive = false;
    private static UUID voteInitiatorUUID = null;
    private static String voteInitiatorName = "";
    private static final Set<UUID> yesVotes = new HashSet<>();
    private static long voteEndTime = 0;

    public static boolean isVoteActive() {
        return voteActive;
    }

    public static void startOrAddVote(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        int totalPlayers = players.size();

        // Singleplayer or 1 player online: Trigger immediately
        if (totalPlayers <= 1) {
            triggerPermanightWithShockwave(level, player);
            return;
        }

        long currentTime = level.getGameTime();

        // Check if vote expired
        if (voteActive && currentTime > voteEndTime) {
            voteActive = false;
            yesVotes.clear();
        }

        if (!voteActive) {
            voteActive = true;
            voteInitiatorUUID = player.getUUID();
            voteInitiatorName = player.getScoreboardName();
            yesVotes.clear();
            yesVotes.add(player.getUUID()); // Initiator automatically votes yes
            voteEndTime = currentTime + (Config.permanightVoteDurationSeconds * 20L);

            int thresholdPct = Config.permanightVoteThresholdPercent;
            int requiredVotes = Math.max(1, (int) Math.ceil((thresholdPct / 100.0D) * totalPlayers));

            broadcastVoteAnnouncement(level, player, requiredVotes, thresholdPct);

            if (yesVotes.size() >= requiredVotes) {
                triggerPermanightWithShockwave(level, player);
            }
        } else {
            // Register vote from player using Moon Tear or command
            if (!yesVotes.contains(player.getUUID())) {
                yesVotes.add(player.getUUID());
                
                int thresholdPct = Config.permanightVoteThresholdPercent;
                int requiredVotes = Math.max(1, (int) Math.ceil((thresholdPct / 100.0D) * totalPlayers));
                
                MutableComponent msg = Component.literal("§d[Moon Tear] §a" + player.getScoreboardName() + " §7ha votado §aSÍ§7. (Votos: §e" + yesVotes.size() + "/" + requiredVotes + "§7)");
                level.getServer().getPlayerList().broadcastSystemMessage(msg, false);

                if (yesVotes.size() >= requiredVotes) {
                    ServerPlayer initiator = level.getServer().getPlayerList().getPlayer(voteInitiatorUUID);
                    if (initiator == null) initiator = player;
                    triggerPermanightWithShockwave(level, initiator);
                }
            } else {
                player.sendSystemMessage(Component.literal("§cYa has votado a favor de la Eternal Permanight. Esperando a los demás jugadores..."));
            }
        }
    }

    private static void broadcastVoteAnnouncement(ServerLevel level, ServerPlayer initiator, int requiredVotes, int thresholdPct) {
        MutableComponent header = Component.literal("\n§d§l==========================================");
        MutableComponent title = Component.literal("\n§d§l[MOON TEAR] §e" + initiator.getScoreboardName() + " §7quiere invocar la §c§lETERNAL PERMANIGHT§7!");
        MutableComponent info = Component.literal("\n§7Se requieren §a" + requiredVotes + " votos §7(" + thresholdPct + "% de los jugadores).");
        
        MutableComponent voteButton = Component.literal("\n §a[¡HAZ CLIC AQUÍ PARA VOTAR SÍ!] ")
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/xeb vote yes"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Haz clic para votar SÍ a la Noche Eterna"))));
        
        MutableComponent footer = Component.literal("\n§d§l==========================================\n");

        MutableComponent fullMessage = header.append(title).append(info).append(voteButton).append(footer);
        level.getServer().getPlayerList().broadcastSystemMessage(fullMessage, false);
    }

    public static void triggerPermanightWithShockwave(ServerLevel level, ServerPlayer invoker) {
        voteActive = false;
        yesVotes.clear();

        PermanightSavedData data = PermanightSavedData.get(level);
        data.setActive(true);
        data.setTicksRemaining(24000); // 1 full MC day
        data.setForceNextNight(false);

        level.setDayTime(18000L);

        // --- SPECTRAL SHOCKWAVE EFFECT AROUND INVOKER ---
        Vec3 pos = invoker != null ? invoker.position() : new Vec3(0, 100, 0);
        double px = pos.x;
        double py = pos.y + 1.0D;
        double pz = pos.z;

        // Shockwave audio
        level.playSound(null, px, py, pz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.WEATHER, 3.0F, 0.6F);
        level.playSound(null, px, py, pz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0F, 0.5F);
        level.playSound(null, px, py, pz, SoundEvents.WITHER_SPAWN, SoundSource.WEATHER, 2.0F, 0.7F);

        // Particle expanding ring (Sonic boom + Soul fire flame + Dark attract)
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            double vx = Math.cos(rad) * 1.5D;
            double vz = Math.sin(rad) * 1.5D;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px + vx, py, pz + vz, 2, 0.1D, 0.2D, 0.1D, 0.1D);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, px + vx * 2.0D, py, pz + vz * 2.0D, 3, 0.2D, 0.5D, 0.2D, 0.05D);
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, px, py, pz, 3, 0.0D, 0.0D, 0.0D, 0.0D);

        // Announcement & Sync
        Component startMsg = Component.translatable("chat.xeb.permanight.start");
        level.getServer().getPlayerList().broadcastSystemMessage(startMsg, false);
        XEBNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new PermanightSyncPacket(true));
    }
}
