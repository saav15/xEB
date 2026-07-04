package org.xeb.xeb.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DoomfistHUDOverlay {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || mc.options.hideGui) return;

            Player player = mc.player;
            
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean holdsV1 = mainHand.is(org.xeb.xeb.item.ModItems.DOOMFIST.get()) || offHand.is(org.xeb.xeb.item.ModItems.DOOMFIST.get());
            boolean holdsV2 = mainHand.is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get()) || offHand.is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get());
            
            if (holdsV1 || holdsV2) {
                renderAbilityCooldowns(event, mc, player, holdsV2);
                renderDamageMitigationFlash(event, mc, player);
            }

            boolean isUsingV1 = player.isUsingItem() && player.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST.get());
            boolean isUsingV2 = player.isUsingItem() && player.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get());

            if (isUsingV1 || isUsingV2) {
                int ticksUsing = player.getTicksUsingItem();
                float progress = Math.min(50.0F, ticksUsing) / 50.0F; // 2.5s = 50 ticks

                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();

                int barW = 49;
                int barH = 4;
                int x = width / 2 - barW / 2;
                int y = height / 2 + 15;

                GuiGraphics g = event.getGuiGraphics();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                int segmentCount = 4;
                int segmentW = 10;
                int segmentSpacing = 3;

                int fillColor;
                if (isUsingV2) {
                    // Volcanic red charging color
                    if (progress >= 1.0F) {
                        float pulse = 0.6F + 0.4F * (float) Math.sin((mc.level.getGameTime() + event.getPartialTick()) * 0.4D);
                        int r = (int) (255 * pulse + 55);
                        fillColor = (0xFF << 24) | (r << 16) | (0 << 8) | 0;
                    } else {
                        fillColor = 0xFFFF4444;
                    }
                } else {
                    // Ice blue charging color for V1
                    if (progress >= 1.0F) {
                        float pulse = 0.6F + 0.4F * (float) Math.sin((mc.level.getGameTime() + event.getPartialTick()) * 0.4D);
                        int r = (int) (100 * pulse);
                        int gVal = (int) (200 * pulse + 55);
                        int b = 255;
                        fillColor = (0xFF << 24) | (r << 16) | (gVal << 8) | b;
                    } else {
                        fillColor = 0xFFE5F5FF;
                    }
                }

                for (int i = 0; i < segmentCount; i++) {
                    float segmentThreshold = (i + 1) / (float) segmentCount;
                    int segX = x + i * (segmentW + segmentSpacing);

                    drawSlantedBar(g, segX - 1, y - 1, segmentW + 2, barH + 2, 0xFF000000);
                    drawSlantedBar(g, segX, y, segmentW, barH, 0x88222222);

                    if (progress >= segmentThreshold) {
                        drawSlantedBar(g, segX, y, segmentW, barH, fillColor);
                    } else if (progress > i / (float) segmentCount) {
                        float segmentProgress = (progress - (i / (float) segmentCount)) * segmentCount;
                        int partialW = (int) (segmentW * segmentProgress);
                        drawSlantedBar(g, segX, y, partialW, barH, fillColor);
                    }
                }

                RenderSystem.disableBlend();
            }
        }
    }

    private static void drawSlantedBar(GuiGraphics g, int x, int y, int width, int height, int color) {
        for (int dy = 0; dy < height; dy++) {
            int offset = (height - 1 - dy);
            g.fill(x + offset, y + dy, x + width + offset, y + dy + 1, color);
        }
    }

    private static void renderAbilityCooldowns(RenderGuiOverlayEvent.Post event, Minecraft mc, Player player, boolean isV2) {
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        
        int xStart = org.xeb.xeb.Config.opticBlastHudX;
        int yStart = height - org.xeb.xeb.Config.opticBlastHudY;
        
        GuiGraphics g = event.getGuiGraphics();
        net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
        
        int uppercutCD = tag.contains("xebUppercutCooldownTicks") ? tag.getInt("xebUppercutCooldownTicks") : 0;
        int slamCD = tag.contains("xebSlamCooldownTicks") ? tag.getInt("xebSlamCooldownTicks") : 0;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        String key1 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_1_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        String key2 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_2_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        
        if (isV2) {
            // Earthquake Slam: 7s cooldown (140 ticks)
            renderAbilityIconBox(g, mc, xStart, yStart, key1, "ESLAM", uppercutCD, 140, true);
            // Power Block: 7s cooldown (140 ticks)
            renderAbilityIconBox(g, mc, xStart + 30, yStart, key2, "BLOCK", slamCD, 140, true);
        } else {
            // Rising Uppercut: 5s cooldown (100 ticks)
            renderAbilityIconBox(g, mc, xStart, yStart, key1, "UPPER", uppercutCD, 100, false);
            // Seismic Slam: 6s cooldown (120 ticks)
            renderAbilityIconBox(g, mc, xStart + 30, yStart, key2, "SLAM", slamCD, 120, false);
        }
        
        RenderSystem.disableBlend();
    }
    
    private static void renderAbilityIconBox(GuiGraphics g, Minecraft mc, int x, int y, String key, String label, int cd, int maxCd, boolean isV2) {
        int boxW = 24;
        int boxH = 24;
        
        g.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF000000);
        g.fill(x, y, x + boxW, y + boxH, 0x44000000);
        
        if (cd > 0) {
            float ratio = cd / (float) maxCd;
            int overlayH = (int) (boxH * ratio);
            g.fill(x, y + boxH - overlayH, x + boxW, y + boxH, 0x99555555);
        }
        
        int borderColor = (cd > 0) ? 0x44FFFFFF : 0xBBFFFFFF;
        g.fill(x, y, x + boxW, y + 1, borderColor);
        g.fill(x, y + boxH - 1, x + boxW, y + boxH, borderColor);
        g.fill(x, y, x + 1, y + boxH, borderColor);
        g.fill(x + boxW - 1, y, x + boxW, y + boxH, borderColor);
        
        int keyColor = (cd > 0) ? 0x88AAAAAA : 0xFFFFFFFF;
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
        
        int labelColor = (cd > 0) ? 0x44FFFFFF : 0x88FFFFFF;
        g.drawString(mc.font, label, x, y - 9, labelColor, false);
        
        if (cd > 0) {
            String timeText = String.format("%.1fs", cd / 20.0F);
            int timeX = x + (boxW - mc.font.width(timeText)) / 2;
            int timerColor = isV2 ? 0xFFFFAA00 : 0xFFE5F5FF; // Orange for V2, blue/white for V1
            g.drawString(mc.font, timeText, timeX, y + boxH + 2, timerColor, true);
        }
    }

    private static void renderDamageMitigationFlash(RenderGuiOverlayEvent.Post event, Minecraft mc, Player player) {
        net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
        if (tag.contains("xebBlockFlashTicks")) {
            int flashTicks = tag.getInt("xebBlockFlashTicks");
            if (flashTicks > 0) {
                // Decrement client-side flash ticks
                tag.putInt("xebBlockFlashTicks", flashTicks - 1);

                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();

                GuiGraphics g = event.getGuiGraphics();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                // Compute red fade alpha: 25 maximum ticks now
                float progress = Math.min(25.0F, flashTicks) / 25.0F;
                int alpha = (int) (180.0F * progress);
                int tintColor = (alpha / 4 << 24) | 0xFF0000;

                // Screen border flash (concentric glowing borders - outer is brightest/thickest)
                int edgeColor = (alpha << 24) | 0xFF0000;
                int innerEdgeColor = ((alpha * 2 / 3) << 24) | 0xDD0000;
                int coreEdgeColor = ((alpha * 1 / 3) << 24) | 0x990000;

                // Border layers:
                // Outer 4px
                g.fill(0, 0, 4, height, edgeColor);
                g.fill(width - 4, 0, width, height, edgeColor);
                g.fill(0, 0, width, 4, edgeColor);
                g.fill(0, height - 4, width, height, edgeColor);

                // Middle 4px (total 8px)
                g.fill(4, 4, 8, height - 4, innerEdgeColor);
                g.fill(width - 8, 4, width - 4, height - 4, innerEdgeColor);
                g.fill(4, 4, width - 4, 8, innerEdgeColor);
                g.fill(4, height - 8, width - 4, height - 4, innerEdgeColor);

                // Inner 4px (total 12px)
                g.fill(8, 8, 12, height - 8, coreEdgeColor);
                g.fill(width - 12, 8, width - 8, height - 8, coreEdgeColor);
                g.fill(8, 8, width - 8, 12, coreEdgeColor);
                g.fill(8, height - 12, width - 8, height - 8, coreEdgeColor);

                // Screen soft red tint
                g.fill(0, 0, width, height, tintColor);

                RenderSystem.disableBlend();
            } else {
                tag.remove("xebBlockFlashTicks");
            }
        }
    }
}
