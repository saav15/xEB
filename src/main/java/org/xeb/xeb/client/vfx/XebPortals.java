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
 * Reusable In-Game 3D Spatial Vortex Portal VFX Engine.
 *
 * <p>Usage:
 * Call {@link #spawnPortal(Vec3, float, int, int, int, int)} from client code to render a 3D spatial portal
 * with rotating plasma rings, customizable RGB color palette, and auto-cleanup after durationTicks.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XebPortals {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);
    private static final Map<Long, ActivePortalData> ACTIVE_PORTALS = new ConcurrentHashMap<>();
    private static final int SEGMENTS = 16;

    public static long spawnPortal(Vec3 position, float radius, int r, int g, int b, int durationTicks) {
        long id = ID_COUNTER.incrementAndGet();
        ACTIVE_PORTALS.put(id, new ActivePortalData(position, radius, r, g, b, System.currentTimeMillis(), durationTicks * 50L));
        return id;
    }

    public static void removePortal(long portalId) {
        ACTIVE_PORTALS.remove(portalId);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_PORTALS.isEmpty()) return;

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

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (Map.Entry<Long, ActivePortalData> entry : ACTIVE_PORTALS.entrySet()) {
            long id = entry.getKey();
            ActivePortalData portal = entry.getValue();
            long elapsed = now - portal.startTime;

            if (elapsed > portal.durationMs) {
                ACTIVE_PORTALS.remove(id);
                continue;
            }

            float rotAngle = (float) (((now + id * 120) % 3600) / 3600.0D * Math.PI * 2.0D);
            Vec3 center = portal.position;
            double radius = portal.radius;

            // Outer Rotating Ring
            double r1Outer = radius;
            double r1Inner = radius * 0.65D;
            for (int s = 0; s <= SEGMENTS; s++) {
                double theta = (2.0D * Math.PI * s / SEGMENTS) + rotAngle;
                double cosT = Math.cos(theta);
                double sinT = Math.sin(theta);

                double xO = center.x + cosT * r1Outer;
                double zO = center.z + sinT * r1Outer;

                double xI = center.x + cosT * r1Inner;
                double zI = center.z + sinT * r1Inner;

                buffer.vertex(matrix, (float) xO, (float) center.y, (float) zO).color(portal.r, portal.g, portal.b, 140).endVertex();
                buffer.vertex(matrix, (float) xI, (float) center.y, (float) zI).color(portal.r, portal.g, portal.b, 220).endVertex();
            }

            // Inner White-Hot Core Ring
            double r2Outer = radius * 0.65D;
            double r2Inner = radius * 0.25D;
            for (int s = 0; s <= SEGMENTS; s++) {
                double theta = (2.0D * Math.PI * s / SEGMENTS) - rotAngle * 1.5D;
                double cosT = Math.cos(theta);
                double sinT = Math.sin(theta);

                double xO = center.x + cosT * r2Outer;
                double zO = center.z + sinT * r2Outer;

                double xI = center.x + cosT * r2Inner;
                double zI = center.z + sinT * r2Inner;

                buffer.vertex(matrix, (float) xO, (float) center.y, (float) zO).color(portal.r, portal.g, portal.b, 220).endVertex();
                buffer.vertex(matrix, (float) xI, (float) center.y, (float) zI).color(255, 255, 255, 255).endVertex();
            }
        }

        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private record ActivePortalData(Vec3 position, float radius, int r, int g, int b, long startTime, long durationMs) {}
}
