package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.medallion.MedallionType;

import javax.annotation.Nullable;
import java.util.List;

public class MedallionEssenceItem extends Item {
    private final String buffId;

    public MedallionEssenceItem(Properties properties, String buffId) {
        super(properties);
        this.buffId = buffId;
    }

    public String getBuffId() {
        return buffId;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        EliteBuff buff = EliteBuffRegistry.getById(buffId);
        if (buff != null) {
            tooltip.add(Component.translatable("item.xeb.essence.buff", buff.getDisplayName())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        String tierName = stack.hasTag() ? stack.getTag().getString("MedallionTier") : "COMMON";
        MedallionType tier;
        try {
            tier = MedallionType.valueOf(tierName);
        } catch (IllegalArgumentException e) {
            tier = MedallionType.COMMON;
        }

        int levelNum = switch (tier) {
            case COMMON -> 1;
            case RARE -> 2;
            case LEGENDARY -> 3;
        };

        tooltip.add(Component.translatable("item.xeb.essence.tier", levelNum)
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.xeb.essence." + buffId + ".desc")
                .withStyle(net.minecraft.ChatFormatting.AQUA));

        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && "LEGENDARY".equals(stack.getTag().getString("MedallionTier"));
    }
}
