package org.xeb.xeb.damagenumber;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Modos de visualización de los marcadores de daño flotantes (Floating Damage Numbers).
 */
public enum DamageNumberMode {
    OFF("gui.xeb.damagenumber.mode.off", "gui.xeb.damagenumber.mode.off.desc", ChatFormatting.GRAY),
    STACK("gui.xeb.damagenumber.mode.stack", "gui.xeb.damagenumber.mode.stack.desc", ChatFormatting.AQUA),
    COMBINE("gui.xeb.damagenumber.mode.combine", "gui.xeb.damagenumber.mode.combine.desc", ChatFormatting.YELLOW),
    HYBRID("gui.xeb.damagenumber.mode.hybrid", "gui.xeb.damagenumber.mode.hybrid.desc", ChatFormatting.LIGHT_PURPLE);

    private final String nameKey;
    private final String descKey;
    private final ChatFormatting color;

    DamageNumberMode(String nameKey, String descKey, ChatFormatting color) {
        this.nameKey = nameKey;
        this.descKey = descKey;
        this.color = color;
    }

    public String getDisplayName() {
        return Component.translatable(nameKey).getString();
    }

    public String getDescription() {
        return Component.translatable(descKey).getString();
    }

    public ChatFormatting getColor() {
        return color;
    }

    public DamageNumberMode next() {
        DamageNumberMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
