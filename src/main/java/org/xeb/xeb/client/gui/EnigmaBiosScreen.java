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
    private int selectedAbilityIndex = 0; // 0: Clic Izq, 1: Clic Der, 2: Activa 1, 3: Activa 2, 4: Ultimate

    // Lore Logs
    private final List<LogEntry> logs = new ArrayList<>();

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
        // Dynamic scaling and layout calculation happens in render/mouseClicked
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Render blur background at original display scale
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

        // Futuristic neon cyan borders
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + 1, 0xFF00FFCC);
        g.fill(this.leftPos, this.topPos + this.guiHeight - 1, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, 0xFF00FFCC);
        g.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.guiHeight, 0xFF00FFCC);
        g.fill(this.leftPos + this.guiWidth - 1, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, 0xFF00FFCC);

        // Neon corners brackets
        // Top-left
        g.fill(this.leftPos - 2, this.topPos - 2, this.leftPos + 8, this.topPos + 1, 0xFF00FFCC);
        g.fill(this.leftPos - 2, this.topPos - 2, this.leftPos + 1, this.topPos + 8, 0xFF00FFCC);
        // Top-right
        g.fill(this.leftPos + this.guiWidth - 8, this.topPos - 2, this.leftPos + this.guiWidth + 2, this.topPos + 1, 0xFF00FFCC);
        g.fill(this.leftPos + this.guiWidth - 1, this.topPos - 2, this.leftPos + this.guiWidth + 2, this.topPos + 8, 0xFF00FFCC);
        // Bottom-left
        g.fill(this.leftPos - 2, this.topPos + this.guiHeight - 1, this.leftPos + 8, this.topPos + this.guiHeight + 2, 0xFF00FFCC);
        g.fill(this.leftPos - 2, this.topPos + this.guiHeight - 8, this.leftPos + 1, this.topPos + this.guiHeight + 2, 0xFF00FFCC);
        // Bottom-right
        g.fill(this.leftPos + this.guiWidth - 8, this.topPos + this.guiHeight - 1, this.leftPos + this.guiWidth + 2, this.topPos + this.guiHeight + 2, 0xFF00FFCC);
        g.fill(this.leftPos + this.guiWidth - 1, this.topPos + this.guiHeight - 8, this.leftPos + this.guiWidth + 2, this.topPos + this.guiHeight + 2, 0xFF00FFCC);

        // Scanline effect
        long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        int scanY = (int) ((time * 1.5) % (this.guiHeight - 20)) + this.topPos + 10;
        g.fill(this.leftPos + 2, scanY, this.leftPos + this.guiWidth - 2, scanY + 1, 0x1A00FFCC);

        // Divider between content and inventory
        g.fill(this.leftPos + 6, this.topPos + 155, this.leftPos + this.guiWidth - 6, this.topPos + 156, 0x3300FFCC);

        // Title and decorative texts (Localized)
        g.drawString(this.font, translate("gui.xeb.enigma_bios.title"), this.leftPos + 10, this.topPos + 5, 0xFF00FFCC, false);
        g.drawString(this.font, translate("gui.xeb.enigma_bios.status"), this.leftPos + this.guiWidth - 90, this.topPos + 5, 0x8800FFCC, false);

        // Render Tabs (Left side)
        renderTabs(g, scaledMouseX, scaledMouseY);

        // Render Content Area
        renderContent(g, scaledMouseX, scaledMouseY);

        // Render Player Inventory
        renderInventory(g, scaledMouseX, scaledMouseY);

        // Tooltip rendering for inventory items (within scaled space)
        ItemStack hoveredStack = getStackAtMouse(scaledMouseX, scaledMouseY);
        if (!hoveredStack.isEmpty()) {
            g.renderTooltip(this.font, hoveredStack, scaledMouseX, scaledMouseY);
        }

        super.render(g, scaledMouseX, scaledMouseY, partialTick);

        g.pose().popPose();
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        int startX = this.leftPos + 6;
        for (int i = 0; i < 6; i++) {
            int y = this.topPos + 18 + i * 22;
            boolean active = (this.activeTab == i);
            boolean hovered = mouseX >= startX && mouseX < startX + 60 && mouseY >= y && mouseY < y + 20;

            int bgColor = active ? 0xCC00FFCC : (hovered ? 0x4400FFCC : 0x1A00FFCC);
            int textColor = active ? 0xFF08111E : (hovered ? 0xFFFFFFFF : 0xFF888888);

            // Tab Box
            g.fill(startX, y, startX + 60, y + 20, bgColor);
            // Tab Border
            g.fill(startX, y, startX + 60, y + 1, 0x3300FFCC);
            g.fill(startX, y + 19, startX + 60, y + 20, 0x3300FFCC);
            g.fill(startX, y, startX + 1, y + 20, 0x3300FFCC);
            g.fill(startX + 59, y, startX + 60, y + 20, 0x3300FFCC);

            String title = (i == 0) ? translate("gui.xeb.enigma_bios.tab.analyze") : "BIT. #" + i;
            int textW = this.font.width(title);
            g.drawString(this.font, title, startX + (60 - textW) / 2, y + 6, textColor, false);
        }
    }

    private void renderContent(GuiGraphics g, int mouseX, int mouseY) {
        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        // Content panel frame
        g.fill(areaX, areaY, areaX + areaW, areaY + areaH, 0x1A000000);
        g.fill(areaX, areaY, areaX + areaW, areaY + 1, 0x4400FFCC);
        g.fill(areaX, areaY + areaH - 1, areaX + areaW, areaY + areaH, 0x4400FFCC);
        g.fill(areaX, areaY, areaX + 1, areaY + areaH, 0x4400FFCC);
        g.fill(areaX + areaW - 1, areaY, areaX + areaW, areaY + areaH, 0x4400FFCC);

        if (this.activeTab == 0) {
            // Render Analyzer Tab
            int slotX = areaX + 12;
            int slotY = areaY + 8;

            // Slot Background frame
            g.fill(slotX - 1, slotY - 1, slotX + 21, slotY + 21, 0x3300FFCC);
            g.fill(slotX, slotY, slotX + 20, slotY + 20, 0xFF08111E);

            // Inner cyan outline
            g.fill(slotX, slotY, slotX + 20, slotY + 1, 0xAA00FFCC);
            g.fill(slotX, slotY + 19, slotX + 20, slotY + 20, 0xAA00FFCC);
            g.fill(slotX, slotY, slotX + 1, slotY + 20, 0xAA00FFCC);
            g.fill(slotX + 19, slotY, slotX + 20, slotY + 20, 0xAA00FFCC);

            if (this.analyzedStack.isEmpty()) {
                // Empty analyzer text
                String placeText = translate("gui.xeb.enigma_bios.empty.1");
                String belowText = translate("gui.xeb.enigma_bios.empty.2");
                g.drawCenteredString(this.font, placeText, areaX + areaW / 2, areaY + 50, 0xFF888888);
                g.drawCenteredString(this.font, belowText, areaX + areaW / 2, areaY + 64, 0xFF666666);
            } else {
                // Render the analyzed item in the slot
                g.renderFakeItem(this.analyzedStack, slotX + 2, slotY + 2);
                g.renderItemDecorations(this.font, this.analyzedStack, slotX + 2, slotY + 2);

                // Fetch analyzed details
                AnalyzedInfo info = analyzeItem(this.analyzedStack);

                // Print Name aligned next to slot
                g.drawString(this.font, translate("gui.xeb.enigma_bios.label.name") + info.name, areaX + 38, areaY + 14, 0xFF00FFCC, false);

                // Print Extended Lore (starting lower, 3 lines limit)
                String loreText = translate(info.translationKey + ".enigma_lore");
                int textY = areaY + 32;
                List<FormattedText> loreLines = this.font.getSplitter().splitLines(loreText, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                for (FormattedText line : loreLines) {
                    if (textY < areaY + 62) {
                        g.drawString(this.font, line.getString(), areaX + 12, textY, 0xFFE0E0E0, false);
                        textY += 10;
                    }
                }

                if (info.hasAbilities) {
                    // Render Ability Buttons
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
                        "gui.xeb.enigma_bios.btn.ultimate"
                    };

                    for (int k = 0; k < 5; k++) {
                        int bx = btnStartX + k * (btnW + btnSpacing);
                        boolean disabled = info.isAbilityDisabled(k);
                        boolean active = (this.selectedAbilityIndex == k) && !disabled;
                        boolean hovered = mouseX >= bx && mouseX < bx + btnW && mouseY >= btnY && mouseY < btnY + btnH && !disabled;

                        int bgColor;
                        int textColor;
                        int borderColor = disabled ? 0x22888888 : 0x3300FFCC;

                        if (disabled) {
                            bgColor = 0x1A444444;
                            textColor = 0xFF555555;
                        } else if (active) {
                            bgColor = 0xCC00FFCC;
                            textColor = 0xFF08111E;
                        } else if (hovered) {
                            bgColor = 0x4400FFCC;
                            textColor = 0xFFFFFFFF;
                        } else {
                            bgColor = 0x1A00FFCC;
                            textColor = 0xFF888888;
                        }

                        // Draw Button Box
                        g.fill(bx, btnY, bx + btnW, btnY + btnH, bgColor);
                        // Draw Button Border
                        g.fill(bx, btnY, bx + btnW, btnY + 1, borderColor);
                        g.fill(bx, btnY + btnH - 1, bx + btnW, btnY + btnH, borderColor);
                        g.fill(bx, btnY, bx + 1, btnY + btnH, borderColor);
                        g.fill(bx + btnW - 1, btnY, bx + btnW, btnY + btnH, borderColor);

                        String btnText = translate(btnKeys[k]);
                        int textW = this.font.width(btnText);
                        g.drawString(this.font, btnText, bx + (btnW - textW) / 2, btnY + 3, textColor, false);
                    }

                    // Render selected ability details
                    int detailY = areaY + 86;
                    int idx = this.selectedAbilityIndex;
                    
                    if (idx == 4) {
                        // Ultimate details: show pending
                        g.drawString(this.font, translate("gui.xeb.enigma_bios.btn.ultimate"), areaX + 12, detailY, 0xFF00FFCC, false);
                        g.drawString(this.font, translate("gui.xeb.enigma_bios.ultimate_pending"), areaX + 12, detailY + 12, 0xFFFF5555, false);
                    } else {
                        String abilityNameKey = "";
                        switch (idx) {
                            case 0: abilityNameKey = "left_click"; break;
                            case 1: abilityNameKey = "right_click"; break;
                            case 2: abilityNameKey = "active1"; break;
                            case 3: abilityNameKey = "active2"; break;
                        }

                        String abName = translate(info.translationKey + ".ability." + abilityNameKey + ".name");
                        String abDesc = translate(info.translationKey + ".ability." + abilityNameKey + ".desc");
                        String dmg = info.damages[idx];
                        String cd = info.cooldowns[idx];

                        // Draw Ability Name
                        g.drawString(this.font, abName, areaX + 12, detailY, 0xFF00FFCC, false);

                        // Draw Stats: Daño | CD
                        String statsStr = translate("gui.xeb.enigma_bios.label.damage") + dmg + " | " + translate("gui.xeb.enigma_bios.label.cooldown") + cd;
                        g.drawString(this.font, statsStr, areaX + 12, detailY + 11, 0xFFFFFF55, false);

                        // Draw Description
                        int descY = detailY + 22;
                        List<FormattedText> descLines = this.font.getSplitter().splitLines(abDesc, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                        for (FormattedText line : descLines) {
                            if (descY < areaY + areaH - 4) {
                                g.drawString(this.font, line.getString(), areaX + 12, descY, 0xFFB0B0B0, false);
                                descY += 9;
                            }
                        }
                    }
                } else {
                    // Render Common Item effect
                    int effectY = areaY + 68;
                    String effectKey = info.translationKey + ".enigma_effect";
                    String effectText = translate(effectKey);

                    g.drawString(this.font, translate("gui.xeb.enigma_bios.label.effect"), areaX + 12, effectY, 0xFFFFFF55, false);

                    int descY = effectY + 12;
                    List<FormattedText> effectLines = this.font.getSplitter().splitLines(effectText, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                    for (FormattedText line : effectLines) {
                        if (descY < areaY + areaH - 4) {
                            g.drawString(this.font, line.getString(), areaX + 12, descY, 0xFFB0B0B0, false);
                            descY += 10;
                        }
                    }
                }
            }
        } else {
            // Render Log tab (Bitácoras 1-5)
            int index = this.activeTab - 1;
            if (index >= 0 && index < logs.size()) {
                LogEntry log = logs.get(index);
                
                // Draw Title (Localized)
                g.drawString(this.font, translate(log.titleKey), areaX + 12, areaY + 8, 0xFF00FFCC, false);
                g.fill(areaX + 12, areaY + 19, areaX + areaW - 12, areaY + 20, 0x4400FFCC);

                // Draw wrapped log content (Localized)
                int textY = areaY + 26;
                List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                for (FormattedText line : lines) {
                    if (textY < areaY + areaH - 8) {
                        g.drawString(this.font, line.getString(), areaX + 12, textY, 0xFFE0E0E0, false);
                        textY += 10;
                    }
                }
            }
        }
    }

    private void renderInventory(GuiGraphics g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int invLeft = this.leftPos + 99;
        int invTop = this.topPos + 165;

        // Draw slots for inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = invLeft + col * 18;
                int y = invTop + row * 18;

                // Render Slot Frame
                renderSlotBackground(g, x, y, mouseX, mouseY);

                // Render Item
                int slotIndex = 9 + row * 9 + col;
                ItemStack stack = mc.player.getInventory().getItem(slotIndex);
                if (!stack.isEmpty()) {
                    g.renderFakeItem(stack, x + 1, y + 1);
                    g.renderItemDecorations(this.font, stack, x + 1, y + 1);
                }
            }
        }

        // Draw slots for hotbar (1 row of 9)
        for (int col = 0; col < 9; col++) {
            int x = invLeft + col * 18;
            int y = invTop + 58;

            // Render Slot Frame
            renderSlotBackground(g, x, y, mouseX, mouseY);

            // Render Item
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
        
        // Slot Outline
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

        // Inventory lookup
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = invLeft + col * 18;
                int y = invTop + row * 18;
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    return mc.player.getInventory().getItem(9 + row * 9 + col);
                }
            }
        }

        // Hotbar lookup
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = getScaleFactor();
        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        int scaledWidth = (int) (this.width / scale);
        int scaledHeight = (int) (this.height / scale);
        this.leftPos = (scaledWidth - this.guiWidth) / 2;
        this.topPos = (scaledHeight - this.guiHeight) / 2;

        // Tab clicks
        int tabX = this.leftPos + 6;
        for (int i = 0; i < 6; i++) {
            int y = this.topPos + 18 + i * 22;
            if (scaledMouseX >= tabX && scaledMouseX < tabX + 60 && scaledMouseY >= y && scaledMouseY < y + 20) {
                this.activeTab = i;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }

        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;

        // Ability button clicks (if analyzed item has abilities and activeTab is Analyzer)
        if (this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
            AnalyzedInfo info = analyzeItem(this.analyzedStack);
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
                            if (this.minecraft != null) {
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            }
                        }
                        return true;
                    }
                }
            }
        }

        // Slot/Inventory clicks
        ItemStack clickedStack = getStackAtMouse(scaledMouseX, scaledMouseY);
        if (!clickedStack.isEmpty()) {
            this.analyzedStack = clickedStack.copy();
            AnalyzedInfo info = analyzeItem(this.analyzedStack);
            // Default to Right Click (index 1) if Left Click is disabled
            this.selectedAbilityIndex = (info.hasAbilities && info.isAbilityDisabled(0)) ? 1 : 0;
            this.activeTab = 0; // Switch to analyzer tab
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.5F));
            }
            return true;
        }

        // Clear analyzer slot click
        int slotX = areaX + 12;
        int slotY = areaY + 8;
        if (this.activeTab == 0 && scaledMouseX >= slotX && scaledMouseX < slotX + 20 && scaledMouseY >= slotY && scaledMouseY < slotY + 20) {
            if (!this.analyzedStack.isEmpty()) {
                this.analyzedStack = ItemStack.EMPTY;
                this.selectedAbilityIndex = 0;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8F));
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private AnalyzedInfo analyzeItem(ItemStack stack) {
        Item item = stack.getItem();
        String name = stack.getHoverName().getString();

        // Check if it is a weapon/weird item with active abilities
        if (item == ModItems.GOLDEN_FLOWER.get()) {
            return new AnalyzedInfo(name, "item.xeb.golden_flower", true,
                    new String[]{"2", "6", "4", "5", ""},
                    new String[]{"2s", "Passive", "8s", "12s", ""});
        }
        if (item == ModItems.DOOMFIST_V2.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist_v2", true,
                    new String[]{"8", "8-15", "6", "0", ""},
                    new String[]{"0.5s", "3s", "6s", "8s", ""});
        }
        if (item == ModItems.DOOMFIST.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist", true,
                    new String[]{"10", "6-12", "5", "5", ""},
                    new String[]{"0.5s", "3s", "5s", "6s", ""});
        }
        if (item == ModItems.OPTIC_BLAST.get()) {
            return new AnalyzedInfo(name, "item.xeb.optic_blast", true,
                    new String[]{"3", "5 / tick", "4", "6", ""},
                    new String[]{"0.5s", "Energy", "10s", "8s", ""});
        }
        if (item == ModItems.HOLY_DUALITY_BLADE.get()) {
            return new AnalyzedInfo(name, "item.xeb.holy_duality_blade", true,
                    new String[]{"8", "18", "10", "12", ""},
                    new String[]{"Standard", "20s", "10s", "15s", ""});
        }
        if (item == ModItems.MECHA_OVERDRIVE.get()) {
            return new AnalyzedInfo(name, "item.xeb.mecha_overdrive", true,
                    new String[]{"", "2", "8", "7", ""},
                    new String[]{"", "0s", "4s", "8s", ""},
                    new boolean[]{true, false, false, false, false}); // Left Click disabled
        }
        if (item == ModItems.BROKEN_DIAMOND.get()) {
            return new AnalyzedInfo(name, "item.xeb.broken_diamond", true,
                    new String[]{"", "8", "8", "0", ""},
                    new String[]{"", "Variable", "5s", "15s", ""},
                    new boolean[]{true, false, false, false, false}); // Left Click disabled
        }
        if (item == ModItems.THE_TEARS.get()) {
            return new AnalyzedInfo(name, "item.xeb.the_tears", true,
                    new String[]{"4", "8", "5", "0", ""},
                    new String[]{"0.4s", "2s", "10s", "15s", ""});
        }

        // Common items (hasAbilities = false)
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

        // Default fallback for any other items
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
