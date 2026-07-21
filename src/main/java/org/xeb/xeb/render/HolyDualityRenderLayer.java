package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;

public class HolyDualityRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public HolyDualityRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!(entity instanceof Player player)) return;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        boolean hasMain = mainHand.is(ModItems.HOLY_DUALITY_BLADE.get());
        boolean hasOff = offHand.is(ModItems.HOLY_DUALITY_BLADE.get());

        if (!hasMain && !hasOff) return;

        M model = this.getParentModel();
        if (!(model instanceof HumanoidModel<?> humanoidModel)) return;

        // 1. RENDER HOLY CROWN
        renderHolyCrown(poseStack, buffer, player, humanoidModel, packedLight);

        // 2. RENDER PHANTOM SWORD IN THE OTHER HAND
        ItemStack swordStack = hasMain ? mainHand : offHand;

        if (hasMain && offHand.isEmpty()) {
            // Render in left hand
            poseStack.pushPose();
            humanoidModel.leftArm.translateAndRotate(poseStack);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            
            // Left arm translation to hand
            poseStack.translate(-0.0625F, 0.125F, -0.625F);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    player,
                    swordStack,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    true,
                    poseStack,
                    buffer,
                    player.level(),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    player.getId()
            );
            poseStack.popPose();
        } else if (hasOff && mainHand.isEmpty()) {
            // Render in right hand
            poseStack.pushPose();
            humanoidModel.rightArm.translateAndRotate(poseStack);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            
            // Right arm translation to hand
            poseStack.translate(0.0625F, 0.125F, -0.625F);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    player,
                    swordStack,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    false,
                    poseStack,
                    buffer,
                    player.level(),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    player.getId()
            );
            poseStack.popPose();
        }
    }

    private void renderHolyCrown(PoseStack poseStack, MultiBufferSource buffer, Player player, HumanoidModel<?> model, int packedLight) {
        poseStack.pushPose();
        // Align with player's head rotation and translation
        model.head.translateAndRotate(poseStack);
        
        // Rotate Z by 180 degrees to flip right-side up (keeps Z facing forward)
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
        
        ItemStack crownStack = new ItemStack(ModItems.HOLY_CROWN.get());
        Minecraft.getInstance().getItemRenderer().renderStatic(
                player,
                crownStack,
                ItemDisplayContext.HEAD,
                false,
                poseStack,
                buffer,
                player.level(),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                player.getId()
        );
        poseStack.popPose();
    }
}
