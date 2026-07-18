package org.xeb.xeb.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import org.xeb.xeb.item.ModItems;

import java.util.ArrayList;
import java.util.List;

public class EnigmaBiosScreen extends Screen {
    private final int guiWidth = 360;
    private final int guiHeight = 260;
    private int leftPos;
    private int topPos;

    private int activeTab = 0; // 0: Analizador, 1-5: Bitácoras
    private ItemStack analyzedStack = ItemStack.EMPTY;
    private int selectedAbilityIndex = 0; // 0: Clic Izq, 1: Clic Der, 2: Activa 1, 3: Activa 2, 4: Extreme Burst

    // Lore Logs
    private final List<LogEntry> logs = new ArrayList<>();

    // Scrolling states
    private float tabScrollAmount = 0.0F;
    private float contentScrollAmount = 0.0F;
    private float analyzerScrollAmount = 0.0F;

    private boolean isDraggingTabScroll = false;
    private boolean isDraggingContentScroll = false;
    private boolean isDraggingAnalyzerScroll = false;

    // Unknown item warning flash states
    private boolean lastAnalyzedUnknown = false;
    private long lastAnalyzedTime = 0L;
    private int unknownTextIndex = 0;

    public EnigmaBiosScreen() {
        super(Component.literal("Enigma Bios"));
        initLogs();
    }

    private void initLogs() {
        logs.add(new LogEntry(
                "gui.xeb.enigma_bios.log1.title",
                "gui.xeb.enigma_bios.log1.content"
        ));
        logs.add(new LogEntry(
                "gui.xeb.enigma_bios.log2.title",
                "gui.xeb.enigma_bios.log2.content"
        ));
        logs.add(new LogEntry(
                "gui.xeb.enigma_bios.log3.title",
                "gui.xeb.enigma_bios.log3.content"
        ));
        logs.add(new LogEntry(
                "gui.xeb.enigma_bios.log4.title",
                "gui.xeb.enigma_bios.log4.content"
        ));
        logs.add(new LogEntry(
                "gui.xeb.enigma_bios.log5.title",
                "gui.xeb.enigma_bios.log5.content"
        ));
        // Medallion Buff Logs
        for (int i = 6; i <= 33; i++) {
            logs.add(new LogEntry(
                    "gui.xeb.enigma_bios.log" + i + ".title",
                    "gui.xeb.enigma_bios.log" + i + ".content"
            ));
        }
    }

    private String translate(String key) {
        return Component.translatable(key).getString();
    }

    private float getScaleFactor() {
        float scale = 1.0F;
        if (this.width >= 720 && this.height >= 520) {
            scale = 2.0F;
        } else if (this.width >= 540 && this.height >= 390) {
            scale = 1.5F;
        }
        return scale;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        float scale = getScaleFactor();
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        int scaledWidth = (int) (this.width / scale);
        int scaledHeight = (int) (this.height / scale);
        this.leftPos = (scaledWidth - this.guiWidth) / 2;
        this.topPos = (scaledHeight - this.guiHeight) / 2;

        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0F);

