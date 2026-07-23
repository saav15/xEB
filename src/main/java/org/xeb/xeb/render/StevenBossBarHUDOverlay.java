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

    private static final XebBossBar STEVEN_BOSS_BAR = new XebBossBar();

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
            // Cancelar el dibujado por defecto de la barra vainilla de Steven
            // Y calcular el desplazamiento necesario segun el numero total de Stevens activos
            int activeCount = getActiveStevensNearPlayer().size();
            int totalSpacing = Math.max(1, activeCount) * 63;

            event.setCanceled(true);
            event.setIncrement(totalSpacing);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        List<StevenBossEntity> stevens = getActiveStevensNearPlayer();
        if (stevens.isEmpty()) return;

        int currentY = 18;
        int index = 1;

        for (StevenBossEntity steven : stevens) {
            float targetHpRatio = steven.getHealth() / steven.getMaxHealth();
            int charges = steven.getStevenCharges();
            String phaseTag = targetHpRatio < 0.25F ? "OVERDRIVE" : "";

            String title = stevens.size() > 1 ? "S T E V E N  #" + index : "S T E V E N";

            // Delegar al componente reutilizable XebBossBar
            STEVEN_BOSS_BAR.render(
                    event.getGuiGraphics(),
                    mc.font,
                    currentY,
                    title,
                    targetHpRatio,
                    (int) steven.getHealth(),
                    (int) steven.getMaxHealth(),
                    charges,
                    phaseTag,
                    XebBossBar.Theme.OBSIDIAN_COSMIC
            );

            currentY += 63;
            index++;
        }
    }
}
