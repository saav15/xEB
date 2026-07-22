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

    public static class HolySlash {
        public final Vec3 pos;
        public final float yaw;
        public final int comboStage;
        public int ticksRemaining;

        public HolySlash(Vec3 pos, float yaw, int comboStage) {
            this.pos = pos;
            this.yaw = yaw;
            this.comboStage = comboStage;
            this.ticksRemaining = 8;
        }
    }

    public static class HolyPulse {
        public final Vec3 pos;
        public final float maxRadius;
        public int ticksRemaining;
        public final int maxTicks;

        public HolyPulse(Vec3 pos, float maxRadius, int duration) {
            this.pos = pos;
            this.maxRadius = maxRadius;
            this.ticksRemaining = duration;
            this.maxTicks = duration;
        }
    }

    public static final java.util.List<HolyBeam> ACTIVE_BEAMS = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final java.util.List<HolySlash> ACTIVE_SLASHES = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final java.util.List<HolyPulse> ACTIVE_PULSES = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addSlash(Vec3 pos, float yaw, int comboStage) {
        ACTIVE_SLASHES.add(new HolySlash(pos, yaw, comboStage));
    }

    public static void addBeam(double x, double y, double z, int duration) {
        ACTIVE_BEAMS.add(new HolyBeam(x, y, z, duration));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (HolyBeam beam : ACTIVE_BEAMS) {
            beam.ticksRemaining--;
            if (beam.ticksRemaining <= 0) {
                ACTIVE_BEAMS.remove(beam);
            }
        }

        for (HolySlash slash : ACTIVE_SLASHES) {
            slash.ticksRemaining--;
            if (slash.ticksRemaining <= 0) {
                ACTIVE_SLASHES.remove(slash);
            }
        }

        for (HolyPulse pulse : ACTIVE_PULSES) {
            pulse.ticksRemaining--;
            if (pulse.ticksRemaining <= 0) {
                ACTIVE_PULSES.remove(pulse);
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        for (Player player : mc.level.players()) {
            boolean holdsHoly = player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem
                    || player.getOffhandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem;

            if (holdsHoly) {
                // Client-side ticking for Annihilation active animation sequence
                boolean annihilation = player.getPersistentData().getBoolean("xebHolyAnnihilationActive");
                if (annihilation) {
                    int clientTicks = player.getPersistentData().getInt("xebHolyAnnihilationTicksClient") + 1;
                    player.getPersistentData().putInt("xebHolyAnnihilationTicksClient", clientTicks);

                    // Spawn expanding shockwave pulses synchronized with spin damage triggers
                    if (clientTicks == 6 || clientTicks == 13 || clientTicks == 20) {
                        ACTIVE_PULSES.add(new HolyPulse(player.position(), 5.0F, 10)); // 5.0m max radius, 10 ticks duration
                        
                        // Spawn cool particles in a circle
                        for (int theta = 0; theta < 360; theta += 15) {
                            double rad = Math.toRadians(theta);
                            double px = player.getX() + Math.cos(rad) * 1.5D;
                            double pz = player.getZ() + Math.sin(rad) * 1.5D;
                            double py = player.getY() + 0.1D;
                            mc.level.addParticle(ParticleTypes.END_ROD, px, py, pz, Math.cos(rad) * 0.15D, 0.02D, Math.sin(rad) * 0.15D);
                        }
                    }
                } else {
                    player.getPersistentData().putInt("xebHolyAnnihilationTicksClient", 0);
                }
            }

            if (holdsHoly && mc.level.getGameTime() % 2 == 0) {
                boolean blessed = player.getPersistentData().getBoolean("xebHolyBlessedActive");
                boolean annihilation = player.getPersistentData().getBoolean("xebHolyAnnihilationActive");

                double py = player.getY() + player.getBbHeight() + 0.25D;

                int particleCount = blessed ? 3 : 1;
                int particleInterval = blessed ? 2 : 4;
                
                if (mc.level.getGameTime() % particleInterval == 0) {
                    for (int i = 0; i < particleCount; i++) {
                        double px = player.getX() + (mc.level.random.nextDouble() - 0.5D) * 0.4D;
                        double pz = player.getZ() + (mc.level.random.nextDouble() - 0.5D) * 0.4D;
                        mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0.0D, 0.02D, 0.0D);
                    }
                }
                
                if (blessed && mc.level.getGameTime() % 5 == 0) {
                    mc.level.addParticle(ParticleTypes.END_ROD, player.getX(), py, player.getZ(), 0.0D, 0.02D, 0.0D);
                }
                
                if (annihilation) {
                    for (int i = 0; i < 5; i++) {
                        double px = player.getX() + (mc.level.random.nextDouble() - 0.5D) * 0.6D;
                        double pz = player.getZ() + (mc.level.random.nextDouble() - 0.5D) * 0.6D;
                        mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0.0D, 0.05D, 0.0D);
                    }
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

        // 1. Render Holy Blast vertical beams
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
                    
                    // Outer cylinder (gold translucent)
                    consumer.vertex(matrix, x1, 0.0F, z1).color(1.0F, 0.85F, 0.0F, alpha * 0.4F).endVertex();
                    consumer.vertex(matrix, x2, 0.0F, z2).color(1.0F, 0.85F, 0.0F, alpha * 0.4F).endVertex();
                    consumer.vertex(matrix, x2, height, z2).color(1.0F, 0.85F, 0.0F, alpha * 0.4F).endVertex();
                    consumer.vertex(matrix, x1, height, z1).color(1.0F, 0.85F, 0.0F, alpha * 0.4F).endVertex();
                    
                    // Inner cylinder (soft warm white)
                    float innerRadius = radius * 0.4F;
                    float ix1 = (float) (Math.cos(angle1) * innerRadius);
                    float iz1 = (float) (Math.sin(angle1) * innerRadius);
                    float ix2 = (float) (Math.cos(angle2) * innerRadius);
                    float iz2 = (float) (Math.sin(angle2) * innerRadius);
                    
                    consumer.vertex(matrix, ix1, 0.0F, iz1).color(1.0F, 0.95F, 0.8F, alpha).endVertex();
                    consumer.vertex(matrix, ix2, 0.0F, iz2).color(1.0F, 0.95F, 0.8F, alpha).endVertex();
                    consumer.vertex(matrix, ix2, height, iz2).color(1.0F, 0.95F, 0.8F, alpha).endVertex();
                    consumer.vertex(matrix, ix1, height, iz1).color(1.0F, 0.95F, 0.8F, alpha).endVertex();
                }
                
                poseStack.popPose();
            }
        }

        // 2. Render Slash Arc visual overlays
        for (HolySlash slash : ACTIVE_SLASHES) {
            poseStack.pushPose();
            poseStack.translate(slash.pos.x - camPos.x, slash.pos.y - camPos.y, slash.pos.z - camPos.z);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-slash.yaw));
            
            float alpha = slash.ticksRemaining / 8.0F;
            Matrix4f matrix = poseStack.last().pose();
            
            if (slash.comboStage == 0) {
                drawSlashArc(consumer, matrix, -45, 45, 1.3F, alpha, true);
            } else if (slash.comboStage == 1) {
                drawSlashArc(consumer, matrix, -45, 45, 1.3F, alpha, false);
            } else {
                drawCrossX(consumer, matrix, 1.6F, alpha);
            }
            poseStack.popPose();
        }

        // 3. Render Holy Annihilation expanding pulses
        if (!ACTIVE_PULSES.isEmpty()) {
            for (HolyPulse pulse : ACTIVE_PULSES) {
                poseStack.pushPose();
                poseStack.translate(pulse.pos.x - camPos.x, pulse.pos.y - camPos.y, pulse.pos.z - camPos.z);
                
                Matrix4f matrix = poseStack.last().pose();
                float progress = 1.0F - (pulse.ticksRemaining / (float) pulse.maxTicks);
                float radius = progress * pulse.maxRadius;
                float alpha = 1.0F - progress; // fade out as it expands
                
                drawExpandingRing(consumer, matrix, radius, alpha);
                poseStack.popPose();
            }
        }
    }

    private static void drawExpandingRing(VertexConsumer consumer, Matrix4f matrix, float radius, float alpha) {
        int segments = 32;
        float width = 0.4F; // width of the ring border
        for (int i = 0; i < segments; i++) {
            double angle1 = (i * 2.0D * Math.PI) / segments;
            double angle2 = ((i + 1) * 2.0D * Math.PI) / segments;
            
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);
            
            float x1_in = cos1 * Math.max(0.0F, radius - width);
            float z1_in = sin1 * Math.max(0.0F, radius - width);
            float x1_out = cos1 * radius;
            float z1_out = sin1 * radius;
            
            float x2_in = cos2 * Math.max(0.0F, radius - width);
            float z2_in = sin2 * Math.max(0.0F, radius - width);
            float x2_out = cos2 * radius;
            float z2_out = sin2 * radius;
            
            // Draw a flat ring on XZ plane (gold/light theme color)
            consumer.vertex(matrix, x1_in, 0.05F, z1_in).color(1.0F, 0.9F, 0.4F, alpha * 0.6F).endVertex();
            consumer.vertex(matrix, x1_out, 0.05F, z1_out).color(1.0F, 0.9F, 0.4F, 0.0F).endVertex();
            consumer.vertex(matrix, x2_out, 0.05F, z2_out).color(1.0F, 0.9F, 0.4F, 0.0F).endVertex();
            consumer.vertex(matrix, x2_in, 0.05F, z2_in).color(1.0F, 0.9F, 0.4F, alpha * 0.6F).endVertex();
        }
    }

    private static void drawSlashArc(VertexConsumer consumer, Matrix4f matrix, float startAngle, float endAngle, float radius, float alpha, boolean rightToLeft) {
        int steps = 12;
        for (int i = 0; i < steps; i++) {
            float pct1 = (float) i / steps;
            float pct2 = (float) (i + 1) / steps;
            
            float a1 = startAngle + (endAngle - startAngle) * pct1;
            float a2 = startAngle + (endAngle - startAngle) * pct2;
            if (!rightToLeft) {
                a1 = -a1;
                a2 = -a2;
            }
            
            float x1 = (float) Math.sin(Math.toRadians(a1)) * radius;
            float z1 = (float) Math.cos(Math.toRadians(a1)) * radius;
            float x2 = (float) Math.sin(Math.toRadians(a2)) * radius;
            float z2 = (float) Math.cos(Math.toRadians(a2)) * radius;
            
            float h = 0.12F;
            // Draw ribbon
            consumer.vertex(matrix, x1, -h, z1).color(1.0F, 0.85F, 0.0F, alpha * 0.6F).endVertex();
            consumer.vertex(matrix, x2, -h, z2).color(1.0F, 0.85F, 0.0F, alpha * 0.6F).endVertex();
            consumer.vertex(matrix, x2, h, z2).color(1.0F, 0.85F, 0.0F, alpha * 0.6F).endVertex();
            consumer.vertex(matrix, x1, h, z1).color(1.0F, 0.85F, 0.0F, alpha * 0.6F).endVertex();
        }
    }

    private static void drawCrossX(VertexConsumer consumer, Matrix4f matrix, float size, float alpha) {
        float h = 0.4F;
        // Diagonal 1
        consumer.vertex(matrix, -size, h, 1.0F).color(1.0F, 0.85F, 0.0F, alpha * 0.8F).endVertex();
        consumer.vertex(matrix, size, -h, 1.0F).color(1.0F, 0.85F, 0.0F, alpha * 0.8F).endVertex();
        consumer.vertex(matrix, size, -h + 0.12F, 1.0F).color(1.0F, 0.85F, 0.0F, alpha * 0.8F).endVertex();
        consumer.vertex(matrix, -size, h + 0.12F, 1.0F).color(1.0F, 0.85F, 0.0F, alpha * 0.8F).endVertex();

        // Diagonal 2
        consumer.vertex(matrix, size, h, 1.0F).color(1.0F, 0.85F, 0.0F, alpha * 0.8F).endVertex();
        consumer.vertex(matrix, -size, -h, 1.0F).color(1.0F, 0.85F, 0.0F, alpha * 0.8F).endVertex();
        consumer.vertex(matrix, -size, -h + 0.12F, 1.0F).color(1.0F, 0.85F, 0.0F, alpha * 0.8F).endVertex();
        consumer.vertex(matrix, size, h + 0.12F, 1.0F).color(1.0F, 0.85F, 0.0F, alpha * 0.8F).endVertex();
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        boolean holdsHoly = player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem
                || player.getOffhandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem;
        if (!holdsHoly) return;

        if (net.minecraftforge.fml.ModList.get().isLoaded("bettercombat")) return;

        boolean annihilation = player.getPersistentData().getBoolean("xebHolyAnnihilationActive");
        if (annihilation) {
            event.getPoseStack().pushPose();
            int clientTicks = player.getPersistentData().getInt("xebHolyAnnihilationTicksClient");
            if (clientTicks > 6) {
                // Spin after landing
                float spinAngle = (clientTicks - 6 + event.getPartialTick()) * 36.0F;
                event.getPoseStack().mulPose(Axis.YP.rotationDegrees(spinAngle));
            } else {
                // Ninja landing pose vertical offset
                event.getPoseStack().translate(0.0D, -0.25D, 0.0D);
            }
            player.getPersistentData().putBoolean("xebHolyPushedPose", true);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (player.getPersistentData().getBoolean("xebHolyPushedPose")) {
            player.getPersistentData().remove("xebHolyPushedPose");
            event.getPoseStack().popPose();
        }
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.body.xRot = 0.0F;
        model.body.yRot = 0.0F;
        model.leftArm.xRot = 0.0F;
        model.rightArm.xRot = 0.0F;
        model.leftArm.yRot = 0.0F;
        model.rightArm.yRot = 0.0F;
        model.leftArm.zRot = 0.0F;
        model.rightArm.zRot = 0.0F;
    }
}
