package org.xeb.xeb.enchantment;

import org.xeb.xeb.Xeb;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Xeb.MODID);

    public static final RegistryObject<Enchantment> MEDALLERO =
            ENCHANTMENTS.register("medallero", MedalleroEnchantment::new);

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}
