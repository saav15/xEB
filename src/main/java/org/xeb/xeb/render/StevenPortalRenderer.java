package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.xeb.xeb.entity.StevenPortalEntity;

import java.util.Random;

public class StevenPortalRenderer extends EntityRenderer<StevenPortalEntity> {
    private static final ResourceLocation DUMMY_TEX = new ResourceLocation("minecraft", "textures/misc/white.png");

    public StevenPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(StevenPortalEntity entity) {
        return DUMMY_TEX;
    }

    @Override
    public void render(StevenPortalEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.1D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float time = entity.tickCount + partialTicks;
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer builder = buffer.getBuffer(RenderType.lightning());

        // 1. Marco exterior del Portal (Anillo ovalado de 1.2x2.2 bloques)
        float width = 0.75F;
        float height = 1.25F;
        int segments = 24;

        for (int i = 0; i < segments; i++) {
            double a1 = i * (Math.PI * 2.0D / segments);
            double a2 = (i + 1) * (Math.PI * 2.0D / segments);

            float x1 = (float) (Math.cos(a1) * width);
            float y1 = (float) (Math.sin(a1) * height);
            float x2 = (float) (Math.cos(a2) * width);
            float y2 = (float) (Math.sin(a2) * height);

            // Borde exterior blanco estático
            renderQuadSimple(builder, matrix, x1, y1, -0.05F, x2, y2, 0.05F, 255, 255, 255, 240);
        }

        // 2. Interior de Estática Blanco y Negro (Vórtice animado de ruido)
        Random rand = new Random((long) (time * 100));
        int staticLayers = 6;
        for (int l = 0; l < staticLayers; l++) {
            float layerScale = 0.9F - (l * 0.12F);
            double rot = (time * (l % 2 == 0 ? 0.15D : -0.2D)) + l;
            float cosR = (float) Math.cos(rot);
            float sinR = (float) Math.sin(rot);

            for (int s = 0; s < 8; s++) {
                float rx = (rand.nextFloat() - 0.5F) * width * 1.6F * layerScale;
                float ry = (rand.nextFloat() - 0.5F) * height * 1.6F * layerScale;
                float rw = 0.15F + rand.nextFloat() * 0.25F;
                float rh = 0.15F + rand.nextFloat() * 0.25F;

                int colorVal = rand.nextBoolean() ? 255 : 10;
                int alpha = 180 + rand.nextInt(75);

                renderQuadSimple(builder, matrix, rx - rw, ry - rh, -0.01F * l, rx + rw, ry + rh, 0.01F * l, colorVal, colorVal, colorVal, alpha);
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void renderQuadSimple(VertexConsumer builder, Matrix4f matrix,
                                         float x1, float y1, float z1, float x2, float y2, float z2,
                                         int r, int g, int b, int a) {
        builder.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y2, z2).color(r, g, b, a).endVertex();
    }
}
