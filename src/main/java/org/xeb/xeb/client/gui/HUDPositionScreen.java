package org.xeb.xeb.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.xeb.xeb.Config;
import org.xeb.xeb.client.renderer.GoldenFlowerHUDOverlay;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.render.DoomfistHUDOverlay;

/**
 * Pantalla de personalización del HUD para armas míticas e interfaz de cooldowns general.
 * Optimizado para movimiento 100% fluido (sin tirones/lag al arrastrar) y con vistas previas 1:1 exactas.
 */
public class HUDPositionScreen extends Screen {
    private final Screen parent;
    private final ItemStack targetStack;

    public enum HUDCategory {
        GENERAL_COOLDOWNS("General Cooldowns HUD (Activa 1, 2, 3)", "GENERAL_COOLDOWNS"),
        DOOMFIST_V1("Doomfist Gauntlet v1", "DOOMFIST_V1"),
        DOOMFIST_V2("Doomfist Gauntlet v2", "DOOMFIST_V2"),
        OPTIC_BLAST("Optic Blast", "OPTIC_BLAST"),
        MECHA("Mecha II Overdrive Core", "MECHA"),
        HOLY("Holy Crown / Holy Duality Blade", "HOLY"),
        GOLDEN_FLOWER("The Golden Flower", "GOLDEN_FLOWER"),
        CRAZY_DIAMOND("The Crazy Diamond", "CRAZY_DIAMOND"),
        THE_TEARS("The Tears / Dogma", "THE_TEARS");

        public final String displayName;
        public final String id;

        HUDCategory(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }
    }

    private HUDCategory currentCategory = HUDCategory.GENERAL_COOLDOWNS;
    private int selectedActiva = 1; // 1, 2, or 3

