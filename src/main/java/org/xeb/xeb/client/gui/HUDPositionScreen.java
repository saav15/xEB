package org.xeb.xeb.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.xeb.xeb.Config;
import org.xeb.xeb.item.ModItems;

public class HUDPositionScreen extends Screen {
    private final Screen parent;
    private final ItemStack targetStack;

    public enum HUDCategory {
        DOOMFIST("Doomfist Gauntlet", "DOOMFIST"),
        OPTIC_BLAST("Optic Blast", "OPTIC_BLAST"),
        MECHA("Mecha Overdrive", "MECHA"),
        HOLY("Holy Duality Blade", "HOLY"),
        GOLDEN_FLOWER("Golden Flower", "GOLDEN_FLOWER"),
        CRAZY_DIAMOND("Crazy Diamond", "CRAZY_DIAMOND"),
        THE_TEARS("The Tears / Dogma", "THE_TEARS");

        public final String displayName;
        public final String id;

        HUDCategory(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }
    }

    private HUDCategory currentCategory = HUDCategory.DOOMFIST;

    private int hudX;
    private int hudY; // Distance from screen bottom
    private float hudScale = 1.0f;

    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private static final int GROUP_WIDTH = 84;
    private static final int GROUP_HEIGHT = 24;

    public HUDPositionScreen(Screen parent, ItemStack targetStack) {
        super(Component.literal("xEB Custom Weapon HUD Configuration"));
        this.parent = parent;
        this.targetStack = targetStack != null ? targetStack.copy() : ItemStack.EMPTY;

        // Auto-detect category from stack if passed
        if (!this.targetStack.isEmpty()) {
            if (this.targetStack.is(ModItems.DOOMFIST.get()) || this.targetStack.is(ModItems.DOOMFIST_V2.get())) {
                this.currentCategory = HUDCategory.DOOMFIST;
            } else if (this.targetStack.is(ModItems.OPTIC_BLAST.get())) {
                this.currentCategory = HUDCategory.OPTIC_BLAST;
            } else if (this.targetStack.getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem) {
                this.currentCategory = HUDCategory.MECHA;
            } else if (this.targetStack.getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem) {
                this.currentCategory = HUDCategory.HOLY;
            } else if (this.targetStack.is(ModItems.GOLDEN_FLOWER.get()) || this.targetStack.is(ModItems.OMEGA_FLOWERY.get())) {
                this.currentCategory = HUDCategory.GOLDEN_FLOWER;
            } else if (this.targetStack.is(ModItems.BROKEN_DIAMOND.get()) || this.targetStack.is(ModItems.CRAZY_DIAMOND_HEAD.get())) {
                this.currentCategory = HUDCategory.CRAZY_DIAMOND;
            } else if (this.targetStack.is(ModItems.THE_TEARS.get()) || this.targetStack.is(ModItems.DOGMA.get())) {
                this.currentCategory = HUDCategory.THE_TEARS;
            }
        }

        loadCategoryConfig();
    }

    public HUDPositionScreen(Screen parent) {
        this(parent, ItemStack.EMPTY);
    }

    private void loadCategoryConfig() {
        switch (this.currentCategory) {
            case DOOMFIST -> {
                this.hudX = Config.doomfistHudX;
                this.hudY = Config.doomfistHudY;
                this.hudScale = Config.doomfistHudScale;
            }
            case OPTIC_BLAST -> {
                this.hudX = Config.opticBlastHudX;
                this.hudY = Config.opticBlastHudY;
                this.hudScale = Config.opticBlastHudScale;
            }
            case MECHA -> {
                this.hudX = Config.mechaHudX;
                this.hudY = Config.mechaHudY;
                this.hudScale = Config.mechaHudScale;
            }
            case HOLY -> {
                this.hudX = Config.holyHudX;
                this.hudY = Config.holyHudY;
                this.hudScale = Config.holyHudScale;
            }
            case GOLDEN_FLOWER -> {
                this.hudX = Config.goldenFlowerHudX;
                this.hudY = Config.goldenFlowerHudY;
                this.hudScale = Config.goldenFlowerHudScale;
            }
            case CRAZY_DIAMOND -> {
                this.hudX = Config.crazyDiamondHudX;
                this.hudY = Config.crazyDiamondHudY;
                this.hudScale = Config.crazyDiamondHudScale;
            }
            case THE_TEARS -> {
                this.hudX = Config.theTearsHudX;
                this.hudY = Config.theTearsHudY;
                this.hudScale = Config.theTearsHudScale;
            }
        }
    }

