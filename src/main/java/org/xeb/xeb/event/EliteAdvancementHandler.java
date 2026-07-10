package org.xeb.xeb.event;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles all advancement-related logic for xEB:
 *  - Granting advancements when elite conditions are met
 *  - Elite Meter level-up notifications on advancement earn
 *  - "Elite Slayer" advancement on killing a 3+ medallion mob
 *
 * Extracted from MedallionSpawnHandler to separate advancement responsibility.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EliteAdvancementHandler {

    /**
     * Grants every criterion of the given advancement to the player.
     * Safe to call even if the advancement does not exist (no-op in that case).
     */
    public static void grantAdvancement(ServerPlayer player, String advancementId) {
        ResourceLocation id = new ResourceLocation(advancementId);
        Advancement adv = player.getServer().getAdvancements().getAdvancement(id);
        if (adv != null) {
            PlayerAdvancements advancements = player.getAdvancements();
            for (String criteria : adv.getCriteria().keySet()) {
                advancements.award(adv, criteria);
            }
        }
    }

    /** Notifies players when their Elite Meter level increases after earning an advancement. */
    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (!org.xeb.xeb.Config.enabled || !org.xeb.xeb.Config.eliteMeterEnabled) return;
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            int currentLevel = MedallionManager.getEliteMeterLevel(serverPlayer, false);
            net.minecraft.nbt.CompoundTag data = serverPlayer.getPersistentData();
            int lastNotifiedLevel = data.contains("xebLastNotifiedLevel") ? data.getInt("xebLastNotifiedLevel") : 1;

            if (currentLevel > lastNotifiedLevel) {
                data.putInt("xebLastNotifiedLevel", currentLevel);
                serverPlayer.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("chat.xeb.elitemeter.up", currentLevel));
            }
        }
    }

    /** Grants "Elite Slayer III" advancement when a player kills an entity with 3+ medallions. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            LivingEntity victim = event.getEntity();
            if (victim.getPersistentData().contains(MedallionManager.MEDALLIONS_KEY)) {
                java.util.List<org.xeb.xeb.medallion.MedallionData> meds = MedallionManager.getMedallions(victim);
                if (meds.size() >= 3) {
                    grantAdvancement(killer, "xeb:elite_slayer_3");
                }
            }
        }
    }
}
