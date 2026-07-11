package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import org.xeb.xeb.compat.ModCompatManager;
import org.xeb.xeb.entity.DemonCoreEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DemonCoreItem extends Item {
    public DemonCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
                boolean isRed = tag.getBoolean("RedCore");
                tag.putBoolean("RedCore", !isRed);
                player.displayClientMessage(Component.literal(
                        !isRed ? "§cDemon Core set to Red Core (Supercritical)!" : "§aDemon Core set to Normal Core!"
                ), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (!level.isClientSide()) {
            // Spawn the Demon Core entity slightly in front of the player
            ItemStack singleDrop = stack.copy();
            singleDrop.setCount(1);
            
            DemonCoreEntity entity = new DemonCoreEntity(level, player.getX(), player.getY() + player.getEyeHeight() - 0.3D, player.getZ(), singleDrop);
            
            // Toss the demon core slightly forward
            Vec3 look = player.getLookAngle();
            entity.setDeltaMovement(look.x * 0.4D, 0.2D, look.z * 0.4D);
            level.addFreshEntity(entity);
            
            // Consume the item from player's hand
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean("RedCore")) {
            tooltip.add(Component.literal("§c[Variant: Red Core (Supercritical)]"));
            tooltip.add(Component.literal("§7Instantly doom everything in a 20x20 area on drop."));
        } else {
            tooltip.add(Component.translatable("item.xeb.demon_core.desc1"));
            tooltip.add(Component.translatable("item.xeb.demon_core.desc2"));
            tooltip.add(Component.translatable("item.xeb.demon_core.desc3"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
