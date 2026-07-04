package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.DoomfistGeoModel;
import org.xeb.xeb.item.DoomfistItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DoomfistGeoRenderer extends GeoItemRenderer<DoomfistItem> {
    public DoomfistGeoRenderer() {
        super(new DoomfistGeoModel());
    }
}
