package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.CrazyDiamondHeadItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrazyDiamondHeadGeoModel extends GeoModel<CrazyDiamondHeadItem> {
    @Override
    public ResourceLocation getModelResource(CrazyDiamondHeadItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/crazydiamond.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CrazyDiamondHeadItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/crazydiamond.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CrazyDiamondHeadItem animatable) {
        // Dummy animation path (the item does not use animations)
        return new ResourceLocation(Xeb.MODID, "animations/item/smart_halberd.animation.json");
    }
}
