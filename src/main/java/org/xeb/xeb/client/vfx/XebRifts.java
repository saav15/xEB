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
 * Reusable In-Game 3D Floor Fissure & Spatial Rift VFX Engine.
 *
 * <p>Usage:
 * Call {@link #spawnFloorFissure(Vec3, Vec3, float, int, int, int, int)} to render glowing spatial floor rifts/fissures
 * along terrain blocks with customizable colors, width, and duration.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XebRifts {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);
    private static final Map<Long, ActiveRiftData> ACTIVE_RIFTS = new ConcurrentHashMap<>();
    private static final int FISSURE_SEGMENTS = 10;

    public static long spawnFloorFissure(Vec3 startPos, Vec3 endPos, float width, int r, int g, int b, int durationTicks) {
        long id = ID_COUNTER.incrementAndGet();
        ACTIVE_RIFTS.put(id, new ActiveRiftData(startPos, endPos, width, r, g, b, System.currentTimeMillis(), durationTicks * 50L));
        return id;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_RIFTS.isEmpty()) return;

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

        for (Map.Entry<Long, ActiveRiftData> entry : ACTIVE_RIFTS.entrySet()) {
            long id = entry.getKey();
            ActiveRiftData rift = entry.getValue();
            long elapsed = now - rift.startTime;

            if (elapsed > rift.durationMs) {
                ACTIVE_RIFTS.remove(id);
                continue;
            }

            float progress = (float) elapsed / rift.durationMs;
            float alphaF = (1.0F - progress);
            int alpha = (int) (alphaF * 230);

            Vec3 s = rift.startPos;
            Vec3 e = rift.endPos;
            Vec3 dir = e.subtract(s).normalize();
            Vec3 perp = dir.cross(new Vec3(0, 1, 0)).normalize().scale(rift.width * 0.5D);

            for (int seg = 0; seg < FISSURE_SEGMENTS; seg++) {
                double t1 = seg / (double) FISSURE_SEGMENTS;
                double t2 = (seg + 1) / (double) FISSURE_SEGMENTS;

                Vec3 p1 = s.add(e.subtract(s).scale(t1));
                Vec3 p2 = s.add(e.subtract(s).scale(t2));

                Vec3 p1L = p1.add(perp);
                Vec3 p1R = p1.subtract(perp);
                Vec3 p2L = p2.add(perp);
                Vec3 p2R = p2.subtract(perp);

                // Outer Glowing Floor Fissure
                buffer.vertex(matrix, (float) p1L.x, (float) (p1L.y + 0.05), (float) p1L.z).color(rift.r, rift.g, rift.b, alpha).endVertex();
                buffer.vertex(matrix, (float) p1R.x, (float) (p1R.y + 0.05), (float) p1R.z).color(rift.r, rift.g, rift.b, alpha).endVertex();
                buffer.vertex(matrix, (float) p2R.x, (float) (p2R.y + 0.05), (float) p2R.z).color(rift.r, rift.g, rift.b, alpha).endVertex();
                buffer.vertex(matrix, (float) p2L.x, (float) (p2L.y + 0.05), (float) p2L.z).color(rift.r, rift.g, rift.b, alpha).endVertex();

                // Core White Plasma Crack
                Vec3 c1L = p1.add(perp.scale(0.3D));
                Vec3 c1R = p1.subtract(perp.scale(0.3D));
                Vec3 c2L = p2.add(perp.scale(0.3D));
                Vec3 c2R = p2.subtract(perp.scale(0.3D));

                buffer.vertex(matrix, (float) c1L.x, (float) (c1L.y + 0.06), (float) c1L.z).color(255, 255, 255, alpha).endVertex();
                buffer.vertex(matrix, (float) c1R.x, (float) (c1R.y + 0.06), (float) c1R.z).color(255, 255, 255, alpha).endVertex();
                buffer.vertex(matrix, (float) c2R.x, (float) (c2R.y + 0.06), (float) c2R.z).color(255, 255, 255, alpha).endVertex();
                buffer.vertex(matrix, (float) c2L.x, (float) (c2L.y + 0.06), (float) c2L.z).color(255, 255, 255, alpha).endVertex();
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

    private record ActiveRiftData(Vec3 startPos, Vec3 endPos, float width, int r, int g, int b, long startTime, long durationMs) {}
}
