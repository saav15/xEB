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
    private static int ticksRemaining = 0;

    public static void setPermanightActive(boolean active) {
        setPermanightActive(active, active ? 24000 : 0);
    }

    public static void setPermanightActive(boolean active, int ticks) {
        isPermanightActive = active;
        ticksRemaining = ticks;
    }

    public static boolean isPermanightActive() {
        return isPermanightActive;
    }

    public static int getTicksRemaining() {
        return ticksRemaining;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && isPermanightActive && ticksRemaining > 0) {
            ticksRemaining--;
        }
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
        float pulse = (float) (Math.sin(time * 0.08D) * 0.06D + 0.28D); // Pulse 0.22 to 0.34 alpha

        // Dual-tone Vignette: Royal Purple + Dark Void Violet
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. Royal Purple Ambient Vignette
        RenderSystem.setShaderColor(0.55F, 0.05F, 0.85F, pulse * 0.75F);
        g.blit(VIGNETTE_TEXTURE, 0, 0, 0, 0.0F, 0.0F, width, height, width, height);

        // 2. Void Purple Deep Overlay
        float p2 = (float) (Math.cos(time * 0.06D) * 0.05D + 0.25D);
        RenderSystem.setShaderColor(0.38F, 0.02F, 0.70F, p2 * 0.85F);
        g.blit(VIGNETTE_TEXTURE, 0, 0, 0, 0.0F, 0.0F, width, height, width, height);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isPermanightActive) return;

        // Dark obsidian void fog color (Deep Violet/Black)
        event.setRed(0.08F);
        event.setGreen(0.02F);
        event.setBlue(0.18F);
    }

    private static double findGroundY(net.minecraft.world.level.Level level, double x, double startY, double z) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos(x, startY, z);
        for (int i = 0; i < 30; i++) {
            if (pos.getY() <= level.getMinBuildHeight()) break;
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos).blocksMotion()) {
                return pos.getY() + 1.05D;
            }
            pos.move(net.minecraft.core.Direction.DOWN);
        }
        return level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, net.minecraft.core.BlockPos.containing(x, startY, z)).getY() + 0.05D;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isPermanightActive) return;
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player == null || !player.level().isClientSide()) return;

        // Ground Floor Aura (Rojo, Morado, Negro)
        if (RANDOM.nextFloat() < 0.65F) {
            double px = player.getX() + (RANDOM.nextDouble() - 0.5D) * 14.0D;
            double pz = player.getZ() + (RANDOM.nextDouble() - 0.5D) * 14.0D;
            double py = findGroundY(player.level(), px, player.getY(), pz);

            float roll = RANDOM.nextFloat();
            if (roll < 0.40F) {
                // Purple Void Portal Ground Flame
                player.level().addParticle(ParticleTypes.PORTAL, px, py, pz, (RANDOM.nextDouble() - 0.5D) * 0.2D, 0.05D, (RANDOM.nextDouble() - 0.5D) * 0.2D);
            } else if (roll < 0.70F) {
                // Purple Dragon Breath Floor Aura
                player.level().addParticle(ParticleTypes.DRAGON_BREATH, px, py, pz, (RANDOM.nextDouble() - 0.5D) * 0.05D, 0.02D, (RANDOM.nextDouble() - 0.5D) * 0.05D);
            } else if (roll < 0.90F) {
                // Crimson Red Soul Fire Flame
                player.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0.0D, 0.03D, 0.0D);
            } else {
                // Obsidian Black Smoke Accent
                player.level().addParticle(ParticleTypes.SMOKE, px, py, pz, 0.0D, 0.01D, 0.0D);
            }
        }
    }
}
