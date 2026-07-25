package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.xeb.xeb.entity.StaticPortalEntity;

public class StaticPortalRenderer extends EntityRenderer<StaticPortalEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("xeb", "textures/entity/white.png");
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    public StaticPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(StaticPortalEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(StaticPortalEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(StaticPortalEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        long gameTime = entity.level().getGameTime();
        int glitchTicks = entity.getGlitchTicks();

        // Glitch displacement (1 tick horizontal shift)
        if (glitchTicks > 0) {
            long seedShift = entity.getId() * 17L + gameTime;
            float shiftX = (((seedShift * 1234567L) ^ (seedShift >> 11)) * 2.3283064e-10f) * 0.3f - 0.15f;
            float shiftY = ((((seedShift + 999) * 1234567L) ^ (seedShift >> 7)) * 2.3283064e-10f) * 0.15f - 0.075f;
            poseStack.translate(shiftX, shiftY, 0.0D);
        }

        // Align facing to camera
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        // 1. CAPA AURA EXTERIOR (Monocromática Blanco/Gris Pulsante)
        renderOuterAura(consumer, matrix, normalMatrix, gameTime);

        // 2. CAPA MARCO EXTERIOR (Negro Profundo Estático)
        renderPortalFrame(consumer, matrix, normalMatrix, entity.getId());

        // 3. CAPA ESTÁTICA TV 8x12 GRID (Monocromático Estricto: Negro, Gris, Blanco)
        renderTVStaticGrid(consumer, matrix, normalMatrix, entity.getId(), gameTime, glitchTicks);

        // 4. CAPA SCANLINES HORIZONTALES (Líneas Blancas Translúcidas)
        renderScanlines(consumer, matrix, normalMatrix, gameTime, entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static float fastNoise(long seed) {
        float n = ((seed * 1234567L) ^ (seed >> 13)) * 2.3283064e-10f;
        return Math.abs(n) % 1.0f;
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

    private static void addQuadBothSides(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                         float x0, float y0, float x1, float y1, float z,
                                         float r, float g, float b, float a, int light) {
        // Front face
        addVertex(consumer, matrix, normalMatrix, x0, y0, z, r, g, b, a, 0.0F, 0.0F, light);
        addVertex(consumer, matrix, normalMatrix, x1, y0, z, r, g, b, a, 1.0F, 0.0F, light);
        addVertex(consumer, matrix, normalMatrix, x1, y1, z, r, g, b, a, 1.0F, 1.0F, light);
        addVertex(consumer, matrix, normalMatrix, x0, y1, z, r, g, b, a, 0.0F, 1.0F, light);

        // Back face
        addVertex(consumer, matrix, normalMatrix, x0, y0, z, r, g, b, a, 0.0F, 0.0F, light);
        addVertex(consumer, matrix, normalMatrix, x0, y1, z, r, g, b, a, 0.0F, 1.0F, light);
        addVertex(consumer, matrix, normalMatrix, x1, y1, z, r, g, b, a, 1.0F, 1.0F, light);
        addVertex(consumer, matrix, normalMatrix, x1, y0, z, r, g, b, a, 1.0F, 0.0F, light);
    }

    private static void addSegmentQuadBothSides(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                                float x1In, float y1In, float x1Out, float y1Out,
                                                float x2Out, float y2Out, float x2In, float y2In, float z,
                                                float r, float g, float b, float a, int light) {
        // Front face quad
        addVertex(consumer, matrix, normalMatrix, x1In, y1In, z, r, g, b, a, 0.0F, 0.0F, light);
        addVertex(consumer, matrix, normalMatrix, x1Out, y1Out, z, r, g, b, a, 1.0F, 0.0F, light);
        addVertex(consumer, matrix, normalMatrix, x2Out, y2Out, z, r, g, b, a, 1.0F, 1.0F, light);
        addVertex(consumer, matrix, normalMatrix, x2In, y2In, z, r, g, b, a, 0.0F, 1.0F, light);

        // Back face quad
        addVertex(consumer, matrix, normalMatrix, x1In, y1In, z, r, g, b, a, 0.0F, 0.0F, light);
        addVertex(consumer, matrix, normalMatrix, x2In, y2In, z, r, g, b, a, 0.0F, 1.0F, light);
        addVertex(consumer, matrix, normalMatrix, x2Out, y2Out, z, r, g, b, a, 1.0F, 1.0F, light);
        addVertex(consumer, matrix, normalMatrix, x1Out, y1Out, z, r, g, b, a, 1.0F, 0.0F, light);
    }

    private void renderOuterAura(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, long gameTime) {
        float pulseAlpha = 0.12f + 0.08f * (float) Math.sin(gameTime * 0.08f);
        int segments = 16;
        float rxIn = 1.02f;
        float ryIn = 1.52f;
        float rxOut = 1.25f;
        float ryOut = 1.75f;
        float cy = 1.5f;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * Math.PI * 2 / segments);
            float a2 = (float) ((i + 1) * Math.PI * 2 / segments);

            float x1In = (float) Math.cos(a1) * rxIn;
            float y1In = cy + (float) Math.sin(a1) * ryIn;
            float x1Out = (float) Math.cos(a1) * rxOut;
            float y1Out = cy + (float) Math.sin(a1) * ryOut;

            float x2In = (float) Math.cos(a2) * rxIn;
            float y2In = cy + (float) Math.sin(a2) * ryIn;
            float x2Out = (float) Math.cos(a2) * rxOut;
            float y2Out = cy + (float) Math.sin(a2) * ryOut;

            // Clean 4-vertex quad ring (Strict Monochromatic White/Light Gray)
            addSegmentQuadBothSides(consumer, matrix, normalMatrix,
                    x1In, y1In, x1Out, y1Out, x2Out, y2Out, x2In, y2In, -0.02f,
                    0.9f, 0.9f, 0.9f, pulseAlpha, FULL_BRIGHT);
        }
    }

    private void renderPortalFrame(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, int entityId) {
        int segments = 16;
        float rxIn = 0.96f;
        float ryIn = 1.46f;
        float rxOut = 1.05f;
        float ryOut = 1.55f;
        float cy = 1.5f;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * Math.PI * 2 / segments);
            float a2 = (float) ((i + 1) * Math.PI * 2 / segments);

            float noise1 = fastNoise(entityId * 1007L + i * 31L) * 0.04f - 0.02f;
            float noise2 = fastNoise(entityId * 1007L + (i + 1) * 31L) * 0.04f - 0.02f;

            float x1In = (float) Math.cos(a1) * (rxIn + noise1);
            float y1In = cy + (float) Math.sin(a1) * (ryIn + noise1);
            float x1Out = (float) Math.cos(a1) * (rxOut + noise1);
            float y1Out = cy + (float) Math.sin(a1) * (ryOut + noise1);

            float x2In = (float) Math.cos(a2) * (rxIn + noise2);
            float y2In = cy + (float) Math.sin(a2) * (ryIn + noise2);
            float x2Out = (float) Math.cos(a2) * (rxOut + noise2);
            float y2Out = cy + (float) Math.sin(a2) * (ryOut + noise2);

            // Clean Frame rim quad (Deep Pitch Black 0.02, 0.02, 0.02, 0.98)
            addSegmentQuadBothSides(consumer, matrix, normalMatrix,
                    x1In, y1In, x1Out, y1Out, x2Out, y2Out, x2In, y2In, -0.01f,
                    0.02f, 0.02f, 0.02f, 0.98f, FULL_BRIGHT);
        }
    }

    private void renderTVStaticGrid(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, int entityId, long gameTime, int glitchTicks) {
        int cols = 8;
        int rows = 12;
        float width = 2.0f;
        float height = 3.0f;

        float cellW = width / cols;
        float cellH = height / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float x0 = -1.0f + c * cellW;
                float x1 = x0 + cellW;
                float y0 = r * cellH;
                float y1 = y0 + cellH;

                float cx = (x0 + x1) * 0.5f;
                float cy = (y0 + y1) * 0.5f;

                // Oval boundary test
                float normX = cx / 0.98f;
                float normY = (cy - 1.5f) / 1.48f;
                if ((normX * normX + normY * normY) > 1.0f) {
                    continue;
                }

                int quadIdx = r * cols + c;
                long seed = (entityId * 31L) + (gameTime * 17L) + (quadIdx * 10007L);

                boolean isGlitchQuad = false;
                if (glitchTicks > 0) {
                    float glitchChance = fastNoise(seed + 9999L);
                    if (glitchChance < 0.35f) {
                        isGlitchQuad = true;
                    }
                }

                float shade;
                float alpha;

                if (isGlitchQuad) {
                    // Glitch flashes: alternating pure white and pitch black
                    shade = fastNoise(seed + 555L) > 0.5f ? 1.0f : 0.0f;
                    alpha = 1.0f;
                } else {
                    // Strictly Monochromatic Noise: Gray range 0.1 to 0.9
                    shade = fastNoise(seed) * 0.8f + 0.1f;
                    alpha = 0.75f + fastNoise(seed + 1234L) * 0.25f;
                }

                // Pure Monochromatic: R = G = B = shade
                addQuadBothSides(consumer, matrix, normalMatrix, x0, y0, x1, y1, 0.0f,
                        shade, shade, shade, alpha, FULL_BRIGHT);
            }
        }
    }

    private void renderScanlines(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, long gameTime, int entityId) {
        int count = 6;
        for (int i = 0; i < count; i++) {
            float scanY = 3.0f - ((gameTime * 0.04f + i * 0.50f) % 3.0f);
            float jitter = (fastNoise(entityId * 77L + gameTime + i * 13L) - 0.5f) * 0.15f;

            float y0 = scanY - 0.02f;
            float y1 = scanY + 0.02f;
            float x0 = -0.9f + jitter;
            float x1 = 0.9f + jitter;

            // Monochromatic White Scanline
            addQuadBothSides(consumer, matrix, normalMatrix, x0, y0, x1, y1, 0.01f,
                    1.0f, 1.0f, 1.0f, 0.40f, FULL_BRIGHT);
        }
    }
}
