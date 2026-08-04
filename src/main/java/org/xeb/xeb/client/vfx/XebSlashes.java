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
 * Reusable In-Game 3D Anime Katana Crescent Energy Slash VFX Engine.
 *
 * <p>Usage:
 * Call {@link #spawnSlash(Vec3, Vec3, double, double, int, int, int, int)} to render razor-sharp tapered crescent arc blades
 * with additive blending, custom orientation vectors, and white-hot cores.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XebSlashes {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);
    private static final Map<Long, ActiveSlashData> ACTIVE_SLASHES = new ConcurrentHashMap<>();
    private static final int ARC_SEGMENTS = 12;

    public static long spawnSlash(Vec3 position, Vec3 direction, double arcLength, double maxWidth, int r, int g, int b, int durationTicks) {
        long id = ID_COUNTER.incrementAndGet();
        ACTIVE_SLASHES.put(id, new ActiveSlashData(position, direction.normalize(), arcLength, maxWidth, r, g, b, System.currentTimeMillis(), durationTicks * 50L));
        return id;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_SLASHES.isEmpty()) return;

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

        for (Map.Entry<Long, ActiveSlashData> entry : ACTIVE_SLASHES.entrySet()) {
            long id = entry.getKey();
            ActiveSlashData slash = entry.getValue();
            long elapsed = now - slash.startTime;

            if (elapsed > slash.durationMs) {
                ACTIVE_SLASHES.remove(id);
                continue;
            }

            float progress = (float) elapsed / slash.durationMs;
            float alphaF = (1.0F - progress);
            int alpha = (int) (alphaF * 255);

            Vec3 dir = slash.direction;
            Vec3 u = dir.cross(new Vec3(0, 1, 0));
            if (u.lengthSqr() < 0.001D) u = dir.cross(new Vec3(1, 0, 0));
            u = u.normalize();
            Vec3 v = dir.cross(u).normalize();

            // Outer Colored Energy Aura
            renderSlashQuad(buffer, matrix, slash.position, dir, v, u, slash.arcLength, slash.maxWidth * 2.2D, slash.r, slash.g, slash.b, alpha / 2);
            // Inner White-Hot Core Blade
            renderSlashQuad(buffer, matrix, slash.position, dir, v, u, slash.arcLength, slash.maxWidth * 0.7D, 255, 255, 255, alpha);
        }

        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderSlashQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 center, Vec3 dir, Vec3 widthVec, Vec3 curveVec, double arcLen, double maxWidth, int r, int g, int b, int alpha) {
        double halfLen = arcLen * 0.5D;

        for (int seg = 0; seg < ARC_SEGMENTS; seg++) {
            double frac1 = seg / (double) ARC_SEGMENTS;
            double frac2 = (seg + 1) / (double) ARC_SEGMENTS;

            double pos1 = -halfLen + (arcLen * frac1);
            double pos2 = -halfLen + (arcLen * frac2);

            double taper1 = Math.sin(frac1 * Math.PI);
            double taper2 = Math.sin(frac2 * Math.PI);

            double w1 = maxWidth * taper1;
            double w2 = maxWidth * taper2;

            double curve1 = (1.0D - Math.cos((frac1 - 0.5D) * Math.PI)) * 0.4D;
            double curve2 = (1.0D - Math.cos((frac2 - 0.5D) * Math.PI)) * 0.4D;

            Vec3 p1 = center.add(dir.scale(pos1)).add(curveVec.scale(curve1));
            Vec3 p2 = center.add(dir.scale(pos2)).add(curveVec.scale(curve2));

            Vec3 offset1 = widthVec.scale(w1);
            Vec3 offset2 = widthVec.scale(w2);

            Vec3 vA1 = p1.add(offset1);
            Vec3 vA2 = p1.subtract(offset1);
            Vec3 vB1 = p2.add(offset2);
            Vec3 vB2 = p2.subtract(offset2);

            buffer.vertex(matrix, (float) vA1.x, (float) vA1.y, (float) vA1.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, (float) vA2.x, (float) vA2.y, (float) vA2.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, (float) vB2.x, (float) vB2.y, (float) vB2.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, (float) vB1.x, (float) vB1.y, (float) vB1.z).color(r, g, b, alpha).endVertex();
        }
    }

    private record ActiveSlashData(Vec3 position, Vec3 direction, double arcLength, double maxWidth, int r, int g, int b, long startTime, long durationMs) {}
}
