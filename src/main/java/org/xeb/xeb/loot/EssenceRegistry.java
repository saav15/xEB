package org.xeb.xeb.loot;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.item.MedallionEssenceItem;
import org.xeb.xeb.medallion.MedallionType;

import java.util.HashMap;
import java.util.Map;

public class EssenceRegistry {
    private static final Map<String, RegistryObject<Item>> ESSENCES = new HashMap<>();

    public static void register(String buffId) {
        RegistryObject<Item> reg = ModItems.ITEMS.register(buffId + "_essence",
                () -> new MedallionEssenceItem(new Item.Properties().stacksTo(64), buffId));
        ESSENCES.put(buffId, reg);
    }

    public static ItemStack createStack(String buffId, MedallionType tier) {
        RegistryObject<Item> reg = ESSENCES.get(buffId);
        if (reg == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(reg.get());
        stack.getOrCreateTag().putString("MedallionTier", tier.name());
        return stack;
    }

    public static Item getEssenceItem(String buffId) {
        RegistryObject<Item> reg = ESSENCES.get(buffId);
        return reg != null ? reg.get() : null;
    }

    public static Map<String, RegistryObject<Item>> getAll() {
        return ESSENCES;
    }
}
