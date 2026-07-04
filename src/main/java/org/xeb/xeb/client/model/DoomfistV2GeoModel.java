package org.xeb.xeb.client.model;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.DoomfistV2Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DoomfistV2GeoModel extends GeoModel<DoomfistV2Item> {
    @Override
    public ResourceLocation getModelResource(DoomfistV2Item animatable) {
        // Point to same geo model or custom v2 model
        return new ResourceLocation(Xeb.MODID, "geo/item/doomfist.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DoomfistV2Item animatable) {
        // Can point to red/v2 texture
        return new ResourceLocation(Xeb.MODID, "textures/entity/doomfist_v2.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DoomfistV2Item animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/item/doomfist.animation.json");
    }
}
