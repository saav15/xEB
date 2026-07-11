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
        }

        public void update(BeamStrugglePacket msg) {
            this.startA = msg.getStartPosA();
            this.startB = msg.getStartPosB();
            this.collisionPoint = msg.getCollisionPoint();
            this.pointsA = msg.getPointsA();
            this.pointsB = msg.getPointsB();
            this.ticksElapsed = msg.getTicksElapsed();
            this.lastUpdateTime = System.currentTimeMillis();
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
        // Find the struggle with corresponding owner IDs and remove it
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

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ACTIVE_STRUGGLES.isEmpty()) return;

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

            // Auto-clean struggles with no updates for 1 second
            if (now - struggle.lastUpdateTime > 1000) {
                ACTIVE_STRUGGLES.remove(entry.getKey());
                continue;
            }

            Vec3 pos = struggle.collisionPoint;

            // 1. Swirling Plasma Ball (white inner sphere + swirling red/green/yellow quads)
            float innerPulse = 0.4F + 0.1F * (float) Math.sin(now * 0.015D);
            drawCrossQuad(consumer, matrix, pos, innerPulse, 1.0F, 1.0F, 1.0F, 0.9F);

            // Swirling aura layers
            float speedMultiplier = 0.005F;
            float rot1 = now * speedMultiplier;
            float rot2 = now * -0.007F;

            // Draw swirling plasma shells
            drawSwirlingQuad(consumer, matrix, pos, innerPulse * 1.5F, rot1, 1.0F, 0.2F, 0.2F, 0.7F);
            drawSwirlingQuad(consumer, matrix, pos, innerPulse * 1.8F, rot2, 0.9F, 0.9F, 0.1F, 0.5F);

            // 2. Connector Lightning Arcs
            Random rand = new Random(struggle.struggleId.getMostSignificantBits() ^ (now / 50));
            for (int i = 0; i < 4; i++) {
                Vec3 target = pos.add(
                        (rand.nextDouble() - 0.5) * 3.5D,
                        (rand.nextDouble() - 0.5) * 3.5D,
                        (rand.nextDouble() - 0.5) * 3.5D
                );
                renderLightningArc(consumer, matrix, pos, target, now, 0xFFFFA0);
            }

            // 3. Expanding Shockwave Ring
            float cycle = (now % 800) / 800.0F; // expansions repeat every 800ms
            renderShockwaveRing(consumer, matrix, pos, cycle);
        }

        poseStack.popPose();
    }

    private static void drawCrossQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 center, float halfWidth,
                                       float r, float g, float b, float a) {
        // Horizontal plane (XZ)
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y, (float) center.z - halfWidth).color(r, g, b, a).endVertex();

        // Vertical plane (XY)
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y - halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y + halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y + halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y - halfWidth, (float) center.z).color(r, g, b, a).endVertex();

        // Vertical plane (YZ)
        consumer.vertex(matrix, (float) center.x, (float) center.y - halfWidth, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y + halfWidth, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y + halfWidth, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y - halfWidth, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
    }

    private static void drawSwirlingQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 center, float halfWidth,
                                         float angle, float r, float g, float b, float a) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        // Rotated plane coordinates on XZ rotated around Y
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
