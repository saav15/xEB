package org.xeb.xeb.compat.adapter;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.util.LazyOptional;
import org.xeb.xeb.compat.ModCompatAdapter;
import org.xeb.xeb.compat.CuriosQueryContext;
import org.xeb.xeb.weapon.WeaponClass;
import org.xeb.xeb.weapon.WeaponStyleData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Curios integration adapter.
 *
 * <p><b>Performance fix (N3):</b> The original called {@code Class.forName} +
 * {@code getMethod} on every {@link #getCuriosItems} invocation, producing
 * ~8 reflection ops per call and up to 2000 chains/s under load.
 * All reflective lookups are now resolved once at construction time and
 * cached as final fields.  Hot-path calls use the cached {@link Method}
 * references directly, reducing reflection overhead to a single
 * {@code Method.invoke} per slot.</p>
 *
 * <p><b>LazyOptional fix:</b> In Curios 1.20.1, {@code getCuriosHandler()}
 * returns {@code LazyOptional<ICuriosItemHandler>} (a Forge wrapper), NOT
 * {@code Optional<?>}. We cache {@code LazyOptional#resolve()} which unwraps
 * it to {@code Optional<T>} that we can then call {@code .get()} on.</p>
 *
 * <p>Also wraps the call site with {@link CuriosQueryContext#INSIDE_CURIOS_QUERY}
 * so the TinfoilHat mixin can detect Curios queries in O(1) instead of
 * scanning the call stack.</p>
 */
public class CuriosAdapter implements ModCompatAdapter {
    private static final String MOD_ID = "curios";
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private final boolean loaded;

    // ── Cached reflection handles resolved once at startup ────────────────────
    // ── Cached reflection handles resolved once at startup ────────────────────
    private Method getCuriosHelper;
    private Method getCuriosHandler;
    /** LazyOptional#resolve() → returns Optional<T> */
    private Method resolve;
    private Method getCurios;
    private Method getStacks;
    private Method getSlots;
    private Method getStackInSlot;
    private Method getRenders;
    private Object helperInstance; // CuriosApi.getCuriosHelper() result (static, stable)
    private boolean initialized = false;

    public CuriosAdapter() {
        boolean modLoaded = false;
        try { modLoaded = ModList.get() != null && ModList.get().isLoaded(MOD_ID); }
        catch (Exception | LinkageError ignored) {}
        this.loaded = modLoaded;
    }

    private synchronized void checkInit() {
        if (initialized) return;
        if (!loaded) {
            initialized = true;
            return;
        }

        try {
            Class<?> apiClass     = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            // getCuriosInventory(LivingEntity) is a static method directly on CuriosApi in newer Curios versions
            this.getCuriosHandler = apiClass.getMethod("getCuriosInventory", LivingEntity.class);
            this.helperInstance   = null;

            // In Curios 1.20.1, getCuriosInventory() returns LazyOptional<ICuriosItemHandler>.
            // LazyOptional.resolve() returns Optional<T>, which has .isPresent() / .get().
            this.resolve          = LazyOptional.class.getMethod("resolve");

            // Resolve handler/stacks methods via a dummy invocation path once
            // (we resolve the class names directly to avoid needing a live entity)
            Class<?> handlerClass;
            try {
                handlerClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            } catch (ClassNotFoundException e) {
                handlerClass = Class.forName("top.theillusivec4.curios.api.type.inventory.ICuriosItemHandler");
            }
            this.getCurios             = handlerClass.getMethod("getCurios");

            Class<?> stacksClass;
            try {
                stacksClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICurioStacksHandler");
            } catch (ClassNotFoundException e) {
                stacksClass = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
            }
            this.getStacks             = stacksClass.getMethod("getStacks");
            try {
                this.getRenders        = stacksClass.getMethod("getRenders");
            } catch (NoSuchMethodException e) {
                try {
                    this.getRenders    = stacksClass.getMethod("isVisible");
                } catch (NoSuchMethodException e2) {
                    this.getRenders    = null;
                }
            }

            Class<?> iItemHandlerClass = Class.forName("net.minecraftforge.items.IItemHandler");
            this.getSlots              = iItemHandlerClass.getMethod("getSlots");
            this.getStackInSlot        = iItemHandlerClass.getMethod("getStackInSlot", int.class);

            this.initialized = true;
            LOGGER.info("xEB: CuriosAdapter lazy initialization successful via CuriosApi.getCuriosInventory.");
        } catch (Exception | LinkageError e) {
            LOGGER.error("xEB: Curios reflection lazy init failed!", e);
            this.initialized = true; // prevent infinite retries on hard classloading errors
        }
    }

    @Override public String modId()    { return MOD_ID; }
    @Override public boolean isLoaded(){ return loaded; }
    @Override public WeaponClass classifyWeapon(ItemStack stack) { return WeaponClass.NON_WEAPON; }
    @Override public boolean isBoss(LivingEntity entity)         { return false; }
    @Override public Optional<WeaponStyleData> getWeaponStyle(ItemStack stack) { return Optional.empty(); }
    @Override public boolean isItemNonWeapon(ItemStack stack)    { return false; }

    /**
     * Returns all ItemStacks equipped in Curios slots.
     * Uses pre-cached {@link Method} handles — no {@code Class.forName} on the hot path.
     * Sets {@link CuriosQueryContext#INSIDE_CURIOS_QUERY} so the TinfoilHat mixin
     * can detect this context in O(1).
     *
     * <p><b>LazyOptional fix:</b> {@code getCuriosHandler()} returns
     * {@code LazyOptional<ICuriosItemHandler>} in 1.20.1, NOT {@code Optional<?>}.
     * We call {@code resolve()} on the LazyOptional to get the inner {@code Optional<T>}.</p>
     */
    @SuppressWarnings("unchecked")
    public List<ItemStack> getCuriosItems(LivingEntity entity) {
        checkInit();
        List<ItemStack> list = new ArrayList<>();
        if (!loaded || entity == null
                || getCuriosHandler == null || getCurios == null || resolve == null) {
            return list;
        }
        CuriosQueryContext.INSIDE_CURIOS_QUERY.set(true);
        try {
            // getCuriosInventory returns LazyOptional<ICuriosItemHandler>
            Object lazyOpt = getCuriosHandler.invoke(null, entity);
            if (lazyOpt == null) return list;

            // LazyOptional.resolve() returns Optional<T>
            Optional<?> resolved = (Optional<?>) resolve.invoke(lazyOpt);
            if (resolved == null || !resolved.isPresent()) return list;

            Object handlerObj = resolved.get();
            if (handlerObj == null) return list;

            java.util.Map<String, Object> curiosMap =
                    (java.util.Map<String, Object>) getCurios.invoke(handlerObj);
            if (curiosMap == null) return list;

            for (Object stacksHandler : curiosMap.values()) {
                Object stacksObj = getStacks.invoke(stacksHandler);
                if (stacksObj == null) continue;
                int slots = (Integer) getSlots.invoke(stacksObj);
                for (int i = 0; i < slots; i++) {
                    ItemStack item = (ItemStack) getStackInSlot.invoke(stacksObj, i);
                    if (item != null && !item.isEmpty()) list.add(item);
                }
            }
        } catch (Exception e) {
            LOGGER.error("xEB: Curios getCuriosItems failed!", e);
        } finally {
            CuriosQueryContext.INSIDE_CURIOS_QUERY.set(false);
        }
        return list;
    }

    /**
     * Checks if the equipped slot containing the given Curio item has cosmetic visibility enabled.
     * Respects the toggle visibility button next to the Curios slots.
     */
    @SuppressWarnings("unchecked")
    public boolean isCurioVisible(LivingEntity entity, net.minecraft.world.item.Item item) {
        checkInit();
        if (!loaded || entity == null
                || getCuriosHandler == null || getCurios == null || resolve == null) {
            return false;
        }
        CuriosQueryContext.INSIDE_CURIOS_QUERY.set(true);
        try {
            Object lazyOpt = getCuriosHandler.invoke(null, entity);
            if (lazyOpt == null) return false;

            Optional<?> resolved = (Optional<?>) resolve.invoke(lazyOpt);
            if (resolved == null || !resolved.isPresent()) return false;

            Object handlerObj = resolved.get();
            if (handlerObj == null) return false;

            java.util.Map<String, Object> curiosMap =
                    (java.util.Map<String, Object>) getCurios.invoke(handlerObj);
            if (curiosMap == null) return false;

            for (Object stacksHandler : curiosMap.values()) {
                Object stacksObj = getStacks.invoke(stacksHandler);
                if (stacksObj == null) continue;
                int slots = (Integer) getSlots.invoke(stacksObj);
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) getStackInSlot.invoke(stacksObj, i);
                    if (stack != null && stack.is(item)) {
                        // Found the curio! Query getRenders() on ICurioStacksHandler
                        if (this.getRenders != null) {
                            try {
                                return (Boolean) this.getRenders.invoke(stacksHandler);
                            } catch (Exception e) {
                                return true; // Default to visible if reflection fails
                            }
                        }
                        return true; // Default to visible if method doesn't exist
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("xEB: Curios isCurioVisible failed!", e);
        } finally {
            CuriosQueryContext.INSIDE_CURIOS_QUERY.set(false);
        }
        return false;
    }

    public String getDiagnosticInfo(LivingEntity entity) {
        checkInit();
        StringBuilder sb = new StringBuilder();
        try {
            Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            sb.append("--- CuriosApi Methods ---\n");
            for (Method m : apiClass.getMethods()) {
                sb.append(m.getReturnType().getSimpleName()).append(" ").append(m.getName()).append("(");
                Class<?>[] params = m.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    sb.append(params[i].getSimpleName());
                    if (i < params.length - 1) sb.append(", ");
                }
                sb.append(")\n");
            }
        } catch (Exception e) {
            sb.append("Failed to list CuriosApi methods: ").append(e.toString()).append("\n");
        }

        try {
            Class<?> helperClass = Class.forName("top.theillusivec4.curios.api.ICuriosHelper");
            sb.append("--- ICuriosHelper Methods ---\n");
            for (Method m : helperClass.getMethods()) {
                sb.append(m.getReturnType().getSimpleName()).append(" ").append(m.getName()).append("(");
                Class<?>[] params = m.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    sb.append(params[i].getSimpleName());
                    if (i < params.length - 1) sb.append(", ");
                }
                sb.append(")\n");
            }
        } catch (Exception e) {
            sb.append("Failed to list ICuriosHelper methods: ").append(e.toString()).append("\n");
        }

        sb.append("loaded: ").append(loaded).append("\n");
        sb.append("helperInstance: ").append(helperInstance != null).append("\n");
        sb.append("getCuriosHelper: ").append(getCuriosHelper != null).append("\n");
        sb.append("getCuriosHandler: ").append(getCuriosHandler != null).append("\n");
        sb.append("resolve: ").append(resolve != null).append("\n");
        sb.append("getCurios: ").append(getCurios != null).append("\n");
        sb.append("getStacks: ").append(getStacks != null).append("\n");
        sb.append("getSlots: ").append(getSlots != null).append("\n");
        sb.append("getStackInSlot: ").append(getStackInSlot != null).append("\n");
        if (entity != null && loaded) {
            try {
                if (getCuriosHandler == null) {
                    sb.append("ERROR: getCuriosHandler is null\n");
                    return sb.toString();
                }
                Object lazyOpt = getCuriosHandler.invoke(null, entity);
                sb.append("lazyOpt: ").append(lazyOpt != null).append("\n");
                if (lazyOpt != null) {
                    sb.append("lazyOpt class: ").append(lazyOpt.getClass().getName()).append("\n");
                    if (resolve != null) {
                        Optional<?> resolved = (Optional<?>) resolve.invoke(lazyOpt);
                        sb.append("resolved: ").append(resolved != null).append("\n");
                        if (resolved != null) {
                            sb.append("isPresent: ").append(resolved.isPresent()).append("\n");
                            if (resolved.isPresent()) {
                                Object handlerObj = resolved.get();
                                sb.append("handlerObj class: ").append(handlerObj.getClass().getName()).append("\n");
                                if (getCurios != null) {
                                    java.util.Map<String, Object> curiosMap = (java.util.Map<String, Object>) getCurios.invoke(handlerObj);
                                    sb.append("curiosMap: ").append(curiosMap != null).append("\n");
                                    if (curiosMap != null) {
                                        sb.append("curiosMap size: ").append(curiosMap.size()).append("\n");
                                        sb.append("curiosMap keys: ").append(curiosMap.keySet()).append("\n");
                                        for (java.util.Map.Entry<String, Object> entry : curiosMap.entrySet()) {
                                            Object stacksHandler = entry.getValue();
                                            if (getStacks != null) {
                                                Object stacksObj = getStacks.invoke(stacksHandler);
                                                if (stacksObj != null) {
                                                    sb.append("stacksObj class: ").append(stacksObj.getClass().getName()).append("\n");
                                                    int slots = (Integer) getSlots.invoke(stacksObj);
                                                    sb.append("  ").append(entry.getKey()).append(" slots: ").append(slots).append("\n");
                                                    for (int i = 0; i < slots; i++) {
                                                        ItemStack item = (ItemStack) getStackInSlot.invoke(stacksObj, i);
                                                        sb.append("    [").append(i).append("]: ").append(item != null ? item.toString() : "null").append("\n");
                                                    }
                                                } else {
                                                    sb.append("  ").append(entry.getKey()).append(" stacksObj is null\n");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                sb.append("EXCEPTION: ").append(e.toString()).append("\n");
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                sb.append(sw.toString());
            }
        }
        return sb.toString();
    }
}
