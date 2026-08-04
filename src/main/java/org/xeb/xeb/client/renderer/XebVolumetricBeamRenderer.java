package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class XebVolumetricBeamRenderer {

    private static final int SIDES = 4; // Square 3D energy beam prism (4-sided 3D mesh)

    public static void render3DBeam(PoseStack poseStack, MultiBufferSource bufferSource,
                                   Vec3 start, Vec3 end,
                                   float r, float g, float b, float a,
                                   float radiusCore, float radiusHalo,
                                   long timeMs) {
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 0.05D) return;

        Vec3 dirN = dir.normalize();

        // Calculate orthonormal basis (u, v) perpendicular to dirN
        Vec3 u = dirN.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 0.001D) {
            u = dirN.cross(new Vec3(1, 0, 0));
        }
        u = u.normalize();
        Vec3 v = dirN.cross(u).normalize();

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        float pulse = 0.92F + 0.08F * (float) Math.sin(timeMs * 0.015D);

        RenderSystem.disableCull();

        // Layer 1: Squared Outer Halo (Soft Translucent)
        renderSquareLayer(consumer, matrix, start, end, dirN, length, u, v, radiusHalo * pulse,
                r * 0.45F, g * 0.45F, b * 0.45F, a * 0.25F);

        // Layer 2: Squared Inner Glow (Saturated Energy)
        renderSquareLayer(consumer, matrix, start, end, dirN, length, u, v, radiusHalo * 0.55F * pulse,
                r * 0.85F, g * 0.85F, b * 0.85F, a * 0.65F);

        // Layer 3: Squared Solid Core (White-Hot Core)
        renderSquareLayer(consumer, matrix, start, end, dirN, length, u, v, radiusCore * pulse,
                1.0F, 1.0F, 1.0F, a * 0.95F);

        RenderSystem.enableCull();
    }

    public static void render3DTvStaticBeam(PoseStack poseStack, MultiBufferSource bufferSource,
                                            Vec3 start, Vec3 end,
                                            float r, float g, float b, float a,
                                            float radiusCore, float radiusHalo,
                                            long timeMs) {
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 0.05D) return;

        Vec3 dirN = dir.normalize();

        Vec3 u = dirN.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 0.001D) {
            u = dirN.cross(new Vec3(1, 0, 0));
        }
        u = u.normalize();
        Vec3 v = dirN.cross(u).normalize();

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        float pulse = 0.92F + 0.08F * (float) Math.sin(timeMs * 0.015D);

        RenderSystem.disableCull();

        // 1. Capa 1: Aura Externa (Estática de TV Monocromática Suave y Transparente, Animación Muy Lenta)
        renderSquareTvStaticLayer(consumer, matrix, start, end, dirN, length, u, v, radiusHalo * pulse, timeMs, a * 0.18F, 140);

        // 2. Capa 2: Resplandor Medio (Estática de TV Monocromática Media)
        renderSquareTvStaticLayer(consumer, matrix, start, end, dirN, length, u, v, radiusHalo * 0.55F * pulse, timeMs, a * 0.45F, 100);

        // 3. Capa 3: Núcleo Interno Opaco con Estática de TV Sutil (Dejado exactamente como estaba)
        renderSquareTvStaticCoreLayer(consumer, matrix, start, end, dirN, length, u, v, radiusCore * pulse, timeMs, a * 0.95F);

        RenderSystem.enableCull();
    }

    private static void renderSquareTvStaticLayer(VertexConsumer consumer, Matrix4f matrix,
                                                  Vec3 start, Vec3 end, Vec3 dirN, double length,
                                                  Vec3 u, Vec3 v, float radius,
                                                  long timeMs, float alpha, long intervalMs) {
        double angleOffset = Math.PI / 4.0D;

        double taperLength = Math.min(1.0D, length * 0.25D);
        Vec3 taperEndPos = start.add(dirN.scale(taperLength));
        float originScale = 0.05F;

        // Semilla de animación muy sutil y lenta para el aura externa/media
        long frameSeed = timeMs / intervalMs;

        int bodySegments = Math.max(4, (int) ((length - taperLength) * 2.0D));
        double bodySegLen = (length - taperLength) / bodySegments;

        for (int i = 0; i < SIDES; i++) {
            double angle1 = (i * 2.0D * Math.PI) / SIDES + angleOffset;
            double angle2 = ((i + 1) * 2.0D * Math.PI) / SIDES + angleOffset;

            double cos1 = Math.cos(angle1) * radius;
            double sin1 = Math.sin(angle1) * radius;
            double cos2 = Math.cos(angle2) * radius;
            double sin2 = Math.sin(angle2) * radius;

            Vec3 offset1 = u.scale(cos1).add(v.scale(sin1));
            Vec3 offset2 = u.scale(cos2).add(v.scale(sin2));

            Vec3 s1 = start.add(offset1.scale(originScale));
            Vec3 s2 = start.add(offset2.scale(originScale));
            Vec3 m1 = taperEndPos.add(offset1);
            Vec3 m2 = taperEndPos.add(offset2);

            draw3DQuad(consumer, matrix, s1, s2, m2, m1, 0.50F, 0.50F, 0.52F, alpha);

            if (length > taperLength) {
                for (int seg = 0; seg < bodySegments; seg++) {
                    double dA = seg * bodySegLen;
                    double dB = (seg + 1) * bodySegLen;

                    Vec3 posA1 = taperEndPos.add(dirN.scale(dA)).add(offset1);
                    Vec3 posA2 = taperEndPos.add(dirN.scale(dA)).add(offset2);
                    Vec3 posB1 = taperEndPos.add(dirN.scale(dB)).add(offset1);
                    Vec3 posB2 = taperEndPos.add(dirN.scale(dB)).add(offset2);

                    java.util.Random randSeg = new java.util.Random(frameSeed + (seg * 31L) + (i * 97L));
                    float[] rgb = getAuraTvStaticColor(randSeg);

                    draw3DQuad(consumer, matrix, posA1, posA2, posB2, posB1, rgb[0], rgb[1], rgb[2], alpha);
                }
            }
        }
    }

    private static float[] getAuraTvStaticColor(java.util.Random rand) {
        int choice = rand.nextInt(5);
        switch (choice) {
            case 0: return new float[]{0.65F, 0.65F, 0.68F}; // Gris Plata Suave
            case 1: return new float[]{0.35F, 0.35F, 0.38F}; // Gris Ceniza
            case 2: return new float[]{0.50F, 0.50F, 0.52F}; // Gris Medio
            case 3: return new float[]{0.20F, 0.20F, 0.22F}; // Gris Oscuro
            default: return new float[]{0.55F, 0.55F, 0.58F}; // Gris Neutro
        }
    }

    private static void renderSquareTvStaticCoreLayer(VertexConsumer consumer, Matrix4f matrix,
                                                       Vec3 start, Vec3 end, Vec3 dirN, double length,
                                                       Vec3 u, Vec3 v, float radius,
                                                       long timeMs, float alpha) {
        double angleOffset = Math.PI / 4.0D;

        double taperLength = Math.min(1.0D, length * 0.25D);
        Vec3 taperEndPos = start.add(dirN.scale(taperLength));
        float originScale = 0.05F;

        // Semilla de animación sutil a 12 FPS (cada 80ms)
        long frameSeed = timeMs / 80;

        int bodySegments = Math.max(4, (int) ((length - taperLength) * 2.0D));
        double bodySegLen = (length - taperLength) / bodySegments;

        for (int i = 0; i < SIDES; i++) {
            double angle1 = (i * 2.0D * Math.PI) / SIDES + angleOffset;
            double angle2 = ((i + 1) * 2.0D * Math.PI) / SIDES + angleOffset;

            double cos1 = Math.cos(angle1) * radius;
            double sin1 = Math.sin(angle1) * radius;
            double cos2 = Math.cos(angle2) * radius;
            double sin2 = Math.sin(angle2) * radius;

            Vec3 offset1 = u.scale(cos1).add(v.scale(sin1));
            Vec3 offset2 = u.scale(cos2).add(v.scale(sin2));

            // Vértices de Origen Cónico 3D
            Vec3 s1 = start.add(offset1.scale(originScale));
            Vec3 s2 = start.add(offset2.scale(originScale));
            Vec3 m1 = taperEndPos.add(offset1);
            Vec3 m2 = taperEndPos.add(offset2);

            // Origen cónico con tono blanco resplandeciente de núcleo
            draw3DQuad(consumer, matrix, s1, s2, m2, m1, 0.85F, 0.85F, 0.85F, alpha);

            // Cuerpo Principal 3D con Núcleo Opaco y Ruido Sutil de TV
            if (length > taperLength) {
                for (int seg = 0; seg < bodySegments; seg++) {
                    double dA = seg * bodySegLen;
                    double dB = (seg + 1) * bodySegLen;

                    Vec3 posA1 = taperEndPos.add(dirN.scale(dA)).add(offset1);
                    Vec3 posA2 = taperEndPos.add(dirN.scale(dA)).add(offset2);
                    Vec3 posB1 = taperEndPos.add(dirN.scale(dB)).add(offset1);
                    Vec3 posB2 = taperEndPos.add(dirN.scale(dB)).add(offset2);

                    java.util.Random randSeg = new java.util.Random(frameSeed + (seg * 41L) + (i * 107L));
                    float[] rgb = getSubtleTvStaticColor(randSeg);

                    draw3DQuad(consumer, matrix, posA1, posA2, posB2, posB1, rgb[0], rgb[1], rgb[2], alpha);
                }
            }
        }
    }

    private static float[] getSubtleTvStaticColor(java.util.Random rand) {
        int choice = rand.nextInt(6);
        switch (choice) {
            case 0: return new float[]{0.90F, 0.90F, 0.90F}; // Gris Plata Claro (Estática sutil)
            case 1: return new float[]{0.40F, 0.40F, 0.45F}; // Gris Ceniza Opaco
            case 2: return new float[]{0.65F, 0.12F, 0.12F}; // Rojo Azufre de Núcleo
            case 3: return new float[]{0.25F, 0.25F, 0.28F}; // Gris Oscuro Opaco
            default: return new float[]{0.80F, 0.80F, 0.80F}; // Núcleo Resplandeciente
        }
    }

    private static void draw3DQuad(VertexConsumer consumer, Matrix4f matrix,
                                   Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                                   float r, float g, float b, float a) {
        // Cara frontal (3D)
        consumer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z).color(r, g, b, a).endVertex();

        // Cara trasera (Doble cara para visibilidad 360°)
        consumer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, a).endVertex();
    }

    private static void renderSquareLayer(VertexConsumer consumer, Matrix4f matrix,
                                           Vec3 start, Vec3 end, Vec3 dirN, double length,
                                           Vec3 u, Vec3 v, float radius,
                                           float r, float g, float b, float a) {
        // 45-degree angle offset for sharp squared diamond orientation
        double angleOffset = Math.PI / 4.0D;

        // Kamehameha Tapering: Origin starts at 5% size at hands/eyes and expands over 1.0 block
        double taperLength = Math.min(1.0D, length * 0.25D);
        Vec3 taperEndPos = start.add(dirN.scale(taperLength));
        float originScale = 0.05F;

        for (int i = 0; i < SIDES; i++) {
            double angle1 = (i * 2.0D * Math.PI) / SIDES + angleOffset;
            double angle2 = ((i + 1) * 2.0D * Math.PI) / SIDES + angleOffset;

            double cos1 = Math.cos(angle1) * radius;
            double sin1 = Math.sin(angle1) * radius;
            double cos2 = Math.cos(angle2) * radius;
            double sin2 = Math.sin(angle2) * radius;

            Vec3 offset1 = u.scale(cos1).add(v.scale(sin1));
            Vec3 offset2 = u.scale(cos2).add(v.scale(sin2));

            // Origin offsets (tapered to near-zero at start)
            Vec3 s1 = start.add(offset1.scale(originScale));
            Vec3 s2 = start.add(offset2.scale(originScale));

            // Mid offsets (full radius at taper end)
            Vec3 m1 = taperEndPos.add(offset1);
            Vec3 m2 = taperEndPos.add(offset2);

            // End offsets (full radius at beam end)
            Vec3 e1 = end.add(offset1);
            Vec3 e2 = end.add(offset2);

            // Segment 1: Origin Kamehameha Bulb Nose Cone (Double-sided for 360° visibility)
            // Front face
            consumer.vertex(matrix, (float) s1.x, (float) s1.y, (float) s1.z).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, (float) s2.x, (float) s2.y, (float) s2.z).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, (float) m2.x, (float) m2.y, (float) m2.z).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, (float) m1.x, (float) m1.y, (float) m1.z).color(r, g, b, a).endVertex();
            // Back face
            consumer.vertex(matrix, (float) m1.x, (float) m1.y, (float) m1.z).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, (float) m2.x, (float) m2.y, (float) m2.z).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, (float) s2.x, (float) s2.y, (float) s2.z).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, (float) s1.x, (float) s1.y, (float) s1.z).color(r, g, b, a).endVertex();

            // Segment 2: Main Beam Body (Double-sided for 360° visibility)
            if (length > taperLength) {
                // Front face
                consumer.vertex(matrix, (float) m1.x, (float) m1.y, (float) m1.z).color(r, g, b, a).endVertex();
                consumer.vertex(matrix, (float) m2.x, (float) m2.y, (float) m2.z).color(r, g, b, a).endVertex();
                consumer.vertex(matrix, (float) e2.x, (float) e2.y, (float) e2.z).color(r, g, b, a).endVertex();
                consumer.vertex(matrix, (float) e1.x, (float) e1.y, (float) e1.z).color(r, g, b, a).endVertex();
                // Back face
                consumer.vertex(matrix, (float) e1.x, (float) e1.y, (float) e1.z).color(r, g, b, a).endVertex();
                consumer.vertex(matrix, (float) e2.x, (float) e2.y, (float) e2.z).color(r, g, b, a).endVertex();
                consumer.vertex(matrix, (float) m2.x, (float) m2.y, (float) m2.z).color(r, g, b, a).endVertex();
                consumer.vertex(matrix, (float) m1.x, (float) m1.y, (float) m1.z).color(r, g, b, a).endVertex();
            }
        }
    }
}
