package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.entity.StevenBossEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class StevenBossRenderer extends GeoEntityRenderer<StevenBossEntity> {
    public StevenBossRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new StevenBossModel());
        this.addRenderLayer(new MedallionGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(StevenBossEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void render(StevenBossEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // 1. Renderizar el Steven principal
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // 2. Renderizar el "Steven Stand" semi-transparente que se SEPARA hasta 3 bloques hacia el objetivo
        if (entity.isStandActive()) {
            poseStack.pushPose();
            double forwardOffset = entity.getStandDistanceToTarget();
            poseStack.translate(0.3D, 0.1D, forwardOffset);
            poseStack.scale(1.15F, 1.15F, 1.15F);

            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
    }
}
