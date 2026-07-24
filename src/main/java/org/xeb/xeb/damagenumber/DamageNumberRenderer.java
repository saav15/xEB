package org.xeb.xeb.damagenumber;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;

import java.util.List;
import java.util.Map;

/**
 * Renderizador Cinemático 3D de Marcadores de Daño (Estilo Sci-Fi Gamer / Anime).
 * Utiliza un sistema de triple capa Z (Sombra de Profundidad Indigo + Halo de Aura Neón + Núcleo de Alto Contraste)
 * para crear un impacto visual deslumbrante, nítido y 100% libre de bordes planos u oscuros.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DamageNumberRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        DamageNumberMode mode = DamageNumberConfig.getMode();
        if (mode == DamageNumberMode.OFF) return;

        Map<Integer, List<DamageNumberInstance>> activeNumbers = DamageNumberManager.getInstance().getActiveNumbers();
        if (activeNumbers.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;
        float partialTick = event.getPartialTick();

        for (Map.Entry<Integer, List<DamageNumberInstance>> entry : activeNumbers.entrySet()) {
            int entityId = entry.getKey();
            List<DamageNumberInstance> instances = entry.getValue();
            if (instances.isEmpty()) continue;

            Entity entity = mc.level.getEntity(entityId);
            Vec3 basePos;

            if (entity != null) {
                double x = Mth.lerp(partialTick, entity.xo, entity.getX());
                double y = Mth.lerp(partialTick, entity.yo, entity.getY());
                double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
                // Base situada al torso medio (evita tapar la corona de medallones sobre la cabeza)
                basePos = new Vec3(x, y + entity.getBbHeight() * 0.50D, z);
            } else {
                DamageNumberInstance first = instances.get(0);
                basePos = first.initialPos != null ? first.initialPos : Vec3.ZERO;
            }

            for (DamageNumberInstance inst : instances) {
                renderSingleInstance(poseStack, bufferSource, font, camera, camPos, basePos, inst, partialTick, mode);
            }
        }

        bufferSource.endBatch();
    }

    private static void renderSingleInstance(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Font font, Camera camera, Vec3 camPos, Vec3 basePos, DamageNumberInstance inst, float partialTick, DamageNumberMode mode) {
        float alpha = inst.getAlpha(partialTick);
        if (alpha <= 0.01F) return;

        // Trayectoria Confetti en 3D
        double worldX = basePos.x + inst.getAnimX(partialTick);
        double worldY = basePos.y + inst.getAnimY(partialTick);
        double worldZ = basePos.z + inst.getAnimZ(partialTick);

        double distSq = camPos.distanceToSqr(worldX, worldY, worldZ);
        if (distSq > 4096.0D) return; // Máximo 64 bloques

        float scaleAnim = inst.getScale(partialTick);
        boolean isSmallPlainHit = (mode == DamageNumberMode.HYBRID && !inst.isHybridCombined);

        double dist = Math.sqrt(distSq);
        float distFactor = (float) Math.max(1.0D, dist * 0.12D);
        float baseScale = isSmallPlainHit ? 0.018F : 0.030F;
        float renderScale = baseScale * scaleAnim * distFactor;

        poseStack.pushPose();
        poseStack.translate(worldX - camPos.x, worldY - camPos.y, worldZ - camPos.z);

        // Billboard Completo 3D orientado hacia la cámara
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-renderScale, -renderScale, renderScale);

        // Formateo del texto de daño estilizado
        String text;
        if (inst.amount == (long) inst.amount) {
            text = String.format("%d", (long) inst.amount);
        } else {
            text = String.format("%.1f", inst.amount);
        }

        if (inst.isCrit && DamageNumberConfig.critIndicatorEnabled) {
            text = "★ " + text;
        }

        // Determinar paleta de colores (Núcleo + Aura Neón)
        int coreColor;
        int auraColor;

        if (inst.isCrit) {
            coreColor = 0xFFFFD700; // Dorado resplandeciente
            auraColor = 0xFFFFA000; // Aura Ámbar neón
        } else if (inst.isHybridCombined || mode == DamageNumberMode.COMBINE) {
            coreColor = DamageNumberConfig.getColorForTotalDamage(inst.amount);
            auraColor = getAuraColorForHex(coreColor);
        } else {
            coreColor = DamageNumberConfig.getColorForSourceCategory(inst.sourceType);
            auraColor = getAuraColorForHex(coreColor);
        }

        int alphaInt = Mth.clamp((int) (alpha * 255.0F), 4, 255);
        int coreWithAlpha = (alphaInt << 24) | (coreColor & 0x00FFFFFF);

        int auraAlphaInt = Mth.clamp((int) (alpha * 160.0F), 2, 160);
        int auraWithAlpha = (auraAlphaInt << 24) | (auraColor & 0x00FFFFFF);

        int shadowAlphaInt = Mth.clamp((int) (alpha * 200.0F), 4, 200);
        int shadowColor = (shadowAlphaInt << 24) | 0x030814; // Sombra Índigo Oscuro de Alta Profundidad

        float textWidth = font.width(text);
        float x = -textWidth / 2.0F;
        float y = 0.0F;

        Matrix4f matrix = poseStack.last().pose();

        if (isSmallPlainHit) {
            // GOLPE INDIVIDUAL PEQUEÑO (HÍBRIDO): Texto limpio con sombra fina e interpenetración Z resuelta
            font.drawInBatch(text, x + 0.6F, y + 0.6F, shadowColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);

            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.015F);
            font.drawInBatch(text, x, y, coreWithAlpha, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            poseStack.popPose();
        } else {
            // NÚMEROS GRANDES / TOTALES / STACK / CRÍTICOS: Renderizado Cinemático de Triple Capa Z
            
            // 1. CAPA POSTERIOR - Sombra Índigo de Profundidad 3D (Z = 0.000)
            font.drawInBatch(text, x + 0.8F, y + 0.8F, shadowColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);

            // 2. CAPA INTERMEDIA - Resplandor de Aura Neón (Z = 0.012)
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.012F);
            Matrix4f auraMatrix = poseStack.last().pose();

            // Dibujar halo neón alrededor del carácter
            font.drawInBatch(text, x - 0.5F, y, auraWithAlpha, false, auraMatrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            font.drawInBatch(text, x + 0.5F, y, auraWithAlpha, false, auraMatrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            font.drawInBatch(text, x, y - 0.5F, auraWithAlpha, false, auraMatrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            font.drawInBatch(text, x, y + 0.5F, auraWithAlpha, false, auraMatrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            poseStack.popPose();

            // 3. CAPA FRONTAL - Núcleo de Texto Brillante de Alto Contraste (Z = 0.025)
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.025F);
            font.drawInBatch(text, x, y, coreWithAlpha, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * Genera un color de Aura Neón traslúcido coordinado según el color principal del daño.
     */
    private static int getAuraColorForHex(int hexColor) {
        int r = (hexColor >> 16) & 0xFF;
        int g = (hexColor >> 8) & 0xFF;
        int b = hexColor & 0xFF;

        // Potencia la saturación y brillo para el halo neón
        r = Math.min(255, (int) (r * 1.25F + 30));
        g = Math.min(255, (int) (g * 1.25F + 30));
        b = Math.min(255, (int) (b * 1.25F + 30));

        return (r << 16) | (g << 8) | b;
    }
}
