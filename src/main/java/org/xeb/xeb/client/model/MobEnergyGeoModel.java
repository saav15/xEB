package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.MobEnergyItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MobEnergyGeoModel extends GeoModel<MobEnergyItem> {
    @Override
    public ResourceLocation getModelResource(MobEnergyItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/mob_energy.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MobEnergyItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/mob_energy.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MobEnergyItem animatable) {
        // Fall back to idle if no animations exist, or use animation file if provided
        return new ResourceLocation(Xeb.MODID, "animations/item/mob_energy.animation.json");
    }
}
