package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

        // Layer to render items held in the custom bones HANDright and HANDleft
        this.addRenderLayer(new BlockAndItemGeoLayer<HotPokerEntity>(this) {
            @Nullable
            @Override
            protected ItemStack getStackForBone(GeoBone bone, HotPokerEntity animatable) {
                if (bone.getName().equals("HANDright")) {
                    return animatable.getMainHandItem();
                }
                if (bone.getName().equals("HANDleft")) {
                    return animatable.getOffhandItem();
                }
                return null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, HotPokerEntity animatable) {
                if (bone.getName().equals("HANDright")) {
                    return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                }
                if (bone.getName().equals("HANDleft")) {
                    return ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                }
                return ItemDisplayContext.NONE;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, HotPokerEntity animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                poseStack.pushPose();
                // Standard rotation & translation offset for weapons to render correctly aligned in Geckolib hands
                poseStack.translate(0.0D, bone.getName().equals("HANDright") ? -0.25D : -0.05D, 0.0D);
                if (bone.getName().equals("HANDleft")) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                } else {
                    poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                
                // Scale weapons down by 50%
                poseStack.scale(0.5F, 0.5F, 0.5F);

                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
                poseStack.popPose();
            }
        });
    }
}
