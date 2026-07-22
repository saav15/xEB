package org.xeb.xeb.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
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
import org.xeb.xeb.medallion.MedallionType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.render.MedallionRenderLayer;
import org.xeb.xeb.extremeburst.ExtremeBurstRegistry;

import java.util.ArrayList;
import java.util.List;

public class EnigmaBiosScreen extends Screen {
    private final int guiWidth = 360;
    private final int guiHeight = 260;
    private int leftPos;
    private int topPos;

    private int activeTab = 0; // 0: Analyzer, 1: Bestiary, 2-6: Bitácoras 1-5
    private ItemStack analyzedStack = ItemStack.EMPTY;
    private int selectedAbilityIndex = 0; // 0: Left Click, 1: Right Click, 2: Active 1, 3: Active 2, 4: Extreme Burst

    private int selectedBestiaryIndex = 0;
    private int selectedBestiaryTierIndex = 0; // 0: BRONZE, 1: SILVER, 2: GOLD

    // Lore Logs (Bitácoras 1 to 5)
    private final List<LogEntry> logs = new ArrayList<>();

    // Scrolling states
    private float tabScrollAmount = 0.0F;
    private float contentScrollAmount = 0.0F;
    private float analyzerScrollAmount = 0.0F;
    private float bestiaryScrollAmount = 0.0F;

    private boolean isDraggingTabScroll = false;
    private boolean isDraggingContentScroll = false;
    private boolean isDraggingAnalyzerScroll = false;
    private boolean isDraggingBestiaryScroll = false;

    // Unknown item warning flash states
    private boolean lastAnalyzedUnknown = false;
    private long lastAnalyzedTime = 0L;
    private int unknownTextIndex = 0;

    public EnigmaBiosScreen() {
        super(Component.literal("Enigma Bios"));
        initLogs();
    }

    @Override
    public boolean isPauseScreen() {
        // Return false so singleplayer world ticks, mob animations, weapon models, and particles continue live in background!
        return false;
    }

    private void initLogs() {
        logs.add(new LogEntry("gui.xeb.enigma_bios.log1.title", "gui.xeb.enigma_bios.log1.content"));
        logs.add(new LogEntry("gui.xeb.enigma_bios.log2.title", "gui.xeb.enigma_bios.log2.content"));
        logs.add(new LogEntry("gui.xeb.enigma_bios.log3.title", "gui.xeb.enigma_bios.log3.content"));
        logs.add(new LogEntry("gui.xeb.enigma_bios.log4.title", "gui.xeb.enigma_bios.log4.content"));
        logs.add(new LogEntry("gui.xeb.enigma_bios.log5.title", "gui.xeb.enigma_bios.log5.content"));
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
        float scale = getScaleFactor();
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        this.leftPos = (int) ((this.width / scale - this.guiWidth) / 2);
        this.topPos = (int) ((this.height / scale - this.guiHeight) / 2);

        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0F);

        this.renderBackground(g);

        int borderColor = 0xFF00FFCC;

        // Flash red if last analyzed item was unknown ("ERROR")
        long elapsed = System.currentTimeMillis() - this.lastAnalyzedTime;
        boolean flashing = this.lastAnalyzedUnknown && (elapsed < 800L);
        if (flashing) {
            float flash = (float) Math.sin(elapsed * 0.02D);
            if (flash > 0) {
                borderColor = 0xFFFF3333;
            }
        }

