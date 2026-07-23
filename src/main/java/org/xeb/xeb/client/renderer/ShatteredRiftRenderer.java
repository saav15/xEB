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
    private static final ResourceLocation BEAM_TEX = new ResourceLocation("xeb", "textures/entity/dark_fountain_beam.png");

    public ShatteredRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ShatteredRiftEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // 1. Animation speed (70% slower)
        float animTime = (entity.tickCount + partialTick) * 0.024F;
        int difficulty = entity.getDifficulty();

        // 2. Player Proximity Brightness Boost (+30% when player is near)
        double camDistSq = this.entityRenderDispatcher.camera.getPosition().distanceToSqr(entity.position());
        double dist = Math.sqrt(camDistSq);
        float proximityBoost = 1.0F;
        if (dist < 10.0D) {
            proximityBoost += 0.30F * (float) (1.0D - (dist / 10.0D));
        }

        // Difficulty Color Palettes (Vibrant Beam vs Dark Grounded Base)
        float rBeam = 1.0F, gBeam = 1.0F, bBeam = 1.0F;
        float rBase = 0.05F, gBase = 0.05F, bBase = 0.05F;

        if (difficulty == 0) { // Blue (Azul Místico / Aqua)
            rBeam = 0.0F; gBeam = 0.65F; bBeam = 1.0F;
            rBase = 0.02F; gBase = 0.12F; bBase = 0.28F;
        } else if (difficulty == 1) { // Green (Verde Esmeralda)
            rBeam = 0.0F; gBeam = 1.0F; bBeam = 0.35F;
            rBase = 0.02F; gBase = 0.22F; bBase = 0.06F;
        } else if (difficulty == 2) { // Red (Rojo Carmesí)
            rBeam = 1.0F; gBeam = 0.10F; bBeam = 0.25F;
            rBase = 0.28F; gBase = 0.02F; bBase = 0.05F;
        } else { // Rainbow / Cosmic (Dificultad 3)
            double cTime = (System.currentTimeMillis() % 2500) / 2500.0 * 2.0 * Math.PI;
            rBeam = (float) (0.5D + 0.5D * Math.sin(cTime));
            gBeam = (float) (0.5D + 0.5D * Math.sin(cTime + 2.0D * Math.PI / 3.0D));
            bBeam = (float) (0.5D + 0.5D * Math.sin(cTime + 4.0D * Math.PI / 3.0D));

            rBase = (float) (0.10D + 0.10D * Math.sin(cTime));
            gBase = (float) (0.02D + 0.02D * Math.sin(cTime + 2.0D * Math.PI / 3.0D));
            bBase = (float) (0.15D + 0.15D * Math.sin(cTime + 4.0D * Math.PI / 3.0D));
        }

        rBeam = Math.min(1.0F, rBeam * proximityBoost);
        gBeam = Math.min(1.0F, gBeam * proximityBoost);
        bBeam = Math.min(1.0F, bBeam * proximityBoost);

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        // -------------------------------------------------------------
        // 1. BASE APERTURE: SMOOTH 64-SEGMENT SOLID DARK GROUNDED RIM & VOID CORE
        // -------------------------------------------------------------
        VertexConsumer solidConsumer = bufferSource.getBuffer(RenderType.entityCutout(WHITE_TEX));

        // Dark Grounded Outer Base Ring (Y = 0.12F, Smooth 64 segments, non-neon dark color)
        drawSolidRing(solidConsumer, matrix, normalMatrix, 0.0F, 0.12F, 0.0F, 1.25F, 0.82F, 64,
                rBase, gBase, bBase, 1.0F, packedLight);

        // Pitch Black Void Core Disc (Y = 0.125F, Smooth 64 segments)
        addSolidCircleFan(solidConsumer, matrix, normalMatrix, 0.0F, 0.125F, 0.0F, 0.82F, 64,
                0.005F, 0.005F, 0.005F, 1.0F, packedLight);

        // -------------------------------------------------------------
        // 2. BEAM COLUMN: INNER EFFECT TEXTURE 100% VISIBLE + TRANSLUCENT OUTER WAVY AURA
        // -------------------------------------------------------------
        float totalHeight = 6.0F + (difficulty * 3.0F);
        int vSteps = (int) (totalHeight * 4);
        int segments = 16;
        float stepHeight = totalHeight / vSteps;

        VertexConsumer beamConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(BEAM_TEX));

        // INNER CORE (Radius 0.42F, Alpha 0.90F, Wave Texture 100% visible)
        renderWavyColumn(beamConsumer, matrix, normalMatrix, animTime, segments, vSteps, totalHeight, stepHeight, 0.42F, rBeam, gBeam, bBeam, 0.90F * Math.min(1.3F, proximityBoost), packedLight, 1.0F);

        // OUTER TRANSLUCENT WAVY AURA (Radius 0.82F, Alpha 0.32F, Wave Texture translucency)
        renderWavyColumn(beamConsumer, matrix, normalMatrix, animTime + 1.5F, segments, vSteps, totalHeight, stepHeight, 0.82F, rBeam, gBeam, bBeam, 0.32F * Math.min(1.3F, proximityBoost), packedLight, -1.2F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void addSolidCircleFan(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                          float cx, float cy, float cz, float radius, int segments,
                                          float r, float g, float b, float a, int light) {
        // Front Face
        addVertex(consumer, matrix, normalMatrix, cx, cy, cz, r, g, b, a, 0.5F, 0.5F, light);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * (2.0D * Math.PI / segments));
            float vx = cx + (float) Math.cos(angle) * radius;
            float vz = cz + (float) Math.sin(angle) * radius;
            addVertex(consumer, matrix, normalMatrix, vx, cy, vz, r, g, b, a, 0.5F, 0.5F, light);
        }

        // Back Face
        addVertex(consumer, matrix, normalMatrix, cx, cy, cz, r, g, b, a, 0.5F, 0.5F, light);
        for (int i = segments; i >= 0; i--) {
            float angle = (float) (i * (2.0D * Math.PI / segments));
            float vx = cx + (float) Math.cos(angle) * radius;
            float vz = cz + (float) Math.sin(angle) * radius;
            addVertex(consumer, matrix, normalMatrix, vx, cy, vz, r, g, b, a, 0.5F, 0.5F, light);
        }
    }

    private static void drawSolidRing(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                     float cx, float cy, float cz, float outerRad, float innerRad,
                                     int segments, float r, float g, float b, float a, int light) {
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            float a1 = (float) (i * (2.0D * Math.PI / segments));
            float a2 = (float) (next * (2.0D * Math.PI / segments));

            float x1Out = cx + (float) Math.cos(a1) * outerRad;
            float z1Out = cz + (float) Math.sin(a1) * outerRad;
            float x2Out = cx + (float) Math.cos(a2) * outerRad;
            float z2Out = cz + (float) Math.sin(a2) * outerRad;

            float x1In = cx + (float) Math.cos(a1) * innerRad;
            float z1In = cz + (float) Math.sin(a1) * innerRad;
            float x2In = cx + (float) Math.cos(a2) * innerRad;
            float z2In = cz + (float) Math.sin(a2) * innerRad;

            // Front
            addVertex(consumer, matrix, normalMatrix, x1Out, cy, z1Out, r, g, b, a, 0.0F, 0.0F, light);
            addVertex(consumer, matrix, normalMatrix, x2Out, cy, z2Out, r, g, b, a, 1.0F, 0.0F, light);
            addVertex(consumer, matrix, normalMatrix, x2In, cy, z2In, r, g, b, a, 1.0F, 1.0F, light);
            addVertex(consumer, matrix, normalMatrix, x1In, cy, z1In, r, g, b, a, 0.0F, 1.0F, light);

            // Back
            addVertex(consumer, matrix, normalMatrix, x1In, cy, z1In, r, g, b, a, 0.0F, 1.0F, light);
            addVertex(consumer, matrix, normalMatrix, x2In, cy, z2In, r, g, b, a, 1.0F, 1.0F, light);
            addVertex(consumer, matrix, normalMatrix, x2Out, cy, z2Out, r, g, b, a, 1.0F, 0.0F, light);
            addVertex(consumer, matrix, normalMatrix, x1Out, cy, z1Out, r, g, b, a, 0.0F, 0.0F, light);
        }
    }

    private static void renderWavyColumn(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                         float animTime, int segments, int vSteps, float totalHeight,
                                         float stepHeight, float baseRadius, float r, float g, float b,
                                         float baseAlpha, int light, float waveDirection) {
        for (int k = 0; k < vSteps; k++) {
            float y1 = k * stepHeight;
            float y2 = (k + 1) * stepHeight;

            // Fade out exponentially towards top (Y = totalHeight)
            float fade1 = Math.max(0.0F, 1.0F - (y1 / totalHeight));
            float fade2 = Math.max(0.0F, 1.0F - (y2 / totalHeight));
            fade1 = fade1 * fade1;
            fade2 = fade2 * fade2;

            float alpha1 = Math.min(1.0F, baseAlpha * fade1);
            float alpha2 = Math.min(1.0F, baseAlpha * fade2);
            if (alpha1 <= 0.01F && alpha2 <= 0.01F) continue;

            // Wavy vertex deformation (70% slower animation speeds)
            float wave1 = (float) (Math.sin((y1 * 1.8D) + (waveDirection * animTime * 4.0D)) * 0.12D
                    + Math.cos((y1 * 3.2D) - (animTime * 2.5D)) * 0.06D);
            float wave2 = (float) (Math.sin((y2 * 1.8D) + (waveDirection * animTime * 4.0D)) * 0.12D
                    + Math.cos((y2 * 3.2D) - (animTime * 2.5D)) * 0.06D);

            float r1 = Math.max(0.1F, baseRadius + wave1);
            float r2 = Math.max(0.1F, baseRadius + wave2);

            float v1 = y1 * 0.35F - animTime * 1.1F;
            float v2 = y2 * 0.35F - animTime * 1.1F;

            for (int i = 0; i < segments; i++) {
                int next = (i + 1) % segments;
                float angle1 = (float) (i * (2.0D * Math.PI / segments));
                float angle2 = (float) (next * (2.0D * Math.PI / segments));

                float u1 = (float) i / segments;
                float u2 = (float) next / segments;

                float x1_y1 = (float) Math.cos(angle1) * r1;
                float z1_y1 = (float) Math.sin(angle1) * r1;
                float x2_y1 = (float) Math.cos(angle2) * r1;
                float z2_y1 = (float) Math.sin(angle2) * r1;

                float x1_y2 = (float) Math.cos(angle1) * r2;
                float z1_y2 = (float) Math.sin(angle1) * r2;
                float x2_y2 = (float) Math.cos(angle2) * r2;
                float z2_y2 = (float) Math.sin(angle2) * r2;

                addVertex(consumer, matrix, normalMatrix, x1_y1, y1, z1_y1, r, g, b, alpha1, u1, v1, light);
                addVertex(consumer, matrix, normalMatrix, x2_y1, y1, z2_y1, r, g, b, alpha1, u2, v1, light);
                addVertex(consumer, matrix, normalMatrix, x2_y2, y2, z2_y2, r, g, b, alpha2, u2, v2, light);
                addVertex(consumer, matrix, normalMatrix, x1_y2, y2, z1_y2, r, g, b, alpha2, u1, v2, light);
            }
        }
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                  float x, float y, float z, float r, float g, float b, float a,
                                  float u, float v, int light) {
        consumer.vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ShatteredRiftEntity entity) {
        return BEAM_TEX;
    }
}
