package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.DoomfistItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DoomfistGeoModel extends GeoModel<DoomfistItem> {
    @Override
    public ResourceLocation getModelResource(DoomfistItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/doomfist.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DoomfistItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/doomfist.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DoomfistItem animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/item/doomfist.animation.json");
    }
}
