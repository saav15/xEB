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

public class DoomfistV2Item extends Item implements software.bernie.geckolib.animatable.GeoItem {
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache cache = software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public DoomfistV2Item(Properties properties) {
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
        return enchantment == org.xeb.xeb.enchantment.ModEnchantments.MEDALLERO.get() || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private org.xeb.xeb.client.renderer.DoomfistV2GeoRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new org.xeb.xeb.client.renderer.DoomfistV2GeoRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {
        com.google.common.collect.ImmutableMultimap.Builder<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> builder = com.google.common.collect.ImmutableMultimap.builder();
        if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
            builder.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 7.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
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
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist_v2.desc1"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist_v2.desc2"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist_v2.desc_damage"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist_v2.desc4", net.minecraft.network.chat.Component.keybind("key.xeb.activa_1")));
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist_v2.desc5", net.minecraft.network.chat.Component.keybind("key.xeb.activa_2")));
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.xeb.doomfist_v2.desc3"));
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
        
        player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);
        
        if (!level.isClientSide()) {
            level.playSound(null, player, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int ticksCharged = this.getUseDuration(stack) - timeLeft;
            float chargeRatio = Math.min(50.0F, ticksCharged) / 50.0F;

            if (!level.isClientSide()) {
                player.getCooldowns().addCooldown(this, 60);

                if (chargeRatio >= 1.0F) {
                    player.addEffect(new MobEffectInstance(ModEffects.CHARGED_FIST.get(), 100, 1));
                    level.playSound(null, player, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5F, 0.5F);
                }

                net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
                tag.putBoolean("xebDoomfistDashing", true);
                tag.putBoolean("xebDoomfistFallProtect", true);
                tag.putInt("xebDoomfistDashTimer", 15);
                tag.putFloat("xebDoomfistChargeRatio", chargeRatio);

                // Check and transfer Ultra Charge to active dash
                if (tag.getBoolean("xebUltraCharged")) {
                    tag.putBoolean("xebDashIsUltraCharged", true);
                    tag.remove("xebUltraCharged");
                    
                    org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new org.xeb.xeb.network.DoomfistUltraChargeSyncPacket(player.getId(), false)
                    );
                }

                // Server-side Earthquake Slam cancel sync
                if (tag.contains("xebSlam2State")) {
                    tag.remove("xebSlam2State");
                    tag.remove("xebSlam2Timer");
                    org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new org.xeb.xeb.network.DoomfistAbilitySyncPacket(player.getId(), 0, 0)
                    );
                }

                org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new org.xeb.xeb.network.DoomfistDashPacket(player.getId(), true, chargeRatio)
                );

                level.playSound(null, player, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
            }

            // Compute momentum transfer on both client and server for smooth physics
            net.minecraft.nbt.CompoundTag checkTag = player.getPersistentData();
            double bonusX = 0.0D;
            double bonusZ = 0.0D;
            if (checkTag.contains("xebSlam2State")) {
                Vec3 curMotion = player.getDeltaMovement();
                bonusX = curMotion.x * 1.5D;
                bonusZ = curMotion.z * 1.5D;
                
                if (level.isClientSide()) {
                    checkTag.remove("xebSlam2State");
                    checkTag.remove("xebSlam2Timer");
                }
            }

            Vec3 look = player.getLookAngle();
            double speed = 0.8D + chargeRatio * 1.6D;
            Vec3 motion = new Vec3(look.x * speed + bonusX, look.y * speed * 0.5D + 0.2D, look.z * speed + bonusZ);
            player.setDeltaMovement(motion);
            player.hurtMarked = true;
        }
    }
}
