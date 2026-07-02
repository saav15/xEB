package org.xeb.xeb.compat.hooks;

public class MowziesMobsHook extends AbstractModCompatHook {
    public MowziesMobsHook() {
        super("mowziesmobs");
    }

    @Override
    public void registerTypes() {
        // Bosses
        register("ferrous_wroughtnaut", true);
        register("barako", true);
        register("frostmaw", true);

        // Mobs
        register("foliaath", false);
        register("barakoan_rider", false);
        register("barakoana", false);
        register("barakoan_sentinel", false);
        register("grottol", false);
        register("naga", false);
    }
}
