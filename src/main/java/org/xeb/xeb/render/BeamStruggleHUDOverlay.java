package org.xeb.xeb.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.client.ModKeyMappings;
import org.xeb.xeb.client.renderer.BeamStruggleRenderer;
import org.xeb.xeb.client.renderer.BeamStruggleRenderer.ClientStruggleData;

/**
 * HUD Overlay for Beam Struggle mini-game.
 * Displays a redesigned golden tug-of-war panel.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BeamStruggleHUDOverlay {

    private static final int GOLD_BRIGHT = 0xFFFFD700;
    private static final int GOLD_DARK = 0xFF8B6914;
    private static final int GOLD_LIGHT = 0xFFFFEC8B;
    private static final int RED_TEAM = 0xFFFF3030;
    private static final int BLUE_TEAM = 0xFF3030FF;
    private static final int SHADOW = 0xFF000000;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        ClientStruggleData struggle = BeamStruggleRenderer.getLocalPlayerStruggle();
        if (struggle == null) return;

        int localId = mc.player.getId();
        boolean isPrep = struggle.phase == 0; // 0 = PREP

        GuiGraphics g = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();

        if (isPrep) {
            renderPrepHUD(g, mc, screenW, screenH, now, struggle);
        } else {
            renderActiveHUD(g, mc, screenW, screenH, now, struggle, localId);
        }
    }

    private static void renderPrepHUD(GuiGraphics g, Minecraft mc, int screenW, int screenH,
                                       long now, ClientStruggleData struggle) {
        int centerX = screenW - 90;
        int centerY = screenH / 2;
        int panelW = 160, panelH = 80;
        int panelX = centerX - panelW / 2;
        int panelY = centerY - panelH / 2;

        float pulse = 0.7F + 0.3F * (float) Math.sin(now * 0.012D);

        // Gold frame
        drawGoldenFrame(g, panelX, panelY, panelW, panelH, pulse);

        // Prep Text
        String prepText = Component.translatable("hud.xeb.beam_struggle.prep").getString();
        int textW = mc.font.width(prepText);
        int textX = centerX - textW / 2;
        int textY = centerY - mc.font.lineHeight / 2;

        // Shadow and outlines
        g.drawString(mc.font, prepText, textX + 1, textY + 1, SHADOW, false);
        g.drawString(mc.font, prepText, textX - 1, textY, GOLD_DARK, false);
        g.drawString(mc.font, prepText, textX + 1, textY, GOLD_DARK, false);
        g.drawString(mc.font, prepText, textX, textY - 1, GOLD_DARK, false);
        g.drawString(mc.font, prepText, textX, textY + 1, GOLD_DARK, false);
        int alpha = (int) (255 * pulse);
        g.drawString(mc.font, prepText, textX, textY, (alpha << 24) | 0xFFFFFF, false);

        // Subtext
        String subText = Component.translatable("hud.xeb.beam_struggle.prep_sub").getString();
        int subW = mc.font.width(subText);
        g.drawString(mc.font, subText, centerX - subW / 2, textY + mc.font.lineHeight + 4, GOLD_LIGHT, true);

        // Progress bar
        int barW = panelW - 20;
        int barH = 4;
        int barX = panelX + 10;
        int barY = panelY + panelH - 12;
        float prepProgress = (float) struggle.ticksElapsed / 40.0F;
        int filledW = (int) (barW * Math.min(1.0F, prepProgress));
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        g.fill(barX, barY, barX + filledW, barY + barH, GOLD_BRIGHT);
    }

    private static void renderActiveHUD(GuiGraphics g, Minecraft mc, int screenW, int screenH,
                                         long now, ClientStruggleData struggle, int localId) {
        boolean isPlayerA = (localId == struggle.ownerAEntityId);
        int centerX = screenW / 2;
        int centerY = screenH / 2;

        // === 1. TIME BAR (top) ===
        int timeBarW = 120;
        int timeBarH = 3;
        int timeBarX = centerX - timeBarW / 2;
        int timeBarY = centerY - 70;
        float timeRatio = 1.0F - (struggle.ticksElapsed / 320.0F);
        int timeFilled = (int) (timeBarW * Math.max(0, timeRatio));
        g.fill(timeBarX, timeBarY, timeBarX + timeBarW, timeBarY + timeBarH, 0xFF333333);
        int timeColor = timeRatio > 0.5F ? 0xFFFFD700 : (timeRatio > 0.25F ? 0xFFFFA500 : 0xFFFF3030);
        g.fill(timeBarX, timeBarY, timeBarX + timeFilled, timeBarY + timeBarH, timeColor);

        // === 2. RHYTHM BAR (center) ===
        int rhythmBarW = 200;
        int rhythmBarH = 20;
        int rhythmBarX = centerX - rhythmBarW / 2;
        int rhythmBarY = centerY - 50;

        // Calcular posición del beat actual en la barra (0-14 → 0.0-1.0)
        float beatProgress = struggle.rhythmCycleTick / 15.0F;
        int beatX = rhythmBarX + (int) (rhythmBarW * beatProgress);

        // Zona PERFECT (verde) — 0-2 de 15 = 0% a 20%
        int perfectEnd = rhythmBarX + (int) (rhythmBarW * (3.0 / 15.0));
        g.fill(rhythmBarX, rhythmBarY, perfectEnd, rhythmBarY + rhythmBarH, 0xCC00FF00);

        // Zona GOOD (amarillo) — 3-5 de 15 = 20% a 40%
        int goodEnd = rhythmBarX + (int) (rhythmBarW * (6.0 / 15.0));
        g.fill(perfectEnd, rhythmBarY, goodEnd, rhythmBarY + rhythmBarH, 0xCCFFFF00);

        // Zona OK (naranja) — 6-11 de 15 = 40% a 80%
        int okEnd = rhythmBarX + (int) (rhythmBarW * (12.0 / 15.0));
        g.fill(goodEnd, rhythmBarY, okEnd, rhythmBarY + rhythmBarH, 0xCCFF8800);

        // Zona MISS (rojo) — 12-14 de 15 = 80% a 100%
        g.fill(okEnd, rhythmBarY, rhythmBarX + rhythmBarW, rhythmBarY + rhythmBarH, 0xCCFF0000);

        // Borde
        g.renderOutline(rhythmBarX - 1, rhythmBarY - 1, rhythmBarW + 2, rhythmBarH + 2, 0xFFFFFFFF);

        // Indicador del beat actual (línea vertical blanca que se mueve)
        g.fill(beatX - 1, rhythmBarY - 3, beatX + 2, rhythmBarY + rhythmBarH + 3, 0xFFFFFFFF);

        // Glow del indicador cuando está en zona PERFECT
        if (struggle.rhythmCycleTick < 3) {
            g.fill(beatX - 3, rhythmBarY - 5, beatX + 4, rhythmBarY + rhythmBarH + 5, 0x44FFFFFF);
        }

        // Texto "RHYTHM"
        g.drawCenteredString(mc.font, Component.translatable("hud.xeb.beam_struggle.rhythm"),
                centerX, rhythmBarY - 11, 0xFFFFFFFF);

        // === 3. TIMING FEEDBACK (debajo del rhythm bar) ===
        int feedbackY = rhythmBarY + rhythmBarH + 4;
        int myTiming = isPlayerA ? struggle.lastTimingA : struggle.lastTimingB;
        int myDisplayTicks = isPlayerA ? struggle.lastTimingDisplayTicksA : struggle.lastTimingDisplayTicksB;

        if (myDisplayTicks > 0 && myTiming >= 0) {
            String[] timingTexts = {"PERFECT!", "GOOD!", "OK", "MISS"};
            int[] timingColors = {0xFF00FF00, 0xFFFFFF00, 0xFFFF8800, 0xFFFF0000};
            String text = timingTexts[myTiming];
            int color = timingColors[myTiming];
            g.drawCenteredString(mc.font, text, centerX, feedbackY, color);
        }

        // === 4. TUG-OF-WAR BAR (bottom) ===
        int tugW = 160;
        int tugH = 12;
        int tugX = centerX - tugW / 2;
        int tugY = centerY + 30;

        float myPoints = isPlayerA ? struggle.pointsA : struggle.pointsB;
        float enemyPoints = isPlayerA ? struggle.pointsB : struggle.pointsA;
        float total = myPoints + enemyPoints;
        float myRatio = total > 0 ? myPoints / total : 0.5F;

        // Background
        g.fill(tugX, tugY, tugX + tugW, tugY + tugH, 0xFF1A1A1A);
        // Enemy half (right, red)
        int centerIndicator = (int) (tugW * myRatio);
        g.fill(tugX + centerIndicator, tugY + 1, tugX + tugW - 1, tugY + tugH - 1, 0xFFCC0000);
        // My half (left, blue/green)
        g.fill(tugX + 1, tugY + 1, tugX + centerIndicator, tugY + tugH - 1, 0xFF00AAFF);
        // Center divider
        g.fill(tugX + centerIndicator - 1, tugY, tugX + centerIndicator + 1, tugY + tugH, 0xFFFFFFFF);
        // Border
        g.renderOutline(tugX, tugY, tugW, tugH, 0xFF888888);

        // Points text
        g.drawString(mc.font, String.format("%.0f", myPoints), tugX + 4, tugY + tugH + 2, 0xFFAAFFFF, true);
        String enemyText = String.format("%.0f", enemyPoints);
        g.drawString(mc.font, enemyText, tugX + tugW - mc.font.width(enemyText) - 4, tugY + tugH + 2, 0xFFFFAAAA, true);

        // === 5. FLOURISH PROMPT ===
        String keyName = ModKeyMappings.FLOURISH_KEY.getTranslatedKeyMessage().getString();
        String promptStr = Component.translatable("hud.xeb.beam_struggle.tap", keyName).getString();
        g.drawCenteredString(mc.font, promptStr, centerX, tugY + tugH + 16, 0xFFCCCCCC);
    }

    private static void drawGoldenFrame(GuiGraphics g, int x, int y, int w, int h, float pulse) {
        int gold = (int) (255 * pulse) << 24 | 0xFFFFD700;
        int darkGold = 0xFF8B6914;

        g.fill(x - 2, y - 2, x + w + 2, y - 1, gold);
        g.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, gold);
        g.fill(x - 2, y - 2, x - 1, y + h + 2, gold);
        g.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, gold);

        g.fill(x, y, x + w, y + 1, darkGold);
        g.fill(x, y + h - 1, x + w, y + h, darkGold);
        g.fill(x, y, x + 1, y + h, darkGold);
        g.fill(x + w - 1, y, x + w, y + h, darkGold);

        int corner = 4;
        g.fill(x - 2, y - 2, x + corner, y - 1, GOLD_BRIGHT);
        g.fill(x + w - corner, y - 2, x + w + 2, y - 1, GOLD_BRIGHT);
        g.fill(x - 2, y + h + 1, x + corner, y + h + 2, GOLD_BRIGHT);
        g.fill(x + w - corner, y + h + 1, x + w + 2, y + h + 2, GOLD_BRIGHT);

        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x90000000);
    }
}
