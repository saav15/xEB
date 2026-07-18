package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.SmartHalberdItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SmartHalberdGeoModel extends GeoModel<SmartHalberdItem> {
    @Override
    public ResourceLocation getModelResource(SmartHalberdItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/smart_halberd.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SmartHalberdItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/smart_halberd.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SmartHalberdItem animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/item/smart_halberd.animation.json");
    }
}
