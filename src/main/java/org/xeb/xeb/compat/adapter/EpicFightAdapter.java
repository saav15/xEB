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
import java.util.Optional;

public class EpicFightAdapter implements ModCompatAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "epicfight";
    private final boolean loaded;

    public EpicFightAdapter() {
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
        if (!loaded || stack.isEmpty()) return WeaponClass.NON_WEAPON;
        try {
            // Reflectively fetch Epic Fight's item capability
            Class<?> helperClass = Class.forName("yesman.epicfight.gamecontrol.CapabilityHelpers");
            Method getCap = helperClass.getMethod("getItemCapability", ItemStack.class);
            Object cap = getCap.invoke(null, stack);
            if (cap != null) {
                String capName = cap.getClass().getSimpleName().toLowerCase();
                if (capName.contains("weapon") || capName.contains("melee") || 
                    capName.contains("sword") || capName.contains("axe") || capName.contains("spear")) {
                    return WeaponClass.MELEE;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to classify weapon via Epic Fight: {}", e.getMessage());
        }
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
}
