package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Meteor Strike — Extreme Burst curio for Doomfist v1 and Doomfist v2 (Limited / Instant with targeting phase).
 *
 * <p>When equipped in the extreme_burst Curios slot and the player holds Doomfist v1 or v2,
 * pressing Activa 3 (N) launches the player skyward, opens a 10-second targeting overhead view,
 * and crashes down dealing 4x Doomfist charged damage at the epicenter (4x4) plus an expanding radial wave.</p>
 */
public class MeteorStrikeItem extends Item {

    public MeteorStrikeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
