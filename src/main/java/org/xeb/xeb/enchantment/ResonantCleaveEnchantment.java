package org.xeb.xeb.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.ItemStack;

public class ResonantCleaveEnchantment extends Enchantment {
    public ResonantCleaveEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinCost(int level) {
        return 15 + (level - 1) * 9;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 30;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return ModEnchantments.isSpecialItem(stack.getItem());
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return canEnchant(stack);
    }
}
