package org.xeb.xeb.compat.hooks;

public class AquamiraeHook extends AbstractModCompatHook {
    public AquamiraeHook() {
        super("aquamirae");
    }

    @Override
    public void registerTypes() {
        // Bosses
        register("captain_cornelia", true);

        // Mobs
        register("anglerfish", false);
        register("maze", false);
        register("eel", false);
        register("maw", false);
        register("tortured_soul", false);
    }
}
