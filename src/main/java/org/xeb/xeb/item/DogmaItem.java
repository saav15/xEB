package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Dogma — Extreme Burst curio for The Tears (Limited / Instant).
 *
 * <p>When equipped in the extreme_burst Curios slot and the player holds
 * The Tears, pressing Activa 3 (N) fires a growing Dogma brimstone beam
 * that deals 9.99 damage/tick over 10 seconds and auto-wins beam struggles.</p>
 */
public class DogmaItem extends Item {

    public DogmaItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.dogma.desc1"));
        tooltip.add(Component.translatable("item.xeb.dogma.desc2", Component.keybind("key.xeb.activa_3")));
        tooltip.add(Component.translatable("item.xeb.dogma.desc3"));
        tooltip.add(Component.translatable("item.xeb.dogma.requires",
                Component.translatable("item.xeb.the_tears")));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
