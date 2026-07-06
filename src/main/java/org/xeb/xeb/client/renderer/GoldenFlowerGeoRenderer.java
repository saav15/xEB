package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.GoldenFlowerGeoModel;
import org.xeb.xeb.item.GoldenFlowerItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GoldenFlowerGeoRenderer extends GeoItemRenderer<GoldenFlowerItem> {
    public GoldenFlowerGeoRenderer() {
        super(new GoldenFlowerGeoModel());
    }
}
