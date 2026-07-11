package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Reusable beam rendering utility.
 * Future beam renderers should use this as a base for consistent visuals.
 *
 * <p>Features:
 * <ul>
 *   <li>4-layer volumetric beam (aura, glow, core, inner hot)</li>
 *   <li>Heat haze distortion via vertex jitter</li>
 *   <li>Pulsing animation</li>
 *   <li>Particle emission points along the beam</li>
 *   <li>Impact sphere with concentric rings</li>
 * </ul>
 */
public class BeamStyle {

    public float coreR = 1.0F, coreG = 0.1F, coreB = 0.1F; // core color
    public float auraR = 1.0F, auraG = 0.0F, auraB = 0.0F; // aura color
    public float coreWidth = 0.12F;
    public float glowWidth = 0.25F;
    public float auraWidth = 0.45F;
    public float innerWidth = 0.04F;
    public boolean heatHaze = true;
    public float pulseSpeed = 0.008F;
    public float pulseAmount = 0.1F;

    /**
     * Renders a styled beam from start to end.
     */
    public void render(VertexConsumer consumer, Matrix4f matrix, Vec3 start, Vec3 end, long timeMs) {
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 0.01D) return;
        Vec3 dirN = dir.normalize();

        Vec3 perp1 = dirN.cross(new Vec3(0, 1, 0));
        if (perp1.lengthSqr() < 0.001D) perp1 = dirN.cross(new Vec3(1, 0, 0));
        perp1 = perp1.normalize();
        Vec3 perp2 = dirN.cross(perp1).normalize();

        float pulse = 1.0F + pulseAmount * (float) Math.sin(timeMs * pulseSpeed);

        // Heat haze: small random offset
        Random rand = new Random((long) (start.x * 100 + timeMs / 100));
        Vec3 hazeStart = heatHaze ? start.add(new Vec3(
                (rand.nextDouble() - 0.5) * 0.03,
                (rand.nextDouble() - 0.5) * 0.03,
                (rand.nextDouble() - 0.5) * 0.03)) : start;
        Vec3 hazeEnd = heatHaze ? end.add(new Vec3(
                (rand.nextDouble() - 0.5) * 0.03,
                (rand.nextDouble() - 0.5) * 0.03,
                (rand.nextDouble() - 0.5) * 0.03)) : end;

        // Layer 1: Aura (transparent, wide)
        drawBeamQuad(consumer, matrix, hazeStart, hazeEnd, perp1, perp2, auraWidth * pulse,
                auraR * 0.5F, auraG * 0.5F, auraB * 0.5F, 0.06F * pulse);

        // Layer 2: Mid Glow
        drawBeamQuad(consumer, matrix, start, end, perp1, perp2, glowWidth * pulse,
                auraR * 0.8F, auraG * 0.8F, auraB * 0.8F, 0.16F * pulse);

        // Layer 3: Core
        drawBeamQuad(consumer, matrix, start, end, perp1, perp2, coreWidth * pulse,
                coreR, coreG, coreB, 0.85F);

