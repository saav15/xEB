package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.HolyCrownItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HolyCrownGeoModel extends GeoModel<HolyCrownItem> {
    @Override
    public ResourceLocation getModelResource(HolyCrownItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/holycrown.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HolyCrownItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/holycrown.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HolyCrownItem animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/item/holycrown.animation.json");
    }
}