        // Glass tablet background (translucent dark blue)
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, 0xCE08111E);

        // Border colors logic (flashes red when scanning unknown specimen)
        int frameBorderColor = 0xFF00FFCC; // default cyan
        long elapsed = System.currentTimeMillis() - this.lastAnalyzedTime;
        boolean flashing = this.lastAnalyzedUnknown && elapsed < 800L;
        if (flashing) {
            float flash = 1.0F - (float) elapsed / 800.0F;
            int red = (int) (0x00 + (0xFF - 0x00) * flash);
            int green = (int) (0xFF + (0x33 - 0xFF) * flash);
            int blue = (int) (0xCC + (0x33 - 0xCC) * flash);
            frameBorderColor = 0xFF000000 | (red << 16) | (green << 8) | blue;
        }

        // Futuristic borders
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + 1, frameBorderColor);
        g.fill(this.leftPos, this.topPos + this.guiHeight - 1, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, frameBorderColor);
        g.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.guiHeight, frameBorderColor);
        g.fill(this.leftPos + this.guiWidth - 1, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, frameBorderColor);

        // Neon corners brackets
        g.fill(this.leftPos - 2, this.topPos - 2, this.leftPos + 8, this.topPos + 1, frameBorderColor);
        g.fill(this.leftPos - 2, this.topPos - 2, this.leftPos + 1, this.topPos + 8, frameBorderColor);
        g.fill(this.leftPos + this.guiWidth - 8, this.topPos - 2, this.leftPos + this.guiWidth + 2, this.topPos + 1, frameBorderColor);
        g.fill(this.leftPos + this.guiWidth - 1, this.topPos - 2, this.leftPos + this.guiWidth + 2, this.topPos + 8, frameBorderColor);
        g.fill(this.leftPos - 2, this.topPos + this.guiHeight - 1, this.leftPos + 8, this.topPos + this.guiHeight + 2, frameBorderColor);
        g.fill(this.leftPos - 2, this.topPos + this.guiHeight - 8, this.leftPos + 1, this.topPos + this.guiHeight + 2, frameBorderColor);
        g.fill(this.leftPos + this.guiWidth - 8, this.topPos + this.guiHeight - 1, this.leftPos + this.guiWidth + 2, this.topPos + this.guiHeight + 2, frameBorderColor);
        g.fill(this.leftPos + this.guiWidth - 1, this.topPos + this.guiHeight - 8, this.leftPos + this.guiWidth + 2, this.topPos + this.guiHeight + 2, frameBorderColor);

        // Scanline effect
        long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        int scanY = (int) ((time * 1.5) % (this.guiHeight - 20)) + this.topPos + 10;
        g.fill(this.leftPos + 2, scanY, this.leftPos + this.guiWidth - 2, scanY + 1, flashing ? 0x1AFF3333 : 0x1A00FFCC);

        // Divider between content and inventory
        g.fill(this.leftPos + 6, this.topPos + 155, this.leftPos + this.guiWidth - 6, this.topPos + 156, flashing ? 0x33FF3333 : 0x3300FFCC);

        // Title and status (statusText aligned to the right to prevent HUD overflow)
        g.drawString(this.font, translate("gui.xeb.enigma_bios.title"), this.leftPos + 10, this.topPos + 5, flashing ? 0xFFFF3333 : 0xFF00FFCC, false);
        
        String statusText = translate(flashing ? "gui.xeb.enigma_bios.status.warning" : "gui.xeb.enigma_bios.status");
        int statusW = this.font.width(statusText);
        g.drawString(this.font, statusText, this.leftPos + this.guiWidth - 10 - statusW, this.topPos + 5, flashing ? 0x88FF3333 : 0x8800FFCC, false);

        // Render sections
        renderTabs(g, scaledMouseX, scaledMouseY);
        renderContent(g, scaledMouseX, scaledMouseY, frameBorderColor, flashing, elapsed);
        renderInventory(g, scaledMouseX, scaledMouseY);

        // Tooltip rendering
        ItemStack hoveredStack = getStackAtMouse(scaledMouseX, scaledMouseY);
        if (!hoveredStack.isEmpty()) {
            g.renderTooltip(this.font, hoveredStack, scaledMouseX, scaledMouseY);
        }

        super.render(g, scaledMouseX, scaledMouseY, partialTick);
        g.pose().popPose();
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 130;
        int totalTabs = 1 + logs.size();
        int contentHeight = totalTabs * 22;
        int maxTabScroll = Math.max(0, contentHeight - viewportH);
 
        // Draw tab scrollbar if content overflows viewport
        if (maxTabScroll > 0) {
            int scrollbarX = startX + 61;
            g.fill(scrollbarX, viewportY, scrollbarX + 3, viewportY + viewportH, 0x3300FFCC);
            int thumbH = Math.max(10, (int) ((float) viewportH * viewportH / contentHeight));
            int thumbY = viewportY + (int) ((float) tabScrollAmount * (viewportH - thumbH) / maxTabScroll);
            thumbY = net.minecraft.util.Mth.clamp(thumbY, viewportY, viewportY + viewportH - thumbH);
            g.fill(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbH, 0xFF00FFCC);
        }
 
        // Draw scissored tabs
        g.enableScissor(startX, viewportY, startX + 65, viewportY + viewportH);
        for (int i = 0; i < totalTabs; i++) {
            int y = viewportY + i * 22 - (int) tabScrollAmount;
            if (y + 20 < viewportY || y > viewportY + viewportH) {
                continue;
            }
 
            boolean active = (this.activeTab == i);
            boolean hovered = mouseX >= startX && mouseX < startX + 60 && mouseY >= y && mouseY < y + 20
                    && y >= viewportY && y + 20 <= viewportY + viewportH;
 
            boolean isUnlocked = true;
            if (i >= 1 && i <= logs.size() && this.minecraft != null && this.minecraft.player != null) {
                isUnlocked = this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + i);
            }

            int bgColor;
            int textColor;
            int borderColor;

            if (!isUnlocked) {
                bgColor = active ? 0xCC550000 : (hovered ? 0x44550000 : 0x1A550000);
                textColor = active ? 0xFFFFFFFF : (hovered ? 0xFFFF7777 : 0xFF884444);
                borderColor = 0x33FF5555;
            } else {
                bgColor = active ? 0xCC00FFCC : (hovered ? 0x4400FFCC : 0x1A00FFCC);
                textColor = active ? 0xFF08111E : (hovered ? 0xFFFFFFFF : 0xFF888888);
                borderColor = 0x3300FFCC;
            }

            g.fill(startX, y, startX + 60, y + 20, bgColor);
            g.fill(startX, y, startX + 60, y + 1, borderColor);
            g.fill(startX, y + 19, startX + 60, y + 20, borderColor);
            g.fill(startX, y, startX + 1, y + 20, borderColor);
            g.fill(startX + 59, y, startX + 60, y + 20, borderColor);

            String title = (i == 0) ? translate("gui.xeb.enigma_bios.tab.analyze") : "BIT. #" + i;
            int textW = this.font.width(title);
            g.drawString(this.font, title, startX + (60 - textW) / 2, y + 6, textColor, false);
        }
        g.disableScissor();
    }

    private void renderContent(GuiGraphics g, int mouseX, int mouseY, int borderColor, boolean flashing, long elapsed) {
        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        // Content box base
        g.fill(areaX, areaY, areaX + areaW, areaY + areaH, 0x1A000000);
        g.fill(areaX, areaY, areaX + areaW, areaY + 1, borderColor);
        g.fill(areaX, areaY + areaH - 1, areaX + areaW, areaY + areaH, borderColor);
        g.fill(areaX, areaY, areaX + 1, areaY + areaH, borderColor);
        g.fill(areaX + areaW - 1, areaY, areaX + areaW, areaY + areaH, borderColor);

        // Flash red overlay on unknown scan
        if (flashing) {
            float flash = 1.0F - (float) elapsed / 800.0F;
            int alpha = (int) (flash * 50);
            g.fill(areaX + 1, areaY + 1, areaX + areaW - 1, areaY + areaH - 1, (alpha << 24) | 0xFF0000);
        }

        if (this.activeTab == 0) {
            // RENDER ANALYZER
            int slotX = areaX + 12;
            int slotY = areaY + 8;

            g.fill(slotX - 1, slotY - 1, slotX + 21, slotY + 21, borderColor);
            g.fill(slotX, slotY, slotX + 20, slotY + 20, 0xFF08111E);
            g.fill(slotX, slotY, slotX + 20, slotY + 1, borderColor);
            g.fill(slotX, slotY + 19, slotX + 20, slotY + 20, borderColor);
            g.fill(slotX, slotY, slotX + 1, slotY + 20, borderColor);
            g.fill(slotX + 19, slotY, slotX + 20, slotY + 20, borderColor);

            if (this.analyzedStack.isEmpty()) {
                String placeText = translate("gui.xeb.enigma_bios.empty.1");
                String belowText = translate("gui.xeb.enigma_bios.empty.2");
                g.drawCenteredString(this.font, placeText, areaX + areaW / 2, areaY + 50, 0xFF888888);
                g.drawCenteredString(this.font, belowText, areaX + areaW / 2, areaY + 64, 0xFF666666);
            } else {
                g.renderFakeItem(this.analyzedStack, slotX + 2, slotY + 2);
                g.renderItemDecorations(this.font, this.analyzedStack, slotX + 2, slotY + 2);

                AnalyzedInfo info = analyzeItem(this.analyzedStack);

                if (info.hasAbilities && info.isAbilityDisabled(this.selectedAbilityIndex)) {
                    for (int k = 0; k < 5; k++) {
                        if (!info.isAbilityDisabled(k)) {
                            this.selectedAbilityIndex = k;
                            this.analyzerScrollAmount = 0.0F;
                            break;
                        }
                    }
                }

                int textColor = flashing ? 0xFFFF3333 : 0xFF00FFCC;
                g.drawString(this.font, translate("gui.xeb.enigma_bios.label.name") + info.name, areaX + 38, areaY + 14, textColor, false);

                // Print Lore (top section) - Random variant index if unknown
                String loreText = info.translationKey.equals("item.unknown")
                        ? translate("item.unknown.enigma_lore." + this.unknownTextIndex)
                        : translate(info.translationKey + ".enigma_lore");
                
                int textY = areaY + 32;
                List<FormattedText> loreLines = this.font.getSplitter().splitLines(loreText, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                for (FormattedText line : loreLines) {
                    if (textY < areaY + 62) {
                        g.drawString(this.font, line.getString(), areaX + 12, textY, 0xFFE0E0E0, false);
                        textY += 10;
                    }
                }

                if (info.hasAbilities) {
                    // Ability Buttons
                    int btnY = areaY + 66;
                    int btnStartX = areaX + 12;
                    int btnW = 48;
                    int btnH = 14;
                    int btnSpacing = 6;

                    String[] btnKeys = {
                            "gui.xeb.enigma_bios.btn.left_click",
                            "gui.xeb.enigma_bios.btn.right_click",
                            "gui.xeb.enigma_bios.btn.active1",
                            "gui.xeb.enigma_bios.btn.active2",
                            "gui.xeb.enigma_bios.btn.extreme_burst"
                    };

                    for (int k = 0; k < 5; k++) {
                        int bx = btnStartX + k * (btnW + btnSpacing);
                        boolean disabled = info.isAbilityDisabled(k);
                        boolean active = (this.selectedAbilityIndex == k) && !disabled;
                        boolean hovered = mouseX >= bx && mouseX < bx + btnW && mouseY >= btnY && mouseY < btnY + btnH && !disabled;

                        int btnBg;
                        int btnTextCol;
                        int btnBorder = disabled ? 0x22888888 : (flashing ? 0x33FF3333 : 0x3300FFCC);

                        if (disabled) {
                            btnBg = 0x1A444444;
                            btnTextCol = 0xFF555555;
                        } else if (active) {
                            btnBg = flashing ? 0xCCFF3333 : 0xCC00FFCC;
                            btnTextCol = 0xFF08111E;
                        } else if (hovered) {
                            btnBg = flashing ? 0x44FF3333 : 0x4400FFCC;
                            btnTextCol = 0xFFFFFFFF;
                        } else {
                            btnBg = flashing ? 0x1AFF3333 : 0x1A00FFCC;
                            btnTextCol = flashing ? 0xFF884444 : 0xFF888888;
                        }

                        g.fill(bx, btnY, bx + btnW, btnY + btnH, btnBg);
                        g.fill(bx, btnY, bx + btnW, btnY + 1, btnBorder);
                        g.fill(bx, btnY + btnH - 1, bx + btnW, btnY + btnH, btnBorder);
                        g.fill(bx, btnY, bx + 1, btnY + btnH, btnBorder);
                        g.fill(bx + btnW - 1, btnY, bx + btnW, btnY + btnH, btnBorder);

                        String btnText = translate(btnKeys[k]);
                        int btnTextW = this.font.width(btnText);
                        int pad = 4;
                        int avail = btnW - pad * 2;
                        if (btnTextW <= avail) {
                            g.drawString(this.font, btnText, bx + (btnW - btnTextW) / 2, btnY + 3, btnTextCol, false);
                        } else {
                            float btnScale = (float) avail / (float) btnTextW;
                            g.pose().pushPose();
                            float cx = bx + btnW / 2.0f;
                            float cy = btnY + 3 + this.font.lineHeight / 2.0f;
                            g.pose().translate(cx, cy, 0);
                            g.pose().scale(btnScale, btnScale, 1.0f);
                            g.pose().translate(-cx, -cy, 0);
                            g.drawString(this.font, btnText, bx + (btnW - btnTextW) / 2, btnY + 3, btnTextCol, false);
                            g.pose().popPose();
                        }
                    }

                    // Render Selected Ability Details (Scrollable viewport)
                    int detailY = areaY + 86;
                    int idx = this.selectedAbilityIndex;

                    if (idx == 4) {
                        // Extreme Burst
                        org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry = null;
                        net.minecraft.world.item.Item item = this.analyzedStack.getItem();
                        if (item == ModItems.GOLDEN_FLOWER.get()) {
                            burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(ModItems.OMEGA_FLOWERY.get());
                        } else if (item == ModItems.THE_TEARS.get()) {
                            burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(ModItems.DOGMA.get());
                        }

                        if (burstEntry != null) {
                            net.minecraft.resources.ResourceKey<Item> rk =
                                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getResourceKey(burstEntry.curioItem).orElse(null);
                            String itemName = rk != null ? new ItemStack(burstEntry.curioItem).getHoverName().getString() : burstEntry.curioItem.toString();

                            g.drawString(this.font, itemName, areaX + 12, detailY, 0xFF00FFCC, false);

                            // Scrollable list for specs
                            int listY = detailY + 12;
                            int listH = areaY + areaH - 4 - listY;

                            List<String> specs = new ArrayList<>();
                            String typeKey = burstEntry.type == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstType.UNIVERSAL
                                    ? "gui.xeb.enigma_bios.extreme_burst.universal" : "gui.xeb.enigma_bios.extreme_burst.limited";
                            specs.add(translate("gui.xeb.enigma_bios.extreme_burst.type") + ": " + translate(typeKey));

                            String versionKey = burstEntry.version == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstVersion.INSTANT
                                    ? "gui.xeb.enigma_bios.extreme_burst.instant" : "gui.xeb.enigma_bios.extreme_burst.instance";
                            specs.add(translate("gui.xeb.enigma_bios.extreme_burst.version") + ": " + translate(versionKey));

                            if (burstEntry.type == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstType.LIMITED && burstEntry.requiredWeaponName != null) {
                                String weaponName = switch (burstEntry.requiredWeaponName) {
                                    case "the_tears" -> translate("item.xeb.the_tears");
                                    case "golden_flower" -> translate("item.xeb.golden_flower");
                                    default -> burstEntry.requiredWeaponName;
                                };
                                specs.add(translate("gui.xeb.enigma_bios.extreme_burst.requires") + ": " + weaponName);
                            }

                            specs.add(translate("gui.xeb.enigma_bios.extreme_burst.cooldown") + ": " + (burstEntry.cooldownTicks / 20) + "s");

                            if (burstEntry.version == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstVersion.INSTANCE && burstEntry.durationTicks > 0) {
                                specs.add(translate("gui.xeb.enigma_bios.extreme_burst.duration") + ": " + (burstEntry.durationTicks / 20) + "s");
                            }

                            int totalHeight = specs.size() * 10;
                            int maxScroll = Math.max(0, totalHeight - listH);

                            if (maxScroll > 0) {
                                int scrollX = areaX + areaW - 6;
                                g.fill(scrollX, listY, scrollX + 3, listY + listH, 0x3300FFCC);
                                int thumbH = Math.max(8, (int) ((float) listH * listH / totalHeight));
                                int thumbY = listY + (int) ((float) analyzerScrollAmount * (listH - thumbH) / maxScroll);
                                thumbY = net.minecraft.util.Mth.clamp(thumbY, listY, listY + listH - thumbH);
                                g.fill(scrollX, thumbY, scrollX + 3, thumbY + thumbH, 0xFF00FFCC);
                            }

                            g.enableScissor(areaX + 12, listY, areaX + areaW - 12, areaY + areaH - 4);
                            int dy = listY - (int) analyzerScrollAmount;
                            for (String spec : specs) {
                                g.drawString(this.font, spec, areaX + 12, dy, 0xFFCCCCCC, false);
                                dy += 10;
                            }
                            g.disableScissor();

                        } else {
                            g.drawString(this.font, translate("gui.xeb.enigma_bios.btn.extreme_burst"), areaX + 12, detailY, 0xFF00FFCC, false);
                            g.drawString(this.font, translate("gui.xeb.enigma_bios.extreme_burst.none"), areaX + 12, detailY + 12, 0xFFFF5555, false);
                        }
                    } else {
                        String abilityNameKey = switch (idx) {
                            case 0 -> "left_click";
                            case 1 -> "right_click";
                            case 2 -> "active1";
                            case 3 -> "active2";
                            default -> "";
                        };

                        String abName = translate(info.translationKey + ".ability." + abilityNameKey + ".name");
                        String abDesc = translate(info.translationKey + ".ability." + abilityNameKey + ".desc");
                        String dmg = info.damages[idx];
                        String cd = info.cooldowns[idx];

                        g.drawString(this.font, abName, areaX + 12, detailY, 0xFF00FFCC, false);

                        String statsStr = translate("gui.xeb.enigma_bios.label.damage") + dmg + " | " + translate("gui.xeb.enigma_bios.label.cooldown") + cd;
                        g.drawString(this.font, statsStr, areaX + 12, detailY + 11, 0xFFFFFF55, false);

                        // Scrollable description box
                        int descY = detailY + 22;
                        int descH = areaY + areaH - 4 - descY;

                        List<FormattedText> descLines = this.font.getSplitter().splitLines(abDesc, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                        int totalHeight = descLines.size() * 9;
                        int maxScroll = Math.max(0, totalHeight - descH);

                        if (maxScroll > 0) {
                            int scrollX = areaX + areaW - 6;
                            g.fill(scrollX, descY, scrollX + 3, descY + descH, 0x3300FFCC);
                            int thumbH = Math.max(8, (int) ((float) descH * descH / totalHeight));
                            int thumbY = descY + (int) ((float) analyzerScrollAmount * (descH - thumbH) / maxScroll);
                            thumbY = net.minecraft.util.Mth.clamp(thumbY, descY, descY + descH - thumbH);
                            g.fill(scrollX, thumbY, scrollX + 3, thumbY + thumbH, 0xFF00FFCC);
                        }

                        g.enableScissor(areaX + 12, descY, areaX + areaW - 12, areaY + areaH - 4);
                        int dy = descY - (int) analyzerScrollAmount;
                        for (FormattedText line : descLines) {
                            g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFB0B0B0, false);
                            dy += 9;
                        }
                        g.disableScissor();
                    }
                } else {
                    // Common item effect (Scrollable description viewport)
                    int effectY = areaY + 68;
                    g.drawString(this.font, translate("gui.xeb.enigma_bios.label.effect"), areaX + 12, effectY, 0xFFFFFF55, false);

                    int descY = effectY + 12;
                    int descH = areaY + areaH - 4 - descY;

                    String effectKey = info.translationKey.equals("item.unknown")
                            ? "item.unknown.enigma_effect." + this.unknownTextIndex
                            : info.translationKey + ".enigma_effect";
                    String effectText = translate(effectKey);
                    List<FormattedText> effectLines = this.font.getSplitter().splitLines(effectText, areaW - 24, net.minecraft.network.chat.Style.EMPTY);

                    int totalHeight = effectLines.size() * 10;
                    int maxScroll = Math.max(0, totalHeight - descH);

                    if (maxScroll > 0) {
                        int scrollX = areaX + areaW - 6;
                        g.fill(scrollX, descY, scrollX + 3, descY + descH, 0x3300FFCC);
                        int thumbH = Math.max(8, (int) ((float) descH * descH / totalHeight));
                        int thumbY = descY + (int) ((float) analyzerScrollAmount * (descH - thumbH) / maxScroll);
                        thumbY = net.minecraft.util.Mth.clamp(thumbY, descY, descY + descH - thumbH);
                        g.fill(scrollX, thumbY, scrollX + 3, thumbY + thumbH, 0xFF00FFCC);
                    }

                    g.enableScissor(areaX + 12, descY, areaX + areaW - 12, areaY + areaH - 4);
                    int dy = descY - (int) analyzerScrollAmount;
                    for (FormattedText line : effectLines) {
                        g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFB0B0B0, false);
                        dy += 10;
                    }
                    g.disableScissor();
                }
            }
        } else {
            // RENDER LOG (Bitácoras 1-5)
            int index = this.activeTab - 1;
            if (index >= 0 && index < logs.size()) {
                boolean isUnlocked = this.minecraft != null && this.minecraft.player != null &&
                        this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + (index + 1));

                if (!isUnlocked) {
                    // LOCKED NOTICE
                    g.drawString(this.font, translate("gui.xeb.enigma_bios.log.locked.title").formatted(index + 1), areaX + 12, areaY + 8, 0xFFFF3333, false);
                    g.fill(areaX + 12, areaY + 19, areaX + areaW - 12, areaY + 20, 0x44FF3333);

                    int textY = areaY + 26;
                    String lockedDesc = translate("gui.xeb.enigma_bios.log.locked.desc");
                    List<FormattedText> lines = this.font.getSplitter().splitLines(lockedDesc, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                    for (FormattedText line : lines) {
                        g.drawString(this.font, line.getString(), areaX + 12, textY, 0xFF777777, false);
                        textY += 10;
                    }
                } else {
                    // UNLOCKED CONTENT (Scrollable viewport)
                    LogEntry log = logs.get(index);
                    g.drawString(this.font, translate(log.titleKey), areaX + 12, areaY + 8, 0xFF00FFCC, false);
                    g.fill(areaX + 12, areaY + 19, areaX + areaW - 12, areaY + 20, 0x4400FFCC);

                    int textY = areaY + 26;
                    int textH = areaY + areaH - 8 - textY;

                    List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                    int totalHeight = lines.size() * 10;
                    int maxScroll = Math.max(0, totalHeight - textH);

                    if (maxScroll > 0) {
                        int scrollX = areaX + areaW - 6;
                        g.fill(scrollX, textY, scrollX + 3, textY + textH, 0x3300FFCC);
                        int thumbH = Math.max(10, (int) ((float) textH * textH / totalHeight));
                        int thumbY = textY + (int) ((float) contentScrollAmount * (textH - thumbH) / maxScroll);
                        thumbY = net.minecraft.util.Mth.clamp(thumbY, textY, textY + textH - thumbH);
                        g.fill(scrollX, thumbY, scrollX + 3, thumbY + thumbH, 0xFF00FFCC);
                    }

                    g.enableScissor(areaX + 12, textY, areaX + areaW - 12, areaY + areaH - 8);
                    int dy = textY - (int) contentScrollAmount;
                    for (FormattedText line : lines) {
                        g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFE0E0E0, false);
                        dy += 10;
                    }
                    g.disableScissor();
                }
            }
        }
    }

    private void renderInventory(GuiGraphics g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int invLeft = this.leftPos + 99;
        int invTop = this.topPos + 165;

        // Draw slots (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = invLeft + col * 18;
                int y = invTop + row * 18;
                renderSlotBackground(g, x, y, mouseX, mouseY);

                int slotIndex = 9 + row * 9 + col;
                ItemStack stack = mc.player.getInventory().getItem(slotIndex);
                if (!stack.isEmpty()) {
                    g.renderFakeItem(stack, x + 1, y + 1);
                    g.renderItemDecorations(this.font, stack, x + 1, y + 1);
                }
            }
        }

        // Draw hotbar (1 row of 9)
        for (int col = 0; col < 9; col++) {
            int x = invLeft + col * 18;
            int y = invTop + 58;
            renderSlotBackground(g, x, y, mouseX, mouseY);

            ItemStack stack = mc.player.getInventory().getItem(col);
            if (!stack.isEmpty()) {
                g.renderFakeItem(stack, x + 1, y + 1);
                g.renderItemDecorations(this.font, stack, x + 1, y + 1);
            }
        }
    }

    private void renderSlotBackground(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
        int color = hovered ? 0x6600FFCC : 0x2200FFCC;

        g.fill(x, y, x + 18, y + 18, 0x9908111E);
        g.fill(x, y, x + 18, y + 1, color);
        g.fill(x, y + 17, x + 18, y + 18, color);
        g.fill(x, y, x + 1, y + 18, color);
        g.fill(x + 17, y, x + 18, y + 18, color);
    }

    private ItemStack getStackAtMouse(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;

        int invLeft = this.leftPos + 99;
        int invTop = this.topPos + 165;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = invLeft + col * 18;
                int y = invTop + row * 18;
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    return mc.player.getInventory().getItem(9 + row * 9 + col);
                }
            }
        }

        for (int col = 0; col < 9; col++) {
            int x = invLeft + col * 18;
            int y = invTop + 58;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    return mc.player.getInventory().getItem(col);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        float scale = getScaleFactor();
        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 130;

        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        // Hover left tabs
        if (scaledMouseX >= startX && scaledMouseX < startX + 65 && scaledMouseY >= viewportY && scaledMouseY < viewportY + viewportH) {
            int maxTabScroll = Math.max(0, 6 * 22 - viewportH);
            if (maxTabScroll > 0) {
                this.tabScrollAmount = net.minecraft.util.Mth.clamp(this.tabScrollAmount - (float) delta * 11.0F, 0.0F, maxTabScroll);
                return true;
            }
        }

        // Hover content area
        if (scaledMouseX >= areaX && scaledMouseX < areaX + areaW && scaledMouseY >= areaY && scaledMouseY < areaY + areaH) {
            if (this.activeTab == 0) {
                // Analyzer scrolling
                if (!this.analyzedStack.isEmpty()) {
                    AnalyzedInfo info = analyzeItem(this.analyzedStack);
                    int maxScroll = 0;
                    if (info.hasAbilities) {
                        int descY = areaY + 86 + 22;
                        int descH = areaY + areaH - 4 - descY;
                        int totalHeight = 0;
                        if (this.selectedAbilityIndex == 4) {
                            // Extreme Burst specs
                            org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry = null;
                            net.minecraft.world.item.Item item = this.analyzedStack.getItem();
                            if (item == ModItems.GOLDEN_FLOWER.get()) {
                                burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(ModItems.OMEGA_FLOWERY.get());
                            } else if (item == ModItems.THE_TEARS.get()) {
                                burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(ModItems.DOGMA.get());
                            }
                            if (burstEntry != null) {
                                int size = 4 + (burstEntry.type == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstType.LIMITED ? 1 : 0) +
                                        (burstEntry.version == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstVersion.INSTANCE && burstEntry.durationTicks > 0 ? 1 : 0);
                                totalHeight = size * 10;
                            }
                        } else {
                            String abilityNameKey = switch (this.selectedAbilityIndex) {
                                case 0 -> "left_click";
                                case 1 -> "right_click";
                                case 2 -> "active1";
                                case 3 -> "active2";
                                default -> "";
                            };
                            String abDesc = translate(info.translationKey + ".ability." + abilityNameKey + ".desc");
                            List<FormattedText> descLines = this.font.getSplitter().splitLines(abDesc, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                            totalHeight = descLines.size() * 9;
                        }
                        maxScroll = Math.max(0, totalHeight - descH);
                    } else {
                        // Common item effect
                        int descY = areaY + 68 + 12;
                        int descH = areaY + areaH - 4 - descY;
                        String effectKey = info.translationKey.equals("item.unknown")
                                ? "item.unknown.enigma_effect." + this.unknownTextIndex
                                : info.translationKey + ".enigma_effect";
                        String effectText = translate(effectKey);
                        List<FormattedText> effectLines = this.font.getSplitter().splitLines(effectText, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                        maxScroll = Math.max(0, effectLines.size() * 10 - descH);
                    }

                    if (maxScroll > 0) {
                        this.analyzerScrollAmount = net.minecraft.util.Mth.clamp(this.analyzerScrollAmount - (float) delta * 9.0F, 0.0F, maxScroll);
                        return true;
                    }
                }
            } else {
                // Log content scrolling
                int index = this.activeTab - 1;
                if (index >= 0 && index < logs.size()) {
                    boolean isUnlocked = this.minecraft != null && this.minecraft.player != null &&
                            this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + (index + 1));
                    if (isUnlocked) {
                        LogEntry log = logs.get(index);
                        int textY = areaY + 26;
                        int textH = areaY + areaH - 8 - textY;
                        List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                        int maxScroll = Math.max(0, lines.size() * 10 - textH);
                        if (maxScroll > 0) {
                            this.contentScrollAmount = net.minecraft.util.Mth.clamp(this.contentScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = getScaleFactor();
        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 130;

        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        // Check if tab scrollbar was clicked
        int maxTabScroll = Math.max(0, (1 + logs.size()) * 22 - viewportH);
        if (maxTabScroll > 0 && scaledMouseX >= startX + 61 && scaledMouseX < startX + 64 && scaledMouseY >= viewportY && scaledMouseY < viewportY + viewportH) {
            this.isDraggingTabScroll = true;
            return true;
        }

        // Tab item clicks
        for (int i = 0; i < 1 + logs.size(); i++) {
            int y = viewportY + i * 22 - (int) tabScrollAmount;
            if (scaledMouseX >= startX && scaledMouseX < startX + 60 && scaledMouseY >= y && scaledMouseY < y + 20
                    && y >= viewportY && y + 20 <= viewportY + viewportH) {
                this.activeTab = i;
                this.contentScrollAmount = 0.0F;
                this.analyzerScrollAmount = 0.0F;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }

        // Analyzer ability clicks
        if (this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
            AnalyzedInfo info = analyzeItem(this.analyzedStack);

            // Click details scrollbar check
            if (info.hasAbilities) {
                int descY = areaY + 86 + 22;
                int descH = areaY + areaH - 4 - descY;
                int totalHeight = 0;
                if (this.selectedAbilityIndex == 4) {
                    org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry = null;
                    net.minecraft.world.item.Item item = this.analyzedStack.getItem();
                    if (item == ModItems.GOLDEN_FLOWER.get()) {
                        burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(ModItems.OMEGA_FLOWERY.get());
                    } else if (item == ModItems.THE_TEARS.get()) {
                        burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(ModItems.DOGMA.get());
                    }
                    if (burstEntry != null) {
                        int size = 4 + (burstEntry.type == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstType.LIMITED ? 1 : 0) +
                                (burstEntry.version == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstVersion.INSTANCE && burstEntry.durationTicks > 0 ? 1 : 0);
                        totalHeight = size * 10;
                    }
                } else {
                    String abilityNameKey = switch (this.selectedAbilityIndex) {
                        case 0 -> "left_click";
                        case 1 -> "right_click";
                        case 2 -> "active1";
                        case 3 -> "active2";
                        default -> "";
                    };
                    String abDesc = translate(info.translationKey + ".ability." + abilityNameKey + ".desc");
                    List<FormattedText> descLines = this.font.getSplitter().splitLines(abDesc, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                    totalHeight = descLines.size() * 9;
                }

                int maxScroll = Math.max(0, totalHeight - descH);
                if (maxScroll > 0 && scaledMouseX >= areaX + areaW - 6 && scaledMouseX < areaX + areaW - 3 && scaledMouseY >= descY && scaledMouseY < areaY + areaH - 4) {
                    this.isDraggingAnalyzerScroll = true;
                    return true;
                }
            } else {
                // Common item effect scrollbar check
                int descY = areaY + 68 + 12;
                int descH = areaY + areaH - 4 - descY;
                String effectKey = info.translationKey.equals("item.unknown")
                        ? "item.unknown.enigma_effect." + this.unknownTextIndex
                        : info.translationKey + ".enigma_effect";
                String effectText = translate(effectKey);
                List<FormattedText> effectLines = this.font.getSplitter().splitLines(effectText, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                int maxScroll = Math.max(0, effectLines.size() * 10 - descH);
                if (maxScroll > 0 && scaledMouseX >= areaX + areaW - 6 && scaledMouseX < areaX + areaW - 3 && scaledMouseY >= descY && scaledMouseY < areaY + areaH - 4) {
                    this.isDraggingAnalyzerScroll = true;
                    return true;
                }
            }

            if (info.hasAbilities) {
                int btnY = areaY + 66;
                int btnStartX = areaX + 12;
                int btnW = 48;
                int btnH = 14;
                int btnSpacing = 6;

                for (int k = 0; k < 5; k++) {
                    int bx = btnStartX + k * (btnW + btnSpacing);
                    if (scaledMouseX >= bx && scaledMouseX < bx + btnW && scaledMouseY >= btnY && scaledMouseY < btnY + btnH) {
                        if (!info.isAbilityDisabled(k)) {
                            this.selectedAbilityIndex = k;
                            this.analyzerScrollAmount = 0.0F;
                            if (this.minecraft != null) {
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            }
                        }
                        return true;
                    }
                }
            }
        }

        // Active log scrollbar check
        if (this.activeTab >= 1 && this.activeTab <= logs.size()) {
            int index = this.activeTab - 1;
            boolean isUnlocked = this.minecraft != null && this.minecraft.player != null &&
                    this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + (index + 1));
            if (isUnlocked) {
                LogEntry log = logs.get(index);
                int textY = areaY + 26;
                int textH = areaY + areaH - 8 - textY;
                List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                int maxScroll = Math.max(0, lines.size() * 10 - textH);
                if (maxScroll > 0 && scaledMouseX >= areaX + areaW - 6 && scaledMouseX < areaX + areaW - 3 && scaledMouseY >= textY && scaledMouseY < areaY + areaH - 8) {
                    this.isDraggingContentScroll = true;
                    return true;
                }
            }
        }

        // Inventory click handler
        ItemStack clickedStack = getStackAtMouse(scaledMouseX, scaledMouseY);
        if (!clickedStack.isEmpty()) {
            this.analyzedStack = clickedStack.copy();
            AnalyzedInfo info = analyzeItem(this.analyzedStack);
            this.selectedAbilityIndex = (info.hasAbilities && info.isAbilityDisabled(0)) ? 1 : 0;
            this.activeTab = 0;
            this.analyzerScrollAmount = 0.0F;

            // Trigger unknown item warn flash and sound
            if (info.translationKey.equals("item.unknown")) {
                this.lastAnalyzedUnknown = true;
                this.lastAnalyzedTime = System.currentTimeMillis();
                this.unknownTextIndex = new java.util.Random().nextInt(5);
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6F));
                }
            } else {
                this.lastAnalyzedUnknown = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.5F));
                }
            }
            return true;
        }

        // Clear slot click
        int slotX = areaX + 12;
        int slotY = areaY + 8;
        if (this.activeTab == 0 && scaledMouseX >= slotX && scaledMouseX < slotX + 20 && scaledMouseY >= slotY && scaledMouseY < slotY + 20) {
            if (!this.analyzedStack.isEmpty()) {
                this.analyzedStack = ItemStack.EMPTY;
                this.selectedAbilityIndex = 0;
                this.analyzerScrollAmount = 0.0F;
                this.lastAnalyzedUnknown = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8F));
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDraggingTabScroll = false;
        this.isDraggingContentScroll = false;
        this.isDraggingAnalyzerScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float scale = getScaleFactor();
        double scaledDragY = dragY / scale;

        int viewportH = 130;
        int areaH = 130;

        if (this.isDraggingTabScroll) {
            int maxTabScroll = Math.max(0, (1 + logs.size()) * 22 - viewportH);
            int thumbH = Math.max(10, (int) ((float) viewportH * viewportH / ((1 + logs.size()) * 22)));
            int trackH = viewportH - thumbH;
            if (trackH > 0) {
                float change = (float) (scaledDragY * maxTabScroll / trackH);
                this.tabScrollAmount = net.minecraft.util.Mth.clamp(this.tabScrollAmount + change, 0.0F, maxTabScroll);
            }
            return true;
        }

        if (this.isDraggingContentScroll && this.activeTab >= 1 && this.activeTab <= logs.size()) {
            int index = this.activeTab - 1;
            LogEntry log = logs.get(index);
            int textY = this.topPos + 18 + 26;
            int textH = this.topPos + 18 + areaH - 8 - textY;
            List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), 280 - 24, net.minecraft.network.chat.Style.EMPTY);
            int totalHeight = lines.size() * 10;
            int maxScroll = Math.max(0, totalHeight - textH);
            int thumbH = Math.max(10, (int) ((float) textH * textH / totalHeight));
            int trackH = textH - thumbH;
            if (trackH > 0) {
                float change = (float) (scaledDragY * maxScroll / trackH);
                this.contentScrollAmount = net.minecraft.util.Mth.clamp(this.contentScrollAmount + change, 0.0F, maxScroll);
            }
            return true;
        }

        if (this.isDraggingAnalyzerScroll && this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
            AnalyzedInfo info = analyzeItem(this.analyzedStack);
            int descY;
            int descH;
            int totalHeight = 0;

            if (info.hasAbilities) {
                descY = this.topPos + 18 + 86 + 22;
                descH = this.topPos + 18 + areaH - 4 - descY;
                if (this.selectedAbilityIndex == 4) {
                    org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry = null;
                    net.minecraft.world.item.Item item = this.analyzedStack.getItem();
                    if (item == ModItems.GOLDEN_FLOWER.get()) {
                        burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(ModItems.OMEGA_FLOWERY.get());
                    } else if (item == ModItems.THE_TEARS.get()) {
                        burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(ModItems.DOGMA.get());
                    }
                    if (burstEntry != null) {
                        int size = 4 + (burstEntry.type == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstType.LIMITED ? 1 : 0) +
                                (burstEntry.version == org.xeb.xeb.extremeburst.ExtremeBurstRegistry.BurstVersion.INSTANCE && burstEntry.durationTicks > 0 ? 1 : 0);
                        totalHeight = size * 10;
                    }
                } else {
                    String abilityNameKey = switch (this.selectedAbilityIndex) {
                        case 0 -> "left_click";
                        case 1 -> "right_click";
                        case 2 -> "active1";
                        case 3 -> "active2";
                        default -> "";
                    };
                    String abDesc = translate(info.translationKey + ".ability." + abilityNameKey + ".desc");
                    List<FormattedText> descLines = this.font.getSplitter().splitLines(abDesc, 280 - 24, net.minecraft.network.chat.Style.EMPTY);
                    totalHeight = descLines.size() * 9;
                }
            } else {
                descY = this.topPos + 18 + 68 + 12;
                descH = this.topPos + 18 + areaH - 4 - descY;
                String effectKey = info.translationKey.equals("item.unknown")
                        ? "item.unknown.enigma_effect." + this.unknownTextIndex
                        : info.translationKey + ".enigma_effect";
                String effectText = translate(effectKey);
                List<FormattedText> effectLines = this.font.getSplitter().splitLines(effectText, 280 - 24, net.minecraft.network.chat.Style.EMPTY);
                totalHeight = effectLines.size() * 10;
            }

            int maxScroll = Math.max(0, totalHeight - descH);
            int thumbH = Math.max(8, (int) ((float) descH * descH / totalHeight));
            int trackH = descH - thumbH;
            if (trackH > 0) {
                float change = (float) (scaledDragY * maxScroll / trackH);
                this.analyzerScrollAmount = net.minecraft.util.Mth.clamp(this.analyzerScrollAmount + change, 0.0F, (float) maxScroll);
            }
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private AnalyzedInfo analyzeItem(ItemStack stack) {
        Item item = stack.getItem();
        String name = stack.getHoverName().getString();

        if (item == ModItems.GOLDEN_FLOWER.get()) {
            return new AnalyzedInfo(name, "item.xeb.golden_flower", true,
                    new String[]{"2", "6", "4", "5", ""},
                    new String[]{"2s", "Passive", "8s", "12s", ""});
        }
        if (item == ModItems.DOOMFIST_V2.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist_v2", true,
                    new String[]{"8", "8-15", "6", "0", ""},
                    new String[]{"0.5s", "3s", "6s", "8s", ""},
                    new boolean[]{false, false, false, false, true});
        }
        if (item == ModItems.DOOMFIST.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist", true,
                    new String[]{"10", "6-12", "5", "5", ""},
                    new String[]{"0.5s", "3s", "5s", "6s", ""},
                    new boolean[]{false, false, false, false, true});
        }
        if (item == ModItems.OPTIC_BLAST.get()) {
            return new AnalyzedInfo(name, "item.xeb.optic_blast", true,
                    new String[]{"3", "5 / tick", "4", "6", ""},
                    new String[]{"0.5s", "Energy", "10s", "8s", ""},
                    new boolean[]{false, false, false, false, true});
        }
        if (item == ModItems.HOLY_DUALITY_BLADE.get()) {
            return new AnalyzedInfo(name, "item.xeb.holy_duality_blade", true,
                    new String[]{"8", "18", "10", "12", ""},
                    new String[]{"Standard", "20s", "10s", "15s", ""},
                    new boolean[]{false, false, false, false, true});
        }
        if (item == ModItems.MECHA_OVERDRIVE.get()) {
            return new AnalyzedInfo(name, "item.xeb.mecha_overdrive", true,
                    new String[]{"", "2", "8", "7", ""},
                    new String[]{"", "0s", "4s", "8s", ""},
                    new boolean[]{true, false, false, false, true});
        }
        if (item == ModItems.BROKEN_DIAMOND.get()) {
            return new AnalyzedInfo(name, "item.xeb.broken_diamond", true,
                    new String[]{"", "8", "8", "0", ""},
                    new String[]{"", "Variable", "5s", "15s", ""},
                    new boolean[]{true, false, false, false, true});
        }
        if (item == ModItems.THE_TEARS.get()) {
            return new AnalyzedInfo(name, "item.xeb.the_tears", true,
                    new String[]{"4", "8", "5", "0", ""},
                    new String[]{"0.4s", "2s", "10s", "15s", ""});
        }
        if (item == ModItems.SMART_HALBERD.get()) {
            return new AnalyzedInfo(name, "item.xeb.smart_halberd", true,
                    new String[]{"9", "14", "", "", ""},
                    new String[]{"1.0s", "0s", "", "", ""},
                    new boolean[]{false, false, true, true, true});
        }

        if (item == ModItems.MOON_TEAR.get()) {
            return new AnalyzedInfo(name, "item.xeb.moon_tear", false, null, null);
        }
        if (item == ModItems.TINFOIL_HAT.get()) {
            return new AnalyzedInfo(name, "item.xeb.tinfoil_hat", false, null, null);
        }
        if (item == ModItems.HOLY_MANTLE.get()) {
            return new AnalyzedInfo(name, "item.xeb.holy_mantle", false, null, null);
        }
        if (item == ModItems.BRASS_KNUCKLES.get()) {
            return new AnalyzedInfo(name, "item.xeb.brass_knuckles", false, null, null);
        }
        if (item == ModItems.DEMON_CORE.get()) {
            return new AnalyzedInfo(name, "item.xeb.demon_core", false, null, null);
        }
        if (item == ModItems.MOB_ENERGY.get()) {
            return new AnalyzedInfo(name, "item.xeb.mob_energy", false, null, null);
        }

        // Unknown fallback
        return new AnalyzedInfo(name, "item.unknown", false, null, null);
    }

    private static class LogEntry {
        final String titleKey;
        final String contentKey;

        LogEntry(String titleKey, String contentKey) {
            this.titleKey = titleKey;
            this.contentKey = contentKey;
        }
    }

    private static class AnalyzedInfo {
        final String name;
        final String translationKey;
        final boolean hasAbilities;
        final String[] damages;
        final String[] cooldowns;
        final boolean[] disabledAbilities;

        AnalyzedInfo(String name, String translationKey, boolean hasAbilities, String[] damages, String[] cooldowns) {
            this(name, translationKey, hasAbilities, damages, cooldowns, new boolean[]{false, false, false, false, false});
        }

        AnalyzedInfo(String name, String translationKey, boolean hasAbilities, String[] damages, String[] cooldowns, boolean[] disabledAbilities) {
            this.name = name;
            this.translationKey = translationKey;
            this.hasAbilities = hasAbilities;
            this.damages = damages;
            this.cooldowns = cooldowns;
            this.disabledAbilities = disabledAbilities;
        }

        public boolean isAbilityDisabled(int index) {
            if (disabledAbilities == null || index < 0 || index >= disabledAbilities.length) {
                return false;
            }
            return disabledAbilities[index];
        }
    }
}
