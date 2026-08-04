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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reusable In-Game 3D Branched Lightning & Electrical Arc VFX Engine.
 *
 * <p>Usage:
 * Call {@link #spawnLightning(Vec3, Vec3, int, int, int, int, int)} to render 3D chaotic branched electrical discharges
 * between any two spatial coordinates with custom color, jitter, and duration.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XebLightning {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);
    private static final Map<Long, ActiveLightningData> ACTIVE_LIGHTNINGS = new ConcurrentHashMap<>();
    private static final int SEGMENTS = 8;

    public static long spawnLightning(Vec3 startPos, Vec3 endPos, int branches, int r, int g, int b, int durationTicks) {
        long id = ID_COUNTER.incrementAndGet();
        ACTIVE_LIGHTNINGS.put(id, new ActiveLightningData(startPos, endPos, branches, r, g, b, System.currentTimeMillis(), durationTicks * 50L));
        return id;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_LIGHTNINGS.isEmpty()) return;

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

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Map.Entry<Long, ActiveLightningData> entry : ACTIVE_LIGHTNINGS.entrySet()) {
            long id = entry.getKey();
            ActiveLightningData bolt = entry.getValue();
            long elapsed = now - bolt.startTime;

            if (elapsed > bolt.durationMs) {
                ACTIVE_LIGHTNINGS.remove(id);
                continue;
            }

            float progress = (float) elapsed / bolt.durationMs;
            float alphaF = (1.0F - progress);
            int alpha = (int) (alphaF * 255);

            Random rand = new Random(id * 3333L + (now / 50L));

            renderLightningBranch(buffer, matrix, bolt.startPos, bolt.endPos, bolt.r, bolt.g, bolt.b, alpha, rand, 0.3D);
        }

        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderLightningBranch(BufferBuilder buffer, Matrix4f matrix, Vec3 start, Vec3 end, int r, int g, int b, int alpha, Random rand, double jitterAmount) {
        Vec3 current = start;

        for (int seg = 0; seg < SEGMENTS; seg++) {
            double t = (seg + 1) / (double) SEGMENTS;
            Vec3 target = start.add(end.subtract(start).scale(t));

            if (seg < SEGMENTS - 1) {
                target = target.add(
                        (rand.nextDouble() - 0.5D) * jitterAmount,
                        (rand.nextDouble() - 0.5D) * jitterAmount,
                        (rand.nextDouble() - 0.5D) * jitterAmount
                );
            }

            double width = 0.08D;
            Vec3 dir = target.subtract(current).normalize();
            Vec3 perp = dir.cross(new Vec3(0, 1, 0)).normalize().scale(width);

            Vec3 c1 = current.add(perp);
            Vec3 c2 = current.subtract(perp);
            Vec3 t1 = target.add(perp);
            Vec3 t2 = target.subtract(perp);

            // Outer Aura Bolt
            buffer.vertex(matrix, (float) c1.x, (float) c1.y, (float) c1.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, (float) c2.x, (float) c2.y, (float) c2.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, (float) t2.x, (float) t2.y, (float) t2.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, (float) t1.x, (float) t1.y, (float) t1.z).color(r, g, b, alpha).endVertex();

            // White Core Bolt
            Vec3 cc1 = current.add(perp.scale(0.35D));
            Vec3 cc2 = current.subtract(perp.scale(0.35D));
            Vec3 tt1 = target.add(perp.scale(0.35D));
            Vec3 tt2 = target.subtract(perp.scale(0.35D));

            buffer.vertex(matrix, (float) cc1.x, (float) cc1.y, (float) cc1.z).color(255, 255, 255, alpha).endVertex();
            buffer.vertex(matrix, (float) cc2.x, (float) cc2.y, (float) cc2.z).color(255, 255, 255, alpha).endVertex();
            buffer.vertex(matrix, (float) tt2.x, (float) tt2.y, (float) tt2.z).color(255, 255, 255, alpha).endVertex();
            buffer.vertex(matrix, (float) tt1.x, (float) tt1.y, (float) tt1.z).color(255, 255, 255, alpha).endVertex();

            current = target;
        }
    }

    private record ActiveLightningData(Vec3 startPos, Vec3 endPos, int branches, int r, int g, int b, long startTime, long durationMs) {}
}
