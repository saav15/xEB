package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.xeb.xeb.item.ModItems;

/**
 * Custom RenderLayer that sets the wielder's arm pose to ear level (visor touch pose)
 * when using The Optic Blast or its abilities.
 * Runs during third-person rendering after model setupAnim.
 */
public class OpticBlastRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public OpticBlastRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        boolean holdsOptic = entity.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.OPTIC_BLAST.get())
                || entity.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.OPTIC_BLAST.get());
        if (!holdsOptic) return;

        boolean isFiringPrimary = entity.isUsingItem() && entity.getUseItem().is(ModItems.OPTIC_BLAST.get());
        boolean isCyclonePush = entity.getPersistentData().getBoolean("xebCyclonePushFiring");
        boolean isGeneSplice = entity.getPersistentData().getBoolean("xebGeneSpliceFiring");

        if (isFiringPrimary || isCyclonePush || isGeneSplice) {
            M model = this.getParentModel();
            if (model instanceof HumanoidModel<?> humanoidModel) {
                HumanoidArm arm = entity.getMainArm();
                if (entity.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.OPTIC_BLAST.get())) {
                    arm = arm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
                }

                ModelPart armPart = (arm == HumanoidArm.RIGHT) ? humanoidModel.rightArm : humanoidModel.leftArm;

                // Visor/temple pose: raise arm up and bend inwards towards the head/ear
                armPart.xRot = -1.5F;
                armPart.yRot = (arm == HumanoidArm.RIGHT) ? -0.7F : 0.7F;
                armPart.zRot = (arm == HumanoidArm.RIGHT) ? -0.2F : 0.2F;
            }
        }
    }
}
