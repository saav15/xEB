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

    public static final RegistryObject<CreativeModeTab> XEB_TAB = CREATIVE_MODE_TABS.register("xeb_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativeTab.xeb"))
                    .icon(() -> new ItemStack(ModItems.BRASS_KNUCKLES.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BRASS_KNUCKLES.get());
                        output.accept(ModItems.MOB_ENERGY.get());
                        output.accept(ModItems.TINFOIL_HAT.get());
                        output.accept(ModItems.DEMON_CORE.get());
                        output.accept(ModItems.HOT_POTATO.get());
                        output.accept(ModItems.HOLY_MANTLE.get());
                        output.accept(ModItems.DOOMFIST.get());
                        output.accept(ModItems.DOOMFIST_V2.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
