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
import org.xeb.xeb.entity.TearsProjectileEntity;

public class TearsProjectileRenderer extends EntityRenderer<TearsProjectileEntity> {
    private static final ResourceLocation TEAR_TEX = new ResourceLocation("xeb", "textures/entity/tear.png");

    public TearsProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TearsProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Billboard: Face the camera
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEAR_TEX));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        // Default: Aqua blue
        float r = 0.2F, g = 0.8F, b = 1.0F, a = 0.9F;

        // Imbuement colors
        switch (entity.getImbueType()) {
            case TearsProjectileEntity.IMBUE_PURPLE -> {
                r = 0.7F; g = 0.0F; b = 1.0F; // Dark violet / purple
            }
            case TearsProjectileEntity.IMBUE_WHITE -> {
                r = 1.0F; g = 1.0F; b = 1.0F; // Bright white
            }
            case TearsProjectileEntity.IMBUE_DARK -> {
                r = 0.15F; g = 0.15F; b = 0.18F; // Dark tar black
            }
            case TearsProjectileEntity.IMBUE_COLD -> {
                r = 0.6F; g = 0.9F; b = 1.0F; // Light ice blue
            }
        }

        // Bounding box size is 0.6, so render size should be ~0.35F radius
        float size = 0.35F;

        // Draw the flat billboard sphere quad
        drawQuadTextured(consumer, matrix, normalMatrix, 0.0F, 0.0F, size, r, g, b, a, packedLight);

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
    public ResourceLocation getTextureLocation(TearsProjectileEntity entity) {
        return TEAR_TEX;
    }
}
