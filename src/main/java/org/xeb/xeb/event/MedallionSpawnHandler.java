package org.xeb.xeb.event;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.compat.ModCompatManager;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Handles medallion assignment and synchronisation during entity lifecycle events:
 *  - Assigning random medallions when a living entity first joins the level
 *  - Re-applying onAttach() callbacks when a saved entity re-joins
 *  - Adjusting entity hitbox size for Mega medallions (EntityEvent.Size)
 *  - Syncing medallion data to newly tracking players (StartTracking)
 *
 * Permanight logic → {@link PermanightLootHandler}
 * Advancement logic → {@link EliteAdvancementHandler}
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MedallionSpawnHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!org.xeb.xeb.Config.enabled) return;

        // Client side: apply pending sync data received before the entity was loaded
        if (event.getLevel().isClientSide()) {
            if (event.getEntity() instanceof LivingEntity living) {
                net.minecraft.nbt.ListTag pending =
                        org.xeb.xeb.network.ClientPacketHandler.getPendingSync(living.getId());
                if (pending != null) {
                    living.getPersistentData().put(MedallionManager.MEDALLIONS_KEY, pending);
                    try { living.refreshDimensions(); } catch (Exception ignored) {}
                }
            }
            return;
        }

        // Server side: assign or re-attach medallions for eligible non-player entities
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            if (ModCompatManager.isEligible(living)) {
                if (living.getPersistentData().contains(MedallionManager.MEDALLIONS_KEY)) {
                    // Entity already had medallions (loaded from NBT) — re-fire onAttach callbacks
                    List<MedallionData> medallions = MedallionManager.getMedallions(living);
                    for (MedallionData m : medallions) {
                        try { m.getBuff().onAttach(living, m.getUniqueId()); } catch (Exception ignored) {}
                    }
                    MedallionManager.refreshDimensionsIfNeeded(living, medallions);
                    MedallionManager.syncToTracking(living);
                } else {
                    // New entity — roll medallions
                    MedallionManager.assignRandomMedallions(living, (ServerLevel) event.getLevel());
                }
            }
        }
    }

    /**
     * Scales the bounding box of entities that carry a Mega medallion.
     * Called by Minecraft whenever an entity queries its dimensions.
     */
    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living instanceof Player) return;
        try {
            List<MedallionData> medallions = MedallionManager.getMedallions(living);
            if (!medallions.isEmpty()) {
                int megaCount = 0;
                for (MedallionData m : medallions) {
                    if (m.getBuff().getId().equals("mega")) megaCount++;
                }
                if (megaCount > 0) {
                    float scaleFactor = 1.0F + 0.30F * megaCount;
                    net.minecraft.world.entity.EntityDimensions original = event.getNewSize();
                    event.setNewSize(original.scale(scaleFactor));
                    event.setNewEyeHeight(event.getNewEyeHeight() * scaleFactor);
                }
            }
        } catch (Exception ignored) {
            // Guard against early initialization issues during entity construction
        }
    }

    /** Sends medallion data to a player the moment they start tracking an entity. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (event.getTarget() instanceof LivingEntity target) {
                MedallionManager.syncToPlayer(target, serverPlayer);
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Convenience delegation — kept for backwards compatibility with callers
    // (e.g. EvolvingBuff) that call MedallionSpawnHandler.grantAdvancement().
    // Internally delegates to EliteAdvancementHandler.
    // ---------------------------------------------------------------------------
    /** @deprecated Use {@link EliteAdvancementHandler#grantAdvancement} directly. */
    @Deprecated(forRemoval = false)
    public static void grantAdvancement(ServerPlayer player, String advancementId) {
        EliteAdvancementHandler.grantAdvancement(player, advancementId);
    }
}
