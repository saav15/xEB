package org.xeb.xeb.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.xeb.xeb.item.ModItems;

public class XebCompletionToast implements Toast {
    private static final String TOAST_TOKEN = "xeb";

    private final Component title;
    private final Component subtitle;
    private final ItemStack icon;
    private final int width;

    public XebCompletionToast(Component title, Component subtitle, ItemStack icon) {
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
        int titleWidth = Minecraft.getInstance().font.width(title);
        int subtitleWidth = Minecraft.getInstance().font.width(subtitle);
        this.width = Math.max(160, 30 + Math.max(titleWidth, subtitleWidth) + 8);
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public Toast.Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long startTime) {
        float f;
        boolean decoding = false;
        float decodeProgress = 0.0F;

        if (startTime < 1000L) {
            float t = (float) startTime / 1000.0F;
            f = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t) * (1.0F - t); // Smooth quartic ease-out slide-in
        } else if (startTime > 4000L) {
            float exitT = (float) (startTime - 4000L) / 1000.0F;
            exitT = net.minecraft.util.Mth.clamp(exitT, 0.0F, 1.0F);
            f = 1.0F - exitT * exitT * exitT; // Smooth cubic ease-in slide-out (slides left off-screen)
            decoding = true;
            decodeProgress = exitT;
        } else {
            f = 1.0F;
        }

        if (startTime >= 5000L) {
            f = 0.0F; // Keep fully off-screen left during Minecraft final cleanup frames
            decoding = true;
            decodeProgress = 1.0F;
        }

        float currentXTranslation = guiGraphics.pose().last().pose().m30();
        float targetX = -this.width + this.width * f;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(-currentXTranslation + targetX, 0.0F, 0.0F);

        // Futuristic neon cyan frame with translucent dark blue background
        int bgAlpha = decoding ? (int) (0xCE * (1.0F - decodeProgress)) : 0xCE;
        int borderAlpha = decoding ? (int) (0xFF * (1.0F - decodeProgress)) : 0xFF;

        int bgColor = (bgAlpha << 24) | 0x08111E;
        guiGraphics.fill(0, 0, this.width, 32, bgColor);

        // Clockwise lighting-up progress border
        double progress = (double) startTime / 5000.0;
        progress = Math.min(1.0, Math.max(0.0, progress));
        double perimeter = 2.0 * this.width + 64.0;
        double d = progress * perimeter;

        int borderLit = (borderAlpha << 24) | 0x00FFCC;
        int borderUnlit = (borderAlpha << 24) | 0x1A005544; // Darker unlit color

        // 1. Top border (y = 0 to 1)
        int topLit = (int) Math.min(this.width, d);
        guiGraphics.fill(0, 0, topLit, 1, borderLit);
        guiGraphics.fill(topLit, 0, this.width, 1, borderUnlit);

        // 2. Right border (x = width - 1 to width)
        int rightLit = (int) Math.min(32, Math.max(0, d - this.width));
        guiGraphics.fill(this.width - 1, 0, this.width, rightLit, borderLit);
        guiGraphics.fill(this.width - 1, rightLit, this.width, 32, borderUnlit);

        // 3. Bottom border (y = 31 to 32)
        int bottomLit = (int) Math.min(this.width, Math.max(0, d - (this.width + 32)));
        guiGraphics.fill(this.width - bottomLit, 31, this.width, 32, borderLit);
        guiGraphics.fill(0, 31, this.width - bottomLit, 32, borderUnlit);

        // 4. Left border (x = 0 to 1)
        int leftLit = (int) Math.min(32, Math.max(0, d - (2 * this.width + 32)));
        guiGraphics.fill(0, 32 - leftLit, 1, 32, borderLit);
        guiGraphics.fill(0, 0, 1, 32 - leftLit, borderUnlit);

        if (!this.icon.isEmpty()) {
            guiGraphics.renderFakeItem(this.icon, 8, 8);
        }

        // Draw scrambled/decoded texts during exit
        String renderTitle = this.title.getString();
        String renderSubtitle = this.subtitle.getString();

        if (decoding) {
            renderTitle = scrambleText(renderTitle, decodeProgress);
            renderSubtitle = scrambleText(renderSubtitle, decodeProgress);
        }

        int titleColor = decoding ? ((int) (0xFF * (1.0F - decodeProgress)) << 24) | 0xFFD700 : 0xFFFFD700;
        int subColor = decoding ? ((int) (0xFF * (1.0F - decodeProgress)) << 24) | 0xFFFFFF : 0xFFFFFFFF;

        guiGraphics.drawString(Minecraft.getInstance().font, renderTitle, 30, 7, titleColor, false);
        guiGraphics.drawString(Minecraft.getInstance().font, renderSubtitle, 30, 18, subColor, false);

        // Render binary bit rain overlay during decode/fadeout
        if (decoding) {
            java.util.Random r = new java.util.Random(123456789L);
            for (int i = 0; i < 24; i++) {
                int bx = r.nextInt(this.width - 10);
                int by = r.nextInt(24);
                if (r.nextFloat() < decodeProgress) {
                    String bit = r.nextBoolean() ? "0" : "1";
                    int bitAlpha = (int) ((1.0F - decodeProgress) * 200);
                    int bitColor = (bitAlpha << 24) | 0x00FFCC;
                    guiGraphics.drawString(Minecraft.getInstance().font, bit, bx, by, bitColor, false);
                }
            }
        }

        guiGraphics.pose().popPose();

        if (startTime >= 5000L) {
            return Toast.Visibility.HIDE;
        }

        return Toast.Visibility.SHOW;
    }

    private String scrambleText(String original, float progress) {
        StringBuilder sb = new StringBuilder();
        java.util.Random rand = new java.util.Random(original.hashCode() + (long) (progress * 15.0F));
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (c != ' ' && rand.nextFloat() < progress) {
                sb.append(rand.nextBoolean() ? '0' : '1');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void show(int logNumber) {
        Minecraft.getInstance().getToasts().addToast(new XebCompletionToast(
                Component.translatable("gui.xeb.enigma_bios.title"),
                Component.translatable("gui.xeb.enigma_bios.toast.subtitle", logNumber),
                new ItemStack(ModItems.ENIGMA_BIOS.get())
        ));
    }
}
