package org.xeb.xeb.mana;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ArsNouveauManaCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean checked = false;
    private static Capability<?> manaCapability = null;
    private static Method getManaMethod = null;
    private static Method setManaMethod = null;
    private static Method removeManaMethod = null;
    private static Method maxManaMethod = null;

    private static void init() {
        if (checked) return;
        checked = true;
        if (ModList.get().isLoaded("ars_nouveau")) {
            try {
                Class<?> registryClass = Class.forName("com.hollingsworth.arsnouveau.common.capability.CapabilityRegistry");
                Field field = registryClass.getField("MANA_CAPABILITY");
                manaCapability = (Capability<?>) field.get(null);

                Class<?> manaCapClass = Class.forName("com.hollingsworth.arsnouveau.api.mana.IManaCap");
                getManaMethod = manaCapClass.getMethod("getMana");
                setManaMethod = manaCapClass.getMethod("setMana", int.class);
                removeManaMethod = manaCapClass.getMethod("removeMana", int.class);
                maxManaMethod = manaCapClass.getMethod("getMaxMana");
            } catch (Exception e) {
                LOGGER.warn("[xEB] Ars Nouveau mana API not found or incompatible version: {}", e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> LazyOptional<T> getCapability(LivingEntity entity) {
        init();
        if (manaCapability != null) {
            return entity.getCapability((Capability<T>) manaCapability);
        }
        return LazyOptional.empty();
    }

    public static int getMana(LivingEntity entity) {
        LazyOptional<?> cap = getCapability(entity);
        if (cap.isPresent()) {
            try {
                Object manaObj = cap.orElse(null);
                if (manaObj != null) {
                    return ((Number) getManaMethod.invoke(manaObj)).intValue();
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to get Ars Nouveau mana: {}", e.getMessage());
            }
        }
        return 0;
    }

    public static int getMaxMana(LivingEntity entity) {
        LazyOptional<?> cap = getCapability(entity);
        if (cap.isPresent()) {
            try {
                Object manaObj = cap.orElse(null);
                if (manaObj != null) {
                    return ((Number) maxManaMethod.invoke(manaObj)).intValue();
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to get Ars Nouveau max mana: {}", e.getMessage());
            }
        }
        return 0;
    }

    public static boolean setMana(LivingEntity entity, int mana) {
        LazyOptional<?> cap = getCapability(entity);
        if (cap.isPresent()) {
            try {
                Object manaObj = cap.orElse(null);
                if (manaObj != null) {
                    setManaMethod.invoke(manaObj, mana);
                    return true;
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to set Ars Nouveau mana: {}", e.getMessage());
            }
        }
        return false;
    }

    public static boolean drainMana(LivingEntity entity, int amount) {
        LazyOptional<?> cap = getCapability(entity);
        if (cap.isPresent()) {
            try {
                Object manaObj = cap.orElse(null);
                if (manaObj != null) {
                    int current = ((Number) getManaMethod.invoke(manaObj)).intValue();
                    if (current >= amount) {
                        removeManaMethod.invoke(manaObj, amount);
                        return true;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to drain Ars Nouveau mana: {}", e.getMessage());
            }
        }
        return false;
    }
}
