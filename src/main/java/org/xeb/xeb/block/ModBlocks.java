package org.xeb.xeb.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.fluid.ModFluids;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Xeb.MODID);

    public static final RegistryObject<LiquidBlock> MOLTEN_BRONZE_ELITE_BLOCK = BLOCKS.register("molten_bronze_elite",
            () -> new LiquidBlock(() -> (FlowingFluid) ModFluids.MOLTEN_BRONZE_ELITE.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .lightLevel(s -> 15)));

    public static final RegistryObject<LiquidBlock> MOLTEN_SILVER_ELITE_BLOCK = BLOCKS.register("molten_silver_elite",
            () -> new LiquidBlock(() -> (FlowingFluid) ModFluids.MOLTEN_SILVER_ELITE.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .lightLevel(s -> 15)));

    public static final RegistryObject<LiquidBlock> MOLTEN_GOLD_ELITE_BLOCK = BLOCKS.register("molten_gold_elite",
            () -> new LiquidBlock(() -> (FlowingFluid) ModFluids.MOLTEN_GOLD_ELITE.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .lightLevel(s -> 15)));

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
