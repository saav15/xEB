package org.xeb.xeb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getPersistentData().getBoolean("xebMechaTargetedDashing")) {
            event.setFOV(event.getFOV() + 18.0F); // Sonic extreme speed FOV zoom!
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean holdsMecha = mc.player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem
                || mc.player.getOffhandItem().getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem;
        if (!holdsMecha) return;

        boolean targetedSpindash = mc.player.getPersistentData().getBoolean("xebMechaTargetedSpindash");
        boolean targetedDashing = mc.player.getPersistentData().getBoolean("xebMechaTargetedDashing");
        int sdState = mc.player.getPersistentData().getInt("xebMechaSpindashState");

        if (sdState > 0 || targetedSpindash || targetedDashing) {
            event.setPitch(event.getPitch() + 12.0F); // Dynamic downward angle for high speed Sonic action view
        }
    }

    private static net.minecraft.client.CameraType PREVIOUS_CAMERA_TYPE = null;
    private static boolean WAS_SPINDASH_CAMERA = false;

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
                if (player == mc.player && WAS_SPINDASH_CAMERA) {
                    if (PREVIOUS_CAMERA_TYPE != null) {
                        mc.options.setCameraType(PREVIOUS_CAMERA_TYPE);
                        PREVIOUS_CAMERA_TYPE = null;
                    }
                    WAS_SPINDASH_CAMERA = false;
                }
                continue;
            }

            // Screen Shake transitions & 3rd Person Spindash Camera
            boolean dashing = player.getPersistentData().getBoolean("xebMechaOverdriveDashing");
            boolean targetedDashing = player.getPersistentData().getBoolean("xebMechaTargetedDashing");
            int sdState = player.getPersistentData().getInt("xebMechaSpindashState");
            boolean targetedSpindash = player.getPersistentData().getBoolean("xebMechaTargetedSpindash");
            boolean isSpindashActive = sdState > 0 || targetedSpindash || targetedDashing;
            
            boolean wasDashing = WAS_DASHING.getOrDefault(uuid, false);
            int prevSdState = PREV_SPINDASH_STATE.getOrDefault(uuid, 0);
            
            if (player == mc.player) {
                // Toggle 3rd person camera during Spindash / Homing action
                if (isSpindashActive && !WAS_SPINDASH_CAMERA) {
                    PREVIOUS_CAMERA_TYPE = mc.options.getCameraType();
                    mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                    WAS_SPINDASH_CAMERA = true;
                } else if (!isSpindashActive && WAS_SPINDASH_CAMERA) {
                    if (PREVIOUS_CAMERA_TYPE != null) {
                        mc.options.setCameraType(PREVIOUS_CAMERA_TYPE);
                        PREVIOUS_CAMERA_TYPE = null;
                    }
                    WAS_SPINDASH_CAMERA = false;
                }

                if ((dashing || targetedDashing) && !wasDashing) {
                    screenShakeTicks = 15;
                    screenShakeIntensity = 2.5F;
                } else if (sdState == 3 && prevSdState == 1) {
                    screenShakeTicks = 12;
                    screenShakeIntensity = 2.0F;
                }
            }
            
            WAS_DASHING.put(uuid, dashing || targetedDashing);
            PREV_SPINDASH_STATE.put(uuid, sdState);

            // Continuous momentum estela trail recording
            ArrayDeque<Vec3> trail = PLAYER_TRAILS.computeIfAbsent(uuid, k -> new ArrayDeque<>());
            boolean jetActive = player.getPersistentData().getBoolean("xebMechaJetActive");
            double momentum = player.getPersistentData().getDouble("xebMechaMomentum");
            boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001D;

            if (sdState > 0) {
                // Spinball form trail
                trail.addFirst(player.position().add(0, player.getBbHeight() / 2.0D, 0));
                
                if (mc.level.random.nextFloat() < 0.5F) {
                    mc.level.addParticle(ParticleTypes.FLAME, player.getX() + (mc.level.random.nextDouble() - 0.5D) * 0.8D,
                            player.getY() + 0.3D, player.getZ() + (mc.level.random.nextDouble() - 0.5D) * 0.8D,
                            0, 0.02D, 0);
                }
            } else if (jetActive || dashing || targetedDashing || momentum > 0.05D || isMoving) {
                // Standing / Walking / Dashing momentum trail
                Vec3 look = player.getLookAngle().normalize();
                Vec3 backOffset = look.scale(-0.4D);
                Vec3 backPos = player.position().add(0, player.getBbHeight() * 0.5D, 0).add(backOffset);
                trail.addFirst(backPos);

                if (momentum > 0.1D) {
                    double vx = -look.x * 0.15D + (mc.level.random.nextDouble() - 0.5D) * 0.05D;
                    double vy = (mc.level.random.nextDouble() - 0.5D) * 0.02D;
                    double vz = -look.z * 0.15D + (mc.level.random.nextDouble() - 0.5D) * 0.05D;
                    
                    mc.level.addParticle(ParticleTypes.FLAME, backPos.x, backPos.y, backPos.z, vx, vy, vz);
                    if (momentum >= 0.8D && mc.level.random.nextFloat() < 0.4F) {
                        mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, backPos.x, backPos.y, backPos.z, vx, vy, vz);
                    }
                }
            } else {
                if (!trail.isEmpty()) {
                    trail.removeLast();
                }
            }

            while (trail.size() > 22) {
                trail.removeLast();
            }

            // Afterimage frame recording
            if (dashing || targetedDashing) {
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

        // 1. Render Jet / Momentum Trails (Continuous Ribbon Scaling With Momentum)
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.disableCull(); // Disable culling so ribbon is visible from both sides

        for (Map.Entry<UUID, ArrayDeque<Vec3>> entry : PLAYER_TRAILS.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = mc.level.getPlayerByUUID(uuid);
            double momentum = player != null ? player.getPersistentData().getDouble("xebMechaMomentum") : 0.5D;

            ArrayDeque<Vec3> trail = entry.getValue();
            if (trail.size() < 2) continue;

            java.util.List<Vec3> list = new java.util.ArrayList<>(trail);

            // Ribbon width starts small (0.10F) and grows up to 0.55F at 300 momentum!
            float maxTrailWidth = 0.10F + (float) momentum * 0.45F;

            for (int i = 0; i < list.size() - 1; i++) {
                Vec3 p1 = list.get(i);
                Vec3 p2 = list.get(i + 1);

                float progress1 = (float) i / list.size();
                float progress2 = (float) (i + 1) / list.size();

                float size1 = maxTrailWidth * (1.0F - progress1);
                float size2 = maxTrailWidth * (1.0F - progress2);

                Vec3 dir = p1.subtract(p2);
                if (dir.lengthSqr() < 0.001D) {
                    dir = new Vec3(0, 0, 1);
                }
                Vec3 perp = new Vec3(-dir.z, 0.0D, dir.x).normalize();

                Vec3 left1 = p1.subtract(perp.scale(size1));
                Vec3 right1 = p1.add(perp.scale(size1));
                Vec3 left2 = p2.subtract(perp.scale(size2));
                Vec3 right2 = p2.add(perp.scale(size2));

                Vec3 up1 = p1.add(0.0D, size1, 0.0D);
                Vec3 down1 = p1.subtract(0.0D, size1, 0.0D);
                Vec3 up2 = p2.add(0.0D, size2, 0.0D);
                Vec3 down2 = p2.subtract(0.0D, size2, 0.0D);

                // Layer 1: Outer Red/Orange transparent
                float a1 = 0.25F * (1.0F - progress1);
                float a2 = 0.25F * (1.0F - progress2);
                float redVal = momentum >= 0.9D ? 0.0F : 1.0F;
                float grnVal = momentum >= 0.9D ? 0.9F : 0.35F;
                float bluVal = momentum >= 0.9D ? 1.0F : 0.0F;
                drawRibbon(consumer, matrix, left1, right1, right2, left2, up1, down1, down2, up2, redVal, grnVal, bluVal, a1, a2);

                // Layer 2: Inner Gold/Yellow
                float a3 = 0.50F * (1.0F - progress1);
                float a4 = 0.50F * (1.0F - progress2);
                drawRibbon(consumer, matrix, left1, right1, right2, left2, up1, down1, down2, up2, 1.0F, 0.75F, 0.0F, a3, a4);

                // Layer 3: White core
                float sizeCore1 = size1 * 0.35F;
                float sizeCore2 = size2 * 0.35F;
                Vec3 leftCore1 = p1.subtract(perp.scale(sizeCore1));
                Vec3 rightCore1 = p1.add(perp.scale(sizeCore1));
                Vec3 leftCore2 = p2.subtract(perp.scale(sizeCore2));
                Vec3 rightCore2 = p2.add(perp.scale(sizeCore2));
                Vec3 upCore1 = p1.add(0.0D, sizeCore1, 0.0D);
                Vec3 downCore1 = p1.subtract(0.0D, sizeCore1, 0.0D);
                Vec3 upCore2 = p2.add(0.0D, sizeCore2, 0.0D);
                Vec3 downCore2 = p2.subtract(0.0D, sizeCore2, 0.0D);

                float a5 = 0.85F * (1.0F - progress1);
                float a6 = 0.85F * (1.0F - progress2);
                drawRibbon(consumer, matrix, leftCore1, rightCore1, rightCore2, leftCore2, upCore1, downCore1, downCore2, upCore2, 1.0F, 1.0F, 1.0F, a5, a6);
            }
        }

        RenderSystem.enableCull(); // Restore culling

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

        // 3. Render Custom Sonic Style 3D Target Reticle & Lock-on Laser Beam
        Player localPlayer = mc.player;
        if (localPlayer != null) {
            boolean holdsMecha = localPlayer.getMainHandItem().getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem
                    || localPlayer.getOffhandItem().getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem;
            int targetId = localPlayer.getPersistentData().getInt("xebSpindashTargetId");
            boolean isActiva2Down = ModKeyMappings.ACTIVA_2_KEY.isDown();
            boolean isSpindashActive = localPlayer.getPersistentData().getInt("xebMechaSpindashState") > 0
                    || localPlayer.getPersistentData().getBoolean("xebMechaTargetedSpindash")
                    || localPlayer.getPersistentData().getBoolean("xebMechaTargetedDashing");

            if (holdsMecha && targetId != -1 && (isActiva2Down || isSpindashActive)) {
                net.minecraft.world.entity.Entity targetEntity = mc.level.getEntity(targetId);
                if (targetEntity instanceof net.minecraft.world.entity.LivingEntity target && target.isAlive()) {
                    Vec3 playerChest = localPlayer.position().add(0, localPlayer.getBbHeight() * 0.5D, 0);
                    Vec3 targetCenter = target.getBoundingBox().getCenter();

                    // Render laser line connecting player and targeted mob
                    VertexConsumer laserBuilder = bufferSource.getBuffer(RenderType.lines());
                    Matrix4f lineMat = poseStack.last().pose();

                    double px = playerChest.x - camPos.x;
                    double py = playerChest.y - camPos.y;
                    double pz = playerChest.z - camPos.z;

                    double tx = targetCenter.x - camPos.x;
                    double ty = targetCenter.y - camPos.y;
                    double tz = targetCenter.z - camPos.z;

                    laserBuilder.vertex(lineMat, (float) px, (float) py, (float) pz).color(1.0F, 0.2F, 0.0F, 0.9F).normal(0, 1, 0).endVertex();
                    laserBuilder.vertex(lineMat, (float) tx, (float) ty, (float) tz).color(1.0F, 0.8F, 0.0F, 0.9F).normal(0, 1, 0).endVertex();

                    poseStack.pushPose();
                    poseStack.translate(tx, ty, tz);
                    poseStack.mulPose(camera.rotation());

                    float time = (float) (mc.level.getGameTime() + event.getPartialTick()) * 0.25F;
                    float scale = (float) (target.getBbWidth() * 0.9D + Math.sin(time * 2.0F) * 0.08D);
                    poseStack.scale(scale, scale, scale);

                    VertexConsumer builder = bufferSource.getBuffer(RenderType.lines());
                    Matrix4f mat = poseStack.last().pose();

                    float boxR = 0.8F;
                    float red = 1.0F, grn = 0.1F, blu = 0.1F, alpha = 1.0F;

                    // Rotating reticle ring effect
                    builder.vertex(mat, -boxR, -boxR, 0).color(red, grn, blu, alpha).normal(0, 0, 1).endVertex();
                    builder.vertex(mat, boxR, -boxR, 0).color(red, grn, blu, alpha).normal(0, 0, 1).endVertex();

                    builder.vertex(mat, boxR, -boxR, 0).color(red, grn, blu, alpha).normal(0, 0, 1).endVertex();
                    builder.vertex(mat, boxR, boxR, 0).color(red, grn, blu, alpha).normal(0, 0, 1).endVertex();

                    builder.vertex(mat, boxR, boxR, 0).color(red, grn, blu, alpha).normal(0, 0, 1).endVertex();
                    builder.vertex(mat, -boxR, boxR, 0).color(red, grn, blu, alpha).normal(0, 0, 1).endVertex();

                    builder.vertex(mat, -boxR, boxR, 0).color(red, grn, blu, alpha).normal(0, 0, 1).endVertex();
                    builder.vertex(mat, -boxR, -boxR, 0).color(red, grn, blu, alpha).normal(0, 0, 1).endVertex();

                    builder.vertex(mat, -boxR * 1.4F, 0, 0).color(1.0F, 0.9F, 0.0F, alpha).normal(0, 0, 1).endVertex();
                    builder.vertex(mat, -boxR * 0.5F, 0, 0).color(1.0F, 0.9F, 0.0F, alpha).normal(0, 0, 1).endVertex();

                    builder.vertex(mat, boxR * 0.5F, 0, 0).color(1.0F, 0.9F, 0.0F, alpha).normal(0, 0, 1).endVertex();
                    builder.vertex(mat, boxR * 1.4F, 0, 0).color(1.0F, 0.9F, 0.0F, alpha).normal(0, 0, 1).endVertex();

                    builder.vertex(mat, 0, -boxR * 1.4F, 0).color(1.0F, 0.9F, 0.0F, alpha).normal(0, 0, 1).endVertex();
                    builder.vertex(mat, 0, -boxR * 0.5F, 0).color(1.0F, 0.9F, 0.0F, alpha).normal(0, 0, 1).endVertex();

                    builder.vertex(mat, 0, boxR * 0.5F, 0).color(1.0F, 0.9F, 0.0F, alpha).normal(0, 0, 1).endVertex();
                    builder.vertex(mat, 0, boxR * 1.4F, 0).color(1.0F, 0.9F, 0.0F, alpha).normal(0, 0, 1).endVertex();

                    bufferSource.endBatch(RenderType.lines());
                    poseStack.popPose();
                }
            }
        }
    }

    private static void drawRibbon(VertexConsumer consumer, Matrix4f matrix,
                                   Vec3 left1, Vec3 right1, Vec3 right2, Vec3 left2,
                                   Vec3 up1, Vec3 down1, Vec3 down2, Vec3 up2,
                                   float r, float g, float b, float a1, float a2) {
        // Horizontal Quad
        consumer.vertex(matrix, (float) left1.x, (float) left1.y, (float) left1.z).color(r, g, b, a1).endVertex();
        consumer.vertex(matrix, (float) right1.x, (float) right1.y, (float) right1.z).color(r, g, b, a1).endVertex();
        consumer.vertex(matrix, (float) right2.x, (float) right2.y, (float) right2.z).color(r, g, b, a2).endVertex();
        consumer.vertex(matrix, (float) left2.x, (float) left2.y, (float) left2.z).color(r, g, b, a2).endVertex();

        // Vertical Quad
        consumer.vertex(matrix, (float) down1.x, (float) down1.y, (float) down1.z).color(r, g, b, a1).endVertex();
        consumer.vertex(matrix, (float) up1.x, (float) up1.y, (float) up1.z).color(r, g, b, a1).endVertex();
        consumer.vertex(matrix, (float) up2.x, (float) up2.y, (float) up2.z).color(r, g, b, a2).endVertex();
        consumer.vertex(matrix, (float) down2.x, (float) down2.y, (float) down2.z).color(r, g, b, a2).endVertex();
    }
}
