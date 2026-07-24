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
            boolean holdsOptic = mainHand.is(org.xeb.xeb.item.ModItems.OPTIC_BLAST.get()) || offHand.is(org.xeb.xeb.item.ModItems.OPTIC_BLAST.get());
            boolean holdsFlower = mainHand.is(org.xeb.xeb.item.ModItems.GOLDEN_FLOWER.get()) || offHand.is(org.xeb.xeb.item.ModItems.GOLDEN_FLOWER.get());
            boolean holdsCD = mainHand.is(org.xeb.xeb.item.ModItems.BROKEN_DIAMOND.get()) || offHand.is(org.xeb.xeb.item.ModItems.BROKEN_DIAMOND.get());
            boolean holdsTears = mainHand.is(org.xeb.xeb.item.ModItems.THE_TEARS.get()) || offHand.is(org.xeb.xeb.item.ModItems.THE_TEARS.get());
            boolean holdsMecha = mainHand.getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem || offHand.getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem;
            boolean holdsHoly = mainHand.getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem || offHand.getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem;
            // Find equipped Extreme Burst (works regardless of Curios presence)
            org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry =
                    org.xeb.xeb.extremeburst.ExtremeBurstRegistry.findActiveBurst(player);
            boolean hasUltimate = burstEntry != null;
            
            if (holdsV1 || holdsV2) {
                renderAbilityCooldowns(event, mc, player, holdsV2, burstEntry, mainHand, offHand);
                renderDamageMitigationFlash(event, mc, player);
            } else if (holdsCD) {
                renderCrazyDiamondAbilityCooldowns(event, mc, player, burstEntry, mainHand, offHand);
                renderCrazyDiamondFistCharges(event, mc, player);
            } else if (holdsTears) {
                renderTheTearsAbilityCooldowns(event, mc, player, burstEntry, mainHand, offHand);
            } else if (holdsMecha) {
                renderMechaAbilityCooldowns(event, mc, player, burstEntry, mainHand, offHand);
            } else if (holdsHoly) {
                renderHolyAbilityCooldowns(event, mc, player, burstEntry, mainHand, offHand);
            } else if (!holdsOptic && !holdsFlower && !holdsCD && !holdsTears && !holdsMecha && !holdsHoly
                    && shouldShowBurstHUD(player, burstEntry, mainHand, offHand)) {
                renderUltimateBox(event.getGuiGraphics(), mc, player, event.getWindow().getGuiScaledHeight(), burstEntry);
            }


            // Render height-based extra damage indicator during active Seismic Slam (V1)
            if (holdsV1 && player.getPersistentData().getInt("xebSlamState") == 2 && player.getPersistentData().contains("xebSlamStartY")) {
                double startY = player.getPersistentData().getDouble("xebSlamStartY");
                double currentY = player.getY();
                double heightFallen = Math.max(0.0D, startY - currentY);
                int extraDamage = (int) Math.round(heightFallen * 1.5D);
                
                if (extraDamage > 0) {
                    GuiGraphics g = event.getGuiGraphics();
                    int width = event.getWindow().getGuiScaledWidth();
                    int height = event.getWindow().getGuiScaledHeight();
                    String text = "[ " + extraDamage + " ]";
                    int textWidth = mc.font.width(text);
                    int drawX = width / 2 - textWidth / 2;
                    int drawY = height / 2 + 5; // Lowered a bit below crosshair to avoid overlap
                    
                    g.drawString(mc.font, text, drawX, drawY, 0xFFE5F5FF, true);
                }
            }

            boolean isUsingV1 = player.isUsingItem() && player.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST.get());
            boolean isUsingV2 = player.isUsingItem() && player.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get());

            if (isUsingV1 || isUsingV2) {
                int ticksUsing = player.getTicksUsingItem();
                float chargeSpeed = (isUsingV1 && player.getPersistentData().getBoolean("xebUppercutEmpoweredPunch")) ? 1.3F : 1.0F;
                float progress = Math.min(50.0F, ticksUsing * chargeSpeed) / 50.0F; // 2.5s = 50 ticks

                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();

                int barW = 49;
                int barH = 4;
                int x = width / 2 - barW / 2 + org.xeb.xeb.Config.doomfistHudX;
                int y = height / 2 + 15 + org.xeb.xeb.Config.doomfistHudY;

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

    public static void drawSlantedBar(GuiGraphics g, int x, int y, int width, int height, int color) {
        for (int dy = 0; dy < height; dy++) {
            int offset = (height - 1 - dy);
            g.fill(x + offset, y + dy, x + width + offset, y + dy + 1, color);
        }
    }

    public static void renderActiva1Box(GuiGraphics g, Minecraft mc, int screenH, String key, String label, int cd, int maxCd, boolean isV2) {
        int xStart = org.xeb.xeb.Config.activa1HudX;
        int yStart = screenH - org.xeb.xeb.Config.activa1HudY;
        float scale = org.xeb.xeb.Config.activa1HudScale;

        g.pose().pushPose();
        g.pose().translate(xStart, yStart, 0);
        g.pose().scale(scale, scale, 1.0f);

        renderAbilityIconBox(g, mc, 0, 0, key, label, cd, maxCd, isV2);

        g.pose().popPose();
    }

    public static void renderActiva2Box(GuiGraphics g, Minecraft mc, int screenH, String key, String label, int cd, int maxCd, boolean isV2) {
        int xStart = org.xeb.xeb.Config.activa2HudX;
        int yStart = screenH - org.xeb.xeb.Config.activa2HudY;
        float scale = org.xeb.xeb.Config.activa2HudScale;

        g.pose().pushPose();
        g.pose().translate(xStart, yStart, 0);
        g.pose().scale(scale, scale, 1.0f);

        renderAbilityIconBox(g, mc, 0, 0, key, label, cd, maxCd, isV2);

        g.pose().popPose();
    }

    private static void renderAbilityCooldowns(RenderGuiOverlayEvent.Post event, Minecraft mc, Player player, boolean isV2,
                                               org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry,
                                               ItemStack mainHand, ItemStack offHand) {
        int height = event.getWindow().getGuiScaledHeight();
        GuiGraphics g = event.getGuiGraphics();
        net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
        
        int uppercutCD = tag.contains("xebUppercutCooldownTicks") ? tag.getInt("xebUppercutCooldownTicks") : 0;
        int slamCD = tag.contains("xebSlamCooldownTicks") ? tag.getInt("xebSlamCooldownTicks") : 0;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        String key1 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_1_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        String key2 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_2_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        
        if (isV2) {
            renderActiva1Box(g, mc, height, key1, "ESLAM", uppercutCD, 140, true);
            renderActiva2Box(g, mc, height, key2, "BLOCK", slamCD, 140, true);
        } else {
            renderActiva1Box(g, mc, height, key1, "UPPER", uppercutCD, 100, false);
            renderActiva2Box(g, mc, height, key2, "SLAM", slamCD, 120, false);
        }
        
        if (shouldShowBurstHUD(player, burstEntry, mainHand, offHand)) {
            renderUltimateBox(g, mc, player, height, burstEntry);
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

    /**
     * Returns true only when ALL conditions are met:
     * 1. {@code entry} is non-null — a burst item was found.
     * 2. The item is equipped in a Curios slot (not just in inventory/offhand).
     * 3. For {@link org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstType#UNIVERSAL}: always true.
     *    For {@link org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstType#LIMITED}: the player
     *    must also be holding the required weapon in main or offhand.
     */
    public static boolean shouldShowBurstHUD(Player player,
                                       org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry entry,
                                       ItemStack mainHand, ItemStack offHand) {
        if (entry == null) return false;
        // Enforce curio slot — items only in inventory/offhand don't show the HUD
        if (!org.xeb.xeb.extremeburst.ExtremeBurstRegistry.isInCurioSlot(player, entry)) return false;
        // UNIVERSAL: show regardless of held weapon
        if (entry.type == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstType.UNIVERSAL) return true;
        // LIMITED: only show when holding the required weapon
        return org.xeb.xeb.extremeburst.ExtremeBurstRegistry.canActivate(player, entry);
    }

    public static void renderUltimateBox(GuiGraphics g, Minecraft mc, Player player, int screenH,
                                         org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry entry) {
        int xStart = org.xeb.xeb.Config.activa3HudX;
        int yStart = screenH - org.xeb.xeb.Config.activa3HudY;
        float scale = org.xeb.xeb.Config.activa3HudScale;

        g.pose().pushPose();
        g.pose().translate(xStart, yStart, 0);
        g.pose().scale(scale, scale, 1.0f);

        renderUltimateBoxAt(g, mc, player, 0, 0, entry);

        g.pose().popPose();
    }

    public static void renderUltimateBoxAt(GuiGraphics g, Minecraft mc, Player player, int x, int y,
                                           org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry entry) {
        int boxW = 24;
        int boxH = 24;
        
        g.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF000000);
        g.fill(x, y, x + boxW, y + boxH, 0x44000000);
        
        // Show the actual equipped burst item icon
        ItemStack curioStack = new ItemStack(entry.curioItem);
        g.renderItem(curioStack, x + 4, y + 4);
        
        // Read cooldown from the actual equipped burst item
        float cdPercent = player.getCooldowns().getCooldownPercent(entry.curioItem, 0.0f);
        if (cdPercent > 0.0f) {
            int overlayH = (int) (boxH * cdPercent);
            g.fill(x, y + boxH - overlayH, x + boxW, y + boxH, 0x99555555);
            
            // Use the entry's actual cooldown duration instead of hardcoded 180s
            float secondsLeft = cdPercent * (entry.cooldownTicks / 20.0F);
            String timerText = String.format("%d", (int) Math.ceil(secondsLeft));
            int textW = mc.font.width(timerText);
            g.drawString(mc.font, timerText, x + (boxW - textW) / 2, y + (boxH - mc.font.lineHeight) / 2, 0xFFFF5555, true);
        }
        
        int borderColor = (cdPercent > 0.0f) ? 0x44FFFFFF : 0xBBFFFFFF;
        g.fill(x, y, x + boxW, y + 1, borderColor);
        g.fill(x, y + boxH - 1, x + boxW, y + boxH, borderColor);
        g.fill(x, y, x + 1, y + boxH, borderColor);
        g.fill(x + boxW - 1, y, x + boxW, y + boxH, borderColor);
        
        String key = org.xeb.xeb.client.ModKeyMappings.ACTIVA_3_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        g.drawString(mc.font, key, x + 2, y + boxH - 9, 0xFFFFFFFF, true);
    }

    private static void renderCrazyDiamondAbilityCooldowns(RenderGuiOverlayEvent.Post event, Minecraft mc, Player player,
                                                       org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry,
                                                       ItemStack mainHand, ItemStack offHand) {
        int height = event.getWindow().getGuiScaledHeight();
        GuiGraphics g = event.getGuiGraphics();
        net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
        
        int a1CD = tag.contains("xebCDA1CooldownTicks") ? tag.getInt("xebCDA1CooldownTicks") : 0;
        int a2CD = tag.contains("xebCDA2CooldownTicks") ? tag.getInt("xebCDA2CooldownTicks") : 0;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        String key1 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_1_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        String key2 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_2_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        
        renderActiva1Box(g, mc, height, key1, "DORA", a1CD, 300, false);
        renderActiva2Box(g, mc, height, key2, "RESTO", a2CD, 300, false);
        
        if (shouldShowBurstHUD(player, burstEntry, mainHand, offHand)) {
            renderUltimateBox(g, mc, player, height, burstEntry);
        }
        
        RenderSystem.disableBlend();
    }

    private static void renderCrazyDiamondFistCharges(RenderGuiOverlayEvent.Post event, Minecraft mc, Player player) {
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        
        int cx = width / 2 + org.xeb.xeb.Config.crazyDiamondHudX;
        int cy = height / 2 + org.xeb.xeb.Config.crazyDiamondHudY;
        
        // Honeycomb layout
        int[][] cellPositions = {
            {cx + 15, cy - 4},
            {cx + 23, cy - 9},
            {cx + 23, cy + 1}
        };
        
        GuiGraphics g = event.getGuiGraphics();
        net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
        
        int punches = tag.getInt("xebCDPunches");
        int chargeTimer = tag.getInt("xebCDChargeTimer");
        int activeBarrageTimer = tag.getInt("xebCDBarrageTimer");
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        for (int i = 0; i < 3; i++) {
            int x = cellPositions[i][0];
            int y = cellPositions[i][1];
            drawHoneycombCell(g, x, y, punches, chargeTimer, i, activeBarrageTimer);
        }
        
        RenderSystem.disableBlend();
    }

    public static void drawHoneycombCell(GuiGraphics g, int x, int y, int punches, int chargeTimer, int cellIndex, int activeBarrageTimer) {
        // Border outline (black)
        g.fill(x + 3, y,     x + 7, y + 1, 0xFF000000);
        g.fill(x + 2, y + 1, x + 8, y + 2, 0xFF000000);
        g.fill(x + 1, y + 2, x + 9, y + 3, 0xFF000000);
        g.fill(x,     y + 3, x + 10, y + 5, 0xFF000000);
        g.fill(x + 1, y + 5, x + 9, y + 6, 0xFF000000);
        g.fill(x + 2, y + 6, x + 8, y + 7, 0xFF000000);
        g.fill(x + 3, y + 7, x + 7, y + 8, 0xFF000000);
        
        // Empty inside (dark grey transparent)
        g.fill(x + 3, y + 1, x + 7, y + 2, 0x44000000);
        g.fill(x + 2, y + 2, x + 8, y + 3, 0x44000000);
        g.fill(x + 1, y + 3, x + 9, y + 5, 0x44000000);
        g.fill(x + 2, y + 5, x + 8, y + 6, 0x44000000);
        g.fill(x + 3, y + 6, x + 7, y + 7, 0x44000000);
        
        boolean isCharged = cellIndex < punches;
        boolean isCharging = cellIndex == punches && activeBarrageTimer <= 0;
        
        int fillColor = 0xFFFF55AA; // pink
        int fistColor = 0xFF888888;
        
        if (isCharged) {
            fistColor = 0xFFFFFFFF;
            // Solid pink inside
            g.fill(x + 3, y + 1, x + 7, y + 2, fillColor);
            g.fill(x + 2, y + 2, x + 8, y + 3, fillColor);
            g.fill(x + 1, y + 3, x + 9, y + 5, fillColor);
            g.fill(x + 2, y + 5, x + 8, y + 6, fillColor);
            g.fill(x + 3, y + 6, x + 7, y + 7, fillColor);
        } else if (isCharging) {
            fistColor = 0xFF555555;
            float progress = Math.min(1.0F, chargeTimer / 60.0F);
            int fillH = (int) (6 * progress); // 0 to 6 rows from bottom
            
            // Fill rows from bottom
            if (fillH >= 1) g.fill(x + 3, y + 6, x + 7, y + 7, 0xFF66CCFF); // row 6
            if (fillH >= 2) g.fill(x + 2, y + 5, x + 8, y + 6, 0xFF66CCFF); // row 5
            if (fillH >= 3) g.fill(x + 1, y + 4, x + 9, y + 5, 0xFF66CCFF); // row 4
            if (fillH >= 4) g.fill(x + 1, y + 3, x + 9, y + 4, 0xFF66CCFF); // row 3
            if (fillH >= 5) g.fill(x + 2, y + 2, x + 8, y + 3, 0xFF66CCFF); // row 2
            if (fillH >= 6) g.fill(x + 3, y + 1, x + 7, y + 2, 0xFF66CCFF); // row 1
        }
        
                g.fill(x + 3, y + 2, x + 4, y + 3, fistColor);
                g.fill(x + 6, y + 2, x + 7, y + 3, fistColor);
                g.fill(x + 3, y + 3, x + 7, y + 5, fistColor);
                g.fill(x + 4, y + 5, x + 6, y + 6, fistColor);
            }

    private static void renderTheTearsAbilityCooldowns(RenderGuiOverlayEvent.Post event, Minecraft mc, Player player,
                                                   org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry,
                                                   ItemStack mainHand, ItemStack offHand) {
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        
        GuiGraphics g = event.getGuiGraphics();
        net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
        
        int a1CD = tag.contains("xebTearsA1Cooldown") ? tag.getInt("xebTearsA1Cooldown") : 0;
        int a2CD = tag.contains("xebTearsA2Cooldown") ? tag.getInt("xebTearsA2Cooldown") : 0;
        int imbueType = tag.contains("xebTearsImbueType") ? tag.getInt("xebTearsImbueType") : 0;
        int imbueDur = tag.contains("xebTearsImbueDuration") ? tag.getInt("xebTearsImbueDuration") : 0;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        String key1 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_1_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        String key2 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_2_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        
        // Activa 1: Playdough Cookie
        if (imbueDur > 0) {
            String label = "NONE";
            int color = 0xFFFFFFFF;
            if (imbueType == 1) { label = "PURP"; color = 0xFFB000FF; }
            else if (imbueType == 2) { label = "WHIT"; color = 0xFFFFFFFF; }
            else if (imbueType == 3) { label = "DARK"; color = 0xFF404040; }
            else if (imbueType == 4) { label = "COLD"; color = 0xFF90E0FF; }
            
            String secsLeft = String.format("%.1fs", imbueDur / 20.0F);
            g.pose().pushPose();
            g.pose().translate(org.xeb.xeb.Config.activa1HudX, height - org.xeb.xeb.Config.activa1HudY, 0);
            g.pose().scale(org.xeb.xeb.Config.activa1HudScale, org.xeb.xeb.Config.activa1HudScale, 1.0f);
            renderAbilityIconBoxWithCustomColor(g, mc, 0, 0, secsLeft, label, color);
            g.pose().popPose();
        } else {
            renderActiva1Box(g, mc, height, key1, "COOKI", a1CD, 300, false);
        }
        
        // Activa 2: Camo Undies
        renderActiva2Box(g, mc, height, key2, "CAMO", a2CD, 400, false);
        
        if (shouldShowBurstHUD(player, burstEntry, mainHand, offHand)) {
            renderUltimateBox(g, mc, player, height, burstEntry);
        }
        
        RenderSystem.disableBlend();

        // Circular crosshair HUD next to crosshair (only renders when charging or firing Brimstone)
        int charge = tag.getInt("xebBrimstoneCharge");
        boolean instant = tag.getBoolean("xebNextBrimstoneInstant");
        int firing = tag.getInt("xebBrimstoneFiringTicks");

        if (charge > 0 || instant || firing > 0) {
            int centerX = width / 2;
            int centerY = height / 2;
            int hudX = centerX + org.xeb.xeb.Config.theTearsHudX;
            int hudY = centerY + org.xeb.xeb.Config.theTearsHudY;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // Draw base dark donut
            drawDonut(g, hudX, hudY, 6, 2, 0x80202020);
            
            if (firing > 0) {
                // Firing: pulsing green donut
                int pulseColor = ((mc.level.getGameTime() % 10) < 5) ? 0xFF00FF00 : 0xFF00AA00;
                drawDonut(g, hudX, hudY, 6, 2, pulseColor);
            } else if (instant) {
                // Next Brimstone instant: fully green donut
                drawDonut(g, hudX, hudY, 6, 2, 0xFF00FF00);
            } else if (charge > 0) {
                // Charging: green clockwise fill progress
                float progress = Math.min(1.0F, (float) charge / 80.0F);
                drawDonutProgress(g, hudX, hudY, 6, 2, progress, 0xFF00FF00);
            }
            
            // Inner colored ring wrapping the donut HUD
            if (imbueType > 0) {
                int ringColor = 0xFFFFFFFF;
                if (imbueType == 1) ringColor = 0xFFB000FF; // Purple
                else if (imbueType == 2) ringColor = 0xFFFFFFFF; // White
                else if (imbueType == 3) ringColor = 0xFF352535; // Visible dark void
                else if (imbueType == 4) ringColor = 0xFF90E0FF; // Cold
                
                drawDonut(g, hudX, hudY, 4, 2, ringColor);
            }
            
            RenderSystem.disableBlend();
        }
    }

    private static void renderAbilityIconBoxWithCustomColor(GuiGraphics g, Minecraft mc, int x, int y, String key, String label, int color) {
        int boxW = 24;
        int boxH = 24;
        
        g.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF000000);
        g.fill(x, y, x + boxW, y + boxH, 0x44000000);
        
        g.fill(x, y, x + boxW, y + 1, color);
        g.fill(x, y + boxH - 1, x + boxW, y + boxH, color);
        g.fill(x, y, x + 1, y + boxH, color);
        g.fill(x + boxW - 1, y, x + boxW, y + boxH, color);
        
        int keyColor = 0xFFFFFFFF;
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
        
        g.drawString(mc.font, label, x, y - 9, color, false);
    }

    public static void drawDonutPublic(GuiGraphics g, int cx, int cy, int radius, int thickness, int color) {
        drawDonut(g, cx, cy, radius, thickness, color);
    }

    private static void drawDonut(GuiGraphics g, int cx, int cy, int radius, int thickness, int color) {
        for (int x = -radius - thickness; x <= radius + thickness; x++) {
            for (int y = -radius - thickness; y <= radius + thickness; y++) {
                double dist = Math.sqrt(x*x + y*y);
                if (dist >= radius && dist < radius + thickness) {
                    g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
                }
            }
        }
    }

    private static void drawDonutProgress(GuiGraphics g, int cx, int cy, int radius, int thickness, float progress, int color) {
        double maxAngle = progress * 2.0D * Math.PI - Math.PI / 2.0D;
        for (int x = -radius - thickness; x <= radius + thickness; x++) {
            for (int y = -radius - thickness; y <= radius + thickness; y++) {
                double dist = Math.sqrt(x*x + y*y);
                if (dist >= radius && dist < radius + thickness) {
                    double angle = Math.atan2(y, x);
                    if (angle < -Math.PI / 2.0D) {
                        angle += 2.0D * Math.PI;
                    }
                    if (angle <= maxAngle) {
                        g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
                    }
                }
            }
        }
    }

    private static void renderMechaAbilityCooldowns(RenderGuiOverlayEvent.Post event, Minecraft mc, Player player,
                                                org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry,
                                                ItemStack mainHand, ItemStack offHand) {
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        
        GuiGraphics g = event.getGuiGraphics();
        net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
        
        int a1CD = tag.getInt("xebMechaA1Cooldown");
        boolean jetActive = tag.getBoolean("xebMechaOverdriveDashing");
        
        int spindashCharges = tag.getInt("xebSpindashCharges");
        if (!tag.contains("xebSpindashCharges")) {
            spindashCharges = 3;
        }
        int spindashTimer = tag.getInt("xebSpindashRechargeTimer");
        boolean spindashActive = tag.getInt("xebMechaSpindashState") > 0;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        String key1 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_1_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        String key2 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_2_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        
        // Activa 1: Jet Dash (JETS)
        renderActiva1Box(g, mc, height, key1, "JETS", a1CD, 160, jetActive);
        
        // Activa 2: Spindash Missile (SPMS)
        g.pose().pushPose();
        g.pose().translate(org.xeb.xeb.Config.activa2HudX, height - org.xeb.xeb.Config.activa2HudY, 0);
        g.pose().scale(org.xeb.xeb.Config.activa2HudScale, org.xeb.xeb.Config.activa2HudScale, 1.0f);
        renderSpindashBox(g, mc, 0, 0, key2, "SPMS", spindashCharges, spindashTimer, spindashActive);
        g.pose().popPose();
        
        if (shouldShowBurstHUD(player, burstEntry, mainHand, offHand)) {
            renderUltimateBox(g, mc, player, height, burstEntry);
        }
        
        RenderSystem.disableBlend();

        // Render Momentum Bar next to crosshair
        int centerX = width / 2 + org.xeb.xeb.Config.mechaHudX;
        int centerY = height / 2 + org.xeb.xeb.Config.mechaHudY;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        double momentum = tag.getDouble("xebMechaMomentum");
        boolean overcharged = tag.getBoolean("xebMechaOvercharged");
        boolean levitating = tag.getBoolean("xebMechaLevitating");
        
        // Futuristic Slanted Segmented Gauge next to crosshairs
        int segmentsCount = 5;
        double segmentMax = 2.0D;
        double step = segmentMax / segmentsCount; // 0.4 per segment
        
        for (int i = 0; i < segmentsCount; i++) {
            // Slanted offset based on segment index
            int segX = centerX + 18 + i;
            int segY = centerY + 14 - i * 8;
            int segW = 6;
            int segH = 5;
            
            // Draw segment background
            g.fill(segX - 1, segY - 1, segX + segW + 1, segY + segH + 1, 0x66000000);
            
            // Check if active
            double required = (i + 1) * step;
            boolean active = momentum >= (required - 0.05D);
            
            if (active) {
                int fillColor = 0xFFFF8800; // default orange
                if (i == 0) fillColor = 0xFFFF3300;
                else if (i == 1) fillColor = 0xFFFF6600;
                else if (i == 2) fillColor = 0xFFFF9900;
                else if (i == 3) fillColor = 0xFFFFCC00;
                else if (i == 4) fillColor = 0xFFFFFF00;
                
                if (overcharged) {
                    float pulse = 0.7F + 0.3F * (float) Math.sin((mc.level.getGameTime() + event.getPartialTick()) * 0.5D);
                    int red = (int) (255 * pulse);
                    int grn = (int) (200 * pulse * (i / 4.0F)); // gold to orange gradient pulse
                    fillColor = (0xFF << 24) | (red << 16) | (grn << 8) | 0;
                }
                g.fill(segX, segY, segX + segW, segY + segH, fillColor);
            }
        }
        
        // Text indicator: O.CLOCK
        int momColor = overcharged ? 0xFFFFD700 : 0xFFFF5500;
        g.drawString(mc.font, "O.CLOCK", centerX + 18, centerY - 28, momColor, true);
        
        // Status labels
        if (overcharged) {
            g.drawString(mc.font, "OVERCHARGE", centerX + 28, centerY - 5, 0xFFFFD700, true);
            int barTicks = tag.getInt("xebMechaOverchargeTicks");
            int barW = 24;
            int barH = 2;
            int barX = centerX + 28;
            int barY = centerY + 4;
            float pct = Math.max(0.0F, Math.min(1.0F, (barTicks - 60) / 40.0F)); // scale 60-100 to 0.0-1.0
            g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0x66000000);
            g.fill(barX, barY, barX + (int)(barW * pct), barY + barH, 0xFFFFD700);
        } else if (levitating) {
            g.drawString(mc.font, "LEVITATING", centerX + 28, centerY - 5, 0xFFFFDD00, true);
        }
        
        RenderSystem.disableBlend();
    }

    private static void renderHolyAbilityCooldowns(RenderGuiOverlayEvent.Post event, Minecraft mc, Player player,
                                                org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry,
                                                ItemStack mainHand, ItemStack offHand) {
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        GuiGraphics g = event.getGuiGraphics();
        net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
        
        int a1CD = tag.getInt("xebHolyA1Cooldown");
        int a2CD = tag.getInt("xebHolyA2Cooldown");
        boolean shieldActive = tag.getBoolean("xebHolyShieldActive");
        boolean annihilationActive = tag.getBoolean("xebHolyAnnihilationActive");
        int blastCharge = tag.getInt("xebHolyBlastCharge");
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        String key1 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_1_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        String key2 = org.xeb.xeb.client.ModKeyMappings.ACTIVA_2_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        
        // Action boxes bottom left
        renderActiva1Box(g, mc, height, key1, "CREAT", a1CD, 320, shieldActive);
        renderActiva2Box(g, mc, height, key2, "ANNIL", a2CD, 133, annihilationActive);
        
        if (shouldShowBurstHUD(player, burstEntry, mainHand, offHand)) {
            renderUltimateBox(g, mc, player, height, burstEntry);
        }
        
        RenderSystem.disableBlend();

        // Render Holy Blast Charge Bar below crosshair
        if (blastCharge > 0) {
            int centerX = width / 2 + org.xeb.xeb.Config.holyHudX;
            int centerY = height / 2 + org.xeb.xeb.Config.holyHudY;
            int barX = centerX - 20;
            int barY = centerY + 12;
            int barW = 40;
            int barH = 4;
            
            float pct = Math.min(1.0F, blastCharge / 160.0F); // Max charge is 160 ticks
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // Draw background
            g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0x66000000);
            
            // Draw fill (aqua blue/white)
            int fillW = (int) (barW * pct);
            g.fill(barX, barY, barX + fillW, barY + barH, 0xFF00FFFF);
            
            if (pct >= 1.0F) {
                // Glow border
                g.fill(barX - 1, barY - 1, barX + barW + 1, barY, 0xFFFFFFFF);
                g.fill(barX - 1, barY + barH, barX + barW + 1, barY + barH + 1, 0xFFFFFFFF);
                g.fill(barX - 1, barY - 1, barX, barY + barH + 1, 0xFFFFFFFF);
                g.fill(barX + barW, barY - 1, barX + barW + 1, barY + barH + 1, 0xFFFFFFFF);
            }

            RenderSystem.disableBlend();
        }
    }

    private static void renderSpindashBox(GuiGraphics g, Minecraft mc, int x, int y,
                                          String key, String label, int charges, int timer,
                                          boolean isActive) {
        int boxW = 24;
        int boxH = 24;

        // Background
        g.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF000000);

        if (isActive) {
            float pulse = 0.5F + 0.5F * (float) Math.sin(mc.level.getGameTime() * 0.4D);
            int color = (0xBB << 24) | (((int)(255 * pulse)) << 16) | (((int)(120 * pulse)) << 8) | 0; // glowing orange/red
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
        if (isActive) borderColor = 0xFFFF6600; // Orange border
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
        g.drawString(mc.font, chargeStr, x + boxW - chargeW - 1, y + boxH - 9, charges > 0 ? 0xFFFFDD00 : 0xFFFF3300, true);

        // Cooldown timer below the box if recharging
        if (charges < 3 && !isActive) {
            String timeText = String.format("%.1fs", (120 - timer) / 20.0F);
            int timeX = x + (boxW - mc.font.width(timeText)) / 2;
            g.drawString(mc.font, timeText, timeX, y + boxH + 2, 0xFFFF9900, true);
        }
    }
}
