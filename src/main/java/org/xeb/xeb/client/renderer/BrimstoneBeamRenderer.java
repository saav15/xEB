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
import org.xeb.xeb.entity.TearsProjectileEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BrimstoneBeamRenderer {

    public static final Map<Integer, ClientBrimstoneData> CLIENT_BRIMSTONES = new ConcurrentHashMap<>();
    private static final BeamStyle BEAM_STYLE = new BeamStyle();

    public static void handleBeamPacket(int ownerEntityId, boolean active, int imbueType, List<Vec3> points, float beamWidth) {
        if (active && points.size() >= 2) {
            CLIENT_BRIMSTONES.put(ownerEntityId, new ClientBrimstoneData(points, imbueType, beamWidth, System.currentTimeMillis()));
        } else {
            CLIENT_BRIMSTONES.remove(ownerEntityId);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || CLIENT_BRIMSTONES.isEmpty()) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        long now = System.currentTimeMillis();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (Map.Entry<Integer, ClientBrimstoneData> entry : CLIENT_BRIMSTONES.entrySet()) {
            ClientBrimstoneData beam = entry.getValue();

            // Expire stale beams (500ms timeout)
            if (now - beam.lastUpdate > 500) {
                CLIENT_BRIMSTONES.remove(entry.getKey());
                continue;
            }

            List<Vec3> points = beam.points;
            int imbue = beam.imbueType;

            // Configure colors based on imbue
            float r = 0.55F, g = 0.03F, b = 0.03F; // Sangre oscuro
            float coreR = 0.75F, coreG = 0.05F, coreB = 0.05F; // Centro más brillante pero aún sangre
            if (imbue == TearsProjectileEntity.IMBUE_PURPLE) {
                r = 0.7F; g = 0.0F; b = 1.0F;
                coreR = 0.9F; coreG = 0.3F; coreB = 1.0F;
            } else if (imbue == TearsProjectileEntity.IMBUE_WHITE) {
                r = 0.9F; g = 0.9F; b = 0.9F;
                coreR = 1.0F; coreG = 1.0F; coreB = 1.0F;
            } else if (imbue == TearsProjectileEntity.IMBUE_DARK) {
                r = 0.15F; g = 0.1F; b = 0.15F;
                coreR = 0.05F; coreG = 0.05F; coreB = 0.05F;
            } else if (imbue == TearsProjectileEntity.IMBUE_COLD) {
                r = 0.4F; g = 0.8F; b = 1.0F;
                coreR = 0.8F; coreG = 0.95F; coreB = 1.0F;
            }

            BEAM_STYLE.coreR = coreR; BEAM_STYLE.coreG = coreG; BEAM_STYLE.coreB = coreB;
            BEAM_STYLE.auraR = r; BEAM_STYLE.auraG = g; BEAM_STYLE.auraB = b;
            
            if (imbue == TearsProjectileEntity.IMBUE_DOGMA) {
                BEAM_STYLE.tvStatic = true;
                BEAM_STYLE.auraWidth = beam.beamWidth * 0.5F;
                BEAM_STYLE.glowWidth = beam.beamWidth * 0.35F;
                BEAM_STYLE.coreWidth = beam.beamWidth * 0.20F;
                BEAM_STYLE.innerWidth = beam.beamWidth * 0.08F;
            } else {
                BEAM_STYLE.tvStatic = false;
                BEAM_STYLE.auraWidth = 0.75F;
                BEAM_STYLE.glowWidth = 0.50F;
                BEAM_STYLE.coreWidth = 0.30F;
                BEAM_STYLE.innerWidth = 0.10F;
            }
            BEAM_STYLE.heatHaze = true;

            // Render wavy organic segments
            Vec3 lastPos = points.get(0);
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 pStart = lastPos;
                Vec3 pEnd = points.get(i + 1);

                // Add organic sine wave displacement to pEnd (unless it is the final collision endpoint)
                if (i < points.size() - 2) {
                    Vec3 dirN = pEnd.subtract(pStart).normalize();
                    Vec3 perp = dirN.cross(new Vec3(0, 1, 0));
                    if (perp.lengthSqr() < 0.001D) perp = dirN.cross(new Vec3(1, 0, 0));
                    perp = perp.normalize();

                    double amp = (imbue == TearsProjectileEntity.IMBUE_PURPLE) ? 0.35D : 0.12D;
                    double freq = (imbue == TearsProjectileEntity.IMBUE_PURPLE) ? 0.02D : 0.015D;
                    double wavePhase = now * freq + i * 0.7D;
                    Vec3 waveOffset = perp.scale(Math.sin(wavePhase) * amp);

                    pEnd = pEnd.add(waveOffset);
                }

                XebVolumetricBeamRenderer.render3DBeam(
                        poseStack, bufferSource, pStart, pEnd,
                        r, g, b, 0.95F,
                        BEAM_STYLE.coreWidth, BEAM_STYLE.auraWidth, now
                );
                lastPos = pEnd;
            }

            // Draw impact sphere
            Matrix4f matrix = poseStack.last().pose();
            BEAM_STYLE.renderImpact(consumer, matrix, points.get(points.size() - 1), now);

            // Spawn custom client-side particles along the segments (BUG 7)
            if (now % 30 < 10) {
                net.minecraft.client.multiplayer.ClientLevel level = mc.level;
                if (level != null) {
                    for (int i = 0; i < points.size() - 1; i++) {
                        Vec3 start = points.get(i);
                        Vec3 end = points.get(i + 1);
                        Vec3 dir = end.subtract(start);
                        double len = dir.length();
                        if (len > 1.0D) {
                            Vec3 dirN = dir.normalize();
                            for (double d = 1.0D; d < len; d += 4.0D) {
                                Vec3 spawnPos = start.add(dirN.scale(d));
                                spawnImbueParticle(level, spawnPos, imbue);
                            }
                        }
                    }
                }
            }
        }

        bufferSource.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    private static void spawnImbueParticle(net.minecraft.client.multiplayer.ClientLevel level, Vec3 pos, int imbue) {
        double rx = (level.random.nextFloat() - 0.5D) * 0.15D;
        double ry = (level.random.nextFloat() - 0.5D) * 0.15D;
        double rz = (level.random.nextFloat() - 0.5D) * 0.15D;

        if (imbue == TearsProjectileEntity.IMBUE_WHITE) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, rx, ry, rz);
        } else if (imbue == TearsProjectileEntity.IMBUE_DARK) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.x, pos.y, pos.z, rx * 0.2D, ry * 0.2D, rz * 0.2D);
        } else if (imbue == TearsProjectileEntity.IMBUE_COLD) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.INSTANT_EFFECT, pos.x, pos.y, pos.z, rx, ry, rz);
        } else {
            // Default/Red/Purple: usar REDSTONE particles en color sangre en vez de FLAME
            // Sin partículas de fuego
            net.minecraft.core.particles.DustParticleOptions bloodDust = new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.55F, 0.03F, 0.03F), 1.0F);
            level.addParticle(bloodDust, pos.x, pos.y, pos.z, rx * 0.1D, 0.01D, rz * 0.1D);
        }
    }

    private static class ClientBrimstoneData {
        final List<Vec3> points;
        final int imbueType;
        final float beamWidth;
        long lastUpdate;

        ClientBrimstoneData(List<Vec3> points, int imbueType, float beamWidth, long lastUpdate) {
            this.points = points;
            this.imbueType = imbueType;
            this.beamWidth = beamWidth;
            this.lastUpdate = lastUpdate;
        }
    }
}
