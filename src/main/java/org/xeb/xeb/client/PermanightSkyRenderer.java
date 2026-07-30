package org.xeb.xeb.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;

@Mod.EventBusSubscriber(modid = Xeb.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PermanightSkyRenderer {

    @SubscribeEvent
    public static void onRenderSky(RenderLevelStageEvent event) {
        if (!PermanightClientHandler.isPermanightActive()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // Position high in the sky (75° pitch)
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        float time = (mc.level.getGameTime() + event.getPartialTick()) * 0.03F;
        float skyDist = 95.0F;
        int segments = 36;

        // Subtle tracking shift based on player camera rotation
        float camYRot = event.getCamera().getYRot();
        float camXRot = event.getCamera().getXRot();
        float shiftX = (float) Math.sin(Math.toRadians(camYRot)) * 3.0F;
        float shiftZ = (float) Math.cos(Math.toRadians(camXRot)) * 2.5F;

        // 1. CÍRCULO OSCURO ATRÁS (Dark Backing Disk / Halo Socket)
        float bgRadius = 55.0F;
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, 0.0F, skyDist, 0.0F).color(8, 2, 14, 250).endVertex();
        for (int i = 0; i <= segments; i++) {
            float angle = i * ((float) Math.PI * 2.0F / segments);
            float vx = (float) Math.cos(angle) * bgRadius;
            float vz = (float) Math.sin(angle) * bgRadius;
            buffer.vertex(matrix, vx, skyDist, vz).color(8, 2, 14, 0).endVertex();
        }
        tesselator.end();

        // 2. ETERNAL ECLIPSE EN MEDIO CON SEGUIMIENTO LIGERO (Crimson Red Corona)
        float moonRadius = 24.0F;
        float outerCorona = moonRadius * 2.1F; // ~50.4F red aura radius

        // Halo Rojo Exterior (Aura Carmesí)
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, shiftX, skyDist + 0.1F, shiftZ).color(255, 30, 60, 220).endVertex();
        for (int i = 0; i <= segments; i++) {
            float angle = i * ((float) Math.PI * 2.0F / segments) + time;
            float pulse = (float) Math.sin(time * 2.0F + i) * 2.0F;
            float vx = shiftX + (float) Math.cos(angle) * (outerCorona + pulse);
            float vz = shiftZ + (float) Math.sin(angle) * (outerCorona + pulse);
            buffer.vertex(matrix, vx, skyDist + 0.1F, vz).color(255, 30, 60, 0).endVertex();
        }
        tesselator.end();

        // Anillo de Fuego Interior
        float innerFlame = moonRadius * 1.45F; // ~34.8F inner flame
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, shiftX, skyDist + 0.2F, shiftZ).color(255, 75, 30, 255).endVertex();
        for (int i = 0; i <= segments; i++) {
            float angle = i * ((float) Math.PI * 2.0F / segments) - time * 1.3F;
            float pulse = (float) Math.sin(time * 2.5F + i) * 1.8F;
            float vx = shiftX + (float) Math.cos(angle) * (innerFlame + pulse);
            float vz = shiftZ + (float) Math.sin(angle) * (innerFlame + pulse);
            buffer.vertex(matrix, vx, skyDist + 0.2F, vz).color(255, 75, 30, 0).endVertex();
        }
        tesselator.end();

        // Núcleo Central de Obsidiana (Obsidian Void Core)
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, shiftX, skyDist + 0.3F, shiftZ).color(10, 4, 16, 255).endVertex();
        for (int i = 0; i <= segments; i++) {
            float angle = i * ((float) Math.PI * 2.0F / segments);
            float vx = shiftX + (float) Math.cos(angle) * moonRadius;
            float vz = shiftZ + (float) Math.sin(angle) * moonRadius;
            buffer.vertex(matrix, vx, skyDist + 0.3F, vz).color(10, 4, 16, 255).endVertex();
        }
        tesselator.end();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }
}
