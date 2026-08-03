package org.xeb.xeb.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import org.xeb.xeb.entity.MechaEggmanMissileEntity;

public class MechaEggmanMissileRenderer extends ThrownItemRenderer<MechaEggmanMissileEntity> {
    public MechaEggmanMissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
