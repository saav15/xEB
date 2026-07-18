package org.xeb.xeb.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class QuantumCatBarrageItem extends Item {
    public QuantumCatBarrageItem(Properties properties) {
        super(properties);
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
        return net.minecraft.world.item.Rarity.RARE;
    }

    public static boolean hasUltimateCurio(LivingEntity entity) {
        for (ItemStack stack : org.xeb.xeb.compat.ModCompatManager.getCuriosItems(entity)) {
            if (stack.is(ModItems.QUANTUM_CAT_BARRAGE.get())) {
                return true;
            }
        }
        return false;
    }

    public static void recordDamageDealt(Player player, float amount) {
        if (player.level().isClientSide() || amount <= 0.0F) return;
        long currentTime = player.level().getGameTime();
        CompoundTag persistent = player.getPersistentData();
        ListTag history = persistent.getList("xebDamageHistory", 10); // 10 is CompoundTag
        
        CompoundTag entry = new CompoundTag();
        entry.putLong("time", currentTime);
        entry.putFloat("dmg", amount);
        history.add(entry);
        
        persistent.put("xebDamageHistory", history);
    }

    public static float getDamageDealtLast60Seconds(Player player) {
        long currentTime = player.level().getGameTime();
        CompoundTag persistent = player.getPersistentData();
        ListTag history = persistent.getList("xebDamageHistory", 10);
        ListTag cleanHistory = new ListTag();
        float total = 0.0F;
        
        for (int i = 0; i < history.size(); i++) {
            CompoundTag entry = history.getCompound(i);
            long entryTime = entry.getLong("time");
            // 60 seconds = 1200 ticks
            if (currentTime - entryTime <= 1200) {
                total += entry.getFloat("dmg");
                cleanHistory.add(entry.copy());
            }
        }
        
        persistent.put("xebDamageHistory", cleanHistory);
        return total;
    }
}
