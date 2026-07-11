package org.xeb.xeb.client.model;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.client.XebPlayerAnimatable;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

public class XebPlayerGeoModel extends GeoModel<XebPlayerAnimatable> {
    @Override
    public ResourceLocation getModelResource(XebPlayerAnimatable animatable) {
        return new ResourceLocation(org.xeb.xeb.Xeb.MODID, "geo/player/xeb_player.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(XebPlayerAnimatable animatable) {
        if (animatable.getPlayer() instanceof AbstractClientPlayer acp) {
            return acp.getSkinTextureLocation();
        }
        return new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    }

    @Override
    public ResourceLocation getAnimationResource(XebPlayerAnimatable animatable) {
        return new ResourceLocation(org.xeb.xeb.Xeb.MODID, "animations/player/xeb_player.animation.json");
    }

    @Override
    public void setCustomAnimations(XebPlayerAnimatable animatable, long instanceId, AnimationState<XebPlayerAnimatable> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(data.headPitch() * ((float) Math.PI / 180F));
            head.setRotY(data.netHeadYaw() * ((float) Math.PI / 180F));
        }
    }
}
