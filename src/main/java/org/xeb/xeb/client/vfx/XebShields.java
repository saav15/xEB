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
 * Reusable In-Game 3D Forcefield Dome & Shield VFX Engine.
 *
 * <p>Usage:
 * Call {@link #spawnShield(Vec3, float, int, int, int, int)} to render 3D spherical forcefield shields
 * with customizable colors, translucent opacity, and auto-cleanup after durationTicks.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XebShields {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);
    private static final Map<Long, ActiveShieldData> ACTIVE_SHIELDS = new ConcurrentHashMap<>();
    private static final int RINGS = 16;
    private static final int SEGMENTS = 24;

    public static long spawnShield(Vec3 position, float radius, int r, int g, int b, int durationTicks) {
        long id = ID_COUNTER.incrementAndGet();
        ACTIVE_SHIELDS.put(id, new ActiveShieldData(position, radius, r, g, b, System.currentTimeMillis(), durationTicks * 50L));
        return id;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_SHIELDS.isEmpty()) return;

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

        for (Map.Entry<Long, ActiveShieldData> entry : ACTIVE_SHIELDS.entrySet()) {
            long id = entry.getKey();
            ActiveShieldData shield = entry.getValue();
            long elapsed = now - shield.startTime;

            if (elapsed > shield.durationMs) {
                ACTIVE_SHIELDS.remove(id);
                continue;
            }

            float progress = (float) elapsed / shield.durationMs;
            float alphaF = (1.0F - progress * 0.3F);
            int alpha = (int) (alphaF * 120);

            Vec3 pos = shield.position;
            double radius = shield.radius;

            for (int ring = 0; ring < RINGS; ring++) {
                double phi1 = -Math.PI * 0.5D + (Math.PI * ring / RINGS);
                double phi2 = -Math.PI * 0.5D + (Math.PI * (ring + 1) / RINGS);

                double y1 = Math.sin(phi1) * radius;
                double r1 = Math.cos(phi1) * radius;

                double y2 = Math.sin(phi2) * radius;
                double r2 = Math.cos(phi2) * radius;

                for (int s = 0; s <= SEGMENTS; s++) {
                    double theta = (2.0D * Math.PI) * ((double) s / SEGMENTS);
                    double cosT = Math.cos(theta);
                    double sinT = Math.sin(theta);

                    double x1 = pos.x + cosT * r1;
                    double z1 = pos.z + sinT * r1;
                    double py1 = pos.y + y1;

                    double x2 = pos.x + cosT * r2;
                    double z2 = pos.z + sinT * r2;
                    double py2 = pos.y + y2;

                    buffer.vertex(matrix, (float) x1, (float) py1, (float) z1).color(shield.r, shield.g, shield.b, alpha).endVertex();
                    buffer.vertex(matrix, (float) x2, (float) py2, (float) z2).color(shield.r, shield.g, shield.b, alpha + 30).endVertex();
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

    private record ActiveShieldData(Vec3 position, float radius, int r, int g, int b, long startTime, long durationMs) {}
}
