package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.OpticBlastGeoModel;
import org.xeb.xeb.item.OpticBlastItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class OpticBlastGeoRenderer extends GeoItemRenderer<OpticBlastItem> {
    public OpticBlastGeoRenderer() {
        super(new OpticBlastGeoModel());
    }
}
