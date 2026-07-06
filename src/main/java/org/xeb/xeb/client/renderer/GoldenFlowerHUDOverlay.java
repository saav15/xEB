package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GoldenFlowerHUDOverlay {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        Player player = mc.player;
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        boolean holdsFlower = mainHand.is(ModItems.GOLDEN_FLOWER.get()) || offHand.is(ModItems.GOLDEN_FLOWER.get());

        if (!holdsFlower) return;

        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();
        GuiGraphics g = event.getGuiGraphics();

        // 1. RENDER 6-FLOWER HUD ON CROSSHAIR
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            int centerX = screenW / 2;
            int centerY = screenH / 2;

            int charges = player.getPersistentData().getInt("xebGoldenFlowerCharges");
            int loaded = player.getPersistentData().getInt("xebGoldenFlowerLoadedCount");

            if (!player.getPersistentData().contains("xebGoldenFlowerCharges")) {
                charges = 6; // default fallback
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // Coordinates for the 6 connected flowers
            int[] flowerColors = {
                    0x00FFFF, // 0: Cyan
                    0xC080FF, // 1: Purple
                    0xFF9F20, // 2: Orange
                    0x40FF40, // 3: Green
                    0x4060FF, // 4: Blue
                    0xFFFF40  // 5: Yellow
            };

            double radius = 18.0D;
            for (int i = 0; i < 6; i++) {
                // Curved arc over the top of the crosshair (from -145 to -35 degrees)
                double angle = Math.toRadians(-145.0D + i * 22.0D);
                int fx = centerX + (int) (Math.cos(angle) * radius);
                int fy = centerY + (int) (Math.sin(angle) * radius);

                boolean isRecharged = charges >= (i + 1);
                boolean isLoaded = i < loaded;

                int color;
                if (isRecharged) {
                    color = flowerColors[i];
                } else {
                    color = 0x303030; // greyed out
                }

                // If loaded, draw glowing white outline with colored filled center
                if (isLoaded) {
                    drawTinyFlower(g, fx, fy, 0xFFFFFFFF);
                    drawTinyCenter(g, fx, fy, color);
                } else {
                    // Empty sprite with outline of the flower
                    drawTinyFlower(g, fx, fy, color);
                }
            }

            RenderSystem.disableBlend();
        }

        // 2. RENDER ACTIVE KICK / DANCE COOLDOWNS AT THE BOTTOM LEFT
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int boxW = 24;
            int xStart = org.xeb.xeb.Config.opticBlastHudX; // Reuse standard xEB HUD position config
            int yStart = screenH - org.xeb.xeb.Config.opticBlastHudY;

            // --- Flower Dance (Activa 1) ---
            int danceCD = player.getPersistentData().getInt("xebGoldenFlowerDanceCooldown");
            boolean danceActive = player.getPersistentData().getBoolean("xebFlowerDanceActive");
            String key1 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_1_KEY.getTranslatedKeyMessage().getString().toUpperCase();
            renderAbilityBox(g, mc, xStart, yStart, key1, "DANC", danceCD, 400, danceActive);

            // --- Jarona (Activa 2) ---
            int jaronaCharges = player.getPersistentData().getInt("xebJaronaCharges");
            if (!player.getPersistentData().contains("xebJaronaCharges")) {
                jaronaCharges = 3; // default fallback
            }
            int jaronaTimer = player.getPersistentData().getInt("xebJaronaRechargeTimer");
            boolean jaronaActive = org.xeb.xeb.client.JaronaAttack.getKick(player) != null;
            String key2 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_2_KEY.getTranslatedKeyMessage().getString().toUpperCase();

            // Render Jarona box
            renderJaronaBox(g, mc, xStart + boxW + 6, yStart, key2, "JARO", jaronaCharges, jaronaTimer, jaronaActive);

            // Ultimate Curio box
            if (org.xeb.xeb.item.QuantumCatBarrageItem.hasUltimateCurio(player)) {
                org.xeb.xeb.render.DoomfistHUDOverlay.renderUltimateBox(g, mc, player, screenH);
            }

            RenderSystem.disableBlend();
        }
    }

    private static void drawTinyFlower(GuiGraphics g, int x, int y, int color) {
        int alpha = 0xFF000000;
        int finalColor = alpha | color;

        // Simple pixel-art flower cross-shape
        g.fill(x, y - 2, x + 1, y + 3, finalColor); // vertical line
        g.fill(x - 2, y, x + 3, y + 1, finalColor); // horizontal line
    }

    private static void drawTinyCenter(GuiGraphics g, int x, int y, int color) {
        int alpha = 0xFF000000;
        int finalColor = alpha | color;
        g.fill(x - 1, y - 1, x + 2, y + 2, finalColor);
    }

    private static void renderAbilityBox(GuiGraphics g, Minecraft mc, int x, int y,
                                          String key, String label, int cd, int maxCd,
                                          boolean isActive) {
        int boxW = 24;
        int boxH = 24;

        // Background
        g.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF000000);

        if (isActive) {
            float pulse = 0.5F + 0.5F * (float) Math.sin(mc.level.getGameTime() * 0.4D);
            int r = (int) (230 * pulse);
            int gVal = (int) (180 * pulse);
            int color = (0xBB << 24) | (r << 16) | (gVal << 8) | 0x00; // glowing gold
            g.fill(x, y, x + boxW, y + boxH, color);
        } else {
            g.fill(x, y, x + boxW, y + boxH, 0x44000000);
        }

        // Cooldown overlay
        if (cd > 0 && !isActive) {
            float ratio = cd / (float) maxCd;
            int overlayH = (int) (boxH * ratio);
            g.fill(x, y + boxH - overlayH, x + boxW, y + boxH, 0x99555555);
        }

        // Border
        int borderColor = (cd > 0 && !isActive) ? 0x44FFFFFF : 0xBBFFFFFF;
        if (isActive) borderColor = 0xFFFFD700; // Gold border
        g.fill(x, y, x + boxW, y + 1, borderColor);
        g.fill(x, y + boxH - 1, x + boxW, y + boxH, borderColor);
        g.fill(x, y, x + 1, y + boxH, borderColor);
        g.fill(x + boxW - 1, y, x + boxW, y + boxH, borderColor);

        // Keybind (Centered & scaled if too long)
        int keyColor = (cd > 0 && !isActive) ? 0x88AAAAAA : 0xFFFFAA00;
        int textWidth = mc.font.width(key);
        if (textWidth > boxW - 2) {
            float scale = (float) (boxW - 2) / textWidth;
            g.pose().pushPose();
            g.pose().translate(x + boxW / 2.0F, y + boxH / 2.0F, 0);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(mc.font, key, -textWidth / 2.0F, -mc.font.lineHeight / 2.0F, keyColor, false);
            g.pose().popPose();
        } else {
            int keyX = x + (boxW - textWidth) / 2;
            int keyY = y + (boxH - mc.font.lineHeight) / 2;
            g.drawString(mc.font, key, keyX, keyY, keyColor, false);
        }

        // Label above the box
        int labelColor = (cd > 0 && !isActive) ? 0x44FFFFFF : 0x88FFFFFF;
        g.drawString(mc.font, label, x, y - 9, labelColor, false);

        // CD Text overlay below the box
        if (cd > 0 && !isActive) {
            String timeText = String.format("%.1fs", cd / 20.0F);
            int timeX = x + (boxW - mc.font.width(timeText)) / 2;
            g.drawString(mc.font, timeText, timeX, y + boxH + 2, 0xFFFF3333, true);
        }
    }

    private static void renderJaronaBox(GuiGraphics g, Minecraft mc, int x, int y,
                                        String key, String label, int charges, int timer,
                                        boolean isActive) {
        int boxW = 24;
        int boxH = 24;

        // Background
        g.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF000000);

        if (isActive) {
            float pulse = 0.5F + 0.5F * (float) Math.sin(mc.level.getGameTime() * 0.4D);
            int b = (int) (240 * pulse);
            int color = (0xBB << 24) | (0 << 16) | ((int)(80 * pulse) << 8) | b; // glowing blue
            g.fill(x, y, x + boxW, y + boxH, color);
        } else {
            g.fill(x, y, x + boxW, y + boxH, 0x44000000);
        }

        // Recharge overlay if not full
        if (charges < 3 && !isActive) {
            float ratio = (120 - timer) / 120.0F;
            int overlayH = (int) (boxH * ratio);
            g.fill(x, y + boxH - overlayH, x + boxW, y + boxH, 0x99555555);
        }

        // Border
        int borderColor = (charges == 0 && !isActive) ? 0x44FFFFFF : 0xBBFFFFFF;
        if (isActive) borderColor = 0xFF3366FF; // Blue border
        g.fill(x, y, x + boxW, y + 1, borderColor);
        g.fill(x, y + boxH - 1, x + boxW, y + boxH, borderColor);
        g.fill(x, y, x + 1, y + boxH, borderColor);
        g.fill(x + boxW - 1, y, x + boxW, y + boxH, borderColor);

        // Keybind (Centered & scaled if too long)
        int keyColor = (charges == 0 && !isActive) ? 0x88AAAAAA : 0xFFFFAA00;
        int textWidth = mc.font.width(key);
        if (textWidth > boxW - 2) {
            float scale = (float) (boxW - 2) / textWidth;
            g.pose().pushPose();
            g.pose().translate(x + boxW / 2.0F, y + boxH / 2.0F, 0);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(mc.font, key, -textWidth / 2.0F, -mc.font.lineHeight / 2.0F, keyColor, false);
            g.pose().popPose();
        } else {
            int keyX = x + (boxW - textWidth) / 2;
            int keyY = y + (boxH - mc.font.lineHeight) / 2;
            g.drawString(mc.font, key, keyX, keyY, keyColor, false);
        }

        // Label above the box
        int labelColor = (charges == 0 && !isActive) ? 0x44FFFFFF : 0x88FFFFFF;
        g.drawString(mc.font, label, x, y - 9, labelColor, false);

        // Charge Counter in the bottom-right corner of the box itself
        String chargeStr = "x" + charges;
        int chargeW = mc.font.width(chargeStr);
        g.drawString(mc.font, chargeStr, x + boxW - chargeW - 1, y + boxH - 9, charges > 0 ? 0xFF00FF00 : 0xFFFF3333, true);

        // Cooldown timer below the box if recharging
        if (charges < 3 && !isActive) {
            String timeText = String.format("%.1fs", (120 - timer) / 20.0F);
            int timeX = x + (boxW - mc.font.width(timeText)) / 2;
            g.drawString(mc.font, timeText, timeX, y + boxH + 2, 0xFF3366FF, true);
        }
    }
}
