package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.MechaLaserPelletEntity;

public class MechaLaserPelletRenderer extends EntityRenderer<MechaLaserPelletEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Xeb.MODID, "textures/entity/white.png");

    public MechaLaserPelletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MechaLaserPelletEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Orient facing movement direction
        float yaw = entity.getViewYRot(partialTicks);
        float pitch = entity.getViewXRot(partialTicks);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));

        // Draw glowing circular laser disc
        VertexConsumer builder = buffer.getBuffer(RenderType.entityTranslucentCull(getTextureLocation(entity)));
        Matrix4f mat = poseStack.last().pose();

        int points = 12;
        float radius = 0.25F;
        int r = 0, g = 220, b = 255, a = 240; // Glowing Cyan laser ring

        for (int i = 0; i < points; i++) {
            double angle1 = (i * 2.0D * Math.PI) / points;
            double angle2 = ((i + 1) * 2.0D * Math.PI) / points;

            float x1 = (float) (Math.cos(angle1) * radius);
            float y1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float y2 = (float) (Math.sin(angle2) * radius);

            builder.vertex(mat, x1, y1, 0.0F).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(240).normal(0, 0, 1).endVertex();
            builder.vertex(mat, x2, y2, 0.0F).color(r, g, b, a).uv(1, 0).overlayCoords(0, 10).uv2(240).normal(0, 0, 1).endVertex();
            builder.vertex(mat, 0.0F, 0.0F, 0.05F).color(255, 255, 255, 255).uv(0.5F, 0.5F).overlayCoords(0, 10).uv2(240).normal(0, 0, 1).endVertex();
            builder.vertex(mat, 0.0F, 0.0F, -0.05F).color(r, g, b, a).uv(0.5F, 0.5F).overlayCoords(0, 10).uv2(240).normal(0, 0, 1).endVertex();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MechaLaserPelletEntity entity) {
        return TEXTURE;
    }
}
