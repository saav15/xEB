package org.xeb.xeb.client.vfx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
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
 * Reusable In-Game 3D Entity Transformation Flame Aura VFX Engine.
 *
 * <p>Usage:
 * Call {@link #attachAura(int, float, int, int, int, int)} to attach a 3D Super Saiyan / Dragon Force style flaming plasma aura
 * around any player or entity with customizable color and duration.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XebAuras {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);
    private static final Map<Long, ActiveAuraData> ACTIVE_AURAS = new ConcurrentHashMap<>();
    private static final int SEGMENTS = 16;
    private static final int LAYERS = 8;

    public static long attachAura(int entityId, float height, int r, int g, int b, int durationTicks) {
        long id = ID_COUNTER.incrementAndGet();
        ACTIVE_AURAS.put(id, new ActiveAuraData(entityId, height, r, g, b, System.currentTimeMillis(), durationTicks * 50L));
        return id;
    }

    public static void removeAura(long auraId) {
        ACTIVE_AURAS.remove(auraId);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_AURAS.isEmpty()) return;

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

        for (Map.Entry<Long, ActiveAuraData> entry : ACTIVE_AURAS.entrySet()) {
            long id = entry.getKey();
            ActiveAuraData aura = entry.getValue();
            long elapsed = now - aura.startTime;

            if (elapsed > aura.durationMs) {
                ACTIVE_AURAS.remove(id);
                continue;
            }

            Entity entity = mc.level.getEntity(aura.entityId);
            if (entity == null || !entity.isAlive()) {
                ACTIVE_AURAS.remove(id);
                continue;
            }

            Vec3 pos = entity.position();
            float rotAngle = (float) (((now + id * 90) % 1500) / 1500.0D * Math.PI * 2.0D);

            for (int l = 0; l < LAYERS; l++) {
                double hFrac1 = (double) l / LAYERS;
                double hFrac2 = (double) (l + 1) / LAYERS;

                double y1 = pos.y + hFrac1 * aura.height;
                double y2 = pos.y + hFrac2 * aura.height;

                double r1 = 0.8D + Math.sin(hFrac1 * Math.PI + rotAngle) * 0.25D;
                double r2 = 0.8D + Math.sin(hFrac2 * Math.PI + rotAngle) * 0.25D;

                int alpha = (int) ((1.0D - hFrac1) * 160);

                for (int s = 0; s <= SEGMENTS; s++) {
                    double theta = (2.0D * Math.PI * s / SEGMENTS) + rotAngle;
                    double cosT = Math.cos(theta);
                    double sinT = Math.sin(theta);

                    double x1 = pos.x + cosT * r1;
                    double z1 = pos.z + sinT * r1;

                    double x2 = pos.x + cosT * r2;
                    double z2 = pos.z + sinT * r2;

                    buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(aura.r, aura.g, aura.b, alpha).endVertex();
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

    private record ActiveAuraData(int entityId, float height, int r, int g, int b, long startTime, long durationMs) {}
}
