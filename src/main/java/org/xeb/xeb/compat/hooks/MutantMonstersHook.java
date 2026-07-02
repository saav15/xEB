package org.xeb.xeb.compat.hooks;

public class MutantMonstersHook extends AbstractModCompatHook {
    public MutantMonstersHook() {
        super("mutantmonsters");
    }

    @Override
    public void registerTypes() {
        // Bosses
        register("mutant_zombie", true);
        register("mutant_skeleton", true);
        register("mutant_creeper", true);
        register("mutant_enderman", true);
        register("mutant_snow_golem", true);
    }
}
