package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ultra-High Quality Client Renderer for Sovereign Arsenal Universal Extreme Burst.
 *
 * <p>Upgrades:
 * 1. 3D Multi-Ring Golden Spatial Vortex Sky Portals with depth test disabled to prevent cloud clipping.
 * 2. Multi-tier vertical portal heights for deep 3D volumetric feel.
 * 3. Individual Staggered Golden Spatial Explosions for each stuck weapon (3D Blast Sphere + Energy Spikes + Plasma Rings + Beam + Sparkle Particles).</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SovereignArsenalRenderer {

    public static final Map<Integer, ClientSovereignData> ACTIVE_SOVEREIGNS = new ConcurrentHashMap<>();
    private static final int PORTAL_SEGMENTS = 16;
    private static final int SHOCKWAVE_SEGMENTS = 20;

    public static void handleSovereignSync(int playerId, boolean active, Vec3 anchor, int totalTicks, ItemStack castItem) {
        if (active) {
            ACTIVE_SOVEREIGNS.put(playerId, new ClientSovereignData(anchor, System.currentTimeMillis(), totalTicks, castItem));
        } else {
            ACTIVE_SOVEREIGNS.remove(playerId);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_SOVEREIGNS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        long now = System.currentTimeMillis();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        ItemRenderer itemRenderer = mc.getItemRenderer();

        for (Map.Entry<Integer, ClientSovereignData> entry : ACTIVE_SOVEREIGNS.entrySet()) {
            int playerId = entry.getKey();
            ClientSovereignData data = entry.getValue();
            long elapsed = now - data.startTime;

            if (elapsed > 5500) {
                ACTIVE_SOVEREIGNS.remove(playerId);
                continue;
            }

            ItemStack castItem = data.castItem;
            boolean hasWeapon = castItem != null && !castItem.isEmpty();

            ItemStack[] fallbackSwords = new ItemStack[]{
                    new ItemStack(Items.NETHERITE_SWORD),
                    new ItemStack(Items.DIAMOND_SWORD),
                    new ItemStack(Items.GOLDEN_SWORD),
                    new ItemStack(Items.IRON_SWORD),
                    new ItemStack(Items.STONE_SWORD)
            };

            Random rand = new Random(playerId * 7777L);

            Vec3[] skyPortals = new Vec3[24];
            Vec3[] groundPoints = new Vec3[24];

            // ── 1. RENDER 24 INDEPENDENT 3D SPATIAL VORTEX SKY PORTALS (PROPER DEPTH TEST) ──────
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);

            for (int p = 0; p < 24; p++) {
                double angle = p * (Math.PI * 2.0D / 24.0D) + (rand.nextDouble() - 0.5D) * 0.3D;
                double dist = 2.5D + (p % 4) * 3.2D + rand.nextDouble() * 1.5D;
                double rx = Math.cos(angle) * dist;
                double rz = Math.sin(angle) * dist;

                // Multi-tiered vertical portal heights for deep 3D volumetric feel (4.5 to 14.5 blocks above anchor)
                double ry = 4.5D + ((p * 5) % 8) * 1.35D + (rand.nextDouble() - 0.5D) * 0.6D;

                Vec3 skyPos = new Vec3(data.anchor.x + rx, data.anchor.y + ry, data.anchor.z + rz);
                skyPortals[p] = skyPos;

                // Exact Ground Floor Raycast Alignment
                Vec3 gStart = new Vec3(skyPos.x, data.anchor.y + 4.0D, skyPos.z);
                Vec3 gEnd = new Vec3(skyPos.x, data.anchor.y - 30.0D, skyPos.z);
                BlockHitResult hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                        gStart, gEnd, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player
                ));
                double floorY = (hit.getType() != HitResult.Type.MISS) ? hit.getLocation().y : data.anchor.y;
                groundPoints[p] = new Vec3(skyPos.x, floorY, skyPos.z);

                // ── STAGGERED PORTAL LIFECYCLE (OPEN INTRO -> ACTIVE -> CLOSE OUTRO A DESTIEMPO) ──
                long launchTime = 250 + (p * 80L);
                long openStart = Math.max(0L, launchTime - 300L);
                long closeStart = launchTime + 200L + 1500L + 100L; // Closes after weapon impact & stick phase
                long closeEnd = closeStart + 400L; // 400ms smooth closing exit animation

                if (elapsed >= openStart && elapsed < closeEnd) {
                    float portalScale = 1.0F;
                    float portalAlpha = 1.0F;

                    if (elapsed < launchTime) {
                        // Intro opening animation (scale 0 -> 1)
                        float introProgress = (elapsed - openStart) / 300.0F;
                        portalScale = (float) Math.sin(Math.min(1.0F, Math.max(0.0F, introProgress)) * Math.PI / 2.0D);
                        portalAlpha = Math.min(1.0F, Math.max(0.0F, introProgress));
                    } else if (elapsed >= closeStart) {
                        // Outro closing exit animation (scale 1 -> 0, staggered a destiempo)
                        float outroProgress = (elapsed - closeStart) / 400.0F;
                        portalScale = (float) Math.cos(Math.min(1.0F, Math.max(0.0F, outroProgress)) * Math.PI / 2.0D);
                        portalAlpha = 1.0F - Math.min(1.0F, Math.max(0.0F, outroProgress));
                    }

                    renderHighQualitySpatialPortal(tesselator, buffer, matrix, skyPos, now, p, portalScale, portalAlpha);
                }
            }

            // Re-enable depth test for ground weapons & explosions so they collide properly with terrain
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);

            // ── 2. WEAPON TRAVEL, STAGGERED GROUND STICK & INDIVIDUAL EXPLOSIONS ──
            for (int w = 0; w < 24; w++) {
                long launchTime = 250 + (w * 65L); // Staggered launch: 250ms to 1745ms
                if (elapsed < launchTime) continue;

                ItemStack weaponToRender = hasWeapon ? castItem.copy() : fallbackSwords[w % fallbackSwords.length];

                Vec3 skyPos = skyPortals[w];
                Vec3 groundPos = groundPoints[w];

                long travelAge = elapsed - launchTime;
                long travelDuration = 200L; // 200ms downward travel
                float travelProgress = Math.min(1.0F, travelAge / (float) travelDuration);

                Vec3 currentPos = skyPos.add(groundPos.subtract(skyPos).scale(travelProgress));

                long stickDuration = 1500L; // 1.5 seconds (1500 ms) ground stick duration!
                long stickTime = launchTime + travelDuration;

                // Trigger client particle burst on impact
                if (elapsed >= stickTime && !data.explodedFlags[w]) {
                    data.explodedFlags[w] = true;
                    spawnGoldenImpactParticles(mc.level, groundPos);
                }

                // Render stuck weapon during downward travel and ground stick phase
                if (elapsed < stickTime + stickDuration) {
                    poseStack.pushPose();
                    
                    // Position weapon along travel trajectory down to floor
                    poseStack.translate(currentPos.x, currentPos.y, currentPos.z);

                    // ── INTELLIGENT TIP-FIRST BLADE ORIENTATION ──────────
                    // 1. Random yaw angle around Y axis for dynamic directional variation (0..360 degrees)
                    float randomYaw = (w * 47.0F) % 360.0F;
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(randomYaw));

                    // 2. Slight 15-25 degree impale tilt relative to ground
                    float impaleTilt = 15.0F + (w % 3) * 5.0F;
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(impaleTilt));

                    // 3. Align 45-degree diagonal texture/model blade vertically
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-45.0F));

                    // 4. Flip 180 degrees around X so the blade TIP points straight DOWN into floor
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));

                    // 5. Shift pivot from handle origin to the blade TIP contact point
                    poseStack.translate(-0.35D, -0.35D, 0.0D);

                    // 6. Scale weapon model
                    poseStack.scale(1.3F, 1.3F, 1.3F);

                    // Render weapon model with immediate batch flush for 100% compatibility with all mod & vanilla items
                    try {
                        itemRenderer.renderStatic(
                                weaponToRender,
                                ItemDisplayContext.FIXED,
                                15728880,
                                OverlayTexture.NO_OVERLAY,
                                poseStack,
                                mc.renderBuffers().bufferSource(),
                                mc.level,
                                0
                        );
                        mc.renderBuffers().bufferSource().endBatch();
                    } catch (Throwable t) {
                        // Fallback to vanilla sword if custom mod item renderer throws internal exception
                        try {
                            itemRenderer.renderStatic(
                                    fallbackSwords[w % fallbackSwords.length],
                                    ItemDisplayContext.FIXED,
                                    15728880,
                                    OverlayTexture.NO_OVERLAY,
                                    poseStack,
                                    mc.renderBuffers().bufferSource(),
                                    mc.level,
                                    0
                            );
                            mc.renderBuffers().bufferSource().endBatch();
                        } catch (Throwable ignored) {}
                    }

                    poseStack.popPose();
                }

                // ── 3. INDIVIDUAL STAGGERED GOLDEN SPATIAL EXPLOSION FOR WEAPON W ──
                if (elapsed >= stickTime && elapsed <= stickTime + stickDuration) {
                    float expProgress = (elapsed - stickTime) / (float) stickDuration;
                    
                    // RE-BIND SHADER & RENDER STATES (Proper depth test prevents X-raying through grass/blocks)
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
                    RenderSystem.depthMask(false);
                    RenderSystem.disableCull();
                    RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

                    renderIndividualGoldenExplosion(tesselator, buffer, matrix, groundPos, skyPos, expProgress);
                }
            }
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void spawnGoldenImpactParticles(ClientLevel level, Vec3 pos) {
        if (level == null) return;
        Random rand = new Random();
        for (int i = 0; i < 8; i++) {
            double vx = (rand.nextDouble() - 0.5D) * 0.3D;
            double vy = 0.10D + rand.nextDouble() * 0.25D;
            double vz = (rand.nextDouble() - 0.5D) * 0.3D;
            level.addParticle(ParticleTypes.GLOW, pos.x, pos.y + 0.2D, pos.z, vx, vy, vz);
        }
    }

    private static void renderHighQualitySpatialPortal(Tesselator tesselator, BufferBuilder buffer, Matrix4f matrix, Vec3 center, long now, int idx, float portalScale, float portalAlpha) {
        if (portalScale <= 0.001F || portalAlpha <= 0.001F) return;

        float rotAngle = (float) (((now + idx * 250) % 3600) / 3600.0D * Math.PI * 2.0D);

        // Ring 1: Outer Rotating Spatial Distortion Ring (#FF8800)
        double r1Outer = (1.15D + 0.05D * Math.sin(rotAngle * 2.0D)) * portalScale;
        double r1Inner = 0.80D * portalScale;
        int a1Outer = (int) (140 * portalAlpha);
        int a1Inner = (int) (220 * portalAlpha);

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int s = 0; s <= PORTAL_SEGMENTS; s++) {
            double theta = (2.0D * Math.PI * s / PORTAL_SEGMENTS) + rotAngle;
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);

            double xO = center.x + cosT * r1Outer;
            double zO = center.z + sinT * r1Outer;

            double xI = center.x + cosT * r1Inner;
            double zI = center.z + sinT * r1Inner;

            buffer.vertex(matrix, (float) xO, (float) center.y, (float) zO).color(255, 136, 0, a1Outer).endVertex();
            buffer.vertex(matrix, (float) xI, (float) center.y, (float) zI).color(255, 215, 0, a1Inner).endVertex();
        }
        tesselator.end();

        // Ring 2: Middle Golden Plasma Wave Ring (#FFD700)
        double r2Outer = 0.80D * portalScale;
        double r2Inner = 0.35D * portalScale;
        int a2Outer = (int) (220 * portalAlpha);
        int a2Inner = (int) (255 * portalAlpha);

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int s = 0; s <= PORTAL_SEGMENTS; s++) {
            double theta = (2.0D * Math.PI * s / PORTAL_SEGMENTS) - rotAngle * 1.5D;
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);

            double xO = center.x + cosT * r2Outer;
            double zO = center.z + sinT * r2Outer;

            double xI = center.x + cosT * r2Inner;
            double zI = center.z + sinT * r2Inner;

            buffer.vertex(matrix, (float) xO, (float) center.y, (float) zO).color(255, 215, 0, a2Outer).endVertex();
            buffer.vertex(matrix, (float) xI, (float) center.y, (float) zI).color(255, 255, 255, a2Inner).endVertex();
        }
        tesselator.end();
    }

    private static void renderIndividualGoldenExplosion(Tesselator tesselator, BufferBuilder buffer, Matrix4f matrix, Vec3 impactPos, Vec3 skyPos, float progress) {
        float alphaF = (1.0F - progress);
        int alpha = (int) (alphaF * 240);
        if (alpha <= 0) return;

        // 1. VOLUMETRIC 8-SIDED GOLDEN ENERGY BEAM (SKY RIFT TO IMPACT POINT)
        double beamRadius = 0.45D * (1.0D + progress * 0.5D);
        double beamHeight = skyPos.y - impactPos.y;

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int s = 0; s <= 8; s++) {
            double theta = 2.0D * Math.PI * s / 8;
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);

            double x = impactPos.x + cosT * beamRadius;
            double z = impactPos.z + sinT * beamRadius;

            buffer.vertex(matrix, (float) x, (float) impactPos.y, (float) z).color(255, 215, 0, alpha).endVertex();
            buffer.vertex(matrix, (float) x, (float) (impactPos.y + beamHeight), (float) z).color(255, 255, 255, alpha).endVertex();
        }
        tesselator.end();

        // 2. DUAL CONCENTRIC EXPANDING GOLDEN SHOCKWAVE RINGS ON TERRAIN
        double shockRadius1 = 0.5D + progress * 3.5D;
        double shockRadius2 = 0.2D + progress * 2.0D;

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int s = 0; s <= SHOCKWAVE_SEGMENTS; s++) {
            double theta = 2.0D * Math.PI * s / SHOCKWAVE_SEGMENTS;
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);

            double x1 = impactPos.x + cosT * shockRadius1;
            double z1 = impactPos.z + sinT * shockRadius1;

            double x2 = impactPos.x + cosT * shockRadius2;
            double z2 = impactPos.z + sinT * shockRadius2;

            buffer.vertex(matrix, (float) x1, (float) (impactPos.y + 0.12D), (float) z1).color(255, 215, 0, alpha).endVertex();
            buffer.vertex(matrix, (float) x2, (float) (impactPos.y + 0.12D), (float) z2).color(255, 255, 255, alpha).endVertex();
        }
        tesselator.end();
    }

    public static class ClientSovereignData {
        public final Vec3 anchor;
        public final long startTime;
        public final int totalTicks;
        public final ItemStack castItem;
        public final boolean[] explodedFlags = new boolean[24];

        public ClientSovereignData(Vec3 anchor, long startTime, int totalTicks, ItemStack castItem) {
            this.anchor = anchor;
            this.startTime = startTime;
            this.totalTicks = totalTicks;
            this.castItem = castItem != null ? castItem.copy() : ItemStack.EMPTY;
        }
    }
}
