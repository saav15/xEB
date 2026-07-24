package org.xeb.xeb.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xeb.xeb.client.BeamStruggleCameraHandler;

@OnlyIn(Dist.CLIENT)
@Mixin(Camera.class)
public class CameraMixin {
    @Shadow private Vec3 position;
    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow private boolean detached;

    @Inject(method = "setup", at = @At("TAIL"))
    private void xeb$overrideCameraSetup(BlockGetter level, Entity entity, boolean thirdPerson, boolean rearView, float partialTicks, CallbackInfo ci) {
        BeamStruggleCameraHandler.overrideCamera((Camera) (Object) this, position, xRot, yRot, partialTicks, (newPos, newPitch, newYaw) -> {
            this.position = newPos;
            this.xRot = newPitch;
            this.yRot = newYaw;
            this.detached = true; // Forzar cámara desatada (tercera persona) para que Minecraft renderice el modelo del jugador local en 3D
        });
    }
}
