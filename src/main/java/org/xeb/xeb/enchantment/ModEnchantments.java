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

    // 10 new custom enchantments
    public static final RegistryObject<Enchantment> RIFT_SIPHON =
            ENCHANTMENTS.register("rift_siphon", RiftSiphonEnchantment::new);

    public static final RegistryObject<Enchantment> DECOHERENCE =
            ENCHANTMENTS.register("decoherence", DecoherenceEnchantment::new);

    public static final RegistryObject<Enchantment> FEEDBACK_LOOP =
            ENCHANTMENTS.register("feedback_loop", FeedbackLoopEnchantment::new);

    public static final RegistryObject<Enchantment> RESONANT_CLEAVE =
            ENCHANTMENTS.register("resonant_cleave", ResonantCleaveEnchantment::new);

    public static final RegistryObject<Enchantment> CHRONOSTASIS =
            ENCHANTMENTS.register("chronostasis", ChronostasisEnchantment::new);

    public static final RegistryObject<Enchantment> MEDALLION_OVERLOAD =
            ENCHANTMENTS.register("medallion_overload", MedallionOverloadEnchantment::new);

    public static final RegistryObject<Enchantment> FLORICULTURAL_ZEAL =
            ENCHANTMENTS.register("floricultural_zeal", FloriculturalZealEnchantment::new);

    public static final RegistryObject<Enchantment> QUANTUM_PHASE =
            ENCHANTMENTS.register("quantum_phase", QuantumPhaseEnchantment::new);

    public static final RegistryObject<Enchantment> PURIFYING_BEAM =
            ENCHANTMENTS.register("purifying_beam", PurifyingBeamEnchantment::new);

    public static final RegistryObject<Enchantment> AEGIS_SHATTER =
            ENCHANTMENTS.register("aegis_shatter", AegisShatterEnchantment::new);

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }

    public static boolean isModEnchantment(Enchantment enchantment) {
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        return id != null && id.getNamespace().equals(Xeb.MODID);
    }

    public static boolean isSpecialItem(net.minecraft.world.item.Item item) {
        return item == org.xeb.xeb.item.ModItems.GOLDEN_FLOWER.get()
            || item == org.xeb.xeb.item.ModItems.DOOMFIST.get()
            || item == org.xeb.xeb.item.ModItems.DOOMFIST_V2.get()
            || item == org.xeb.xeb.item.ModItems.OPTIC_BLAST.get()
            || item == org.xeb.xeb.item.ModItems.HOLY_DUALITY_BLADE.get()
            || item == org.xeb.xeb.item.ModItems.MECHA_OVERDRIVE.get()
            || item == org.xeb.xeb.item.ModItems.BROKEN_DIAMOND.get()
            || item == org.xeb.xeb.item.ModItems.THE_TEARS.get()
            || item == org.xeb.xeb.item.ModItems.SMART_HALBERD.get();
    }
}
