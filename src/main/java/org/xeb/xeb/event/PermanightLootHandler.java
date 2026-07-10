package org.xeb.xeb.event;

import org.xeb.xeb.Xeb;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles all Permanight loot and economy side-effects:
 *  - Triggering Permanight at nightfall and ending it after 24 000 ticks
 *  - Doubling XP drops during Permanight
 *  - 30 % bonus item drops during Permanight
 *  - Syncing Permanight state to players on login
 *
 * Extracted from MedallionSpawnHandler to separate Permanight responsibility.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PermanightLootHandler {

    /** Syncs Permanight state to a player the moment they log in. */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            org.xeb.xeb.world.PermanightSavedData data =
                    org.xeb.xeb.world.PermanightSavedData.get(serverPlayer.serverLevel());
            org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new org.xeb.xeb.network.PermanightSyncPacket(data.isActive()));
        }
    }

    /** Main Permanight tick: triggers at dusk, runs for 24 000 ticks, then ends. */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!org.xeb.xeb.Config.enabled || !org.xeb.xeb.Config.permanightEnabled) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;

        org.xeb.xeb.world.PermanightSavedData data =
                org.xeb.xeb.world.PermanightSavedData.get(serverLevel);

        if (data.isActive()) {
            int remaining = data.getTicksRemaining();
            if (remaining > 0) {
                data.setTicksRemaining(remaining - 1);
                serverLevel.setDayTime(18000L); // lock visual time to midnight
                // N11 — flush ticksRemaining to disk every 200 ticks (~10 s) to avoid
                // I/O spam while still surviving a crash with at most 10 s of lost progress.
                if (remaining % 200 == 0) {
                    data.markDirtyTick();
                }
            } else {
                // Permanight ends
                data.setActive(false);
                org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                        new org.xeb.xeb.network.PermanightSyncPacket(false));

                serverLevel.players().forEach(p -> {
                    p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.xeb.permanight.end"));
                    EliteAdvancementHandler.grantAdvancement(p, "xeb:survive_permanight");
                });
            }
        } else {
            // Check if we should trigger Permanight at dusk (dayTime == 13000)
            long dayTime = serverLevel.getDayTime() % 24000L;
            if (dayTime == 13000L) {
                boolean trigger = false;
                if (data.isForceNextNight()) {
                    trigger = true;
                    data.setForceNextNight(false);
                } else if (serverLevel.random.nextDouble() < org.xeb.xeb.Config.permanightChance) {
                    trigger = true;
                }

                if (trigger) {
                    data.setActive(true);
                    data.setTicksRemaining(24000);

                    // Thunder clap at first player's position (or origin if empty)
                    double px = serverLevel.players().isEmpty() ? 0 : serverLevel.players().get(0).getX();
                    double py = serverLevel.players().isEmpty() ? 0 : serverLevel.players().get(0).getY();
                    double pz = serverLevel.players().isEmpty() ? 0 : serverLevel.players().get(0).getZ();
                    serverLevel.playSound(null, px, py, pz,
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0F, 0.5F);

                    serverLevel.players().forEach(p ->
                            p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.xeb.permanight.start")));

                    org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                            new org.xeb.xeb.network.PermanightSyncPacket(true));
                }
            }
        }
    }

    /** Doubles XP drops during an active Permanight. */
    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!org.xeb.xeb.Config.enabled || !org.xeb.xeb.Config.permanightEnabled) return;
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            if (org.xeb.xeb.world.PermanightSavedData.get(serverLevel).isActive()) {
                event.setDroppedExperience(event.getDroppedExperience() * 2);
            }
        }
    }

    /** Adds up to 30 % bonus items per stack to drops during an active Permanight. */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!org.xeb.xeb.Config.enabled || !org.xeb.xeb.Config.permanightEnabled) return;
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            if (org.xeb.xeb.world.PermanightSavedData.get(serverLevel).isActive()) {
                for (net.minecraft.world.entity.item.ItemEntity itemEntity : event.getDrops()) {
                    net.minecraft.world.item.ItemStack stack = itemEntity.getItem();
                    int count = stack.getCount();
                    int extra = 0;
                    for (int k = 0; k < count; k++) {
                        if (serverLevel.random.nextDouble() < 0.30) extra++;
                    }
                    if (extra > 0) stack.setCount(count + extra);
                }
            }
        }
    }
}
