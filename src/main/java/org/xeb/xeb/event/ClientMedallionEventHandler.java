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

        boolean pushedPose = false;

        // Materialize effect scaling (starts at 10% size, grows to 100% over 10 ticks)
        if (entity.getPersistentData().contains("xebMaterializingTicks")) {
            int matTicks = entity.getPersistentData().getInt("xebMaterializingTicks");
            if (matTicks > 0) {
                float matScale = 1.0F - (matTicks / 10.0F * 0.9F);
                event.getPoseStack().pushPose();
                event.getPoseStack().scale(matScale, matScale, matScale);
                entity.getPersistentData().putBoolean("xebPushedMatPose", true);
                pushedPose = true;
            }
        }

        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        if (!medallions.isEmpty()) {
            int megaCount = 0;
            for (MedallionData m : medallions) {
                if (m.getBuff().getId().equals("mega")) megaCount++;
            }
            if (megaCount > 0) {
                float scaleFactor = 1.0F + 0.30F * megaCount;
                if (!pushedPose) {
                    event.getPoseStack().pushPose();
                    entity.getPersistentData().putBoolean("xebPushedMatPose", true);
                }
                event.getPoseStack().scale(scaleFactor, scaleFactor, scaleFactor);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        if (entity.getPersistentData().getBoolean("xebPushedMatPose")) {
            entity.getPersistentData().remove("xebPushedMatPose");
            event.getPoseStack().popPose();
        }
    }
}
