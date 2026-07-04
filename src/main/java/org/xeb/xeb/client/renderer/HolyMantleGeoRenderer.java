package org.xeb.xeb.client.renderer;

import org.jetbrains.annotations.Nullable;
import org.xeb.xeb.client.model.HolyMantleGeoModel;
import org.xeb.xeb.item.HolyMantleItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.client.model.HumanoidModel;

public class HolyMantleGeoRenderer extends GeoArmorRenderer<HolyMantleItem> {
    public HolyMantleGeoRenderer() {
        super(new HolyMantleGeoModel());
    }

    @Override
    @Nullable
    public GeoBone getBodyBone() {
        return this.model.getBone("bb_main").orElse(null);
    }

}
