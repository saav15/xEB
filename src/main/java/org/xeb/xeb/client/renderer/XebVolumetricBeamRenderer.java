package org.xeb.xeb.client.renderer;

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

        // Layer 1: Squared Outer Halo (Soft Translucent)
        renderSquareLayer(consumer, matrix, start, end, u, v, radiusHalo * pulse,
                r * 0.45F, g * 0.45F, b * 0.45F, a * 0.25F, timeMs, 0.5F);

        // Layer 2: Squared Inner Glow (Saturated Energy)
        renderSquareLayer(consumer, matrix, start, end, u, v, radiusHalo * 0.55F * pulse,
                r * 0.85F, g * 0.85F, b * 0.85F, a * 0.65F, timeMs, 1.0F);

        // Layer 3: Squared Solid Core (White-Hot Core)
        renderSquareLayer(consumer, matrix, start, end, u, v, radiusCore * pulse,
                1.0F, 1.0F, 1.0F, a * 0.95F, timeMs, 2.0F);
    }

    private static void renderSquareLayer(VertexConsumer consumer, Matrix4f matrix,
                                           Vec3 start, Vec3 end,
                                           Vec3 u, Vec3 v, float radius,
                                           float r, float g, float b, float a,
                                           long timeMs, float speedMult) {
        // 45-degree angle offset for sharp squared diamond orientation
        double angleOffset = Math.PI / 4.0D;

        for (int i = 0; i < SIDES; i++) {
            double angle1 = (i * 2.0D * Math.PI) / SIDES + angleOffset;
            double angle2 = ((i + 1) * 2.0D * Math.PI) / SIDES + angleOffset;

            double cos1 = Math.cos(angle1) * radius;
            double sin1 = Math.sin(angle1) * radius;
            double cos2 = Math.cos(angle2) * radius;
            double sin2 = Math.sin(angle2) * radius;

            Vec3 offset1 = u.scale(cos1).add(v.scale(sin1));
            Vec3 offset2 = u.scale(cos2).add(v.scale(sin2));

            Vec3 s1 = start.add(offset1);
            Vec3 s2 = start.add(offset2);
            Vec3 e1 = end.add(offset1);
            Vec3 e2 = end.add(offset2);

            // Quad vertex order for outward normals (no black inverted faces!)
            consumer.vertex(matrix, (float) s1.x, (float) s1.y, (float) s1.z)
                    .color(r, g, b, a).endVertex();
            consumer.vertex(matrix, (float) s2.x, (float) s2.y, (float) s2.z)
                    .color(r, g, b, a).endVertex();
            consumer.vertex(matrix, (float) e2.x, (float) e2.y, (float) e2.z)
                    .color(r, g, b, a).endVertex();
            consumer.vertex(matrix, (float) e1.x, (float) e1.y, (float) e1.z)
                    .color(r, g, b, a).endVertex();
        }
    }
}
