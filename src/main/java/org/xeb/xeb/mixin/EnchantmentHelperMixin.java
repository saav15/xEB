package org.xeb.xeb.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xeb.xeb.compat.CuriosQueryContext;
import org.xeb.xeb.item.TinfoilHatItem;

/**
 * Intercepts EnchantmentHelper.getItemEnchantmentLevel so that TinfoilHat
 * reports 0 enchantment level when queried by Curios (preventing Curios from
 * applying unintended enchantment bonuses to the hat).
 *
 * <p><b>Performance fix (N1):</b> The original used {@link Thread#getStackTrace()}
 * which is O(call-stack-depth) and was being triggered hundreds of times per tick.
 * Replaced with a {@link ThreadLocal} boolean flag set by
 * {@link org.xeb.xeb.compat.adapter.CuriosAdapter} before Curios calls
 * {@code getItemEnchantmentLevel}, making the check O(1) with no stack allocation.</p>
 *
 * <p>The flag lives in {@link CuriosQueryContext} (not in this class) because
 * Mixin does not permit non-@Shadow / non-@Unique fields on @Mixin classes.</p>
 *
 * <p>If Curios is not loaded the flag is never set and this mixin is a no-op.</p>
 */
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "getItemEnchantmentLevel", at = @At("HEAD"), cancellable = true)
    private static void onGetItemEnchantmentLevel(Enchantment enchantment, ItemStack stack,
                                                   CallbackInfoReturnable<Integer> cir) {
        if (!stack.isEmpty()
                && stack.getItem() instanceof TinfoilHatItem
                && Boolean.TRUE.equals(CuriosQueryContext.INSIDE_CURIOS_QUERY.get())) {
            cir.setReturnValue(0);
        }
    }
}
