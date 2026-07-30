package org.xeb.xeb.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.xeb.xeb.client.PermanightClientHandler;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Redirect(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"
        )
    )
    private void xeb$hideSunAndMoonDuringPermanight(int unit, ResourceLocation location) {
        if (PermanightClientHandler.isPermanightActive() && location != null) {
            String path = location.getPath();
            if (path.contains("sun") || path.contains("moon")) {
                RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 0.0F);
                RenderSystem.setShaderTexture(unit, location);
                return;
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(unit, location);
    }
}
