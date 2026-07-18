package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class OpticBlastPlayerLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public static boolean RENDERING_HEAD_LAYER = false;

    public OpticBlastPlayerLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    private static ItemStack getCurioTinfoilHat(LivingEntity entity) {
        for (ItemStack stack : org.xeb.xeb.compat.ModCompatManager.getCuriosItems(entity)) {
            if (stack.is(ModItems.TINFOIL_HAT.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack getCurioHolyMantle(LivingEntity entity) {
        for (ItemStack stack : org.xeb.xeb.compat.ModCompatManager.getCuriosItems(entity)) {
            if (stack.is(ModItems.HOLY_MANTLE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);

        boolean hasMain = mainHand.is(ModItems.OPTIC_BLAST.get());
        boolean hasOff = offHand.is(ModItems.OPTIC_BLAST.get());

        boolean isHelmet = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)
                .is(ModItems.TINFOIL_HAT.get());
        ItemStack tinfoilHatStack = isHelmet ? ItemStack.EMPTY : getCurioTinfoilHat(entity);
        boolean isCurioTinfoil = !tinfoilHatStack.isEmpty() && org.xeb.xeb.compat.ModCompatManager.isCurioVisible(entity, ModItems.TINFOIL_HAT.get());

        boolean isChestplate = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .is(ModItems.HOLY_MANTLE.get());
        ItemStack holyMantleStack = isChestplate ? ItemStack.EMPTY : getCurioHolyMantle(entity);
        boolean isCurioMantle = !holyMantleStack.isEmpty() && org.xeb.xeb.compat.ModCompatManager.isCurioVisible(entity, ModItems.HOLY_MANTLE.get());

        if (!hasMain && !hasOff && !isCurioTinfoil && !isCurioMantle)
            return;

        M model = this.getParentModel();
        if (model instanceof HumanoidModel<?> humanoidModel) {
            RENDERING_HEAD_LAYER = true;
            try {
                // Render Holy Mantle if worn as Curio
                if (isCurioMantle) {
                    poseStack.pushPose();
                    // Align with player's body (chest/torso) rotation and translation
                    humanoidModel.body.translateAndRotate(poseStack);
                    // Rotate Z by 180 degrees to flip right-side up
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));

                    // Render the 3D model using ItemDisplayContext.NONE to avoid vanilla json head offsets
                    Minecraft.getInstance().getItemRenderer().renderStatic(
                            holyMantleStack,
                            ItemDisplayContext.NONE,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            poseStack,
                            buffer,
                            entity.level(),
                            entity.getId());
                    poseStack.popPose();
                }

                // Render Tinfoil Hat if worn as Curio
                if (isCurioTinfoil) {
                    poseStack.pushPose();
                    // Align with player's head rotation and translation
                    humanoidModel.head.translateAndRotate(poseStack);
                    // Rotate Z by 180 degrees to flip right-side up
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
                    // Translate UP to sit on top of the head (partially inside the top head model)
                    poseStack.translate(0.0F, 0.56F, 0.0F);

                    // Render the 3D model on the head using ItemDisplayContext.NONE to avoid vanilla json head offsets
                    Minecraft.getInstance().getItemRenderer().renderStatic(
                            tinfoilHatStack,
                            ItemDisplayContext.NONE,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            poseStack,
                            buffer,
                            entity.level(),
                            entity.getId());
                    poseStack.popPose();
                }

                // Render Optic Blast if held in hand
                if (hasMain || hasOff) {
                    ItemStack opticBlastStack = hasMain ? mainHand : offHand;
                    poseStack.pushPose();
                    // Align with player's head rotation and translation
                    humanoidModel.head.translateAndRotate(poseStack);
                    // Rotate Z by 180 degrees to flip right-side up (keeps Z facing forward)
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
                    // Translate UP (Y positive is up after Z rotation) and FORWARD (Z negative is forward)
                    poseStack.translate(0.0D, 0.25D, -0.07D);
                    // Scale down by 0.625 (using positive scales only to avoid backface/lighting
                    // bugs)
                    poseStack.scale(0.625F, 0.625F, 0.625F);

                    // Render the 3D model on the head
                    Minecraft.getInstance().getItemRenderer().renderStatic(
                            opticBlastStack,
                            ItemDisplayContext.HEAD,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            poseStack,
                            buffer,
                            entity.level(),
                            entity.getId());
                    poseStack.popPose();
                }
            } finally {
                RENDERING_HEAD_LAYER = false;
            }
        }
    }

    /**
     * Cancel first-person hand rendering of Optic Blast.
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getItemStack().is(ModItems.OPTIC_BLAST.get())) {
            event.setCanceled(true);
        }
    }
}
