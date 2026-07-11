package org.xeb.xeb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
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

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HolyDualityClientHandler {

    public static class HolyBeam {
        public final double x, y, z;
        public int ticksRemaining;

        public HolyBeam(double x, double y, double z, int ticksRemaining) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.ticksRemaining = ticksRemaining;
        }
    }

    public static final java.util.List<HolyBeam> ACTIVE_BEAMS = new java.util.concurrent.CopyOnWriteArrayList<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (HolyBeam beam : ACTIVE_BEAMS) {
            beam.ticksRemaining--;
            if (beam.ticksRemaining <= 0) {
                ACTIVE_BEAMS.remove(beam);
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        for (Player player : mc.level.players()) {
            boolean holdsHoly = player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem
                    || player.getOffhandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem;

            if (holdsHoly && mc.level.getGameTime() % 2 == 0) {
                int crownState = player.getPersistentData().getInt("xebHolyCrownState");
                int crownTicks = player.getPersistentData().getInt("xebHolyCrownTicks");

                double baseHeight = 2.4D;
                double targetHeight = baseHeight;

                if (crownState == 1) { // DROPPING
                    float pct = crownTicks / 10.0F;
                    targetHeight = Mth.lerp(pct, baseHeight, 0.1D);
                } else if (crownState == 2) { // RISING
                    float pct = crownTicks / 10.0F;
                    targetHeight = Mth.lerp(pct, 0.1D, baseHeight);
                }

                long ticks = mc.level.getGameTime();
                double hoverY = Math.sin(ticks * 0.08D) * 0.15D;
                double hoverX = Math.cos(ticks * 0.05D) * 0.1D;

                double px = player.getX() + hoverX + (mc.level.random.nextDouble() - 0.5D) * 0.4D;
                double py = player.getY() + targetHeight + hoverY + (mc.level.random.nextDouble() - 0.5D) * 0.2D;
                double pz = player.getZ() + (mc.level.random.nextDouble() - 0.5D) * 0.4D;
                mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0.0D, 0.02D, 0.0D);
                if (mc.level.random.nextFloat() < 0.3F) {
                    mc.level.addParticle(ParticleTypes.GLOW, px, py, pz, 0.0D, 0.01D, 0.0D);
                }
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

        if (!ACTIVE_BEAMS.isEmpty()) {
            for (HolyBeam beam : ACTIVE_BEAMS) {
                poseStack.pushPose();
                poseStack.translate(beam.x - camPos.x, beam.y - camPos.y, beam.z - camPos.z);
                
                Matrix4f matrix = poseStack.last().pose();
                float radius = 0.20F;
                float height = 64.0F;
                float alpha = Math.min(0.8F, beam.ticksRemaining / 15.0F);
                
                int segments = 8;
                for (int i = 0; i < segments; i++) {
                    double angle1 = (i * 2.0D * Math.PI) / segments;
                    double angle2 = ((i + 1) * 2.0D * Math.PI) / segments;
                    
                    float x1 = (float) (Math.cos(angle1) * radius);
                    float z1 = (float) (Math.sin(angle1) * radius);
                    float x2 = (float) (Math.cos(angle2) * radius);
                    float z2 = (float) (Math.sin(angle2) * radius);
                    
                    // Outer cylinder (semi-transparent white)
                    consumer.vertex(matrix, x1, 0.0F, z1).color(1.0F, 1.0F, 1.0F, alpha * 0.5F).endVertex();
                    consumer.vertex(matrix, x2, 0.0F, z2).color(1.0F, 1.0F, 1.0F, alpha * 0.5F).endVertex();
                    consumer.vertex(matrix, x2, height, z2).color(1.0F, 1.0F, 1.0F, alpha * 0.5F).endVertex();
                    consumer.vertex(matrix, x1, height, z1).color(1.0F, 1.0F, 1.0F, alpha * 0.5F).endVertex();
                    
                    // Inner core cylinder (opaque white)
                    float innerRadius = radius * 0.4F;
                    float ix1 = (float) (Math.cos(angle1) * innerRadius);
                    float iz1 = (float) (Math.sin(angle1) * innerRadius);
                    float ix2 = (float) (Math.cos(angle2) * innerRadius);
                    float iz2 = (float) (Math.sin(angle2) * innerRadius);
                    
                    consumer.vertex(matrix, ix1, 0.0F, iz1).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
                    consumer.vertex(matrix, ix2, 0.0F, iz2).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
                    consumer.vertex(matrix, ix2, height, iz2).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
                    consumer.vertex(matrix, ix1, height, iz1).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
                }
                
                poseStack.popPose();
            }
        }

        for (Player player : mc.level.players()) {
            boolean holdsHoly = player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem
                    || player.getOffhandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem;

            if (!holdsHoly) continue;

            int crownState = player.getPersistentData().getInt("xebHolyCrownState");
            int crownTicks = player.getPersistentData().getInt("xebHolyCrownTicks");

            double baseHeight = 2.4D;
            double targetHeight = baseHeight;

            if (crownState == 1) { // DROPPING
                float pct = crownTicks / 10.0F;
                targetHeight = Mth.lerp(pct, baseHeight, 0.1D);
            } else if (crownState == 2) { // RISING
                float pct = crownTicks / 10.0F;
                targetHeight = Mth.lerp(pct, 0.1D, baseHeight);
            }

            long ticks = mc.level.getGameTime();
            double hoverY = Math.sin(ticks * 0.08D) * 0.15D;
            double hoverX = Math.cos(ticks * 0.05D) * 0.1D;

            Vec3 crownPos = player.position().add(hoverX, targetHeight + hoverY, 0.0D);

            poseStack.pushPose();
            poseStack.translate(crownPos.x - camPos.x, crownPos.y - camPos.y, crownPos.z - camPos.z);

            Matrix4f matrix = poseStack.last().pose();
            float radius = 0.35F;
            boolean shieldActive = player.getPersistentData().getBoolean("xebHolyShieldActive");
            float r = shieldActive ? 1.0F : 0.0F;
            float g = shieldActive ? 1.0F : 0.9F;
            float b = shieldActive ? 1.0F : 1.0F;
            float a = 0.9F;

            int segments = 12;
            for (int j = 0; j < segments; j++) {
                double angle1 = (j * 2.0D * Math.PI) / segments;
                double angle2 = ((j + 1) * 2.0D * Math.PI) / segments;

                float x1 = (float) (Math.cos(angle1) * radius);
                float z1 = (float) (Math.sin(angle1) * radius);
                float x2 = (float) (Math.cos(angle2) * radius);
                float z2 = (float) (Math.sin(angle2) * radius);

                // Top rim
                consumer.vertex(matrix, x1, 0.08F, z1).color(r, g, b, a).endVertex();
                consumer.vertex(matrix, x2, 0.08F, z2).color(r, g, b, a).endVertex();
                consumer.vertex(matrix, x2, -0.02F, z2).color(r, g, b, a).endVertex();
                consumer.vertex(matrix, x1, -0.02F, z1).color(r, g, b, a).endVertex();

                // 3D spikes / pikes on the crown: segment 0 (front center - large), 3 and 9 (sides - small)
                float peakY = 0.08F;
                if (j == 0) {
                    peakY = 0.32F;
                } else if (j == 3 || j == 9) {
                    peakY = 0.20F;
                }

                if (peakY > 0.08F) {
                    float xMid = (x1 + x2) * 0.5F;
                    float zMid = (z1 + z2) * 0.5F;
                    
                    consumer.vertex(matrix, x1, 0.08F, z1).color(r, g, b, a).endVertex();
                    consumer.vertex(matrix, x2, 0.08F, z2).color(r, g, b, a).endVertex();
                    consumer.vertex(matrix, xMid, peakY, zMid).color(r, g, b, a).endVertex();
                    consumer.vertex(matrix, x1, 0.08F, z1).color(r, g, b, a).endVertex();
                }
            }

            poseStack.popPose();
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        boolean holdsHoly = player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem
                || player.getOffhandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem;

        if (holdsHoly) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            
            ItemStack stack = player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem
                    ? player.getMainHandItem() : player.getOffhandItem();
            int combo = player.getPersistentData().getInt("xebHolyComboStage");
            float swingProgress = player.getAttackAnim(event.getPartialTick());

            if (player.swingTime > 0) {
                float swingAngle = Mth.sin(Mth.sqrt(swingProgress) * (float)Math.PI);
                float swingAngleY = Mth.sin(swingProgress * (float)Math.PI);
                
                if (combo == 0) {
                    // Right arm swings (handled by game), left arm stays idle
                    model.leftArm.xRot = -0.6F;
                    model.leftArm.yRot = 0.1F;
                    model.leftArm.zRot = 0.0F;
                } else if (combo == 1) {
                    // Left arm swings, right arm stays idle
                    model.rightArm.xRot = -0.6F;
                    model.rightArm.yRot = -0.1F;
                    model.rightArm.zRot = 0.0F;
                    
                    model.leftArm.xRot = -0.6F - swingAngle * 1.2F;
                    model.leftArm.yRot = 0.1F - swingAngleY * 0.4F;
                    model.leftArm.zRot = 0.0F;
                } else {
                    // Both arms swing (Tajo doble X)
                    model.rightArm.xRot = -0.6F - swingAngle * 1.0F;
                    model.rightArm.yRot = -0.1F + swingAngleY * 0.3F;
                    model.rightArm.zRot = -0.4F * swingAngle;
                    
                    model.leftArm.xRot = -0.6F - swingAngle * 1.0F;
                    model.leftArm.yRot = 0.1F - swingAngleY * 0.3F;
                    model.leftArm.zRot = 0.4F * swingAngle;
                }
            } else {
                // Symmetrical idle stance: mirror right arm to left arm
                model.leftArm.xRot = model.rightArm.xRot;
                model.leftArm.yRot = -model.rightArm.yRot;
                model.leftArm.zRot = -model.rightArm.zRot;
            }

            // Render second sword in left hand
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            
            model.leftArm.translateAndRotate(poseStack);
            
            // Align with left hand using standard ItemInHandLayer sequence:
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(-0.0625F, 0.125F, -0.625F);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    player,
                    stack,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    true,
                    poseStack,
                    event.getMultiBufferSource(),
                    player.level(),
                    event.getPackedLight(),
                    OverlayTexture.NO_OVERLAY,
                    player.getId()
            );

            poseStack.popPose();
        }
    }
}
