package org.xeb.xeb.compat.hooks;

public class AlexMobsHook extends AbstractModCompatHook {
    public AlexMobsHook() {
        super("alexsmobs");
    }

    @Override
    public void registerTypes() {
        // Bosses
        register("void_worm", true);
        register("warped_mosco", true);

        // Hostile/Common Mobs
        register("grizzly_bear", false);
        register("roadrunner", false);
        register("bone_serpent", false);
        register("gator", false);
        register("crimson_mosquito", false);
        register("mimicube", false);
        register("soul_vulture", false);
        register("spectre", false);
        register("dropbear", false);
        register("tarantula_hawk", false);
        register("giant_squid", false);
        register("cachalot_whale", false);
        register("murmur", false);
        register("rock_golem", false);
    }
}
