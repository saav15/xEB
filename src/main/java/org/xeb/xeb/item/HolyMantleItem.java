package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
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

public class HolyMantleItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HolyMantleItem(Properties properties) {
        super(TinfoilArmorMaterial.INSTANCE, Type.CHESTPLATE, properties);
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
            private org.xeb.xeb.client.renderer.HolyMantleGeoRenderer renderer;
            private software.bernie.geckolib.renderer.GeoItemRenderer<HolyMantleItem> itemRenderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new software.bernie.geckolib.renderer.GeoItemRenderer<HolyMantleItem>(new org.xeb.xeb.client.model.HolyMantleGeoModel());
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
                    this.renderer = new org.xeb.xeb.client.renderer.HolyMantleGeoRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
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
