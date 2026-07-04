package org.xeb.xeb.compat.jade;

import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import java.util.List;

public enum XEBJadeRenderer implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (accessor.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
            List<MedallionData> medallions = MedallionManager.getMedallions(living);
            if (!medallions.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.xeb.jade.header").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
                for (MedallionData m : medallions) {
                    net.minecraft.ChatFormatting color = switch (m.getTier()) {
                        case COMMON -> net.minecraft.ChatFormatting.GOLD; // Bronze/Orange
                        case RARE -> net.minecraft.ChatFormatting.AQUA; // Silver/Blue
                        default -> net.minecraft.ChatFormatting.YELLOW; // Golden/Yellow
                    };
                    
                    Component text = Component.literal("  • ")
                        .append(Component.literal("[" + m.getTier().name() + "] ").withStyle(color))
                        .append(m.getBuff().getDisplayName());
                    tooltip.add(text);
                }
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return XEBJadePlugin.MEDALLIONS;
    }
}
