package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.MobEnergyGeoModel;
import org.xeb.xeb.item.MobEnergyItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MobEnergyGeoRenderer extends GeoItemRenderer<MobEnergyItem> {
    public MobEnergyGeoRenderer() {
        super(new MobEnergyGeoModel());
    }
}
