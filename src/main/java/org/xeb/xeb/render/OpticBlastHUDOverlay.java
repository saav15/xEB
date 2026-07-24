package org.xeb.xeb.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.item.OpticBlastItem;

/**
 * HUD overlay for the Optic Blast weapon.
 * <p>
 * Renders:
 * <ul>
 *   <li>A curved energy bar to the left of the crosshair that drains while firing
 *       and regenerates when idle. Goes from white/green (full) to red (depleted).</li>
 *   <li>Warning flash at ~20% energy.</li>
 *   <li>Overheat pulsing red when energy hits 0 and forced cooldown kicks in.</li>
 *   <li>Ability icons (Cyclone Push and Gene Splice) with cooldown overlays
 *       at bottom left (standardized to Doomfist coordinates, configurable).</li>
 *   <li>Red screen vignette overlay in first person showing resource depletion,
 *       mini-laser firing flashes, and ability usage glows (rendered behind the hotbar, no black edges).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class OpticBlastHUDOverlay {

    /** Total bar height in pixels. */
    private static final int BAR_HEIGHT = 40;
    /** Bar width in pixels. */
    private static final int BAR_WIDTH = 5;
    /** Horizontal offset from crosshair center (negative = left). */
    private static final int X_OFFSET = -22;
    /** Vertical offset from crosshair center (negative = above). */
    private static final int Y_OFFSET = -BAR_HEIGHT / 2;

    /** Curve offset in pixels (max horizontal displacement at the midpoint of the bar). */
    private static final float CURVE_MAGNITUDE = 3.0F;

    /** Energy threshold for warning flash. */
    private static final float WARNING_THRESHOLD = 0.20F;

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        Player player = mc.player;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean holdsOptic = mainHand.is(ModItems.OPTIC_BLAST.get()) || offHand.is(ModItems.OPTIC_BLAST.get());

        if (!holdsOptic) return;

        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();
        long gameTime = mc.level.getGameTime();
        float partialTick = event.getPartialTick();
        float time = gameTime + partialTick;

        GuiGraphics g = event.getGuiGraphics();

        // 1. RENDER SCREEN VIGNETTE BEFORE HOTBAR (hooked into VanillaGuiOverlay.VIGNETTE)
        if (event.getOverlay().id().equals(VanillaGuiOverlay.VIGNETTE.id())) {
            if (mc.options.getCameraType().isFirstPerson()) {
                float energy = player.getPersistentData().contains("xebOpticEnergy")
                        ? player.getPersistentData().getFloat("xebOpticEnergy")
                        : OpticBlastItem.MAX_ENERGY;
                float energyRatio = energy / OpticBlastItem.MAX_ENERGY;

                float useRatio = 1.0F - energyRatio;
                float primaryAura = useRatio * 0.70F; // Max intensity 0.70

                // Mini-laser transient flash (starts at 30% and fades out quickly over 6 ticks)
                long lastMiniShot = player.getPersistentData().getLong("xebMiniLaserLastShot");
                long ticksSinceMini = gameTime - lastMiniShot;
                float miniAura = 0.0F;
                if (ticksSinceMini >= 0 && ticksSinceMini < 6) {
                    miniAura = ((6 - ticksSinceMini) / 6.0F) * 0.30F;
                }

                // Cyclone Push fading overlay (starts at 50% and fades out over duration)
                int cycloneTicks = player.getPersistentData().getInt("xebCyclonePushTicks");
                float cycloneAura = 0.0F;
                if (cycloneTicks > 0) {
                    cycloneAura = (cycloneTicks / (float) OpticBlastItem.CYCLONE_PUSH_DURATION) * 0.50F;
                }

                // Gene Splice fading overlay (starts at 60% and fades out over duration)
                int spliceTicks = player.getPersistentData().getInt("xebGeneSpliceTicks");
                float spliceAura = 0.0F;
                if (spliceTicks > 0) {
                    spliceAura = (spliceTicks / (float) OpticBlastItem.GENE_SPLICE_DURATION) * 0.60F;
                }

                float vignetteIntensity = Math.max(primaryAura, Math.max(miniAura, Math.max(cycloneAura, spliceAura)));
                if (vignetteIntensity > 0.01F) {
                    int borderSize = 40;
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    
                    // Draw pure red gradients over the screen borders (no black components)
                    // Since it runs in VIGNETTE stage, it renders behind/underneath the hotbar automatically!
                    
                    // Top border (vertical gradient, top is red, bottom is clear)
                    drawVerticalRedGradient(g, 0, 0, screenW, borderSize, vignetteIntensity, true);
                    
                    // Bottom border (vertical gradient, top is clear, bottom is red)
                    drawVerticalRedGradient(g, 0, screenH - borderSize, screenW, borderSize, vignetteIntensity, false);
                    
                    // Left border (horizontal gradient, left is red, right is clear)
                    drawHorizontalRedGradient(g, 0, 0, borderSize, screenH, vignetteIntensity, true);
                    
                    // Right border (horizontal gradient, left is clear, right is red)
                    drawHorizontalRedGradient(g, screenW - borderSize, 0, borderSize, screenH, vignetteIntensity, false);
                    
                    RenderSystem.depthMask(true);
                    RenderSystem.enableDepthTest();
                }
            }
            return;
        }

        // 2. RENDER THE REST OF HUD ON CROSSHAIR
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) return;

        int centerX = screenW / 2;
        int centerY = screenH / 2;

        int barBaseX = centerX + org.xeb.xeb.Config.opticBlastHudX;
        int barBaseY = centerY - 18 + org.xeb.xeb.Config.opticBlastHudY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Read synced energy state
        float energy = player.getPersistentData().contains("xebOpticEnergy")
                ? player.getPersistentData().getFloat("xebOpticEnergy")
                : OpticBlastItem.MAX_ENERGY;
        boolean isOverheated = player.getPersistentData().getBoolean("xebOpticOverheated");

        float energyRatio = energy / OpticBlastItem.MAX_ENERGY;

        boolean isFiring = player.isUsingItem() && player.getUseItem().is(ModItems.OPTIC_BLAST.get());

        float cooldownPercent = player.getCooldowns().getCooldownPercent(ModItems.OPTIC_BLAST.get(), partialTick);

        if (isOverheated) {
            // --- OVERHEAT STATE: Full bar pulsing red, countdown ---
            renderCurvedBar(g, barBaseX, barBaseY, 1.0F, time, true, cooldownPercent);

            // Cooldown timer text
            float secondsLeft = (cooldownPercent * OpticBlastItem.OVERHEAT_COOLDOWN) / 20.0F;
            String timerText = String.format("%.1fs", secondsLeft);
            int textX = barBaseX - mc.font.width(timerText) - 2;
            int textY = barBaseY + BAR_HEIGHT / 2 - mc.font.lineHeight / 2;

            float pulse = 0.6F + 0.4F * (float) Math.sin(time * 0.3D);
            int textAlpha = (int) (255 * pulse);
            int textColor = (textAlpha << 24) | 0xFF3333;
            g.drawString(mc.font, timerText, textX, textY, textColor, true);

            // "OVERHEAT" label
            String overheatText = "OVERHEAT";
            int ohX = barBaseX - mc.font.width(overheatText) - 2;
            int ohY = barBaseY - 10;
            int ohAlpha = (int) (220 * pulse);
            int ohColor = (ohAlpha << 24) | 0xFF2222;
            g.drawString(mc.font, overheatText, ohX, ohY, ohColor, true);

        } else {
            // --- NORMAL / FIRING STATE ---
            renderCurvedBar(g, barBaseX, barBaseY, energyRatio, time, false, 0.0F);

            // If low on energy, show warning
            if (energyRatio <= WARNING_THRESHOLD && energyRatio > 0.0F && isFiring) {
                float warnPulse = 0.5F + 0.5F * (float) Math.sin(time * 0.5D);
                String warnText = "LOW";
                int warnX = barBaseX - mc.font.width(warnText) - 2;
                int warnY = barBaseY + BAR_HEIGHT + 2;
                int warnAlpha = (int) (200 * warnPulse);
                int warnColor = (warnAlpha << 24) | 0xFFAA00;
                g.drawString(mc.font, warnText, warnX, warnY, warnColor, true);
            }
        }

        // --- ABILITY ICONS ---
        renderAbilityIcons(g, mc, player, screenH, time);

        RenderSystem.disableBlend();
    }

    /**
     * Renders the energy bar. For NORMAL state, energyRatio represents how FULL the bar is
     * (1.0 = full, 0.0 = empty). Bar fills from bottom to top.
     */
    public static void renderCurvedBar(GuiGraphics g, int baseX, int baseY,
                                         float fillProgress, float time,
                                         boolean cooldownGlow, float cooldownPct) {
        for (int row = 0; row < BAR_HEIGHT; row++) {
            float normalizedPos = row / (float) (BAR_HEIGHT - 1);

            float curveFactor = (float) Math.sin(normalizedPos * Math.PI);
            int curveOffset = (int) (CURVE_MAGNITUDE * curveFactor);

            int rowX = baseX - curveOffset;
            int rowY = baseY + row;

            float fillThreshold = 1.0F - fillProgress;
            boolean isFilled = normalizedPos >= fillThreshold;

            // Background
            int bgAlpha;
            if (fillProgress > 0.0F || cooldownGlow) {
                bgAlpha = 0x88;
            } else {
                bgAlpha = 0x33;
            }
            int bgColor = (bgAlpha << 24) | 0x111111;
            g.fill(rowX - 1, rowY, rowX + BAR_WIDTH + 1, rowY + 1, bgColor);

            if (cooldownGlow) {
                // Overheat: pulsing red glow
                float pulse = 0.4F + 0.6F * (float) Math.sin(time * 0.25D + normalizedPos * 2.0D);

                float drainThreshold = 1.0F - cooldownPct;
                boolean isDrained = normalizedPos < drainThreshold;

                if (isDrained) {
                    int dimR = (int) (40 * pulse);
                    int dimColor = (0x44 << 24) | (dimR << 16) | 0x0000;
                    g.fill(rowX, rowY, rowX + BAR_WIDTH, rowY + 1, dimColor);
                } else {
                    int r = (int) (255 * pulse);
                    int gVal = (int) (30 * pulse);
                    int glowAlpha = (int) (200 * pulse);
                    int glowColor = (glowAlpha << 24) | (r << 16) | (gVal << 8) | 0x00;
                    g.fill(rowX, rowY, rowX + BAR_WIDTH, rowY + 1, glowColor);
                }

            } else if (isFilled) {
                // Energy bar: color based on energy level
                float rowFillRatio = (normalizedPos - fillThreshold) / Math.max(0.001F, fillProgress);

                int r, gVal, b, alpha;

                if (fillProgress <= WARNING_THRESHOLD) {
                    // Low energy: orange-red warning with flicker
                    float warn = 0.6F + 0.4F * (float) Math.sin(time * 0.5D + normalizedPos * 3.0D);
                    r = 255;
                    gVal = (int) (100 * warn);
                    b = 0;
                    alpha = (int) (200 * warn);
                } else {
                    // Normal: bright red with subtle white at bottom
                    r = 255;
                    gVal = (int) (60 * rowFillRatio);
                    b = (int) (30 * rowFillRatio);
                    alpha = (int) (180 + 75 * rowFillRatio);
                }

                // Shimmer at the fill edge
                if (Math.abs(normalizedPos - fillThreshold) < 0.05F) {
                    float shimmer = 0.7F + 0.3F * (float) Math.sin(time * 0.6D);
                    alpha = (int) (alpha * shimmer);
                    gVal = (int) (200 * shimmer);
                    b = (int) (100 * shimmer);
                }

                int fillColor = (Math.min(255, alpha) << 24) | (r << 16) | (gVal << 8) | b;
                g.fill(rowX, rowY, rowX + BAR_WIDTH, rowY + 1, fillColor);

            } else {
                // Empty portion
                int emptyColor = (0x22 << 24) | 0x222222;
                g.fill(rowX, rowY, rowX + BAR_WIDTH, rowY + 1, emptyColor);
            }
        }

        // Border highlight
        for (int row = 0; row < BAR_HEIGHT; row++) {
            float normalizedPos = row / (float) (BAR_HEIGHT - 1);
            float curveFactor = (float) Math.sin(normalizedPos * Math.PI);
            int curveOffset = (int) (CURVE_MAGNITUDE * curveFactor);
            int rowX = baseX - curveOffset;
            int rowY = baseY + row;

            int borderAlpha;
            if (fillProgress > 0.0F || cooldownGlow) {
                borderAlpha = 0x66;
            } else {
                borderAlpha = 0x22;
            }

            int borderColor;
            if (cooldownGlow) {
                float pulse = 0.3F + 0.7F * (float) Math.sin(time * 0.2D);
                int rBorder = (int) (200 * pulse);
                borderColor = ((int)(borderAlpha * pulse) << 24) | (rBorder << 16) | 0x0000;
            } else {
                borderColor = (borderAlpha << 24) | 0xAAAAAA;
            }

            g.fill(rowX - 1, rowY, rowX, rowY + 1, borderColor);
            g.fill(rowX + BAR_WIDTH, rowY, rowX + BAR_WIDTH + 1, rowY + 1, borderColor);
        }
    }

    /**
     * Renders ability icons at the bottom left (matching Doomfist standard, configurable).
     */
    private static void renderAbilityIcons(GuiGraphics g, Minecraft mc, Player player,
                                            int screenH, float time) {
        // Read cooldowns from synced data
        int cycloneCD = player.getPersistentData().getInt("xebCyclonePushCooldown");
        int spliceCD = player.getPersistentData().getInt("xebGeneSpliceCooldown");

        boolean cycloneFiring = player.getPersistentData().getBoolean("xebCyclonePushFiring");
        boolean spliceFiring = player.getPersistentData().getBoolean("xebGeneSpliceFiring");

        String key1 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_1_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        String key2 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_2_KEY.getTranslatedKeyMessage().getString().toUpperCase();

        // Cyclone Push icon (Activa 1)
        g.pose().pushPose();
        g.pose().translate(org.xeb.xeb.Config.activa1HudX, screenH - org.xeb.xeb.Config.activa1HudY, 0);
        g.pose().scale(org.xeb.xeb.Config.activa1HudScale, org.xeb.xeb.Config.activa1HudScale, 1.0f);
        renderAbilityBox(g, mc, 0, 0, key1, "CYCL", cycloneCD,
                OpticBlastItem.CYCLONE_PUSH_COOLDOWN, cycloneFiring, time);
        g.pose().popPose();

        // Gene Splice icon (Activa 2)
        g.pose().pushPose();
        g.pose().translate(org.xeb.xeb.Config.activa2HudX, screenH - org.xeb.xeb.Config.activa2HudY, 0);
        g.pose().scale(org.xeb.xeb.Config.activa2HudScale, org.xeb.xeb.Config.activa2HudScale, 1.0f);
        renderAbilityBox(g, mc, 0, 0, key2, "GENE", spliceCD,
                OpticBlastItem.GENE_SPLICE_COOLDOWN, spliceFiring, time);
        g.pose().popPose();

        // Extreme Burst curio box (Activa 3)
        org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry =
                org.xeb.xeb.extremeburst.ExtremeBurstRegistry.findActiveBurst(player);
        if (DoomfistHUDOverlay.shouldShowBurstHUD(player, burstEntry,
                player.getMainHandItem(), player.getOffhandItem())) {
            DoomfistHUDOverlay.renderUltimateBox(g, mc, player, screenH, burstEntry);
        }
    }

    private static void renderAbilityBox(GuiGraphics g, Minecraft mc, int x, int y,
                                          String key, String label, int cd, int maxCd,
                                          boolean isFiring, float time) {
        int boxW = 24;
        int boxH = 24;

        // Box background
        g.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF000000);

        if (isFiring) {
            float pulse = 0.5F + 0.5F * (float) Math.sin(time * 0.4D);
            int r = (int) (255 * pulse);
            int gVal = (int) (40 * pulse);
            int fireColor = (0xBB << 24) | (r << 16) | (gVal << 8) | 0x00;
            g.fill(x, y, x + boxW, y + boxH, fireColor);
        } else {
            g.fill(x, y, x + boxW, y + boxH, 0x44000000);
        }

        // Cooldown overlay
        if (cd > 0 && !isFiring) {
            float ratio = cd / (float) maxCd;
            int overlayH = (int) (boxH * ratio);
            g.fill(x, y + boxH - overlayH, x + boxW, y + boxH, 0x99555555);
        }

        // Border
        int borderColor = (cd > 0 && !isFiring) ? 0x44FFFFFF : 0xBBFFFFFF;
        if (isFiring) borderColor = 0xFFFF4444;
        g.fill(x, y, x + boxW, y + 1, borderColor);
        g.fill(x, y + boxH - 1, x + boxW, y + boxH, borderColor);
        g.fill(x, y, x + 1, y + boxH, borderColor);
        g.fill(x + boxW - 1, y, x + boxW, y + boxH, borderColor);

        // Key label (auto-scaled for long names)
        int keyColor = (cd > 0 && !isFiring) ? 0x88AAAAAA : 0xFFFFFFFF;
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

        // Ability name
        int labelColor = (cd > 0 && !isFiring) ? 0x44FFFFFF : 0x88FFFFFF;
        if (isFiring) labelColor = 0xFFFF6666;
        g.drawString(mc.font, label, x, y - 9, labelColor, false);

        // Cooldown timer
        if (cd > 0 && !isFiring) {
            String timeText = String.format("%.1fs", cd / 20.0F);
            int timeX = x + (boxW - mc.font.width(timeText)) / 2;
            int timerColor = 0xFFFF6644;
            g.drawString(mc.font, timeText, timeX, y + boxH + 2, timerColor, true);
        }
    }

    private static void drawVerticalRedGradient(GuiGraphics g, int x, int y, int width, int height, float maxAlpha, boolean topToBottom) {
        int steps = 20;
        int stepHeight = Math.max(1, height / steps);
        for (int i = 0; i < steps; i++) {
            float t = (float) i / steps;
            float alpha = topToBottom ? (1.0F - t) * maxAlpha : t * maxAlpha;
            int alphaInt = (int) (alpha * 255);
            if (alphaInt <= 0) continue;
            int color = (alphaInt << 24) | 0xFF0000;
            g.fill(x, y + i * stepHeight, x + width, y + (i + 1) * stepHeight, color);
        }
    }

    private static void drawHorizontalRedGradient(GuiGraphics g, int x, int y, int width, int height, float maxAlpha, boolean leftToRight) {
        int steps = 20;
        int stepWidth = Math.max(1, width / steps);
        for (int i = 0; i < steps; i++) {
            float t = (float) i / steps;
            float alpha = leftToRight ? (1.0F - t) * maxAlpha : t * maxAlpha;
            int alphaInt = (int) (alpha * 255);
            if (alphaInt <= 0) continue;
            int color = (alphaInt << 24) | 0xFF0000;
            g.fill(x + i * stepWidth, y, x + (i + 1) * stepWidth, y + height, color);
        }
    }
}
