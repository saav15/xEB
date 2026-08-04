package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Judgement Cut End — Universal Extreme Burst curio.
 *
 * <p>When equipped in the extreme_burst Curios slot, pressing Activa 3 (N)
 * projects a 0.75s 3D Telegraph Warning Zone before freezing time in a 24m domain,
 * locking all entities inside behind an impenetrable 3D forcefield dome,
 * and executing 12 dimensional slashes per entity scaling with 2.0x mainhand weapon damage.</p>
 */
public class JudgementCutItem extends Item {

    public JudgementCutItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
