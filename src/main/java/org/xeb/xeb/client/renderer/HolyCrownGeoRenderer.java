package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.client.model.HolyCrownGeoModel;
import org.xeb.xeb.item.HolyCrownItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HolyCrownGeoRenderer extends GeoItemRenderer<HolyCrownItem> {
    public HolyCrownGeoRenderer() {
        super(new HolyCrownGeoModel());
    }

    @Override
    public void actuallyRender(PoseStack poseStack, HolyCrownItem animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Base crown render
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        // Aqua-blue translucent aura render
        poseStack.pushPose();
        float scale = 1.05F;
        poseStack.scale(scale, scale, scale);
        RenderType auraType = RenderType.entityTranslucent(getTextureLocation(animatable));
        VertexConsumer auraBuffer = bufferSource.getBuffer(auraType);
        super.actuallyRender(poseStack, animatable, model, auraType, bufferSource, auraBuffer, true, partialTick, packedLight, packedOverlay, 0.0F, 0.9F, 1.0F, 0.15F);
        poseStack.popPose();
    }
}
