package org.xeb.xeb.item;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

public class CrazyDiamondHeadItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Modifiers for stand head visual positioning and scaling (feel free to adjust)
    public static double TX = 0.0D;
    public static double TY = 0.2D;
    public static double TZ = 0.0D;
    public static float SCALE = 0.7F; // 1.0F is 100%, can be lowered e.g. to 0.5F (50%)

    public CrazyDiamondHeadItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return event.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("Idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private org.xeb.xeb.client.renderer.CrazyDiamondHeadGeoRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new org.xeb.xeb.client.renderer.CrazyDiamondHeadGeoRenderer();
                }
                return this.renderer;
            }
        });
    }
}
