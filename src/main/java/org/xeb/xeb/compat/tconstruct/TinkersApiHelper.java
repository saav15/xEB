package org.xeb.xeb.compat.tconstruct;

import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

public class TinkersApiHelper {

    public static int getModifierLevel(ItemStack stack, String buffId) {
        try {
            if (stack == null || stack.isEmpty() || !stack.hasTag()) return 0;
            if (!stack.getTag().contains("tic_modifiers") && !stack.getTag().contains("tic_upgrades")) return 0;

            IToolStackView tool = ToolStack.from(stack);
            ModifierId modId = new ModifierId("xeb", buffId);
            int lvl = tool.getModifierLevel(modId);
            if (lvl > 0) return lvl;

            ModifierId tconModId = new ModifierId("tconstruct", buffId);
            return tool.getModifierLevel(tconModId);
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
