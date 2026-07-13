package org.xeb.xeb.fluid;

import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.xeb.xeb.Xeb;

public class ModFluidTypes {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Xeb.MODID);

    public static final RegistryObject<FluidType> MOLTEN_BRONZE_ELITE_TYPE = FLUID_TYPES.register("molten_bronze_elite",
            () -> new TintedFluidType(FluidType.Properties.create()
                    .descriptionId("fluid.xeb.molten_bronze_elite")
                    .density(3000)
                    .viscosity(6000)
                    .temperature(700)
                    .lightLevel(15), 0xFFD2691E));

    public static final RegistryObject<FluidType> MOLTEN_SILVER_ELITE_TYPE = FLUID_TYPES.register("molten_silver_elite",
            () -> new TintedFluidType(FluidType.Properties.create()
                    .descriptionId("fluid.xeb.molten_silver_elite")
                    .density(3000)
                    .viscosity(6000)
                    .temperature(950)
                    .lightLevel(15), 0xFFBDC3C7));

    public static final RegistryObject<FluidType> MOLTEN_GOLD_ELITE_TYPE = FLUID_TYPES.register("molten_gold_elite",
            () -> new TintedFluidType(FluidType.Properties.create()
                    .descriptionId("fluid.xeb.molten_gold_elite")
                    .density(3000)
                    .viscosity(6000)
                    .temperature(1200)
                    .lightLevel(15), 0xFFF1C40F));

    public static class TintedFluidType extends FluidType {
        private final int tintColor;

        public TintedFluidType(Properties properties, int tintColor) {
            super(properties);
            this.tintColor = tintColor;
        }

        @Override
        public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
                private static final net.minecraft.resources.ResourceLocation STILL = new net.minecraft.resources.ResourceLocation("minecraft:block/water_still");
                private static final net.minecraft.resources.ResourceLocation FLOWING = new net.minecraft.resources.ResourceLocation("minecraft:block/water_flow");

                @Override
                public net.minecraft.resources.ResourceLocation getStillTexture() {
                    return STILL;
                }

                @Override
                public net.minecraft.resources.ResourceLocation getFlowingTexture() {
                    return FLOWING;
                }

                @Override
                public int getTintColor() {
                    return tintColor;
                }
            });
        }
    }
}
