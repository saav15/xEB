package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class MedallionRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation BRONZE_TEXTURE = new ResourceLocation(Xeb.MODID, "textures/entity/medallion_bronze.png");
    private static final ResourceLocation SILVER_TEXTURE = new ResourceLocation(Xeb.MODID, "textures/entity/medallion_silver.png");
    private static final ResourceLocation GOLD_TEXTURE = new ResourceLocation(Xeb.MODID, "textures/entity/medallion_gold.png");
    private static final ResourceLocation WHITE_TEX = new ResourceLocation(Xeb.MODID, "textures/entity/white.png");

    /**
     * N12 fix — MedallionModel bakes all ModelParts in its constructor.
     * Previously {@link #renderMedallionsStatic} created {@code new MedallionRenderLayer(null)}
     * on every frame call (~60 fps × every tracked entity), triggering ~1200+ bake ops/s in
     * combat.  A single static instance is shared instead.
     */
    private static final MedallionModel SHARED_MODEL = new MedallionModel();

    private final MedallionModel model = SHARED_MODEL;

    public MedallionRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    public static float getMedallionWorldHeight(LivingEntity entity) {
        float height = entity.getBbHeight();
        String typeId = net.minecraft.world.entity.EntityType.getKey(entity.getType()).toString();
        
        switch (typeId) {
            case "minecraft:ghast":
                return 4.6F;
            case "minecraft:wither":
                return 2.7F;
            case "minecraft:ender_dragon":
                return 8.5F;
            case "minecraft:giant":
                return 13.5F;
            case "xeb:elite_fly":
                return height + 0.20F;
            case "minecraft:spider":
                return height + 0.25F;
            case "minecraft:cave_spider":
                return height + 0.20F;
        }
        
        // Proportional floating gap based on height ranges
        float baseFloat;
        if (height > 3.0F) {
            baseFloat = 0.65F; // Larger bosses/monsters (e.g. Cataclysm, Twilight)
        } else if (height > 1.2F) {
            baseFloat = 0.45F; // Regular size mobs (humanoids, animals)
        } else {
            baseFloat = 0.30F; // Tiny/short mobs
        }
        
        // Dynamic scaling: height + base float + 8% scaling factor
        return height + baseFloat + (height * 0.08F);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        if (medallions.isEmpty()) return;

        if (!org.xeb.xeb.Config.medallionRenderEnabled) return;

        // Render distance check (configurable via render.medallionRenderDistance)
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            double distSq = entity.distanceToSqr(mc.player);
            double maxDist = org.xeb.xeb.Config.medallionRenderDistance;
            if (distSq > maxDist * maxDist) {
                return;
            }
        }

        // Calculate the current scale of the PoseStack to undo any parent scaling (like for baby zombies or giants)
        org.joml.Vector3f dir = new org.joml.Vector3f(0.0F, 1.0F, 0.0F);
        poseStack.last().pose().transformDirection(dir);
        float scale = dir.length();
        if (scale < 0.001F) scale = 1.0F;
        boolean isYInverted = dir.y < 0.0F;

        // Count mega medallions for extra offset
        int megaCount = 0;
        for (MedallionData m : medallions) {
            if (m.getBuff().getId().equals("mega")) {
                megaCount++;
            }
        }

        float worldHeight = getMedallionWorldHeight(entity);

        // In vanilla LivingEntityRenderer, the model is translated by -1.501F blocks.
        // So the current origin of the PoseStack is already 1.501F blocks above the feet.
        // We subtract 1.501F blocks to get the correct offset from the current origin.
        float heightOffset = (worldHeight / scale) - 1.501F;

        poseStack.pushPose();
        poseStack.translate(0.0F, -heightOffset, 0.0F);

        renderAtPose(poseStack, bufferSource, packedLight, entity, partialTick);
        poseStack.popPose();
    }


    /**
     * Static entry point for rendering medallions without needing a layer parent.
     * Called by ClientMedallionEventHandler for entities whose renderer has no MedallionRenderLayer.
     * The caller is responsible for translating the PoseStack to the correct height before calling this.
     */
    public static void renderMedallionsStatic(PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, LivingEntity entity, float partialTick) {
        // N12 fix — reuse the shared static instance instead of allocating a new
        // MedallionRenderLayer (which bakes all ModelParts) on every frame.
        @SuppressWarnings({"unchecked", "rawtypes"})
        MedallionRenderLayer instance = new MedallionRenderLayer(null); // lightweight — model field now points to SHARED_MODEL
        instance.renderAtPose(poseStack, bufferSource, packedLight, entity, partialTick);
    }

    public void renderAtPose(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, LivingEntity entity, float partialTick) {
        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        if (medallions.isEmpty()) return;

        int count = medallions.size();
        float time = (entity.tickCount + partialTick);
        double bob = Math.sin(time / 10.0D) * 0.08D;

        // Calculate the current scale of the PoseStack to undo any parent scaling
        org.joml.Vector3f dir = new org.joml.Vector3f(0.0F, 1.0F, 0.0F);
        poseStack.last().pose().transformDirection(dir);
        float scale = dir.length();
        if (scale < 0.001F) scale = 1.0F;

        // Base medallion scale in world coordinates
        float entityWidth = entity.getBbWidth();
        float speedMultiplier = 1.0F / (float) Math.max(0.5D, Math.sqrt(entityWidth / 0.6F));
        float rotationSpeed = 4.5F * speedMultiplier;
        float rotation = (time * rotationSpeed) % 360.0F;
        float targetWorldScale = 0.2F + 0.8F * entityWidth;
        targetWorldScale = Math.max(0.25F, Math.min(2.5F, targetWorldScale)) * 0.80F;

        // Spacing between multiple medallions in PoseStack coordinates
        float targetWorldSpacing = 0.55F * targetWorldScale;
        float spacing = targetWorldSpacing / scale;

        for (int i = 0; i < count; i++) {
            MedallionData m = medallions.get(i);
            poseStack.pushPose();

            // Translate horizontally to form a row
            float xOffset = (i - (count - 1) / 2.0F) * spacing;
            poseStack.translate(xOffset, bob, 0.0D);

            // Rotate
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

            // Scale to model units (1/16 of a block) to ensure consistent size and scaling with the mob
            float baseScale = (targetWorldScale / scale) * 0.0625F;
            poseStack.scale(baseScale, baseScale, baseScale);

            // 1. Render 3D Case using the tier-specific texture
            ResourceLocation caseTexture = switch (m.getTier()) {
                case COMMON -> BRONZE_TEXTURE;
                case RARE -> SILVER_TEXTURE;
                case LEGENDARY -> GOLD_TEXTURE;
            };
            VertexConsumer caseConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(caseTexture));
            poseStack.pushPose();
            poseStack.scale(16.0F, 16.0F, 16.0F);
            switch (m.getTier()) {
                case COMMON -> model.renderBronze(poseStack, caseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F);
                case RARE -> model.renderSilver(poseStack, caseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F);
                case LEGENDARY -> model.renderGold(poseStack, caseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F);
            }
            poseStack.popPose();

            // 2. Render gold shine wave & sparkle effects if it is a Gold (LEGENDARY) Case
            if (m.getTier() == MedallionType.LEGENDARY) {
                // Animate progress over 80 ticks (4 seconds)
                float progress = (time % 80.0F) / 80.0F;
                renderGoldShine(poseStack, bufferSource, packedLight, progress, time);
            }

            // 3. Dynamically load the unique medallion icon texture for this specific buff based on the tier subfolder
            String tierFolder = switch (m.getTier()) {
                case COMMON -> "bronze";
                case RARE -> "silver";
                case LEGENDARY -> "gold";
            };
            ResourceLocation medallionTexture = new ResourceLocation(Xeb.MODID, "textures/entity/medallion/" + tierFolder + "/icon_" + m.getBuff().getId() + ".png");

            // 4. Get buffer for Medallion and render the quads in model units (1/16)
            VertexConsumer medallionConsumer = bufferSource.getBuffer(RenderType.entityCutout(medallionTexture));
            PoseStack.Pose pose = poseStack.last();
            
            float innerSize = switch (m.getTier()) {
                case COMMON -> 2.5F;      // Bronze Octagon (fills the 5.0F wide opening)
                case RARE -> 2.0F;        // Silver Star (fills the 4.0F wide opening)
                case LEGENDARY -> 3.0F;   // Gold Diamond (fills the 6.0F wide opening)
            };

            // Icon in the center (Z = 0.0F, rendered once and visible from both sides due to noCull)
            drawQuad(pose, medallionConsumer, -innerSize, innerSize, -innerSize, innerSize, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, false);

            poseStack.popPose();
        }
    }

    private static void renderGoldShine(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float progress, float time) {
        VertexConsumer whiteConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
        PoseStack.Pose pose = poseStack.last();

        // A. Diagonal Shine Wave (progress 0.0 to 0.45)
        if (progress <= 0.45F) {
            float p = progress / 0.45F;
            float beamX = -7.0F + 14.0F * p; // Sweep from left to right
            float alpha = 0.5F * (1.0F - (float) Math.abs(beamX) / 7.0F); // Fade out near the edges
            
            // Draw diagonal highlight band on front (Z = -0.52) and back (Z = 0.52)
            drawShineBand(pose, whiteConsumer, beamX, -0.52F, alpha, packedLight, false);
            drawShineBand(pose, whiteConsumer, beamX, 0.52F, alpha, packedLight, true);
        }

        // B. Sparkle Flash (progress 0.45 to 0.65)
        if (progress > 0.45F && progress <= 0.65F) {
            float p = (progress - 0.45F) / 0.20F; // 0.0 to 1.0
            float flashScale = (float) Math.sin(p * Math.PI) * 2.0F; // Pulse size
            float rotation = time * 18.0F;

            // Front Sparkle (at top-right corner of the frame: x=2.2, y=-2.2)
            poseStack.pushPose();
            poseStack.translate(2.2F, -2.2F, -0.53F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
            poseStack.scale(flashScale, flashScale, 1.0F);
            
            PoseStack.Pose sparklePose = poseStack.last();
            drawQuad(sparklePose, whiteConsumer, -0.15F, 0.15F, -1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, packedLight, 1.0F, 1.0F, 1.0F, 0.8F, false);
            drawQuad(sparklePose, whiteConsumer, -1.0F, 1.0F, -0.15F, 0.15F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, packedLight, 1.0F, 1.0F, 1.0F, 0.8F, false);
            poseStack.popPose();

            // Back Sparkle (at back top-right corner, which is x=-2.2 viewed from behind)
            poseStack.pushPose();
            poseStack.translate(-2.2F, -2.2F, 0.53F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-rotation));
            poseStack.scale(flashScale, flashScale, 1.0F);
            
            sparklePose = poseStack.last();
            drawQuad(sparklePose, whiteConsumer, -0.15F, 0.15F, -1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, packedLight, 1.0F, 1.0F, 1.0F, 0.8F, true);
            drawQuad(sparklePose, whiteConsumer, -1.0F, 1.0F, -0.15F, 0.15F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, packedLight, 1.0F, 1.0F, 1.0F, 0.8F, true);
            poseStack.popPose();
        }
    }

    private static void drawShineBand(PoseStack.Pose pose, VertexConsumer consumer, float beamX, float z, float alpha, int packedLight, boolean isBack) {
        float normalZ = isBack ? 1.0F : -1.0F;
        float width = 0.8F;

        // Split into 2 quads to clip to diamond shape (limitX = 4.0F - |y|)
        float y1 = -4.0F;
        float y2 = 0.0F;
        float y3 = 4.0F;

        float limit1 = 0.0F;
        float limit2 = 4.0F;
        float limit3 = 0.0F;

        float cx1 = beamX - 0.4F;
        float cx2 = beamX;
        float cx3 = beamX + 0.4F;

        float l1 = Math.max(-limit1, Math.min(limit1, cx1 - width));
        float r1 = Math.max(-limit1, Math.min(limit1, cx1 + width));

        float l2 = Math.max(-limit2, Math.min(limit2, cx2 - width));
        float r2 = Math.max(-limit2, Math.min(limit2, cx2 + width));

        float l3 = Math.max(-limit3, Math.min(limit3, cx3 - width));
        float r3 = Math.max(-limit3, Math.min(limit3, cx3 + width));

        // Quad 1: y1 to y2
        consumer.vertex(pose.pose(), l1, y1, z).color(1.0F, 1.0F, 1.0F, alpha).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, normalZ).endVertex();
        consumer.vertex(pose.pose(), r1, y1, z).color(1.0F, 1.0F, 1.0F, alpha).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, normalZ).endVertex();
        consumer.vertex(pose.pose(), r2, y2, z).color(1.0F, 1.0F, 1.0F, alpha).uv(1.0F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, normalZ).endVertex();
        consumer.vertex(pose.pose(), l2, y2, z).color(1.0F, 1.0F, 1.0F, alpha).uv(0.0F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, normalZ).endVertex();

        // Quad 2: y2 to y3
        consumer.vertex(pose.pose(), l2, y2, z).color(1.0F, 1.0F, 1.0F, alpha).uv(0.0F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, normalZ).endVertex();
        consumer.vertex(pose.pose(), r2, y2, z).color(1.0F, 1.0F, 1.0F, alpha).uv(1.0F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, normalZ).endVertex();
        consumer.vertex(pose.pose(), r3, y3, z).color(1.0F, 1.0F, 1.0F, alpha).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, normalZ).endVertex();
        consumer.vertex(pose.pose(), l3, y3, z).color(1.0F, 1.0F, 1.0F, alpha).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, normalZ).endVertex();
    }

    private static void drawQuad(PoseStack.Pose pose, VertexConsumer consumer, float minX, float maxX, float minY, float maxY, float z, float minU, float maxU, float minV, float maxV, int packedLight, float r, float g, float b, float a, boolean isBack) {
        float normalZ = isBack ? 1.0F : -1.0F;
        
        // Vertex 1: Top-Left
        consumer.vertex(pose.pose(), minX, minY, z)
                .color(r, g, b, a)
                .uv(minU, minV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 0.0F, normalZ)
                .endVertex();
        // Vertex 2: Top-Right
        consumer.vertex(pose.pose(), maxX, minY, z)
                .color(r, g, b, a)
                .uv(maxU, minV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 0.0F, normalZ)
                .endVertex();
        // Vertex 3: Bottom-Right
        consumer.vertex(pose.pose(), maxX, maxY, z)
                .color(r, g, b, a)
                .uv(maxU, maxV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 0.0F, normalZ)
                .endVertex();
        // Vertex 4: Bottom-Left
        consumer.vertex(pose.pose(), minX, maxY, z)
                .color(r, g, b, a)
                .uv(minU, maxV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 0.0F, normalZ)
                .endVertex();
    }

    // Retained for compatibility but no longer used in render loop
    public static class MedallionModel extends Model {
        private final ModelPart bronzeFrame;
        private final ModelPart bronzeRibbon;
        private final ModelPart silverFrame;
        private final ModelPart silverRibbon;
        private final ModelPart goldFrame;
        private final ModelPart goldRibbon;

        public MedallionModel() {
            super(RenderType::entityCutoutNoCull);

            MeshDefinition mesh = new MeshDefinition();
            PartDefinition rootPart = mesh.getRoot();

            // 🥉 Bronze: Circular frame (octagon of 3D cubes)
            PartDefinition bronzeFrame = rootPart.addOrReplaceChild("bronzeFrame", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-1.65F, -3.5F, -0.8F, 3.3F, 1.0F, 1.6F)
                    .addBox(-1.65F, 2.5F, -0.8F, 3.3F, 1.0F, 1.6F)
                    .addBox(-3.5F, -1.65F, -0.8F, 1.0F, 3.3F, 1.6F)
                    .addBox(2.5F, -1.65F, -0.8F, 1.0F, 3.3F, 1.6F),
                    PartPose.ZERO);

            bronzeFrame.addOrReplaceChild("cornerTR", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5F, -1.3F, -0.79F, 1.0F, 2.6F, 1.58F),
                    PartPose.offsetAndRotation(2.575F, -2.575F, 0.0F, 0.0F, 0.0F, -0.7853982F));

            bronzeFrame.addOrReplaceChild("cornerBR", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5F, -1.3F, -0.79F, 1.0F, 2.6F, 1.58F),
                    PartPose.offsetAndRotation(2.575F, 2.575F, 0.0F, 0.0F, 0.0F, 0.7853982F));

            bronzeFrame.addOrReplaceChild("cornerBL", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5F, -1.3F, -0.79F, 1.0F, 2.6F, 1.58F),
                    PartPose.offsetAndRotation(-2.575F, 2.575F, 0.0F, 0.0F, 0.0F, -0.7853982F));

            bronzeFrame.addOrReplaceChild("cornerTL", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5F, -1.3F, -0.79F, 1.0F, 2.6F, 1.58F),
                    PartPose.offsetAndRotation(-2.575F, -2.575F, 0.0F, 0.0F, 0.0F, 0.7853982F));

            rootPart.addOrReplaceChild("bronzeRibbon", CubeListBuilder.create().texOffs(0, 16)
                    .addBox(-1.5F, 3.0F, -0.4F, 1.0F, 5.0F, 0.8F)
                    .addBox(0.5F, 3.0F, -0.4F, 1.0F, 5.0F, 0.8F),
                    PartPose.ZERO);

            // 🥈 Silver: 8-pointed spiky star frame
            PartDefinition silverFrame = rootPart.addOrReplaceChild("silverFrame", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-2.0F, -2.8F, -0.8F, 4.0F, 0.8F, 1.6F)
                    .addBox(-2.0F, 2.0F, -0.8F, 4.0F, 0.8F, 1.6F)
                    .addBox(-2.8F, -2.8F, -0.8F, 0.8F, 5.6F, 1.6F)
                    .addBox(2.0F, -2.8F, -0.8F, 0.8F, 5.6F, 1.6F),
                    PartPose.ZERO);

            silverFrame.addOrReplaceChild("rotatedSquare", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-2.0F, -2.8F, -0.78F, 4.0F, 0.8F, 1.56F)
                    .addBox(-2.0F, 2.0F, -0.78F, 4.0F, 0.8F, 1.56F)
                    .addBox(-2.8F, -2.8F, -0.78F, 0.8F, 5.6F, 1.56F)
                    .addBox(2.0F, -2.8F, -0.78F, 0.8F, 5.6F, 1.56F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));

            rootPart.addOrReplaceChild("silverRibbon", CubeListBuilder.create().texOffs(0, 16)
                    .addBox(-1.5F, 3.0F, -0.4F, 1.0F, 5.0F, 0.8F)
                    .addBox(0.5F, 3.0F, -0.4F, 1.0F, 5.0F, 0.8F),
                    PartPose.ZERO);

            // 🥇 Gold: Diamond / Rhombus frame
            PartDefinition goldFrame = rootPart.addOrReplaceChild("goldFrame", CubeListBuilder.create(), PartPose.ZERO);

            goldFrame.addOrReplaceChild("sideTR", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5F, -2.85F, -0.8F, 1.0F, 5.7F, 1.6F),
                    PartPose.offsetAndRotation(2.0F, -2.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));

            goldFrame.addOrReplaceChild("sideBR", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5F, -2.85F, -0.79F, 1.0F, 5.7F, 1.58F),
                    PartPose.offsetAndRotation(2.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));

            goldFrame.addOrReplaceChild("sideBL", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5F, -2.85F, -0.8F, 1.0F, 5.7F, 1.6F),
                    PartPose.offsetAndRotation(-2.0F, 2.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));

            goldFrame.addOrReplaceChild("sideTL", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5F, -2.85F, -0.79F, 1.0F, 5.7F, 1.58F),
                    PartPose.offsetAndRotation(-2.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));

            rootPart.addOrReplaceChild("goldRibbon", CubeListBuilder.create().texOffs(0, 16)
                    .addBox(-1.5F, 3.0F, -0.4F, 1.0F, 5.0F, 0.8F)
                    .addBox(0.5F, 3.0F, -0.4F, 1.0F, 5.0F, 0.8F),
                    PartPose.ZERO);

            ModelPart root = LayerDefinition.create(mesh, 32, 32).bakeRoot();
            this.bronzeFrame = root.getChild("bronzeFrame");
            this.bronzeRibbon = root.getChild("bronzeRibbon");
            this.silverFrame = root.getChild("silverFrame");
            this.silverRibbon = root.getChild("silverRibbon");
            this.goldFrame = root.getChild("goldFrame");
            this.goldRibbon = root.getChild("goldRibbon");
        }

        public void renderBronze(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float alpha) {
            this.bronzeFrame.render(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, alpha);
            this.bronzeRibbon.render(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, alpha);
        }

        public void renderSilver(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float alpha) {
            this.silverFrame.render(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, alpha);
            this.silverRibbon.render(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, alpha);
        }

        public void renderGold(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float alpha) {
            this.goldFrame.render(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, alpha);
            this.goldRibbon.render(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, alpha);
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        }
    }
}
