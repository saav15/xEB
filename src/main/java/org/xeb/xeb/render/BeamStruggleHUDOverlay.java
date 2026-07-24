package org.xeb.xeb.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.client.ModKeyMappings;
import org.xeb.xeb.client.renderer.BeamStruggleRenderer;
import org.xeb.xeb.client.renderer.BeamStruggleRenderer.ClientStruggleData;

/**
 * HUD Overlay estilizado de alto impacto visual para el minijuego Beam Struggle.
 * Oculta la Hotbar, la mano/ítem sostenido y el Crosshair durante el duelo para una experiencia cinemática limpia.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BeamStruggleHUDOverlay {

    private static final int GOLD_BRIGHT = 0xFFFFD700;
    private static final int GOLD_DARK = 0xFF8B6914;
    private static final int GOLD_LIGHT = 0xFFFFEC8B;
    private static final int CYAN_GLOW = 0xFF00E5FF;
    private static final int RED_GLOW = 0xFFFF2A2A;

    /**
     * Cancela la mano y el ítem sostenido en primera persona durante el choque.
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ClientStruggleData struggle = BeamStruggleRenderer.getLocalPlayerStruggle();
        if (struggle != null && struggle.phase == 1) { // 1 = ACTIVE
            event.setCanceled(true);
        }
    }

    /**
     * Renderiza el HUD de BeamStruggle y cancela los elementos Vanilla no deseados (Hotbar, Crosshair, etc.).
     */
    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        ClientStruggleData struggle = BeamStruggleRenderer.getLocalPlayerStruggle();
        if (struggle == null) return;

        ResourceLocation id = event.getOverlay().id();

        // Ocultar Hotbar, Crosshair, Nombre de Item e Indicador de Casco durante el duelo activo
        if (struggle.phase == 1) {
            if (id.equals(VanillaGuiOverlay.CROSSHAIR.id()) ||
                id.equals(VanillaGuiOverlay.ITEM_NAME.id()) ||
                id.equals(VanillaGuiOverlay.HELMET.id())) {
                event.setCanceled(true);
            }
        }

        // RENDERIZAR EL HUD DEL BEAM STRUGGLE EN EL OVERLAY DE LA HOTBAR (Garantiza ejecución 100% garantizada en Pre)
        if (id.equals(VanillaGuiOverlay.HOTBAR.id())) {
            int localId = mc.player.getId();
            boolean isPrep = struggle.phase == 0;

            GuiGraphics g = event.getGuiGraphics();
            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();
            long now = System.currentTimeMillis();

            if (isPrep) {
                renderPrepHUD(g, mc, screenW, screenH, now, struggle);
            } else {
                renderActiveHUD(g, mc, screenW, screenH, now, struggle, localId);
            }

            // Cancelar la hotbar vanilla sólo si estamos en fase de duelo activo
            if (struggle.phase == 1) {
                event.setCanceled(true);
            }
        }
    }

    private static void renderPrepHUD(GuiGraphics g, Minecraft mc, int screenW, int screenH,
                                       long now, ClientStruggleData struggle) {
        int centerX = screenW / 2;
        int centerY = screenH / 3;
        int panelW = 200, panelH = 75;
        int panelX = centerX - panelW / 2;
        int panelY = centerY - panelH / 2;

        float pulse = 0.7F + 0.3F * (float) Math.sin(now * 0.012D);

        // Marco Sci-Fi de preparación
        drawSciFiPanel(g, panelX, panelY, panelW, panelH, 0xEE0B1220, GOLD_BRIGHT);

        // Prep Text
        String prepText = Component.translatable("hud.xeb.beam_struggle.prep").getString();
        int textW = mc.font.width(prepText);
        g.drawString(mc.font, prepText, centerX - textW / 2, panelY + 12, GOLD_BRIGHT, true);

        // Subtext
        String subText = Component.translatable("hud.xeb.beam_struggle.prep_sub").getString();
        int subW = mc.font.width(subText);
        g.drawString(mc.font, subText, centerX - subW / 2, panelY + 28, GOLD_LIGHT, true);

        // Barra de progreso de preparación
        int barW = panelW - 30;
        int barH = 6;
        int barX = panelX + 15;
        int barY = panelY + panelH - 18;
        float prepProgress = (float) struggle.ticksElapsed / 40.0F;
        int filledW = (int) (barW * Math.min(1.0F, prepProgress));

        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF050B14);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF182232);
        g.fill(barX, barY, barX + filledW, barY + barH, GOLD_BRIGHT);
        g.fill(barX, barY, barX + filledW, barY + 2, 0xFFFFFFFF);
    }

    private static void renderActiveHUD(GuiGraphics g, Minecraft mc, int screenW, int screenH,
                                         long now, ClientStruggleData struggle, int localId) {
        boolean isPlayerA = (localId == struggle.ownerAEntityId);
        int centerX = screenW / 2;

        // === 1. PANEL SUPERIOR DE TIEMPO Y CABECERA (TOP HEADER) ===
        int headerW = 240;
        int headerH = 26;
        int headerX = centerX - headerW / 2;
        int headerY = 12;

        drawSciFiPanel(g, headerX, headerY, headerW, headerH, 0xDD080E1A, CYAN_GLOW);

        // Título del duelo
        String titleText = "★ BEAM STRUGGLE ★";
        g.drawCenteredString(mc.font, titleText, centerX, headerY + 4, GOLD_BRIGHT);

        // Barra de tiempo restante en la base del header
        float timeRatio = Math.max(0.0F, 1.0F - (struggle.ticksElapsed / 320.0F));
        int timeBarW = headerW - 16;
        int timeBarH = 4;
        int timeBarX = headerX + 8;
        int timeBarY = headerY + headerH - 8;
        int timeFilled = (int) (timeBarW * timeRatio);
        int timeColor = timeRatio > 0.5F ? CYAN_GLOW : (timeRatio > 0.25F ? GOLD_BRIGHT : RED_GLOW);

        g.fill(timeBarX, timeBarY, timeBarX + timeBarW, timeBarY + timeBarH, 0xFF101B2B);
        g.fill(timeBarX, timeBarY, timeBarX + timeFilled, timeBarY + timeBarH, timeColor);
        g.fill(timeBarX, timeBarY, timeBarX + timeFilled, timeBarY + 1, 0xFFFFFFFF);

        // === 2. MEDIDOR DE RITMO Y BEAT PULSE (CENTRO SUPERIOR) ===
        int rhythmW = 220;
        int rhythmH = 34;
        int rhythmX = centerX - rhythmW / 2;
        int rhythmY = screenH / 2 - 75;

        drawSciFiPanel(g, rhythmX, rhythmY, rhythmW, rhythmH, 0xEE091120, 0xFF4A6FA5);

        // Zonas de ritmo coloreadas
        int innerX = rhythmX + 6;
        int innerW = rhythmW - 12;
        int innerY = rhythmY + 12;
        int innerH = 14;

        // Zona PERFECT (Verde neón esmeralda)
        int perfectW = (int) (innerW * (3.0 / 15.0));
        g.fill(innerX, innerY, innerX + perfectW, innerY + innerH, 0xFF00E676);

        // Zona GOOD (Amarillo neón)
        int goodW = (int) (innerW * (3.0 / 15.0));
        g.fill(innerX + perfectW, innerY, innerX + perfectW + goodW, innerY + innerH, 0xFFFFEA00);

        // Zona OK (Naranja neón)
        int okW = (int) (innerW * (6.0 / 15.0));
        g.fill(innerX + perfectW + goodW, innerY, innerX + perfectW + goodW + okW, innerY + innerH, 0xFFFF9100);

        // Zona MISS (Rojo intenso)
        int missX = innerX + perfectW + goodW + okW;
        g.fill(missX, innerY, innerX + innerW, innerY + innerH, 0xFFFF1744);

        // Aguja indicadora móvil del beat
        float beatProgress = struggle.rhythmCycleTick / 15.0F;
        int needleX = innerX + (int) (innerW * beatProgress);

        g.fill(needleX - 2, innerY - 3, needleX + 2, innerY + innerH + 3, 0xFFFFFFFF);
        g.fill(needleX - 1, innerY - 2, needleX + 1, innerY + innerH + 2, CYAN_GLOW);

        if (struggle.rhythmCycleTick < 3) {
            // Destello de zona perfecta
            g.fill(needleX - 4, innerY - 5, needleX + 4, innerY + innerH + 5, 0x6600E676);
        }

        // Etiqueta RHYTHM PULSE
        g.drawString(mc.font, "RHYTHM PULSE", rhythmX + 8, rhythmY + 3, 0xFFB0BEC5, false);

        // Feedback de tiempo (PERFECT! / GREAT! / OK / MISS)
        int myTiming = isPlayerA ? struggle.lastTimingA : struggle.lastTimingB;
        int myDisplayTicks = isPlayerA ? struggle.lastTimingDisplayTicksA : struggle.lastTimingDisplayTicksB;

        if (myDisplayTicks > 0 && myTiming >= 0) {
            String[] timingTexts = {"PERFECT!!", "GREAT!", "OK", "MISS"};
            int[] timingColors = {0xFF00E676, 0xFFFFEA00, 0xFFFF9100, 0xFFFF1744};
            String text = timingTexts[myTiming];
            int color = timingColors[myTiming];
            int textW = mc.font.width(text);
            g.drawString(mc.font, text, rhythmX + rhythmW - textW - 8, rhythmY + 3, color, true);
        }

        // === 3. BARRA DE FUERZA DE CHOQUE (TUG-OF-WAR POWER GAUGE) ===
        int tugW = 260;
        int tugH = 22;
        int tugX = centerX - tugW / 2;
        int tugY = screenH - 65;

        drawSciFiPanel(g, tugX - 4, tugY - 4, tugW + 8, tugH + 8, 0xEE070D18, GOLD_BRIGHT);

        float myPoints = isPlayerA ? struggle.pointsA : struggle.pointsB;
        float enemyPoints = isPlayerA ? struggle.pointsB : struggle.pointsA;
        float total = myPoints + enemyPoints;
        float myRatio = total > 0 ? myPoints / total : 0.5F;

        int gaugeX = tugX;
        int gaugeY = tugY;
        int gaugeW = tugW;
        int gaugeH = tugH;

        int centerSplit = (int) (gaugeW * myRatio);

        // Lado propio (Azul/Cian neón)
        g.fill(gaugeX, gaugeY, gaugeX + centerSplit, gaugeY + gaugeH, 0xFF0091EA);
        g.fill(gaugeX, gaugeY, gaugeX + centerSplit, gaugeY + 3, 0xFF40C4FF);

        // Lado enemigo (Rojo neón)
        g.fill(gaugeX + centerSplit, gaugeY, gaugeX + gaugeW, gaugeY + gaugeH, 0xFFD50000);
        g.fill(gaugeX + centerSplit, gaugeY, gaugeX + gaugeW, gaugeY + 3, 0xFFFF5252);

        // Línea divisora central brillante de choque
        g.fill(gaugeX + centerSplit - 2, gaugeY - 2, gaugeX + centerSplit + 2, gaugeY + gaugeH + 2, 0xFFFFFFFF);
        g.fill(gaugeX + centerSplit - 4, gaugeY - 4, gaugeX + centerSplit + 4, gaugeY + gaugeH + 4, 0x55FFFFFF);

        // Texto de puntuación
        String myScore = String.format("YOU: %.0f", myPoints);
        String enemyScore = String.format("ENEMY: %.0f", enemyPoints);

        g.drawString(mc.font, myScore, gaugeX + 6, gaugeY + 7, 0xFFFFFFFF, true);
        int enemyW = mc.font.width(enemyScore);
        g.drawString(mc.font, enemyScore, gaugeX + gaugeW - enemyW - 6, gaugeY + 7, 0xFFFFFFFF, true);

        // === 4. PROMPT DE TECLA (MASH PROMPT) ===
        String keyName = ModKeyMappings.FLOURISH_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        String promptStr = "▶ MASH [" + keyName + "] TO OVERPOWER ◀";
        int promptW = mc.font.width(promptStr);

        float keyPulse = 0.8F + 0.2F * (float) Math.sin(now * 0.02D);
        int promptColor = (int) (255 * keyPulse) << 24 | (GOLD_BRIGHT & 0x00FFFFFF);

        g.drawString(mc.font, promptStr, centerX - promptW / 2, tugY + tugH + 10, promptColor, true);
    }

    private static void drawSciFiPanel(GuiGraphics g, int x, int y, int w, int h, int bgColor, int borderColor) {
        g.fill(x, y, x + w, y + h, bgColor);

        g.fill(x, y, x + w, y + 1, borderColor);
        g.fill(x, y + h - 1, x + w, y + h, borderColor);
        g.fill(x, y, x + 1, y + h, borderColor);
        g.fill(x + w - 1, y, x + w, y + h, borderColor);

        int corner = 6;
        g.fill(x, y, x + corner, y + 2, CYAN_GLOW);
        g.fill(x + w - corner, y, x + w, y + 2, CYAN_GLOW);
        g.fill(x, y + h - 2, x + corner, y + h, CYAN_GLOW);
        g.fill(x + w - corner, y + h - 2, x + w, y + h, CYAN_GLOW);
    }
}
