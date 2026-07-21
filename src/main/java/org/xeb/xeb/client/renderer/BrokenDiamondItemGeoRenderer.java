package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.BrokenDiamondItemGeoModel;
import org.xeb.xeb.item.BrokenDiamondItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BrokenDiamondItemGeoRenderer extends GeoItemRenderer<BrokenDiamondItem> {
    public BrokenDiamondItemGeoRenderer() {
        super(new BrokenDiamondItemGeoModel());
    }
}
