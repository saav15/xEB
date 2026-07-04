package org.xeb.xeb.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PermanightClientRenderer {
    public static boolean isPermanightActive = false;

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (isPermanightActive) {
            // Dark reddish fog tint (RGB: 75, 10, 10 / 255 -> 0.29F, 0.04F, 0.04F)
            event.setRed(0.29F);
            event.setGreen(0.04F);
            event.setBlue(0.04F);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (isPermanightActive) {
            float farPlaneDistance = event.getFarPlaneDistance();
            // Restrict fog draw boundaries slightly for a gloomier atmosphere
            event.setNearPlaneDistance(farPlaneDistance * 0.05F);
            event.setFarPlaneDistance(farPlaneDistance * 0.60F);
            event.setCanceled(true); // Cancels default fog boundaries to apply ours
        }
    }
}
