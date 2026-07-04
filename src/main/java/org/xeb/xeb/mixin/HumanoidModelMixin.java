package org.xeb.xeb.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.client.renderer.OpticBlastBeamRenderer;

/**
 * Mixin to intercept HumanoidModel.setupAnim to force the player's arm
 * to their ear/temple when firing The Optic Blast or using its active abilities.
 * This guarantees the animation works correctly in third-person and overrides all biped defaults.
 */
@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        boolean holdsOptic = entity.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.OPTIC_BLAST.get())
                || entity.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.OPTIC_BLAST.get());
        if (!holdsOptic) return;

        boolean isUsing = entity.isUsingItem() && entity.getUseItem().is(ModItems.OPTIC_BLAST.get());
        boolean hasActiveBeam = OpticBlastBeamRenderer.CLIENT_BEAMS.containsKey(entity.getId());
        boolean hasActiveChain = OpticBlastBeamRenderer.CLIENT_CHAINS.containsKey(entity.getId());
        boolean isSwinging = entity.swinging; // Swing covers mini-laser click/activation

        if (isUsing || hasActiveBeam || hasActiveChain || isSwinging) {
            HumanoidArm arm = entity.getMainArm();
            if (entity.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.OPTIC_BLAST.get())) {
                arm = arm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
            }

            ModelPart armPart = (arm == HumanoidArm.RIGHT) ? this.rightArm : this.leftArm;

            // Ear touch pose: raise arm up and rotate it towards the temple/ear
            armPart.xRot = -1.5F;
            armPart.yRot = (arm == HumanoidArm.RIGHT) ? -0.7F : 0.7F;
            armPart.zRot = (arm == HumanoidArm.RIGHT) ? -0.2F : 0.2F;
        }
    }
}
