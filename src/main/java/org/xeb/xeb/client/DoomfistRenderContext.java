package org.xeb.xeb.client;

import net.minecraft.world.entity.LivingEntity;

import java.lang.ref.WeakReference;

/**
 * Client-side holder that tracks the {@link LivingEntity} currently being rendered, so that
 * GeckoLib {@code GeoItem} animation controllers can resolve the wielder of the Doomfist even
 * though {@code DataTickets.ENTITY} is unavailable inside the {@code BlockEntityWithoutLevelRenderer}
 * pipeline (in-hand / GUI / ground item rendering).
 *
 * <p>The render layer / event hooks set the active entity around the render call, and the animation
 * controller reads it back. A {@link WeakReference} is used so a forgotten clear() can never leak
 * an entity. All access is thread-confined to the render thread.
 */
public final class DoomfistRenderContext {

    private static WeakReference<LivingEntity> currentEntityRef = null;

    private DoomfistRenderContext() {}

    /** Sets the entity about to be rendered (called from RenderLivingEvent.Pre). */
    public static void setCurrentEntity(LivingEntity entity) {
        currentEntityRef = (entity == null) ? null : new WeakReference<>(entity);
    }

    /**
     * Returns the entity currently being rendered, or {@code null} if none is set.
     * The returned reference is only valid for the duration of the render call.
     */
    public static LivingEntity getCurrentEntity() {
        if (currentEntityRef == null) return null;
        LivingEntity entity = currentEntityRef.get();
        // If the entity was garbage-collected, drop the reference.
        if (entity == null) currentEntityRef = null;
        return entity;
    }

    /** Clears the tracked entity (called from RenderLivingEvent.Post). */
    public static void clearCurrentEntity() {
        currentEntityRef = null;
    }
}
