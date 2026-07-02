package org.xeb.xeb.compat.hooks;

public class BornInChaosHook extends AbstractModCompatHook {
    public BornInChaosHook() {
        super("born_in_chaos_v1");
    }

    @Override
    public void registerTypes() {
        // Bosses
        register("sir_pumpkinhead", true);
        register("pumpkin_spirit", true);
        register("decayed_collector", true);

        // Mobs
        register("nightmare_stalker", false);
        register("corrupted_baron", false);
        register("bone_knight", false);
        register("withered_sir", false);
        register("baby_skeleton", false);
        register("dark_vort", false);
        register("flesh_scavenger", false);
        register("skeleton_demoman", false);
    }
}
