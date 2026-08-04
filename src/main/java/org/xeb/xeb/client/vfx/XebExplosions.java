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
 * Reusable In-Game Volumetric 3D Energy Explosion VFX Engine.
 *
 * <p>Usage:
 * Call {@link #spawnExplosion(Vec3, float, float, int, int, int, int)} to render a volumetric 3D energy explosion
 * with 8-sided plasma beam pillars, dual concentric expanding shockwaves, and white-hot cores.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XebExplosions {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);
    private static final Map<Long, ActiveExplosionData> ACTIVE_EXPLOSIONS = new ConcurrentHashMap<>();
    private static final int SHOCKWAVE_SEGMENTS = 20;

    public static long spawnExplosion(Vec3 position, float radius, float pillarHeight, int r, int g, int b, int durationTicks) {
        long id = ID_COUNTER.incrementAndGet();
        ACTIVE_EXPLOSIONS.put(id, new ActiveExplosionData(position, radius, pillarHeight, r, g, b, System.currentTimeMillis(), durationTicks * 50L));
        return id;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_EXPLOSIONS.isEmpty()) return;

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

        for (Map.Entry<Long, ActiveExplosionData> entry : ACTIVE_EXPLOSIONS.entrySet()) {
            long id = entry.getKey();
            ActiveExplosionData exp = entry.getValue();
            long elapsed = now - exp.startTime;

            if (elapsed > exp.durationMs) {
                ACTIVE_EXPLOSIONS.remove(id);
                continue;
            }

            float progress = (float) elapsed / exp.durationMs;
            float alphaF = (1.0F - progress);
            int alpha = (int) (alphaF * 240);
            if (alpha <= 0) continue;

            Vec3 pos = exp.position;

            // 1. Volumetric 8-Sided Energy Cylinder Beam
            double beamRadius = exp.radius * 0.35D * (1.0D + progress * 0.4D);
            for (int s = 0; s <= 8; s++) {
                double theta = 2.0D * Math.PI * s / 8;
                double cosT = Math.cos(theta);
                double sinT = Math.sin(theta);

                double x = pos.x + cosT * beamRadius;
                double z = pos.z + sinT * beamRadius;

                buffer.vertex(matrix, (float) x, (float) pos.y, (float) z).color(exp.r, exp.g, exp.b, alpha).endVertex();
                buffer.vertex(matrix, (float) x, (float) (pos.y + exp.pillarHeight), (float) z).color(255, 255, 255, alpha).endVertex();
            }

            // 2. Dual Concentric Expanding Shockwave Rings
            double shock1 = exp.radius * (0.2D + progress * 1.8D);
            double shock2 = exp.radius * (0.1D + progress * 1.0D);

            for (int s = 0; s <= SHOCKWAVE_SEGMENTS; s++) {
                double theta = 2.0D * Math.PI * s / SHOCKWAVE_SEGMENTS;
                double cosT = Math.cos(theta);
                double sinT = Math.sin(theta);

                double x1 = pos.x + cosT * shock1;
                double z1 = pos.z + sinT * shock1;

                double x2 = pos.x + cosT * shock2;
                double z2 = pos.z + sinT * shock2;

                buffer.vertex(matrix, (float) x1, (float) (pos.y + 0.1D), (float) z1).color(exp.r, exp.g, exp.b, alpha).endVertex();
                buffer.vertex(matrix, (float) x2, (float) (pos.y + 0.1D), (float) z2).color(255, 255, 255, alpha).endVertex();
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

    private record ActiveExplosionData(Vec3 position, float radius, float pillarHeight, int r, int g, int b, long startTime, long durationMs) {}
}
