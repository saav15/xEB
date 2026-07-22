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
import org.xeb.xeb.medallion.MedallionType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;

import java.util.ArrayList;
import java.util.List;

public class EnigmaBiosScreen extends Screen {
    private final int guiWidth = 400;
    private final int guiHeight = 260;
    private int leftPos;
    private int topPos;

    private int activeTab = 0; // 0: Analizador, 1: Bestiario, 2-6: Bitácoras 1-5
    private ItemStack analyzedStack = ItemStack.EMPTY;
    private int selectedAbilityIndex = 0; // 0: Clic Izq, 1: Clic Der, 2: Activa 1, 3: Activa 2, 4: Extreme Burst

    private int selectedBestiaryIndex = 0;
    private int selectedBestiaryTierIndex = 0; // 0: Bronze, 1: Silver, 2: Gold

    // Lore Logs (Bitácoras 1 to 5)
    private final List<LogEntry> logs = new ArrayList<>();

    // Scrolling states
    private float tabScrollAmount = 0.0F;
    private float contentScrollAmount = 0.0F;
    private float analyzerScrollAmount = 0.0F;
    private float bestiaryScrollAmount = 0.0F;
    private float bestiaryDetailScrollAmount = 0.0F;

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

        // Flash red if last analyzed item was unknown
        long elapsed = System.currentTimeMillis() - this.lastAnalyzedTime;
        boolean flashing = this.lastAnalyzedUnknown && (elapsed < 800L);
        if (flashing) {
            float flash = (float) Math.sin(elapsed * 0.02D);
            if (flash > 0) {
                borderColor = 0xFFFF3333;
            }
        }

