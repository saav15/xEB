package org.xeb.xeb.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.RawAnimation;

import javax.annotation.Nullable;

/**
 * Shared animation-resolution logic for the Doomfist (v1) and Doomfist V2 items.
 *
 * <p>Both items expose the same four GeckoLib animations — {@code idle}, {@code charge},
 * {@code punch} and {@code block} — and select between them based on the wielder's state.
 * The wrinkle is that inside the {@code BlockEntityWithoutLevelRenderer} pipeline GeckoLib's
 * {@link DataTickets#ENTITY} is unavailable, so the wielder must be recovered through the
 * render-context tracker (see {@link DoomfistRenderContext}).
 */
public final class DoomfistAnimationResolver {

    private DoomfistAnimationResolver() {}

    /**
     * Resolves which raw animation name the Doomfist should play, based on the wielder's state.
     *
     * @param entityFromTicket the entity from {@code DataTickets.ENTITY} (may be {@code null})
     * @param item             the item instance, used to confirm the wielder is actually holding it
     * @return the animation name ({@code "block"}, {@code "charge"}, {@code "punch"} or {@code "idle"})
     */
    public static String resolveAnimationName(net.minecraft.world.entity.Entity entityFromTicket, Item item) {
        LivingEntity living = resolveWielder(entityFromTicket, item);
        if (living == null) {
            return "idle"; // GUI / ground item render
        }

        // Priority: block > charge > punch > idle
        if (living.getPersistentData().getBoolean("xebPowerBlocking")) {
            return "block";
        }
        if (living.isUsingItem() && living.getUseItem().getItem() == item) {
            return "charge";
        }
        if (living.getPersistentData().getBoolean("xebDoomfistDashing")) {
            return "punch";
        }
        return "idle";
    }

    /**
     * Recovers the living entity holding the item, in priority order:
     * <ol>
     *   <li>The GeckoLib entity data ticket (works for entity-held rendering).</li>
     *   <li>The render-context tracker (third-person via RenderLivingEvent, first-person via RenderHandEvent).</li>
     *   <li>The local client player (last-resort fallback for first-person).</li>
     * </ol>
     * The result is validated: the entity must actually hold the item, otherwise {@code null} is
     * returned so the caller defaults to {@code idle} (this is what happens for GUI/ground renders
     * where no real wielder exists).
     */
    @Nullable
    private static LivingEntity resolveWielder(net.minecraft.world.entity.Entity entityFromTicket, Item item) {
        // 1) GeckoLib ticket
        if (entityFromTicket instanceof LivingEntity living && isHolding(living, item)) {
            return living;
        }

        // 2) Render-context tracker (set by DoomfistRenderLayer event hooks)
        LivingEntity tracked = DoomfistRenderContext.getCurrentEntity();
        if (tracked != null && (tracked instanceof Player || isHolding(tracked, item))) {
            return tracked;
        }

        // 3) Last-resort: local player (first-person hand render may bypass living render events)
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && isHolding(mc.player, item)) {
                return mc.player;
            }
        } catch (Throwable ignored) {
            // Safe in headless/test environments
        }

        return null;
    }

    private static boolean isHolding(LivingEntity entity, Item item) {
        return entity.getMainHandItem().getItem() == item
                || entity.getOffhandItem().getItem() == item;
    }
}
