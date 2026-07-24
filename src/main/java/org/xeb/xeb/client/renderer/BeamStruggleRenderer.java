package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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
 * Renderizador Cliente-Side 3D de alta precisión para los duelos de rayos (Beam Struggle).
 * Alinea los rayos a la altura real del pecho/ojos de las entidades y calcula el avance
 * dinámico y suave del choque (Push & Pull) según los puntos de cada participante.
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

        // Ratio interpolado suavemente para el avance del choque
        public float smoothRatioA = 0.5F;

        // Campos de ritmo
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
            
            float total = this.pointsA + this.pointsB;
            this.smoothRatioA = total > 0.001F ? (this.pointsA / total) : 0.5F;

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
        float partialTick = event.getPartialTick();

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

            // === 1. ALINEACIÓN PRECISA EN TIEMPO REAL CON LAS ENTIDADES ===
            Entity entA = mc.level.getEntity(struggle.ownerAEntityId);
            Entity entB = mc.level.getEntity(struggle.ownerBEntityId);

            Vec3 liveStartA = entA != null ? getBeamOrigin(entA, partialTick) : struggle.startA;
            Vec3 liveStartB = entB != null ? getBeamOrigin(entB, partialTick) : struggle.startB;

            // === 2. AVANCE DINÁMICO SUAVE DEL CHOQUE (PUSH & PULL) ===
            float totalPoints = struggle.pointsA + struggle.pointsB;
            float targetRatioA = totalPoints > 0.001F ? (struggle.pointsA / totalPoints) : 0.5F;
            targetRatioA = Mth.clamp(targetRatioA, 0.08F, 0.92F);

            // Interpolación fluida (Lerp) para que la colisión se mueva con suavidad natural
            struggle.smoothRatioA += (targetRatioA - struggle.smoothRatioA) * 0.15F;

            Vec3 beamVector = liveStartB.subtract(liveStartA);
            Vec3 liveCollisionPoint = liveStartA.add(beamVector.scale(struggle.smoothRatioA));
            boolean isPrep = struggle.phase == 0;

            // Colores de los rayos
            float[] colorA = getOwnerColor(struggle.ownerAEntityId, mc);
            float[] colorB = getOwnerColor(struggle.ownerBEntityId, mc);

            // Grosor dinámico del rayo según los puntos (quien va ganando tiene un rayo más grueso)
            float beamRadiusA = 0.28F + (struggle.pointsA / Math.max(1.0F, totalPoints)) * 0.22F;
            float beamRadiusB = 0.28F + (struggle.pointsB / Math.max(1.0F, totalPoints)) * 0.22F;

            // 3. Renderizar Rayo Volumétrico A desde liveStartA -> liveCollisionPoint
            XebVolumetricBeamRenderer.render3DBeam(poseStack, bufferSource, liveStartA, liveCollisionPoint,
                    colorA[0], colorA[1], colorA[2], 0.95F, beamRadiusA, 0.70F, now);

            // 4. Renderizar Rayo Volumétrico B desde liveStartB -> liveCollisionPoint
            XebVolumetricBeamRenderer.render3DBeam(poseStack, bufferSource, liveStartB, liveCollisionPoint,
                    colorB[0], colorB[1], colorB[2], 0.95F, beamRadiusB, 0.70F, now);

            // 5. Esfera de Fusión de Energía Central
            float sphereSize = 0.75F + Math.min(1.8F, totalPoints * 0.04F);
            if (isPrep) sphereSize = 0.85F;
            renderFusionSphere(consumer, matrix, liveCollisionPoint, colorA, colorB, sphereSize, now, isPrep);

            // 6. Anillos de Energía Giratorios y Arcos Eléctricos
            renderDualColorRings(consumer, matrix, liveCollisionPoint, colorA, colorB, now);
            renderSparks(consumer, matrix, liveCollisionPoint, liveStartA, liveStartB, now);
            renderEnergyRings(consumer, matrix, liveCollisionPoint, now, struggle.ticksElapsed);
        }

        bufferSource.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    /**
     * Calcula la posición exacta de emisión del rayo a la altura del pecho/ojos de la entidad.
     */
    private static Vec3 getBeamOrigin(Entity entity, float partialTick) {
        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double y = Mth.lerp(partialTick, entity.yo, entity.getY());
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
        double heightOffset = entity.getBbHeight() * 0.65D; // Altura del pecho/ojos
        return new Vec3(x, y + heightOffset, z);
    }

    private static float[] getOwnerColor(int entityId, Minecraft mc) {
        if (mc.level != null && mc.level.getEntity(entityId) != null) {
            net.minecraft.world.entity.Entity ent = mc.level.getEntity(entityId);
            String typeName = net.minecraft.world.entity.EntityType.getKey(ent.getType()).toString();
            if (typeName.contains("tremorzilla")) return new float[]{0.25F, 1.0F, 0.5F}; // Verde
            if (typeName.contains("harbinger") || typeName.contains("leviathan")) return new float[]{0.5F, 0.2F, 1.0F}; // Púrpura
            
            if (ent instanceof net.minecraft.world.entity.player.Player player) {
                net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
                if (stack.is(org.xeb.xeb.item.ModItems.THE_TEARS.get())) {
                    int imbue = stack.getOrCreateTag().getInt("xebTearsImbueType");
                    if (imbue == 1) return new float[]{0.7F, 0.0F, 1.0F};
                    else if (imbue == 2) return new float[]{1.0F, 1.0F, 1.0F};
                    else if (imbue == 3) return new float[]{0.15F, 0.15F, 0.15F};
                    else if (imbue == 4) return new float[]{0.56F, 0.88F, 1.0F};
                }
            }
        }
        return new float[]{1.0F, 0.1F, 0.1F}; // Rojo por defecto
    }

    private static void renderFusionSphere(VertexConsumer consumer, Matrix4f matrix, Vec3 center,
                                            float[] colorA, float[] colorB, float size, long timeMs, boolean isPrep) {
        float pulse = 0.85F + 0.15F * (float) Math.sin(timeMs * 0.02D);
        float s = size * pulse;

        float mixR = Math.min(1.0F, colorA[0] + colorB[0]);
        float mixG = Math.min(1.0F, colorA[1] + colorB[1]);
        float mixB = Math.min(1.0F, colorA[2] + colorB[2]);

        drawCrossQuad(consumer, matrix, center, s * 1.8F, mixR, mixG, mixB, 0.25F * pulse);
        drawCrossQuad(consumer, matrix, center, s * 1.3F, mixR * 0.9F, mixG * 0.9F, mixB * 0.9F, 0.55F * pulse);
        drawCrossQuad(consumer, matrix, center, s * 0.8F, 1.0F, 1.0F, 1.0F, 0.95F * pulse);
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
