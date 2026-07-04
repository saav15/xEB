package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.xeb.xeb.entity.MiniLaserProjectileEntity;

/**
 * Renders the mini-laser projectile as a small glowing red elongated quad
 * oriented along the velocity vector.
 */
public class MiniLaserRenderer extends EntityRenderer<MiniLaserProjectileEntity> {

    public MiniLaserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MiniLaserProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Vec3 motion = entity.getDeltaMovement();
        double speed = motion.length();
        if (speed < 0.01D) return;

        Vec3 dir = motion.normalize();

        // Build perpendicular vectors for the quad
        Vec3 up = Math.abs(dir.y) > 0.99D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 perp1 = dir.cross(up).normalize();
        Vec3 perp2 = dir.cross(perp1).normalize();

        float halfWidth = 0.06F;
        float halfLength = 0.35F;

        poseStack.pushPose();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        Vec3 front = dir.scale(halfLength);
        Vec3 back = dir.scale(-halfLength);

        // Draw two cross quads (+ shape) for a 3D look
        drawMiniQuad(consumer, matrix, front, back, perp1, halfWidth,
                1.0F, 0.2F, 0.1F, 0.9F); // bright red
        drawMiniQuad(consumer, matrix, front, back, perp2, halfWidth,
                1.0F, 0.2F, 0.1F, 0.9F);

        // Inner bright core
        drawMiniQuad(consumer, matrix, front, back, perp1, halfWidth * 0.4F,
                1.0F, 0.7F, 0.4F, 0.95F); // orange-white core
        drawMiniQuad(consumer, matrix, front, back, perp2, halfWidth * 0.4F,
                1.0F, 0.7F, 0.4F, 0.95F);

        poseStack.popPose();
    }

    private static void drawMiniQuad(VertexConsumer consumer, Matrix4f matrix,
                                      Vec3 front, Vec3 back, Vec3 perp, float halfWidth,
                                      float r, float g, float b, float a) {
        Vec3 p1 = front.add(perp.scale(halfWidth));
        Vec3 p2 = front.add(perp.scale(-halfWidth));
        Vec3 p3 = back.add(perp.scale(-halfWidth));
        Vec3 p4 = back.add(perp.scale(halfWidth));

        consumer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                .color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                .color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z)
                .color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z)
                .color(r, g, b, a).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(MiniLaserProjectileEntity entity) {
        // No texture needed — rendered procedurally via lightning render type
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }
}
