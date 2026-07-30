package org.xeb.xeb.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = Xeb.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PermanightFloorAuraRenderer {

    // Toggle flags for easy enabling/disabling
    public static boolean ENABLE_VOID_CRACKS = false;
    public static boolean ENABLE_EXPANDING_WAVES = false;

    private static class ExpandingWave {
        Vec3 center;
        float currentRadius;
        float maxRadius;
        float alpha;

        ExpandingWave(Vec3 center) {
            this.center = center;
            this.currentRadius = 0.5F;
            this.maxRadius = 12.0F;
            this.alpha = 1.0F;
        }

        boolean update() {
            currentRadius += 0.25F;
            alpha = 1.0F - (currentRadius / maxRadius);
            return currentRadius < maxRadius;
        }
    }

    private static final List<ExpandingWave> ACTIVE_WAVES = new ArrayList<>();
    private static long lastWaveTime = 0;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!PermanightClientHandler.isPermanightActive()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        long time = mc.level.getGameTime();
        if (ENABLE_EXPANDING_WAVES && time - lastWaveTime >= 40) { // Every 2 seconds
            lastWaveTime = time;
            ACTIVE_WAVES.add(new ExpandingWave(mc.player.position()));
        }

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // 1. Render Void Cracks around player
        if (ENABLE_VOID_CRACKS) {
            renderVoidCracks(buffer, tesselator, matrix, mc.player.position(), time);
        }

        // 2. Render Expanding Shockwaves
        if (ENABLE_EXPANDING_WAVES) {
            renderExpandingWaves(buffer, tesselator, matrix, time);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderVoidCracks(BufferBuilder buffer, Tesselator tesselator, Matrix4f matrix, Vec3 pPos, long time) {
        float pulse = (float) (Math.sin(time * 0.1D) * 0.25D + 0.75D);
        float y = (float) pPos.y + 0.02F;

        int numCracks = 8;
        float radius = 7.0F;

        for (int i = 0; i < numCracks; i++) {
            float baseAngle = i * ((float) Math.PI * 2.0F / numCracks);
            float startX = (float) pPos.x + (float) Math.cos(baseAngle) * 0.8F;
            float startZ = (float) pPos.z + (float) Math.sin(baseAngle) * 0.8F;

            float endX = (float) pPos.x + (float) Math.cos(baseAngle + 0.2F) * radius;
            float endZ = (float) pPos.z + (float) Math.sin(baseAngle + 0.2F) * radius;

            // Draw glowing crack quad strip
            float width = 0.12F * pulse;

            int red = (int) (255 * pulse);
            int green = 30;
            int blue = (int) (230 * (1.0F - pulse * 0.5F));
            int alpha = (int) (180 * pulse);

            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            buffer.vertex(matrix, startX - width, y, startZ - width).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, startX + width, y, startZ + width).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, endX - width, y, endZ - width).color(red, green, blue, 0).endVertex();
            buffer.vertex(matrix, endX + width, y, endZ + width).color(red, green, blue, 0).endVertex();
            tesselator.end();
        }
    }

    private static void renderExpandingWaves(BufferBuilder buffer, Tesselator tesselator, Matrix4f matrix, long time) {
        Iterator<ExpandingWave> iterator = ACTIVE_WAVES.iterator();
        while (iterator.hasNext()) {
            ExpandingWave wave = iterator.next();
            if (!wave.update()) {
                iterator.remove();
                continue;
            }

            float y = (float) wave.center.y + 0.03F;
            float r = wave.currentRadius;
            float thickness = 0.4F;

            int alpha = (int) (220 * wave.alpha);
            if (alpha <= 0) continue;

            int segments = 40;
            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i <= segments; i++) {
                float angle = i * ((float) Math.PI * 2.0F / segments);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                float innerX = (float) wave.center.x + cos * (r - thickness);
                float innerZ = (float) wave.center.z + sin * (r - thickness);

                float outerX = (float) wave.center.x + cos * (r + thickness);
                float outerZ = (float) wave.center.z + sin * (r + thickness);

                // Alternating Red/Purple color across circumference
                int red = (i % 2 == 0) ? 255 : 153;
                int blue = (i % 2 == 0) ? 64 : 230;

                buffer.vertex(matrix, innerX, y, innerZ).color(red, 30, blue, 0).endVertex();
                buffer.vertex(matrix, outerX, y, outerZ).color(red, 30, blue, alpha).endVertex();
            }
            tesselator.end();
        }
    }
}
