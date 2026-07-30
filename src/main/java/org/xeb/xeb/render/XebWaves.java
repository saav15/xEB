package org.xeb.xeb.render;

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

/**
 * Componente modular reutilizable XebWaves para ondas expansivas de piso (xebwaves).
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class XebWaves {

    public static class WaveInstance {
        public Vec3 center;
        public float currentRadius;
        public float maxRadius;
        public float expandSpeed;
        public int red;
        public int green;
        public int blue;
        public float alpha;

        public WaveInstance(Vec3 center, float maxRadius, float expandSpeed, int red, int green, int blue) {
            this.center = center;
            this.currentRadius = 0.5F;
            this.maxRadius = maxRadius;
            this.expandSpeed = expandSpeed;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = 1.0F;
        }

        public boolean update() {
            currentRadius += expandSpeed;
            alpha = 1.0F - (currentRadius / maxRadius);
            return currentRadius < maxRadius;
        }
    }

    private static final List<WaveInstance> ACTIVE_WAVES = new ArrayList<>();

    /** Invoca una nueva onda concéntrica expansiva en la posición especificada. */
    public static void spawnWave(Vec3 pos, float maxRadius, float expandSpeed, int red, int green, int blue) {
        ACTIVE_WAVES.add(new WaveInstance(pos, maxRadius, expandSpeed, red, green, blue));
    }

    public static void clearAll() {
        ACTIVE_WAVES.clear();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (ACTIVE_WAVES.isEmpty()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

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

        Iterator<WaveInstance> iterator = ACTIVE_WAVES.iterator();
        while (iterator.hasNext()) {
            WaveInstance wave = iterator.next();
            if (!wave.update()) {
                iterator.remove();
                continue;
            }

            float y = (float) wave.center.y + 0.03F;
            float r = wave.currentRadius;
            float thickness = 0.4F;

            int a = (int) (220 * wave.alpha);
            if (a <= 0) continue;

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

                buffer.vertex(matrix, innerX, y, innerZ).color(wave.red, wave.green, wave.blue, 0).endVertex();
                buffer.vertex(matrix, outerX, y, outerZ).color(wave.red, wave.green, wave.blue, a).endVertex();
            }
            tesselator.end();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }
}
