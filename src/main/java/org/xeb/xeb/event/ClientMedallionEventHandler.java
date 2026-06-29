package org.xeb.xeb.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;

import java.util.List;

/**
 * Central CLIENT-ONLY event handler that ensures medallion rendering, color overlay,
 * and Mega scaling works for ALL LivingEntity types — including Creepers — using
 * RenderLivingEvent which fires reliably for every entity rendered by LivingEntityRenderer.
 *
 * Uses RenderLivingEvent.Pre/Post which fire for ALL mobs that use LivingEntityRenderer,
 * including CreeperRenderer, without depending on layer injection.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientMedallionEventHandler {

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        if (medallions.isEmpty()) return;

        int megaCount = 0;
        for (MedallionData m : medallions) {
            if (m.getBuff().getId().equals("mega")) megaCount++;
        }
        if (megaCount > 0) {
            float scaleFactor = 1.0F + 0.30F * megaCount;
            event.getPoseStack().pushPose();
            event.getPoseStack().scale(scaleFactor, scaleFactor, scaleFactor);
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        if (medallions.isEmpty()) return;

        int megaCount = 0;
        for (MedallionData m : medallions) {
            if (m.getBuff().getId().equals("mega")) megaCount++;
        }

        // Pop the Mega scale pose that was pushed in Pre
        if (megaCount > 0) {
            event.getPoseStack().popPose();
        }
    }
}
