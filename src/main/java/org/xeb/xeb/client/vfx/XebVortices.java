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
 * Reusable In-Game 3D Tornado & Plasma Vortex VFX Engine.
 *
 * <p>Usage:
 * Call {@link #spawnVortex(Vec3, float, float, int, int, int, int)} to render 3D spiraling plasma vortices/swirling tornadoes
 * ascending from basePosition with customizable colors and duration.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XebVortices {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);
    private static final Map<Long, ActiveVortexData> ACTIVE_VORTICES = new ConcurrentHashMap<>();
    private static final int RINGS = 12;
    private static final int SEGMENTS = 16;

    public static long spawnVortex(Vec3 basePosition, float height, float radius, int r, int g, int b, int durationTicks) {
        long id = ID_COUNTER.incrementAndGet();
        ACTIVE_VORTICES.put(id, new ActiveVortexData(basePosition, height, radius, r, g, b, System.currentTimeMillis(), durationTicks * 50L));
        return id;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_VORTICES.isEmpty()) return;

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

        for (Map.Entry<Long, ActiveVortexData> entry : ACTIVE_VORTICES.entrySet()) {
            long id = entry.getKey();
            ActiveVortexData vortex = entry.getValue();
            long elapsed = now - vortex.startTime;

            if (elapsed > vortex.durationMs) {
                ACTIVE_VORTICES.remove(id);
                continue;
            }

            float progress = (float) elapsed / vortex.durationMs;
            float alphaF = (1.0F - progress);
            int alpha = (int) (alphaF * 180);

            Vec3 base = vortex.basePosition;
            float rotAngle = (float) (((now + id * 80) % 2000) / 2000.0D * Math.PI * 2.0D);

            for (int ring = 0; ring < RINGS; ring++) {
                double hFraction1 = (double) ring / RINGS;
                double hFraction2 = (double) (ring + 1) / RINGS;

                double y1 = base.y + hFraction1 * vortex.height;
                double y2 = base.y + hFraction2 * vortex.height;

                double r1 = vortex.radius * (0.3D + hFraction1 * 0.7D);
                double r2 = vortex.radius * (0.3D + hFraction2 * 0.7D);

                double spiralOffset1 = rotAngle + (hFraction1 * Math.PI * 3.0D);
                double spiralOffset2 = rotAngle + (hFraction2 * Math.PI * 3.0D);

                for (int s = 0; s <= SEGMENTS; s++) {
                    double theta1 = (2.0D * Math.PI * s / SEGMENTS) + spiralOffset1;
                    double theta2 = (2.0D * Math.PI * s / SEGMENTS) + spiralOffset2;

                    double x1 = base.x + Math.cos(theta1) * r1;
                    double z1 = base.z + Math.sin(theta1) * r1;

                    double x2 = base.x + Math.cos(theta2) * r2;
                    double z2 = base.z + Math.sin(theta2) * r2;

                    buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(vortex.r, vortex.g, vortex.b, alpha).endVertex();
                    buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(255, 255, 255, alpha).endVertex();
                }
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

    private record ActiveVortexData(Vec3 basePosition, float height, float radius, int r, int g, int b, long startTime, long durationMs) {}
}
