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
        // Return false so singleplayer world, weapon animations, entity ticks, and particles keep ticking live in background!
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

                // Render 5 Move Selector Buttons
                String[] btnLabels = {"Clic Izq.", "Clic Der.", "Activa 1", "Activa 2", "Ex. Burst"};
                int btnW = 56;
                int btnH = 14;
                int btnStartX = areaX + 12;
                int btnY = areaY + 34;

                for (int b = 0; b < 5; b++) {
                    int bx = btnStartX + b * 60;
                    boolean isSel = (this.selectedAbilityIndex == b);
                    boolean bHov = mouseX >= bx && mouseX < bx + btnW && mouseY >= btnY && mouseY < btnY + btnH;

                    int bg = isSel ? 0xCC00FFCC : (bHov ? 0x4400FFCC : 0x1A00FFCC);
                    int txt = isSel ? 0xFF08111E : (bHov ? 0xFFFFFFFF : 0xFF888888);

                    g.fill(bx, btnY, bx + btnW, btnY + btnH, bg);
                    g.fill(bx, btnY, bx + btnW, btnY + 1, 0x5500FFCC);
                    g.fill(bx, btnY + btnH - 1, bx + btnW, btnY + btnH, 0x5500FFCC);
                    g.fill(bx, btnY, bx + 1, btnY + btnH, 0x5500FFCC);
                    g.fill(bx + btnW - 1, btnY, bx + btnW, btnY + btnH, 0x5500FFCC);

                    int tw = this.font.width(btnLabels[b]);
                    g.drawString(this.font, btnLabels[b], bx + (btnW - tw) / 2, btnY + 3, txt, false);
                }

                // Ability Move Details Viewport
                int moveBoxY = areaY + 52;
                int moveBoxH = areaH - 58;
                g.fill(areaX + 12, moveBoxY, areaX + areaW - 12, moveBoxY + moveBoxH, 0x33000000);
                g.fill(areaX + 12, moveBoxY, areaX + areaW - 12, moveBoxY + 1, 0x3300FFCC);

                String moveTitle = getMoveTitle(info, this.selectedAbilityIndex);
                String moveDesc = getMoveDescription(info, this.selectedAbilityIndex);

                g.drawString(this.font, moveTitle, areaX + 16, moveBoxY + 4, 0xFF00FFCC, false);
                g.fill(areaX + 16, moveBoxY + 14, areaX + areaW - 16, moveBoxY + 15, 0x4400FFCC);

                List<FormattedText> moveLines = this.font.getSplitter().splitLines(moveDesc, areaW - 36, net.minecraft.network.chat.Style.EMPTY);
                int mY = moveBoxY + 18;
                for (FormattedText line : moveLines) {
                    if (mY < moveBoxY + moveBoxH - 4) {
                        g.drawString(this.font, line.getString(), areaX + 16, mY, 0xFFE0E0E0, false);
                        mY += 10;
                    }
                }
            }
        } else if (this.activeTab == 1) {
            // RENDER ELITE BESTIARIO
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

                // FLOATING 3D MEDALLION MODEL RENDER IN GUI (ENLARGED TO 2.2X SCALE)
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

    private String getMoveTitle(AnalyzedInfo info, int index) {
        String baseKey = info.translationKey;
        return switch (index) {
            case 0 -> {
                String name = translate(baseKey + ".ability.left_click.name");
                yield !name.equals(baseKey + ".ability.left_click.name") ? "[ Clic Izq. ] " + name : "[ Clic Izquierdo ] - Ataque Básico";
            }
            case 1 -> {
                String name = translate(baseKey + ".ability.right_click.name");
                yield !name.equals(baseKey + ".ability.right_click.name") ? "[ Clic Der. ] " + name : "[ Clic Derecho ] - Habilidad Secundaria";
            }
            case 2 -> {
                String name = translate(baseKey + ".ability.active1.name");
                yield !name.equals(baseKey + ".ability.active1.name") ? "[ Activa 1 ] " + name : "[ Activa 1 ] - Movimiento Especial A";
            }
            case 3 -> {
                String name = translate(baseKey + ".ability.active2.name");
                yield !name.equals(baseKey + ".ability.active2.name") ? "[ Activa 2 ] " + name : "[ Activa 2 ] - Movimiento Especial B";
            }
            case 4 -> {
                if (info.translationKey.contains("omega_flowery")) {
                    yield "[ EXTREME BURST ] - Estado Omega Flowery";
                } else if (info.translationKey.contains("dogma")) {
                    yield "[ EXTREME BURST ] - Haz Brimstone Dogma";
                }
                yield "[ EXTREME BURST ] - Definitiva Reliquia";
            }
            default -> "Movimiento Desconocido";
        };
    }

    private String getMoveDescription(AnalyzedInfo info, int index) {
        String baseKey = info.translationKey;
        if (index == 4) {
            // Extreme Burst Details
            String burstEffect = translate(baseKey + ".enigma_effect");
            if (!burstEffect.equals(baseKey + ".enigma_effect")) {
                return "ESTADO DEFINITIVO: " + burstEffect;
            }
            if (baseKey.contains("omega_flowery")) {
                return translate("item.xeb.omega_flowery.enigma_effect");
            } else if (baseKey.contains("dogma")) {
                return translate("item.xeb.dogma.enigma_effect");
            }
            return "RÁFAGA EXTREMA: Desata la potencia máxima del núcleo reliquia. Otorga invulnerabilidad temporal y potencia masivamente todas las activas.";
        }

        String moveKey = switch (index) {
            case 0 -> baseKey + ".ability.left_click.desc";
            case 1 -> baseKey + ".ability.right_click.desc";
            case 2 -> baseKey + ".ability.active1.desc";
            case 3 -> baseKey + ".ability.active2.desc";
            default -> "";
        };

        String desc = translate(moveKey);
        if (desc.equals(moveKey) || desc.isEmpty()) {
            // Check enigma lore fallback
            String lore = translate(baseKey + ".enigma_lore");
            if (!lore.equals(baseKey + ".enigma_lore")) {
                return lore;
            }
            return switch (index) {
                case 0 -> "Ejecuta un combo de tajos rápidos que infligen daño físico y empuje moderado.";
                case 1 -> "Canaliza la energía del arma para lanzar un proyectil o barrera defensiva.";
                case 2 -> "Inicia una maniobra de alta movilidad o embestida direccional.";
                case 3 -> "Desata un ataque de carga pesada que rompe la guardia enemiga.";
                default -> "Información de ataque no disponible.";
            };
        }
        return desc;
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

            // Click check for move selector buttons 0..4
            int btnW = 56;
            int btnH = 14;
            int btnStartX = areaX + 12;
            int btnY = areaY + 34;
            for (int b = 0; b < 5; b++) {
                int bx = btnStartX + b * 60;
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
