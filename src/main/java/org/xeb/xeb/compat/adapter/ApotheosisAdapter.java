package org.xeb.xeb.compat.adapter;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.xeb.xeb.compat.ModCompatAdapter;
import org.xeb.xeb.weapon.WeaponClass;
import org.xeb.xeb.weapon.WeaponStyleData;

import java.util.Optional;

public class ApotheosisAdapter implements ModCompatAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "apotheosis";
    private final boolean loaded;

    public ApotheosisAdapter() {
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
     * Calculates score bonus based on the Apotheosis affix weapon rarity.
     */
    public double getRarityBonus(ItemStack stack) {
        if (!loaded || stack.isEmpty() || !stack.hasTag()) return 0.0;
        try {
            CompoundTag tag = stack.getTag();
            if (tag.contains("affixes") || tag.contains("rarity") || tag.contains("apoth_rpg")) {
                String rarity = tag.getString("rarity").toLowerCase();
                if (rarity.contains("mythic") || rarity.contains("ancient")) {
                    return 10.0;
                } else if (rarity.contains("epic")) {
                    return 8.0;
                } else if (rarity.contains("rare")) {
                    return 6.0;
                } else if (rarity.contains("uncommon")) {
                    return 4.0;
                } else if (rarity.contains("common")) {
                    return 2.0;
                }
                return 5.0; // Standard affix bonus
            }
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to read Apotheosis rarity from {}: {}", stack.getItem(), e.getMessage());
        }
        return 0.0;
    }
}
