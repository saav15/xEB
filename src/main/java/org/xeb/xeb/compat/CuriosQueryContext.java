package org.xeb.xeb.compat;

/**
 * Shared flag used to coordinate between {@link org.xeb.xeb.mixin.EnchantmentHelperMixin}
 * and {@link org.xeb.xeb.compat.adapter.CuriosAdapter}.
 *
 * <p><b>N1 fix:</b> Extracted from the mixin class because Mixin does not allow
 * non-@Shadow / non-@Unique fields on @Mixin classes.  A plain helper class is the
 * correct place for shared state between a mixin and the code that drives it.</p>
 */
public final class CuriosQueryContext {

    private CuriosQueryContext() {}

    /**
     * Set to {@code true} by {@link org.xeb.xeb.compat.adapter.CuriosAdapter}
     * while Curios is evaluating enchantment levels.
     * {@link org.xeb.xeb.mixin.EnchantmentHelperMixin} reads this flag in O(1)
     * instead of scanning the call stack.
     */
    public static final ThreadLocal<Boolean> INSIDE_CURIOS_QUERY =
            ThreadLocal.withInitial(() -> false);
}
