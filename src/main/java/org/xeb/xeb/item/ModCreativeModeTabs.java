package org.xeb.xeb.item;

import org.xeb.xeb.Xeb;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Xeb.MODID);

    public static final RegistryObject<CreativeModeTab> ELITE_BUFFS_TAB = CREATIVE_MODE_TABS.register("elite_buffs_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativeTab.xeb.elite_buffs"))
                    .icon(() -> new ItemStack(ModItems.MOON_TEAR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MOON_TEAR.get());
                        output.accept(ModItems.DEMON_CORE.get());
                        output.accept(ModItems.TINFOIL_HAT.get());
                        output.accept(ModItems.HOLY_MANTLE.get());
                        output.accept(ModItems.BRASS_KNUCKLES.get());
                        output.accept(ModItems.MOB_ENERGY.get());
                        output.accept(ModItems.HOT_POTATO.get());
                        output.accept(ModItems.ENIGMA_BIOS.get());
                        output.accept(ModItems.HOT_POKER_SPAWN_EGG.get());

                        // Add Medallero III Enchanted Book
                        ItemStack book = net.minecraft.world.item.Items.ENCHANTED_BOOK.getDefaultInstance();
                        net.minecraft.world.item.enchantment.EnchantmentInstance inst =
                                new net.minecraft.world.item.enchantment.EnchantmentInstance(org.xeb.xeb.enchantment.ModEnchantments.MEDALLERO.get(), 3);
                        net.minecraft.world.item.EnchantedBookItem.addEnchantment(book, inst);
                        output.accept(book);
                    })
                    .build()
    );

    public static final RegistryObject<CreativeModeTab> XEB_TINKERS_TAB = CREATIVE_MODE_TABS.register("xeb_tinkers_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativeTab.xeb.xeb_tinkers"))
                    .icon(() -> new ItemStack(ModItems.BRONZE_ELITE_INGOT.get()))
                    .displayItems((parameters, output) -> {
                        // Elite Bits
                        output.accept(ModItems.BRONZE_ELITE_BIT.get());
                        output.accept(ModItems.SILVER_ELITE_BIT.get());
                        output.accept(ModItems.GOLD_ELITE_BIT.get());
                        
                        // Elite Ingots
                        output.accept(ModItems.BRONZE_ELITE_INGOT.get());
                        output.accept(ModItems.SILVER_ELITE_INGOT.get());
                        output.accept(ModItems.GOLD_ELITE_INGOT.get());
                        
                        // Molten Buckets
                        output.accept(ModItems.MOLTEN_BRONZE_ELITE_BUCKET.get());
                        output.accept(ModItems.MOLTEN_SILVER_ELITE_BUCKET.get());
                        output.accept(ModItems.MOLTEN_GOLD_ELITE_BUCKET.get());

                        // Essences (all tiers)
                        for (var reg : org.xeb.xeb.loot.EssenceRegistry.getAll().values()) {
                            net.minecraft.world.item.ItemStack commonStack = new net.minecraft.world.item.ItemStack(reg.get());
                            commonStack.getOrCreateTag().putString("MedallionTier", "COMMON");
                            output.accept(commonStack);
                            
                            net.minecraft.world.item.ItemStack rareStack = new net.minecraft.world.item.ItemStack(reg.get());
                            rareStack.getOrCreateTag().putString("MedallionTier", "RARE");
                            output.accept(rareStack);
                            
                            net.minecraft.world.item.ItemStack legendaryStack = new net.minecraft.world.item.ItemStack(reg.get());
                            legendaryStack.getOrCreateTag().putString("MedallionTier", "LEGENDARY");
                            output.accept(legendaryStack);
                        }
                    })
                    .build()
    );

    public static final RegistryObject<CreativeModeTab> ETERNAL_BULWARK_TAB = CREATIVE_MODE_TABS.register("eternal_bulwark_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativeTab.xeb.eternal_bulwark"))
                    .icon(() -> new ItemStack(ModItems.OPTIC_BLAST.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.OPTIC_BLAST.get());
                        output.accept(ModItems.DOOMFIST.get());
                        output.accept(ModItems.DOOMFIST_V2.get());
                        output.accept(ModItems.GOLDEN_FLOWER.get());
                        output.accept(ModItems.THE_TEARS.get());
                        output.accept(ModItems.BROKEN_DIAMOND.get());
                        output.accept(ModItems.QUANTUM_CAT_BARRAGE.get());
                        output.accept(ModItems.DOGMA.get());
                        output.accept(ModItems.OMEGA_FLOWERY.get());
                        output.accept(ModItems.MECHA_OVERDRIVE.get());
                        output.accept(ModItems.HOLY_DUALITY_BLADE.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
