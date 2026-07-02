package org.xeb.xeb.compat.hooks;

public class TwilightForestHook extends AbstractModCompatHook {
    public TwilightForestHook() {
        super("twilightforest");
    }

    @Override
    public void registerTypes() {
        // Bosses
        register("naga", true);
        register("lich", true);
        register("hydra", true);
        register("ur_ghast", true);
        register("minoshroom", true);
        register("alpha_yeti", true);
        register("snow_queen", true);

        // Mobs
        register("minotaur", false);
        register("redcap", false);
        register("redcap_sapper", false);
        register("kobold", false);
        register("death_tome", false);
        register("fire_beetle", false);
        register("slime_beetle", false);
        register("pinch_beetle", false);
        register("maze_slime", false);
        register("carminite_ghastguard", false);
        register("carminite_golem", false);
        register("yeti", false);
        register("giant_miner", false);
    }
}
