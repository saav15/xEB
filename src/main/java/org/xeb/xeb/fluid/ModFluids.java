package org.xeb.xeb.fluid;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.xeb.xeb.Xeb;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Xeb.MODID);

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
                    ModFluidTypes.MOLTEN_BRONZE_ELITE_TYPE,
                    MOLTEN_BRONZE_ELITE,
                    FLOWING_MOLTEN_BRONZE_ELITE
            )
            .bucket(org.xeb.xeb.item.ModItems.MOLTEN_BRONZE_ELITE_BUCKET)
            .block(org.xeb.xeb.block.ModBlocks.MOLTEN_BRONZE_ELITE_BLOCK);

    public static final ForgeFlowingFluid.Properties SILVER_ELITE_PROPERTIES =
            new ForgeFlowingFluid.Properties(
                    ModFluidTypes.MOLTEN_SILVER_ELITE_TYPE,
                    MOLTEN_SILVER_ELITE,
                    FLOWING_MOLTEN_SILVER_ELITE
            )
            .bucket(org.xeb.xeb.item.ModItems.MOLTEN_SILVER_ELITE_BUCKET)
            .block(org.xeb.xeb.block.ModBlocks.MOLTEN_SILVER_ELITE_BLOCK);

    public static final ForgeFlowingFluid.Properties GOLD_ELITE_PROPERTIES =
            new ForgeFlowingFluid.Properties(
                    ModFluidTypes.MOLTEN_GOLD_ELITE_TYPE,
                    MOLTEN_GOLD_ELITE,
                    FLOWING_MOLTEN_GOLD_ELITE
            )
            .bucket(org.xeb.xeb.item.ModItems.MOLTEN_GOLD_ELITE_BUCKET)
            .block(org.xeb.xeb.block.ModBlocks.MOLTEN_GOLD_ELITE_BLOCK);

    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
    }
}
