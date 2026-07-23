package org.xeb.xeb.client;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    public static final String KEY_CATEGORY_XEB = "key.categories.xeb";

    public static final KeyMapping ACTIVA_1_KEY = new KeyMapping(
            "key.xeb.activa_1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEY_CATEGORY_XEB
    );

    public static final KeyMapping ACTIVA_2_KEY = new KeyMapping(
            "key.xeb.activa_2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KEY_CATEGORY_XEB
    );

    public static final KeyMapping ACTIVA_3_KEY = new KeyMapping(
            "key.xeb.activa_3",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            KEY_CATEGORY_XEB
    );

    public static final KeyMapping FLOURISH_KEY = new KeyMapping(
            "key.xeb.flourish",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KEY_CATEGORY_XEB
    );

    public static final KeyMapping ENIGMA_KEY = new KeyMapping(
            "key.xeb.enigma",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            KEY_CATEGORY_XEB
    );
}
