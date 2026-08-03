package org.xeb.xeb.compat.tconstruct;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import slimeknights.tconstruct.library.modifiers.Modifier;

public class XebModifier extends Modifier {
    private final int color;

    public XebModifier(int color) {
        super();
        this.color = color;
    }

    @Override
    public Component getDisplayName(int level) {
        Component name = super.getDisplayName(level);
        return name.copy().withStyle(Style.EMPTY.withColor(this.color));
    }
}
