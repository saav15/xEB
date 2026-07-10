package org.xeb.xeb.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
 *
 * @OnlyIn(Dist.CLIENT) — HumanoidModel is a client-only class. Forge will skip this
 * mixin on dedicated servers thanks to the dist-marker check at load time.
 */
@OnlyIn(Dist.CLIENT)
@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;
    @Shadow public ModelPart body;
    @Shadow public ModelPart head;

    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void onSetupAnimHead(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            this.body.zRot = 0.0F;
            this.body.yRot = 0.0F;
            this.rightLeg.zRot = 0.0F;
            this.rightLeg.yRot = 0.0F;
            this.leftLeg.zRot = 0.0F;
            this.leftLeg.yRot = 0.0F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
        }
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            org.xeb.xeb.client.JaronaAttack.ActiveKick kick = org.xeb.xeb.client.JaronaAttack.getKick(player);
            if (kick != null) {
                float step = kick.comboStep;

                this.rightLeg.xRot = (float) Math.toRadians(-120 * step * 0.8F);
                this.rightLeg.yRot = 0.0F;
                this.rightLeg.zRot = 0.0F;

                this.leftLeg.xRot = (float) Math.toRadians(45 * step * 0.8F);
                this.leftLeg.yRot = 0.0F;
                this.leftLeg.zRot = 0.0F;

                this.body.xRot = (float) Math.toRadians(25 * step * 0.8F);
                this.body.yRot = 0.0F;
                if (step == 3) {
                    this.body.zRot = (float) Math.toRadians(-15);
                } else {
                    this.body.zRot = 0.0F;
                }

                this.rightArm.xRot = (float) Math.toRadians(60 * step * 0.8F);
                this.rightArm.yRot = 0.0F;
                this.rightArm.zRot = (float) Math.toRadians(-20);

                this.leftArm.xRot = (float) Math.toRadians(60 * step * 0.8F);
                this.leftArm.yRot = 0.0F;
                this.leftArm.zRot = (float) Math.toRadians(20);

                this.head.xRot = (float) Math.toRadians(headPitch);
                this.head.yRot = (float) Math.toRadians(netHeadYaw);
                this.head.zRot = 0.0F;
                return;
            }
        }

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
