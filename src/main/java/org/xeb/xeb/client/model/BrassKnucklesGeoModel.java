package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.BrassKnucklesItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BrassKnucklesGeoModel extends GeoModel<BrassKnucklesItem> {
    @Override
    public ResourceLocation getModelResource(BrassKnucklesItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/brass_knuckles.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BrassKnucklesItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/brass_knuckles.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BrassKnucklesItem animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/item/brass_knuckles.animation.json");
    }
}
