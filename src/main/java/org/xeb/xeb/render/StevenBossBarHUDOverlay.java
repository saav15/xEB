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

    @SubscribeEvent
    public static void onBossBarProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        BossEvent bossEvent = event.getBossEvent();
        if (bossEvent == null || bossEvent.getName() == null) return;
        String name = bossEvent.getName().getString().toUpperCase();

        if (name.contains("STEVEN")) {
            // Cancelar el dibujado por defecto de la barra vainilla
            event.setCanceled(true);
            event.setIncrement(0);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        AABB box = mc.player.getBoundingBox().inflate(96.0D);
        List<StevenBossEntity> list = mc.level.getEntitiesOfClass(StevenBossEntity.class, box);
        if (list.isEmpty()) return;

        StevenBossEntity steven = list.get(0);
        if (!steven.isAlive()) return;

        float targetHpRatio = steven.getHealth() / steven.getMaxHealth();
        int charges = steven.getStevenCharges();
        String extraStatus = charges > 0 ? "✦ " + charges + " CHARGES" : "";
        String phaseTag = targetHpRatio < 0.25F ? "OVERDRIVE" : "";

        // Delegar al componente reutilizable XebBossBar
        STEVEN_BOSS_BAR.render(
                event.getGuiGraphics(),
                mc.font,
                18,
                "S T E V E N",
                targetHpRatio,
                (int) steven.getHealth(),
                (int) steven.getMaxHealth(),
                extraStatus,
                phaseTag,
                XebBossBar.Theme.OBSIDIAN_COSMIC
        );
    }
}
