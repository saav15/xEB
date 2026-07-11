package org.xeb.xeb.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
 * Displays a tug-of-war bar on the right side of the screen when in a struggle.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BeamStruggleHUDOverlay {

    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 10;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x88000000;
    private static final int BLUE_TEAM_COLOR = 0xFF3333FF;
    private static final int RED_TEAM_COLOR = 0xFFFF3333;
    private static final int TUG_COLOR = 0xFFFFFF33;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        ClientStruggleData struggle = BeamStruggleRenderer.getLocalPlayerStruggle();
        if (struggle == null) return;

        Player player = mc.player;
        int localId = player.getId();
        boolean isPlayerA = (localId == struggle.ownerAEntityId);

        // Compute struggle progress ratio from player perspective
        Vec3 startMine = isPlayerA ? struggle.startA : struggle.startB;
        Vec3 startEnemy = isPlayerA ? struggle.startB : struggle.startA;

        double progress = 0.5D;
        Vec3 dirToEnemy = startEnemy.subtract(startMine);
        double totalDist = dirToEnemy.length();
        if (totalDist > 0.01D) {
            dirToEnemy = dirToEnemy.normalize();
            Vec3 playerToCollision = struggle.collisionPoint.subtract(startMine);
            double projectedDist = playerToCollision.dot(dirToEnemy);
            progress = projectedDist / totalDist;
        }
        float ratio = net.minecraft.util.Mth.clamp((float) progress, 0.0F, 1.0F);

        // GUI coordinates
        GuiGraphics g = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = screenW - BAR_WIDTH - 25;
        int y = screenH / 2 - BAR_HEIGHT / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. Draw Background
        g.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BG_COLOR);

        // 2. Draw Tug Areas (Left is blue/friendly, right is red/enemy)
        int centerIndicator = (int) (ratio * BAR_WIDTH);
        g.fill(x, y + 1, x + centerIndicator, y + BAR_HEIGHT - 1, BLUE_TEAM_COLOR);
        g.fill(x + centerIndicator, y + 1, x + BAR_WIDTH, y + BAR_HEIGHT - 1, RED_TEAM_COLOR);

        // 3. Draw Tug Indicator line
        g.fill(x + centerIndicator - 1, y, x + centerIndicator + 1, y + BAR_HEIGHT, TUG_COLOR);

        // 4. Draw Border
        g.fill(x, y, x + BAR_WIDTH, y + 1, BORDER_COLOR); // top
        g.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, BORDER_COLOR); // bottom
        g.fill(x, y, x + 1, y + BAR_HEIGHT, BORDER_COLOR); // left
        g.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, BORDER_COLOR); // right

        // 5. Draw Title Above Bar
        String titleStr = Component.translatable("hud.xeb.beam_struggle.title").getString();
        int titleW = mc.font.width(titleStr);
        g.drawString(mc.font, titleStr, x + BAR_WIDTH / 2 - titleW / 2, y - 14, 0xFFFFFFFF, true);

        // 6. Draw Pulsing/Popping "MASH B!" prompt below bar
        long now = System.currentTimeMillis();
        float pulse = 0.7F + 0.3F * (float) Math.sin(now * 0.015D);
        float popScale = 1.0F + 0.20F * BeamStruggleRenderer.getLocalMashProgress(localId);

        String keyName = ModKeyMappings.FLOURISH_KEY.getTranslatedKeyMessage().getString();
        String promptStr = Component.translatable("hud.xeb.beam_struggle.mash", keyName).getString();
        int promptW = mc.font.width(promptStr);

        PoseStack pose = g.pose();
        pose.pushPose();
        // Scale around prompt center
        float px = x + BAR_WIDTH / 2.0F;
        float py = y + BAR_HEIGHT + 14.0F;
        pose.translate(px, py, 0.0F);
        pose.scale(popScale, popScale, 1.0F);

        int alpha = (int) (255 * pulse);
        int color = (alpha << 24) | 0xFFFF00; // yellow flash
        g.drawString(mc.font, promptStr, -promptW / 2, -mc.font.lineHeight / 2, color, true);

        pose.popPose();
    }
}
