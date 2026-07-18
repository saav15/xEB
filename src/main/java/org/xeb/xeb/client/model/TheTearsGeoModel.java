package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.TheTearsItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TheTearsGeoModel extends GeoModel<TheTearsItem> {
    @Override
    public ResourceLocation getModelResource(TheTearsItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/the_tears.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TheTearsItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/the_tears.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TheTearsItem animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/item/the_tears.animation.json");
    }
}
