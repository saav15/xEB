package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.entity.StevenCloneEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class StevenCloneRenderer extends GeoEntityRenderer<StevenCloneEntity> {
    public StevenCloneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new StevenCloneModel());
    }

    @Override
    public RenderType getRenderType(StevenCloneEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void render(StevenCloneEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // Clones más pequeños (0.65x) y más traslúcidos espectrales
        poseStack.scale(0.65F, 0.65F, 0.65F);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
