package org.xeb.xeb.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.medallion.MedallionType;
import org.xeb.xeb.render.MedallionRenderLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Toast shown when the player's Elite Mastery level increases.
 *
 * Visual design identical to XebCompletionToast:
 *   - Quartic ease-out slide-in
 *   - Clockwise progress border with tier accent colour
 *   - Text scramble + binary bit-rain on exit
 *
 * Icon: the real 3D animated medallion model (MedallionRenderLayer.renderSingleMedallionGUI)
 * using a random buff ID from the registry, matching the reached tier.
 *
 * Tier colours:
 *   Bronze  (levels 1-3)  -> 0xCD7F32
 *   Silver  (levels 4-6)  -> 0xC0C0C0
 *   Gold    (levels 7-10) -> 0xFFD700
 */
public class EliteMasteryToast implements Toast {

    private final Component title;
    private final Component subtitle;
    private final int       accentColor;
    private final int       width;
    private final MedallionType tier;
    private final String        buffId;

    public EliteMasteryToast(Component title, Component subtitle,
                             int accentColor, MedallionType tier, String buffId) {
        this.title       = title;
        this.subtitle    = subtitle;
        this.accentColor = accentColor;
        this.tier        = tier;
        this.buffId      = buffId;
        int tw = Minecraft.getInstance().font.width(title);
        int sw = Minecraft.getInstance().font.width(subtitle);
        this.width = Math.max(160, 34 + Math.max(tw, sw) + 8);
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static void show(int newLevel) {
        MedallionType tier = tierForLevel(newLevel);
        String        bid  = randomBuffId();
        Component title    = Component.translatable("gui.xeb.mastery.toast.title");
        Component subtitle = Component.translatable("gui.xeb.mastery.toast.subtitle", newLevel);
        Minecraft.getInstance().getToasts().addToast(
                new EliteMasteryToast(title, subtitle, tier.getColor(), tier, bid));
    }

    // -------------------------------------------------------------------------
    // Toast interface
    // -------------------------------------------------------------------------

    @Override
    public int width() { return this.width; }

    @Override
    public Toast.Visibility render(GuiGraphics gui, ToastComponent tc, long startTime) {

        float   f            = 1.0F;
        boolean decoding     = false;
        float   decodeProgress = 0.0F;

        if (startTime < 1000L) {
            float t = (float) startTime / 1000.0F;
            // Quartic ease-out slide-in
            f = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t) * (1.0F - t);
        } else if (startTime > 4000L) {
            float exitT    = Mth.clamp((float)(startTime - 4000L) / 1000.0F, 0.0F, 1.0F);
            f              = 1.0F - exitT * exitT * exitT;
            decoding       = true;
            decodeProgress = exitT;
        }
        if (startTime >= 5000L) { f = 0.0F; decoding = true; decodeProgress = 1.0F; }

        // Slide translation
        float currentX = gui.pose().last().pose().m30();
        float targetX  = -this.width + this.width * f;
        gui.pose().pushPose();
        gui.pose().translate(-currentX + targetX, 0.0F, 0.0F);

        // Background
        int bgAlpha     = decoding ? (int)(0xCE * (1.0F - decodeProgress)) : 0xCE;
        int borderAlpha = decoding ? (int)(0xFF * (1.0F - decodeProgress)) : 0xFF;
        gui.fill(0, 0, this.width, 32, (bgAlpha << 24) | 0x0A0A0A);

        // Clockwise progress border
        double progress  = Math.min(1.0, Math.max(0.0, (double) startTime / 5000.0));
        double perimeter = 2.0 * this.width + 64.0;
        double d         = progress * perimeter;
        int    borderLit   = (borderAlpha << 24) | (accentColor & 0x00FFFFFF);
        int    borderUnlit = (borderAlpha << 24) | 0x1A1A1A;

        // Top
        int topLit = (int) Math.min(this.width, d);
        gui.fill(0, 0, topLit, 1, borderLit);
        gui.fill(topLit, 0, this.width, 1, borderUnlit);
        // Right
        int rightLit = (int) Math.min(32, Math.max(0, d - this.width));
        gui.fill(this.width - 1, 0, this.width, rightLit, borderLit);
        gui.fill(this.width - 1, rightLit, this.width, 32, borderUnlit);
        // Bottom
        int bottomLit = (int) Math.min(this.width, Math.max(0, d - (this.width + 32)));
        gui.fill(this.width - bottomLit, 31, this.width, 32, borderLit);
        gui.fill(0, 31, this.width - bottomLit, 32, borderUnlit);
        // Left
        int leftLit = (int) Math.min(32, Math.max(0, d - (2 * this.width + 32)));
        gui.fill(0, 32 - leftLit, 1, 32, borderLit);
        gui.fill(0, 0, 1, 32 - leftLit, borderUnlit);

        // 3D animated medallion icon (left 32x32 area)
        float rotAngle = (System.currentTimeMillis() % 3600L) / 10.0F;
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(16, 16, 150.0F);
        pose.scale(1.6F, 1.6F, 1.6F);
        MultiBufferSource.BufferSource bufSource =
                Minecraft.getInstance().renderBuffers().bufferSource();
        MedallionRenderLayer.renderSingleMedallionGUI(pose, bufSource, tier, buffId, rotAngle, 0xF000F0);
        bufSource.endBatch();
        pose.popPose();

        // Text (scramble on exit)
        String rt = decoding ? scrambleText(title.getString(),    decodeProgress) : title.getString();
        String rs = decoding ? scrambleText(subtitle.getString(), decodeProgress) : subtitle.getString();
        int alpha      = decoding ? (int)(0xFF * (1.0F - decodeProgress)) : 0xFF;
        int titleColor = (alpha << 24) | (accentColor & 0x00FFFFFF);
        int subColor   = (alpha << 24) | 0xFFFFFF;
        gui.drawString(Minecraft.getInstance().font, rt, 34,  7, titleColor, false);
        gui.drawString(Minecraft.getInstance().font, rs, 34, 18, subColor,   false);

        // Binary bit-rain overlay on exit
        if (decoding) {
            java.util.Random r = new java.util.Random(987654321L);
            for (int i = 0; i < 20; i++) {
                int bx = r.nextInt(this.width - 14) + 14;
                int by = r.nextInt(24);
                if (r.nextFloat() < decodeProgress) {
                    int ba = (int)((1.0F - decodeProgress) * 200);
                    gui.drawString(Minecraft.getInstance().font,
                            r.nextBoolean() ? "0" : "1", bx, by,
                            (ba << 24) | (accentColor & 0x00FFFFFF), false);
                }
            }
        }

        gui.pose().popPose();
        return startTime >= 5000L ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static MedallionType tierForLevel(int level) {
        if (level >= 7) return MedallionType.LEGENDARY;
        if (level >= 4) return MedallionType.RARE;
        return MedallionType.COMMON;
    }

    /** Picks a random buff ID from the registry. Falls back to "speed" if empty. */
    private static String randomBuffId() {
        List<EliteBuff> all = new ArrayList<>(EliteBuffRegistry.getAll());
        if (all.isEmpty()) return "speed";
        return all.get((int)(System.currentTimeMillis() % all.size())).getId();
    }

    private String scrambleText(String original, float progress) {
        StringBuilder sb = new StringBuilder();
        java.util.Random rand = new java.util.Random(original.hashCode() + (long)(progress * 15.0F));
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (c != ' ' && rand.nextFloat() < progress) {
                sb.append(rand.nextBoolean() ? '0' : '1');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
