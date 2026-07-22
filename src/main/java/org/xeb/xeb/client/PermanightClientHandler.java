package org.xeb.xeb.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation VIGNETTE_TEXTURE = new ResourceLocation("textures/misc/vignette.png");
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
        float pulse = (float) (Math.sin(time * 0.08D) * 0.05D + 0.25D); // Smooth subtle pulse 0.20 to 0.30 alpha

        // Render soft, smooth, non-intrusive vignette texture overlay instead of hard solid black boxes
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.08F, 0.04F, 0.12F, pulse);
        g.blit(VIGNETTE_TEXTURE, 0, 0, 0, 0.0F, 0.0F, width, height, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isPermanightActive) return;

        // Tint fog to subtle apocalyptic slate dark gray
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
