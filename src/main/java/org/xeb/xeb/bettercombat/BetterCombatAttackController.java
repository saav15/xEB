package org.xeb.xeb.bettercombat;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Drives player attacks in a way that BetterCombat recognizes.
 *
 * <h3>Why this exists</h3>
 * BetterCombat intercepts the player's <b>real attack-key input</b> (the {@code keyAttack}
 * press) via a client hook/mixin and, for compatible weapons, cancels the vanilla pipeline to
 * play its own combo animations + AoE hits. The previous Madness bot invoked
 * {@code Minecraft.startAttack()} reflectively; that is the internal routine vanilla runs
 * <i>after</i> the input event, so BetterCombat never sees it — the bot therefore attacked
 * "like vanilla" even with BetterCombat installed.
 *
 * <p>The fix is to reproduce the exact input BetterCombat listens for:
 * <ol>
 *   <li>Prefer a direct reflective call into BetterCombat's client attack handler (best fidelity).</li>
 *   <li>Otherwise simulate a real {@link KeyMapping#click()} on the attack key, which fires the
 *       input event BetterCombat hooks. This is the reliable generic path.</li>
 *   <li>Finally fall back to the vanilla {@code startAttack()} / {@code gameMode.attack()} path
 *       when neither of the above is available (e.g. BC not loaded).</li>
 * </ol>
 *
 * <p>All BetterCombat access is reflective so the mod stays a soft dependency.
 */
@OnlyIn(Dist.CLIENT)
public final class BetterCombatAttackController {
    private static final Logger LOGGER = LogUtils.getLogger();

    private BetterCombatAttackController() {}

    // --- BetterCombat reflective handles (resolved lazily, cached) ---
    private static volatile boolean bcResolved = false;
    private static volatile boolean bcAvailable = false;
    // A no-arg static method that kicks off a BetterCombat client attack, if discoverable.
    private static java.lang.reflect.Method bcStartAttackMethod;
    // A way to query "is an attack currently playing" to avoid spamming. Field or method.
    private static java.lang.reflect.Method bcIsAttackingMethod;
    private static java.lang.reflect.Field bcIsAttackingField;
    private static Object bcAttackHandlerInstance; // resolved singleton if needed

    /**
     * Triggers one attack attempt. Returns {@code true} if an attack was (very likely) issued.
     *
     * @param target the entity to attack (used by the vanilla fallback)
     */
    public static boolean triggerAttack(Minecraft mc, LocalPlayer player, LivingEntity target) {
        if (mc == null || player == null) return false;

        // 1) Try the direct BetterCombat entry point first.
        if (tryBetterCombatDirect()) {
            return true;
        }

        // 2) Try simulating a raw attack-key press event. This keeps keyAttack down
        //    for 1 tick, allowing Better Combat's tick/input handlers to intercept it.
        if (simulateAttackKeyPress(mc)) {
            return true;
        }

        // 3) Fall back to Minecraft's official startAttack() routine.
        if (triggerVanillaFallback(mc, player, target)) {
            return true;
        }

        return false;
    }

    /**
     * Indicates whether an attack/animation is currently in progress according to BetterCombat,
     * so the caller can avoid stacking a new attack on top of a running combo. When BetterCombat
     * state can't be read, returns {@code false} (caller must rely on its own cadence timer).
     */
    public static boolean isAttackInProgress(Minecraft mc, LocalPlayer player) {
        // 1) Query the official Better Combat client API interface if available.
        try {
            Class<?> clientClass = null;
            String[] candidateNames = {
                "net.bettercombat.api.client.MinecraftClient_BetterCombat",
                "net.bettercombat.client.MinecraftClient_BetterCombat",
                "net.bettercombat.client.MinecraftClientExtension",
                "net.bettercombat.api.client.MinecraftClientExtension"
            };
            for (String name : candidateNames) {
                try {
                    clientClass = Class.forName(name);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            if (clientClass != null && clientClass.isInstance(mc)) {
                java.lang.reflect.Method m = null;
                try {
                    m = clientClass.getMethod("isWeaponSwingInProgress");
                } catch (NoSuchMethodException e) {
                    m = clientClass.getMethod("getIsWeaponSwingInProgress");
                }
                return (Boolean) m.invoke(mc);
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] BC client API isWeaponSwingInProgress not available: {}", e.getMessage());
        }

        // 2) Fallback to internal reflection-based checks.
        try {
            resolveBetterCombat();
            if (!bcAvailable) return false;
            if (bcIsAttackingMethod != null) {
                Object self = bcAttackHandlerInstance != null ? bcAttackHandlerInstance : null;
                Object result = bcIsAttackingMethod.invoke(self);
                return Boolean.TRUE.equals(result);
            }
            if (bcIsAttackingField != null) {
                Object self = bcAttackHandlerInstance;
                return bcIsAttackingField.getBoolean(self);
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to check BC attack-in-progress state: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Resets any transient state (e.g. keys held down by a failed attempt).
     */
    public static void reset(Minecraft mc) {
        try {
            if (mc != null && mc.options != null) {
                mc.options.keyAttack.setDown(false);
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to reset attack key: {}", e.getMessage());
        }
    }

    // --- Internals --------------------------------------------------------

    /**
     * Attempts to invoke BetterCombat's client attack starter reflectively.
     * BetterCombat's exact internal class names vary across versions, so we probe a few
     * known candidates. Discovery failures are non-fatal — we simply fall through.
     */
    private static boolean tryBetterCombatDirect() {
        try {
            resolveBetterCombat();
            if (!bcAvailable || bcStartAttackMethod == null) return false;
            bcStartAttackMethod.invoke(bcAttackHandlerInstance);
            return true;
        } catch (Throwable e) {
            LOGGER.debug("[xEB] BC direct attack invocation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Simulates a real attack-key press: {@link KeyMapping#click()} fires the Forge
     * {@code InputEvent.InteractionKeyMappingTriggered} / {@code ClickInputEvent} flow that
     * BetterCombat listens to, so BC will play its combo animation and apply the AoE hit.
     * We also hold the key down for one tick (the controller calls this each tick) and the
     * state machine releases it via its cadence gating.
     */
    private static boolean simulateAttackKeyPress(Minecraft mc) {
        try {
            KeyMapping attackKey = mc.options.keyAttack;
            // Ensure we're not stuck "down" from a previous tick, then click.
            attackKey.setDown(false);
            incrementClickCount(attackKey);
            attackKey.setDown(true);
            return true;
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to simulate attack key press: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Original vanilla path: reflective {@code Minecraft.startAttack()} with obfuscated-name
     * fallbacks, and if reflection fails, a direct server-mode attack + swing.
     */
    private static boolean triggerVanillaFallback(Minecraft mc, LocalPlayer player, LivingEntity target) {
        try {
            java.lang.reflect.Method method = findStartAttack(mc.getClass());
            if (method != null) {
                KeyMapping attackKey = mc.options.keyAttack;
                attackKey.setDown(true);
                incrementClickCount(attackKey);
                
                method.setAccessible(true);
                method.invoke(mc);
                
                attackKey.setDown(false);
                return true;
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Vanilla startAttack reflective call failed: {}", e.getMessage());
        }
        // Direct game-mode attack as the ultimate fallback.
        try {
            if (mc.gameMode != null && target != null) {
                KeyMapping attackKey = mc.options.keyAttack;
                attackKey.setDown(true);
                incrementClickCount(attackKey);
                
                mc.gameMode.attack(player, target);
                player.swing(InteractionHand.MAIN_HAND);
                
                attackKey.setDown(false);
                return true;
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Direct gameMode.attack fallback failed: {}", e.getMessage());
        }
        return false;
    }

    /** Searches the class hierarchy for vanilla {@code startAttack} (deobf or SRG names). */
    private static java.lang.reflect.Method findStartAttack(Class<?> clazz) {
        String[] names = {"startAttack", "m_91244_", "m_91243_"};
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                String n = m.getName();
                for (String name : names) {
                    if (n.equals(name) && m.getParameterCount() == 0) {
                        return m;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Lazily resolves BetterCombat's client attack API. We look for a handful of candidate
     * class/method names that have existed across BetterCombat 1.20.1 builds. This is best-effort:
     * if nothing matches, {@link #bcAvailable} stays false and callers degrade to key simulation.
     */
    private static void resolveBetterCombat() {
        if (bcResolved) return;
        synchronized (BetterCombatAttackController.class) {
            if (bcResolved) return;
            bcResolved = true;
            try {
                if (!net.minecraftforge.fml.ModList.get().isLoaded("bettercombat") &&
                    !net.minecraftforge.fml.ModList.get().isLoaded("better_combat")) {
                    return;
                }
            } catch (Throwable ignored) {
                return;
            }

            // Candidate client handler classes (historical names). We try each in order.
            String[] handlerClassCandidates = {
                    "net.bettercombat.client.BetterCombatClient",
                    "net.bettercombat.BetterCombat",
                    "net.bettercombat.client.MinecraftClientExtension",
                    "net.bettercombat.client.BetterCombatClientMod"
            };
            Class<?> handlerClass = null;
            for (String name : handlerClassCandidates) {
                try {
                    handlerClass = Class.forName(name);
                    break;
                } catch (Throwable ignored) {}
            }
            if (handlerClass == null) {
                // Still mark BC as "available" so the key-simulation path is preferred over
                // the startAttack() path (BetterCombat DOES hook the attack key even if we
                // can't find its handler class reflectively).
                bcAvailable = true;
                return;
            }

            // Try to find a static no-arg "begin/trigger attack" method.
            String[] startCandidates = {"startAttack", "attack", "beginAttack", "doAttack", "handleAttack"};
            for (String name : startCandidates) {
                try {
                    java.lang.reflect.Method m = handlerClass.getMethod(name);
                    if (m.getParameterCount() == 0) {
                        bcStartAttackMethod = m;
                        m.setAccessible(true);
                        break;
                    }
                } catch (Throwable ignored) {}
            }

            // Try to find an "is attacking / is in progress" query.
            String[] isAttackingMethods = {"isAttacking", "isInProgress", "isAttackInProgress", "isComboActive"};
            for (String name : isAttackingMethods) {
                try {
                    java.lang.reflect.Method m = handlerClass.getMethod(name);
                    if (m.getParameterCount() == 0 && (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)) {
                        bcIsAttackingMethod = m;
                        m.setAccessible(true);
                        break;
                    }
                } catch (Throwable ignored) {}
            }
            if (bcIsAttackingMethod == null) {
                String[] isAttackingFields = {"isAttacking", "isInProgress", "isAttackInProgress"};
                for (String name : isAttackingFields) {
                    try {
                        java.lang.reflect.Field f = handlerClass.getField(name);
                        if (f.getType() == boolean.class || f.getType() == Boolean.class) {
                            bcIsAttackingField = f;
                            break;
                        }
                    } catch (Throwable ignored) {}
                }
            }

            // If the resolved methods are non-static, capture an instance via a getInstance() / INSTANCE lookup.
            if (needsInstance(bcStartAttackMethod) || needsInstance(bcIsAttackingMethod) || bcIsAttackingField != null) {
                bcAttackHandlerInstance = resolveInstance(handlerClass);
            }

            bcAvailable = true;
        }
    }

    private static boolean needsInstance(java.lang.reflect.Method m) {
        return m != null && (m.getModifiers() & java.lang.reflect.Modifier.STATIC) == 0;
    }

    private static Object resolveInstance(Class<?> handlerClass) {
        // getInstance()
        for (String getter : new String[]{"getInstance"}) {
            try {
                java.lang.reflect.Method m = handlerClass.getMethod(getter);
                if (m.getParameterCount() == 0) {
                    Object obj = m.invoke(null);
                    if (obj != null) return obj;
                }
            } catch (Throwable ignored) {}
        }
        // INSTANCE field
        try {
            java.lang.reflect.Field f = handlerClass.getField("INSTANCE");
            return f.get(null);
        } catch (Throwable ignored) {}
        // Fall back to a fresh instance (handler classes with only static state are fine with null).
        try {
            return handlerClass.getDeclaredConstructor().newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void incrementClickCount(KeyMapping key) {
        try {
            java.lang.reflect.Field field = null;
            for (String name : new String[]{"clickCount", "f_90815_"}) {
                try {
                    field = KeyMapping.class.getDeclaredField(name);
                    break;
                } catch (Throwable ignored) {}
            }
            if (field != null) {
                field.setAccessible(true);
                int current = field.getInt(key);
                field.setInt(key, current + 1);
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to increment click count: {}", e.getMessage());
        }
    }
}
