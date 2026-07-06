package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

public class TinfoilHatItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TinfoilHatItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private software.bernie.geckolib.renderer.GeoItemRenderer<TinfoilHatItem> itemRenderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new software.bernie.geckolib.renderer.GeoItemRenderer<TinfoilHatItem>(new org.xeb.xeb.client.model.TinfoilHatGeoModel()) {
                        @Override
                        public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext transformType, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
                            if (transformType == net.minecraft.world.item.ItemDisplayContext.HEAD && !org.xeb.xeb.render.OpticBlastPlayerLayer.RENDERING_HEAD_LAYER) {
                                return;
                            }
                            super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
                        }

                        @Override
                        public void preRender(com.mojang.blaze3d.vertex.PoseStack poseStack, TinfoilHatItem animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
                            super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
                            // Shift the 3D armor model down to the block origin (since it is defined at head level Y=33.7)
                            poseStack.translate(0.0F, -2.10625F, 0.0F);
                        }
                    };
                }
                return this.itemRenderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.tinfoil_hat.desc1"));
        tooltip.add(Component.translatable("item.xeb.tinfoil_hat.desc2"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
