package org.xeb.xeb.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Componente modular XebBossBar para renderizado de barras de jefes personalizadas de alto nivel.
 * Soporta temas de color, marco esculpido pixel-art, rastro de daño fantasma lerpeado,
 * letras flotantes animadas y etiquetas de estado.
 */
public class XebBossBar {

    public enum Theme {
        OBSIDIAN_COSMIC(0xFF0D0D11, 0xFFF0F0F0, 0xFF777788, 0xFF050508, 0xFFFFFFFF),
        FIERY_NETHER(0xFF1D0905, 0xFFFF4400, 0xFF772200, 0xFF0D0302, 0xFFFF6600),
        VOID_ENDER(0xFF0F051A, 0xFFD044FF, 0xFF551177, 0xFF06020A, 0xFFE088FF),
        GOLDEN_DIVINE(0xFF1F1B05, 0xFFFFDD44, 0xFF887711, 0xFF0A0802, 0xFFFFEE88);

        public final int coreBg;
        public final int topBorder;
        public final int bottomBorder;
        public final int frameBg;
        public final int wingOutline;

        Theme(int coreBg, int topBorder, int bottomBorder, int frameBg, int wingOutline) {
            this.coreBg = coreBg;
            this.topBorder = topBorder;
            this.bottomBorder = bottomBorder;
            this.frameBg = frameBg;
            this.wingOutline = wingOutline;
        }
    }

    private float smoothHpRatio = 1.0F;
    private float smoothGhostHpRatio = 1.0F;

    public void render(GuiGraphics gui, Font font, int yOffset, String bossTitle, float targetHpRatio,
                       int currentHp, int maxHp, int charges, String phaseTagLeft, Theme theme) {
        render(gui, font, yOffset, bossTitle, targetHpRatio, currentHp, maxHp, null, charges, phaseTagLeft, theme);
    }

    public void render(GuiGraphics gui, Font font, int yOffset, String bossTitle, float targetHpRatio,
                       int currentHp, int maxHp, String extraStatusRight, String phaseTagLeft, Theme theme) {
        render(gui, font, yOffset, bossTitle, targetHpRatio, currentHp, maxHp, extraStatusRight, 0, phaseTagLeft, theme);
    }

    public void render(GuiGraphics gui, Font font, int yOffset, String bossTitle, float targetHpRatio,
                       int currentHp, int maxHp, String extraStatusRight, int charges, String phaseTagLeft, Theme theme) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int barWidth = 260;
        int barHeight = 14;
        int startX = (screenWidth - barWidth) / 2;
        int startY = yOffset;

        smoothHpRatio = Mth.lerp(0.18F, smoothHpRatio, targetHpRatio);
        if (smoothGhostHpRatio < smoothHpRatio) {
            smoothGhostHpRatio = smoothHpRatio;
        } else {
            smoothGhostHpRatio = Mth.lerp(0.03F, smoothGhostHpRatio, smoothHpRatio);
        }

        double gameTime = mc.level.getGameTime() + mc.getFrameTime();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. SOMBRA EXTERIOR
        gui.fill(startX - 4, startY - 4, startX + barWidth + 4, startY + barHeight + 4, 0xE6020204);

        // 2. RASTRO DE DAÑO GHOST LERPEADO (ROJO FUEGO)
        int ghostWidth = (int) (barWidth * Math.max(0.0F, Math.min(1.0F, smoothGhostHpRatio)));
        gui.fill(startX, startY, startX + ghostWidth, startY + barHeight, 0xFFB32424);

        // 3. BARRA DE VIDA PRINCIPAL SEGÚN EL TEMA
        int currentHpWidth = (int) (barWidth * Math.max(0.0F, Math.min(1.0F, smoothHpRatio)));
        if (currentHpWidth > 0) {
            gui.fill(startX, startY, startX + currentHpWidth, startY + barHeight, theme.coreBg);
            gui.fill(startX, startY, startX + currentHpWidth, startY + 3, theme.topBorder);
            gui.fill(startX, startY + barHeight - 3, startX + currentHpWidth, startY + barHeight, theme.bottomBorder);

            // Scanline eléctrico brillante
            int scanX = (int) ((gameTime * 6.0D) % (barWidth + 50)) - 25;
            int scanStart = Math.max(startX, startX + scanX);
            int scanEnd = Math.min(startX + currentHpWidth, startX + scanX + 20);
            if (scanEnd > scanStart) {
                gui.fill(scanStart, startY, scanEnd, startY + barHeight, 0x70FFFFFF);
            }
        }

        // 4. MARCO PIXEL-ART ESCULPIDO CON ALAS ESPINOSAS
        renderSpikyFrame(gui, startX, startY, barWidth, barHeight, gameTime, theme);

        // 5. TÍTULO ANIMADO Y FLOTANTE POR CARÁCTER
        renderFloatingTitle(gui, font, startX + (barWidth / 2), startY - 16, bossTitle, gameTime);

        // 6. VALORES EN NÚMEROS Y ETIQUETAS
        String hpText = String.format("%d / %d HP (%d%%)", currentHp, maxHp, (int) (targetHpRatio * 100));
        int fontW = font.width(hpText);
        gui.drawString(font, hpText, startX + (barWidth - fontW) / 2, startY + 3, 0xFFFFFFFF, true);

