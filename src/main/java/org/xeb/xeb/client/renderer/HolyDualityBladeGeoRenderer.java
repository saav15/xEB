package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.client.model.HolyDualityBladeGeoModel;
import org.xeb.xeb.item.HolyDualityBladeItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HolyDualityBladeGeoRenderer extends GeoItemRenderer<HolyDualityBladeItem> {
    public HolyDualityBladeGeoRenderer() {
        super(new HolyDualityBladeGeoModel());
    }

    @Override
    public void actuallyRender(PoseStack poseStack, HolyDualityBladeItem animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Base weapon render
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        // White translucent aura render
        poseStack.pushPose();
        float scale = 1.05F;
        poseStack.scale(scale, scale, scale);
        RenderType auraType = RenderType.entityTranslucent(getTextureLocation(animatable));
        VertexConsumer auraBuffer = bufferSource.getBuffer(auraType);
        super.actuallyRender(poseStack, animatable, model, auraType, bufferSource, auraBuffer, true, partialTick, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 0.15F);
        poseStack.popPose();
    }
}
