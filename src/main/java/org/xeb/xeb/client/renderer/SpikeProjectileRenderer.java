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
import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.SpikeProjectileEntity;

/**
 * Renders the big spike projectile as a 3D crystal/ice spike.
 * Oriented along its velocity direction.
 */
public class SpikeProjectileRenderer extends EntityRenderer<SpikeProjectileEntity> {

    public SpikeProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SpikeProjectileEntity entity) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/spike_projectile.png");
    }

    @Override
    public void render(SpikeProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        
        // Orient the spike along velocity direction
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() > 0.001) {
            float yaw = (float) Math.toDegrees(Math.atan2(-motion.x, motion.z));
            float pitch = (float) Math.toDegrees(Math.asin(motion.y / motion.length()));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-pitch));
        }
        
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        
        // Draw the spike as an elongated hexagonal cone
        float length = 1.5F;
        float radius = 0.25F;
        
        // Color: Aqua green / cyan (hielo/energía)
        float r = 0.0F, g = 1.0F, b = 0.8F;
        float a = 0.9F;
        
        // 6 sides of the cone
        int segments = 6;
        for (int i = 0; i < segments; i++) {
            double angle1 = (i * 2.0 * Math.PI) / segments;
            double angle2 = ((i + 1) * 2.0 * Math.PI) / segments;
            
            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);
            
            // Side faces
            consumer.vertex(matrix, x1, 0.0F, z1).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, 0.0F, z2).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, length, z2).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x1, length, z1).color(r, g, b, a).endVertex();
        }
        
        // Tip (cone)
        for (int i = 0; i < segments; i++) {
            double angle1 = (i * 2.0 * Math.PI) / segments;
            double angle2 = ((i + 1) * 2.0 * Math.PI) / segments;
            
            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);
            
            consumer.vertex(matrix, x1, length, z1).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, length, z2).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, 0.0F, length + 0.5F, 0.0F).color(1.0F, 1.0F, 1.0F, a).endVertex();
            consumer.vertex(matrix, 0.0F, length + 0.5F, 0.0F).color(1.0F, 1.0F, 1.0F, a).endVertex();
        }
        
        // Base
        for (int i = 0; i < segments; i++) {
            double angle1 = (i * 2.0 * Math.PI) / segments;
            double angle2 = ((i + 1) * 2.0 * Math.PI) / segments;
            
            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);
            
            consumer.vertex(matrix, x1, 0.0F, z1).color(r * 0.7F, g * 0.7F, b * 0.7F, a).endVertex();
            consumer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(r * 0.7F, g * 0.7F, b * 0.7F, a).endVertex();
            consumer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(r * 0.7F, g * 0.7F, b * 0.7F, a).endVertex();
            consumer.vertex(matrix, x2, 0.0F, z2).color(r * 0.7F, g * 0.7F, b * 0.7F, a).endVertex();
        }
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
