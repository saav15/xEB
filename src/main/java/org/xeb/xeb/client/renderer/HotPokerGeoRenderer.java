package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.xeb.xeb.client.model.HotPokerGeoModel;
import org.xeb.xeb.entity.HotPokerEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class HotPokerGeoRenderer extends GeoEntityRenderer<HotPokerEntity> {
    public HotPokerGeoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HotPokerGeoModel());

        // Capa para renderizar los ítems en las manos del Hotpoker usando los nuevos huesos locator right_hand_item y left_hand_item
        this.addRenderLayer(new BlockAndItemGeoLayer<HotPokerEntity>(this) {
            @Nullable
            @Override
            protected ItemStack getStackForBone(GeoBone bone, HotPokerEntity animatable) {
                if (bone.getName().equals("left_hand_item")) {
                    return animatable.getMainHandItem();
                }
                if (bone.getName().equals("right_hand_item")) {
                    return animatable.getOffhandItem();
                }
                return null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, HotPokerEntity animatable) {
                if (bone.getName().equals("left_hand_item") || bone.getName().equals("right_hand_item")) {
                    return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                }
                return ItemDisplayContext.NONE;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, HotPokerEntity animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                poseStack.pushPose();
                if (bone.getName().equals("left_hand_item") || bone.getName().equals("right_hand_item")) {
                    // Escala al 80% (20% reducida) y rotación a 250°
                    poseStack.scale(0.80F, 0.80F, 0.80F);
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(250.0F));
                }
                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
                poseStack.popPose();
            }
        });
    }
}
