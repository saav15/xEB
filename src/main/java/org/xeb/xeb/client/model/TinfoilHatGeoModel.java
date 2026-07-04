package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.TinfoilHatItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TinfoilHatGeoModel extends GeoModel<TinfoilHatItem> {
    @Override
    public ResourceLocation getModelResource(TinfoilHatItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/tinfoil_hat.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TinfoilHatItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/tinfoil_hat.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TinfoilHatItem animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/item/tinfoil_hat.animation.json");
    }
}
