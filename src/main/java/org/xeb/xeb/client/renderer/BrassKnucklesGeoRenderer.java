package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.client.model.BrassKnucklesGeoModel;
import org.xeb.xeb.item.BrassKnucklesItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renders the Brass Knuckles GeckoLib model in-hand and in Curios slots.
 *
 * <p>
 * The source geo model is built at Bedrock scale (~2 units tall), which is tiny
 * in the
 * 16-unit-per-block space GeckoLib uses for items. We scale it up and translate
 * it down so the
 * "base" (palm bar) sits against the inside of the player's hand instead of
 * floating above it.
 *
 * <p>
 * GeckoLib scales from the world origin (0,0,0 = bottom of the block the hand
 * rests in).
 * The model's geometry occupies roughly Y = 0.3..2.0 in Bedrock units, so after
 * scaling the whole
 * thing sits well above the hand. We counter-translate downward before scaling
 * so the base of the
 * knuckles meets the hand.
 */
public class BrassKnucklesGeoRenderer extends GeoItemRenderer<BrassKnucklesItem> {

    public BrassKnucklesGeoRenderer() {
        super(new BrassKnucklesGeoModel());
    }

    @Override
    public RenderType getRenderType(BrassKnucklesItem animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
