package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.List;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

public class TinfoilHatItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TinfoilHatItem(Properties properties) {
        super(TinfoilArmorMaterial.INSTANCE, Type.HELMET, properties);
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
            private org.xeb.xeb.client.renderer.TinfoilHatGeoRenderer renderer;
            private software.bernie.geckolib.renderer.GeoItemRenderer<TinfoilHatItem> itemRenderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new software.bernie.geckolib.renderer.GeoItemRenderer<TinfoilHatItem>(new org.xeb.xeb.client.model.TinfoilHatGeoModel()) {
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

            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    net.minecraft.world.entity.LivingEntity livingEntity,
                    ItemStack itemStack,
                    net.minecraft.world.entity.EquipmentSlot equipmentSlot,
                    net.minecraft.client.model.HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new org.xeb.xeb.client.renderer.TinfoilHatGeoRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().contains("curios")) {
                return ImmutableMultimap.of();
            }
        }
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().contains("curios")) {
                return ImmutableMultimap.of();
            }
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.tinfoil_hat.desc1"));
        tooltip.add(Component.translatable("item.xeb.tinfoil_hat.desc2"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
