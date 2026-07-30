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
            // Dark void purple fog tint
            event.setRed(0.12F);
            event.setGreen(0.02F);
            event.setBlue(0.25F);
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
