package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.TearsProjectileEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BrimstoneBeamRenderer {

    public static final Map<Integer, ClientBrimstoneData> CLIENT_BRIMSTONES = new ConcurrentHashMap<>();

    public static void handleBeamPacket(int ownerEntityId, boolean active, int imbueType, List<Vec3> points) {
        if (active && points.size() >= 2) {
            CLIENT_BRIMSTONES.put(ownerEntityId, new ClientBrimstoneData(points, imbueType, System.currentTimeMillis()));
        } else {
            CLIENT_BRIMSTONES.remove(ownerEntityId);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || CLIENT_BRIMSTONES.isEmpty()) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        long now = System.currentTimeMillis();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (Map.Entry<Integer, ClientBrimstoneData> entry : CLIENT_BRIMSTONES.entrySet()) {
            ClientBrimstoneData beam = entry.getValue();

            // Expire stale beams (500ms timeout)
            if (now - beam.lastUpdate > 500) {
                CLIENT_BRIMSTONES.remove(entry.getKey());
                continue;
            }

            List<Vec3> points = beam.points;
            int imbue = beam.imbueType;

            // Colors mapping
            float r = 0.9F, g = 0.0F, b = 0.0F, a = 0.9F; // Default: Blood red
            if (imbue == TearsProjectileEntity.IMBUE_PURPLE) {
                r = 0.7F; g = 0.0F; b = 1.0F; // Purple
            } else if (imbue == TearsProjectileEntity.IMBUE_WHITE) {
                r = 1.0F; g = 1.0F; b = 1.0F; // White
            } else if (imbue == TearsProjectileEntity.IMBUE_DARK) {
                r = 0.15F; g = 0.1F; b = 0.15F; // Tar black
            } else if (imbue == TearsProjectileEntity.IMBUE_COLD) {
                r = 0.5F; g = 0.85F; b = 1.0F; // Ice blue
            }

            for (int i = 0; i < points.size() - 1; i++) {
                drawBrimstoneSegment(poseStack, consumer, points.get(i), points.get(i + 1), r, g, b, now);
            }

            // Draw volumetric collision sprite at final endpoint
            renderCollisionSprite(poseStack, consumer, points.get(points.size() - 1), r, g, b, now);
        }

        bufferSource.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    private static void drawBrimstoneSegment(PoseStack poseStack, VertexConsumer consumer,
                                             Vec3 start, Vec3 end, float r, float g, float b, long timeMs) {
        Vec3 beamDir = end.subtract(start);
        double length = beamDir.length();
        if (length < 0.01D) return;

        Vec3 beamDirNorm = beamDir.normalize();

        // Visor aligned perpendicular vectors
        Vec3 perp1 = beamDirNorm.cross(new Vec3(0, 1, 0));
        if (perp1.lengthSqr() < 0.0001D) {
            perp1 = beamDirNorm.cross(new Vec3(1, 0, 0));
        }
        perp1 = perp1.normalize();
        Vec3 perp2 = beamDirNorm.cross(perp1).normalize();

        float pulse = 0.90F + 0.10F * (float) Math.sin(timeMs * 0.008D);
        float glowPulse = 0.75F + 0.25F * (float) Math.sin(timeMs * 0.005D);

        // Core thickness scaled up to 1.5x1.5 blocks width (0.75F radius)
        float coreH = 0.50F * pulse;
        float coreV = 0.50F * pulse;

        float midH = 0.75F * glowPulse;
        float midV = 0.75F * glowPulse;

        float auraH = 1.10F * glowPulse;
        float auraV = 1.10F * glowPulse;

        Matrix4f matrix = poseStack.last().pose();

        // Layer 1: Outer aura
        drawRectQuad(consumer, matrix, start, end, perp1, perp2, auraH, auraV, r, g, b, 0.06F * glowPulse);

        // Layer 2: Mid glow
        drawRectQuad(consumer, matrix, start, end, perp1, perp2, midH, midV, r, g, b, 0.16F * glowPulse);

        // Layer 3: Core beam (slightly darker core for Tar, white center for normal/laser)
        float coreR = r, coreG = g, coreB = b;
        if (r == 0.9F && g == 0.0F && b == 0.0F) { // Blood red
            coreR = 0.3F; coreG = 0.0F; coreB = 0.0F; // Darker center representing brimstone core
        }
        drawRectQuad(consumer, matrix, start, end, perp1, perp2, coreH, coreV, coreR, coreG, coreB, 0.85F);

        // Layer 4: Inner hot line
        float innerH = coreH * 0.3F;
        float innerV = coreV * 0.3F;
        drawRectQuad(consumer, matrix, start, end, perp1, perp2, innerH, innerV, 1.0F, 1.0F, 1.0F, 0.95F);
    }

    private static void renderCollisionSprite(PoseStack poseStack, VertexConsumer consumer, Vec3 end,
                                               float r, float g, float b, long timeMs) {
        Matrix4f matrix = poseStack.last().pose();
        float pulse = 0.85F + 0.15F * (float) Math.sin(timeMs * 0.02D);

        float outerSize = 1.2F * pulse;
        drawCrossQuad(consumer, matrix, end, outerSize, r, g, b, 0.20F * pulse);

        float midSize = 0.8F * pulse;
        drawCrossQuad(consumer, matrix, end, midSize, r, g, b, 0.50F * pulse);

        float innerSize = 0.4F * pulse;
        drawCrossQuad(consumer, matrix, end, innerSize, 1.0F, 1.0F, 1.0F, 0.85F);
    }

    private static void drawCrossQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 center, float halfWidth,
                                       float r, float g, float b, float a) {
        // Horizontal (XZ)
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y, (float) center.z - halfWidth).color(r, g, b, a).endVertex();

        // Vertical XY
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y - halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y + halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y + halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y - halfWidth, (float) center.z).color(r, g, b, a).endVertex();

        // Vertical YZ
        consumer.vertex(matrix, (float) center.x, (float) center.y - halfWidth, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y + halfWidth, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y + halfWidth, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y - halfWidth, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
    }

    private static void drawRectQuad(VertexConsumer consumer, Matrix4f matrix,
                                      Vec3 start, Vec3 end, Vec3 perpHoriz, Vec3 perpVert,
                                      float halfWidthH, float halfWidthV,
                                      float r, float g, float b, float a) {
        drawQuad(consumer, matrix, start, end, perpHoriz, halfWidthH, r, g, b, a);
        drawQuad(consumer, matrix, start, end, perpVert, halfWidthV, r, g, b, a);
    }

    private static void drawQuad(VertexConsumer consumer, Matrix4f matrix,
                                  Vec3 start, Vec3 end, Vec3 perp, float halfWidth,
                                  float r, float g, float b, float a) {
        Vec3 p1 = start.add(perp.scale(halfWidth));
        Vec3 p2 = start.add(perp.scale(-halfWidth));
        Vec3 p3 = end.add(perp.scale(-halfWidth));
        Vec3 p4 = end.add(perp.scale(halfWidth));

        consumer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z).color(r, g, b, a).endVertex();
    }

    private static class ClientBrimstoneData {
        final List<Vec3> points;
        final int imbueType;
        long lastUpdate;

        ClientBrimstoneData(List<Vec3> points, int imbueType, long lastUpdate) {
            this.points = points;
            this.imbueType = imbueType;
            this.lastUpdate = lastUpdate;
        }
    }
}
