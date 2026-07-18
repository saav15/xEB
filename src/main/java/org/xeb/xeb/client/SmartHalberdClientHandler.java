package org.xeb.xeb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SmartHalberdClientHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        Player player = mc.player;
        int chargeTicks = player.getPersistentData().getInt("xebHalberdChargeTicks");
        if (chargeTicks <= 0) return;
        
        int spikeCount = player.getPersistentData().getInt("xebHalberdSpikeCount");
        
        // Spawn telegraph particles at each charged spike position
        for (int i = 0; i < spikeCount; i++) {
            String key = "xebHalberdSpike" + i;
            double x = player.getPersistentData().getDouble(key + "X");
            double y = player.getPersistentData().getDouble(key + "Y");
            double z = player.getPersistentData().getDouble(key + "Z");
            
            if (mc.level.getGameTime() % 3 == 0) {
                mc.level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.02, 0.0);
                mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                        (mc.level.random.nextDouble() - 0.5) * 0.1,
                        (mc.level.random.nextDouble() - 0.5) * 0.1,
                        (mc.level.random.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        // Render telegraph spikes for players actively charging
        for (Player player : mc.level.players()) {
            int chargeTicks = player.getPersistentData().getInt("xebHalberdChargeTicks");
            if (chargeTicks <= 0) continue;
            
            int spikeCount = player.getPersistentData().getInt("xebHalberdSpikeCount");
            if (spikeCount <= 0) continue;
            
            Camera camera = event.getCamera();
            Vec3 camPos = camera.getPosition();
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
            
            for (int i = 0; i < spikeCount; i++) {
                String key = "xebHalberdSpike" + i;
                double x = player.getPersistentData().getDouble(key + "X");
                double y = player.getPersistentData().getDouble(key + "Y");
                double z = player.getPersistentData().getDouble(key + "Z");
                
                poseStack.pushPose();
                poseStack.translate(x - camPos.x, y - camPos.y, z - camPos.z);
                
                // Orient based on player's look direction
                Vec3 lookDir = player.getLookAngle();
                float yaw = (float) Math.toDegrees(Math.atan2(-lookDir.x, lookDir.z));
                float pitch = (float) Math.toDegrees(Math.asin(-lookDir.y));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
                
                Matrix4f matrix = poseStack.last().pose();
                
                // Draw translucent telegraph spike (Aqua color matching Smart Halberd)
                drawTelegraphSpike(consumer, matrix, 0.4F);
                
                poseStack.popPose();
            }
            
            bufferSource.endBatch(RenderType.lightning());
        }
    }
    
    private static void drawTelegraphSpike(VertexConsumer consumer, Matrix4f matrix, float alpha) {
        float length = 1.5F;
        float radius = 0.25F;
        
        // Color: Aqua green/cyan matching smart_halberd
        float r = 0.0F, g = 1.0F, b = 0.8F;
        
        // 6 faces of the conal spike
        int segments = 6;
        for (int i = 0; i < segments; i++) {
            double angle1 = (i * 2.0 * Math.PI) / segments;
            double angle2 = ((i + 1) * 2.0 * Math.PI) / segments;
            
            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);
            
            consumer.vertex(matrix, x1, 0.0F, z1).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, x2, 0.0F, z2).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, x2, length, z2).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, x1, length, z1).color(r, g, b, alpha).endVertex();
        }
        
        // Tip
        for (int i = 0; i < segments; i++) {
            double angle1 = (i * 2.0 * Math.PI) / segments;
            double angle2 = ((i + 1) * 2.0 * Math.PI) / segments;
            
            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);
            
            consumer.vertex(matrix, x1, length, z1).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, x2, length, z2).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, 0.0F, length + 0.5F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
            consumer.vertex(matrix, 0.0F, length + 0.5F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        }
    }
}
