package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;

import java.util.List;

@Mod.EventBusSubscriber(modid = Xeb.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LaserScorchRenderer {

    private static final int POINTS = 12; // 12-sided organic starburst decal polygon

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        List<LaserScorchManager.ScorchMark> marks = LaserScorchManager.getActiveMarks();
        if (marks.isEmpty()) return;

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        long now = System.currentTimeMillis();

        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (LaserScorchManager.ScorchMark mark : marks) {
            long age = now - mark.spawnTimeMs;
            if (age > 5000L) continue;

            float alphaMult = 1.0F;
            if (age > 4000L) {
                alphaMult = 1.0F - ((age - 4000L) / 1000.0F); // Smooth 1-second fade out
            }

            float r = mark.r * 0.08F;
            float g = mark.g * 0.08F;
            float b = mark.b * 0.08F;
            float coreAlpha = 0.55F * alphaMult; // Dark charcoal core

            double cx = mark.pos.x;
            double cy = mark.pos.y;
            double cz = mark.pos.z;
            float baseRadius = Math.max(0.35F, mark.radius * 1.15F);
            Direction face = mark.face != null ? mark.face : Direction.UP;

            // Generate 12 organic perimeter offsets with randomized noise variations
            double[] px = new double[POINTS];
            double[] py = new double[POINTS];

            for (int i = 0; i < POINTS; i++) {
                double angle = (i * 2.0D * Math.PI) / POINTS + mark.rotationRad;
                // Deterministic noise per vertex based on mark seed
                double noise = 0.75D + 0.50D * Math.abs(Math.sin(mark.seed + i * 2.7D));
                double rad = baseRadius * noise;
                px[i] = Math.cos(angle) * rad;
                py[i] = Math.sin(angle) * rad;
            }

            // Render 12 triangle fan slices radiating from dark core (alpha 0.55) to transparent outer edge (alpha 0.0)
            for (int i = 0; i < POINTS; i++) {
                int next = (i + 1) % POINTS;

                Vec3 c = getFacePos(cx, cy, cz, 0, 0, face);
                Vec3 p1 = getFacePos(cx, cy, cz, px[i], py[i], face);
                Vec3 p2 = getFacePos(cx, cy, cz, px[next], py[next], face);

                // Center vertex (Dark core)
                buffer.vertex(matrix, (float) c.x, (float) c.y, (float) c.z).color(r, g, b, coreAlpha).endVertex();
                // Edge vertex 1 (Fades smoothly to 0.0 alpha at boundary)
                buffer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, 0.0F).endVertex();
                // Edge vertex 2 (Fades smoothly to 0.0 alpha at boundary)
                buffer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, 0.0F).endVertex();
            }
        }

        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static Vec3 getFacePos(double cx, double cy, double cz, double u, double v, Direction face) {
        return switch (face) {
            case UP -> new Vec3(cx + u, cy + 0.005D, cz + v);
            case DOWN -> new Vec3(cx + u, cy - 0.005D, cz + v);
            case NORTH -> new Vec3(cx + u, cy + v, cz - 0.005D);
            case SOUTH -> new Vec3(cx + u, cy + v, cz + 0.005D);
            case WEST -> new Vec3(cx - 0.005D, cy + v, cz + u);
            case EAST -> new Vec3(cx + 0.005D, cy + v, cz + u);
        };
    }
}
