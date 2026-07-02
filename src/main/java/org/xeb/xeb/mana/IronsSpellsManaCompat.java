package org.xeb.xeb.mana;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Method;

public class IronsSpellsManaCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean checked = false;
    private static Method getPlayerMagicDataMethod = null;
    private static Method getManaMethod = null;
    private static Method setManaMethod = null;

    private static void init() {
        if (checked) return;
        checked = true;
        if (ModList.get().isLoaded("irons_spellbooks")) {
            try {
                Class<?> magicDataClass = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                getPlayerMagicDataMethod = magicDataClass.getMethod("getPlayerMagicData", Player.class);
                getManaMethod = magicDataClass.getMethod("getMana");
                setManaMethod = magicDataClass.getMethod("setMana", float.class);
            } catch (Exception e) {
                LOGGER.warn("[xEB] Iron's Spells mana API not found or incompatible version: {}", e.getMessage());
            }
        }
    }

    public static float getMana(Player player) {
        init();
        if (getPlayerMagicDataMethod != null) {
            try {
                Object magicData = getPlayerMagicDataMethod.invoke(null, player);
                if (magicData != null) {
                    return ((Number) getManaMethod.invoke(magicData)).floatValue();
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to get Iron's Spells mana: {}", e.getMessage());
            }
        }
        return 0f;
    }

    public static boolean setMana(Player player, float mana) {
        init();
        if (getPlayerMagicDataMethod != null) {
            try {
                Object magicData = getPlayerMagicDataMethod.invoke(null, player);
                if (magicData != null) {
                    setManaMethod.invoke(magicData, mana);
                    return true;
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to set Iron's Spells mana: {}", e.getMessage());
            }
        }
        return false;
    }

    public static boolean drainMana(Player player, float amount) {
        init();
        if (getPlayerMagicDataMethod != null) {
            try {
                Object magicData = getPlayerMagicDataMethod.invoke(null, player);
                if (magicData != null) {
                    float current = ((Number) getManaMethod.invoke(magicData)).floatValue();
                    if (current >= amount) {
                        setManaMethod.invoke(magicData, current - amount);
                        return true;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to drain Iron's Spells mana: {}", e.getMessage());
            }
        }
        return false;
    }
}
