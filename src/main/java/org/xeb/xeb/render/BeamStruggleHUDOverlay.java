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
        int centerX = screenW - 90;
        int centerY = screenH / 2;
        int panelW = 180, panelH = 110;
        int panelX = centerX - panelW / 2;
        int panelY = centerY - panelH / 2;

        boolean isPlayerA = (localId == struggle.ownerAEntityId);
        float myPoints = isPlayerA ? struggle.pointsA : struggle.pointsB;
        float enemyPoints = isPlayerA ? struggle.pointsB : struggle.pointsA;
        float total = myPoints + enemyPoints;
        float myRatio = total > 0 ? myPoints / total : 0.5F;

        drawGoldenFrame(g, panelX, panelY, panelW, panelH, 1.0F);

        // Title
        String title = Component.translatable("hud.xeb.beam_struggle.title").getString();
        int titleW = mc.font.width(title);
        g.drawString(mc.font, title, centerX - titleW / 2, panelY + 10, GOLD_BRIGHT, true);

        // Time bar
        int timeBarW = panelW - 20;
        int timeBarH = 3;
        int timeBarX = panelX + 10;
        int timeBarY = panelY + 22;
        float timeRatio = 1.0F - (struggle.ticksElapsed / 160.0F);
        int timeFilled = (int) (timeBarW * Math.max(0, timeRatio));
        g.fill(timeBarX, timeBarY, timeBarX + timeBarW, timeBarY + timeBarH, 0xFF333333);
        int timeColor = timeRatio > 0.5F ? GOLD_BRIGHT : (timeRatio > 0.25F ? 0xFFFFA500 : 0xFFFF3030);
        g.fill(timeBarX, timeBarY, timeBarX + timeFilled, timeBarY + timeBarH, timeColor);

        // Tug Bar
        int tugW = panelW - 30;
        int tugH = 18;
        int tugX = panelX + 15;
        int tugY = panelY + 38;

        g.fill(tugX, tugY, tugX + tugW, tugY + tugH, 0xFF1A1A1A);

        int centerIndicator = (int) (tugW * myRatio);
        g.fill(tugX + centerIndicator, tugY + 2, tugX + tugW - 2, tugY + tugH - 2, BLUE_TEAM);

        // Gradient own side
        for (int i = 0; i < centerIndicator; i++) {
            float t = (float) i / Math.max(1, tugW);
            int r = (int) (0xFF + (0xFF - 0xFF) * t);
            int gC = (int) (0x30 + (0xD7 - 0x30) * t);
            int b = (int) (0x30 + (0x00 - 0x30) * t);
            int color = 0xFF000000 | (r << 16) | (gC << 8) | b;
            g.fill(tugX + i, tugY + 2, tugX + i + 1, tugY + tugH - 2, color);
        }

        // Gold Diamond Center Indicator
        float pulse = 0.8F + 0.2F * (float) Math.sin(now * 0.02D);
        int indicatorX = tugX + centerIndicator;
        int indicatorY = tugY + tugH / 2;
        int dSize = (int) (4 * pulse);
        g.fill(indicatorX - dSize, indicatorY, indicatorX, indicatorY - dSize, GOLD_BRIGHT);
        g.fill(indicatorX, indicatorY - dSize, indicatorX + dSize, indicatorY, GOLD_BRIGHT);
        g.fill(indicatorX + dSize, indicatorY, indicatorX, indicatorY + dSize, GOLD_BRIGHT);
        g.fill(indicatorX, indicatorY + dSize, indicatorX - dSize, indicatorY, GOLD_BRIGHT);

        // Border of tug bar
        g.fill(tugX, tugY, tugX + tugW, tugY + 1, GOLD_DARK);
        g.fill(tugX, tugY + tugH - 1, tugX + tugW, tugY + tugH, GOLD_DARK);
        g.fill(tugX, tugY, tugX + 1, tugY + tugH, GOLD_DARK);
        g.fill(tugX + tugW - 1, tugY, tugX + tugW, tugY + tugH, GOLD_DARK);

        // Scores
        String myText = String.format("%.0f", myPoints);
        String enemyText = String.format("%.0f", enemyPoints);
        g.drawString(mc.font, myText, tugX + 4, tugY + tugH + 4, 0xFFFFAAAA, true);
        int enemyTextW = mc.font.width(enemyText);
        g.drawString(mc.font, enemyText, tugX + tugW - enemyTextW - 4, tugY + tugH + 4, 0xFFAAAAFF, true);

        // Mash Prompt
        float popScale = 1.0F + 0.25F * BeamStruggleRenderer.getLocalMashProgress(localId);
        String keyName = ModKeyMappings.FLOURISH_KEY.getTranslatedKeyMessage().getString();
        String promptStr = Component.translatable("hud.xeb.beam_struggle.mash", keyName).getString();
        int promptW = mc.font.width(promptStr);

        PoseStack pose = g.pose();
        pose.pushPose();
        float px = centerX;
        float py = panelY + panelH - 14;
        pose.translate(px, py, 0.0F);
        pose.scale(popScale, popScale, 1.0F);
        int alpha = (int) (255 * (0.7F + 0.3F * (float) Math.sin(now * 0.015D)));
        int promptColor = (alpha << 24) | 0xFFFFD700;
        g.drawString(mc.font, promptStr, -promptW / 2, -mc.font.lineHeight / 2, promptColor, true);
        pose.popPose();
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
