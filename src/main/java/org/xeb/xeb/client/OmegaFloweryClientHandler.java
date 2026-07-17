package org.xeb.xeb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;

/**
 * Client-side rendering effects for the Omega Flowery instance.
 *
 * <p>Scales the player model slightly larger and applies a subtle rainbow glow
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
        // Subtle scale up — marks the pushed state for the Post handler
        poseStack.scale(1.08F, 1.08F, 1.08F);
        player.getPersistentData().putBoolean("xebOmegaPosePushed", true);
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (player.getPersistentData().getBoolean("xebOmegaPosePushed")) {
            player.getPersistentData().remove("xebOmegaPosePushed");
            event.getPoseStack().popPose();
        }
    }
}
