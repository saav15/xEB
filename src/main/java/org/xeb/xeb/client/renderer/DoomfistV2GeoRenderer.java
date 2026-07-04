package org.xeb.xeb.client.renderer;

import org.xeb.xeb.client.model.DoomfistV2GeoModel;
import org.xeb.xeb.item.DoomfistV2Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DoomfistV2GeoRenderer extends GeoItemRenderer<DoomfistV2Item> {
    public DoomfistV2GeoRenderer() {
        super(new DoomfistV2GeoModel());
    }
}
