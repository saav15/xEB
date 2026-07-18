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

        // Spin the crack lines slowly over time to keep it dynamic
        float angle = (entity.tickCount + partialTick) * 0.4F;
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

        // Generate persistent randomized jagged polygon seeded by entity ID
        java.util.Random rand = new java.util.Random(entity.getId());
        int numPoints = 9 + rand.nextInt(5); // 9 to 13 vertices for inexact broken look
        float[] angles = new float[numPoints];
        float[] radii = new float[numPoints];

        for (int i = 0; i < numPoints; i++) {
            angles[i] = (float) (i * (2.0D * Math.PI / numPoints) + (rand.nextDouble() - 0.5D) * 0.15D);
            radii[i] = 0.6F + rand.nextFloat() * 0.8F; // variable radial spikes (0.6 to 1.4 blocks)
        }

        // 1. Draw the inner dark void (Triangle Fan, Y = +0.01)
        // Center vertex of the fan
        consumer.vertex(matrix, 0.0F, 0.01F, 0.0F)
                .color(r * 0.02F, g * 0.02F, b * 0.02F, 0.95F)
                .uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                .endVertex();

        for (int i = 0; i <= numPoints; i++) {
            int idx = i % numPoints;
            float cos = (float) Math.cos(angles[idx]);
            float sin = (float) Math.sin(angles[idx]);
            float vx = cos * radii[idx];
            float vz = sin * radii[idx];
            consumer.vertex(matrix, vx, 0.01F, vz)
                    .color(r * 0.02F, g * 0.02F, b * 0.02F, 0.95F)
                    .uv(0.5F + cos * 0.5F, 0.5F + sin * 0.5F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(packedLight)
                    .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                    .endVertex();
        }

        // 2. Draw the glowing jagged boundary perimeter lines (Y = +0.02)
        for (int i = 0; i < numPoints; i++) {
            int idxA = i;
            int idxB = (i + 1) % numPoints;
            
            float cosA = (float) Math.cos(angles[idxA]);
            float sinA = (float) Math.sin(angles[idxA]);
            float cosB = (float) Math.cos(angles[idxB]);
            float sinB = (float) Math.sin(angles[idxB]);
            
            float ax = cosA * radii[idxA];
            float az = sinA * radii[idxA];
            float bx = cosB * radii[idxB];
            float bz = sinB * radii[idxB];

            // Primary bright core outline
            drawGlowLine(consumer, matrix, normalMatrix, ax, az, bx, bz, 0.04F, r, g, b, 0.90F, packedLight);
            // Secondary wide translucent halo aura outline
            drawGlowLine(consumer, matrix, normalMatrix, ax, az, bx, bz, 0.12F, r, g, b, 0.35F, packedLight);

            // Add optional mini internal crack splits branching from vertices towards center
            if (i % 2 == 0) {
                float cx = ax * 0.4F;
                float cz = az * 0.4F;
                drawGlowLine(consumer, matrix, normalMatrix, ax, az, cx, cz, 0.03F, r * 0.8F, g * 0.8F, b * 0.8F, 0.70F, packedLight);
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void drawGlowLine(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                     float ax, float az, float bx, float bz, float width,
                                     float r, float g, float b, float a, int light) {
        float dx = bx - ax;
        float dz = bz - az;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001F) return;
        float nx = -dz / len * width;
        float nz = dx / len * width;
        
        consumer.vertex(matrix, ax - nx, 0.02F, az - nz).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, ax + nx, 0.02F, az - nz).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, bx + nx, 0.02F, bz + nz).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, bx - nx, 0.02F, bz + nz).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ShatteredRiftEntity entity) {
        return WHITE_TEX;
    }
}
