package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders active Optic Blast beams as 3D rectangular quads using RenderLevelStageEvent.
 * <p>
 * All beams are COMPLETELY RED with a transparent red aura.
 * The beam has a rectangular cross-section (wider vertically, thinner horizontally)
 * to give a flat, visor-like laser look.
 * <p>
 * Supports three beam types:
 * <ul>
 *   <li>PRIMARY / CYCLONE_PUSH: single beam from eyes to target</li>
 *   <li>GENE_SPLICE (chain): multiple winding plasma segments between entities</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class OpticBlastBeamRenderer {

    /** Client-side beam data keyed by owner entity ID. */
    public static final Map<Integer, ClientBeamData> CLIENT_BEAMS = new ConcurrentHashMap<>();

    /** Client-side chain beam data (Gene Splice) keyed by owner entity ID. */
    public static final Map<Integer, ClientChainData> CLIENT_CHAINS = new ConcurrentHashMap<>();

    /**
     * Called from the OpticBlastBeamPacket handler on the client thread.
     */
    public static void handleBeamPacket(int ownerEntityId, boolean active,
                                         double startX, double startY, double startZ,
                                         double endX, double endY, double endZ,
                                         byte beamType) {
        if (active) {
            CLIENT_BEAMS.put(ownerEntityId, new ClientBeamData(
                    new Vec3(startX, startY, startZ),
                    new Vec3(endX, endY, endZ),
                    System.currentTimeMillis(),
                    beamType
            ));
        } else {
            CLIENT_BEAMS.remove(ownerEntityId);
        }
    }

    /**
     * Called from the OpticBlastChainBeamPacket handler on the client thread.
     */
    public static void handleChainBeamPacket(int ownerEntityId, boolean active, List<double[]> chainPoints) {
        if (active && chainPoints.size() >= 2) {
            List<Vec3> points = new ArrayList<>(chainPoints.size());
            for (double[] p : chainPoints) {
                points.add(new Vec3(p[0], p[1], p[2]));
            }
            CLIENT_CHAINS.put(ownerEntityId, new ClientChainData(points, System.currentTimeMillis()));
        } else {
            CLIENT_CHAINS.remove(ownerEntityId);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Player localPlayer = mc.player;

        boolean hasBeams = !CLIENT_BEAMS.isEmpty();
        boolean hasChains = !CLIENT_CHAINS.isEmpty();
        if (!hasBeams && !hasChains) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        long now = System.currentTimeMillis();


        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        // ── Render single beams (PRIMARY + CYCLONE_PUSH) ────────────────────
        for (Map.Entry<Integer, ClientBeamData> entry : CLIENT_BEAMS.entrySet()) {
            ClientBeamData beam = entry.getValue();

            // Expire stale beams (500ms timeout)
            if (now - beam.lastUpdate > 500) {
                CLIENT_BEAMS.remove(entry.getKey());
                continue;
            }

            Vec3 renderStart = beam.start;
            Vec3 renderEnd = beam.end;

            boolean isLocalPlayerFirstPerson = (localPlayer != null && entry.getKey().intValue() == localPlayer.getId() && mc.options.getCameraType().isFirstPerson());
            if (isLocalPlayerFirstPerson) {
                renderCollisionSprite(poseStack, consumer, renderEnd, now);
            } else {
                renderBeam(poseStack, consumer, renderStart, renderEnd, now, false);
            }
        }

        // ── Render chain beams (GENE SPLICE) ────────────────────────────────
        for (Map.Entry<Integer, ClientChainData> entry : CLIENT_CHAINS.entrySet()) {
            ClientChainData chain = entry.getValue();

            if (now - chain.lastUpdate > 500) {
                CLIENT_CHAINS.remove(entry.getKey());
                continue;
            }

            List<Vec3> points = chain.points;

            boolean isLocalPlayerFirstPerson = (localPlayer != null && entry.getKey() == localPlayer.getId() && mc.options.getCameraType().isFirstPerson());
            if (isLocalPlayerFirstPerson) {
                // First-person: DO NOT draw winding paths, ONLY draw the collision sprites at each target in the chain
                for (int i = 1; i < points.size(); i++) {
                    renderCollisionSprite(poseStack, consumer, points.get(i), now);
                }
            } else {
                // Third-person / other players: draw winding curves and collision sprites
                for (int i = 0; i < points.size() - 1; i++) {
                    renderWindingPlasmaBeam(poseStack, consumer, points.get(i), points.get(i + 1), now);
                }
            }
        }

        bufferSource.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    /**
     * Renders a winding helix plasma beam between points to make Gene Splice look
     * like it is intertwining between entities.
     */
    private static void renderWindingPlasmaBeam(PoseStack poseStack, VertexConsumer consumer,
                                                Vec3 start, Vec3 end, long timeMs) {
        Vec3 dir = end.subtract(start);
        double dist = dir.length();
        if (dist < 0.05D) return;

        Vec3 dirNorm = dir.normalize();

        // Calculate perpendicular vectors to create spiral offsets
        Vec3 ortho = dirNorm.cross(new Vec3(0, 1, 0));
        if (ortho.lengthSqr() < 0.001D) {
            ortho = dirNorm.cross(new Vec3(1, 0, 0));
        }
        ortho = ortho.normalize();
        Vec3 ortho2 = dirNorm.cross(ortho).normalize();

        int steps = 12;
        Vec3 lastPos = start;

        for (int step = 1; step <= steps; step++) {
            float pct = step / (float) steps;
            Vec3 basePos = start.add(dir.scale(pct));

            // Sine/Cosine spiraling curves over time and distance
            double waveFreq = 3.0D;
            double wavePhase = pct * Math.PI * waveFreq - (timeMs * 0.012D);
            double offsetAmp = 0.15D * Math.sin(pct * Math.PI); // maximum offset in the middle of segment

            // Primary spiral wave
            Vec3 offset = ortho.scale(Math.sin(wavePhase) * offsetAmp)
                    .add(ortho2.scale(Math.cos(wavePhase) * offsetAmp));

            // Secondary faster spiral wave for complexity
            double wavePhase2 = pct * Math.PI * 6.0D + (timeMs * 0.024D);
            double offsetAmp2 = 0.06D * Math.sin(pct * Math.PI);
            offset = offset.add(ortho.scale(Math.sin(wavePhase2) * offsetAmp2))
                    .add(ortho2.scale(Math.cos(wavePhase2) * offsetAmp2));

            Vec3 currentPos = basePos.add(offset);

            // Render this winding sub-segment
            renderBeamSegment(poseStack, consumer, lastPos, currentPos, timeMs, true);
            lastPos = currentPos;
        }

        // Draw volumetric collision sprite at segment endpoint (the entity hitpoint)
        renderCollisionSprite(poseStack, consumer, end, timeMs);
    }

    /**
     * Renders a single straight beam segment from start to end.
     */
    private static void renderBeamSegment(PoseStack poseStack, VertexConsumer consumer,
                                          Vec3 start, Vec3 end, long timeMs, boolean isChain) {
        Vec3 beamDir = end.subtract(start);
        double length = beamDir.length();
        if (length < 0.01D) return;

        Vec3 beamDirNorm = beamDir.normalize();

        // Perpendicular vectors (visor-aligned: perp1 horizontal, perp2 vertical)
        Vec3 perp1 = beamDirNorm.cross(new Vec3(0, 1, 0));
        if (perp1.lengthSqr() < 0.0001D) {
            perp1 = beamDirNorm.cross(new Vec3(1, 0, 0));
        }
        perp1 = perp1.normalize();

        Vec3 perp2 = beamDirNorm.cross(perp1).normalize();

        float pulse = (isChain ? 0.80F + 0.20F * (float) Math.sin(timeMs * 0.02D)
                               : 0.90F + 0.10F * (float) Math.sin(timeMs * 0.008D));
        float glowPulse = (isChain ? 0.65F + 0.35F * (float) Math.sin(timeMs * 0.015D)
                                   : 0.75F + 0.25F * (float) Math.sin(timeMs * 0.005D));

        float coreHoriz = (isChain ? 0.09F : 0.06F) * pulse;
        float coreVert = (isChain ? 0.20F : 0.14F) * pulse;

        float midHoriz = (isChain ? 0.22F : 0.14F) * glowPulse;
        float midVert = (isChain ? 0.40F : 0.28F) * glowPulse;

        float auraHoriz = (isChain ? 0.45F : 0.30F) * glowPulse;
        float auraVert = (isChain ? 0.75F : 0.50F) * glowPulse;

        Matrix4f matrix = poseStack.last().pose();

        // Layer 1: Outer aura — very transparent red
        drawRectQuad(consumer, matrix, start, end, perp1, perp2,
                auraHoriz, auraVert,
                0.7F, 0.0F, 0.0F, 0.08F * glowPulse);

        // Layer 2: Mid glow — semi-transparent red
        drawRectQuad(consumer, matrix, start, end, perp1, perp2,
                midHoriz, midVert,
                0.9F, 0.02F, 0.0F, 0.22F * glowPulse);

        // Layer 3: Core beam — solid bright red, rectangular
        drawRectQuad(consumer, matrix, start, end, perp1, perp2,
                coreHoriz, coreVert,
                1.0F, isChain ? 0.0F : 0.08F, isChain ? 0.15F : 0.02F, 0.90F);

        // Layer 4: Inner hot line — very narrow, slightly brighter red
        float innerH = coreHoriz * 0.3F;
        float innerV = coreVert * 0.3F;
        drawRectQuad(consumer, matrix, start, end, perp1, perp2,
                innerH, innerV,
                1.0F, 0.15F, 0.05F, 0.95F);
    }

    /**
     * Entry method to render standard straight beams (PRIMARY / CYCLONE_PUSH).
     */
    private static void renderBeam(PoseStack poseStack, VertexConsumer consumer,
                                    Vec3 start, Vec3 end, long timeMs, boolean isChain) {
        renderBeamSegment(poseStack, consumer, start, end, timeMs, isChain);
        
        // Draw volumetric collision sprite at the beam endpoint (block hit)
        renderCollisionSprite(poseStack, consumer, end, timeMs);
    }

    /**
     * Renders a small 3D volumetric collision sprite/glow at the laser endpoint.
     */
    private static void renderCollisionSprite(PoseStack poseStack, VertexConsumer consumer,
                                               Vec3 end, long timeMs) {
        org.joml.Matrix4f matrix = poseStack.last().pose();

        float pulse = 0.85F + 0.15F * (float) Math.sin(timeMs * 0.02D);
        
        // Outer soft glow cross
        float outerSize = 0.50F * pulse;
        drawCrossQuad(consumer, matrix, end, outerSize, 1.0F, 0.0F, 0.0F, 0.25F * pulse);

        // Mid glow
        float midSize = 0.30F * pulse;
        drawCrossQuad(consumer, matrix, end, midSize, 1.0F, 0.1F, 0.0F, 0.55F * pulse);

        // Inner solid core
        float innerSize = 0.14F * pulse;
        drawCrossQuad(consumer, matrix, end, innerSize, 1.0F, 0.6F, 0.5F, 0.90F);
    }

    /**
     * Draws three perpendicular planes/quads to form a volumetric 3D cross star.
     */
    private static void drawCrossQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 center, float halfWidth,
                                       float r, float g, float b, float a) {
        // 1. Horizontal plane (XZ)
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y, (float) center.z - halfWidth).color(r, g, b, a).endVertex();

        // 2. Vertical plane (XY)
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y - halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x - halfWidth, (float) center.y + halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y + halfWidth, (float) center.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x + halfWidth, (float) center.y - halfWidth, (float) center.z).color(r, g, b, a).endVertex();

        // 3. Vertical plane (YZ)
        consumer.vertex(matrix, (float) center.x, (float) center.y - halfWidth, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y + halfWidth, (float) center.z - halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y + halfWidth, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) center.x, (float) center.y - halfWidth, (float) center.z + halfWidth).color(r, g, b, a).endVertex();
    }

    /**
     * Draws a rectangular cross-section beam using two perpendicular quads.
     */
    private static void drawRectQuad(VertexConsumer consumer, Matrix4f matrix,
                                      Vec3 start, Vec3 end,
                                      Vec3 perpHoriz, Vec3 perpVert,
                                      float halfWidthH, float halfWidthV,
                                      float r, float g, float b, float a) {
        // Horizontal quad (thin)
        drawQuad(consumer, matrix, start, end, perpHoriz, halfWidthH, r, g, b, a);
        // Vertical quad (tall)
        drawQuad(consumer, matrix, start, end, perpVert, halfWidthV, r, g, b, a);
    }

    /**
     * Draws a single billboard quad.
     */
    private static void drawQuad(VertexConsumer consumer, Matrix4f matrix,
                                  Vec3 start, Vec3 end, Vec3 perp, float halfWidth,
                                  float r, float g, float b, float a) {
        Vec3 p1 = start.add(perp.scale(halfWidth));
        Vec3 p2 = start.add(perp.scale(-halfWidth));
        Vec3 p3 = end.add(perp.scale(-halfWidth));
        Vec3 p4 = end.add(perp.scale(halfWidth));

        consumer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                .color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                .color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z)
                .color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z)
                .color(r, g, b, a).endVertex();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLIENT DATA CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    private static class ClientBeamData {
        final Vec3 start;
        final Vec3 end;
        long lastUpdate;
        final byte beamType;

        ClientBeamData(Vec3 start, Vec3 end, long lastUpdate, byte beamType) {
            this.start = start;
            this.end = end;
            this.lastUpdate = lastUpdate;
            this.beamType = beamType;
        }
    }

    private static class ClientChainData {
        final List<Vec3> points;
        long lastUpdate;

        ClientChainData(List<Vec3> points, long lastUpdate) {
            this.points = points;
            this.lastUpdate = lastUpdate;
        }
    }
}
