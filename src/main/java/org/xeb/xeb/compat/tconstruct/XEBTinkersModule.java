package org.xeb.xeb.compat.tconstruct;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraftforge.fml.ModList;
import org.xeb.xeb.Config;

/**
 * Entry point for Tinkers' Construct integration.
 * Only initializes if tconstruct is loaded and config allows it.
 */
public class XEBTinkersModule {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        if (!ModList.get().isLoaded("tconstruct")) return;
        if (!Config.tconstructIntegrationEnabled) return;

        initialized = true;
        LOGGER.info("xEB: Initializing Tinkers' Construct integration");
    }

    public static boolean isActive() {
        return initialized;
    }
}
