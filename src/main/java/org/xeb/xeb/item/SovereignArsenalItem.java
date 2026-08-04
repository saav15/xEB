package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sovereign Arsenal — Universal Extreme Burst curio.
 *
 * <p>When equipped in the extreme_burst Curios slot, pressing Activa 3 (N)
 * opens 24 Golden Spatial Portals behind the player, projecting spectral copies
 * of the player's held weapon at 2/3 weapon damage (or random Minecraft swords if barehanded).
 * The weapons stick into floor/targets before detonating in a golden spatial burst.</p>
 */
public class SovereignArsenalItem extends Item {

    public SovereignArsenalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
