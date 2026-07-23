package org.xeb.xeb.render;

import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.StevenCloneEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class StevenCloneModel extends DefaultedEntityGeoModel<StevenCloneEntity> {
    public StevenCloneModel() {
        super(new ResourceLocation(Xeb.MODID, "steven"));
    }

    @Override
    public ResourceLocation getModelResource(StevenCloneEntity animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/steven.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StevenCloneEntity animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/steven.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StevenCloneEntity animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/steven.animation.json");
    }
}
