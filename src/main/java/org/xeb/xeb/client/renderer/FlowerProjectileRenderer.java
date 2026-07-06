package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.xeb.xeb.entity.FlowerProjectileEntity;

public class FlowerProjectileRenderer extends EntityRenderer<FlowerProjectileEntity> {
    private static final ResourceLocation WHITE_TEX = new ResourceLocation("xeb", "textures/entity/white.png");

    public FlowerProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FlowerProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Billboard: Face the camera
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        
        // Spin the flower over time
        float angle = (entity.tickCount + partialTick) * 8.0F;
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        // Get colors based on color index
        float r = 1.0F, g = 1.0F, b = 1.0F;
        switch (entity.getColorIndex()) {
            case 0 -> { r = 0.0F; g = 1.0F; b = 1.0F; } // Cyan
            case 1 -> { r = 0.6F; g = 0.1F; b = 0.9F; } // Purple
            case 2 -> { r = 1.0F; g = 0.6F; b = 0.0F; } // Orange
            case 3 -> { r = 0.0F; g = 1.0F; b = 0.0F; } // Green
            case 4 -> { r = 0.1F; g = 0.1F; b = 1.0F; } // Blue
            case 5 -> { r = 1.0F; g = 1.0F; b = 0.0F; } // Yellow
        }

        // Draw petals (scaled down by 100% less than previous version)
        float petalSize = 0.20F;
        float petalOffset = 0.28F;
        for (int i = 0; i < 5; i++) {
            double rad = Math.toRadians(i * 72);
            float dx = (float) Math.cos(rad) * petalOffset;
            float dy = (float) Math.sin(rad) * petalOffset;
            drawQuadTextured(consumer, matrix, normalMatrix, dx, dy, petalSize, r, g, b, 0.9F, packedLight);
        }

        // Draw glowing inner core (scaled down by 100% less, slightly offset forward on Z to avoid Z-fighting)
        poseStack.translate(0.0F, 0.0F, -0.01F);
        Matrix4f matrixCore = poseStack.last().pose();
        drawQuadTextured(consumer, matrixCore, normalMatrix, 0.0F, 0.0F, 0.24F, 1.0F, 1.0F, 1.0F, 0.95F, packedLight); // white-hot center

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void drawQuadTextured(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, float cx, float cy, float size,
                                         float r, float g, float b, float a, int light) {
        consumer.vertex(matrix, cx - size, cy - size, 0.0F).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, cx + size, cy - size, 0.0F).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, cx + size, cy + size, 0.0F).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, cx - size, cy + size, 0.0F).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FlowerProjectileEntity entity) {
        return WHITE_TEX;
    }
}
