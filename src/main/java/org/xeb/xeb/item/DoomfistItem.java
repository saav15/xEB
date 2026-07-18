package org.xeb.xeb.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.effect.ModEffects;

public class DoomfistItem extends Item implements software.bernie.geckolib.animatable.GeoItem {
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache cache = software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public DoomfistItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "controller", 2, event -> {
            // Inside the BEWLR (in-hand/GUI) pipeline DataTickets.ENTITY is null, so resolve the
            // wielder through the client render-context tracker with a local-player fallback.
            net.minecraft.world.entity.Entity entity = event.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
            String animName = org.xeb.xeb.client.DoomfistAnimationResolver.resolveAnimationName(entity, this);
            return event.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop(animName));
        }));
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || stack.isEnchanted();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return enchantment == org.xeb.xeb.enchantment.ModEnchantments.MEDALLERO.get() 
            || enchantment == org.xeb.xeb.enchantment.ModEnchantments.IMPACT_OVERLOAD.get() 
            || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private org.xeb.xeb.client.renderer.DoomfistGeoRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new org.xeb.xeb.client.renderer.DoomfistGeoRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {
        com.google.common.collect.ImmutableMultimap.Builder<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> builder = com.google.common.collect.ImmutableMultimap.builder();
        if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
            builder.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 9.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
            builder.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level level, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist.desc1"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist.desc2"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist.desc_damage"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist.desc4", net.minecraft.network.chat.Component.keybind("key.xeb.activa_1")));
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist.desc5", net.minecraft.network.chat.Component.keybind("key.xeb.activa_2")));
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist.desc3"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        return net.minecraft.network.chat.Component.translatable(this.getDescriptionId(stack)).withStyle(net.minecraft.ChatFormatting.RED);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        
        // Activate fall protect tag immediately upon starting the charge
        player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);
        
        if (!level.isClientSide()) {
            // Highly satisfying sci-fi gauntlet charge-up hum sound
            level.playSound(null, player, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int ticksCharged = this.getUseDuration(stack) - timeLeft;
            boolean empowered = player.getPersistentData().getBoolean("xebUppercutEmpoweredPunch");
            float chargeSpeed = empowered ? 1.3F : 1.0F;
            float chargeRatio = Math.min(50.0F, ticksCharged * chargeSpeed) / 50.0F; // Max 50 ticks (2.5s)

            if (!level.isClientSide()) {
                // Apply 3-second (60 ticks) item cooldown to prevent spamming
                player.getCooldowns().addCooldown(this, 60);

                // If fully charged, give Charged Fist II for 5 seconds (amplifier 1 is level II)
                if (chargeRatio >= 1.0F) {
                    player.addEffect(new MobEffectInstance(ModEffects.CHARGED_FIST.get(), 100, 1));
                    level.playSound(null, player, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5F, 0.5F);
                }

                // Activate dash state in player NBT
                net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
                tag.putBoolean("xebDoomfistDashing", true);
                tag.putBoolean("xebDoomfistFallProtect", true);
                tag.putInt("xebDoomfistDashTimer", 15); // Max 15 ticks (0.75s)
                tag.putFloat("xebDoomfistChargeRatio", chargeRatio);

                // Sync to clients
                org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new org.xeb.xeb.network.DoomfistDashPacket(player.getId(), true, chargeRatio)
                );

                // Deep rocket blast sound on release
                level.playSound(null, player, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
            }

            // Launch the player forward (with 1.3x speed/distance multiplier if empowered)
            Vec3 look = player.getLookAngle();
            double speed = 0.8D + chargeRatio * 1.6D; // Up to 2.4 blocks/tick
            if (empowered) {
                speed *= 1.3D;
                if (!level.isClientSide()) {
                    player.getPersistentData().remove("xebUppercutEmpoweredPunch");
                }
            }
            Vec3 motion = new Vec3(look.x * speed, look.y * speed * 0.5D + 0.2D, look.z * speed);
            player.setDeltaMovement(motion);
            player.hurtMarked = true; // Sync velocity to client
        }
    }
}
