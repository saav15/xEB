package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.HolyMantleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HolyMantleGeoModel extends GeoModel<HolyMantleItem> {
    @Override
    public ResourceLocation getModelResource(HolyMantleItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/holy_mantle.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HolyMantleItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/holy_mantle.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HolyMantleItem animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/item/holy_mantle.animation.json");
    }
}
