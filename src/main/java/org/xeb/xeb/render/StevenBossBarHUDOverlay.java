package org.xeb.xeb.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.BossEvent;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.StevenBossEntity;

import java.util.List;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StevenBossBarHUDOverlay {

    public static final XebBossBar STEVEN_BOSS_BAR = new XebBossBar();

    private static List<StevenBossEntity> getActiveStevensNearPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return java.util.Collections.emptyList();
        AABB box = mc.player.getBoundingBox().inflate(96.0D);
        return mc.level.getEntitiesOfClass(StevenBossEntity.class, box, StevenBossEntity::isAlive);
    }

    @SubscribeEvent
    public static void onBossBarProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        BossEvent bossEvent = event.getBossEvent();
        if (bossEvent == null || bossEvent.getName() == null) return;
        
        // Remove all spaces for clean matching (e.g. "S T E V E N" -> "STEVEN")
        String cleanName = bossEvent.getName().getString().replace(" ", "").toUpperCase();

        if (cleanName.contains("STEVEN")) {
            int activeCount = getActiveStevensNearPlayer().size();
            int totalSpacing = Math.max(1, activeCount) * 63;

            event.setCanceled(true);
            event.setIncrement(totalSpacing);
        }
    }

    // Render loop is now handled cleanly by MedallionBossBarHUDOverlay for unified priority & mini-bars
}
