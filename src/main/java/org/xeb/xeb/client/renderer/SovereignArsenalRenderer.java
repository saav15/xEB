package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
 * 1. 3D Multi-Ring Golden Spatial Vortex Sky Portals with counter-rotating rings and white-hot cores.
 * 2. Downward Travel Phase with blade-first ground pointing & exact terrain floor stick.
 * 3. Volumetric 8-Sided Golden Energy Beams + Dual Concentric Expanding Shockwave Rings.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SovereignArsenalRenderer {

    public static final Map<Integer, ClientSovereignData> ACTIVE_SOVEREIGNS = new ConcurrentHashMap<>();
    private static final int PORTAL_SEGMENTS = 16;
    private static final int SHOCKWAVE_SEGMENTS = 20;

    public static void handleSovereignSync(int playerId, boolean active, Vec3 anchor, int totalTicks) {
        if (active) {
            ACTIVE_SOVEREIGNS.put(playerId, new ClientSovereignData(anchor, System.currentTimeMillis(), totalTicks));
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
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
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

            if (elapsed > 3200) {
                ACTIVE_SOVEREIGNS.remove(playerId);
                continue;
            }

            Entity playerEnt = mc.level.getEntity(playerId);
            if (!(playerEnt instanceof LivingEntity living)) continue;

            ItemStack mainhand = living.getMainHandItem();
            boolean hasWeapon = !mainhand.isEmpty();

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

            // ── 1. RENDER 24 HIGH-FIDELITY 3D SPATIAL VORTEX SKY PORTALS ──────
            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            for (int p = 0; p < 24; p++) {
                double rx = (rand.nextDouble() - 0.5D) * 18.0D;
                double ry = 13.0D + rand.nextDouble() * 4.0D;
                double rz = (rand.nextDouble() - 0.5D) * 18.0D;

                Vec3 skyPos = data.anchor.add(rx, ry, rz);
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

                // Multi-Ring Golden Spatial Vortex Mesh (#FFD700 & #FFFFFF)
                renderHighQualitySpatialPortal(buffer, matrix, skyPos, now, p);
            }
            tesselator.end();

            // ── 2. WEAPON TRAVEL & EXACT GROUND STICK RENDER ──────────────────
            if (elapsed > 500 && elapsed < 2800) {
                for (int w = 0; w < 24; w++) {
                    long launchTime = 500 + (w * 40L);
                    if (elapsed < launchTime) continue;

                    ItemStack weaponToRender = hasWeapon ? mainhand : fallbackSwords[w % fallbackSwords.length];

                    Vec3 skyPos = skyPortals[w];
                    Vec3 groundPos = groundPoints[w];

                    long travelAge = elapsed - launchTime;
                    float travelProgress = Math.min(1.0F, travelAge / 200.0F); // 200ms downward travel

                    Vec3 currentPos = skyPos.add(groundPos.subtract(skyPos).scale(travelProgress));

                    poseStack.pushPose();
                    poseStack.translate(currentPos.x, currentPos.y, currentPos.z);

                    // Blade points straight down toward floor
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(w * 25.0F));
                    poseStack.scale(1.3F, 1.3F, 1.3F);

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

                    poseStack.popPose();
                }
            }

            // ── 3. VOLUMETRIC GOLDEN SPATIAL EXPLOSION ENGINE ────────────────
            if (elapsed >= 2400 && elapsed <= 3000) {
                float expProgress = (elapsed - 2400) / 600.0F;

                buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                for (int w = 0; w < 24; w++) {
                    Vec3 groundPos = groundPoints[w];
                    Vec3 skyPos = skyPortals[w];
                    renderVolumetricGoldenExplosion(buffer, matrix, groundPos, skyPos, expProgress);
                }
                tesselator.end();
            }
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderHighQualitySpatialPortal(BufferBuilder buffer, Matrix4f matrix, Vec3 center, long now, int idx) {
        float rotAngle = (float) (((now + idx * 250) % 3600) / 3600.0D * Math.PI * 2.0D);

        // Ring 1: Outer Rotating Spatial Distortion Ring (#FF8800)
        double r1Outer = 1.15D + 0.05D * Math.sin(rotAngle * 2.0D);
        double r1Inner = 0.80D;
        for (int s = 0; s <= PORTAL_SEGMENTS; s++) {
            double theta = (2.0D * Math.PI * s / PORTAL_SEGMENTS) + rotAngle;
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);

            double xO = center.x + cosT * r1Outer;
            double zO = center.z + sinT * r1Outer;

            double xI = center.x + cosT * r1Inner;
            double zI = center.z + sinT * r1Inner;

            buffer.vertex(matrix, (float) xO, (float) center.y, (float) zO).color(255, 136, 0, 140).endVertex();
            buffer.vertex(matrix, (float) xI, (float) center.y, (float) zI).color(255, 215, 0, 220).endVertex();
        }

        // Ring 2: Middle Golden Plasma Wave Ring (#FFD700)
        double r2Outer = 0.80D;
        double r2Inner = 0.35D;
        for (int s = 0; s <= PORTAL_SEGMENTS; s++) {
            double theta = (2.0D * Math.PI * s / PORTAL_SEGMENTS) - rotAngle * 1.5D;
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);

            double xO = center.x + cosT * r2Outer;
            double zO = center.z + sinT * r2Outer;

            double xI = center.x + cosT * r2Inner;
            double zI = center.z + sinT * r2Inner;

            buffer.vertex(matrix, (float) xO, (float) center.y, (float) zO).color(255, 215, 0, 220).endVertex();
            buffer.vertex(matrix, (float) xI, (float) center.y, (float) zI).color(255, 255, 255, 255).endVertex();
        }
    }

    private static void renderVolumetricGoldenExplosion(BufferBuilder buffer, Matrix4f matrix, Vec3 impactPos, Vec3 skyPos, float progress) {
        float alphaF = (1.0F - progress);
        int alpha = (int) (alphaF * 240);
        if (alpha <= 0) return;

        // 1. Volumetric 8-Sided Golden Energy Cylinder Beam (Sky Rift to Impact Point)
        double beamRadius = 0.45D * (1.0D + progress * 0.5D);
        double beamHeight = skyPos.y - impactPos.y;

        for (int s = 0; s <= 8; s++) {
            double theta = 2.0D * Math.PI * s / 8;
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);

            double x = impactPos.x + cosT * beamRadius;
            double z = impactPos.z + sinT * beamRadius;

            buffer.vertex(matrix, (float) x, (float) impactPos.y, (float) z).color(255, 215, 0, alpha).endVertex();
            buffer.vertex(matrix, (float) x, (float) (impactPos.y + beamHeight), (float) z).color(255, 255, 255, alpha).endVertex();
        }

        // 2. Dual Concentric Expanding Golden Shockwave Rings
        double shockRadius1 = 0.5D + progress * 3.5D;
        double shockRadius2 = 0.2D + progress * 2.0D;

        for (int s = 0; s <= SHOCKWAVE_SEGMENTS; s++) {
            double theta = 2.0D * Math.PI * s / SHOCKWAVE_SEGMENTS;
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);

            double x1 = impactPos.x + cosT * shockRadius1;
            double z1 = impactPos.z + sinT * shockRadius1;

            double x2 = impactPos.x + cosT * shockRadius2;
            double z2 = impactPos.z + sinT * shockRadius2;

            buffer.vertex(matrix, (float) x1, (float) (impactPos.y + 0.15D), (float) z1).color(255, 215, 0, alpha).endVertex();
            buffer.vertex(matrix, (float) x2, (float) (impactPos.y + 0.15D), (float) z2).color(255, 255, 255, alpha).endVertex();
        }
    }

    public static class ClientSovereignData {
        public final Vec3 anchor;
        public final long startTime;
        public final int totalTicks;

        public ClientSovereignData(Vec3 anchor, long startTime, int totalTicks) {
            this.anchor = anchor;
            this.startTime = startTime;
            this.totalTicks = totalTicks;
        }
    }
}
