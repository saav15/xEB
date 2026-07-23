package org.xeb.xeb.item;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.xeb.xeb.compat.ModCompatManager;

import java.util.List;

public class EnigmaBiosItem extends Item {

    public EnigmaBiosItem(Properties properties) {
        super(properties);
    }

    public static boolean hasEnigmaBiosEquippedOrInInventory(Player player) {
        if (player == null) return false;

        if (player.getInventory().contains(new ItemStack(ModItems.ENIGMA_BIOS.get())) ||
                player.getOffhandItem().is(ModItems.ENIGMA_BIOS.get())) {
            return true;
        }

        for (ItemStack curioStack : ModCompatManager.getCuriosItems(player)) {
            if (curioStack.is(ModItems.ENIGMA_BIOS.get())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            org.xeb.xeb.event.EnigmaBiosHandler.syncBitacoras(serverPlayer, -1);
        }
        if (level.isClientSide()) {
            org.xeb.xeb.client.ClientAccess.openEnigmaBiosScreen();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
