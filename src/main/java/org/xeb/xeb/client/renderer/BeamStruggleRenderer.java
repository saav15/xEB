package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.network.BeamStruggleEndPacket;
import org.xeb.xeb.network.BeamStrugglePacket;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side renderer for the 3D Beam Struggle effects.
 * Displays swirling plasma, connector lightning arcs, and expanding shockwave rings.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BeamStruggleRenderer {

    public static final Map<UUID, ClientStruggleData> ACTIVE_STRUGGLES = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> LOCAL_MASH_TIMES = new ConcurrentHashMap<>();

    public static class ClientStruggleData {
        public final UUID struggleId;
        public final int ownerAEntityId;
        public final int ownerBEntityId;
        public Vec3 startA;
        public Vec3 startB;
        public Vec3 collisionPoint;
        public float pointsA;
        public float pointsB;
        public int ticksElapsed;
        public final long creationTime;
        public long lastUpdateTime;
        public byte phase; // 0=PREP, 1=ACTIVE, 2=RESOLVED
        public double initialDistance;

        // Rhythm fields on client
        public int rhythmCycleTick;
        public int lastTimingA;
        public int lastTimingB;
        public int lastTimingDisplayTicksA;
        public int lastTimingDisplayTicksB;

        public ClientStruggleData(BeamStrugglePacket msg) {
            this.struggleId = msg.getStruggleId();
            this.ownerAEntityId = msg.getOwnerAEntityId();
            this.ownerBEntityId = msg.getOwnerBEntityId();
            this.startA = msg.getStartPosA();
            this.startB = msg.getStartPosB();
            this.collisionPoint = msg.getCollisionPoint();
            this.pointsA = msg.getPointsA();
            this.pointsB = msg.getPointsB();
            this.ticksElapsed = msg.getTicksElapsed();
            this.creationTime = System.currentTimeMillis();
            this.lastUpdateTime = System.currentTimeMillis();
            this.phase = msg.getPhase();
            this.initialDistance = msg.getInitialDistance();
            
            this.rhythmCycleTick = msg.getRhythmCycleTick();
            this.lastTimingA = msg.getLastTimingA();
            this.lastTimingB = msg.getLastTimingB();
            this.lastTimingDisplayTicksA = msg.getLastTimingDisplayTicksA();
            this.lastTimingDisplayTicksB = msg.getLastTimingDisplayTicksB();
        }

        public void update(BeamStrugglePacket msg) {
            this.startA = msg.getStartPosA();
            this.startB = msg.getStartPosB();
            this.collisionPoint = msg.getCollisionPoint();
            this.pointsA = msg.getPointsA();
            this.pointsB = msg.getPointsB();
            this.ticksElapsed = msg.getTicksElapsed();
            this.lastUpdateTime = System.currentTimeMillis();
            this.phase = msg.getPhase();
            this.initialDistance = msg.getInitialDistance();
            
            this.rhythmCycleTick = msg.getRhythmCycleTick();
            this.lastTimingA = msg.getLastTimingA();
            this.lastTimingB = msg.getLastTimingB();
            this.lastTimingDisplayTicksA = msg.getLastTimingDisplayTicksA();
            this.lastTimingDisplayTicksB = msg.getLastTimingDisplayTicksB();
        }
    }

    public static void handleStrugglePacket(BeamStrugglePacket msg) {
        if (msg.isStart()) {
            ACTIVE_STRUGGLES.put(msg.getStruggleId(), new ClientStruggleData(msg));
        } else {
            ClientStruggleData s = ACTIVE_STRUGGLES.get(msg.getStruggleId());
            if (s != null) {
                s.update(msg);
            } else {
                ACTIVE_STRUGGLES.put(msg.getStruggleId(), new ClientStruggleData(msg));
            }
        }
    }

    public static void handleStruggleEnd(BeamStruggleEndPacket msg) {
        UUID toRemove = null;
        for (Map.Entry<UUID, ClientStruggleData> entry : ACTIVE_STRUGGLES.entrySet()) {
            ClientStruggleData s = entry.getValue();
            if ((s.ownerAEntityId == msg.getWinnerEntityId() && s.ownerBEntityId == msg.getLoserEntityId()) ||
                (s.ownerAEntityId == msg.getLoserEntityId() && s.ownerBEntityId == msg.getWinnerEntityId())) {
                toRemove = entry.getKey();
                break;
            }
        }
        if (toRemove != null) {
            ACTIVE_STRUGGLES.remove(toRemove);
        }
    }

    public static void recordLocalMash(int entityId) {
        LOCAL_MASH_TIMES.put(entityId, System.currentTimeMillis());
    }

    public static float getLocalMashProgress(int entityId) {
        Long time = LOCAL_MASH_TIMES.get(entityId);
        if (time == null) return 0.0F;
        long diff = System.currentTimeMillis() - time;
        if (diff > 250) return 0.0F;
        return 1.0F - (diff / 250.0F); // decays over 250ms
    }

    public static ClientStruggleData getLocalPlayerStruggle() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        int localId = mc.player.getId();
        for (ClientStruggleData s : ACTIVE_STRUGGLES.values()) {
            if (s.ownerAEntityId == localId || s.ownerBEntityId == localId) {
                return s;
            }
        }
        return null;
    }

    public static boolean isOwnerInStruggle(int entityId) {
        for (ClientStruggleData s : ACTIVE_STRUGGLES.values()) {
            if (s.ownerAEntityId == entityId || s.ownerBEntityId == entityId) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_STRUGGLES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        long now = System.currentTimeMillis();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = poseStack.last().pose();

        for (Map.Entry<UUID, ClientStruggleData> entry : ACTIVE_STRUGGLES.entrySet()) {
            ClientStruggleData struggle = entry.getValue();
            if (now - struggle.lastUpdateTime > 1000) {
                ACTIVE_STRUGGLES.remove(entry.getKey());
                continue;
            }

            // Dynamic clash midpoint calculation based on live score ratio (Points A vs Points B)
            float totalPoints = struggle.pointsA + struggle.pointsB;
            float ratioA = totalPoints > 0.001F ? (struggle.pointsA / totalPoints) : 0.5F;
            ratioA = Math.max(0.08F, Math.min(0.92F, ratioA)); // Keep clash within 8% to 92% range

            Vec3 dirAB = struggle.startB.subtract(struggle.startA);
            Vec3 collision = struggle.startA.add(dirAB.scale(ratioA));
            boolean isPrep = struggle.phase == 0; // 0 = PREP

            // Determine beam colors
            float[] colorA = getOwnerColor(struggle.ownerAEntityId, mc);
            float[] colorB = getOwnerColor(struggle.ownerBEntityId, mc);

            // 1. Draw 3D Volumetric Beam A from startA -> dynamic clash point
            XebVolumetricBeamRenderer.render3DBeam(poseStack, bufferSource, struggle.startA, collision,
                    colorA[0], colorA[1], colorA[2], 0.95F, 0.28F, 0.70F, now);

            // 2. Draw 3D Volumetric Beam B from startB -> dynamic clash point
            XebVolumetricBeamRenderer.render3DBeam(poseStack, bufferSource, struggle.startB, collision,
                    colorB[0], colorB[1], colorB[2], 0.95F, 0.28F, 0.70F, now);

            // 3. Fusion Sphere at dynamic clash point
            float totalMash = struggle.pointsA + struggle.pointsB;
            float sphereSize = 0.6F + Math.min(1.5F, totalMash * 0.05F);
            if (isPrep) sphereSize = 0.8F;
            renderFusionSphere(consumer, matrix, collision, colorA, colorB, sphereSize, now, isPrep);

            // 4. Swirling Dual Color Rings
            renderDualColorRings(consumer, matrix, collision, colorA, colorB, now);

            // 5. Lightning Spark connectors
            renderSparks(consumer, matrix, collision, struggle.startA, struggle.startB, now);

            // 6. Expanding Shockwave Rings
            renderEnergyRings(consumer, matrix, collision, now, struggle.ticksElapsed);
        }

        bufferSource.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    private static float[] getOwnerColor(int entityId, Minecraft mc) {
        if (mc.level != null && mc.level.getEntity(entityId) != null) {
            net.minecraft.world.entity.Entity ent = mc.level.getEntity(entityId);
            String typeName = net.minecraft.world.entity.EntityType.getKey(ent.getType()).toString();
            if (typeName.contains("tremorzilla")) return new float[]{0.25F, 1.0F, 0.5F}; // Green
            if (typeName.contains("harbinger") || typeName.contains("leviathan")) return new float[]{0.5F, 0.2F, 1.0F}; // Purple
            
            // Check active item / Stand type color
            if (ent instanceof net.minecraft.world.entity.player.Player player) {
                // If holding Broken Diamond (Crazy Diamond), Stand colors or similar.
                // But Optic Blast default is Red.
                // Brimstone checks:
                net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
                if (stack.is(org.xeb.xeb.item.ModItems.THE_TEARS.get())) {
                    int imbue = stack.getOrCreateTag().getInt("xebTearsImbueType");
                    if (imbue == 1) return new float[]{0.7F, 0.0F, 1.0F}; // purple
                    else if (imbue == 2) return new float[]{1.0F, 1.0F, 1.0F}; // white
                    else if (imbue == 3) return new float[]{0.15F, 0.15F, 0.15F}; // dark
                    else if (imbue == 4) return new float[]{0.56F, 0.88F, 1.0F}; // cold (cyan)
                }
            }
        }
        return new float[]{1.0F, 0.1F, 0.1F}; // Default Red
    }

    private static void renderFusionBeam(VertexConsumer consumer, Matrix4f matrix,
                                          Vec3 start, Vec3 end, float[] color, long timeMs, boolean isPrep) {
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 0.1D) return;
        Vec3 dirN = dir.normalize();
        Vec3 perp1 = dirN.cross(new Vec3(0, 1, 0));
        if (perp1.lengthSqr() < 0.001D) perp1 = dirN.cross(new Vec3(1, 0, 0));
        perp1 = perp1.normalize();
        Vec3 perp2 = dirN.cross(perp1).normalize();

        float pulse = 0.9F + 0.1F * (float) Math.sin(timeMs * 0.015D);
        if (isPrep) pulse = 0.7F + 0.3F * (float) Math.sin(timeMs * 0.02D);

        float r = color[0], g = color[1], b = color[2];
        drawBeamLayer(consumer, matrix, start, end, perp1, perp2, 0.45F * pulse, r * 0.5F, g * 0.5F, b * 0.5F, 0.15F);
        drawBeamLayer(consumer, matrix, start, end, perp1, perp2, 0.25F * pulse, r * 0.8F, g * 0.8F, b * 0.8F, 0.5F);
        drawBeamLayer(consumer, matrix, start, end, perp1, perp2, 0.12F * pulse, r, g, b, 0.95F);
        drawBeamLayer(consumer, matrix, start, end, perp1, perp2, 0.04F * pulse, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawBeamLayer(VertexConsumer consumer, Matrix4f matrix, Vec3 start, Vec3 end,
                                       Vec3 perp1, Vec3 perp2, float halfWidth,
                                       float r, float g, float b, float a) {
        Vec3 p1 = start.add(perp1.scale(halfWidth)).add(perp2.scale(halfWidth));
        Vec3 p2 = start.add(perp1.scale(-halfWidth)).add(perp2.scale(halfWidth));
        Vec3 p3 = end.add(perp1.scale(-halfWidth)).add(perp2.scale(-halfWidth));
        Vec3 p4 = end.add(perp1.scale(halfWidth)).add(perp2.scale(-halfWidth));

        consumer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z).color(r, g, b, a).endVertex();

        Vec3 p5 = start.add(perp1.scale(halfWidth)).add(perp2.scale(-halfWidth));
        Vec3 p6 = start.add(perp1.scale(-halfWidth)).add(perp2.scale(-halfWidth));
        Vec3 p7 = end.add(perp1.scale(-halfWidth)).add(perp2.scale(halfWidth));
        Vec3 p8 = end.add(perp1.scale(halfWidth)).add(perp2.scale(halfWidth));

        consumer.vertex(matrix, (float) p5.x, (float) p5.y, (float) p5.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p6.x, (float) p6.y, (float) p6.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p7.x, (float) p7.y, (float) p7.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p8.x, (float) p8.y, (float) p8.z).color(r, g, b, a).endVertex();
    }

    private static void renderFusionSphere(VertexConsumer consumer, Matrix4f matrix, Vec3 center,
                                            float[] colorA, float[] colorB, float size, long timeMs, boolean isPrep) {
        float pulse = 0.85F + 0.15F * (float) Math.sin(timeMs * 0.02D);
        float s = size * pulse;

        // Additive color mixing
        float mixR = Math.min(1.0F, colorA[0] + colorB[0]);
        float mixG = Math.min(1.0F, colorA[1] + colorB[1]);
        float mixB = Math.min(1.0F, colorA[2] + colorB[2]);

        drawCrossQuad(consumer, matrix, center, s * 1.8F, mixR, mixG, mixB, 0.2F * pulse);
        drawCrossQuad(consumer, matrix, center, s * 1.3F, mixR * 0.9F, mixG * 0.9F, mixB * 0.9F, 0.5F * pulse);
        drawCrossQuad(consumer, matrix, center, s * 0.8F, 1.0F, 1.0F, 1.0F, 0.9F * pulse);
        drawCrossQuad(consumer, matrix, center, s * 0.4F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderDualColorRings(VertexConsumer consumer, Matrix4f matrix, Vec3 center,
                                              float[] colorA, float[] colorB, long timeMs) {
        float rot1 = timeMs * 0.004F;
        float rot2 = timeMs * -0.005F;

        drawSwirlingQuad(consumer, matrix, center, 1.1F, rot1, colorA[0], colorA[1], colorA[2], 0.7F);
        drawSwirlingQuad(consumer, matrix, center, 1.4F, rot2, colorB[0], colorB[1], colorB[2], 0.7F);
    }

    private static void renderSparks(VertexConsumer consumer, Matrix4f matrix, Vec3 center, Vec3 startA, Vec3 startB, long timeMs) {
        Random rand = new Random(center.hashCode() ^ (timeMs / 50));
        for (int i = 0; i < 4; i++) {
            Vec3 target = center.add(
                    (rand.nextDouble() - 0.5) * 3.5D,
                    (rand.nextDouble() - 0.5) * 3.5D,
                    (rand.nextDouble() - 0.5) * 3.5D
            );
            renderLightningArc(consumer, matrix, center, target, timeMs, 0xFFFFA0);
        }
    }

    private static void renderEnergyRings(VertexConsumer consumer, Matrix4f matrix, Vec3 center, long timeMs, int ticksElapsed) {
        float cycle = (timeMs % 800) / 800.0F;
        renderShockwaveRing(consumer, matrix, center, cycle);
    }

    private static void drawCrossQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 center, float halfWidth,
                                       float r, float g, float b, float a) {
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y, (float) center.z - halfWidth).color(r, g, b, a).endVertex();

        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y - halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y + halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y + halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y - halfWidth, (float) center.z).color(r, g, b, a).endVertex();

        consumer.vertex(matrix, (float) center.x, (float) center.y - halfWidth, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y + halfWidth, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y + halfWidth, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y - halfWidth, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
    }

    private static void drawSwirlingQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 center, float halfWidth,
                                         float angle, float r, float g, float b, float a) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        float dx1 = -halfWidth * cos - -halfWidth * sin;
        float dz1 = -halfWidth * sin + -halfWidth * cos;

        float dx2 = -halfWidth * cos - halfWidth * sin;
        float dz2 = -halfWidth * sin + halfWidth * cos;

        float dx3 = halfWidth * cos - halfWidth * sin;
        float dz3 = halfWidth * sin + halfWidth * cos;

        float dx4 = halfWidth * cos - -halfWidth * sin;
        float dz4 = halfWidth * sin + -halfWidth * cos;

        consumer.vertex(matrix, (float) center.x + dx1, (float) center.y, (float) center.z + dz1).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + dx2, (float) center.y, (float) center.z + dz2).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + dx3, (float) center.y, (float) center.z + dz3).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + dx4, (float) center.y, (float) center.z + dz4).color(r, g, b, a).endVertex();
    }

    private static void renderLightningArc(VertexConsumer consumer, Matrix4f matrix, Vec3 start, Vec3 end, long time, int color) {
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = 0.8F;

        Vec3 diff = end.subtract(start);
        int segments = 4;
        Vec3 prev = start;
        Random rand = new Random(start.hashCode() ^ time);

        for (int i = 1; i <= segments; i++) {
            float progress = (float) i / segments;
            Vec3 base = start.add(diff.scale(progress));
            if (i < segments) {
                base = base.add(
                        (rand.nextDouble() - 0.5) * 0.35,
                        (rand.nextDouble() - 0.5) * 0.35,
                        (rand.nextDouble() - 0.5) * 0.35
                );
            }
            drawCrossQuad(consumer, matrix, prev, 0.04F, r, g, b, a);
            prev = base;
        }
    }

    private static void renderShockwaveRing(VertexConsumer consumer, Matrix4f matrix, Vec3 center, float timeProgress) {
        float radius = 0.3F + timeProgress * 2.8F;
        float alpha = Math.max(0.0F, 1.0F - timeProgress);
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            double angle1 = (double) i / segments * 2.0 * Math.PI;
            double angle2 = (double) (i + 1) / segments * 2.0 * Math.PI;
            Vec3 p1 = center.add(Math.cos(angle1) * radius, 0, Math.sin(angle1) * radius);
            Vec3 p2 = center.add(Math.cos(angle2) * radius, 0, Math.sin(angle2) * radius);
            drawCrossQuad(consumer, matrix, p1, 0.06F, 0.8F, 0.9F, 1.0F, alpha * 0.6F);
        }
    }
}
