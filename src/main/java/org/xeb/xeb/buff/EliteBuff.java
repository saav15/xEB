package org.xeb.xeb.buff;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

public abstract class EliteBuff {
    private final String id;
    private final String displayName;
    private final BuffType buffType;
    private final int color;
    private final double weight;
    // Whether multiple medallions of this buff on the same entity stack their effects
    private final boolean stackable;

    public EliteBuff(String id, String displayName, BuffType buffType, int color, double weight) {
        this(id, displayName, buffType, color, weight, true);
    }

    public EliteBuff(String id, String displayName, BuffType buffType, int color, double weight, boolean stackable) {
        this.id = id;
        this.displayName = displayName;
        this.buffType = buffType;
        this.color = color;
        this.weight = weight;
        this.stackable = stackable;
    }

    public String getId() {
        return id;
    }

    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("xeb.buff." + this.id);
    }

    public BuffType getBuffType() {
        return buffType;
    }

    public int getColor() {
        return color;
    }

    public double getWeight() {
        return weight;
    }

    public boolean isStackable() {
        return stackable;
    }

    /**
     * Returns the set of buff IDs that are incompatible with this buff.
     * If this buff is assigned, none of the returned IDs will be rolled afterwards.
     * Override in concrete buffs to declare conflicts; returns empty set by default.
     */
    public java.util.Set<String> getConflicts() {
        return java.util.Collections.emptySet();
    }

    // --- Core lifecycle methods (UUID-aware versions for stacking) ---

    /**
     * Called when a medallion is attached to an entity.
     * medallionId is the unique UUID of THIS specific medallion instance,
     * used as the attribute modifier UUID so stacking works properly.
     */
    public void onAttach(LivingEntity entity, UUID medallionId) {
        // Default: delegate to legacy method for buffs that don't use the UUID
        onAttach(entity);
    }

    /**
     * Called when a medallion is detached from an entity.
     * medallionId matches the UUID passed during onAttach so the correct
     * attribute modifier can be removed.
     */
    public void onDetach(LivingEntity entity, UUID medallionId) {
        // Default: delegate to legacy method for buffs that don't use the UUID
        onDetach(entity);
    }

    // Legacy abstract methods — override onAttach(entity, uuid) instead for stackable attribute buffs
    public abstract void onAttach(LivingEntity entity);
    public abstract void onDetach(LivingEntity entity);

    public abstract void onServerTick(LivingEntity entity, ServerLevel level);

    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {}
    public void onDamageDealt(LivingEntity entity, LivingHurtEvent event) {}
    public void onHurt(LivingEntity entity, LivingHurtEvent event) {}
    public void onDeath(LivingEntity entity, LivingDeathEvent event) {}
    public void onKill(LivingEntity entity, LivingDeathEvent event) {}
    public void onProjectileImpact(LivingEntity entity, ProjectileImpactEvent event) {}
}
