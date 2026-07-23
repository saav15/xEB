package org.xeb.xeb.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.xeb.xeb.item.ModItems;

public class FlourishAnimationManager {
    private static Item activeItem = null;
    private static long startTime = 0L;
    private static int totalDurationTicks = 25; // ~1.25 seconds for The Tears

    // Smart Halberd Stacking Neon Yellow Glow Pulse
    private static int halberdGlowLevel = 0;
    private static long lastHalberdGlowTime = 0L;
    private static final long GLOW_DURATION_MS = 1000L; // 1 second pulse duration

    public static void triggerFlourish(Item item) {
        if (item == null) return;
        activeItem = item;
        startTime = System.currentTimeMillis();

        if (item == ModItems.THE_TEARS.get()) {
            totalDurationTicks = 25;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                org.xeb.xeb.item.TheTearsItem.triggerAbilityAnim(mc.player);
            }
        } else if (item == ModItems.SMART_HALBERD.get()) {
            triggerSmartHalberdGlow();
        }
    }

    public static void triggerSmartHalberdGlow() {
        long now = System.currentTimeMillis();
        if (now - lastHalberdGlowTime > GLOW_DURATION_MS) {
            halberdGlowLevel = 1;
        } else {
            halberdGlowLevel = Math.min(4, halberdGlowLevel + 1);
        }
        lastHalberdGlowTime = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getPersistentData().putBoolean("xebHalberdEyeForced", true);
        }
    }

    public static int getSmartHalberdGlowLevel() {
        long elapsed = System.currentTimeMillis() - lastHalberdGlowTime;
        if (elapsed > GLOW_DURATION_MS) {
            halberdGlowLevel = 0;
            return 0;
        }
        return halberdGlowLevel;
    }

    public static float getSmartHalberdGlowIntensity() {
        int level = getSmartHalberdGlowLevel();
        if (level <= 0) return 0.0F;
        long elapsed = System.currentTimeMillis() - lastHalberdGlowTime;
        float progress = 1.0F - (elapsed / (float) GLOW_DURATION_MS); // 1.0 -> 0.0 fade out
        float pulse = (float) Math.sin((1.0F - progress) * Math.PI * 6.0D) * 0.18F + 0.82F;
        return (level / 4.0F) * pulse * progress;
    }

    public static boolean isFlourishActive() {
        if (activeItem == null) return false;
        long elapsed = System.currentTimeMillis() - startTime;
        long totalMs = totalDurationTicks * 50L;
        if (elapsed >= totalMs) {
            activeItem = null;
            return false;
        }
        return true;
    }

    public static Item getActiveItem() {
        if (!isFlourishActive()) return null;
        return activeItem;
    }

    public static float getProgress(float partialTick) {
        if (activeItem == null) return 0.0F;
        long elapsed = System.currentTimeMillis() - startTime;
        float totalMs = totalDurationTicks * 50.0F;
        float progress = (elapsed + partialTick * 50.0F) / totalMs;
        return Math.min(1.0F, Math.max(0.0F, progress));
    }
}
