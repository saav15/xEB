package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
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

public class GlowEyeOverlay<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation WHITE_TEX = new ResourceLocation(Xeb.MODID, "textures/entity/white.png");
    private final GlowEyeModel eyesModel = new GlowEyeModel();

    public GlowEyeOverlay(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        if (medallions.isEmpty()) return;

        if (!org.xeb.xeb.Config.glowEyesEnabled) return;

        // Pulse alpha: 0.7 to 1.0 over 2 seconds (40 ticks)
        float time = entity.tickCount + partialTick;
        float alpha = 0.7F + (float) Math.sin(time * 0.157F) * 0.15F;

        // Get primary buff color
        int color = medallions.get(0).getBuff().getColor();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.eyes(WHITE_TEX));
        ModelPart headPart = findHeadPart(getParentModel());

        if (headPart != null) {
            poseStack.pushPose();
            // Translate and rotate using the head's animation transforms
            headPart.translateAndRotate(poseStack);
            // Scale to match the pixel-based coordinates (1/16 of a block)
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            // Render the eye model
            eyesModel.renderToBuffer(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
            poseStack.popPose();
        } else {
            // Fallback rendering in front of head/body center
            poseStack.pushPose();
            float width = entity.getBbWidth();
            float height = entity.getBbHeight();
            // Translate to top-front of the entity
            poseStack.translate(0.0F, -height * 0.8F, -width * 0.51F);
            // Scale to pixel-based coordinates
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            // Re-align the model Y to local origin (model is at Y = -4.2F, so translate by +4.2F)
            poseStack.translate(0.0F, 4.2F, 0.0F);
            eyesModel.renderToBuffer(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
            poseStack.popPose();
        }
    }

    private ModelPart findHeadPart(EntityModel<?> model) {
        Class<?> clazz = model.getClass();
        while (clazz != null && clazz != Object.class) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (field.getType() == ModelPart.class) {
                    String name = field.getName();
                    if (name.equalsIgnoreCase("head") || 
                        name.equals("f_102812_") || 
                        name.equals("f_102808_") || 
                        name.equals("f_103859_") || 
                        name.equals("f_102008_") || 
                        name.contains("head") || 
                        name.contains("Head")) {
                        try {
                            field.setAccessible(true);
                            return (ModelPart) field.get(model);
                        } catch (Exception e) {
                            LOGGER.debug("[xEB] Failed to access head ModelPart field '{}': {}", name, e.getMessage());
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    public static class GlowEyeModel extends Model {
        private final ModelPart root;

        public GlowEyeModel() {
            super(RenderType::eyes);
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition rootPart = mesh.getRoot();

            // Left eye and Right eye cubes (0.8x0.8x0.1)
            rootPart.addOrReplaceChild("eyes", CubeListBuilder.create()
                            .texOffs(0, 0)
                            .addBox(1.5F, -4.6F, -4.05F, 0.8F, 0.8F, 0.1F)
                            .addBox(-2.3F, -4.6F, -4.05F, 0.8F, 0.8F, 0.1F),
                    PartPose.ZERO);

            ModelPart bakedRoot = LayerDefinition.create(mesh, 16, 16).bakeRoot();
            this.root = bakedRoot.getChild("eyes");
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}
