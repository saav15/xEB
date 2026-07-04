package org.xeb.xeb.client.renderer;

import org.jetbrains.annotations.Nullable;
import org.xeb.xeb.client.model.TinfoilHatGeoModel;
import org.xeb.xeb.item.TinfoilHatItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.client.model.HumanoidModel;

public class TinfoilHatGeoRenderer extends GeoArmorRenderer<TinfoilHatItem> {
    public TinfoilHatGeoRenderer() {
        super(new TinfoilHatGeoModel());
    }

    @Override
    @Nullable
    public GeoBone getHeadBone() {
        return this.model.getBone("bone").orElse(null);
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);
        if (this.head != null) {
            // Shift the hat slightly up (Y axis is inverted, so subtracting moves it up) to not cover the eyes
            this.head.setPosY(this.head.getPosY() - 3.0F);
        }
    }

}
