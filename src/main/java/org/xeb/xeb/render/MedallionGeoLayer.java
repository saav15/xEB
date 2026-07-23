package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class MedallionGeoLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    public MedallionGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        LivingEntity entity = null;
        if (animatable instanceof LivingEntity living) {
            entity = living;
        } else if (this.getRenderer() instanceof software.bernie.geckolib.renderer.GeoReplacedEntityRenderer<?, ?> replacedRenderer) {
            entity = org.xeb.xeb.Xeb.getEntityFromReplacedRenderer(replacedRenderer);
        }
        if (entity == null) return;

        org.joml.Vector3f dir = new org.joml.Vector3f(0.0F, 1.0F, 0.0F);
        poseStack.last().pose().transformDirection(dir);
        float scale = dir.length();
        if (scale < 0.001F) scale = 1.0F;

        float worldHeight = MedallionRenderLayer.getMedallionWorldHeight(entity);
        float heightOffset = worldHeight / scale;

        poseStack.pushPose();
        // Corregir inversión de Y de GeckoLib: elevar hacia arriba y rotar 180°
        poseStack.translate(0.0F, heightOffset, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        MedallionRenderLayer.renderMedallionsStatic(poseStack, bufferSource, packedLight, entity, partialTick);
        poseStack.popPose();
    }
}
