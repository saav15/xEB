package org.xeb.xeb.compat.adapter;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.xeb.xeb.compat.ModCompatAdapter;
import org.xeb.xeb.weapon.WeaponClass;
import org.xeb.xeb.weapon.WeaponStyleData;

import java.util.Optional;

public class CataclysmAdapter implements ModCompatAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "cataclysm";
    private final boolean loaded;

    public CataclysmAdapter() {
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
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl != null && rl.getNamespace().equals(MOD_ID)) {
            String path = rl.getPath().toLowerCase();
            if (path.contains("incinerator") || path.contains("greatsword") || 
                path.contains("blade") || path.contains("cleaver") || path.contains("spear")) {
                return WeaponClass.MELEE;
            }
        }
        return WeaponClass.NON_WEAPON;
    }

    @Override
    public boolean isBoss(LivingEntity entity) {
        if (!loaded || entity == null) return false;
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (rl != null && rl.getNamespace().equals(MOD_ID)) {
            String path = rl.getPath().toLowerCase();
            return path.equals("ignis") || path.equals("ender_guardian") || 
                   path.equals("netherite_monstrosity") || path.equals("the_harbinger") || 
                   path.equals("leviathan") || path.equals("ancient_remnant");
        }
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
     * Checks if a Cataclysm boss is currently undergoing a phase transition.
     */
    public boolean isTransitioning(LivingEntity entity) {
        if (!loaded || entity == null) return false;
        String name = entity.getClass().getName();
        if (name.contains("cataclysm")) {
            try {
                // Heuristics for Cataclysm animation states / invulnerability
                if (entity.isInvulnerable()) {
                    return true;
                }
                // Reflective field check for commonly used transition markers
                for (java.lang.reflect.Field field : entity.getClass().getDeclaredFields()) {
                    String fieldName = field.getName().toLowerCase();
                    if (fieldName.contains("transition") || fieldName.contains("phase") || fieldName.contains("state")) {
                        field.setAccessible(true);
                        Object val = field.get(entity);
                        if (val instanceof Number && ((Number) val).intValue() > 0) {
                            // Non-zero state/phase could represent active animation transition
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to check Cataclysm transition state: {}", e.getMessage());
            }
        }
        return false;
    }
}
