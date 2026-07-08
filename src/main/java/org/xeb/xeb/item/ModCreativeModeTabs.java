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
                        output.accept(ModItems.HOT_POKER_SPAWN_EGG.get());
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
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
