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
    private static final BeamStyle BEAM_STYLE = new BeamStyle();

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

            // BUG 2 fix: skip rendering original beam if this owner is currently in a struggle
            if (org.xeb.xeb.client.renderer.BeamStruggleRenderer.isOwnerInStruggle(entry.getKey())) {
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

    private static void renderBeamSegment(PoseStack poseStack, VertexConsumer consumer,
                                          Vec3 start, Vec3 end, long timeMs, boolean isChain) {
        Matrix4f matrix = poseStack.last().pose();

        BEAM_STYLE.coreR = 1.0F; BEAM_STYLE.coreG = 0.1F; BEAM_STYLE.coreB = 0.1F;
        BEAM_STYLE.auraR = 1.0F; BEAM_STYLE.auraG = 0.0F; BEAM_STYLE.auraB = 0.0F;

        if (isChain) {
            BEAM_STYLE.auraWidth = 0.5F;
            BEAM_STYLE.glowWidth = 0.3F;
            BEAM_STYLE.coreWidth = 0.15F;
            BEAM_STYLE.innerWidth = 0.05F;
        } else {
            BEAM_STYLE.auraWidth = 0.85F;
            BEAM_STYLE.glowWidth = 0.50F;
            BEAM_STYLE.coreWidth = 0.28F;
            BEAM_STYLE.innerWidth = 0.10F;
        }
        BEAM_STYLE.heatHaze = true;

        BEAM_STYLE.render(consumer, matrix, start, end, timeMs);
    }

    /**
     * Entry method to render standard straight beams (PRIMARY / CYCLONE_PUSH).
     */
    private static void renderBeam(PoseStack poseStack, VertexConsumer consumer,
                                    Vec3 start, Vec3 end, long timeMs, boolean isChain) {
        renderBeamSegment(poseStack, consumer, start, end, timeMs, isChain);

        Matrix4f matrix = poseStack.last().pose();
        BEAM_STYLE.renderImpact(consumer, matrix, end, timeMs);

        // Spawn sparks and fire particles along the beam on the client (BUG 7)
        if (timeMs % 40 < 10) {
            Vec3 dir = end.subtract(start);
            double length = dir.length();
            if (length > 1.0D) {
                Vec3 dirN = dir.normalize();
                net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
                if (level != null) {
                    for (double d = 2.0D; d < length; d += 5.0D) {
                        Vec3 spawnPos = start.add(dirN.scale(d));
                        level.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                                spawnPos.x, spawnPos.y, spawnPos.z,
                                (level.random.nextFloat() - 0.5F) * 0.1D,
                                (level.random.nextFloat() - 0.5F) * 0.1D,
                                (level.random.nextFloat() - 0.5F) * 0.1D);
                    }
                }
            }
        }
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
     * Draws a 4-plane volumetric cross-section beam (0, 45, 90, 135 degrees) for 360-degree visibility from all camera angles.
     */
    private static void drawRectQuad(VertexConsumer consumer, Matrix4f matrix,
                                      Vec3 start, Vec3 end,
                                      Vec3 perpHoriz, Vec3 perpVert,
                                      float halfWidthH, float halfWidthV,
                                      float r, float g, float b, float a) {
        // Horizontal plane (0 deg)
        drawQuad(consumer, matrix, start, end, perpHoriz, halfWidthH, r, g, b, a);
        // Vertical plane (90 deg)
        drawQuad(consumer, matrix, start, end, perpVert, halfWidthV, r, g, b, a);

        // Diagonal planes (45 deg and 135 deg) for 360-degree angle visibility
        Vec3 diag1 = perpHoriz.add(perpVert).normalize();
        Vec3 diag2 = perpHoriz.subtract(perpVert).normalize();
        float halfWidthDiag = (halfWidthH + halfWidthV) * 0.5F;
        drawQuad(consumer, matrix, start, end, diag1, halfWidthDiag, r, g, b, a);
        drawQuad(consumer, matrix, start, end, diag2, halfWidthDiag, r, g, b, a);
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
