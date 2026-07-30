package org.xeb.xeb.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class MedallionBossBar {

    public enum MetalTier {
        BRONZE(0xFF1E0E06, 0xFFCD7F32, 0xFF6E3B13, 0xFF0B0502, 0xFFE5984A, ""),
        SILVER(0xFF10141A, 0xFFE0E6ED, 0xFF5C6B73, 0xFF050709, 0xFFFFFFFF, ""),
        GOLD(0xFF221A04, 0xFFFFD700, 0xFF8B6508, 0xFF0A0701, 0xFFFFF066, ""),
        MADNESS(0xFF1A041A, 0xFFFF0066, 0xFF440044, 0xFF080108, 0xFFFF3388, "§c§l[MADNESS]");

        public final int coreBg;
        public final int topAccent;
        public final int bottomAccent;
        public final int darkSocket;
        public final int metallicOutline;
        public final String defaultTag;

        MetalTier(int coreBg, int topAccent, int bottomAccent, int darkSocket, int metallicOutline, String defaultTag) {
            this.coreBg = coreBg;
            this.topAccent = topAccent;
            this.bottomAccent = bottomAccent;
            this.darkSocket = darkSocket;
            this.metallicOutline = metallicOutline;
            this.defaultTag = defaultTag;
        }
    }

    private float smoothHpRatio = 1.0F;
    private float smoothGhostHpRatio = 1.0F;

    public void render(GuiGraphics gui, Font font, int yOffset, String bossTitle, float targetHpRatio,
                       int currentHp, int maxHp, int charges, String leftBadgeTag,
                       LivingEntity mobEntity, float animScale, MetalTier tier) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float scale = Math.max(0.05F, Math.min(1.0F, animScale));
        int fullBarWidth = 270;
        int barWidth = (int) (fullBarWidth * scale);
        int barHeight = 15;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
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

        // 1. SOMBRA EXTERIOR ESCULPIDA (MEDALLION SOCKET)
        gui.fill(startX - 6, startY - 5, startX + barWidth + 6, startY + barHeight + 5, 0xF0020204);

        // 2. RASTRO DE DAÑO LERPEADO (GHOST HP)
        int ghostWidth = (int) (barWidth * Math.max(0.0F, Math.min(1.0F, smoothGhostHpRatio)));
        if (ghostWidth > 0) {
            gui.fill(startX, startY, startX + ghostWidth, startY + barHeight, 0xFF9E1B1B);
        }

        // 3. BARRA DE VIDA DE MEDALLÓN PRINCIPAL
        int currentHpWidth = (int) (barWidth * Math.max(0.0F, Math.min(1.0F, smoothHpRatio)));
        if (currentHpWidth > 0) {
            gui.fill(startX, startY, startX + currentHpWidth, startY + barHeight, tier.coreBg);
            gui.fill(startX, startY, startX + currentHpWidth, startY + 3, tier.topAccent);
            gui.fill(startX, startY + barHeight - 3, startX + currentHpWidth, startY + barHeight, tier.bottomAccent);

            // Shimmer metalizado que recorre la barra
            int scanX = (int) ((gameTime * 5.0D) % (barWidth + 60)) - 30;
            int scanStart = Math.max(startX, startX + scanX);
            int scanEnd = Math.min(startX + currentHpWidth, startX + scanX + 22);
            if (scanEnd > scanStart) {
                gui.fill(scanStart, startY, scanEnd, startY + barHeight, 0x60FFFFFF);
            }
        }

        // 4. MARCO MEDALLÓN METALIZADO CON BEVELS Y CORNER RIVETS
        renderMedallionFrame(gui, startX, startY, barWidth, barHeight, gameTime, tier);

        // 5. FLASH DE APERTURA SI LA ANIMACIÓN ESTÁ TRANSCURRIENDO
        if (scale < 0.99F) {
            int flashAlpha = (int) (210 * (1.0F - scale));
            int flashColor = (flashAlpha << 24) | (tier.topAccent & 0x00FFFFFF);
            gui.fill(startX - 12, startY - 7, startX + barWidth + 12, startY + barHeight + 7, flashColor);
        }

        // 6. ESCUDO DE MEDALLÓN CON ICONO DEL MOB 3D
        if (mobEntity != null && scale > 0.5F) {
            renderMobCrest(gui, font, startX - 42, startY - 5, mobEntity, tier);
        }

        // 7. TÍTULO ANIMADO FLOTANTE POR CARÁCTER
        renderFloatingTitle(gui, font, startX + (barWidth / 2), startY - 16, bossTitle, gameTime);

        // 8. TEXTO HP Y VALORES
        String hpText = String.format("%d / %d HP (%d%%)", currentHp, maxHp, (int) (targetHpRatio * 100));
        int fontW = font.width(hpText);
        gui.drawString(font, hpText, startX + (barWidth - fontW) / 2, startY + 4, 0xFFFFFFFF, true);

        // 9. ETIQUETA DE CLASE Y MEDALLONES
        if (leftBadgeTag != null && !leftBadgeTag.isEmpty()) {
            gui.drawString(font, leftBadgeTag, startX - font.width(leftBadgeTag) - 10, startY + 4, 0xFFFFFFFF, true);
        }

        if (charges > 0) {
            String medText = "§6" + charges;
            gui.drawString(font, medText, startX + barWidth + 10, startY + 4, 0xFFFFDD44, true);
        }

        RenderSystem.disableBlend();
    }

    private static void renderMedallionFrame(GuiGraphics gui, int x, int y, int w, int h, double time, MetalTier tier) {
        // Outer metallic border
        gui.fill(x - 2, y - 2, x + w + 2, y, tier.metallicOutline);
        gui.fill(x - 2, y + h, x + w + 2, y + h + 2, tier.metallicOutline);
        gui.fill(x - 2, y - 2, x, y + h + 2, tier.metallicOutline);
        gui.fill(x + w, y - 2, x + w + 2, y + h + 2, tier.metallicOutline);

        // Metallic corner rivets
        gui.fill(x - 4, y - 4, x - 1, y - 1, tier.topAccent);
        gui.fill(x + w + 1, y - 4, x + w + 4, y - 1, tier.topAccent);
        gui.fill(x - 4, y + h + 1, x - 1, y + h + 4, tier.bottomAccent);
        gui.fill(x + w + 1, y + h + 1, x + w + 4, y + h + 4, tier.bottomAccent);
    }

    private static void renderMobCrest(GuiGraphics gui, Font font, int badgeX, int badgeY, LivingEntity entity, MetalTier tier) {
        int size = 26;
        // Outer Shield Box
        gui.fill(badgeX - 2, badgeY - 2, badgeX + size + 2, badgeY + size + 2, 0xF0020204);
        gui.fill(badgeX, badgeY, badgeX + size, badgeY + size, tier.darkSocket);

        // Metallic Bevel Frame
        gui.fill(badgeX, badgeY, badgeX + size, badgeY + 2, tier.topAccent);
        gui.fill(badgeX, badgeY + size - 2, badgeX + size, badgeY + size, tier.bottomAccent);
        gui.fill(badgeX, badgeY, badgeX + 2, badgeY + size, tier.metallicOutline);
        gui.fill(badgeX + size - 2, badgeY, badgeX + size, badgeY + size, tier.metallicOutline);

        try {
            float scale = 11.0F;
            if (entity.getBbHeight() > 2.0F) {
                scale = 7.0F;
            } else if (entity.getBbWidth() > 1.5F) {
                scale = 8.0F;
            }
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsAngle(
                    gui, badgeX + 13, badgeY + 22, (int) scale, 0.0F, 0.0F, entity
            );
        } catch (Throwable ignored) {
            net.minecraft.world.item.Item egg = net.minecraft.world.item.SpawnEggItem.byId(entity.getType());
            if (egg != null) {
                gui.renderItem(new net.minecraft.world.item.ItemStack(egg), badgeX + 5, badgeY + 5);
            }
        }
    }

    private static void renderFloatingTitle(GuiGraphics gui, Font font, int centerX, int y, String title, double time) {
        if (title == null || title.isEmpty()) return;
        int totalW = font.width(title);
        int curX = centerX - (totalW / 2);

        for (int i = 0; i < title.length(); i++) {
            char ch = title.charAt(i);
            String str = String.valueOf(ch);
            int charW = font.width(str);

            double wave = Math.sin(time * 0.18D + (i * 0.35D)) * 2.0D;
            int renderY = (int) (y + wave);

            gui.drawString(font, str, curX, renderY, 0xFFFFFFFF, true);
            curX += charW;
        }
    }

    public void renderMini(GuiGraphics gui, Font font, int startX, int startY, String bossTitle, float targetHpRatio,
                           int currentHp, int maxHp, int charges, String leftBadgeTag,
                           LivingEntity mobEntity, float animScale, MetalTier tier) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float scale = Math.max(0.05F, Math.min(1.0F, animScale));
        int fullBarWidth = 105;
        int barWidth = (int) (fullBarWidth * scale);
        int barHeight = 11;

        smoothHpRatio = Mth.lerp(0.20F, smoothHpRatio, targetHpRatio);
        if (smoothGhostHpRatio < smoothHpRatio) {
            smoothGhostHpRatio = smoothHpRatio;
        } else {
            smoothGhostHpRatio = Mth.lerp(0.04F, smoothGhostHpRatio, smoothHpRatio);
        }

        double gameTime = mc.level.getGameTime() + mc.getFrameTime();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. SOMBRA EXTERIOR MINI
        gui.fill(startX - 4, startY - 3, startX + barWidth + 4, startY + barHeight + 3, 0xE6020204);

        // 2. RASTRO DE DAÑO GHOST
        int ghostWidth = (int) (barWidth * Math.max(0.0F, Math.min(1.0F, smoothGhostHpRatio)));
        if (ghostWidth > 0) {
            gui.fill(startX, startY, startX + ghostWidth, startY + barHeight, 0xFF9E1B1B);
        }

        // 3. BARRA DE VIDA MINI
        int currentHpWidth = (int) (barWidth * Math.max(0.0F, Math.min(1.0F, smoothHpRatio)));
        if (currentHpWidth > 0) {
            gui.fill(startX, startY, startX + currentHpWidth, startY + barHeight, tier.coreBg);
            gui.fill(startX, startY, startX + currentHpWidth, startY + 2, tier.topAccent);
            gui.fill(startX, startY + barHeight - 2, startX + currentHpWidth, startY + barHeight, tier.bottomAccent);
        }

        // 4. MARCO METALIZADO MINI
        gui.fill(startX - 1, startY - 1, startX + barWidth + 1, startY, tier.metallicOutline);
        gui.fill(startX - 1, startY + barHeight, startX + barWidth + 1, startY + barHeight + 1, tier.metallicOutline);
        gui.fill(startX - 1, startY - 1, startX, startY + barHeight + 1, tier.metallicOutline);
        gui.fill(startX + barWidth, startY - 1, startX + barWidth + 1, startY + barHeight + 1, tier.metallicOutline);

        // 5. FLASH DE APERTURA SI ANIMACIÓN TRANSCURRE
        if (scale < 0.99F) {
            int flashAlpha = (int) (200 * (1.0F - scale));
            int flashColor = (flashAlpha << 24) | (tier.topAccent & 0x00FFFFFF);
            gui.fill(startX - 6, startY - 4, startX + barWidth + 6, startY + barHeight + 4, flashColor);
        }

        // 6. MINI ESCUDO CON ICONO DEL MOB
        if (mobEntity != null && scale > 0.5F) {
            renderMiniMobCrest(gui, font, startX - 22, startY - 3, mobEntity, tier);
        }

        // 7. TEXTO DE TÍTULO Y VIDA MINI
        String titleText = (leftBadgeTag != null && !leftBadgeTag.isEmpty() ? leftBadgeTag + " " : "") + bossTitle;
        gui.drawString(font, titleText, startX + 2, startY - 9, 0xFFFFFFFF, true);

        String hpText = String.format("%d%%", (int) (targetHpRatio * 100));
        gui.drawString(font, hpText, startX + barWidth - font.width(hpText) - 2, startY + 1, 0xFFFFFFFF, true);

        if (charges > 0) {
            String medText = "§6" + charges;
            gui.drawString(font, medText, startX + barWidth + 4, startY + 1, 0xFFFFDD44, true);
        }

        RenderSystem.disableBlend();
    }

    private static void renderMiniMobCrest(GuiGraphics gui, Font font, int badgeX, int badgeY, LivingEntity entity, MetalTier tier) {
        int size = 18;
        gui.fill(badgeX - 1, badgeY - 1, badgeX + size + 1, badgeY + size + 1, 0xF0020204);
        gui.fill(badgeX, badgeY, badgeX + size, badgeY + size, tier.darkSocket);
        gui.fill(badgeX, badgeY, badgeX + size, badgeY + 1, tier.topAccent);
        gui.fill(badgeX, badgeY + size - 1, badgeX + size, badgeY + size, tier.bottomAccent);

        try {
            float scale = 8.0F;
            if (entity.getBbHeight() > 2.0F) {
                scale = 5.0F;
            }
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsAngle(
                    gui, badgeX + 9, badgeY + 15, (int) scale, 0.0F, 0.0F, entity
            );
        } catch (Throwable ignored) {
            net.minecraft.world.item.Item egg = net.minecraft.world.item.SpawnEggItem.byId(entity.getType());
            if (egg != null) {
                gui.renderItem(new net.minecraft.world.item.ItemStack(egg), badgeX + 1, badgeY + 1);
            }
        }
    }
}
