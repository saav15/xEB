package org.xeb.xeb.effect;

import org.xeb.xeb.Xeb;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Xeb.MODID);

    // --- Existing effects ---
    public static final RegistryObject<MobEffect> HOLY_SHIELD  = EFFECTS.register("holy_shield",  HolyShieldEffect::new);
    public static final RegistryObject<MobEffect> PETRIFY      = EFFECTS.register("petrify",      PetrifyEffect::new);
    public static final RegistryObject<MobEffect> TARRED       = EFFECTS.register("tarred",       TarredEffect::new);
    public static final RegistryObject<MobEffect> SANDSTORM    = EFFECTS.register("sandstorm",    SandstormEffect::new);
    public static final RegistryObject<MobEffect> MANA_DRAIN   = EFFECTS.register("mana_drain",   ManaDrainEffect::new);
    public static final RegistryObject<MobEffect> BURN         = EFFECTS.register("burn",         BurnEffect::new);
    public static final RegistryObject<MobEffect> MADNESS      = EFFECTS.register("madness",      MadnessEffect::new);
    public static final RegistryObject<MobEffect> ALL_STATS_DOWN = EFFECTS.register("all_stats_down", AllStatsDownEffect::new);
    public static final RegistryObject<MobEffect> ALL_STATS_UP   = EFFECTS.register("all_stats_up",   AllStatsUpEffect::new);
    public static final RegistryObject<MobEffect> KINETIC_SPIKES = EFFECTS.register("kinetic_spikes", KineticSpikesEffect::new);

    // --- New potion effects ---
    public static final RegistryObject<MobEffect> BLIND          = EFFECTS.register("blind",           BlindEffect::new);
    public static final RegistryObject<MobEffect> MAGIC_WEAKNESS = EFFECTS.register("magic_weakness",  MagicWeaknessEffect::new);
    public static final RegistryObject<MobEffect> MANA_LEECH     = EFFECTS.register("mana_leech",      ManaLeechEffect::new);
    public static final RegistryObject<MobEffect> MARKED         = EFFECTS.register("marked",          MarkedEffect::new);
    public static final RegistryObject<MobEffect> NO_HEALTH_REGEN = EFFECTS.register("no_health_regen", NoHealthRegenEffect::new);
    public static final RegistryObject<MobEffect> NO_MANA_REGEN   = EFFECTS.register("no_mana_regen",   NoManaRegenEffect::new);
    public static final RegistryObject<MobEffect> FEAR            = EFFECTS.register("fear",            FearEffect::new);
    public static final RegistryObject<MobEffect> EXHAUSTED       = EFFECTS.register("exhausted",       ExhaustedEffect::new);
    public static final RegistryObject<MobEffect> ADRENALINE      = EFFECTS.register("adrenaline",      AdrenalineEffect::new);
    public static final RegistryObject<MobEffect> DOOMED          = EFFECTS.register("doomed",          DoomedEffect::new);
    public static final RegistryObject<MobEffect> CHARGED_FIST    = EFFECTS.register("charged_fist",    ChargedFistEffect::new);
    public static final RegistryObject<MobEffect> BRUISE          = EFFECTS.register("bruise",          BruiseEffect::new);
    public static final RegistryObject<MobEffect> BOUNTY          = EFFECTS.register("bounty",          BountyEffect::new);
    public static final RegistryObject<MobEffect> REFLECT         = EFFECTS.register("reflect",         ReflectEffect::new);
    public static final RegistryObject<MobEffect> DELAYED_PAIN    = EFFECTS.register("delayed_pain",    DelayedPainEffect::new);
    public static final RegistryObject<MobEffect> CHARRED_BURN    = EFFECTS.register("charred_burn",    CharredBurnEffect::new);
    public static final RegistryObject<MobEffect> CHARMED         = EFFECTS.register("charmed",         CharmedEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}