    private int hudX;
    private int hudY;
    private float hudScale = 1.0f;

    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HUDPositionScreen(Screen parent, ItemStack targetStack) {
        super(Component.translatable("gui.xeb.hud_customizer.title"));
        this.parent = parent;
        this.targetStack = targetStack != null ? targetStack.copy() : ItemStack.EMPTY;

        // Autodetectar la categoría de arma según el ítem inspeccionado en la Bitácora Enigma
        if (!this.targetStack.isEmpty()) {
            if (this.targetStack.is(ModItems.DOOMFIST.get())) {
                this.currentCategory = HUDCategory.DOOMFIST_V1;
            } else if (this.targetStack.is(ModItems.DOOMFIST_V2.get())) {
                this.currentCategory = HUDCategory.DOOMFIST_V2;
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
        return false;
    }

    private void loadCategoryConfig() {
        switch (this.currentCategory) {
            case GENERAL_COOLDOWNS -> {
                if (this.selectedActiva == 1) {
                    this.hudX = Config.activa1HudX;
                    this.hudY = Config.activa1HudY;
                    this.hudScale = Config.activa1HudScale;
                } else if (this.selectedActiva == 2) {
                    this.hudX = Config.activa2HudX;
                    this.hudY = Config.activa2HudY;
                    this.hudScale = Config.activa2HudScale;
                } else if (this.selectedActiva == 3) {
                    this.hudX = Config.activa3HudX;
                    this.hudY = Config.activa3HudY;
                    this.hudScale = Config.activa3HudScale;
                }
            }
            case DOOMFIST_V1, DOOMFIST_V2 -> {
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

    /**
     * Actualiza únicamente las variables de memoria RAM estáticas.
     * Cero eventos de Forge y cero escrituras en disco durante el movimiento (0% Lag Spike).
     */
    private void saveCategoryConfigInMemory() {
        switch (this.currentCategory) {
            case GENERAL_COOLDOWNS -> {
                if (this.selectedActiva == 1) {
                    Config.activa1HudX = this.hudX;
                    Config.activa1HudY = this.hudY;
                    Config.activa1HudScale = this.hudScale;
                } else if (this.selectedActiva == 2) {
                    Config.activa2HudX = this.hudX;
                    Config.activa2HudY = this.hudY;
                    Config.activa2HudScale = this.hudScale;
                } else if (this.selectedActiva == 3) {
                    Config.activa3HudX = this.hudX;
                    Config.activa3HudY = this.hudY;
                    Config.activa3HudScale = this.hudScale;
                }
            }
            case DOOMFIST_V1, DOOMFIST_V2 -> {
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

    /**
     * Guarda permanentemente los cambios en el archivo .toml en disco.
     * Se invoca únicamente al soltar el ratón o hacer clic en botones.
     */
    private void saveCategoryConfigToDisk() {
        saveCategoryConfigInMemory();
        switch (this.currentCategory) {
            case GENERAL_COOLDOWNS -> {
                if (this.selectedActiva == 1) {
                    Config.ACTIVA1_HUD_X.set(this.hudX);
                    Config.ACTIVA1_HUD_Y.set(this.hudY);
                    Config.ACTIVA1_HUD_SCALE.set((double) this.hudScale);
                } else if (this.selectedActiva == 2) {
                    Config.ACTIVA2_HUD_X.set(this.hudX);
                    Config.ACTIVA2_HUD_Y.set(this.hudY);
                    Config.ACTIVA2_HUD_SCALE.set((double) this.hudScale);
                } else if (this.selectedActiva == 3) {
                    Config.ACTIVA3_HUD_X.set(this.hudX);
                    Config.ACTIVA3_HUD_Y.set(this.hudY);
                    Config.ACTIVA3_HUD_SCALE.set((double) this.hudScale);
                }
            }
            case DOOMFIST_V1, DOOMFIST_V2 -> {
                Config.DOOMFIST_HUD_X.set(this.hudX);
                Config.DOOMFIST_HUD_Y.set(this.hudY);
                Config.DOOMFIST_HUD_SCALE.set((double) this.hudScale);
            }
            case OPTIC_BLAST -> {
                Config.OPTIC_BLAST_HUD_X.set(this.hudX);
                Config.OPTIC_BLAST_HUD_Y.set(this.hudY);
                Config.OPTIC_BLAST_HUD_SCALE.set((double) this.hudScale);
            }
            case MECHA -> {
                Config.MECHA_HUD_X.set(this.hudX);
                Config.MECHA_HUD_Y.set(this.hudY);
                Config.MECHA_HUD_SCALE.set((double) this.hudScale);
            }
            case HOLY -> {
                Config.HOLY_HUD_X.set(this.hudX);
                Config.HOLY_HUD_Y.set(this.hudY);
                Config.HOLY_HUD_SCALE.set((double) this.hudScale);
            }
            case GOLDEN_FLOWER -> {
                Config.GOLDEN_FLOWER_HUD_X.set(this.hudX);
                Config.GOLDEN_FLOWER_HUD_Y.set(this.hudY);
                Config.GOLDEN_FLOWER_HUD_SCALE.set((double) this.hudScale);
            }
            case CRAZY_DIAMOND -> {
                Config.CRAZY_DIAMOND_HUD_X.set(this.hudX);
                Config.CRAZY_DIAMOND_HUD_Y.set(this.hudY);
                Config.CRAZY_DIAMOND_HUD_SCALE.set((double) this.hudScale);
            }
            case THE_TEARS -> {
                Config.THE_TEARS_HUD_X.set(this.hudX);
                Config.THE_TEARS_HUD_Y.set(this.hudY);
                Config.THE_TEARS_HUD_SCALE.set((double) this.hudScale);
            }
        }
        try {
            Config.SPEC.save();
        } catch (Exception ignored) {}
    }

    @Override
    protected void init() {
        super.init();

        // Botón de título superior (DESACTIVADO por solicitud del usuario para evitar navegación innecesaria)
        Button categoryBtn = Button.builder(
                Component.literal("HUD: " + this.currentCategory.displayName),
                button -> {}
        ).bounds(this.width / 2 - 120, 10, 240, 20).build();

        categoryBtn.active = false;
        this.addRenderableWidget(categoryBtn);

        // Botón Reducir Escala
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.xeb.hud_customizer.scale_down"),
                button -> {
                    this.hudScale = Math.max(0.5f, (float) (Math.round((this.hudScale - 0.1f) * 10.0) / 10.0));
                    saveCategoryConfigToDisk();
                }
        ).bounds(this.width / 2 - 130, this.height - 65, 80, 20).build());

        // BOTÓN REINICIAR
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.xeb.hud_customizer.reset"),
                button -> {
                    switch (this.currentCategory) {
                        case GENERAL_COOLDOWNS -> {
                            if (this.selectedActiva == 1) {
                                this.hudX = 10;
                                this.hudY = 42;
                                this.hudScale = 1.0f;
                            } else if (this.selectedActiva == 2) {
                                this.hudX = 40;
                                this.hudY = 42;
                                this.hudScale = 1.0f;
                            } else if (this.selectedActiva == 3) {
                                this.hudX = 70;
                                this.hudY = 42;
                                this.hudScale = 1.0f;
                            }
                        }
                        case OPTIC_BLAST -> {
                            this.hudX = -29;
                            this.hudY = 0;
                            this.hudScale = 1.0f;
                        }
                        case THE_TEARS -> {
                            this.hudX = 11;
                            this.hudY = -12;
                            this.hudScale = 1.0f;
                        }
                        default -> {
                            this.hudX = 0;
                            this.hudY = 0;
                            this.hudScale = 1.0f;
                        }
                    }
                    saveCategoryConfigToDisk();
                }
        ).bounds(this.width / 2 - 40, this.height - 65, 80, 20).build());

        // Botón Aumentar Escala
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.xeb.hud_customizer.scale_up"),
                button -> {
                    this.hudScale = Math.min(2.0f, (float) (Math.round((this.hudScale + 0.1f) * 10.0) / 10.0));
                    saveCategoryConfigToDisk();
                }
        ).bounds(this.width / 2 + 50, this.height - 65, 80, 20).build());

        // Botones Guardar y Cancelar
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.xeb.hud_customizer.save"),
                button -> {
                    saveCategoryConfigToDisk();
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
        if (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) {
            g.drawCenteredString(this.font, String.format("Pos X: %d | Y: %d | Scale: %.1fx  [Modificando Activa %d]", this.hudX, this.hudY, this.hudScale, this.selectedActiva), this.width / 2, 48, 0xFF00FFCC);
        } else {
            g.drawCenteredString(this.font, String.format("Pos X: %d | Y: %d | Scale: %.1fx", this.hudX, this.hudY, this.hudScale), this.width / 2, 48, 0x88FFFFFF);
        }

        // Crosshair de guía central
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

        if (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) {
            renderAllActivasPreview(g);
        } else {
            g.pose().pushPose();
            g.pose().translate(renderX, renderY, 0);
            g.pose().scale(this.hudScale, this.hudScale, 1.0f);

            // Renderizado de vista previa 1:1 exacta de la interfaz del juego
            renderCrosshairHUDPreview(g);

            g.pose().popPose();
        }

        // Cuadro de selección para arrastrar
        int boxWidth = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? 24 : 54;
        int boxHeight = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? 24 : 36;
        int boundX = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? renderX : renderX - 27;
        int boundY = (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) ? renderY : renderY - 18;
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

    private void renderAllActivasPreview(GuiGraphics g) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        renderActivaPreviewBox(g, 1, Config.activa1HudX, this.height - Config.activa1HudY, Config.activa1HudScale, this.selectedActiva == 1);
        renderActivaPreviewBox(g, 2, Config.activa2HudX, this.height - Config.activa2HudY, Config.activa2HudScale, this.selectedActiva == 2);
        renderActivaPreviewBox(g, 3, Config.activa3HudX, this.height - Config.activa3HudY, Config.activa3HudScale, this.selectedActiva == 3);

        RenderSystem.disableBlend();
    }

    private void renderActivaPreviewBox(GuiGraphics g, int num, int x, int y, float scale, boolean isSelected) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);

        int border = isSelected ? 0xFFFFAA00 : 0x88AAAAAA;
        int bg = isSelected ? 0x66000000 : 0x22000000;
        int txtColor = isSelected ? 0xFFFFAA00 : 0x88AAAAAA;

        g.fill(-1, -1, 25, 25, 0xFF000000);
        g.fill(0, 0, 24, 24, bg);
        g.fill(0, 0, 24, 1, border);
        g.fill(0, 23, 24, 24, border);
        g.fill(0, 0, 1, 24, border);
        g.fill(23, 0, 24, 24, border);

        String label = "ACT " + num;
        g.drawString(this.font, label, 2, 8, txtColor, false);

        g.pose().popPose();
    }

    private void renderCrosshairHUDPreview(GuiGraphics g) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        long gameTime = System.currentTimeMillis() / 50L;
        float time = (float) System.currentTimeMillis() / 50.0F;

        switch (this.currentCategory) {
            case DOOMFIST_V2 -> {
                int segmentCount = 4;
                int segmentW = 10;
                int segmentSpacing = 3;
                int barH = 4;
                int barW = 49;
                int x = -barW / 2;
                int y = 15;

                for (int i = 0; i < segmentCount; i++) {
                    int segX = x + i * (segmentW + segmentSpacing);
                    DoomfistHUDOverlay.drawSlantedBar(g, segX - 1, y - 1, segmentW + 2, barH + 2, 0xFF000000);
                    DoomfistHUDOverlay.drawSlantedBar(g, segX, y, segmentW, barH, 0x88222222);

                    if (i < 3) {
                        DoomfistHUDOverlay.drawSlantedBar(g, segX, y, segmentW, barH, 0xFFFF3333);
                    } else {
                        DoomfistHUDOverlay.drawSlantedBar(g, segX, y, 4, barH, 0xFFFF3333);
                    }
                }
            }
            case DOOMFIST_V1 -> {
                int segmentCount = 4;
                int segmentW = 10;
                int segmentSpacing = 3;
                int barH = 4;
                int barW = 49;
                int x = -barW / 2;
                int y = 15;

                for (int i = 0; i < segmentCount; i++) {
                    int segX = x + i * (segmentW + segmentSpacing);
                    DoomfistHUDOverlay.drawSlantedBar(g, segX - 1, y - 1, segmentW + 2, barH + 2, 0xFF000000);
                    DoomfistHUDOverlay.drawSlantedBar(g, segX, y, segmentW, barH, 0x88222222);
                    DoomfistHUDOverlay.drawSlantedBar(g, segX, y, segmentW, barH, 0xFF00DDFF);
                }
            }
            case OPTIC_BLAST -> {
                org.xeb.xeb.render.OpticBlastHUDOverlay.renderCurvedBar(g, 0, -18, 1.0F, time, true, 0.85F);
                g.drawString(this.font, "OVERHEAT", -46, -10, 0xFFFF2222, true);
                g.drawString(this.font, "5.1s", -26, 16, 0xFFFF3333, true);
            }
            case MECHA -> {
                for (int i = 0; i < 5; i++) {
                    int segX = 18 + i;
                    int segY = 14 - i * 8;
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
                g.drawString(this.font, "O.CLOCK", 18, -28, 0xFFFFD700, true);
            }
            case HOLY -> {
                int barX = -20;
                int barY = 12;
                g.fill(barX - 1, barY - 1, barX + 41, barY + 5, 0x66000000);
                g.fill(barX, barY, barX + 30, barY + 4, 0xFF00FFFF);
                g.drawCenteredString(this.font, "HOLY BLAST 75%", 0, barY + 6, 0xFF00FFFF);
            }
            case GOLDEN_FLOWER -> {
                int[] flowerColors = {0x00FFFF, 0xC080FF, 0xFF9F20, 0x40FF40, 0x4060FF, 0xFFFF40};
                double radius = 18.0D;
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(-145.0D + i * 22.0D);
                    int fx = (int) (Math.cos(angle) * radius);
                    int fy = -18 + (int) (Math.sin(angle) * radius);
                    GoldenFlowerHUDOverlay.drawLoadedFlowerHUD(g, fx, fy, flowerColors[i], gameTime);
                }
            }
            case CRAZY_DIAMOND -> {
                int[][] cellPositions = {
                        {15, -4},
                        {23, -9},
                        {23, 1}
                };
                for (int i = 0; i < 3; i++) {
                    int cx = cellPositions[i][0];
                    int cy = cellPositions[i][1];
                    DoomfistHUDOverlay.drawHoneycombCell(g, cx, cy, 3, 0, i, 0);
                }
            }
            case THE_TEARS -> {
                DoomfistHUDOverlay.drawDonutPublic(g, 0, 0, 6, 2, 0xFF00FF00);
                DoomfistHUDOverlay.drawDonutPublic(g, 0, 0, 4, 2, 0xFFB000FF);
                g.drawCenteredString(this.font, "BRIMSTONE", 0, 14, 0xFF00FF00);
            }
            default -> {}
        }

        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.currentCategory == HUDCategory.GENERAL_COOLDOWNS) {
                for (int act = 1; act <= 3; act++) {
                    int bx = (act == 1) ? Config.activa1HudX : (act == 2 ? Config.activa2HudX : Config.activa3HudX);
                    int by = this.height - ((act == 1) ? Config.activa1HudY : (act == 2 ? Config.activa2HudY : Config.activa3HudY));
                    float bscale = (act == 1) ? Config.activa1HudScale : (act == 2 ? Config.activa2HudScale : Config.activa3HudScale);
                    int ew = (int) (24 * bscale);
                    int eh = (int) (24 * bscale);

                    if (mouseX >= bx && mouseX <= bx + ew && mouseY >= by && mouseY <= by + eh) {
                        this.selectedActiva = act;
                        loadCategoryConfig();
                        this.dragging = true;
                        this.dragOffsetX = (int) mouseX - bx;
                        this.dragOffsetY = (int) mouseY - by;
                        return true;
                    }
                }
            } else {
                int centerX = this.width / 2;
                int centerY = this.height / 2;
                int renderX = centerX + this.hudX;
                int renderY = centerY + this.hudY;

                int boxWidth = 54;
                int boxHeight = 36;
                int boundX = renderX - 27;
                int boundY = renderY - 18;
                int effectiveW = (int) (boxWidth * this.hudScale);
                int effectiveH = (int) (boxHeight * this.hudScale);

                if (mouseX >= boundX && mouseX <= boundX + effectiveW && mouseY >= boundY && mouseY <= boundY + effectiveH) {
                    this.dragging = true;
                    this.dragOffsetX = (int) mouseX - renderX;
                    this.dragOffsetY = (int) mouseY - renderY;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.dragging) {
            this.dragging = false;
            saveCategoryConfigToDisk(); // Guardar en disco ONCE al finalizar el arrastre
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
            saveCategoryConfigInMemory(); // Actualiza solo las variables Java en RAM en tiempo real durante el arrastre (0% Lag Spike)
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