        // Draw Frame Background
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, 0xEE08111E);

        // Frame Border Lines
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + 2, borderColor);
        g.fill(this.leftPos, this.topPos + this.guiHeight - 2, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, borderColor);
        g.fill(this.leftPos, this.topPos, this.leftPos + 2, this.topPos + this.guiHeight, borderColor);
        g.fill(this.leftPos + this.guiWidth - 2, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, borderColor);

        // Header Title Bar
        g.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.guiWidth - 4, this.topPos + 16, 0x3300FFCC);
        g.drawString(this.font, translate("gui.xeb.enigma_bios.title"), this.leftPos + 8, this.topPos + 6, borderColor, false);

        renderTabs(g, scaledMouseX, scaledMouseY);
        renderContent(g, scaledMouseX, scaledMouseY, borderColor, flashing, elapsed);
        renderInventory(g, scaledMouseX, scaledMouseY);

        super.render(g, scaledMouseX, scaledMouseY, partialTick);
        g.pose().popPose();
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 135;
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
            int borderColor = !isUnlocked ? 0x33FF5555 : 0x3300FFCC;

            g.fill(startX, y, startX + 60, y + 20, bgColor);
            g.fill(startX, y, startX + 60, y + 1, borderColor);
            g.fill(startX, y + 19, startX + 60, y + 20, borderColor);
            g.fill(startX, y, startX + 1, y + 20, borderColor);
            g.fill(startX + 59, y, startX + 60, y + 20, borderColor);

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
        int areaW = 320;
        int areaH = 135;

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

                int textColor = flashing ? 0xFFFF3333 : 0xFF00FFCC;
                g.drawString(this.font, translate("gui.xeb.enigma_bios.label.name") + info.name, areaX + 38, areaY + 14, textColor, false);

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

                // Customize HUD button
                int hudBtnX = areaX + areaW - 98;
                int hudBtnY = areaY + 6;
                int hudBtnW = 90;
                int hudBtnH = 14;
                boolean hudHovered = mouseX >= hudBtnX && mouseX < hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY < hudBtnY + hudBtnH;

                int hudBg = hudHovered ? 0xCC00FFCC : 0x2200FFCC;
                int hudTxt = hudHovered ? 0xFF08111E : 0xFF00FFCC;
                g.fill(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH, hudBg);
                g.fill(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + 1, 0xFF00FFCC);
                g.fill(hudBtnX, hudBtnY + hudBtnH - 1, hudBtnX + hudBtnW, hudBtnY + hudBtnH, 0xFF00FFCC);
                g.fill(hudBtnX, hudBtnY, hudBtnX + 1, hudBtnY + hudBtnH, 0xFF00FFCC);
                g.fill(hudBtnX + hudBtnW - 1, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH, 0xFF00FFCC);

                String hudBtnText = translate("gui.xeb.enigma_bios.btn.customize_hud");
                int hudTxtW = this.font.width(hudBtnText);
                g.drawString(this.font, hudBtnText, hudBtnX + (hudBtnW - hudTxtW) / 2, hudBtnY + 3, hudTxt, false);
            }
        } else if (this.activeTab == 1) {
            // RENDER ELITE BESTIARY
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

                // Tier cycling logic (Bronze, Silver, Gold)
                MedallionType tier = MedallionType.values()[this.selectedBestiaryTierIndex % MedallionType.values().length];
                String tierName = tier.name();
                int tierColor = switch (tier) {
                    case COMMON -> 0xFFCD7F32; // Bronze
                    case RARE -> 0xFFC0C0C0;   // Silver
                    case LEGENDARY -> 0xFFFFD700; // Gold
                };

                // Render Tier Badge
                g.fill(detX, detY + 14, detX + 44, detY + 24, tierColor);
                g.drawString(this.font, tierName, detX + 4, detY + 15, 0xFF08111E, false);

                // Interactive 3D Spinning Medallion Box in top right
                int box3DX = detX + detW - 36;
                int box3DY = detY + 2;
                int box3DS = 32;

                boolean box3DHovered = mouseX >= box3DX && mouseX < box3DX + box3DS && mouseY >= box3DY && mouseY < box3DY + box3DS;
                int box3DBorder = box3DHovered ? 0xFF00FFCC : tierColor;

                g.fill(box3DX, box3DY, box3DX + box3DS, box3DY + box3DS, 0x44000000);
                g.fill(box3DX, box3DY, box3DX + box3DS, box3DY + 1, box3DBorder);
                g.fill(box3DX, box3DY + box3DS - 1, box3DX + box3DS, box3DY + box3DS, box3DBorder);
                g.fill(box3DX, box3DY, box3DX + 1, box3DY + box3DS, box3DBorder);
                g.fill(box3DX + box3DS - 1, box3DY, box3DX + box3DS, box3DY + box3DS, box3DBorder);

                // Spin 3D Medallion model / item stack inside box
                long rotTime = System.currentTimeMillis();
                float angle = (rotTime % 3600L) / 10.0F;

                g.pose().pushPose();
                g.pose().translate(box3DX + 16, box3DY + 16, 100);
                g.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));
                g.pose().scale(1.2F, 1.2F, 1.2F);

                ItemStack medallionStack = switch (tier) {
                    case COMMON -> new ItemStack(net.minecraft.world.item.Items.RAW_COPPER);
                    case RARE -> new ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
                    case LEGENDARY -> new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT);
                };
                g.renderFakeItem(medallionStack, -8, -8);
                g.pose().popPose();

                // Kill counter
                int kills = 0;
                if (this.minecraft != null && this.minecraft.player != null) {
                    kills = this.minecraft.player.getPersistentData().getInt("xebKilled_" + selBuff.getId());
                }
                g.drawString(this.font, translate("gui.xeb.enigma_bios.bestiary.kills") + kills, detX + 50, detY + 15, 0xFFFFCC00, false);

                g.fill(detX, detY + 28, detX + detW, detY + 29, 0x4400FFCC);

                // Mechanical Mob Details & Counter-Strategy Text
                String descText = translate("xeb.buff." + selBuff.getId() + ".desc");
                if (descText.equals("xeb.buff." + selBuff.getId() + ".desc")) {
                    descText = "Medallón Élite (" + tierName + "): Confiere propiedades especiales de combate y defensivas a la entidad huésped.";
                }
                List<FormattedText> descLines = this.font.getSplitter().splitLines(descText, detW - 4, net.minecraft.network.chat.Style.EMPTY);
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
            }
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
        int viewportH = 135;

        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 320;
        int areaH = 135;

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
        int viewportH = 135;

        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 320;
        int areaH = 135;

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
            int hudBtnX = areaX + areaW - 98;
            int hudBtnY = areaY + 6;
            int hudBtnW = 90;
            int hudBtnH = 14;
            if (scaledMouseX >= hudBtnX && scaledMouseX < hudBtnX + hudBtnW && scaledMouseY >= hudBtnY && scaledMouseY < hudBtnY + hudBtnH) {
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.minecraft.setScreen(new HUDPositionScreen(this, this.analyzedStack));
                }
                return true;
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

            // Click check for 3D spinning medallion tier cycle box
            int detX = areaX + 106;
            int detY = areaY + 6;
            int detW = areaW - 112;
            int box3DX = detX + detW - 36;
            int box3DY = detY + 2;
            int box3DS = 32;

            if (scaledMouseX >= box3DX && scaledMouseX < box3DX + box3DS && scaledMouseY >= box3DY && scaledMouseY < box3DY + box3DS) {
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
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.5F));
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
        if (stack.isEmpty()) return new AnalyzedInfo("item.empty", translate("gui.xeb.enigma_bios.empty.1"), false);
        return new AnalyzedInfo(stack.getItem().getDescriptionId(), stack.getHoverName().getString(), true);
    }

    private static class LogEntry {
        public final String titleKey;
        public final String contentKey;

        public LogEntry(String titleKey, String contentKey) {
            this.titleKey = titleKey;
            this.contentKey = contentKey;
        }
    }

    private static class AnalyzedInfo {
        public final String translationKey;
        public final String name;
        public final boolean hasAbilities;

        public AnalyzedInfo(String translationKey, String name, boolean hasAbilities) {
            this.translationKey = translationKey;
            this.name = name;
            this.hasAbilities = hasAbilities;
        }
    }
}
