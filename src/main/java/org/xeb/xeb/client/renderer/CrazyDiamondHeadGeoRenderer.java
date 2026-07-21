package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.CrazyDiamondHeadGeoModel;
import org.xeb.xeb.item.CrazyDiamondHeadItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CrazyDiamondHeadGeoRenderer extends GeoItemRenderer<CrazyDiamondHeadItem> {
    public CrazyDiamondHeadGeoRenderer() {
        super(new CrazyDiamondHeadGeoModel());
    }
}
