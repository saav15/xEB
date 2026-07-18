package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;

/**
 * Omega Flowery — Extreme Burst curio for Golden Flower (Limited / Instance).
 *
 * <p>When equipped in the extreme_burst Curios slot and the player holds
 * Golden Flower, pressing Activa 3 (N) enters the Omega Flowery instance:
 * 20 seconds of zero-gravity transformation with doubled Jarona damage,
 * 20% HP Flower Dance heal, and 50% faster cooldowns.</p>
 */
public class OmegaFloweryItem extends Item {

    public OmegaFloweryItem(Properties properties) {
        super(properties);
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
