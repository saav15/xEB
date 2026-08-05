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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import org.xeb.xeb.effect.ModEffects;
import net.minecraft.world.entity.Mob;
import org.xeb.xeb.item.capability.IMobWeaponCapability;

public class DoomfistItem extends Item implements software.bernie.geckolib.animatable.GeoItem, IMobWeaponCapability {
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
        return org.xeb.xeb.enchantment.ModEnchantments.isModEnchantment(enchantment) 
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
        if (player.getPersistentData().getInt("xebMeteorStrikeState") > 0) {
            return InteractionResultHolder.pass(stack);
        }
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

    // ═══════════════════════════════════════════════════════════════════════════
    // IMobWeaponCapability IMPLEMENTATION (MOB AI LOGIC FOR DOOMFIST V1)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public double getPreferredAttackDistance(Mob mob) {
        return 16.0D;
    }

    @Override
    public void tickMobAI(Mob mob, LivingEntity target, Level level, long gameTime, double distSq) {
        if (level.isClientSide()) return;

        CompoundTag tag = mob.getPersistentData();

        long cdRocket     = tag.getLong("xebMobCD_RocketPunch");
        long cdUppercut   = tag.getLong("xebMobCD_Uppercut");
        long cdSlam       = tag.getLong("xebMobCD_Slam");
        long cdHandCannon = tag.getLong("xebMobCD_HandCannon");

        // ── A. Activa 1: Rising Uppercut (3 - 10 blocks, 7s CD) ──
        if (distSq >= 9.0D && distSq <= 100.0D && gameTime - cdUppercut >= 140) {
            tag.putLong("xebMobCD_Uppercut", gameTime);
            tag.putBoolean("xebUppercutEmpoweredPunch", true); // Empower next Rocket Punch (+30% speed)
            tag.putInt("xebUppercutFloatTicks", 40);

            Vec3 look = target.position().subtract(mob.position()).normalize();
            mob.setDeltaMovement(look.x * 0.6D, 1.2D, look.z * 0.6D);
            mob.hurtMarked = true;

            AABB area = mob.getBoundingBox().inflate(1.5D, 1.0D, 1.5D);
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != mob && e.isAlive() && !e.isAlliedTo(mob))) {
                nearby.hurt(level.damageSources().mobAttack(mob), 10.0F);
                nearby.setDeltaMovement(nearby.getDeltaMovement().x * 0.5D, 0.9D, nearby.getDeltaMovement().z * 0.5D);
                nearby.hurtMarked = true;
            }

            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.5F, 1.4F);
            return;
        }

        // ── B. Activa 2: Seismic Slam v1 (4 - 15 blocks, 10s CD) ──
        if (distSq >= 16.0D && distSq <= 225.0D && gameTime - cdSlam >= 200) {
            tag.putLong("xebMobCD_Slam", gameTime);
            Vec3 dir = target.position().subtract(mob.position()).normalize();
            mob.setDeltaMovement(dir.x * 1.5D, 0.85D, dir.z * 1.5D);
            mob.hurtMarked = true;

            AABB area = target.getBoundingBox().inflate(3.0D);
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != mob && e.isAlive() && !e.isAlliedTo(mob))) {
                nearby.hurt(level.damageSources().mobAttack(mob), 14.0F);
            }
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2F, 0.8F);
            return;
        }

        // ── C. Click Derecho: Charged Rocket Punch (2 - 14 blocks, 5s CD) ──
        if (distSq >= 4.0D && distSq <= 196.0D && gameTime - cdRocket >= 100) {
            tag.putLong("xebMobCD_RocketPunch", gameTime);
            boolean empowered = tag.getBoolean("xebUppercutEmpoweredPunch");
            if (empowered) tag.remove("xebUppercutEmpoweredPunch");

            double speed = empowered ? 2.4D * 1.3D : 2.4D;
            Vec3 dir = target.position().subtract(mob.position()).normalize();
            mob.setDeltaMovement(dir.x * speed, 0.2D, dir.z * speed);
            mob.hurtMarked = true;

            // Deal rocket punch damage + Wall Slam check
            float damage = empowered ? 18.0F : 12.0F;
            target.hurt(level.damageSources().mobAttack(mob), damage);
            target.setDeltaMovement(dir.x * 1.8D, 0.35D, dir.z * 1.8D);
            target.hurtMarked = true;

            // Check Wall Slam: If target's new position hits a solid block
            net.minecraft.core.BlockPos wallPos = target.blockPosition().relative(mob.getDirection());
            if (level.getBlockState(wallPos).isSolid()) {
                target.hurt(level.damageSources().mobAttack(mob), damage * 0.5F); // +50% wall slam damage
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2F, 0.9F);
            }

            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.2F, 0.6F);
            return;
        }

        // ── D. Ranged (3 - 12 blocks): Kinetic Hand Cannon Shot (2s CD) ──
        if (distSq > 9.0D && distSq <= 144.0D && gameTime - cdHandCannon >= 40) {
            tag.putLong("xebMobCD_HandCannon", gameTime);
            target.hurt(level.damageSources().mobAttack(mob), 8.0F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(0.5D), target.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
            }
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.4F);
        }
    }
}
