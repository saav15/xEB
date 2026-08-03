package org.xeb.xeb.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xeb.xeb.client.LaserHeavyAimHandler;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void xeb$applyLaserMouseSensitivity(CallbackInfo ci) {
        if (this.minecraft.player != null) {
            float mult = LaserHeavyAimHandler.getTurnMultiplier(this.minecraft.player);
            if (mult < 1.0F) {
                this.accumulatedDX *= (double) mult;
                this.accumulatedDY *= (double) mult;
            }
        }
    }
}
