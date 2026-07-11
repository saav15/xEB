package org.xeb.xeb.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.xeb.xeb.client.XebPlayerAnimatable;
import org.xeb.xeb.client.model.XebPlayerGeoModel;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

/**
 * Replaces the vanilla PlayerRenderer with GeckoLib.
 * ONLY used when Better Combat is NOT loaded.
 */
public class XebPlayerRenderer extends GeoReplacedEntityRenderer<AbstractClientPlayer, XebPlayerAnimatable> {

    private static final XebPlayerAnimatable WRAPPER = new XebPlayerAnimatable(null);

    public XebPlayerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new XebPlayerGeoModel(), null);
    }

    @Override
    public void actuallyRender(com.mojang.blaze3d.vertex.PoseStack poseStack, XebPlayerAnimatable animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.RenderType renderType, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
