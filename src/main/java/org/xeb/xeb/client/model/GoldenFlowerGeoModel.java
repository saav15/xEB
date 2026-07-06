package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.GoldenFlowerItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GoldenFlowerGeoModel extends GeoModel<GoldenFlowerItem> {
    @Override
    public ResourceLocation getModelResource(GoldenFlowerItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/the_golden_flower.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GoldenFlowerItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/the_golden_flower.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GoldenFlowerItem animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/item/the_golden_flower.animation.json");
    }
}
