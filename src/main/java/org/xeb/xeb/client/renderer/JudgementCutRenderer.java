package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;

import java.util.Map;
import java.util.Random;

/**
 * Ultra-High Quality DMC Vergil-style Crescent Slash Arc Blade Renderer for Judgement Cut End.
 *
 * <p>Fixes:
 * 1. Additive Blending + depthMask(false): Eliminates all moiré z-fighting zebra patterns and stippling artifacts.
 * 2. Sleek Katana Crescent Blade Dimensions: Tapered razor-thin arc blades (0.18m wide) instead of fat 3D shapes.
 * 3. Mainhand Weapon RGB Color Inheritance: Dynamically derives colors for ANY weapon from any mod.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class JudgementCutRenderer {

    private static final int TOTAL_SLASHES = 36;
    private static final int ARC_SEGMENTS = 12;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (JudgementDomeRenderer.ACTIVE_DOMES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        long now = System.currentTimeMillis();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        // Additive Blending without Depth Mask Writing -> Pure Additive Glowing Energy Slashes (No Z-Fighting Moiré!)
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        for (Map.Entry<Integer, JudgementDomeRenderer.ClientJudgementData> entry : JudgementDomeRenderer.ACTIVE_DOMES.entrySet()) {
            int playerId = entry.getKey();
            JudgementDomeRenderer.ClientJudgementData dome = entry.getValue();
            long elapsed = now - dome.startTime;

            if (elapsed < 750 || elapsed > 2400) continue;

            long phaseTime = elapsed - 750;
            Vec3 anchor = dome.anchor;
            Random rand = new Random(playerId * 9999L);

            // ── Mainhand Weapon RGB Color Inheritance ────────────────────────
            int[] wRGB = new int[]{0, 229, 255};
            Entity playerEnt = mc.level.getEntity(playerId);
            if (playerEnt instanceof LivingEntity living) {
                wRGB = getWeaponRGBColor(living.getMainHandItem());
            }

            int wR = wRGB[0];
            int wG = wRGB[1];
            int wB = wRGB[2];

            // ── Progressive Sequential Spawn Window ──────────────────────────
            int activeSlashCount;
            if (phaseTime < 300) {
                activeSlashCount = 6 + (int) ((phaseTime / 300.0D) * 6);
            } else if (phaseTime < 700) {
                activeSlashCount = 12 + (int) (((phaseTime - 300) / 400.0D) * 12);
            } else {
                activeSlashCount = TOTAL_SLASHES;
            }

            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i < activeSlashCount; i++) {
                double rx = (rand.nextDouble() - 0.5D) * 20.0D;
                double ry = (rand.nextDouble() - 0.4D) * 12.0D;
                double rz = (rand.nextDouble() - 0.5D) * 20.0D;

                Vec3 slashCenter = anchor.add(rx, ry + 1.5D, rz);

                double pitch = (rand.nextDouble() - 0.5D) * Math.PI;
                double yaw = (rand.nextDouble() - 0.5D) * Math.PI * 2.0D;
                double roll = (rand.nextDouble() - 0.5D) * Math.PI;

                Vec3 dir = new Vec3(
                        Math.cos(pitch) * Math.cos(yaw),
                        Math.sin(pitch),
                        Math.cos(pitch) * Math.sin(yaw)
                ).normalize();

                Vec3 u = dir.cross(new Vec3(0, 1, 0));
                if (u.lengthSqr() < 0.001D) u = dir.cross(new Vec3(1, 0, 0));
                u = u.normalize();
                Vec3 v = dir.cross(u).normalize();

                double cosR = Math.cos(roll);
                double sinR = Math.sin(roll);
                Vec3 normalU = u.scale(cosR).add(v.scale(sinR));
                Vec3 normalV = u.scale(-sinR).add(v.scale(cosR));

                long slashSpawnDelay = (i * 35L);
                long slashAge = phaseTime - slashSpawnDelay;
                if (slashAge < 0) continue;

                // Slice unfurl animation from top to bottom (0.0 to 1.0 over 120ms)
                float unfurl = Math.min(1.0F, slashAge / 120.0F);

                double arcLength = (3.5D + rand.nextDouble() * 2.0D) * unfurl;
                double baseWidth = 0.16D + rand.nextDouble() * 0.08D; // Sleek razor-thin blade

                // Outer Weapon Energy Aura
                renderCrescentBladeLayer(buffer, matrix, slashCenter, dir, normalV, normalU, arcLength, baseWidth * 2.2D, wR, wG, wB, 120);
                // Inner White-Hot Core Blade
                renderCrescentBladeLayer(buffer, matrix, slashCenter, dir, normalV, normalU, arcLength, baseWidth * 0.7D, 255, 255, 255, 255);
            }

            tesselator.end();
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderCrescentBladeLayer(BufferBuilder buffer, Matrix4f matrix,
                                                 Vec3 center, Vec3 dir, Vec3 widthVec, Vec3 curveVec,
                                                 double arcLength, double maxWidth,
                                                 int r, int g, int b, int alpha) {
        double halfLen = arcLength * 0.5D;

        for (int seg = 0; seg < ARC_SEGMENTS; seg++) {
            double frac1 = seg / (double) ARC_SEGMENTS;
            double frac2 = (seg + 1) / (double) ARC_SEGMENTS;

            double pos1 = -halfLen + (arcLength * frac1);
            double pos2 = -halfLen + (arcLength * frac2);

            // Needle-sharp tapering at both tips (0 at tip, 1.0 in middle)
            double taper1 = Math.sin(frac1 * Math.PI);
            double taper2 = Math.sin(frac2 * Math.PI);

            double w1 = maxWidth * taper1;
            double w2 = maxWidth * taper2;

            // Subtle curved arc offset
            double curve1 = (1.0D - Math.cos((frac1 - 0.5D) * Math.PI)) * 0.4D;
            double curve2 = (1.0D - Math.cos((frac2 - 0.5D) * Math.PI)) * 0.4D;

            Vec3 p1 = center.add(dir.scale(pos1)).add(curveVec.scale(curve1));
            Vec3 p2 = center.add(dir.scale(pos2)).add(curveVec.scale(curve2));

            Vec3 offset1 = widthVec.scale(w1);
            Vec3 offset2 = widthVec.scale(w2);

            Vec3 vA1 = p1.add(offset1);
            Vec3 vA2 = p1.subtract(offset1);
            Vec3 vB1 = p2.add(offset2);
            Vec3 vB2 = p2.subtract(offset2);

            // Quad face
            buffer.vertex(matrix, (float) vA1.x, (float) vA1.y, (float) vA1.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, (float) vA2.x, (float) vA2.y, (float) vA2.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, (float) vB2.x, (float) vB2.y, (float) vB2.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, (float) vB1.x, (float) vB1.y, (float) vB1.z).color(r, g, b, alpha).endVertex();
        }
    }

    public static int[] getWeaponRGBColor(ItemStack stack) {
        if (stack.isEmpty()) return new int[]{0, 229, 255}; // Default Vergil Azure

        Item item = stack.getItem();

        // 1. Explicit Hand-Crafted xEB Special Weapon Overrides
        if (item == ModItems.DOOMFIST.get() || item == ModItems.DOOMFIST_V2.get()) {
            return new int[]{255, 136, 0}; // Orange Gold
        } else if (item == ModItems.OPTIC_BLAST.get()) {
            return new int[]{255, 17, 34}; // Ruby Red
        } else if (item == ModItems.GOLDEN_FLOWER.get()) {
            return new int[]{255, 215, 0}; // Solar Gold
        } else if (item == ModItems.THE_TEARS.get()) {
            return new int[]{255, 0, 68}; // Brimstone Crimson
        } else if (item == ModItems.HOLY_DUALITY_BLADE.get()) {
            return new int[]{0, 240, 255}; // Holy Cyan
        } else if (item == ModItems.SMART_HALBERD.get()) {
            return new int[]{0, 255, 255}; // Cyber Aqua
        } else if (item == ModItems.BROKEN_DIAMOND.get()) {
            return new int[]{68, 204, 255}; // Diamond Blue
        } else if (item == ModItems.MECHA_OVERDRIVE.get()) {
            return new int[]{170, 0, 255}; // Overdrive Violet
        }

        // 2. Minecraft ItemColor Tint Query (Works for dyed or tinted modded items)
        try {
            int tintColor = Minecraft.getInstance().getItemColors().getColor(stack, 0);
            if (tintColor != -1 && tintColor != 0) {
                int red = (tintColor >> 16) & 0xFF;
                int green = (tintColor >> 8) & 0xFF;
                int blue = tintColor & 0xFF;

                if (red > 15 || green > 15 || blue > 15) {
                    return new int[]{red, green, blue};
                }
            }
        } catch (Exception ignored) {}

        // 3. Universal Dynamic Modded Weapon HSL Registry Hash
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        if (key != null) {
            String path = key.toString().toLowerCase(java.util.Locale.ROOT);

            if (path.contains("diamond")) return new int[]{85, 255, 255};
            if (path.contains("netherite")) return new int[]{180, 25, 45};
            if (path.contains("gold") || path.contains("gilded")) return new int[]{255, 221, 0};
            if (path.contains("iron")) return new int[]{220, 220, 230};
            if (path.contains("ruby")) return new int[]{255, 20, 50};
            if (path.contains("emerald")) return new int[]{0, 255, 120};
            if (path.contains("copper")) return new int[]{220, 120, 70};
            if (path.contains("amethyst")) return new int[]{170, 80, 255};
            if (path.contains("void") || path.contains("dark") || path.contains("shadow")) return new int[]{140, 0, 220};
            if (path.contains("fire") || path.contains("flame") || path.contains("magma")) return new int[]{255, 80, 0};
            if (path.contains("ice") || path.contains("frost")) return new int[]{120, 220, 255};

            int hash = Math.abs(key.toString().hashCode());
            float hue = (hash % 360) / 360.0F;
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.85F, 1.0F);

            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            return new int[]{r, g, b};
        }

        return new int[]{0, 229, 255}; // Default Vergil Azure
    }
}
