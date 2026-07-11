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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MechaOverdriveClientHandler {
    private static final Map<UUID, ArrayDeque<Vec3>> PLAYER_TRAILS = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (Player player : mc.level.players()) {
            boolean holdsMecha = player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem
                    || player.getOffhandItem().getItem() instanceof org.xeb.xeb.item.MechaOverdriveItem;

            UUID uuid = player.getUUID();
            if (!holdsMecha) {
                PLAYER_TRAILS.remove(uuid);
                continue;
            }

            ArrayDeque<Vec3> trail = PLAYER_TRAILS.computeIfAbsent(uuid, k -> new ArrayDeque<>());

            int sdState = player.getPersistentData().getInt("xebMechaSpindashState");
            boolean jetActive = player.getPersistentData().getBoolean("xebMechaJetActive");

            if (sdState > 0) {
                // Spinball form trail (center of player)
                trail.addFirst(player.position().add(0, player.getBbHeight() / 2.0D, 0));
                
                // Spawn spindash fire/smoke sparks
                if (mc.level.random.nextFloat() < 0.4F) {
                    mc.level.addParticle(ParticleTypes.FLAME, player.getX() + (mc.level.random.nextDouble() - 0.5D) * 0.8D,
                            player.getY() + 0.3D, player.getZ() + (mc.level.random.nextDouble() - 0.5D) * 0.8D,
                            0, 0.02D, 0);
                }
            } else if (jetActive) {
                // Walking jet trail (offset behind player's back chest)
                Vec3 look = player.getLookAngle().normalize();
                Vec3 backOffset = look.scale(-0.4D);
                Vec3 backPos = player.position().add(0, player.getBbHeight() * 0.6D, 0).add(backOffset);
                trail.addFirst(backPos);

                // Beautiful mechanical jet core exhaust fire and smoke
                double vx = -look.x * 0.15D + (mc.level.random.nextDouble() - 0.5D) * 0.05D;
                double vy = (mc.level.random.nextDouble() - 0.5D) * 0.02D;
                double vz = -look.z * 0.15D + (mc.level.random.nextDouble() - 0.5D) * 0.05D;
                mc.level.addParticle(ParticleTypes.FLAME, backPos.x, backPos.y, backPos.z, vx, vy, vz);
                if (mc.level.random.nextFloat() < 0.3F) {
                    mc.level.addParticle(ParticleTypes.SMOKE, backPos.x, backPos.y, backPos.z, vx, vy, vz);
                }
            } else {
                // Decaying speed / braking friction sparks at feet
                Vec3 motion = player.getDeltaMovement();
                double horiz = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                if (player.onGround() && horiz > 0.05D) {
                    if (mc.level.random.nextFloat() < 0.2F) {
                        mc.level.addParticle(ParticleTypes.CRIT, player.getX() + (mc.level.random.nextDouble() - 0.5D) * 0.4D,
                                player.getY() + 0.1D, player.getZ() + (mc.level.random.nextDouble() - 0.5D) * 0.4D,
                                0.0D, 0.0D, 0.0D);
                    }
                }

                // Decay trail
                if (!trail.isEmpty()) {
                    trail.removeLast();
                }
                continue;
            }

            while (trail.size() > 15) {
                trail.removeLast();
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
                float size = 0.4F * (1.0F - progress);
                
                // Color gradient: Orange/Red -> Yellow transparent
                float r = 1.0F;
                float g = 0.2F + 0.6F * progress;
                float b = 0.0F;
                float a = 0.8F * (1.0F - progress);

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

                i++;
            }
        }

        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        int sdState = player.getPersistentData().getInt("xebMechaSpindashState");

        if (sdState > 0) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            model.leftArm.visible = false;
            model.rightArm.visible = false;
            model.leftLeg.visible = false;
            model.rightLeg.visible = false;
            model.head.visible = false;
            model.hat.visible = false;

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            // Scaling body to make it a ball
            poseStack.scale(1.5F, 1.0F, 1.5F);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        int sdState = player.getPersistentData().getInt("xebMechaSpindashState");

        if (sdState > 0) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            model.leftArm.visible = true;
            model.rightArm.visible = true;
            model.leftLeg.visible = true;
            model.rightLeg.visible = true;
            model.head.visible = true;
            model.hat.visible = true;

            PoseStack poseStack = event.getPoseStack();
            poseStack.popPose();
        }
    }
}
