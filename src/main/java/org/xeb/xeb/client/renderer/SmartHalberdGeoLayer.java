package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.item.SmartHalberdItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders a pure Neon Yellow translucent emissive aura overlay on the Smart Halberd when Flourish (B) is pressed.
 * Uses a white texture layer to avoid texture color bleeding (red/black artifacts).
 */
public class SmartHalberdGeoLayer extends GeoRenderLayer<SmartHalberdItem> {
    private static final ResourceLocation WHITE_TEX = new ResourceLocation(Xeb.MODID, "textures/entity/white.png");

    public SmartHalberdGeoLayer(GeoRenderer<SmartHalberdItem> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, SmartHalberdItem animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        float intensity = FlourishAnimationManager.getSmartHalberdGlowIntensity();
        int level = FlourishAnimationManager.getSmartHalberdGlowLevel();
        if (intensity <= 0.0F || level <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !(player.getMainHandItem().is(ModItems.SMART_HALBERD.get()) || player.getOffhandItem().is(ModItems.SMART_HALBERD.get()))) {
            return;
        }

        // Pure Electric Neon Yellow Aura Color (RGB: 0.95, 1.0, 0.05)
        float alpha = Math.min(0.60F, (level / 4.0F) * 0.40F * intensity + 0.15F * intensity);
        float red = 0.95F;
        float green = 1.0F;
        float blue = 0.05F;

        RenderType auraRenderType = RenderType.entityTranslucentEmissive(WHITE_TEX);
        VertexConsumer auraConsumer = bufferSource.getBuffer(auraRenderType);

        poseStack.pushPose();
        // Slightly scale up for a smooth outline aura effect
        float auraScale = 1.012F + (0.006F * level);
        poseStack.scale(auraScale, auraScale, auraScale);

        this.getRenderer().actuallyRender(
                poseStack, animatable, bakedModel, auraRenderType, bufferSource, auraConsumer,
                true, partialTick, 15728880, OverlayTexture.NO_OVERLAY,
                red, green, blue, alpha
        );

        poseStack.popPose();
    }
}
