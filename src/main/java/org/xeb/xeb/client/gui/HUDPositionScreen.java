package org.xeb.xeb.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
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
        GENERAL_COOLDOWNS("General Cooldowns HUD (Activa 1, 2, 3)", "GENERAL_COOLDOWNS"),
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

    private HUDCategory currentCategory = HUDCategory.GENERAL_COOLDOWNS;

    private int hudX;
    private int hudY; // Offset or position
    private float hudScale = 1.0f;

    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HUDPositionScreen(Screen parent, ItemStack targetStack) {
        super(Component.translatable("gui.xeb.hud_customizer.title"));
        this.parent = parent;
        this.targetStack = targetStack != null ? targetStack.copy() : ItemStack.EMPTY;

        // Auto-detect category from stack if passed, otherwise default to GENERAL_COOLDOWNS
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
        } else {
            this.currentCategory = HUDCategory.GENERAL_COOLDOWNS;
        }

        loadCategoryConfig();
    }

    public HUDPositionScreen(Screen parent) {
        this(parent, ItemStack.EMPTY);
    }

    @Override
    public boolean isPauseScreen() {
        // Return false so world, mob ticks, and item hold animations don't freeze while customizing HUD!
        return false;
    }

    private void loadCategoryConfig() {
        switch (this.currentCategory) {
            case GENERAL_COOLDOWNS -> {
                this.hudX = Config.hudX;
                this.hudY = Config.hudY;
                this.hudScale = Config.hudScale;
            }
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
            case GENERAL_COOLDOWNS -> {
                Config.hudX = this.hudX;
                Config.hudY = this.hudY;
                Config.hudScale = this.hudScale;
            }
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

        // Scale Down Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.xeb.hud_customizer.scale_down"),
                button -> {
                    this.hudScale = Math.max(0.5f, (float) (Math.round((this.hudScale - 0.1f) * 10.0) / 10.0));
                }
        ).bounds(this.width / 2 - 130, this.height - 65, 80, 20).build());

        // RESET BUTTON (Restores selected HUD category to default optimal position)
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.xeb.hud_customizer.reset"),
                button -> {
                    switch (this.currentCategory) {
                        case GENERAL_COOLDOWNS -> {
                            this.hudX = 10;
                            this.hudY = 42;
                            this.hudScale = 1.0f;
                        }
                        case OPTIC_BLAST -> {
                            this.hudX = -18;
                            this.hudY = 0;
                            this.hudScale = 1.0f;
                        }
                        case THE_TEARS -> {
                            this.hudX = 14;
                            this.hudY = 12;
                            this.hudScale = 1.0f;
                        }
                        default -> {
                            this.hudX = 0;
                            this.hudY = 0;
                            this.hudScale = 1.0f;
                        }
                    }
                    saveCategoryConfig();
                }
        ).bounds(this.width / 2 - 40, this.height - 65, 80, 20).build());

        // Scale Up Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.xeb.hud_customizer.scale_up"),
                button -> {
                    this.hudScale = Math.min(2.0f, (float) (Math.round((this.hudScale + 0.1f) * 10.0) / 10.0));
                }
        ).bounds(this.width / 2 + 50, this.height - 65, 80, 20).build());

        // Save & Cancel Buttons
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.xeb.hud_customizer.save"),
                button -> {
                    saveCategoryConfig();
                    this.minecraft.setScreen(this.parent);
                }
        ).bounds(this.width / 2 - 100, this.height - 35, 90, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.xeb.hud_customizer.cancel"),
                button -> this.minecraft.setScreen(this.parent)
        ).bounds(this.width / 2 + 10, this.height - 35, 90, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        g.drawCenteredString(this.font, Component.translatable("gui.xeb.hud_customizer.instruction"), this.width / 2, 35, 0xFFFFFFFF);
        g.drawCenteredString(this.font, String.format("Pos X: %d | Y: %d | Scale: %.1fx", this.hudX, this.hudY, this.hudScale), this.width / 2, 48, 0x88FFFFFF);

        // Center crosshair
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        g.fill(centerX - 4, centerY, centerX + 5, centerY + 1, 0x88FFFFFF);
        g.fill(centerX, centerY - 4, centerX + 1, centerY + 5, 0x88FFFFFF);

        int renderX;
        int renderY;
        if (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) {
            renderX = this.hudX;
            renderY = this.height - this.hudY;
        } else {
            renderX = centerX + this.hudX;
            renderY = centerY + this.hudY;
        }

        g.pose().pushPose();
        g.pose().translate(renderX, renderY, 0);
        g.pose().scale(this.hudScale, this.hudScale, 1.0f);

        // Render authentic pixel-perfect in-game HUD preview
        renderCrosshairHUDPreview(g);

        g.pose().popPose();

        // Highlight box around customizable HUD
        int boxWidth = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? 84 : 60;
        int boxHeight = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? 24 : 40;
        int boundX = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? renderX : renderX - 10;
        int boundY = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? renderY : renderY - 10;
        int effectiveW = (int) (boxWidth * this.hudScale);
        int effectiveH = (int) (boxHeight * this.hudScale);

        boolean hovered = mouseX >= boundX && mouseX <= boundX + effectiveW && mouseY >= boundY && mouseY <= boundY + effectiveH;
        if (hovered || this.dragging) {
            g.fill(boundX - 2, boundY - 2, boundX + effectiveW + 2, boundY - 1, 0xFFFFAA00);
            g.fill(boundX - 2, boundY + effectiveH + 1, boundX + effectiveW + 2, boundY + effectiveH + 2, 0xFFFFAA00);
            g.fill(boundX - 2, boundY - 2, boundX - 1, boundY + effectiveH + 2, 0xFFFFAA00);
            g.fill(boundX + effectiveW + 1, boundY - 2, boundX + effectiveW + 2, boundY + effectiveH + 2, 0xFFFFAA00);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderCrosshairHUDPreview(GuiGraphics g) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        switch (this.currentCategory) {
            case GENERAL_COOLDOWNS -> {
                // Bottom-left 3 action boxes preview (ACTIVA 1, ACTIVA 2, ACTIVA 3)
                for (int b = 0; b < 3; b++) {
                    int bx = b * 30;
                    g.fill(bx - 1, -1, bx + 25, 25, 0xFF000000);
                    g.fill(bx, 0, bx + 24, 24, 0x44000000);
                    g.fill(bx, 0, bx + 24, 1, 0xFF00FFCC);
                    g.fill(bx, 23, bx + 24, 24, 0xFF00FFCC);
                    g.fill(bx, 0, bx + 1, 24, 0xFF00FFCC);
                    g.fill(bx + 23, 0, bx + 24, 24, 0xFF00FFCC);
                    String label = "ACT " + (b + 1);
                    g.drawString(this.font, label, bx + 2, 8, 0xFF00FFCC, false);
                }
            }
            case DOOMFIST -> {
                // Doomfist Power Block mitigation bar preview
                g.fill(0, 0, 54, 6, 0x66000000);
                g.fill(1, 1, 40, 5, 0xFFFFCC00);
                g.drawString(this.font, "MITIGATE 75%", 0, 10, 0xFFFFCC00, true);
            }
            case OPTIC_BLAST -> {
                // Vertical Curved Overheat Bar Preview
                float time = (float) System.currentTimeMillis() / 50.0F;
                org.xeb.xeb.render.OpticBlastHUDOverlay.renderCurvedBar(g, 0, 0, 1.0F, time, true, 0.85F);
                g.drawString(this.font, "OVERHEAT", -46, -10, 0xFFFF2222, true);
                g.drawString(this.font, "5.1s", -26, 16, 0xFFFF3333, true);
            }
            case MECHA -> {
                // Slanted 5-Segment O.CLOCK Gauge Preview
                for (int i = 0; i < 5; i++) {
                    int segX = i * 7;
                    int segY = -i * 8;
                    g.fill(segX - 1, segY - 1, segX + 7, segY + 6, 0x66000000);
                    int fillColor = switch (i) {
                        case 0 -> 0xFFFF3300;
                        case 1 -> 0xFFFF6600;
                        case 2 -> 0xFFFF9900;
                        case 3 -> 0xFFFFCC00;
                        default -> 0xFFFFFF00;
                    };
                    g.fill(segX, segY, segX + 6, segY + 5, fillColor);
                }
                g.drawString(this.font, "O.CLOCK", 0, -42, 0xFFFFD700, true);
            }
            case HOLY -> {
                // Holy Blast Charge Bar Preview
                g.fill(-20, 12, 20, 16, 0x66000000);
                g.fill(-19, 13, 12, 15, 0xFF00FFFF);
                g.drawString(this.font, "HOLY BLAST", -20, 20, 0xFF00FFFF, true);
            }
            case GOLDEN_FLOWER -> {
                // 6-Petal Resource Ring Orbit Preview
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60);
                    int px = (int) (20 * Math.cos(angle));
                    int py = (int) (20 * Math.sin(angle));
                    g.fill(px - 3, py - 3, px + 4, py + 4, 0xFFFFD700);
                }
                g.drawString(this.font, "FLOWERY", -16, 26, 0xFFFFD700, true);
            }
            case CRAZY_DIAMOND -> {
                // 3-Honeycomb Fist Charge Preview
                g.fill(0, 0, 12, 12, 0xFFFF55AA);
                g.fill(14, -6, 26, 6, 0xFFFF55AA);
                g.fill(14, 6, 26, 18, 0x44FFFFFF);
                g.drawString(this.font, "DORA (2/3)", 0, 22, 0xFFFF55AA, true);
            }
            case THE_TEARS -> {
                // Dark Donut Ring Brimstone Preview
                org.xeb.xeb.render.DoomfistHUDOverlay.drawDonutPublic(g, 0, 0, 8, 3, 0xFF00FF00);
                g.drawString(this.font, "BRIMSTONE", -20, 16, 0xFF00FF00, true);
            }
        }

        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int renderX;
            int renderY;
            if (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) {
                renderX = this.hudX;
                renderY = this.height - this.hudY;
            } else {
                renderX = centerX + this.hudX;
                renderY = centerY + this.hudY;
            }

            int boxWidth = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? 84 : 60;
            int boxHeight = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? 24 : 40;
            int boundX = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? renderX : renderX - 10;
            int boundY = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? renderY : renderY - 10;
            int effectiveW = (int) (boxWidth * this.hudScale);
            int effectiveH = (int) (boxHeight * this.hudScale);

            if (mouseX >= boundX && mouseX <= boundX + effectiveW && mouseY >= boundY && mouseY <= boundY + effectiveH) {
                this.dragging = true;
                this.dragOffsetX = (int) mouseX - renderX;
                this.dragOffsetY = (int) mouseY - renderY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.dragging) {
            this.dragging = false;
            saveCategoryConfig();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging && button == 0) {
            int newRenderX = (int) mouseX - this.dragOffsetX;
            int newRenderY = (int) mouseY - this.dragOffsetY;

            if (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) {
                this.hudX = newRenderX;
                this.hudY = this.height - newRenderY;
            } else {
                int centerX = this.width / 2;
                int centerY = this.height / 2;
                this.hudX = newRenderX - centerX;
                this.hudY = newRenderY - centerY;
            }
            saveCategoryConfig();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
