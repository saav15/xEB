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

    public static final RegistryObject<Enchantment> FLORICULTURA =
            ENCHANTMENTS.register("floricultura", FloriculturaEnchantment::new);

    public static final RegistryObject<Enchantment> SPIKE_FRENZY =
            ENCHANTMENTS.register("spike_frenzy", SpikeFrenzyEnchantment::new);

    public static final RegistryObject<Enchantment> IMPACT_OVERLOAD =
            ENCHANTMENTS.register("impact_overload", ImpactOverloadEnchantment::new);

    public static final RegistryObject<Enchantment> REFRACTION =
            ENCHANTMENTS.register("refraction", RefractionEnchantment::new);

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}