        if (extraStatusRight != null && !extraStatusRight.isEmpty()) {
            gui.drawString(font, extraStatusRight, startX + barWidth + 8, startY + 3, 0xFF00FFFF, true);
        }

        if (phaseTagLeft != null && !phaseTagLeft.isEmpty()) {
            gui.drawString(font, phaseTagLeft, startX - font.width(phaseTagLeft) - 8, startY + 3, 0xFFFF2222, true);
        }

        // 7. BADGE ESTILIZADO DE CARGAS ACOMODADO CENTRADO DEBAJO DE LA BARRA
        if (charges > 0) {
            renderChargesBadge(gui, font, startX + (barWidth / 2), startY + barHeight + 5, charges, theme);
        }

        RenderSystem.disableBlend();
    }

    private static void renderChargesBadge(GuiGraphics gui, Font font, int centerX, int baseY, int charges, Theme theme) {
        String labelText;
        if (charges <= 6) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < charges; i++) {
                if (i > 0) sb.append(" ");
                sb.append("✦");
            }
            labelText = sb.toString();
        } else {
            labelText = "✦ " + charges + " CHARGES";
        }

        int paddingX = 6;
        int paddingY = 2;
        int textW = font.width(labelText);
        int badgeW = textW + (paddingX * 2);
        int badgeH = font.lineHeight + (paddingY * 2);
        int badgeX = centerX - (badgeW / 2);

        gui.fill(badgeX - 1, baseY - 1, badgeX + badgeW + 1, baseY + badgeH + 1, 0xE6020204);
        gui.fill(badgeX, baseY, badgeX + badgeW, baseY + badgeH, 0xD00A0A12);

        gui.fill(badgeX, baseY, badgeX + badgeW, baseY + 1, theme.topBorder);
        gui.fill(badgeX, baseY + badgeH - 1, badgeX + badgeW, baseY + badgeH, theme.bottomBorder);

        gui.drawString(font, labelText, badgeX + paddingX, baseY + paddingY, 0xFFE0E0FF, true);
    }

    private static void renderSpikyFrame(GuiGraphics gui, int x, int y, int w, int h, double gameTime, Theme theme) {
        gui.fill(x - 2, y - 2, x + w + 2, y - 1, theme.topBorder);
        gui.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, theme.topBorder);
        gui.fill(x - 2, y - 2, x - 1, y + h + 2, theme.topBorder);
        gui.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, theme.topBorder);

        // Ala Izquierda
        gui.fill(x - 8, y - 5, x - 2, y + h + 5, theme.frameBg);
        gui.fill(x - 14, y - 3, x - 8, y + h + 3, 0xFF14141B);
        gui.fill(x - 20, y, x - 14, y + h, 0xFF2A2A38);
        gui.fill(x - 26, y + 3, x - 20, y + h - 3, 0xFF55556A);
        gui.fill(x - 30, y + 5, x - 26, y + h - 5, theme.wingOutline);

        // Ala Derecha
        gui.fill(x + w + 2, y - 5, x + w + 8, y + h + 5, theme.frameBg);
        gui.fill(x + w + 8, y - 3, x + w + 14, y + h + 3, 0xFF14141B);
        gui.fill(x + w + 14, y, x + w + 20, y + h, 0xFF2A2A38);
        gui.fill(x + w + 20, y + 3, x + w + 26, y + h - 3, 0xFF55556A);
        gui.fill(x + w + 26, y + 5, x + w + 30, y + h - 5, theme.wingOutline);

        // Gema / Núcleo Central
        int cx = x + (w / 2);
        int gy = y - 7;
        boolean pulse = (int) (gameTime * 0.15D) % 2 == 0;
        int coreColor = pulse ? 0xFFFFFFFF : 0xFF000000;
        gui.fill(cx - 10, gy - 4, cx + 10, gy + 4, 0xFF020203);
        gui.fill(cx - 7, gy - 3, cx + 7, gy + 3, coreColor);
        gui.fill(cx - 3, gy - 1, cx + 3, gy + 1, pulse ? 0xFF000000 : 0xFFFFFFFF);
    }

    private static void renderFloatingTitle(GuiGraphics gui, Font font, int centerX, int baseY, String title, double gameTime) {
        int totalWidth = font.width(title);
        int currentX = centerX - (totalWidth / 2);

        char[] chars = title.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            String charStr = String.valueOf(c);
            float floatY = baseY + (float) (Math.sin((gameTime * 0.18D) + (i * 0.75D)) * 3.5D);

            int color = 0xFFFFFFFF;
            if (c != ' ') {
                double colorPhase = Math.sin((gameTime * 0.22D) + (i * 0.6D));
                if (colorPhase > 0.4D) {
                    color = 0xFFFFFFFF;
                } else if (colorPhase < -0.4D) {
                    color = 0xFFAAAABB;
                } else {
                    color = 0xFFDDDDDD;
                }
            }

            gui.drawString(font, charStr, currentX, (int) floatY, color, true);
            currentX += font.width(charStr);
        }
    }
}
