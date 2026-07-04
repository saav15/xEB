package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.xeb.xeb.network.PermanightSyncPacket;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.world.PermanightSavedData;
import net.minecraftforge.network.PacketDistributor;

public class MoonTearItem extends Item {
    public MoonTearItem(Properties properties) {
        // Creative tier item
        super(properties.rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level level, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.moon_tear.desc1"));
        tooltip.add(Component.translatable("item.xeb.moon_tear.desc2"));
        tooltip.add(Component.translatable("item.xeb.moon_tear.desc3"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            PermanightSavedData data = PermanightSavedData.get(serverLevel);

            if (data.isActive()) {
                player.sendSystemMessage(Component.translatable("chat.xeb.permanight.active"));
                return InteractionResultHolder.fail(stack);
            }

            // Play epic thunder sound globally at player
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0F, 0.5F);

            // Trigger immediately!
            data.setActive(true);
            data.setTicksRemaining(24000); // lasts 24000 ticks (a full MC day)
            data.setForceNextNight(false);
            
            // Force server time to night
            serverLevel.setDayTime(18000L);
            
            // Broadcast announcement
            serverLevel.players().forEach(p -> {
                p.sendSystemMessage(Component.translatable("chat.xeb.permanight.start"));
            });
            
            // Sync to all clients
            XEBNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new PermanightSyncPacket(true));

            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
