package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.BrokenDiamondItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BrokenDiamondItemGeoModel extends GeoModel<BrokenDiamondItem> {
    @Override
    public ResourceLocation getModelResource(BrokenDiamondItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/crazydiamonditem.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BrokenDiamondItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/crazyitem.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BrokenDiamondItem animatable) {
        // Dummy animation path (the item does not use animations)
        return new ResourceLocation(Xeb.MODID, "animations/item/smart_halberd.animation.json");
    }
}
