package org.xeb.xeb.compat.hooks;

import org.xeb.xeb.compat.CompatHook;
import org.xeb.xeb.compat.ModCompatManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Base class for modded-mob compat hooks that register entity types as bosses
 * or eligible mobs. Subclasses only need to supply the mod ID and call
 * {@link #register(String, boolean)} in {@link #registerTypes()}.
 */
public abstract class AbstractModCompatHook implements CompatHook {

    private final String modId;

    protected AbstractModCompatHook(String modId) {
        this.modId = modId;
    }

    /**
     * Looks up an entity type by name from this hook's mod and registers it as
     * either a boss or an eligible mob.
     *
     * @param name   the entity type name within the mod's namespace
     * @param isBoss true to register as a boss, false for eligible mob
     */
    protected void register(String name, boolean isBoss) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(modId, name));
        if (type != null) {
            if (isBoss) {
                ModCompatManager.registerBoss(type);
            } else {
                ModCompatManager.registerEligible(type);
            }
        }
    }
}
