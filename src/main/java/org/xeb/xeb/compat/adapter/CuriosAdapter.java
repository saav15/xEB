package org.xeb.xeb.compat.adapter;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.xeb.xeb.compat.ModCompatAdapter;
import org.xeb.xeb.mixin.EnchantmentHelperMixin;
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
 * <p>Also wraps the call site with {@link EnchantmentHelperMixin#INSIDE_CURIOS_QUERY}
 * so the TinfoilHat mixin can detect Curios queries in O(1) instead of
 * scanning the call stack.</p>
 */
public class CuriosAdapter implements ModCompatAdapter {
    private static final String MOD_ID = "curios";
    private final boolean loaded;

    // ── Cached reflection handles resolved once at startup ────────────────────
    private final Method getCuriosHelper;
    private final Method getCuriosHandler;
    private final Method orElse;
    private final Method getCurios;
    private final Method getStacks;
    private final Method getSlots;
    private final Method getStackInSlot;
    private final Object helperInstance; // CuriosApi.getCuriosHelper() result (static, stable)

    public CuriosAdapter() {
        boolean modLoaded = false;
        try { modLoaded = ModList.get() != null && ModList.get().isLoaded(MOD_ID); }
        catch (Exception | LinkageError ignored) {}
        this.loaded = modLoaded;

        // Resolve all reflection handles now — fail fast at startup rather than per call
        Method mGetCuriosHelper = null, mGetCuriosHandler = null, mOrElse = null;
        Method mGetCurios = null, mGetStacks = null, mGetSlots = null, mGetStackInSlot = null;
        Object helperObj = null;

        if (modLoaded) {
            try {
                Class<?> apiClass     = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                mGetCuriosHelper      = apiClass.getMethod("getCuriosHelper");
                helperObj             = mGetCuriosHelper.invoke(null);

                if (helperObj != null) {
                    mGetCuriosHandler = helperObj.getClass().getMethod("getCuriosHandler", LivingEntity.class);

                    // orElse lives on Optional<ICuriosItemHandler>
                    mOrElse           = Optional.class.getMethod("orElse", Object.class);

                    // Resolve handler/stacks methods via a dummy invocation path once
                    // (we resolve the class names directly to avoid needing a live entity)
                    Class<?> handlerClass  = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
                    mGetCurios             = handlerClass.getMethod("getCurios");

                    Class<?> stacksClass   = Class.forName("top.theillusivec4.curios.api.type.capability.ICurioStacksHandler");
                    mGetStacks             = stacksClass.getMethod("getStacks");

                    Class<?> iItemHandlerClass = Class.forName("net.minecraftforge.items.IItemHandlerModifiable");
                    mGetSlots              = iItemHandlerClass.getMethod("getSlots");
                    mGetStackInSlot        = iItemHandlerClass.getMethod("getStackInSlot", int.class);
                }
            } catch (Exception | LinkageError ignored) {
                // Curios present but API changed — degrade gracefully
                helperObj = null;
            }
        }

        this.getCuriosHelper  = mGetCuriosHelper;
        this.getCuriosHandler = mGetCuriosHandler;
        this.orElse           = mOrElse;
        this.getCurios        = mGetCurios;
        this.getStacks        = mGetStacks;
        this.getSlots         = mGetSlots;
        this.getStackInSlot   = mGetStackInSlot;
        this.helperInstance   = helperObj;
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
     * Sets {@link EnchantmentHelperMixin#INSIDE_CURIOS_QUERY} so the TinfoilHat mixin
     * can detect this context in O(1).
     */
    @SuppressWarnings("unchecked")
    public List<ItemStack> getCuriosItems(LivingEntity entity) {
        List<ItemStack> list = new ArrayList<>();
        if (!loaded || entity == null || helperInstance == null
                || getCuriosHandler == null || getCurios == null) {
            return list;
        }
        EnchantmentHelperMixin.INSIDE_CURIOS_QUERY.set(true);
        try {
            Optional<?> lazyOpt = (Optional<?>) getCuriosHandler.invoke(helperInstance, entity);
            if (lazyOpt == null || !lazyOpt.isPresent()) return list;
            Object handlerObj = lazyOpt.get();
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
        } catch (Exception ignored) {
        } finally {
            EnchantmentHelperMixin.INSIDE_CURIOS_QUERY.set(false);
        }
        return list;
    }
}
