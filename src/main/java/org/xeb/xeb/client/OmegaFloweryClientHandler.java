package org.xeb.xeb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;

/**
 * Client-side rendering effects for the Omega Flowery instance.
 *
 * <p>Scales the player model slightly larger and applies a beautiful rainbow glow
 * while {@code xebOmegaFloweryActive} is set in the player's persistent data.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class OmegaFloweryClientHandler {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.getPersistentData().getBoolean("xebOmegaFloweryActive")) return;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // Scale up model slightly
        poseStack.scale(1.08F, 1.08F, 1.08F);
        player.getPersistentData().putBoolean("xebOmegaPosePushed", true);

        // Apply animated rainbow color overlay directly to player model
        long time = player.level().getGameTime();
        float hue = (float) ((time % 60) / 60.0);
        int rgb = hsbToRgb(hue, 1.0F, 1.0F);
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >>  8) & 0xFF) / 255.0F;
        float b = ( rgb        & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, 1.0F);
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (player.getPersistentData().getBoolean("xebOmegaPosePushed")) {
            player.getPersistentData().remove("xebOmegaPosePushed");
            event.getPoseStack().popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static int hsbToRgb(float hue, float saturation, float brightness) {
        if (saturation == 0) {
            int v = (int) (brightness * 255);
            return (v << 16) | (v << 8) | v;
        }
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1.0F - saturation);
        float q = brightness * (1.0F - saturation * f);
        float t = brightness * (1.0F - saturation * (1.0F - f));
        return switch ((int) h) {
            case 0  -> rgb(brightness, t,          p);
            case 1  -> rgb(q,          brightness, p);
            case 2  -> rgb(p,          brightness, t);
            case 3  -> rgb(p,          q,          brightness);
            case 4  -> rgb(t,          p,          brightness);
            default -> rgb(brightness, p,          q);
        };
    }

    private static int rgb(float r, float g, float b) {
        return (((int) (r * 255)) << 16) | (((int) (g * 255)) << 8) | ((int) (b * 255));
    }
}