        // Layer 4: Inner hot (white)
        drawBeamQuad(consumer, matrix, start, end, perp1, perp2, innerWidth * pulse,
                1.0F, 1.0F, 1.0F, 0.95F);
    }

    /**
     * Renders an impact sphere at the given point.
     */
    public void renderImpact(VertexConsumer consumer, Matrix4f matrix, Vec3 center, long timeMs) {
        float pulse = 0.85F + 0.15F * (float) Math.sin(timeMs * 0.02);
        float size = 0.4F * pulse;

        // Outer aura
        drawCrossQuad(consumer, matrix, center, size * 2.0F, auraR * 0.6F, auraG * 0.6F, auraB * 0.6F, 0.2F * pulse);
        // Mid
        drawCrossQuad(consumer, matrix, center, size * 1.3F, auraR, auraG, auraB, 0.5F * pulse);
        // Core
        drawCrossQuad(consumer, matrix, center, size * 0.8F, coreR, coreG, coreB, 0.85F * pulse);
        // Inner white
        drawCrossQuad(consumer, matrix, center, size * 0.4F, 1.0F, 1.0F, 1.0F, 0.95F);

        // Concentric rings
        for (int i = 0; i < 3; i++) {
            float ringRadius = size * (1.5F + i * 0.8F) * (0.8F + 0.2F * (float) Math.sin(timeMs * 0.01 + i));
            drawImpactRing(consumer, matrix, center, ringRadius, auraR, auraG, auraB, 0.4F - i * 0.1F);
        }
    }

    private static void drawBeamQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 start, Vec3 end,
                                      Vec3 perp1, Vec3 perp2, float halfWidth,
                                      float r, float g, float b, float a) {
        // 4 quads forming a cylinder-like shape (rotated planes)
        Vec3 p1 = start.add(perp1.scale(halfWidth));
        Vec3 p2 = start.add(perp1.scale(-halfWidth));
        Vec3 p3 = end.add(perp1.scale(-halfWidth));
        Vec3 p4 = end.add(perp1.scale(halfWidth));
        consumer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z).color(r, g, b, a).endVertex();

        Vec3 p5 = start.add(perp2.scale(halfWidth));
        Vec3 p6 = start.add(perp2.scale(-halfWidth));
        Vec3 p7 = end.add(perp2.scale(-halfWidth));
        Vec3 p8 = end.add(perp2.scale(halfWidth));
        consumer.vertex(matrix, (float) p5.x, (float) p5.y, (float) p5.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p6.x, (float) p6.y, (float) p6.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p7.x, (float) p7.y, (float) p7.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p8.x, (float) p8.y, (float) p8.z).color(r, g, b, a).endVertex();

        // Diagonal quads for volumetric look
        Vec3 d1 = perp1.add(perp2).normalize().scale(halfWidth * 0.7F);
        Vec3 d2 = perp1.subtract(perp2).normalize().scale(halfWidth * 0.7F);

        Vec3 p9 = start.add(d1), p10 = start.add(d2);
        Vec3 p11 = end.add(d2), p12 = end.add(d1);
        consumer.vertex(matrix, (float) p9.x, (float) p9.y, (float) p9.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p10.x, (float) p10.y, (float) p10.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p11.x, (float) p11.y, (float) p11.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p12.x, (float) p12.y, (float) p12.z).color(r, g, b, a).endVertex();

        Vec3 p13 = start.add(d2), p14 = start.add(d1.scale(-1));
        Vec3 p15 = end.add(d1.scale(-1)), p16 = end.add(d2);
        consumer.vertex(matrix, (float) p13.x, (float) p13.y, (float) p13.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p14.x, (float) p14.y, (float) p14.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p15.x, (float) p15.y, (float) p15.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p16.x, (float) p16.y, (float) p16.z).color(r, g, b, a).endVertex();
    }

    private static void drawCrossQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 center, float halfWidth,
                                       float r, float g, float b, float a) {
        // XZ plane
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y, (float) center.z - halfWidth).color(r, g, b, a).endVertex();

        // XY plane
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y - halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y + halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y + halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y - halfWidth, (float) center.z).color(r, g, b, a).endVertex();

        // YZ plane
        consumer.vertex(matrix, (float) center.x, (float) center.y - halfWidth, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y + halfWidth, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y + halfWidth, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y - halfWidth, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
    }

    private static void drawImpactRing(VertexConsumer consumer, Matrix4f matrix, Vec3 center, float radius,
                                        float r, float g, float b, float a) {
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            double angle1 = (double) i / segments * 2.0 * Math.PI;
            double angle2 = (double) (i + 1) / segments * 2.0 * Math.PI;

            Vec3 p1 = center.add(Math.cos(angle1) * radius, 0, Math.sin(angle1) * radius);
            Vec3 p2 = center.add(Math.cos(angle2) * radius, 0, Math.sin(angle2) * radius);

            drawCrossQuad(consumer, matrix, p1, 0.05F, r, g, b, a);
        }
    }
}
