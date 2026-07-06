package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.List;
import java.util.function.Consumer;

public class HolyMantleItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HolyMantleItem(Properties properties) {
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
            private software.bernie.geckolib.renderer.GeoItemRenderer<HolyMantleItem> itemRenderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new software.bernie.geckolib.renderer.GeoItemRenderer<HolyMantleItem>(new org.xeb.xeb.client.model.HolyMantleGeoModel()) {
                        @Override
                        public void preRender(com.mojang.blaze3d.vertex.PoseStack poseStack, HolyMantleItem animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
                            super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
                            // Shift the 3D model down to the body origin (since it is defined at chest level Y=24.0)
                            poseStack.translate(0.0F, -1.5F, 0.0F);
                        }
                    };
                }
                return this.itemRenderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.holy_mantle.desc1"));
        tooltip.add(Component.translatable("item.xeb.holy_mantle.desc2"));
        tooltip.add(Component.translatable("item.xeb.holy_mantle.desc3"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
