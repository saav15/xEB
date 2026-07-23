package org.xeb.xeb.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.ModEntities;
import org.xeb.xeb.entity.StevenBossEntity;
import org.xeb.xeb.entity.StevenPortalEntity;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Xeb.MODID)
public class StevenSpawnHandler {
    private static int timerTicks = 0;
    private static final int THIRTY_MINUTES_TICKS = 36000;
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;

        timerTicks++;
        if (timerTicks >= THIRTY_MINUTES_TICKS) {
            timerTicks = 0;

            if (RANDOM.nextBoolean()) {
                spawnStevenStalker(serverLevel, null, null, 15, false);
            }
        }
    }

    public static void spawnStevenStalker(ServerLevel level, ServerPlayer targetPlayer, String forcedAttack, int durationSeconds, boolean isFullMode) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty() && targetPlayer == null) return;

        ServerPlayer chosen = targetPlayer != null ? targetPlayer : players.get(RANDOM.nextInt(players.size()));
        Vec3 spawnPos = chosen.position().add(
                (RANDOM.nextDouble() - 0.5D) * 6.0D,
                0.5D,
                (RANDOM.nextDouble() - 0.5D) * 6.0D
        );

        // Spawn Portal
        StevenPortalEntity portal = new StevenPortalEntity(ModEntities.STEVEN_PORTAL.get(), level);
        portal.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        level.addFreshEntity(portal);

        // Spawn Steven Boss Entity
        StevenBossEntity steven = new StevenBossEntity(ModEntities.STEVEN_BOSS.get(), level);
        steven.setPos(spawnPos.x, spawnPos.y + 0.2D, spawnPos.z);

        if (isFullMode) {
            steven.setFullMode(true);
        } else if (durationSeconds > 0) {
            steven.setCustomDurationSeconds(durationSeconds);
        }

        if (forcedAttack != null && !forcedAttack.isEmpty()) {
            steven.forceAttack(forcedAttack, chosen);
        }

        level.addFreshEntity(steven);
    }
}
