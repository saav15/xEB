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
        renderHolyCrown(poseStack, buffer, player, humanoidModel);

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

    private void renderHolyCrown(PoseStack poseStack, MultiBufferSource buffer, Player player, HumanoidModel<?> model) {
        boolean blessed = player.getPersistentData().getBoolean("xebHolyBlessedActive");
        boolean annihilation = player.getPersistentData().getBoolean("xebHolyAnnihilationActive");

        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        poseStack.translate(0.0D, -0.18D, 0.0D);

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer crownConsumer = buffer.getBuffer(RenderType.lightning());

        float r = 1.0F, g = 0.84F, b = 0.0F; // dorado normal
        if (blessed) { r = 1.0F; g = 1.0F; b = 0.93F; } // blanco brillante
        if (annihilation) { r = 1.0F; g = 0.25F; b = 0.0F; } // rojo sagrado
        float a = 0.95F;

        // Base de la corona (anillo)
        float baseRadius = 0.28F;
        float baseHeight = 0.06F;
        int baseSegs = 10;
        for (int j = 0; j < baseSegs; j++) {
            double angle1 = (j * 2.0D * Math.PI) / baseSegs;
            double angle2 = ((j + 1) * 2.0D * Math.PI) / baseSegs;
            float x1 = (float) (Math.cos(angle1) * baseRadius);
            float z1 = (float) (Math.sin(angle1) * baseRadius);
            float x2 = (float) (Math.cos(angle2) * baseRadius);
            float z2 = (float) (Math.sin(angle2) * baseRadius);

            crownConsumer.vertex(matrix, x1, 0.0F, z1).color(r, g, b, a).endVertex();
            crownConsumer.vertex(matrix, x2, 0.0F, z2).color(r, g, b, a).endVertex();
            crownConsumer.vertex(matrix, x2, baseHeight, z2).color(r, g, b, a).endVertex();
            crownConsumer.vertex(matrix, x1, baseHeight, z1).color(r, g, b, a).endVertex();
        }

        // 3 PICOS: 2 pequeños laterales (izq y der) + 1 grande en medio (frente)
        float bigPeakX = 0.0F, bigPeakZ = -baseRadius;
        float bigPeakH = 0.35F;
        float bigPeakW = 0.10F;
        crownConsumer.vertex(matrix, bigPeakX - bigPeakW, baseHeight, bigPeakZ).color(r, g, b, a).endVertex();
        crownConsumer.vertex(matrix, bigPeakX + bigPeakW, baseHeight, bigPeakZ).color(r, g, b, a).endVertex();
        crownConsumer.vertex(matrix, bigPeakX + bigPeakW * 0.5F, baseHeight + bigPeakH, bigPeakZ).color(r, g, b, a).endVertex();
        crownConsumer.vertex(matrix, bigPeakX - bigPeakW * 0.5F, baseHeight + bigPeakH, bigPeakZ).color(r, g, b, a).endVertex();

        double leftAngle = Math.PI * 0.75D; // 135°
        float leftX = (float) (Math.cos(leftAngle) * baseRadius);
        float leftZ = (float) (Math.sin(leftAngle) * baseRadius);
        float smallH = 0.20F;
        float smallW = 0.07F;
        float perpLeftX = (float) Math.sin(leftAngle);
        float perpLeftZ = -(float) Math.cos(leftAngle);
        crownConsumer.vertex(matrix, leftX - perpLeftX * smallW, baseHeight, leftZ - perpLeftZ * smallW).color(r, g, b, a).endVertex();
        crownConsumer.vertex(matrix, leftX + perpLeftX * smallW, baseHeight, leftZ + perpLeftZ * smallW).color(r, g, b, a).endVertex();
        crownConsumer.vertex(matrix, leftX + perpLeftX * smallW * 0.5F, baseHeight + smallH, leftZ + perpLeftZ * smallW * 0.5F).color(r, g, b, a).endVertex();
        crownConsumer.vertex(matrix, leftX - perpLeftX * smallW * 0.5F, baseHeight + smallH, leftZ - perpLeftZ * smallW * 0.5F).color(r, g, b, a).endVertex();

        double rightAngle = Math.PI * 1.25D; // 225°
        float rightX = (float) (Math.cos(rightAngle) * baseRadius);
        float rightZ = (float) (Math.sin(rightAngle) * baseRadius);
        float perpRightX = (float) Math.sin(rightAngle);
        float perpRightZ = -(float) Math.cos(rightAngle);
        crownConsumer.vertex(matrix, rightX - perpRightX * smallW, baseHeight, rightZ - perpRightZ * smallW).color(r, g, b, a).endVertex();
        crownConsumer.vertex(matrix, rightX + perpRightX * smallW, baseHeight, rightZ + perpRightZ * smallW).color(r, g, b, a).endVertex();
        crownConsumer.vertex(matrix, rightX + perpRightX * smallW * 0.5F, baseHeight + smallH, rightZ + perpRightZ * smallW * 0.5F).color(r, g, b, a).endVertex();
        crownConsumer.vertex(matrix, rightX - perpRightX * smallW * 0.5F, baseHeight + smallH, rightZ - perpRightZ * smallW * 0.5F).color(r, g, b, a).endVertex();

        poseStack.popPose();
    }
}
