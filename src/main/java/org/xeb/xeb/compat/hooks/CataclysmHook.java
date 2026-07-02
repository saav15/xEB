package org.xeb.xeb.compat.hooks;

public class CataclysmHook extends AbstractModCompatHook {
    public CataclysmHook() {
        super("cataclysm");
    }

    @Override
    public void registerTypes() {
        // Bosses
        register("ignis", true);
        register("ender_guardian", true);
        register("netherite_monstrosity", true);
        register("the_harbinger", true);
        register("leviathan", true);
        register("ancient_remnant", true);

        // Mobs
        register("ignited_revenant", false);
        register("deepling", false);
        register("deepling_angler", false);
        register("deepling_brute", false);
        register("deepling_warlock", false);
        register("coralssus", false);
        register("amethyst_crab", false);
    }
}
