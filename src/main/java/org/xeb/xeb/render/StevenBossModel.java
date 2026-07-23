package org.xeb.xeb.render;

import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.StevenBossEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class StevenBossModel extends DefaultedEntityGeoModel<StevenBossEntity> {
    public StevenBossModel() {
        super(new ResourceLocation(Xeb.MODID, "steven"));
    }

    @Override
    public ResourceLocation getModelResource(StevenBossEntity animatable) {
        return new ResourceLocation(Xeb.MODID, "geo/steven.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StevenBossEntity animatable) {
        return new ResourceLocation(Xeb.MODID, "textures/entity/steven.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StevenBossEntity animatable) {
        return new ResourceLocation(Xeb.MODID, "animations/steven.animation.json");
    }
}
