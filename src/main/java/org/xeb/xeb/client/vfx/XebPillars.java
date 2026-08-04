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
 * Reusable In-Game Volumetric 3D Energy Pillar / Orbital Laser Beam VFX Engine.
 *
 * <p>Usage:
 * Call {@link #spawnPillar(Vec3, float, float, int, int, int, int)} to render 3D cylindrical plasma beams/orbital strikes
 * connecting sky to ground with customizable color, radius, and duration.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XebPillars {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);
    private static final Map<Long, ActivePillarData> ACTIVE_PILLARS = new ConcurrentHashMap<>();
    private static final int SIDES = 12;

    public static long spawnPillar(Vec3 basePosition, float height, float radius, int r, int g, int b, int durationTicks) {
        long id = ID_COUNTER.incrementAndGet();
        ACTIVE_PILLARS.put(id, new ActivePillarData(basePosition, height, radius, r, g, b, System.currentTimeMillis(), durationTicks * 50L));
        return id;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_PILLARS.isEmpty()) return;

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

        for (Map.Entry<Long, ActivePillarData> entry : ACTIVE_PILLARS.entrySet()) {
            long id = entry.getKey();
            ActivePillarData pillar = entry.getValue();
            long elapsed = now - pillar.startTime;

            if (elapsed > pillar.durationMs) {
                ACTIVE_PILLARS.remove(id);
                continue;
            }

            float progress = (float) elapsed / pillar.durationMs;
            float alphaF = (1.0F - progress);
            int alpha = (int) (alphaF * 220);

            Vec3 base = pillar.basePosition;
            double radius = pillar.radius;
            double height = pillar.height;

            // Outer Plasma Wall
            for (int s = 0; s <= SIDES; s++) {
                double theta = 2.0D * Math.PI * s / SIDES;
                double cosT = Math.cos(theta);
                double sinT = Math.sin(theta);

                double x = base.x + cosT * radius;
                double z = base.z + sinT * radius;

                buffer.vertex(matrix, (float) x, (float) base.y, (float) z).color(pillar.r, pillar.g, pillar.b, alpha).endVertex();
                buffer.vertex(matrix, (float) x, (float) (base.y + height), (float) z).color(pillar.r, pillar.g, pillar.b, alpha).endVertex();
            }

            // Inner Core Plasma Wall
            double innerR = radius * 0.4D;
            for (int s = 0; s <= SIDES; s++) {
                double theta = 2.0D * Math.PI * s / SIDES;
                double cosT = Math.cos(theta);
                double sinT = Math.sin(theta);

                double x = base.x + cosT * innerR;
                double z = base.z + sinT * innerR;

                buffer.vertex(matrix, (float) x, (float) base.y, (float) z).color(255, 255, 255, alpha).endVertex();
                buffer.vertex(matrix, (float) x, (float) (base.y + height), (float) z).color(255, 255, 255, alpha).endVertex();
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

    private record ActivePillarData(Vec3 basePosition, float height, float radius, int r, int g, int b, long startTime, long durationMs) {}
}
