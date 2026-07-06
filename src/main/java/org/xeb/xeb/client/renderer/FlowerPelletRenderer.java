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
import org.xeb.xeb.entity.FlowerPelletEntity;

public class FlowerPelletRenderer extends EntityRenderer<FlowerPelletEntity> {
    private static final ResourceLocation WHITE_TEX = new ResourceLocation("xeb", "textures/entity/white.png");

    public FlowerPelletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FlowerPelletEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // 1. Continuous 3D spin rotation on its own axes
        float age = entity.tickCount + partialTick;
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(age * 12.0F));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(age * 8.0F));

        // 2. Scale Y and Z differently to make it a small white oval (size of a potato)
        poseStack.scale(0.18F, 0.10F, 0.10F);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        // Render a 3D box procedurally (white, fullbright, glowing)
        drawCubeTextured(consumer, matrix, normalMatrix, packedLight);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void drawCubeTextured(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, int light) {
        // Render 6 faces of a unit cube [-1, 1]
        // Front face
        drawFaceTextured(consumer, matrix, normalMatrix, -0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F, 0.0F, 0.0F, 1.0F, light);
        // Back face
        drawFaceTextured(consumer, matrix, normalMatrix, -0.5F, -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.0F, 0.0F, -1.0F, light);
        // Top face
        drawFaceTextured(consumer, matrix, normalMatrix, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, -0.5F, 0.0F, 1.0F, 0.0F, light);
        // Bottom face
        drawFaceTextured(consumer, matrix, normalMatrix, -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F, 0.0F, -1.0F, 0.0F, light);
        // Left face
        drawFaceTextured(consumer, matrix, normalMatrix, -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, -0.5F, -1.0F, 0.0F, 0.0F, light);
        // Right face
        drawFaceTextured(consumer, matrix, normalMatrix, 0.5F, -0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 1.0F, 0.0F, 0.0F, light);
    }

    private static void drawFaceTextured(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                         float x1, float y1, float z1,
                                         float x2, float y2, float z2,
                                         float x3, float y3, float z3,
                                         float x4, float y4, float z4,
                                         float nx, float ny, float nz, int light) {
        float r = 1.0F, g = 1.0F, b = 1.0F, a = 0.9F;
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, nx, ny, nz).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, nx, ny, nz).endVertex();
        consumer.vertex(matrix, x3, y3, z3).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, nx, ny, nz).endVertex();
        consumer.vertex(matrix, x4, y4, z4).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, nx, ny, nz).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FlowerPelletEntity entity) {
        return WHITE_TEX;
    }
}
