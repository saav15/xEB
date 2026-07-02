package org.xeb.xeb.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Config;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.render.MedallionRenderLayer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final Logger LOGGER = LogUtils.getLogger();

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
    @SuppressWarnings({"unchecked", "rawtypes"})
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

        // If this renderer already has MedallionRenderLayer, it handles its own rendering.
        // Skip fallback to avoid double rendering.
        LivingEntityRenderer renderer = (LivingEntityRenderer) event.getRenderer();
        if (rendererHasMedallionLayer(renderer)) return;

        // ---- FALLBACK: render for mobs whose renderer has no injected MedallionRenderLayer ----
        if (!Config.medallionRenderEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double distSq = entity.distanceToSqr(mc.player);
        if (distSq > Config.medallionRenderDistance * Config.medallionRenderDistance) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();
        float partialTick = event.getPartialTick();

        // Medallion 3D models above the entity's head
        org.joml.Vector3f dir = new org.joml.Vector3f(0.0F, 1.0F, 0.0F);
        poseStack.last().pose().transformDirection(dir);
        float scale = dir.length();
        if (scale < 0.001F) scale = 1.0F;

        float worldHeight = MedallionRenderLayer.getMedallionWorldHeight(entity);
        float heightOffset = worldHeight / scale;

        poseStack.pushPose();
        poseStack.translate(0.0F, -heightOffset, 0.0F);
        MedallionRenderLayer.renderMedallionsStatic(poseStack, bufferSource, packedLight, entity, partialTick);
        poseStack.popPose();
    }

    // ----- Layer detection (cached via reflection) -----

    private static final Map<Class<?>, Field> LAYER_FIELD_CACHE = new HashMap<>();
    private static final Map<Class<?>, Boolean> LAYER_FIELD_ABSENT = new HashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean rendererHasMedallionLayer(LivingEntityRenderer renderer) {
        if (renderer == null) return false;
        if (renderer instanceof software.bernie.geckolib.renderer.GeoRenderer) return true;
        try {
            Class<?> cls = renderer.getClass();
            if (LAYER_FIELD_ABSENT.containsKey(cls)) return false;
            Field f = LAYER_FIELD_CACHE.get(cls);
            if (f == null) {
                f = findLayersField(cls);
                if (f == null) {
                    LAYER_FIELD_ABSENT.put(cls, Boolean.TRUE);
                    return false;
                }
                f.setAccessible(true);
                LAYER_FIELD_CACHE.put(cls, f);
            }
            List<RenderLayer> layers = (List<RenderLayer>) f.get(renderer);
            if (layers == null) return false;
            for (RenderLayer layer : layers) {
                if (layer instanceof MedallionRenderLayer) return true;
            }
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to check renderer layers via reflection: {}", e.getMessage());
        }
        return false;
    }

    private static Field findLayersField(Class<?> clazz) {
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                // "layers" in dev environment, "f_115322_" in obfuscated production
                if (f.getName().equals("layers") || f.getName().equals("f_115322_")) {
                    return f;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
