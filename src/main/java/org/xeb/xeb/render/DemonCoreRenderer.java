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
}
