package org.xeb.xeb.event;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.compat.ModCompatManager;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MedallionSpawnHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!org.xeb.xeb.Config.enabled) return;

        if (event.getLevel().isClientSide()) {
            if (event.getEntity() instanceof LivingEntity living) {
                net.minecraft.nbt.ListTag pending = org.xeb.xeb.network.ClientPacketHandler.getPendingSync(living.getId());
                if (pending != null) {
                    living.getPersistentData().put(org.xeb.xeb.medallion.MedallionManager.MEDALLIONS_KEY, pending);
                    try {
                        living.refreshDimensions();
                    } catch (Exception e) {
                        LOGGER.warn("[xEB] Failed to refresh dimensions for entity {} during client sync", living.getId(), e);
                    }
                }
            }
            return;
        }
        
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            // Check if eligible
            if (ModCompatManager.isEligible(living)) {
                if (living.getPersistentData().contains(MedallionManager.MEDALLIONS_KEY)) {
                    java.util.List<org.xeb.xeb.medallion.MedallionData> medallions = MedallionManager.getMedallions(living);
                    for (org.xeb.xeb.medallion.MedallionData m : medallions) {
                        try {
                            m.getBuff().onAttach(living, m.getUniqueId());
                        } catch (Exception e) {
                            LOGGER.warn("[xEB] Failed to attach buff '{}' to entity {}", m.getBuff().getId(), living.getId(), e);
                        }
                    }
                    MedallionManager.refreshDimensionsIfNeeded(living, medallions);
                    MedallionManager.syncToTracking(living);
                } else {
                    MedallionManager.assignRandomMedallions(living, (ServerLevel) event.getLevel());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntitySize(net.minecraftforge.event.entity.EntityEvent.Size event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            try {
                java.util.List<org.xeb.xeb.medallion.MedallionData> medallions = MedallionManager.getMedallions(living);
                if (!medallions.isEmpty()) {
                    int megaCount = 0;
                    for (org.xeb.xeb.medallion.MedallionData m : medallions) {
                        if (m.getBuff().getId().equals("mega")) {
                            megaCount++;
                        }
                    }
                    if (megaCount > 0) {
                        float scaleFactor = 1.0F + 0.30F * megaCount;
                        net.minecraft.world.entity.EntityDimensions original = event.getNewSize();
                        event.setNewSize(original.scale(scaleFactor));
                        event.setNewEyeHeight(event.getNewEyeHeight() * scaleFactor);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Error during entity size calculation for {}: {}", living.getId(), e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (event.getTarget() instanceof LivingEntity target) {
                MedallionManager.syncToPlayer(target, serverPlayer);
            }
        }
    }
}
