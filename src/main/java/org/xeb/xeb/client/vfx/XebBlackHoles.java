package org.xeb.xeb.client.vfx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reusable In-Game 3D Black Hole & Event Horizon Gravitational VFX Engine.
 *
 * <p>Usage:
 * Call {@link #spawnBlackHole(Vec3, float, int, int, int, int, int, int, int)} to render 3D event horizon spheres
 * with photonic accretion rings and light-bending dark cores.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XebBlackHoles {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);
    private static final Map<Long, ActiveBlackHoleData> ACTIVE_BLACK_HOLES = new ConcurrentHashMap<>();
    private static final int SPHERE_RINGS = 16;
    private static final int SPHERE_SEGMENTS = 24;

    public static long spawnBlackHole(Vec3 position, float radius, int cR, int cG, int cB, int rR, int rG, int rB, int durationTicks) {
        long id = ID_COUNTER.incrementAndGet();
        ACTIVE_BLACK_HOLES.put(id, new ActiveBlackHoleData(position, radius, cR, cG, cB, rR, rG, rB, System.currentTimeMillis(), durationTicks * 50L));
        return id;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_BLACK_HOLES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        long now = System.currentTimeMillis();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        for (Map.Entry<Long, ActiveBlackHoleData> entry : ACTIVE_BLACK_HOLES.entrySet()) {
            long id = entry.getKey();
            ActiveBlackHoleData bh = entry.getValue();
            long elapsed = now - bh.startTime;

            if (elapsed > bh.durationMs) {
                ACTIVE_BLACK_HOLES.remove(id);
                continue;
            }

            float progress = (float) elapsed / bh.durationMs;
            float rotAngle = (float) (((now + id * 100) % 3600) / 3600.0D * Math.PI * 2.0D);
            Vec3 pos = bh.position;
            double radius = bh.radius;

            // 1. Accretion Disk Photonic Ring
            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            double diskOuter = radius * 1.8D;
            double diskInner = radius * 0.95D;

            for (int s = 0; s <= SPHERE_SEGMENTS; s++) {
                double theta = (2.0D * Math.PI * s / SPHERE_SEGMENTS) + rotAngle;
                double cosT = Math.cos(theta);
                double sinT = Math.sin(theta);

                double xO = pos.x + cosT * diskOuter;
                double zO = pos.z + sinT * diskOuter;

                double xI = pos.x + cosT * diskInner;
                double zI = pos.z + sinT * diskInner;

                buffer.vertex(matrix, (float) xO, (float) pos.y, (float) zO).color(bh.rR, bh.rG, bh.rB, 180).endVertex();
                buffer.vertex(matrix, (float) xI, (float) pos.y, (float) zI).color(255, 255, 255, 240).endVertex();
            }
            tesselator.end();

            // 2. Dark Event Horizon Core Sphere
            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int ring = 0; ring < SPHERE_RINGS; ring++) {
                double phi1 = -Math.PI * 0.5D + (Math.PI * ring / SPHERE_RINGS);
                double phi2 = -Math.PI * 0.5D + (Math.PI * (ring + 1) / SPHERE_RINGS);

                double y1 = Math.sin(phi1) * radius;
                double r1 = Math.cos(phi1) * radius;

                double y2 = Math.sin(phi2) * radius;
                double r2 = Math.cos(phi2) * radius;

                for (int s = 0; s <= SPHERE_SEGMENTS; s++) {
                    double theta = (2.0D * Math.PI) * ((double) s / SPHERE_SEGMENTS);
                    double cosT = Math.cos(theta);
                    double sinT = Math.sin(theta);

                    double x1 = pos.x + cosT * r1;
                    double z1 = pos.z + sinT * r1;
                    double py1 = pos.y + y1;

                    double x2 = pos.x + cosT * r2;
                    double z2 = pos.z + sinT * r2;
                    double py2 = pos.y + y2;

                    buffer.vertex(matrix, (float) x1, (float) py1, (float) z1).color(bh.cR, bh.cG, bh.cB, 240).endVertex();
                    buffer.vertex(matrix, (float) x2, (float) py2, (float) z2).color(bh.cR, bh.cG, bh.cB, 240).endVertex();
                }
            }
            tesselator.end();
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private record ActiveBlackHoleData(Vec3 position, float radius, int cR, int cG, int cB, int rR, int rG, int rB, long startTime, long durationMs) {}
}
