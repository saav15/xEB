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
        Matrix3f normalMatrix = poseStack.last().normal();

        // Get colors based on color index (same as load sequence)
        float r = 1.0F, g = 1.0F, b = 1.0F;
        switch (entity.getColorIndex() % 6) {
            case 0 -> { r = 0.0F; g = 1.0F; b = 1.0F; } // Cyan
            case 1 -> { r = 0.6F; g = 0.1F; b = 0.9F; } // Purple
            case 2 -> { r = 1.0F; g = 0.6F; b = 0.0F; } // Orange
            case 3 -> { r = 0.0F; g = 1.0F; b = 0.0F; } // Green
            case 4 -> { r = 0.1F; g = 0.1F; b = 1.0F; } // Blue
            case 5 -> { r = 1.0F; g = 1.0F; b = 0.0F; } // Yellow
        }

        // Draw 3D flower components:
        // 1. Calyx / Back Support (dark green)
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.04F);
        drawQuadTextured(consumer, poseStack.last().pose(), normalMatrix, 0.0F, 0.0F, 0.28F, r * 0.1F, g * 0.5F + 0.1F, b * 0.1F, 0.8F, packedLight);
        poseStack.popPose();

        // 2. Anillo de 5 Pétalos
        for (int pIndex = 0; pIndex < 5; pIndex++) {
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(pIndex * 72.0F));
            poseStack.translate(0.0F, 0.22F, 0.01F);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(15.0F)); // Tilt forward
            drawQuadTextured(consumer, poseStack.last().pose(), normalMatrix, 0.0F, 0.0F, 0.32F, r, g, b, 0.9F, packedLight);
            poseStack.popPose();
        }

        // 3. Central Core & Pulsing Glow Aura
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -0.04F);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-angle * 0.6F)); // Slow reverse spin
        drawQuadTextured(consumer, poseStack.last().pose(), normalMatrix, 0.0F, 0.0F, 0.22F, 1.0F, 1.0F, 1.0F, 0.95F, packedLight); // White center
        
        float glowPulse = 0.35F + 0.1F * (float) Math.sin((entity.tickCount + partialTick) * 0.2F);
        drawQuadTextured(consumer, poseStack.last().pose(), normalMatrix, 0.0F, 0.0F, 0.45F, r, g, b, glowPulse, packedLight); // Glow aura
        poseStack.popPose();

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
