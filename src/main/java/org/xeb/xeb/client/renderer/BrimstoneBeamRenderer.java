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

import net.minecraft.client.player.LocalPlayer;

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

            int wielderId = entry.getKey();
            LocalPlayer localPlayer = mc.player;
            boolean isLocalPlayer = (localPlayer != null && wielderId == localPlayer.getId());
            boolean isLocalPlayerFirstPerson = (isLocalPlayer && mc.options.getCameraType().isFirstPerson());

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

            Vec3 liveStart;
            Vec3 impactPos;

            if (isLocalPlayer) {
                // Live 1:1 crosshair raycast on client frame for zero network delay
                float partialTick = event.getPartialTick();
                liveStart = localPlayer.getEyePosition(partialTick);
                Vec3 look = localPlayer.getViewVector(partialTick).normalize();
                Vec3 maxReach = liveStart.add(look.scale(48.0D));

                net.minecraft.world.phys.BlockHitResult clip = mc.level.clip(new net.minecraft.world.level.ClipContext(
                        liveStart, maxReach, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, localPlayer
                ));
                impactPos = clip.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? clip.getLocation() : maxReach;
            } else {
                List<Vec3> points = beam.points;
                liveStart = points.get(0);
                impactPos = points.get(points.size() - 1);
            }

            // Render 3D beam tube only when NOT in local 1st person mode (3D Volumetric Conical Tapering Geometry)
            if (!isLocalPlayerFirstPerson) {
                if (imbue == TearsProjectileEntity.IMBUE_DOGMA) {
                    XebVolumetricBeamRenderer.render3DTvStaticBeam(
                            poseStack, bufferSource, liveStart, impactPos,
                            r, g, b, 0.95F,
                            BEAM_STYLE.coreWidth, BEAM_STYLE.auraWidth, now
                    );
                } else {
                    XebVolumetricBeamRenderer.render3DBeam(
                            poseStack, bufferSource, liveStart, impactPos,
                            r, g, b, 0.95F,
                            BEAM_STYLE.coreWidth, BEAM_STYLE.auraWidth, now
                    );
                }
            }

            // Draw impact sphere & scorch mark on solid block
            Matrix4f matrix = poseStack.last().pose();
            BEAM_STYLE.renderImpact(consumer, matrix, impactPos, now);
            LaserScorchManager.addScorchMarkOnBlock(mc.level, liveStart, impactPos, 0.85F, r, g, b);

            // Spawn custom client-side particles along the segment
            if (now % 30 < 10) {
                net.minecraft.client.multiplayer.ClientLevel level = mc.level;
                if (level != null) {
                    Vec3 dir = impactPos.subtract(liveStart);
                    double len = dir.length();
                    if (len > 1.0D) {
                        Vec3 dirN = dir.normalize();
                        for (double d = 1.0D; d < len; d += 4.0D) {
                            Vec3 spawnPos = liveStart.add(dirN.scale(d));
                            spawnImbueParticle(level, spawnPos, imbue);
                        }
                    }
                }
            }
        }

        bufferSource.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    private static void spawnImbueParticle(net.minecraft.client.multiplayer.ClientLevel level, Vec3 pos, int imbue) {
        if (imbue == TearsProjectileEntity.IMBUE_DOGMA) {
            // Dogma es 100% estática monocromática de TV - Sin partículas rojas de azufre
            return;
        }

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

    public static class ClientBrimstoneData {
        public final List<Vec3> points;
        public final int imbueType;
        public final float beamWidth;
        public long lastUpdate;

        ClientBrimstoneData(List<Vec3> points, int imbueType, float beamWidth, long lastUpdate) {
            this.points = points;
            this.imbueType = imbueType;
            this.beamWidth = beamWidth;
            this.lastUpdate = lastUpdate;
        }
    }
}
