package org.xeb.xeb.fluid;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.xeb.xeb.Xeb;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Xeb.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Xeb.MODID);

    // Fluid Types
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

    // Fluids
    public static final RegistryObject<FlowingFluid> MOLTEN_BRONZE_ELITE = FLUIDS.register("molten_bronze_elite",
            () -> new ForgeFlowingFluid.Source(ModFluids.BRONZE_ELITE_PROPERTIES));
    public static final RegistryObject<FlowingFluid> FLOWING_MOLTEN_BRONZE_ELITE = FLUIDS.register("flowing_molten_bronze_elite",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.BRONZE_ELITE_PROPERTIES));

    public static final RegistryObject<FlowingFluid> MOLTEN_SILVER_ELITE = FLUIDS.register("molten_silver_elite",
            () -> new ForgeFlowingFluid.Source(ModFluids.SILVER_ELITE_PROPERTIES));
    public static final RegistryObject<FlowingFluid> FLOWING_MOLTEN_SILVER_ELITE = FLUIDS.register("flowing_molten_silver_elite",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.SILVER_ELITE_PROPERTIES));

    public static final RegistryObject<FlowingFluid> MOLTEN_GOLD_ELITE = FLUIDS.register("molten_gold_elite",
            () -> new ForgeFlowingFluid.Source(ModFluids.GOLD_ELITE_PROPERTIES));
    public static final RegistryObject<FlowingFluid> FLOWING_MOLTEN_GOLD_ELITE = FLUIDS.register("flowing_molten_gold_elite",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.GOLD_ELITE_PROPERTIES));

    // Properties
    public static final ForgeFlowingFluid.Properties BRONZE_ELITE_PROPERTIES =
            new ForgeFlowingFluid.Properties(
                    MOLTEN_BRONZE_ELITE_TYPE,
                    MOLTEN_BRONZE_ELITE,
                    FLOWING_MOLTEN_BRONZE_ELITE
            )
            .bucket(org.xeb.xeb.item.ModItems.MOLTEN_BRONZE_ELITE_BUCKET)
            .block(org.xeb.xeb.block.ModBlocks.MOLTEN_BRONZE_ELITE_BLOCK);

    public static final ForgeFlowingFluid.Properties SILVER_ELITE_PROPERTIES =
            new ForgeFlowingFluid.Properties(
                    MOLTEN_SILVER_ELITE_TYPE,
                    MOLTEN_SILVER_ELITE,
                    FLOWING_MOLTEN_SILVER_ELITE
            )
            .bucket(org.xeb.xeb.item.ModItems.MOLTEN_SILVER_ELITE_BUCKET)
            .block(org.xeb.xeb.block.ModBlocks.MOLTEN_SILVER_ELITE_BLOCK);

    public static final ForgeFlowingFluid.Properties GOLD_ELITE_PROPERTIES =
            new ForgeFlowingFluid.Properties(
                    MOLTEN_GOLD_ELITE_TYPE,
                    MOLTEN_GOLD_ELITE,
                    FLOWING_MOLTEN_GOLD_ELITE
            )
            .bucket(org.xeb.xeb.item.ModItems.MOLTEN_GOLD_ELITE_BUCKET)
            .block(org.xeb.xeb.block.ModBlocks.MOLTEN_GOLD_ELITE_BLOCK);

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
