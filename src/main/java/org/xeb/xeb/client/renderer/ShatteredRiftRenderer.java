package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.xeb.xeb.entity.ShatteredRiftEntity;

public class ShatteredRiftRenderer extends EntityRenderer<ShatteredRiftEntity> {
    private static final ResourceLocation WHITE_TEX = new ResourceLocation("xeb", "textures/entity/white.png");

    public ShatteredRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ShatteredRiftEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Spin the crack lines slowly over time
        float angle = (entity.tickCount + partialTick) * 0.8F;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        // Get colors based on difficulty
        int difficulty = entity.getDifficulty();
        float r = 1.0F, g = 1.0F, b = 1.0F;

        if (difficulty == 0) { // Blue
            r = 0.1F; g = 0.4F; b = 1.0F;
        } else if (difficulty == 1) { // Green
            r = 0.1F; g = 0.9F; b = 0.3F;
        } else if (difficulty == 2) { // Red
            r = 1.0F; g = 0.1F; b = 0.1F;
        } else { // Rainbow: cycling HSL
            double cTime = (System.currentTimeMillis() % 2000) / 2000.0 * 2.0 * Math.PI;
            r = (float) (0.5D + 0.5D * Math.sin(cTime));
            g = (float) (0.5D + 0.5D * Math.sin(cTime + 2.0D * Math.PI / 3.0D));
            b = (float) (0.5D + 0.5D * Math.sin(cTime + 4.0D * Math.PI / 3.0D));
        }

        // Draw flat crack design on the ground
        // 1. Center Core
        drawFlatQuad(consumer, matrix, normalMatrix, 0.6F, 0.6F, r, g, b, 0.9F, packedLight);

        // 2. Glowing outer backing (large soft quad)
        drawFlatQuad(consumer, matrix, normalMatrix, 1.2F, 1.2F, r, g, b, 0.3F, packedLight);

        // 3. Radial Crack arms (thin long quads rotated at different directions)
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(i * 45.0F));
            drawFlatQuad(consumer, poseStack.last().pose(), normalMatrix, 0.15F, 1.4F, r, g, b, 0.85F, packedLight);
            // Jagged cross branch
            drawFlatQuad(consumer, poseStack.last().pose(), normalMatrix, 0.8F, 0.08F, r * 0.9F, g * 0.9F, b * 0.9F, 0.75F, packedLight);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void drawFlatQuad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, float sizeX, float sizeZ,
                                     float r, float g, float b, float a, int light) {
        consumer.vertex(matrix, -sizeX, 0.02F, -sizeZ).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, sizeX, 0.02F, -sizeZ).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, sizeX, 0.02F, sizeZ).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -sizeX, 0.02F, sizeZ).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ShatteredRiftEntity entity) {
        return WHITE_TEX;
    }
}
