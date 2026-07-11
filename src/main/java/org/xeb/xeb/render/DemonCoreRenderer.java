package org.xeb.xeb.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.xeb.xeb.client.model.DemonCoreGeoModel;
import org.xeb.xeb.entity.DemonCoreEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DemonCoreRenderer extends GeoEntityRenderer<DemonCoreEntity> {
    public DemonCoreRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DemonCoreGeoModel());
        this.shadowRadius = 0.9F;
    }

    @Override
    public void render(DemonCoreEntity entity, float entityYaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(3.0F, 3.0F, 3.0F);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public software.bernie.geckolib.core.object.Color getRenderColor(DemonCoreEntity animatable, float partialTick, int packedLight) {
        if (animatable.getItem().getOrCreateTag().getBoolean("RedCore")) {
            return software.bernie.geckolib.core.object.Color.ofRGBA(1.0F, 0.3F, 0.3F, 1.0F);
        }
        return super.getRenderColor(animatable, partialTick, packedLight);
    }
}
