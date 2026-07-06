package org.xeb.xeb.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.xeb.xeb.Config;

public class HUDPositionScreen extends Screen {
    private final Screen parent;
    
    // Position of the HUD boxes
    private int hudX;
    private int hudY; // stored as distance from bottom
    
    // Dragging state
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    
    // Bounding box of the ability HUD group (contains three 24x24 boxes side-by-side with 6px spacing)
    // Total width = 24 + 6 + 24 + 6 + 24 = 84. Total height = 24.
    private static final int GROUP_WIDTH = 84;
    private static final int GROUP_HEIGHT = 24;

    public HUDPositionScreen(Screen parent) {
        super(Component.literal("xEB HUD Position Config"));
        this.parent = parent;
        this.hudX = Config.opticBlastHudX;
        this.hudY = Config.opticBlastHudY;
    }

    @Override
    protected void init() {
        super.init();
        // Add a Save button at the bottom
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Guardar"),
                button -> {
                    // Update Forge Config
                    Config.OPTIC_BLAST_HUD_X.set(this.hudX);
                    Config.OPTIC_BLAST_HUD_Y.set(this.hudY);
                    
                    // Also update the static variables immediately for runtime reflection
                    Config.opticBlastHudX = this.hudX;
                    Config.opticBlastHudY = this.hudY;
                    
                    this.minecraft.setScreen(this.parent);
                }
        ).bounds(this.width / 2 - 100, this.height - 40, 90, 20).build());

        // Add a Cancel button
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Cancelar"),
                button -> this.minecraft.setScreen(this.parent)
        ).bounds(this.width / 2 + 10, this.height - 40, 90, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Draw blurred/darkened background
        this.renderBackground(g);
        
        // Draw instructions
        g.drawCenteredString(this.font, "Arrastra los recuadros de cooldown a la posición deseada", this.width / 2, 20, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "Coordenadas actuales: X: " + this.hudX + ", Y (desde abajo): " + this.hudY, this.width / 2, 35, 0x88FFFFFF);

        // Render crosshair for preview reference
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        g.fill(centerX - 4, centerY, centerX + 5, centerY + 1, 0x88FFFFFF);
        g.fill(centerX, centerY - 4, centerX + 1, centerY + 5, 0x88FFFFFF);

        // Render HUD boxes preview (using local coordinates)
        int x = this.hudX;
        int y = this.height - this.hudY; // translate Y from bottom-relative to screen coordinates
        
        // Draw the preview ability boxes
        renderPreviewBox(g, x, y, "ACT1", "UPPER");
        renderPreviewBox(g, x + 30, y, "ACT2", "SLAM");
        renderPreviewBox(g, x + 60, y, "ACT3", "ULTI");
        
        // Highlight bounding box if dragging or hovering
        boolean hovered = mouseX >= x && mouseX <= x + GROUP_WIDTH && mouseY >= y && mouseY <= y + GROUP_HEIGHT;
        if (hovered || this.dragging) {
            g.fill(x - 2, y - 2, x + GROUP_WIDTH + 2, y - 1, 0xFFFFAA00);
            g.fill(x - 2, y + GROUP_HEIGHT + 1, x + GROUP_WIDTH + 2, y + GROUP_HEIGHT + 2, 0xFFFFAA00);
            g.fill(x - 2, y - 2, x - 1, y + GROUP_HEIGHT + 2, 0xFFFFAA00);
            g.fill(x + GROUP_WIDTH + 1, y - 2, x + GROUP_WIDTH + 2, y + GROUP_HEIGHT + 2, 0xFFFFAA00);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderPreviewBox(GuiGraphics g, int x, int y, String key, String label) {
        int boxW = 24;
        int boxH = 24;
        
        g.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF000000);
        g.fill(x, y, x + boxW, y + boxH, 0x6600FFCC); // Cool cyan tint for preview
        
        // Border
        g.fill(x, y, x + boxW, y + 1, 0xBBFFFFFF);
        g.fill(x, y + boxH - 1, x + boxW, y + boxH, 0xBBFFFFFF);
        g.fill(x, y, x + 1, y + boxH, 0xBBFFFFFF);
        g.fill(x + boxW - 1, y, x + boxW, y + boxH, 0xBBFFFFFF);
        
        // Center label
        int textWidth = this.font.width(key);
        int keyX = x + (boxW - textWidth) / 2;
        int keyY = y + (boxH - this.font.lineHeight) / 2;
        g.drawString(this.font, key, keyX, keyY, 0xFFFFFFFF, false);
        
        // Upper label
        g.drawString(this.font, label, x, y - 9, 0x88FFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.hudX;
        int y = this.height - this.hudY;
        
        // Check if click is inside the HUD group bounding box
        if (mouseX >= x && mouseX <= x + GROUP_WIDTH && mouseY >= y && mouseY <= y + GROUP_HEIGHT) {
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
            // Update HUD X position (constrain inside screen boundaries)
            int newX = (int) (mouseX - this.dragOffsetX);
            newX = Math.max(0, Math.min(this.width - GROUP_WIDTH, newX));
            
            // Update HUD Y position (bottom-relative, constrain inside screen boundaries)
            int newScreenY = (int) (mouseY - this.dragOffsetY);
            newScreenY = Math.max(0, Math.min(this.height - GROUP_HEIGHT, newScreenY));
            int newY = this.height - newScreenY;
            
            this.hudX = newX;
            this.hudY = newY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
