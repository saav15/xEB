package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.HolyDualityBladeItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HolyDualityBladeGeoModel extends GeoModel<HolyDualityBladeItem> {
    @Override
    public ResourceLocation getModelResource(HolyDualityBladeItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/holyblade.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HolyDualityBladeItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/holyblade.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HolyDualityBladeItem animatable) {
        // Dummy animation path (the blade does not use animations)
        return new ResourceLocation(Xeb.MODID, "animations/item/smart_halberd.animation.json");
    }
}
