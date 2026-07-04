package org.xeb.xeb.compat.jade;

import snownee.jade.api.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

@WailaPlugin
public class XEBJadePlugin implements IWailaPlugin {
    public static final ResourceLocation MEDALLIONS = new ResourceLocation("xeb", "medallions");

    @Override
    public void register(IWailaCommonRegistration registration) {
        // No server-side registration needed as data is synchronized via existing custom packets
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(XEBJadeRenderer.INSTANCE, LivingEntity.class);
    }
}
