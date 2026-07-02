package org.xeb.xeb.compat.adapter;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.xeb.xeb.compat.ModCompatAdapter;
import org.xeb.xeb.weapon.WeaponClass;
import org.xeb.xeb.weapon.WeaponStyleData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CuriosAdapter implements ModCompatAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "curios";
    private final boolean loaded;

    public CuriosAdapter() {
        boolean modLoaded = false;
        try { modLoaded = ModList.get() != null && ModList.get().isLoaded(MOD_ID); } catch (Exception | LinkageError ignored) {}
        this.loaded = modLoaded;
    }

    @Override
    public String modId() {
        return MOD_ID;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public WeaponClass classifyWeapon(ItemStack stack) {
        return WeaponClass.NON_WEAPON;
    }

    @Override
    public boolean isBoss(LivingEntity entity) {
        return false;
    }

    @Override
    public Optional<WeaponStyleData> getWeaponStyle(ItemStack stack) {
        return Optional.empty();
    }

    @Override
    public boolean isItemNonWeapon(ItemStack stack) {
        return false;
    }

    /**
     * Reflectively grabs all ItemStacks equipped in Curios slots for a LivingEntity.
     */
    public List<ItemStack> getCuriosItems(LivingEntity entity) {
        List<ItemStack> list = new ArrayList<>();
        if (!loaded || entity == null) return list;
        try {
            Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getHelper = apiClass.getMethod("getCuriosHelper");
            Object helper = getHelper.invoke(null);
            if (helper != null) {
                Method getCuriosHandler = helper.getClass().getMethod("getCuriosHandler", LivingEntity.class);
                Object lazyOpt = getCuriosHandler.invoke(helper, entity);
                if (lazyOpt != null) {
                    Method orElse = lazyOpt.getClass().getMethod("orElse", Object.class);
                    Object handlerObj = orElse.invoke(lazyOpt, (Object) null);
                    if (handlerObj != null) {
                        Method getCurios = handlerObj.getClass().getMethod("getCurios");
                        java.util.Map<String, Object> curiosMap = (java.util.Map<String, Object>) getCurios.invoke(handlerObj);
                        if (curiosMap != null) {
                            for (Object stacksHandler : curiosMap.values()) {
                                Method getStacks = stacksHandler.getClass().getMethod("getStacks");
                                Object stacksObj = getStacks.invoke(stacksHandler);
                                if (stacksObj != null) {
                                    Method getSlots = stacksObj.getClass().getMethod("getSlots");
                                    int slotsCount = (Integer) getSlots.invoke(stacksObj);
                                    Method getStackInSlot = stacksObj.getClass().getMethod("getStackInSlot", int.class);
                                    for (int i = 0; i < slotsCount; i++) {
                                        ItemStack itemStack = (ItemStack) getStackInSlot.invoke(stacksObj, i);
                                        if (itemStack != null && !itemStack.isEmpty()) {
                                            list.add(itemStack);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to get Curios items for entity {}: {}", entity.getId(), e.getMessage());
        }
        return list;
    }
}
