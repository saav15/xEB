package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.SmartHalberdGeoModel;
import org.xeb.xeb.item.SmartHalberdItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SmartHalberdGeoRenderer extends GeoItemRenderer<SmartHalberdItem> {
    public SmartHalberdGeoRenderer() {
        super(new SmartHalberdGeoModel());
    }
}
