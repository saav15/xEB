package org.xeb.xeb.compat.hooks;

public class AlexCavesHook extends AbstractModCompatHook {
    public AlexCavesHook() {
        super("alexscaves");
    }

    @Override
    public void registerTypes() {
        // Bosses
        register("tremorzilla", true);
        register("luxtructosaurus", true);
        register("hullbreaker", true);

        // Mobs
        register("subterranodon", false);
        register("vallumraptor", false);
        register("grottol", false);
        register("underperch", false);
        register("corrodent", false);
        register("vesper", false);
        register("deep_one", false);
        register("deep_one_knight", false);
        register("deep_one_mage", false);
        register("mine_guardian", false);
        register("radgill", false);
        register("watcher", false);
        register("brainiac", false);
    }
}
