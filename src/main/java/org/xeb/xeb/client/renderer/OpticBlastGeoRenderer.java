package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.OpticBlastGeoModel;
import org.xeb.xeb.item.OpticBlastItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class OpticBlastGeoRenderer extends GeoItemRenderer<OpticBlastItem> {
    public OpticBlastGeoRenderer() {
        super(new OpticBlastGeoModel());
    }

    @Override
    public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext transformType, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Skip rendering in hands since it is rendered on the head instead
        if (transformType == net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || 
            transformType == net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND || 
            transformType == net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || 
            transformType == net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return;
        }
        // Skip rendering in general head contexts unless triggered by our layer
        if (transformType == net.minecraft.world.item.ItemDisplayContext.HEAD && !org.xeb.xeb.render.OpticBlastPlayerLayer.RENDERING_HEAD_LAYER) {
            return;
        }
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