    private void saveCategoryConfig() {
        switch (this.currentCategory) {
            case DOOMFIST -> {
                Config.doomfistHudX = this.hudX;
                Config.doomfistHudY = this.hudY;
                Config.doomfistHudScale = this.hudScale;
            }
            case OPTIC_BLAST -> {
                Config.opticBlastHudX = this.hudX;
                Config.opticBlastHudY = this.hudY;
                Config.opticBlastHudScale = this.hudScale;
            }
            case MECHA -> {
                Config.mechaHudX = this.hudX;
                Config.mechaHudY = this.hudY;
                Config.mechaHudScale = this.hudScale;
            }
            case HOLY -> {
                Config.holyHudX = this.hudX;
                Config.holyHudY = this.hudY;
                Config.holyHudScale = this.hudScale;
            }
            case GOLDEN_FLOWER -> {
                Config.goldenFlowerHudX = this.hudX;
                Config.goldenFlowerHudY = this.hudY;
                Config.goldenFlowerHudScale = this.hudScale;
            }
            case CRAZY_DIAMOND -> {
                Config.crazyDiamondHudX = this.hudX;
                Config.crazyDiamondHudY = this.hudY;
                Config.crazyDiamondHudScale = this.hudScale;
            }
            case THE_TEARS -> {
                Config.theTearsHudX = this.hudX;
                Config.theTearsHudY = this.hudY;
                Config.theTearsHudScale = this.hudScale;
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        // Category Switcher Button
        this.addRenderableWidget(Button.builder(
                Component.literal("HUD: " + this.currentCategory.displayName),
                button -> {
                    saveCategoryConfig();
                    int nextIdx = (this.currentCategory.ordinal() + 1) % HUDCategory.values().length;
                    this.currentCategory = HUDCategory.values()[nextIdx];
                    loadCategoryConfig();
                    button.setMessage(Component.literal("HUD: " + this.currentCategory.displayName));
                }
        ).bounds(this.width / 2 - 120, 10, 240, 20).build());

        // Scale Adjust Buttons
        this.addRenderableWidget(Button.builder(
                Component.literal("- Escala"),
                button -> {
                    this.hudScale = Math.max(0.5f, (float) (Math.round((this.hudScale - 0.1f) * 10.0) / 10.0));
                }
        ).bounds(this.width / 2 - 120, this.height - 65, 75, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Escala"),
                button -> {
                    this.hudScale = Math.min(2.0f, (float) (Math.round((this.hudScale + 0.1f) * 10.0) / 10.0));
                }
        ).bounds(this.width / 2 + 45, this.height - 65, 75, 20).build());

        // Save & Cancel Buttons
        this.addRenderableWidget(Button.builder(
                Component.literal("Guardar"),
                button -> {
                    saveCategoryConfig();
                    this.minecraft.setScreen(this.parent);
                }
        ).bounds(this.width / 2 - 100, this.height - 35, 90, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancelar"),
                button -> this.minecraft.setScreen(this.parent)
        ).bounds(this.width / 2 + 10, this.height - 35, 90, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        g.drawCenteredString(this.font, "Arrastra el HUD para posicionar | Usa +/- para re-escalar", this.width / 2, 35, 0xFFFFFFFF);
        g.drawCenteredString(this.font, String.format("Posición X: %d | Y (desde abajo): %d | Escala: %.1fx", this.hudX, this.hudY, this.hudScale), this.width / 2, 48, 0x88FFFFFF);

        // Center crosshair
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        g.fill(centerX - 4, centerY, centerX + 5, centerY + 1, 0x88FFFFFF);
        g.fill(centerX, centerY - 4, centerX + 1, centerY + 5, 0x88FFFFFF);

        int x = this.hudX;
        int y = this.height - this.hudY;

        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(this.hudScale, this.hudScale, 1.0f);

        // Render preview boxes
        switch (this.currentCategory) {
            case DOOMFIST -> {
                renderPreviewBox(g, 0, 0, "ACT1", "UPPER");
                renderPreviewBox(g, 30, 0, "ACT2", "SLAM");
            }
            case OPTIC_BLAST -> {
                renderPreviewBox(g, 0, 0, "BEAM", "CHARGE");
            }
            case MECHA -> {
                renderPreviewBox(g, 0, 0, "JET", "VULCAN");
                renderPreviewBox(g, 30, 0, "DASH", "OVER");
            }
            case HOLY -> {
                renderPreviewBox(g, 0, 0, "STANCE", "SHIELD");
                renderPreviewBox(g, 30, 0, "BLAST", "COMBO");
            }
            case GOLDEN_FLOWER -> {
                renderPreviewBox(g, 0, 0, "PETAL", "BURST");
            }
            case CRAZY_DIAMOND -> {
                renderPreviewBox(g, 0, 0, "FIST", "DORA");
            }
            case THE_TEARS -> {
                renderPreviewBox(g, 0, 0, "DOGMA", "RING");
            }
        }

        g.pose().popPose();

        // Highlight box
        int effectiveW = (int) (GROUP_WIDTH * this.hudScale);
        int effectiveH = (int) (GROUP_HEIGHT * this.hudScale);
        boolean hovered = mouseX >= x && mouseX <= x + effectiveW && mouseY >= y && mouseY <= y + effectiveH;
        if (hovered || this.dragging) {
            g.fill(x - 2, y - 2, x + effectiveW + 2, y - 1, 0xFFFFAA00);
            g.fill(x - 2, y + effectiveH + 1, x + effectiveW + 2, y + effectiveH + 2, 0xFFFFAA00);
            g.fill(x - 2, y - 2, x - 1, y + effectiveH + 2, 0xFFFFAA00);
            g.fill(x + effectiveW + 1, y - 2, x + effectiveW + 2, y + effectiveH + 2, 0xFFFFAA00);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderPreviewBox(GuiGraphics g, int bx, int by, String key, String label) {
        int boxW = 24;
        int boxH = 24;

        g.fill(bx - 1, by - 1, bx + boxW + 1, by + boxH + 1, 0xFF000000);
        g.fill(bx, by, bx + boxW, by + boxH, 0x6600FFCC);

        g.fill(bx, by, bx + boxW, by + 1, 0xBBFFFFFF);
        g.fill(bx, by + boxH - 1, bx + boxW, by + boxH, 0xBBFFFFFF);
        g.fill(bx, by, bx + 1, by + boxH, 0xBBFFFFFF);
        g.fill(bx + boxW - 1, by, bx + boxW, by + boxH, 0xBBFFFFFF);

        int textWidth = this.font.width(key);
        int keyX = bx + (boxW - textWidth) / 2;
        int keyY = by + (boxH - this.font.lineHeight) / 2;
        g.drawString(this.font, key, keyX, keyY, 0xFFFFFFFF, false);
        g.drawString(this.font, label, bx, by - 9, 0x88FFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.hudX;
        int y = this.height - this.hudY;
        int effectiveW = (int) (GROUP_WIDTH * this.hudScale);
        int effectiveH = (int) (GROUP_HEIGHT * this.hudScale);

        if (mouseX >= x && mouseX <= x + effectiveW && mouseY >= y && mouseY <= y + effectiveH) {
            this.dragging = true;
            this.dragOffsetX = (int) (mouseX - x);
            this.dragOffsetY = (int) (mouseY - y);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging) {
            int effectiveW = (int) (GROUP_WIDTH * this.hudScale);
            int effectiveH = (int) (GROUP_HEIGHT * this.hudScale);

            int newX = (int) (mouseX - this.dragOffsetX);
            newX = Math.max(0, Math.min(this.width - effectiveW, newX));

            int newScreenY = (int) (mouseY - this.dragOffsetY);
            newScreenY = Math.max(0, Math.min(this.height - effectiveH, newScreenY));
            int newY = this.height - newScreenY;

            this.hudX = newX;
            this.hudY = newY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            this.hudScale = Math.min(2.0f, (float) (Math.round((this.hudScale + 0.1f) * 10.0) / 10.0));
            return true;
        } else if (delta < 0) {
            this.hudScale = Math.max(0.5f, (float) (Math.round((this.hudScale - 0.1f) * 10.0) / 10.0));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            this.minecraft.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
