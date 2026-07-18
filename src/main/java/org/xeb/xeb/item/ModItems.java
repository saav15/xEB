package org.xeb.xeb.item;

import org.xeb.xeb.Xeb;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Xeb.MODID);

    public static final RegistryObject<Item> BRASS_KNUCKLES = ITEMS.register("brass_knuckles",
            () -> new BrassKnucklesItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MOB_ENERGY = ITEMS.register("mob_energy",
            () -> new MobEnergyItem(new Item.Properties().stacksTo(1).durability(2).craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE)));

    public static final RegistryObject<Item> TINFOIL_HAT = ITEMS.register("tinfoil_hat",
            () -> new TinfoilHatItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DEMON_CORE = ITEMS.register("demon_core",
            () -> new DemonCoreItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HOT_POTATO = ITEMS.register("hot_potato",
            () -> new HotPotatoItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HOLY_MANTLE = ITEMS.register("holy_mantle",
            () -> new HolyMantleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DOOMFIST = ITEMS.register("doomfist",
            () -> new DoomfistItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DOOMFIST_V2 = ITEMS.register("doomfist_v2",
            () -> new DoomfistV2Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MOON_TEAR = ITEMS.register("moon_tear",
            () -> new MoonTearItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> WITHERFIST_SPAWN_EGG = ITEMS.register("witherfist_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    org.xeb.xeb.entity.ModEntities.WITHERFIST,
                    0x1a0030, 0xaa00ff, // Dark purple shell, bright violet spots
                    new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> TANKWITHERFIST_SPAWN_EGG = ITEMS.register("tankwitherfist_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    org.xeb.xeb.entity.ModEntities.TANKWITHERFIST,
                    0x1a0030, 0xff0000, // Dark purple shell, red spots
                    new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> HOT_POKER_SPAWN_EGG = ITEMS.register("hot_poker_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    org.xeb.xeb.entity.ModEntities.HOT_POKER,
                    0x7f2400, 0xff7f00, // Dark red-orange shell, bright orange spots
                    new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> OPTIC_BLAST = ITEMS.register("optic_blast",
            () -> new OpticBlastItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GOLDEN_FLOWER = ITEMS.register("golden_flower",
            () -> new org.xeb.xeb.item.GoldenFlowerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> QUANTUM_CAT_BARRAGE = ITEMS.register("quantum_cat_barrage",
            () -> new QuantumCatBarrageItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DOGMA = ITEMS.register("dogma",
            () -> new DogmaItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> OMEGA_FLOWERY = ITEMS.register("omega_flowery",
            () -> new OmegaFloweryItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> BROKEN_DIAMOND = ITEMS.register("broken_diamond",
            () -> new org.xeb.xeb.item.BrokenDiamondItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> THE_TEARS = ITEMS.register("the_tears",
            () -> new org.xeb.xeb.item.TheTearsItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> MECHA_OVERDRIVE = ITEMS.register("mecha_overdrive",
            () -> new MechaOverdriveItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HOLY_DUALITY_BLADE = ITEMS.register("holy_duality_blade",
            () -> new HolyDualityBladeItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SMART_HALBERD = ITEMS.register("smart_halberd",
            () -> new SmartHalberdItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENIGMA_BIOS = ITEMS.register("enigma_bios",
            () -> new EnigmaBiosItem(new Item.Properties().stacksTo(1)));

    // ── Elite Bits (drops de mobs elite) ──
    public static final RegistryObject<Item> BRONZE_ELITE_BIT = ITEMS.register("bronze_elite_bit",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> SILVER_ELITE_BIT = ITEMS.register("silver_elite_bit",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> GOLD_ELITE_BIT = ITEMS.register("gold_elite_bit",
            () -> new Item(new Item.Properties().stacksTo(64)));

    // ── Elite Ingots (materiales base) ──
    public static final RegistryObject<Item> BRONZE_ELITE_INGOT = ITEMS.register("bronze_elite_ingot",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> SILVER_ELITE_INGOT = ITEMS.register("silver_elite_ingot",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> GOLD_ELITE_INGOT = ITEMS.register("gold_elite_ingot",
            () -> new Item(new Item.Properties().stacksTo(64)));

    // ── Molten Buckets (Tinkers molten parts) ──
    public static final RegistryObject<Item> MOLTEN_BRONZE_ELITE_BUCKET = ITEMS.register("molten_bronze_elite_bucket",
            () -> new net.minecraft.world.item.BucketItem(() -> org.xeb.xeb.fluid.ModFluids.MOLTEN_BRONZE_ELITE.get(),
                    new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final RegistryObject<Item> MOLTEN_SILVER_ELITE_BUCKET = ITEMS.register("molten_silver_elite_bucket",
            () -> new net.minecraft.world.item.BucketItem(() -> org.xeb.xeb.fluid.ModFluids.MOLTEN_SILVER_ELITE.get(),
                    new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final RegistryObject<Item> MOLTEN_GOLD_ELITE_BUCKET = ITEMS.register("molten_gold_elite_bucket",
            () -> new net.minecraft.world.item.BucketItem(() -> org.xeb.xeb.fluid.ModFluids.MOLTEN_GOLD_ELITE.get(),
                    new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<Item> MOLTEN_BRONZE_ELITE_BLOCK_ITEM = ITEMS.register("molten_bronze_elite",
            () -> new net.minecraft.world.item.BlockItem(org.xeb.xeb.block.ModBlocks.MOLTEN_BRONZE_ELITE_BLOCK.get(),
                    new Item.Properties()));
    public static final RegistryObject<Item> MOLTEN_SILVER_ELITE_BLOCK_ITEM = ITEMS.register("molten_silver_elite",
            () -> new net.minecraft.world.item.BlockItem(org.xeb.xeb.block.ModBlocks.MOLTEN_SILVER_ELITE_BLOCK.get(),
                    new Item.Properties()));
    public static final RegistryObject<Item> MOLTEN_GOLD_ELITE_BLOCK_ITEM = ITEMS.register("molten_gold_elite",
            () -> new net.minecraft.world.item.BlockItem(org.xeb.xeb.block.ModBlocks.MOLTEN_GOLD_ELITE_BLOCK.get(),
                    new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
