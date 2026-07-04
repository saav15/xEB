package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.DoomfistGeoModel;
import org.xeb.xeb.item.DoomfistItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DoomfistGeoRenderer extends GeoItemRenderer<DoomfistItem> {
    public DoomfistGeoRenderer() {
        super(new DoomfistGeoModel());
    }

    @Override
    public void preRender(com.mojang.blaze3d.vertex.PoseStack poseStack, DoomfistItem animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.translate(-0.5F, -0.51F, -0.5F);
    }
}
