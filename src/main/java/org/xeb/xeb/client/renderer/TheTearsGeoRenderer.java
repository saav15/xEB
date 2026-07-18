package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.TheTearsGeoModel;
import org.xeb.xeb.item.TheTearsItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TheTearsGeoRenderer extends GeoItemRenderer<TheTearsItem> {
    public TheTearsGeoRenderer() {
        super(new TheTearsGeoModel());
    }
}