        // Draw Main Frame Background
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, 0xEE08111E);

        // Frame Border Lines
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + 2, borderColor);
        g.fill(this.leftPos, this.topPos + this.guiHeight - 2, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, borderColor);
        g.fill(this.leftPos, this.topPos, this.leftPos + 2, this.topPos + this.guiHeight, borderColor);
        g.fill(this.leftPos + this.guiWidth - 2, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, borderColor);

        // Header Title Bar: ENIGMA BIOS v1.0 | STATUS: LINKED
        g.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.guiWidth - 4, this.topPos + 16, 0x3300FFCC);
        g.drawString(this.font, "ENIGMA BIOS v1.0", this.leftPos + 8, this.topPos + 6, borderColor, false);
        String statusText = translate("gui.xeb.enigma_bios.status");
        if (statusText.equals("gui.xeb.enigma_bios.status")) statusText = "STATUS: LINKED";
        g.drawString(this.font, statusText, this.leftPos + this.guiWidth - 8 - this.font.width(statusText), this.topPos + 6, borderColor, false);

        renderTabs(g, scaledMouseX, scaledMouseY);
        renderContent(g, scaledMouseX, scaledMouseY, borderColor, flashing, elapsed);
        renderInventory(g, scaledMouseX, scaledMouseY);

        super.render(g, scaledMouseX, scaledMouseY, partialTick);
        g.pose().popPose();
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 130;
        int totalTabs = 2 + logs.size(); // 0: Analyzer, 1: Bestiary, 2-6: Bitácoras 1-5
        int contentHeight = totalTabs * 22;
        int maxTabScroll = Math.max(0, contentHeight - viewportH);

        if (maxTabScroll > 0) {
            int scrollbarX = startX + 61;
            g.fill(scrollbarX, viewportY, scrollbarX + 3, viewportY + viewportH, 0x3300FFCC);
            int thumbH = Math.max(10, (int) ((float) viewportH * viewportH / contentHeight));
            int thumbY = viewportY + (int) ((float) tabScrollAmount * (viewportH - thumbH) / maxTabScroll);
            thumbY = net.minecraft.util.Mth.clamp(thumbY, viewportY, viewportY + viewportH - thumbH);
            g.fill(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbH, 0xFF00FFCC);
        }

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
            if (i >= 2 && this.minecraft != null && this.minecraft.player != null) {
                int logNum = i - 1;
                isUnlocked = this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + logNum);
            }

            int bgColor = !isUnlocked ? (active ? 0xCC550000 : (hovered ? 0x44550000 : 0x1A550000))
                    : (active ? 0xCC00FFCC : (hovered ? 0x4400FFCC : 0x1A00FFCC));
            int textColor = !isUnlocked ? (active ? 0xFFFFFFFF : (hovered ? 0xFFFF7777 : 0xFF884444))
                    : (active ? 0xFF08111E : (hovered ? 0xFFFFFFFF : 0xFF888888));
            int bColor = !isUnlocked ? 0x33FF5555 : 0x3300FFCC;

            g.fill(startX, y, startX + 60, y + 20, bgColor);
            g.fill(startX, y, startX + 60, y + 1, bColor);
            g.fill(startX, y + 19, startX + 60, y + 20, bColor);
            g.fill(startX, y, startX + 1, y + 20, bColor);
            g.fill(startX + 59, y, startX + 60, y + 20, bColor);

            String title = (i == 0) ? translate("gui.xeb.enigma_bios.tab.analyze")
                    : ((i == 1) ? translate("gui.xeb.enigma_bios.tab.bestiary") : "BIT. #" + (i - 1));
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

        g.fill(areaX, areaY, areaX + areaW, areaY + areaH, 0x1A000000);
        g.fill(areaX, areaY, areaX + areaW, areaY + 1, borderColor);
        g.fill(areaX, areaY + areaH - 1, areaX + areaW, areaY + areaH, borderColor);
        g.fill(areaX, areaY, areaX + 1, areaY + areaH, borderColor);
        g.fill(areaX + areaW - 1, areaY, areaX + areaW, areaY + areaH, borderColor);

        if (flashing) {
            float flash = 1.0F - (float) elapsed / 800.0F;
            int alpha = (int) (flash * 50);
            g.fill(areaX + 1, areaY + 1, areaX + areaW - 1, areaY + areaH - 1, (alpha << 24) | 0xFF0000);
        }

        if (this.activeTab == 0) {
            // RENDER ANALYZER (EXACT ORIGINAL DESIGN MATCHING SCREENSHOT)
            int slotX = areaX + 8;
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

                int textColor = flashing ? 0xFFFF3333 : 0xFF00FFCC;
                g.drawString(this.font, translate("gui.xeb.enigma_bios.label.name") + info.name, areaX + 34, areaY + 10, textColor, false);

                // Customize HUD Button (compact top-right)
                int hudBtnX = areaX + areaW - 84;
                int hudBtnY = areaY + 6;
                int hudBtnW = 78;
                int hudBtnH = 12;
                boolean hudHovered = mouseX >= hudBtnX && mouseX < hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY < hudBtnY + hudBtnH;

                int hudBg = hudHovered ? 0xCC00FFCC : 0x2200FFCC;
                int hudTxt = hudHovered ? 0xFF08111E : 0xFF00FFCC;
                g.fill(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH, hudBg);
                g.fill(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + 1, 0xFF00FFCC);
                g.fill(hudBtnX, hudBtnY + hudBtnH - 1, hudBtnX + hudBtnW, hudBtnY + hudBtnH, 0xFF00FFCC);
                g.fill(hudBtnX, hudBtnY, hudBtnX + 1, hudBtnY + hudBtnH, 0xFF00FFCC);
                g.fill(hudBtnX + hudBtnW - 1, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH, 0xFF00FFCC);
                g.drawString(this.font, translate("gui.xeb.enigma_bios.btn.customize_hud"), hudBtnX + 4, hudBtnY + 2, hudTxt, false);

                // Full Lore Paragraph
                String loreText = info.translationKey.equals("item.unknown")
                        ? translate("item.unknown.enigma_lore." + this.unknownTextIndex)
                        : translate(info.translationKey + ".enigma_lore");

                List<FormattedText> loreLines = this.font.getSplitter().splitLines(loreText, areaW - 40, net.minecraft.network.chat.Style.EMPTY);
                int textY = areaY + 26;
                for (FormattedText line : loreLines) {
                    if (textY < areaY + 54) {
                        g.drawString(this.font, line.getString(), areaX + 34, textY, 0xFFFFFFFF, false);
                        textY += 9;
                    }
                }

                // 5 Ability Selector Buttons Bar: Left Click | Right Click | Active 1 | Active 2 | Ultimate
                String[] btnKeys = {
                        "gui.xeb.enigma_bios.btn.left_click",
                        "gui.xeb.enigma_bios.btn.right_click",
                        "gui.xeb.enigma_bios.btn.active1",
                        "gui.xeb.enigma_bios.btn.active2",
                        "gui.xeb.enigma_bios.btn.extreme_burst"
                };

                int btnW = 50;
                int btnH = 14;
                int btnY = areaY + 56;

                for (int k = 0; k < 5; k++) {
                    int bx = areaX + 12 + k * 52;
                    boolean active = (this.selectedAbilityIndex == k);
                    boolean hovered = mouseX >= bx && mouseX < bx + btnW && mouseY >= btnY && mouseY < btnY + btnH;
                    boolean disabled = info.isAbilityDisabled(k);

                    int btnBg;
                    int btnTextCol;
                    int btnBorder = disabled ? 0x33666666 : (flashing ? 0xFFFF3333 : 0xFF00FFCC);

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
                    if (btnText.equals(btnKeys[k])) {
                        btnText = switch (k) {
                            case 0 -> "Left Click";
                            case 1 -> "Right Click";
                            case 2 -> "Active 1";
                            case 3 -> "Active 2";
                            default -> "Ultimate";
                        };
                    }
                    int btnTextW = this.font.width(btnText);
                    int pad = 2;
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

                // Render Selected Move Details (Move Title in Cyan + Damage/Cooldown in Bright Yellow + Description in White)
                int detailY = areaY + 74;
                int idx = this.selectedAbilityIndex;

                if (info.hasAbilities) {
                    if (idx == 4) {
                        // Ultimate / Extreme Burst
                        ExtremeBurstRegistry.ExtremeBurstEntry burstEntry = null;
                        Item item = this.analyzedStack.getItem();
                        if (item == ModItems.GOLDEN_FLOWER.get()) {
                            burstEntry = ExtremeBurstRegistry.getEntry(ModItems.OMEGA_FLOWERY.get());
                        } else if (item == ModItems.THE_TEARS.get()) {
                            burstEntry = ExtremeBurstRegistry.getEntry(ModItems.DOGMA.get());
                        }

                        if (burstEntry != null) {
                            g.drawString(this.font, burstEntry.curioItem.getDescriptionId(), areaX + 12, detailY, 0xFF00FFCC, false);

                            String typeStr = (burstEntry.type == ExtremeBurstRegistry.BurstType.UNIVERSAL ? "Universal" : "Limited");
                            String verStr = (burstEntry.version == ExtremeBurstRegistry.BurstVersion.INSTANT ? "Instant" : "Instance");
                            String statLine = String.format("Type: %s | Version: %s | Cooldown: %ds", typeStr, verStr, burstEntry.cooldownTicks / 20);
                            g.drawString(this.font, statLine, areaX + 12, detailY + 11, 0xFFFFCC00, false);

                            String desc = translate(info.translationKey + ".enigma_effect");
                            g.drawString(this.font, desc, areaX + 12, detailY + 22, 0xFFFFFFFF, false);
                        } else {
                            g.drawString(this.font, "Ultimate / Extreme Burst", areaX + 12, detailY, 0xFF00FFCC, false);
                            g.drawString(this.font, "No Extreme Burst assigned for this item", areaX + 12, detailY + 11, 0xFFFF5555, false);
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

                        String dmg = (info.damages != null && idx < info.damages.length && !info.damages[idx].isEmpty()) ? info.damages[idx] : "N/A";
                        String cd = (info.cooldowns != null && idx < info.cooldowns.length && !info.cooldowns[idx].isEmpty()) ? info.cooldowns[idx] : "N/A";

                        g.drawString(this.font, abName, areaX + 12, detailY, 0xFF00FFCC, false);

                        // Bright Yellow Stats Line: Damage: X | Cooldown: Ys
                        String statLine = String.format("Damage: %s | Cooldown: %s", dmg, cd);
                        g.drawString(this.font, statLine, areaX + 12, detailY + 11, 0xFFFFCC00, false);

                        // Description paragraph in white text
                        List<FormattedText> descLines = this.font.getSplitter().splitLines(abDesc, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                        int dy = detailY + 22;
                        for (FormattedText line : descLines) {
                            if (dy < areaY + areaH - 4) {
                                g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFFFFFFF, false);
                                dy += 9;
                            }
                        }
                    }
                } else {
                    String effectKey = info.translationKey.equals("item.unknown")
                            ? "item.unknown.enigma_effect." + this.unknownTextIndex
                            : info.translationKey + ".enigma_effect";
                    String effectText = translate(effectKey);
                    g.drawString(this.font, "Special Effect / Lore Result", areaX + 12, detailY, 0xFF00FFCC, false);
                    List<FormattedText> effectLines = this.font.getSplitter().splitLines(effectText, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                    int dy = detailY + 11;
                    for (FormattedText line : effectLines) {
                        if (dy < areaY + areaH - 4) {
                            g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFFFFFFF, false);
                            dy += 9;
                        }
                    }
                }
            }
        } else if (this.activeTab == 1) {
            // RENDER ELITE BESTIARIO (AGRANDADO A 2.2X SCALE, SIN RECUADRO)
            List<EliteBuff> allBuffs = new ArrayList<>(EliteBuffRegistry.getAll());
            if (!allBuffs.isEmpty()) {
                if (this.selectedBestiaryIndex < 0 || this.selectedBestiaryIndex >= allBuffs.size()) {
                    this.selectedBestiaryIndex = 0;
                }

                // Left column: Scrollable list of 28 buffs
                int listX = areaX + 6;
                int listY = areaY + 6;
                int listW = 95;
                int listH = areaH - 12;

                g.fill(listX, listY, listX + listW, listY + listH, 0x1A000000);
                g.fill(listX, listY, listX + listW, listY + 1, borderColor);
                g.fill(listX, listY + listH - 1, listX + listW, listY + listH, borderColor);
                g.fill(listX, listY, listX + 1, listY + listH, borderColor);
                g.fill(listX + listW - 1, listY, listX + listW, listY + listH, borderColor);

                g.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);
                for (int b = 0; b < allBuffs.size(); b++) {
                    int by = listY + 2 + b * 16 - (int) bestiaryScrollAmount;
                    if (by + 14 < listY || by > listY + listH) continue;

                    EliteBuff buff = allBuffs.get(b);
                    boolean isSel = (this.selectedBestiaryIndex == b);
                    boolean bHov = mouseX >= listX + 2 && mouseX < listX + listW - 2 && mouseY >= by && mouseY < by + 14;

                    int itemBg = isSel ? 0xCC00FFCC : (bHov ? 0x4400FFCC : 0x1A00FFCC);
                    int itemTxt = isSel ? 0xFF08111E : (bHov ? 0xFFFFFFFF : 0xFF888888);

                    g.fill(listX + 2, by, listX + listW - 2, by + 14, itemBg);
                    g.drawString(this.font, buff.getDisplayName().getString(), listX + 6, by + 3, itemTxt, false);
                }
                g.disableScissor();

                // Right details panel
                int detX = areaX + 106;
                int detY = areaY + 6;
                int detW = areaW - 112;

                EliteBuff selBuff = allBuffs.get(this.selectedBestiaryIndex);
                g.drawString(this.font, selBuff.getDisplayName().getString(), detX, detY + 2, 0xFF00FFCC, false);

                // Tier cycling (BRONZE, SILVER, GOLD)
                MedallionType tier = MedallionType.values()[this.selectedBestiaryTierIndex % MedallionType.values().length];
                String tierName = switch (tier) {
                    case COMMON -> "BRONZE";
                    case RARE -> "SILVER";
                    case LEGENDARY -> "GOLD";
                };
                int tierColor = switch (tier) {
                    case COMMON -> 0xFFCD7F32;   // Bronze
                    case RARE -> 0xFFC0C0C0;     // Silver
                    case LEGENDARY -> 0xFFFFD700; // Gold
                };

                // Render Tier Badge
                g.fill(detX, detY + 14, detX + 46, detY + 24, tierColor);
                g.drawString(this.font, tierName, detX + 4, detY + 15, 0xFF08111E, false);

                // Defeated Elites kill counter
                int kills = 0;
                if (this.minecraft != null && this.minecraft.player != null) {
                    kills = this.minecraft.player.getPersistentData().getInt("xebKilled_" + selBuff.getId());
                }
                g.drawString(this.font, translate("gui.xeb.enigma_bios.bestiary.kills") + kills, detX + 52, detY + 15, 0xFFFFCC00, false);

                // FLOATING 3D MEDALLION MODEL RENDER IN GUI (ENLARGED TO 2.2X SCALE, FLOATING FREELY NO RECUADRO)
                float rotAngle = (System.currentTimeMillis() % 3600L) / 10.0F;
                int renderCenterX = detX + detW - 32;
                int renderCenterY = detY + 36;

                g.pose().pushPose();
                g.pose().translate(renderCenterX, renderCenterY, 150.0F);
                g.pose().scale(2.2F, 2.2F, 2.2F);

                // Render authentic 3D mob medallion with rotAngle, tier texture, and buff icon PNG
                MedallionRenderLayer.renderSingleMedallionGUI(g.pose(), g.bufferSource(), tier, selBuff.getId(), rotAngle, 0xF000F0);
                g.pose().popPose();

                g.fill(detX, detY + 28, detX + detW - 65, detY + 29, 0x4400FFCC);

                // Mechanical Mob Details & Counter-Strategy Text
                String descText = translate("xeb.buff." + selBuff.getId() + ".desc");
                if (descText.equals("xeb.buff." + selBuff.getId() + ".desc")) {
                    descText = "Medallón Élite (" + tierName + "): Confiere propiedades especiales de combate y defensivas a la entidad huésped.";
                }
                List<FormattedText> descLines = this.font.getSplitter().splitLines(descText, detW - 65, net.minecraft.network.chat.Style.EMPTY);
                int dY = detY + 34;
                for (FormattedText line : descLines) {
                    if (dY < detY + 80) {
                        g.drawString(this.font, line.getString(), detX, dY, 0xFFE0E0E0, false);
                        dY += 10;
                    }
                }

                g.drawString(this.font, translate("gui.xeb.enigma_bios.bestiary.strategy"), detX, detY + 84, 0xFF00FFCC, false);
                String stratText = translate("xeb.buff." + selBuff.getId() + ".counter");
                if (stratText.equals("xeb.buff." + selBuff.getId() + ".counter")) {
                    stratText = "Usa encantamientos especiales o ataques combinados para contrarrestar este efecto.";
                }
                List<FormattedText> stratLines = this.font.getSplitter().splitLines(stratText, detW - 4, net.minecraft.network.chat.Style.EMPTY);
                int sY = detY + 96;
                for (FormattedText line : stratLines) {
                    if (sY < detY + areaH - 4) {
                        g.drawString(this.font, line.getString(), detX, sY, 0xFF888888, false);
                        sY += 10;
                    }
                }
            }
        } else {
            // RENDER LOG (Bitácoras 1-5)
            int index = this.activeTab - 2;
            if (index >= 0 && index < logs.size()) {
                boolean isUnlocked = this.minecraft != null && this.minecraft.player != null &&
                        this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + (index + 1));

                if (!isUnlocked) {
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

        int invLeft = this.leftPos + (this.guiWidth - 162) / 2;
        int invTop = this.topPos + 160;

        ItemStack hoveredStack = ItemStack.EMPTY;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = invLeft + col * 18;
                int y = invTop + row * 18;
                g.fill(x, y, x + 18, y + 18, 0x3300FFCC);
                g.fill(x, y, x + 18, y + 1, 0x5500FFCC);
                g.fill(x, y + 17, x + 18, y + 18, 0x5500FFCC);
                g.fill(x, y, x + 1, y + 18, 0x5500FFCC);
                g.fill(x + 17, y, x + 18, y + 18, 0x5500FFCC);

                ItemStack stack = mc.player.getInventory().getItem(9 + row * 9 + col);
                if (!stack.isEmpty()) {
                    g.renderFakeItem(stack, x + 1, y + 1);
                    g.renderItemDecorations(this.font, stack, x + 1, y + 1);
                    if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                        hoveredStack = stack;
                    }
                }
            }
        }

        for (int col = 0; col < 9; col++) {
            int x = invLeft + col * 18;
            int y = invTop + 58;
            g.fill(x, y, x + 18, y + 18, 0x3300FFCC);
            g.fill(x, y, x + 18, y + 1, 0x5500FFCC);
            g.fill(x, y + 17, x + 18, y + 18, 0x5500FFCC);
            g.fill(x, y, x + 1, y + 18, 0x5500FFCC);
            g.fill(x + 17, y, x + 18, y + 18, 0x5500FFCC);

            ItemStack stack = mc.player.getInventory().getItem(col);
            if (!stack.isEmpty()) {
                g.renderFakeItem(stack, x + 1, y + 1);
                g.renderItemDecorations(this.font, stack, x + 1, y + 1);
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    hoveredStack = stack;
                }
            }
        }

        // Render floating Minecraft item tooltip on inventory hover!
        if (!hoveredStack.isEmpty()) {
            g.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private ItemStack getStackAtMouse(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;

        int invLeft = this.leftPos + (this.guiWidth - 162) / 2;
        int invTop = this.topPos + 160;

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

        if (scaledMouseX >= startX && scaledMouseX < startX + 65 && scaledMouseY >= viewportY && scaledMouseY < viewportY + viewportH) {
            int maxTabScroll = Math.max(0, (2 + logs.size()) * 22 - viewportH);
            if (maxTabScroll > 0) {
                this.tabScrollAmount = net.minecraft.util.Mth.clamp(this.tabScrollAmount - (float) delta * 11.0F, 0.0F, maxTabScroll);
                return true;
            }
        }

        if (scaledMouseX >= areaX && scaledMouseX < areaX + areaW && scaledMouseY >= areaY && scaledMouseY < areaY + areaH) {
            if (this.activeTab == 1) {
                int totalBuffs = EliteBuffRegistry.getAll().size();
                int maxScroll = Math.max(0, totalBuffs * 16 - (areaH - 12));
                if (maxScroll > 0) {
                    this.bestiaryScrollAmount = net.minecraft.util.Mth.clamp(this.bestiaryScrollAmount - (float) delta * 9.0F, 0.0F, maxScroll);
                    return true;
                }
            } else if (this.activeTab >= 2) {
                int index = this.activeTab - 2;
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

        int maxTabScroll = Math.max(0, (2 + logs.size()) * 22 - viewportH);
        if (maxTabScroll > 0 && scaledMouseX >= startX + 61 && scaledMouseX < startX + 64 && scaledMouseY >= viewportY && scaledMouseY < viewportY + viewportH) {
            this.isDraggingTabScroll = true;
            return true;
        }

        for (int i = 0; i < 2 + logs.size(); i++) {
            int y = viewportY + i * 22 - (int) tabScrollAmount;
            if (scaledMouseX >= startX && scaledMouseX < startX + 60 && scaledMouseY >= y && scaledMouseY < y + 20
                    && y >= viewportY && y + 20 <= viewportY + viewportH) {
                this.activeTab = i;
                this.contentScrollAmount = 0.0F;
                this.analyzerScrollAmount = 0.0F;
                this.bestiaryScrollAmount = 0.0F;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }

        if (this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
            int hudBtnX = areaX + areaW - 84;
            int hudBtnY = areaY + 6;
            int hudBtnW = 78;
            int hudBtnH = 12;
            if (scaledMouseX >= hudBtnX && scaledMouseX < hudBtnX + hudBtnW && scaledMouseY >= hudBtnY && scaledMouseY < hudBtnY + hudBtnH) {
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.minecraft.setScreen(new HUDPositionScreen(this, this.analyzedStack));
                }
                return true;
            }

            // Click check for move selector buttons 0..4
            int btnW = 50;
            int btnH = 14;
            int btnY = areaY + 56;
            for (int b = 0; b < 5; b++) {
                int bx = areaX + 12 + b * 52;
                if (scaledMouseX >= bx && scaledMouseX < bx + btnW && scaledMouseY >= btnY && scaledMouseY < btnY + btnH) {
                    this.selectedAbilityIndex = b;
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }
        }

        if (this.activeTab == 1) {
            List<EliteBuff> allBuffs = new ArrayList<>(EliteBuffRegistry.getAll());
            int listX = areaX + 6;
            int listY = areaY + 6;
            int listW = 95;
            int listH = areaH - 12;

            if (scaledMouseX >= listX && scaledMouseX < listX + listW && scaledMouseY >= listY && scaledMouseY < listY + listH) {
                for (int b = 0; b < allBuffs.size(); b++) {
                    int by = listY + 2 + b * 16 - (int) bestiaryScrollAmount;
                    if (scaledMouseX >= listX + 2 && scaledMouseX < listX + listW - 2 && scaledMouseY >= by && scaledMouseY < by + 14) {
                        this.selectedBestiaryIndex = b;
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                        return true;
                    }
                }
            }

            // Click check for 3D floating medallion model tier cycle
            int detX = areaX + 106;
            int detY = areaY + 6;
            int detW = areaW - 112;
            int renderCenterX = detX + detW - 32;
            int renderCenterY = detY + 36;

            double dist = Math.hypot(scaledMouseX - renderCenterX, scaledMouseY - renderCenterY);
            if (dist <= 36.0) {
                this.selectedBestiaryTierIndex = (this.selectedBestiaryTierIndex + 1) % 3;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                }
                return true;
            }
        }

        ItemStack clickedStack = getStackAtMouse(scaledMouseX, scaledMouseY);
        if (!clickedStack.isEmpty()) {
            this.analyzedStack = clickedStack.copy();
            this.activeTab = 0;
            this.analyzerScrollAmount = 0.0F;
            AnalyzedInfo info = analyzeItem(this.analyzedStack);

            if (info.translationKey.equals("item.unknown")) {
                this.lastAnalyzedUnknown = true;
                this.lastAnalyzedTime = System.currentTimeMillis();
                this.unknownTextIndex = (int) (Math.random() * 5);
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 0.8F));
                }
            } else {
                this.lastAnalyzedUnknown = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.5F));
                }
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDraggingTabScroll = false;
        this.isDraggingContentScroll = false;
        this.isDraggingAnalyzerScroll = false;
        this.isDraggingBestiaryScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
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
                    new boolean[]{false, false, false, false, false});
        }
        if (item == ModItems.DOOMFIST.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist", true,
                    new String[]{"10", "6-12", "5", "6", ""},
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
