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
    private int selectedAbilityIndex = 0; // Set to first enabled ability index

    private int selectedBestiaryIndex = 0;
    private int selectedBestiaryTierIndex = 0; // 0: BRONZE, 1: SILVER, 2: GOLD

    // Lore Logs (Bitácoras 1 to 5)
    private final List<LogEntry> logs = new ArrayList<>();

    // Independent Scrolling states
    private float tabScrollAmount = 0.0F;
    private float contentScrollAmount = 0.0F;
    private float analyzerScrollAmount = 0.0F;
    private float bestiaryListScrollAmount = 0.0F;
    private float bestiaryDetailsScrollAmount = 0.0F;

    private long lastTabScrollTime = 0L;
    private long lastAnalyzerScrollTime = 0L;
    private long lastBestiaryListScrollTime = 0L;
    private long lastBestiaryDetailsScrollTime = 0L;
    private long lastLogScrollTime = 0L;

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

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Native Minecraft GUI scaling ensures 100% GL Scissor clipping on all GUI scales!
        this.leftPos = (this.width - this.guiWidth) / 2;
        this.topPos = (this.height - this.guiHeight) / 2;

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

        // Futuristic Calm Sci-Fi Scanlines & Grid Lines
        renderFuturisticBackgroundScanlines(g, borderColor);

        // Frame Border Lines
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + 2, borderColor);
        g.fill(this.leftPos, this.topPos + this.guiHeight - 2, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, borderColor);
        g.fill(this.leftPos, this.topPos, this.leftPos + 2, this.topPos + this.guiHeight, borderColor);
        g.fill(this.leftPos + this.guiWidth - 2, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, borderColor);

        // Header Title Bar: ENIGMA BIOS v1.0 | STATUS: LINKED
        g.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.guiWidth - 4, this.topPos + 16, 0x3300FFCC);
        g.drawString(this.font, "ENIGMA BIOS v1.0", this.leftPos + 8, this.topPos + 6, borderColor, false);
        String statusText = translate("gui.xeb.enigma_bios.status");
        if (statusText.equals("gui.xeb.enigma_bios.status")) statusText = "SYSTEM ACTIVE";
        g.drawString(this.font, statusText, this.leftPos + this.guiWidth - 8 - this.font.width(statusText), this.topPos + 6, borderColor, false);

        renderTabs(g, mouseX, mouseY);
        renderContent(g, mouseX, mouseY, borderColor, flashing, elapsed);
        renderInventory(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderFuturisticBackgroundScanlines(GuiGraphics g, int borderColor) {
        long time = System.currentTimeMillis();
        int scanY = (int) ((time % 8000L) / 80.0F * (this.guiHeight / 100.0F));

        g.enableScissor(this.leftPos + 2, this.topPos + 2, this.leftPos + this.guiWidth - 2, this.topPos + this.guiHeight - 2);

        for (int offset = 0; offset < this.guiHeight; offset += 36) {
            int lineY = this.topPos + ((scanY + offset) % this.guiHeight);
            g.fill(this.leftPos + 2, lineY, this.leftPos + this.guiWidth - 2, lineY + 1, 0x0E00FFCC);
        }

        for (int x = this.leftPos + 36; x < this.leftPos + this.guiWidth; x += 36) {
            g.fill(x, this.topPos + 2, x + 1, this.topPos + this.guiHeight - 2, 0x0600FFCC);
        }

        g.disableScissor();
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 130;
        int totalTabs = 2 + logs.size();
        int contentHeight = totalTabs * 22;
        int maxTabScroll = Math.max(0, contentHeight - viewportH);

        long scrollElapsed = System.currentTimeMillis() - this.lastTabScrollTime;
        if (maxTabScroll > 0 && scrollElapsed < 2000L) {
            float fade = 1.0F - (scrollElapsed / 2000.0F);
            int alpha = (int) (255 * fade);
            int trackCol = (alpha / 4 << 24) | 0x00FFCC;
            int thumbCol = (alpha << 24) | 0x00FFCC;

            int scrollbarX = startX + 61;
            g.fill(scrollbarX, viewportY, scrollbarX + 3, viewportY + viewportH, trackCol);
            int thumbH = Math.max(10, (int) ((float) viewportH * viewportH / contentHeight));
            int thumbY = viewportY + (int) ((float) tabScrollAmount * (viewportH - thumbH) / maxTabScroll);
            thumbY = net.minecraft.util.Mth.clamp(thumbY, viewportY, viewportY + viewportH - thumbH);
            g.fill(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbH, thumbCol);
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
            // RENDER ANALYZER WITH PRECISE NON-OVERLAPPING LAYOUT AND SCISSORS
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

                if (info.hasAbilities) {
                    // RENDER [ Customize HUD ] BUTTON ONLY FOR WEAPONS WITH CUSTOM IN-GAME HUDS!
                    if (info.hasCustomHUD) {
                        int hudBtnX = areaX + areaW - 84;
                        int hudBtnY = areaY + 6;
                        int hudBtnW = 78;
                        int hudBtnH = 12;
                        boolean hudHovered = mouseX >= hudBtnX && mouseX < hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY < hudBtnY + hudBtnH;

                        int hudBg = hudHovered ? 0x9900FFCC : 0x3300FFCC;
                        int hudTxt = hudHovered ? 0xFF08111E : 0xFF00FFCC;
                        g.fill(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH, hudBg);
                        g.fill(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + 1, 0x6600FFCC);
                        g.fill(hudBtnX, hudBtnY + hudBtnH - 1, hudBtnX + hudBtnW, hudBtnY + hudBtnH, 0x6600FFCC);
                        g.fill(hudBtnX, hudBtnY, hudBtnX + 1, hudBtnY + hudBtnH, 0x6600FFCC);
                        g.fill(hudBtnX + hudBtnW - 1, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH, 0x6600FFCC);
                        g.drawString(this.font, translate("gui.xeb.enigma_bios.btn.customize_hud"), hudBtnX + 4, hudBtnY + 2, hudTxt, false);
                    }

                    // Scissored Top Lore Section (strictly max 2-3 lines between areaY + 22 and areaY + 50)
                    String loreText = translate(info.translationKey + ".enigma_lore");
                    int maxLoreW = info.hasCustomHUD ? (areaW - 120) : (areaW - 40);
                    List<FormattedText> loreLines = this.font.getSplitter().splitLines(loreText, maxLoreW, net.minecraft.network.chat.Style.EMPTY);

                    g.enableScissor(areaX + 34, areaY + 22, areaX + areaW - 6, areaY + 50);
                    int textY = areaY + 22;
                    for (FormattedText line : loreLines) {
                        if (textY < areaY + 48) {
                            g.drawString(this.font, line.getString(), areaX + 34, textY, 0xFFFFFFFF, false);
                            textY += 9;
                        }
                    }
                    g.disableScissor();

                    // 5 Ability Buttons at areaY + 54
                    String[] btnKeys = {
                            "gui.xeb.enigma_bios.btn.left_click",
                            "gui.xeb.enigma_bios.btn.right_click",
                            "gui.xeb.enigma_bios.btn.active1",
                            "gui.xeb.enigma_bios.btn.active2",
                            "gui.xeb.enigma_bios.btn.extreme_burst"
                    };

                    int btnW = 50;
                    int btnH = 14;
                    int btnY = areaY + 54;

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
                                default -> "Burst";
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

                    // Render Selected Move Details with STRICT TEXT WRAPPING AND AUTO-FADING SCROLLBAR
                    int detailY = areaY + 72;
                    int detailH = areaY + areaH - 4 - detailY;
                    int idx = this.selectedAbilityIndex;

                    g.enableScissor(areaX + 12, detailY, areaX + areaW - 12, areaY + areaH - 4);

                    if (idx == 4) {
                        ExtremeBurstRegistry.ExtremeBurstEntry burstEntry = null;
                        Item item = this.analyzedStack.getItem();
                        if (item == ModItems.GOLDEN_FLOWER.get()) {
                            burstEntry = ExtremeBurstRegistry.getEntry(ModItems.OMEGA_FLOWERY.get());
                        } else if (item == ModItems.THE_TEARS.get()) {
                            burstEntry = ExtremeBurstRegistry.getEntry(ModItems.DOGMA.get());
                        }

                        if (burstEntry != null) {
                            String burstName = translate(burstEntry.curioItem.getDescriptionId());
                            if (burstName.equals(burstEntry.curioItem.getDescriptionId())) {
                                burstName = (item == ModItems.GOLDEN_FLOWER.get()) ? "Omega Flowery" : "Dogma";
                            }
                            g.drawString(this.font, burstName, areaX + 12, detailY, 0xFF00FFCC, false);

                            String typeStr = (burstEntry.type == ExtremeBurstRegistry.BurstType.UNIVERSAL ? "Universal" : "Limited");
                            String verStr = (burstEntry.version == ExtremeBurstRegistry.BurstVersion.INSTANT ? "Instant" : "Instance");
                            String statLine = String.format("Type: %s | Version: %s | Cooldown: %ds", typeStr, verStr, burstEntry.cooldownTicks / 20);
                            g.drawString(this.font, statLine, areaX + 12, detailY + 11, 0xFFFFCC00, false);

                            String desc = translate(info.translationKey + ".enigma_effect");
                            if (desc.equals(info.translationKey + ".enigma_effect")) {
                                desc = translate(burstEntry.curioItem.getDescriptionId() + ".enigma_effect");
                            }
                            if (desc.contains(".enigma_effect")) {
                                desc = (item == ModItems.GOLDEN_FLOWER.get())
                                        ? "Entra en instancia Omega Flowery 20 s: sin gravedad, Final Jarona (x2), enfriamientos 50% mas rapidos."
                                        : "Dispara un rayo de Azufre creciente (9.99 danio/tick, 10 s). Gana automaticamente los duelos de rayos.";
                            }
                            List<FormattedText> descLines = this.font.getSplitter().splitLines(desc, areaW - 28, net.minecraft.network.chat.Style.EMPTY);
                            int dy = detailY + 22;
                            for (FormattedText line : descLines) {
                                if (dy < areaY + areaH - 4) {
                                    g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFFFFFFF, false);
                                    dy += 9;
                                }
                            }
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

                        String statLine = String.format("Damage: %s | Cooldown: %s", dmg, cd);
                        g.drawString(this.font, statLine, areaX + 12, detailY + 11, 0xFFFFCC00, false);

                        List<FormattedText> descLines = this.font.getSplitter().splitLines(abDesc, areaW - 28, net.minecraft.network.chat.Style.EMPTY);
                        int dy = detailY + 22;
                        for (FormattedText line : descLines) {
                            if (dy < areaY + areaH - 4) {
                                g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFFFFFFF, false);
                                dy += 9;
                            }
                        }
                    }

                    g.disableScissor();
                } else {
                    // NON-WEAPON ITEM: Start description text at areaY + 34
                    int descY = areaY + 34;
                    int descH = areaY + areaH - 6 - descY;

                    String effectKey = info.translationKey.equals("item.unknown")
                            ? "item.unknown.enigma_effect." + this.unknownTextIndex
                            : info.translationKey + ".enigma_effect";
                    String effectText = translate(effectKey);
                    if (effectText.equals(effectKey) || effectText.isEmpty()) {
                        effectText = translate(info.translationKey + ".enigma_lore");
                    }
                    if (effectText.equals(info.translationKey + ".enigma_lore") || effectText.isEmpty()) {
                        effectText = "Objeto analizado: Sin propiedades de combate reliquia detectadas.";
                    }

                    List<FormattedText> effectLines = this.font.getSplitter().splitLines(effectText, areaW - 28, net.minecraft.network.chat.Style.EMPTY);
                    int totalHeight = effectLines.size() * 10;
                    int maxScroll = Math.max(0, totalHeight - descH);

                    long scrollElapsed = System.currentTimeMillis() - this.lastAnalyzerScrollTime;
                    if (maxScroll > 0 && scrollElapsed < 2000L) {
                        float fade = 1.0F - (scrollElapsed / 2000.0F);
                        int alpha = (int) (255 * fade);
                        int trackCol = (alpha / 4 << 24) | 0x00FFCC;
                        int thumbCol = (alpha << 24) | 0x00FFCC;

                        int scrollX = areaX + areaW - 6;
                        g.fill(scrollX, descY, scrollX + 3, descY + descH, trackCol);
                        int thumbH = Math.max(8, (int) ((float) descH * descH / totalHeight));
                        int thumbY = descY + (int) ((float) analyzerScrollAmount * (descH - thumbH) / maxScroll);
                        thumbY = net.minecraft.util.Mth.clamp(thumbY, descY, descY + descH - thumbH);
                        g.fill(scrollX, thumbY, scrollX + 3, thumbY + thumbH, thumbCol);
                    }

                    g.enableScissor(areaX + 12, descY, areaX + areaW - 12, areaY + areaH - 4);
                    int dy = descY - (int) analyzerScrollAmount;
                    for (FormattedText line : effectLines) {
                        g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFFFFFFF, false);
                        dy += 10;
                    }
                    g.disableScissor();
                }
            }
        } else if (this.activeTab == 1) {
            // RENDER ELITE BESTIARIO WITH DUAL INDEPENDENT SCROLLBARS AND STRICT LIMITS
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

                int totalListH = allBuffs.size() * 16;
                int maxListScroll = Math.max(0, totalListH - (listH - 4));
                this.bestiaryListScrollAmount = net.minecraft.util.Mth.clamp(this.bestiaryListScrollAmount, 0.0F, maxListScroll);

                long listScrollElapsed = System.currentTimeMillis() - this.lastBestiaryListScrollTime;
                if (maxListScroll > 0 && listScrollElapsed < 2000L) {
                    float fade = 1.0F - (listScrollElapsed / 2000.0F);
                    int alpha = (int) (255 * fade);
                    int trackCol = (alpha / 4 << 24) | 0x00FFCC;
                    int thumbCol = (alpha << 24) | 0x00FFCC;

                    int scrollX = listX + listW - 4;
                    g.fill(scrollX, listY + 2, scrollX + 2, listY + listH - 2, trackCol);
                    int thumbH = Math.max(8, (int) ((float) listH * listH / totalListH));
                    int thumbY = listY + 2 + (int) ((float) bestiaryListScrollAmount * (listH - 4 - thumbH) / maxListScroll);
                    thumbY = net.minecraft.util.Mth.clamp(thumbY, listY + 2, listY + listH - 2 - thumbH);
                    g.fill(scrollX, thumbY, scrollX + 2, thumbY + thumbH, thumbCol);
                }

                g.enableScissor(listX + 1, listY + 1, listX + listW - 5, listY + listH - 1);
                for (int b = 0; b < allBuffs.size(); b++) {
                    int by = listY + 2 + b * 16 - (int) bestiaryListScrollAmount;
                    if (by + 14 < listY || by > listY + listH) continue;

                    EliteBuff buff = allBuffs.get(b);
                    boolean isSel = (this.selectedBestiaryIndex == b);
                    boolean bHov = mouseX >= listX + 2 && mouseX < listX + listW - 6 && mouseY >= by && mouseY < by + 14;

                    int itemBg = isSel ? 0xCC00FFCC : (bHov ? 0x4400FFCC : 0x1A00FFCC);
                    int itemTxt = isSel ? 0xFF08111E : (bHov ? 0xFFFFFFFF : 0xFF888888);

                    g.fill(listX + 2, by, listX + listW - 6, by + 14, itemBg);
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
                    case COMMON -> 0xFFCD7F32;
                    case RARE -> 0xFFC0C0C0;
                    case LEGENDARY -> 0xFFFFD700;
                };

                g.fill(detX, detY + 14, detX + 46, detY + 24, tierColor);
                g.drawString(this.font, tierName, detX + 4, detY + 15, 0xFF08111E, false);

                int kills = 0;
                if (this.minecraft != null && this.minecraft.player != null) {
                    kills = this.minecraft.player.getPersistentData().getInt("xebKilled_" + selBuff.getId());
                }
                g.drawString(this.font, translate("gui.xeb.enigma_bios.bestiary.kills") + kills, detX + 52, detY + 15, 0xFFFFCC00, false);

                // FLOATING 3D MEDALLION MODEL
                float rotAngle = (System.currentTimeMillis() % 3600L) / 10.0F;
                int renderCenterX = detX + detW - 32;
                int renderCenterY = detY + 36;

                g.pose().pushPose();
                g.pose().translate(renderCenterX, renderCenterY, 150.0F);
                g.pose().scale(2.2F, 2.2F, 2.2F);
                MedallionRenderLayer.renderSingleMedallionGUI(g.pose(), g.bufferSource(), tier, selBuff.getId(), rotAngle, 0xF000F0);
                g.pose().popPose();

                g.fill(detX, detY + 28, detX + detW - 65, detY + 29, 0x4400FFCC);

                // 100% CODE-EXACT PER-BUFF QUALITATIVE & QUANTITATIVE TIER SCALING
                String tierQualityText = getBuffTierQualityDescription(selBuff, tier);

                // Scissored Bestiary Details with Strict Scrollbar Limit
                int descStartY = detY + 34;
                int descMaxH = areaH - 42;

                String descText = translate("xeb.buff." + selBuff.getId() + ".desc");
                if (descText.equals("xeb.buff." + selBuff.getId() + ".desc")) {
                    descText = "Medallon Elite (" + tierName + "): Confiere propiedades especiales a la entidad huesped.";
                }

                String stratText = translate("xeb.buff." + selBuff.getId() + ".counter");
                if (stratText.equals("xeb.buff." + selBuff.getId() + ".counter")) {
                    stratText = "Usa encantamientos especiales o ataques combinados para contrarrestar este efecto.";
                }

                String fullBestiaryText = descText + "\n\nQualities & Effects (" + tierName + "):\n" + tierQualityText + "\n\nCounter Strategy:\n" + stratText;
                List<FormattedText> bestiaryLines = this.font.getSplitter().splitLines(fullBestiaryText, detW - 68, net.minecraft.network.chat.Style.EMPTY);

                int totalBestiaryH = bestiaryLines.size() * 10;
                int maxBestiaryDetailsScroll = Math.max(0, totalBestiaryH - descMaxH);
                this.bestiaryDetailsScrollAmount = net.minecraft.util.Mth.clamp(this.bestiaryDetailsScrollAmount, 0.0F, maxBestiaryDetailsScroll);

                long detailsScrollElapsed = System.currentTimeMillis() - this.lastBestiaryDetailsScrollTime;
                if (maxBestiaryDetailsScroll > 0 && detailsScrollElapsed < 2000L) {
                    float fade = 1.0F - (detailsScrollElapsed / 2000.0F);
                    int alpha = (int) (255 * fade);
                    int trackCol = (alpha / 4 << 24) | 0x00FFCC;
                    int thumbCol = (alpha << 24) | 0x00FFCC;

                    int scrollX = detX + detW - 62;
                    g.fill(scrollX, descStartY, scrollX + 3, descStartY + descMaxH, trackCol);
                    int thumbH = Math.max(8, (int) ((float) descMaxH * descMaxH / totalBestiaryH));
                    int thumbY = descStartY + (int) ((float) bestiaryDetailsScrollAmount * (descMaxH - thumbH) / maxBestiaryDetailsScroll);
                    thumbY = net.minecraft.util.Mth.clamp(thumbY, descStartY, descStartY + descMaxH - thumbH);
                    g.fill(scrollX, thumbY, scrollX + 3, thumbY + thumbH, thumbCol);
                }

                g.enableScissor(detX, descStartY, detX + detW - 65, detY + areaH - 4);
                int bY = descStartY - (int) bestiaryDetailsScrollAmount;
                for (FormattedText line : bestiaryLines) {
                    g.drawString(this.font, line.getString(), detX, bY, 0xFFE0E0E0, false);
                    bY += 10;
                }
                g.disableScissor();
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

                    long logScrollElapsed = System.currentTimeMillis() - this.lastLogScrollTime;
                    if (maxScroll > 0 && logScrollElapsed < 2000L) {
                        float fade = 1.0F - (logScrollElapsed / 2000.0F);
                        int alpha = (int) (255 * fade);
                        int trackCol = (alpha / 4 << 24) | 0x00FFCC;
                        int thumbCol = (alpha << 24) | 0x00FFCC;

                        int scrollX = areaX + areaW - 6;
                        g.fill(scrollX, textY, scrollX + 3, textY + textH, trackCol);
                        int thumbH = Math.max(10, (int) ((float) textH * textH / totalHeight));
                        int thumbY = textY + (int) ((float) contentScrollAmount * (textH - thumbH) / maxScroll);
                        thumbY = net.minecraft.util.Mth.clamp(thumbY, textY, textY + textH - thumbH);
                        g.fill(scrollX, thumbY, scrollX + 3, thumbY + thumbH, thumbCol);
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

    private String getBuffTierQualityDescription(EliteBuff buff, MedallionType tier) {
        String id = buff.getId();
        return switch (id) {
            case "depressing" -> switch (tier) {
                case COMMON -> "Applies ALL_STATS_DOWN I (move speed, attack damage, armor reduced) to entities within 10 blocks.";
                case RARE -> "Applies ALL_STATS_DOWN II (greater stat reduction) to entities within 10 blocks.";
                case LEGENDARY -> "Applies ALL_STATS_DOWN III (severe crippling stat reduction) to entities within 10 blocks.";
            };
            case "slightly_depressing" -> switch (tier) {
                case COMMON -> "Applies ALL_STATS_DOWN I to entities within 2 blocks.";
                case RARE -> "Applies ALL_STATS_DOWN II to entities within 2 blocks.";
                case LEGENDARY -> "Applies ALL_STATS_DOWN III to entities within 2 blocks.";
            };
            case "mirror" -> switch (tier) {
                case COMMON -> "Applies REFLECT II status effect (reflects incoming damage).";
                case RARE -> "Applies REFLECT IV status effect (greater damage reflection).";
                case LEGENDARY -> "Applies REFLECT VI status effect (massive damage reflection).";
            };
            case "evolving" -> switch (tier) {
                case COMMON -> "Attaches a new random medallion every 30 seconds of combat (Cap 5).";
                case RARE -> "Attaches a new random medallion every 20 seconds of combat (Cap 5).";
                case LEGENDARY -> "Attaches a new random medallion every 10 seconds of combat (Cap 5).";
            };
            case "absorbent" -> switch (tier) {
                case COMMON -> "Drains 1 Mana/sec from stationary targets within 6 blocks into magic damage.";
                case RARE -> "Drains 2 Mana/sec from stationary targets within 8 blocks into magic damage.";
                case LEGENDARY -> "Drains 4 Mana/sec from stationary targets within 12 blocks into magic damage.";
            };
            case "hardy" -> switch (tier) {
                case COMMON -> "Instantly clears Freeze state and Petrify debuffs.";
                case RARE -> "Instantly clears Freeze, Petrify, and Slowness debuffs.";
                case LEGENDARY -> "Instantly clears ALL crowd-control debuffs and grants immunity.";
            };
            case "spiky" -> switch (tier) {
                case COMMON -> "Reflects 20% melee damage back to attackers.";
                case RARE -> "Reflects 40% melee damage & inflicts Bleeding (3s).";
                case LEGENDARY -> "Reflects 70% melee damage, inflicts Bleeding, & fires radial thorn bursts.";
            };
            case "reactive" -> switch (tier) {
                case COMMON -> "Grants Regeneration I for 3s upon taking damage.";
                case RARE -> "Grants Regeneration II & Resistance I for 5s upon taking damage.";
                case LEGENDARY -> "Grants Regeneration III, Resistance II, & Knockback Immunity for 8s.";
            };
            case "damaging" -> switch (tier) {
                case COMMON -> "+25% Melee Attack Damage.";
                case RARE -> "+50% Melee Damage & Critical Striking.";
                case LEGENDARY -> "+100% Melee Damage & Armor-Penetrating Strikes.";
            };
            case "tough" -> switch (tier) {
                case COMMON -> "+20% Knockback Resistance & +4 Armor.";
                case RARE -> "+50% Knockback Resistance & +8 Armor.";
                case LEGENDARY -> "+100% Knockback Resistance, +14 Armor, & Damage Cap per hit.";
            };
            case "shielded" -> switch (tier) {
                case COMMON -> "Absorbs 1 projectile attack every 10 seconds.";
                case RARE -> "Absorbs 2 projectile attacks every 8 seconds.";
                case LEGENDARY -> "Absorbs ALL ranged projectiles & reflects arrows back at attackers.";
            };
            case "protected" -> switch (tier) {
                case COMMON -> "Barrier absorbs 30% of incoming damage.";
                case RARE -> "Barrier absorbs 50% damage & grants Resistance II while active.";
                case LEGENDARY -> "Barrier absorbs 75% damage & triggers Invulnerability Stasis when broken.";
            };
            case "speedy" -> switch (tier) {
                case COMMON -> "+30% Movement Speed & Step Height.";
                case RARE -> "+60% Movement Speed & Swift Attack Speed.";
                case LEGENDARY -> "+100% Movement Speed, Teleport Dash behind targets, & Haste III.";
            };
            case "flaming" -> switch (tier) {
                case COMMON -> "Ignites attackers for 3s & Fire Immunity.";
                case RARE -> "Ignites attackers for 6s & leaves Flame Trails on ground.";
                case LEGENDARY -> "Ignites attackers for 10s, Hellfire Eruptions on hit, & Lava Healing.";
            };
            case "creepy" -> switch (tier) {
                case COMMON -> "Emits a small explosive burst upon death.";
                case RARE -> "Emits a volatile fiery explosion on death (Radius 3.5).";
                case LEGENDARY -> "Detonates a thermonuclear explosion on death (Radius 6.0) with residual fallout.";
            };
            case "lucky" -> switch (tier) {
                case COMMON -> "25% Chance to dodge incoming attacks completely.";
                case RARE -> "45% Chance to dodge attacks & +50% Critical Strike Chance.";
                case LEGENDARY -> "65% Chance to dodge attacks & Fortune Counter Strikes.";
            };
            case "static" -> switch (tier) {
                case COMMON -> "Zap nearby enemies with Chain Lightning (3.0 damage).";
                case RARE -> "Chain Lightning hits up to 3 targets (6.0 damage) with Slowness.";
                case LEGENDARY -> "Thunderstorms hit up to 6 targets with Stun & Shock.";
            };
            case "bouncy" -> switch (tier) {
                case COMMON -> "+50% Jump Height & Fall Damage Immunity.";
                case RARE -> "+100% Jump Height & Ground Slam Shockwave on landing.";
                case LEGENDARY -> "Super Jumps & Meteor Impact Shockwaves (12 damage).";
            };
            case "resonant" -> switch (tier) {
                case COMMON -> "Sonic pulse pushes nearby entities 3 blocks away.";
                case RARE -> "Sonic shockwave disarms target and pushes entities 6 blocks away.";
                case LEGENDARY -> "Devastating Sonic Boom crushes armor & launches targets 12 blocks up.";
            };
            case "undying" -> switch (tier) {
                case COMMON -> "Revives once upon death with 20% HP.";
                case RARE -> "Revives once with 50% HP & 5s Invulnerability Stasis.";
                case LEGENDARY -> "Revives TWICE with 100% HP, Invulnerability, & Wrath Buff.";
            };
            case "healthy" -> switch (tier) {
                case COMMON -> "+50% Maximum Health.";
                case RARE -> "+100% Maximum Health & Regeneration I.";
                case LEGENDARY -> "+200% Maximum Health, Regeneration II, & Resistance I.";
            };
            case "sandy" -> switch (tier) {
                case COMMON -> "Blinds nearby attackers for 2s in a Sandstorm.";
                case RARE -> "Blinds & slows attackers in a dense Sandstorm aura (5 block radius).";
                case LEGENDARY -> "Suffocates attackers while gaining 50% Dodge chance inside Sandstorm.";
            };
            case "infested" -> switch (tier) {
                case COMMON -> "Spawns 2 Silverfish upon taking heavy damage.";
                case RARE -> "Spawns 4 Silverfish / Endermites upon taking damage.";
                case LEGENDARY -> "Spawns a swarming horde of 8 aggressive Parasites that inflict Wither.";
            };
            case "plow" -> switch (tier) {
                case COMMON -> "Charges through blocks & knockbacks targets.";
                case RARE -> "Bulldozer charge breaks weak blocks & deals 8 damage.";
                case LEGENDARY -> "Juggernaut Ram crushes obsidian/blocks & launches targets into the sky.";
            };
            case "mega" -> switch (tier) {
                case COMMON -> "Entity size +50%, +40% Health, +30% Damage.";
                case RARE -> "Entity size +100%, +80% Health, +60% Damage.";
                case LEGENDARY -> "Titan Size +200%, +150% Health, +120% Damage, & Ground Tremors.";
            };
            case "mad" -> switch (tier) {
                case COMMON -> "Attacks all nearby living entities in Madness mode.";
                case RARE -> "Madness mode +30% Attack Speed & Berserk Frenzy.";
                case LEGENDARY -> "Blood Madness: +100% Attack Speed, Lifesteal, & attacks indiscriminately.";
            };
            case "twin" -> switch (tier) {
                case COMMON -> "Spawns 1 Minion clone on engagement.";
                case RARE -> "Spawns 1 Identical Twin clone with shared stats.";
                case LEGENDARY -> "Spawns 2 Empowered Twin clones that share damage and coordinate attacks.";
            };
            case "sticky" -> switch (tier) {
                case COMMON -> "Disarms attacker on melee hit (15% chance).";
                case RARE -> "Disarms attacker (35% chance) & sticks to weapons.";
                case LEGENDARY -> "60% Disarm chance & pulls disarmed weapons into entity inventory.";
            };
            default -> switch (tier) {
                case COMMON -> "Bronze Tier: +30% Base Stats.";
                case RARE -> "Silver Tier: +60% Base Stats.";
                case LEGENDARY -> "Gold Tier: +100% Base Stats & Special Aura.";
            };
        };
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
        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 130;

        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        if (mouseX >= startX && mouseX < startX + 65 && mouseY >= viewportY && mouseY < viewportY + viewportH) {
            int maxTabScroll = Math.max(0, (2 + logs.size()) * 22 - viewportH);
            if (maxTabScroll > 0) {
                this.lastTabScrollTime = System.currentTimeMillis();
                this.tabScrollAmount = net.minecraft.util.Mth.clamp(this.tabScrollAmount - (float) delta * 11.0F, 0.0F, maxTabScroll);
                return true;
            }
        }

        if (mouseX >= areaX && mouseX < areaX + areaW && mouseY >= areaY && mouseY < areaY + areaH) {
            if (this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
                AnalyzedInfo info = analyzeItem(this.analyzedStack);
                if (!info.hasAbilities) {
                    int descY = areaY + 34;
                    int descH = areaY + areaH - 6 - descY;
                    String effectKey = info.translationKey.equals("item.unknown")
                            ? "item.unknown.enigma_effect." + this.unknownTextIndex
                            : info.translationKey + ".enigma_effect";
                    String effectText = translate(effectKey);
                    if (effectText.equals(effectKey) || effectText.isEmpty()) effectText = translate(info.translationKey + ".enigma_lore");
                    List<FormattedText> lines = this.font.getSplitter().splitLines(effectText, areaW - 28, net.minecraft.network.chat.Style.EMPTY);
                    int maxScroll = Math.max(0, lines.size() * 10 - descH);
                    if (maxScroll > 0) {
                        this.lastAnalyzerScrollTime = System.currentTimeMillis();
                        this.analyzerScrollAmount = net.minecraft.util.Mth.clamp(this.analyzerScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                        return true;
                    }
                }
            } else if (this.activeTab == 1) {
                int listX = areaX + 6;
                int listW = 95;
                int detX = areaX + 106;
                int detW = areaW - 112;

                if (mouseX >= listX && mouseX < listX + listW) {
                    int totalBuffs = EliteBuffRegistry.getAll().size();
                    int maxScroll = Math.max(0, totalBuffs * 16 - (areaH - 16));
                    if (maxScroll > 0) {
                        this.lastBestiaryListScrollTime = System.currentTimeMillis();
                        this.bestiaryListScrollAmount = net.minecraft.util.Mth.clamp(this.bestiaryListScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                        return true;
                    }
                } else if (mouseX >= detX && mouseX < detX + detW) {
                    List<EliteBuff> allBuffs = new ArrayList<>(EliteBuffRegistry.getAll());
                    if (!allBuffs.isEmpty() && this.selectedBestiaryIndex >= 0 && this.selectedBestiaryIndex < allBuffs.size()) {
                        EliteBuff selBuff = allBuffs.get(this.selectedBestiaryIndex);
                        MedallionType tier = MedallionType.values()[this.selectedBestiaryTierIndex % MedallionType.values().length];
                        String tierQualityText = getBuffTierQualityDescription(selBuff, tier);
                        String descText = translate("xeb.buff." + selBuff.getId() + ".desc");
                        String stratText = translate("xeb.buff." + selBuff.getId() + ".counter");
                        String fullText = descText + "\n\nQualities & Effects:\n" + tierQualityText + "\n\nCounter Strategy:\n" + stratText;
                        List<FormattedText> bestiaryLines = this.font.getSplitter().splitLines(fullText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                        int totalH = bestiaryLines.size() * 10;
                        int maxScroll = Math.max(0, totalH - (areaH - 42));
                        if (maxScroll > 0) {
                            this.lastBestiaryDetailsScrollTime = System.currentTimeMillis();
                            this.bestiaryDetailsScrollAmount = net.minecraft.util.Mth.clamp(this.bestiaryDetailsScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                            return true;
                        }
                    }
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
                            this.lastLogScrollTime = System.currentTimeMillis();
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
        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 130;

        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        for (int i = 0; i < 2 + logs.size(); i++) {
            int y = viewportY + i * 22 - (int) tabScrollAmount;
            if (mouseX >= startX && mouseX < startX + 60 && mouseY >= y && mouseY < y + 20
                    && y >= viewportY && y + 20 <= viewportY + viewportH) {
                this.activeTab = i;
                this.contentScrollAmount = 0.0F;
                this.analyzerScrollAmount = 0.0F;
                this.bestiaryListScrollAmount = 0.0F;
                this.bestiaryDetailsScrollAmount = 0.0F;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }

        if (this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
            AnalyzedInfo info = analyzeItem(this.analyzedStack);
            if (info.hasAbilities) {
                if (info.hasCustomHUD) {
                    int hudBtnX = areaX + areaW - 84;
                    int hudBtnY = areaY + 6;
                    int hudBtnW = 78;
                    int hudBtnH = 12;
                    if (mouseX >= hudBtnX && mouseX < hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY < hudBtnY + hudBtnH) {
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            this.minecraft.setScreen(new HUDPositionScreen(this, this.analyzedStack));
                        }
                        return true;
                    }
                }

                int btnW = 50;
                int btnH = 14;
                int btnY = areaY + 54;
                for (int b = 0; b < 5; b++) {
                    int bx = areaX + 12 + b * 52;
                    if (mouseX >= bx && mouseX < bx + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                        if (info.isAbilityDisabled(b)) {
                            return false;
                        }
                        this.selectedAbilityIndex = b;
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                        return true;
                    }
                }
            }
        }

        if (this.activeTab == 1) {
            List<EliteBuff> allBuffs = new ArrayList<>(EliteBuffRegistry.getAll());
            int listX = areaX + 6;
            int listY = areaY + 6;
            int listW = 95;
            int listH = areaH - 12;

            if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                for (int b = 0; b < allBuffs.size(); b++) {
                    int by = listY + 2 + b * 16 - (int) bestiaryListScrollAmount;
                    if (mouseX >= listX + 2 && mouseX < listX + listW - 2 && mouseY >= by && mouseY < by + 14) {
                        this.selectedBestiaryIndex = b;
                        this.bestiaryDetailsScrollAmount = 0.0F;
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                        return true;
                    }
                }
            }

            int detX = areaX + 106;
            int detY = areaY + 6;
            int detW = areaW - 112;
            int renderCenterX = detX + detW - 32;
            int renderCenterY = detY + 36;

            double dist = Math.hypot(mouseX - renderCenterX, mouseY - renderCenterY);
            if (dist <= 36.0) {
                this.selectedBestiaryTierIndex = (this.selectedBestiaryTierIndex + 1) % 3;
                this.bestiaryDetailsScrollAmount = 0.0F;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                }
                return true;
            }
        }

        ItemStack clickedStack = getStackAtMouse(mouseX, mouseY);
        if (!clickedStack.isEmpty()) {
            this.analyzedStack = clickedStack.copy();
            this.activeTab = 0;
            this.analyzerScrollAmount = 0.0F;
            AnalyzedInfo info = analyzeItem(this.analyzedStack);

            // ALWAYS RESET TO FIRST ENABLED ABILITY INDEX! (Prevents stuck selection on disabled buttons like Left Click for Crazy Diamond)
            this.selectedAbilityIndex = info.getFirstEnabledAbilityIndex();

            if (info.translationKey.equals("item.unknown")) {
                this.lastAnalyzedUnknown = true;
                this.lastAnalyzedTime = System.currentTimeMillis();
                this.unknownTextIndex = (int) (Math.random() * 5);
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_DEACTIVATE, 0.5F));
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
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private AnalyzedInfo analyzeItem(ItemStack stack) {
        Item item = stack.getItem();
        String name = stack.getHoverName().getString();

        if (item == ModItems.GOLDEN_FLOWER.get()) {
            return new AnalyzedInfo(name, "item.xeb.golden_flower", true, true,
                    new String[]{"2", "6", "4", "5", ""},
                    new String[]{"2s", "Passive", "8s", "12s", ""});
        }
        if (item == ModItems.DOOMFIST_V2.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist_v2", true, true,
                    new String[]{"8", "8-15", "6", "0", ""},
                    new String[]{"0.5s", "3s", "6s", "8s", ""},
                    new boolean[]{false, false, false, false, false});
        }
        if (item == ModItems.DOOMFIST.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist", true, true,
                    new String[]{"10", "6-12", "5", "6", ""},
                    new String[]{"0.5s", "3s", "5s", "6s", ""},
                    new boolean[]{false, false, false, false, true});
        }
        if (item == ModItems.OPTIC_BLAST.get()) {
            return new AnalyzedInfo(name, "item.xeb.optic_blast", true, true,
                    new String[]{"3", "5 / tick", "4", "6", ""},
                    new String[]{"0.5s", "Energy", "10s", "8s", ""},
                    new boolean[]{false, false, false, false, true});
        }
        if (item == ModItems.HOLY_DUALITY_BLADE.get()) {
            return new AnalyzedInfo(name, "item.xeb.holy_duality_blade", true, true,
                    new String[]{"8", "18", "10", "12", ""},
                    new String[]{"Standard", "20s", "10s", "15s", ""},
                    new boolean[]{false, false, false, false, true});
        }
        if (item == ModItems.MECHA_OVERDRIVE.get()) {
            return new AnalyzedInfo(name, "item.xeb.mecha_overdrive", true, true,
                    new String[]{"", "2", "8", "7", ""},
                    new String[]{"", "0s", "4s", "8s", ""},
                    new boolean[]{true, false, false, false, true});
        }
        if (item == ModItems.BROKEN_DIAMOND.get()) {
            return new AnalyzedInfo(name, "item.xeb.broken_diamond", true, true,
                    new String[]{"", "8", "8", "0", ""},
                    new String[]{"", "Variable", "5s", "15s", ""},
                    new boolean[]{true, false, false, false, true});
        }
        if (item == ModItems.THE_TEARS.get()) {
            return new AnalyzedInfo(name, "item.xeb.the_tears", true, true,
                    new String[]{"4", "8", "5", "0", ""},
                    new String[]{"0.4s", "2s", "10s", "15s", ""});
        }
        if (item == ModItems.SMART_HALBERD.get()) {
            return new AnalyzedInfo(name, "item.xeb.smart_halberd", true, false,
                    new String[]{"9", "14", "", "", ""},
                    new String[]{"1.0s", "0s", "", "", ""},
                    new boolean[]{false, false, true, true, true});
        }

        if (item == ModItems.MOON_TEAR.get()) {
            return new AnalyzedInfo(name, "item.xeb.moon_tear", false, false, null, null);
        }
        if (item == ModItems.TINFOIL_HAT.get()) {
            return new AnalyzedInfo(name, "item.xeb.tinfoil_hat", false, false, null, null);
        }
        if (item == ModItems.HOLY_MANTLE.get()) {
            return new AnalyzedInfo(name, "item.xeb.holy_mantle", false, false, null, null);
        }
        if (item == ModItems.BRASS_KNUCKLES.get()) {
            return new AnalyzedInfo(name, "item.xeb.brass_knuckles", false, false, null, null);
        }
        if (item == ModItems.DEMON_CORE.get()) {
            return new AnalyzedInfo(name, "item.xeb.demon_core", false, false, null, null);
        }
        if (item == ModItems.MOB_ENERGY.get()) {
            return new AnalyzedInfo(name, "item.xeb.mob_energy", false, false, null, null);
        }

        return new AnalyzedInfo(name, "item.unknown", false, false, null, null);
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
        final boolean hasCustomHUD;
        final String[] damages;
        final String[] cooldowns;
        final boolean[] disabledAbilities;

        AnalyzedInfo(String name, String translationKey, boolean hasAbilities, boolean hasCustomHUD, String[] damages, String[] cooldowns) {
            this(name, translationKey, hasAbilities, hasCustomHUD, damages, cooldowns, new boolean[]{false, false, false, false, false});
        }

        AnalyzedInfo(String name, String translationKey, boolean hasAbilities, boolean hasCustomHUD, String[] damages, String[] cooldowns, boolean[] disabledAbilities) {
            this.name = name;
            this.translationKey = translationKey;
            this.hasAbilities = hasAbilities;
            this.hasCustomHUD = hasCustomHUD;
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

        public int getFirstEnabledAbilityIndex() {
            if (disabledAbilities != null) {
                for (int i = 0; i < disabledAbilities.length; i++) {
                    if (!disabledAbilities[i]) {
                        return i;
                    }
                }
            }
            return 0;
        }
    }
}
