package org.xeb.xeb.client.model;

import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.HotPokerEntity;
import software.bernie.geckolib.model.GeoModel;

public class HotPokerGeoModel extends GeoModel<HotPokerEntity> {
    @Override
    public ResourceLocation getModelResource(HotPokerEntity animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/hot_poker.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HotPokerEntity animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/hot_poker.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HotPokerEntity animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/hot_poker.animation.json");
    }
}
