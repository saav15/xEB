package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.DemonCoreEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DemonCoreGeoModel extends GeoModel<DemonCoreEntity> {
    @Override
    public ResourceLocation getModelResource(DemonCoreEntity animatable) {
        // The user said the 3D model is in geo/item/demon_core.geo.json
        return new ResourceLocation(Xeb.MODID, "geo/item/demon_core.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DemonCoreEntity animatable) {
        // Texture location
        return new ResourceLocation(Xeb.MODID, "textures/entity/demon_core.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DemonCoreEntity animatable) {
        // Animation location
        return new ResourceLocation(Xeb.MODID, "animations/demon_core.animation.json");
    }
}
