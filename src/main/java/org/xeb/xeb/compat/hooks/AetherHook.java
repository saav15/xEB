package org.xeb.xeb.compat.hooks;

public class AetherHook extends AbstractModCompatHook {
    public AetherHook() {
        super("aether");
    }

    @Override
    public void registerTypes() {
        // Bosses
        register("slider", true);
        register("valkyrie_queen", true);
        register("sun_spirit", true);

        // Mobs
        register("cockatrice", false);
        register("sentry", false);
        register("mimic", false);
        register("zephyr", false);
        register("valkyrie", false);
    }
}
