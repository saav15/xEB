package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StevenLaserBeamRenderer {

    public static class StevenLaserData {
        public final Vec3 start;
        public final Vec3 end;
        public final long timestamp;

        public StevenLaserData(Vec3 start, Vec3 end, long timestamp) {
            this.start = start;
            this.end = end;
            this.timestamp = timestamp;
        }
    }

    public static final Map<Integer, StevenLaserData> ACTIVE_STEVEN_LASERS = new ConcurrentHashMap<>();

    public static void updateLaser(int entityId, boolean active, Vec3 start, Vec3 end) {
        if (active) {
            ACTIVE_STEVEN_LASERS.put(entityId, new StevenLaserData(start, end, System.currentTimeMillis()));
        } else {
            ACTIVE_STEVEN_LASERS.remove(entityId);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_STEVEN_LASERS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = System.currentTimeMillis();
        ACTIVE_STEVEN_LASERS.entrySet().removeIf(e -> now - e.getValue().timestamp > 300);

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        for (StevenLaserData laser : ACTIVE_STEVEN_LASERS.values()) {
            renderBrimstoneStyleStevenBeam(poseStack, consumer, laser.start, laser.end, now);
        }

        bufferSource.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    private static void renderBrimstoneStyleStevenBeam(PoseStack poseStack, VertexConsumer consumer, Vec3 start, Vec3 end, long timeMs) {
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        if (len < 0.1D) return;

        int numSegments = 12;
        List<Vec3> points = new ArrayList<>();
        Vec3 norm = dir.normalize();

        for (int i = 0; i <= numSegments; i++) {
            double pct = (double) i / numSegments;
            Vec3 base = start.add(norm.scale(len * pct));
            points.add(base);
        }

        // Subdividir en segmentos orgánicos con desplazamiento de onda de seno (igual a Brimstone)
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 perpHoriz = norm.cross(up);
        if (perpHoriz.lengthSqr() < 0.001D) perpHoriz = new Vec3(1, 0, 0);
        else perpHoriz = perpHoriz.normalize();
        Vec3 perpVert = norm.cross(perpHoriz).normalize();

        Vec3 lastPos = points.get(0);
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 pStart = lastPos;
            Vec3 pEnd = points.get(i + 1);

            if (i < points.size() - 2) {
                double wavePhase = timeMs * 0.015D + i * 0.7D;
                double amp = 0.14D;
                Vec3 waveOffset = perpHoriz.scale(Math.sin(wavePhase) * amp).add(perpVert.scale(Math.cos(wavePhase) * amp));
                pEnd = pEnd.add(waveOffset);
            }

            drawBrimstoneLayeredSegment(poseStack, consumer, pStart, pEnd, perpHoriz, perpVert, timeMs);
            lastPos = pEnd;
        }
    }

    private static void drawBrimstoneLayeredSegment(PoseStack poseStack, VertexConsumer consumer,
                                                    Vec3 start, Vec3 end, Vec3 perpH, Vec3 perpV, long timeMs) {
        Matrix4f matrix = poseStack.last().pose();

        // 1. Capa Aura Translúcida Exterior (1.5x1.5 bloques - Blanco Pulsante)
        float pulse = (float) (Math.sin(timeMs * 0.01D) * 0.15D + 0.85D);
        drawConcentricQuads(consumer, matrix, start, end, perpH, perpV, 0.75F * pulse, 1.0F, 1.0F, 1.0F, 0.25F);

        // 2. Capa Intermedia de Resplandor (1.0 bloque - Blanco Sólido)
        drawConcentricQuads(consumer, matrix, start, end, perpH, perpV, 0.50F, 0.9F, 0.9F, 1.0F, 0.55F);

        // 3. Núcleo Oscuro Brimstone (0.6 bloques - Negro Absoluto)
        drawConcentricQuads(consumer, matrix, start, end, perpH, perpV, 0.30F, 0.05F, 0.05F, 0.05F, 0.95F);

        // 4. Centro Relámpago Caliente (0.2 bloques - Blanco Puro)
        drawConcentricQuads(consumer, matrix, start, end, perpH, perpV, 0.10F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawConcentricQuads(VertexConsumer consumer, Matrix4f matrix,
                                            Vec3 start, Vec3 end, Vec3 perpH, Vec3 perpV,
                                            float halfWidth, float r, float g, float b, float a) {
        // Quad Horizontal
        drawSingleQuad(consumer, matrix, start, end, perpH, halfWidth, r, g, b, a);
        // Quad Vertical
        drawSingleQuad(consumer, matrix, start, end, perpV, halfWidth, r, g, b, a);
    }

    private static void drawSingleQuad(VertexConsumer consumer, Matrix4f matrix,
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
}
