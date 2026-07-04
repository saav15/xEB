package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.OpticBlastItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OpticBlastGeoModel extends GeoModel<OpticBlastItem> {
    @Override
    public ResourceLocation getModelResource(OpticBlastItem animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/item/optic_blast.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OpticBlastItem animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/optic_blast.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OpticBlastItem animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/item/optic_blast.animation.json");
    }
}
