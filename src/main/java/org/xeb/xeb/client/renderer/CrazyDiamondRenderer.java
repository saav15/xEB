package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.CrazyDiamondEntity;

public class CrazyDiamondRenderer extends HumanoidMobRenderer<CrazyDiamondEntity, CrazyDiamondModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Xeb.MODID,
            "textures/entity/crazy_diamond.png");

    public CrazyDiamondRenderer(EntityRendererProvider.Context context) {
        super(context, new CrazyDiamondModel(context.bakeLayer(ModelLayers.PLAYER)), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(CrazyDiamondEntity entity) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(CrazyDiamondEntity animatable, boolean texture, boolean isVisible,
            boolean glowing) {
        // Render as translucent to represent the Stand essence
        return RenderType.entityTranslucent(this.getTextureLocation(animatable));
    }

    @Override
    protected void setupRotations(CrazyDiamondEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw,
            float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
    }

    @Override
    protected void scale(CrazyDiamondEntity entity, PoseStack poseStack, float partialTickTime) {
        super.scale(entity, poseStack, partialTickTime);

        // 1. Contrarrestamos el empuje hacia abajo que hace Minecraft internamente
        // (1.501)
        poseStack.translate(0.0F, 1.501F, 0.0F);

        // 2. Flotación suave
        float floatOffset = (float) Math.sin((entity.tickCount + partialTickTime) * 0.1F) * 0.2F;

        // 3. Ajuste fino de altura: Como el eje Y está invertido, valores negativos
        // mueven el modelo hacia ARRIBA
        poseStack.translate(0.0F, -1.8F - floatOffset, -0.3F);

        // 4. Inclinación
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(entity.getXRot() * 0.025F));
    }
}
