package org.xeb.xeb.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Xeb.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PermanightClientHandler {
    private static final Random RANDOM = new Random();
    private static boolean isPermanightActive = false;

    public static void setPermanightActive(boolean active) {
        isPermanightActive = active;
    }

    public static boolean isPermanightActive() {
        return isPermanightActive;
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!isPermanightActive) return;
        if (event.getOverlay() != VanillaGuiOverlay.VIGNETTE.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        GuiGraphics g = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        long time = mc.level.getGameTime();
        float pulse = (float) (Math.sin(time * 0.08D) * 0.15D + 0.35D); // Smooth pulsing pulse 0.20 to 0.50

        int alpha = (int) (pulse * 255);
        int borderSize = Math.max(12, height / 10);

        // Render apocalyptic dark vignette border
        int darkColor = (alpha << 24) | 0x05000A;

        g.fill(0, 0, width, borderSize, darkColor);
        g.fill(0, height - borderSize, width, height, darkColor);
        g.fill(0, 0, borderSize, height, darkColor);
        g.fill(width - borderSize, 0, width, height, darkColor);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isPermanightActive) return;

        // Tint fog to apocalyptic slate dark gray
        event.setRed(0.06F);
        event.setGreen(0.06F);
        event.setBlue(0.10F);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isPermanightActive) return;
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player == null || !player.level().isClientSide()) return;

        // Spawn ambient ash & echo particles around client player
        if (RANDOM.nextFloat() < 0.35F) {
            double rx = player.getX() + (RANDOM.nextDouble() - 0.5D) * 20.0D;
            double ry = player.getY() + RANDOM.nextDouble() * 8.0D;
            double rz = player.getZ() + (RANDOM.nextDouble() - 0.5D) * 20.0D;

            player.level().addParticle(ParticleTypes.ASH, rx, ry, rz, 0.0D, -0.02D, 0.0D);
            if (RANDOM.nextFloat() < 0.15F) {
                player.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, rx, ry, rz, 0.0D, 0.01D, 0.0D);
            }
        }
    }
}
