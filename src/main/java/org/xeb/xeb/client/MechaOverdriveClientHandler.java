package org.xeb.xeb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MechaOverdriveClientHandler {
    private static final Map<UUID, ArrayDeque<Vec3>> PLAYER_TRAILS = new HashMap<>();
    private static final Map<UUID, ArrayDeque<PlayerFrame>> DASH_FRAMES = new HashMap<>();

    // Screen Shake variables
    public static float screenShakeIntensity = 0.0F;
    public static int screenShakeTicks = 0;

    // Previous states to trigger screen shake on transition
    private static final Map<UUID, Boolean> WAS_DASHING = new HashMap<>();
    private static final Map<UUID, Integer> PREV_SPINDASH_STATE = new HashMap<>();

    public static class PlayerFrame {
        public final Vec3 pos;
        public final float yRot;
        public final float xRot;
        public final float yHeadRot;
        public final float yBodyRot;
        
        public PlayerFrame(Vec3 pos, float yRot, float xRot, float yHeadRot, float yBodyRot) {
            this.pos = pos;
            this.yRot = yRot;
            this.xRot = xRot;
            this.yHeadRot = yHeadRot;
            this.yBodyRot = yBodyRot;
        }
    }

    private static class AlphaOverrideVertexConsumer implements VertexConsumer {
        private final VertexConsumer parent;
        private final float alpha;

        public AlphaOverrideVertexConsumer(VertexConsumer parent, float alpha) {
            this.parent = parent;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            parent.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            parent.color(r, g, b, (int) (a * this.alpha));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            parent.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            parent.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            parent.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            parent.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            parent.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            parent.defaultColor(r, g, b, (int) (a * this.alpha));
        }

        @Override
        public void unsetDefaultColor() {
            parent.unsetDefaultColor();
        }
    }

    @SubscribeEvent
    public static void onComputeFov(net.minecraftforge.client.event.ViewportEvent.ComputeFov event) {
        if (screenShakeTicks > 0) {
            float wobble = (float) Math.sin(event.getRenderer().getMinecraft().level.getGameTime() * 1.5F) * screenShakeIntensity;
            event.setFOV(event.getFOV() + wobble);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (screenShakeTicks > 0) {
            screenShakeTicks--;
        }

        for (Player player : mc.level.players()) {
            boolean holdsMecha = player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem
                    || player.getOffhandItem().getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem;

            UUID uuid = player.getUUID();
            if (!holdsMecha) {
                PLAYER_TRAILS.remove(uuid);
                DASH_FRAMES.remove(uuid);
                WAS_DASHING.remove(uuid);
                PREV_SPINDASH_STATE.remove(uuid);
                continue;
            }

            // Screen Shake transitions
            boolean dashing = player.getPersistentData().getBoolean("xebMechaOverdriveDashing");
            int sdState = player.getPersistentData().getInt("xebMechaSpindashState");
            
            boolean wasDashing = WAS_DASHING.getOrDefault(uuid, false);
            int prevSdState = PREV_SPINDASH_STATE.getOrDefault(uuid, 0);
            
            if (player == mc.player) {
                if (dashing && !wasDashing) {
                    screenShakeTicks = 15;
                    screenShakeIntensity = 2.5F;
                } else if (sdState == 3 && prevSdState == 1) {
                    screenShakeTicks = 12;
                    screenShakeIntensity = 2.0F;
                }
            }
            
            WAS_DASHING.put(uuid, dashing);
            PREV_SPINDASH_STATE.put(uuid, sdState);

            // Jet hover / momentum trails
            ArrayDeque<Vec3> trail = PLAYER_TRAILS.computeIfAbsent(uuid, k -> new ArrayDeque<>());
            boolean jetActive = player.getPersistentData().getBoolean("xebMechaJetActive");

            if (sdState > 0) {
                // Spinball form trail
                trail.addFirst(player.position().add(0, player.getBbHeight() / 2.0D, 0));
                
                if (mc.level.random.nextFloat() < 0.4F) {
                    mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, player.getX() + (mc.level.random.nextDouble() - 0.5D) * 0.8D,
                            player.getY() + 0.3D, player.getZ() + (mc.level.random.nextDouble() - 0.5D) * 0.8D,
                            0, 0.02D, 0);
                }
                if (mc.level.random.nextFloat() < 0.2F) {
                    mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, player.getX() + (mc.level.random.nextDouble() - 0.5D) * 0.8D,
                            player.getY() + 0.3D, player.getZ() + (mc.level.random.nextDouble() - 0.5D) * 0.8D,
                            0, 0.0D, 0);
                }
            } else if (jetActive || dashing) {
                // Walking jet trail
                Vec3 look = player.getLookAngle().normalize();
                Vec3 backOffset = look.scale(-0.4D);
                Vec3 backPos = player.position().add(0, player.getBbHeight() * 0.6D, 0).add(backOffset);
                trail.addFirst(backPos);

                double vx = -look.x * 0.15D + (mc.level.random.nextDouble() - 0.5D) * 0.05D;
                double vy = (mc.level.random.nextDouble() - 0.5D) * 0.02D;
                double vz = -look.z * 0.15D + (mc.level.random.nextDouble() - 0.5D) * 0.05D;
                
                mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, backPos.x, backPos.y, backPos.z, vx, vy, vz);
                if (mc.level.random.nextFloat() < 0.3F) {
                    mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, backPos.x, backPos.y, backPos.z, vx, vy, vz);
                }
            } else {
                if (!trail.isEmpty()) {
                    trail.removeLast();
                }
            }

            while (trail.size() > 15) {
                trail.removeLast();
            }

            // Afterimage frame recording
            if (dashing) {
                ArrayDeque<PlayerFrame> frames = DASH_FRAMES.computeIfAbsent(uuid, k -> new ArrayDeque<>());
                frames.addFirst(new PlayerFrame(player.position(), player.getYRot(), player.getXRot(), player.getYHeadRot(), player.yBodyRot));
                while (frames.size() > 12) {
                    frames.removeLast();
                }
            } else {
                DASH_FRAMES.remove(uuid);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // 1. Render Jet / Momentum Trails
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = poseStack.last().pose();

        for (Map.Entry<UUID, ArrayDeque<Vec3>> entry : PLAYER_TRAILS.entrySet()) {
            ArrayDeque<Vec3> trail = entry.getValue();
            int i = 0;
            int total = trail.size();
            for (Vec3 point : trail) {
                float progress = (float) i / total; // 0 to 1

                // Layer 1: Outer Cyan transparent
                drawTrailLayer(consumer, matrix, point, 0.5F * (1.0F - progress), 0.0F, 1.0F, 1.0F, 0.3F * (1.0F - progress));
                // Layer 2: Medium Blue
                drawTrailLayer(consumer, matrix, point, 0.3F * (1.0F - progress), 0.0F, 0.0F, 1.0F, 0.6F * (1.0F - progress));
                // Layer 3: Inner White core
                drawTrailLayer(consumer, matrix, point, 0.1F * (1.0F - progress), 1.0F, 1.0F, 1.0F, 0.9F * (1.0F - progress));

                i++;
            }
        }
        poseStack.popPose();

        // 2. Render Jet Dash Afterimages
        for (Map.Entry<UUID, ArrayDeque<PlayerFrame>> entry : DASH_FRAMES.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = mc.level.getPlayerByUUID(uuid);
            if (player == null) continue;

            ArrayDeque<PlayerFrame> frames = entry.getValue();
            int idx = 0;
            for (PlayerFrame frame : frames) {
                if (idx == 3 || idx == 6 || idx == 9) {
                    float alpha = 0.6F - (idx / 12.0F) * 0.5F;
                    poseStack.pushPose();

                    double dx = frame.pos.x - camPos.x;
                    double dy = frame.pos.y - camPos.y;
                    double dz = frame.pos.z - camPos.z;
                    poseStack.translate(dx, dy, dz);

                    MultiBufferSource transparentBuffer = type -> new AlphaOverrideVertexConsumer(bufferSource.getBuffer(type), alpha);
                    mc.getEntityRenderDispatcher().render(player, 0, 0, 0, frame.yRot, event.getPartialTick(), poseStack, transparentBuffer, 15728880);

                    poseStack.popPose();
                }
                idx++;
            }
        }
    }

    private static void drawTrailLayer(VertexConsumer consumer, Matrix4f matrix, Vec3 point, float size, float r, float g, float b, float a) {
        // Horizontal (XZ)
        consumer.vertex(matrix, (float) point.x - size, (float) point.y, (float) point.z - size).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) point.x - size, (float) point.y, (float) point.z + size).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) point.x + size, (float) point.y, (float) point.z + size).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) point.x + size, (float) point.y, (float) point.z - size).color(r, g, b, a).endVertex();

        // Vertical (XY)
        consumer.vertex(matrix, (float) point.x - size, (float) point.y - size, (float) point.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) point.x - size, (float) point.y + size, (float) point.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) point.x + size, (float) point.y + size, (float) point.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float) point.x + size, (float) point.y - size, (float) point.z).color(r, g, b, a).endVertex();
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        CompoundTag tag = player.getPersistentData();
        int sdState = tag.getInt("xebMechaSpindashState");
        boolean dashing = tag.getBoolean("xebMechaOverdriveDashing");
        boolean levitating = tag.getBoolean("xebMechaLevitating");
        boolean vulcan = tag.getBoolean("xebMechaVulcanFiring");

        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();

        boolean needsPop = sdState == 3 || dashing || levitating;
        if (needsPop) {
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();

            if (sdState == 3) { // Spindash ATTACKING (Spinball)
                model.leftArm.visible = false;
                model.rightArm.visible = false;
                model.leftLeg.visible = false;
                model.rightLeg.visible = false;
                model.head.visible = false;
                model.hat.visible = false;

                // Flattened ball body scaling
                poseStack.scale(1.6F, 0.8F, 1.6F);
                float rot = (player.level().getGameTime() + event.getPartialTick()) * 30.0F;
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rot));
            } else if (dashing) { // Jet Dash
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(80.0F));
                poseStack.translate(0, -0.5F, -0.3F);
                
                model.rightArm.xRot = 1.5F;
                model.leftArm.xRot = 1.5F;
            } else if (levitating) { // Hover levitating
                poseStack.translate(0, 0.15F, 0);
                model.rightArm.xRot = 0.3F;
                model.leftArm.xRot = 0.3F;
            }
        }

        // Apply visual rotations/inclinations directly to model segments (no poseStack pop required)
        if (sdState == 1) { // Spindash charging
            model.body.xRot = 0.5F;
            model.rightArm.xRot = -0.8F;
            model.rightArm.yRot = -0.5F;
            model.leftArm.xRot = -0.8F;
            model.leftArm.yRot = 0.5F;
        } else if (vulcan) { // Vulcan firing
            model.rightArm.xRot = -1.5F;
            model.leftArm.xRot = -1.5F;
            model.body.xRot = 0.17F; // inclined 10 deg
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        CompoundTag tag = player.getPersistentData();
        int sdState = tag.getInt("xebMechaSpindashState");
        boolean dashing = tag.getBoolean("xebMechaOverdriveDashing");
        boolean levitating = tag.getBoolean("xebMechaLevitating");

        boolean needsPop = sdState == 3 || dashing || levitating;
        if (needsPop) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            model.leftArm.visible = true;
            model.rightArm.visible = true;
            model.leftLeg.visible = true;
            model.rightLeg.visible = true;
            model.head.visible = true;
            model.hat.visible = true;

            event.getPoseStack().popPose();
        }
        
        // Reset model rotations
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.body.xRot = 0.0F;
        model.rightArm.yRot = 0.0F;
        model.leftArm.yRot = 0.0F;
    }
}
