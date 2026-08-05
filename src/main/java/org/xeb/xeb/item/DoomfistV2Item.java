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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import org.xeb.xeb.effect.ModEffects;
import net.minecraft.world.entity.Mob;
import org.xeb.xeb.item.capability.IMobWeaponCapability;

public class DoomfistV2Item extends Item implements software.bernie.geckolib.animatable.GeoItem, IMobWeaponCapability {
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
        return org.xeb.xeb.enchantment.ModEnchantments.isModEnchantment(enchantment) 
            || super.canApplyAtEnchantingTable(stack, enchantment);
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
        if (player.getPersistentData().getInt("xebMeteorStrikeState") > 0) {
            return InteractionResultHolder.pass(stack);
        }
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

    // ═══════════════════════════════════════════════════════════════════════════
    // IMobWeaponCapability IMPLEMENTATION (MOB AI LOGIC FOR DOOMFIST V2)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public double getPreferredAttackDistance(Mob mob) {
        return 16.0D;
    }

    @Override
    public void tickMobAI(Mob mob, LivingEntity target, Level level, long gameTime, double distSq) {
        if (level.isClientSide()) return;

        CompoundTag tag = mob.getPersistentData();

        // 1. Tick active Power Block for mob
        int blockTimer = tag.getInt("xebBlockTimer");
        if (tag.getBoolean("xebPowerBlocking")) {
            blockTimer++;
            tag.putInt("xebBlockTimer", blockTimer);
            if (blockTimer >= 50) { // 2.5 seconds max duration
                tag.remove("xebPowerBlocking");
                tag.remove("xebBlockTimer");
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.HOSTILE, 1.0F, 1.2F);
            } else {
                // Defensive resistance & particles
                mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 5, 3, false, false));
                if (gameTime % 3 == 0 && level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 4, 0.4D, 0.4D, 0.4D, 0.05D);
                }
            }
        }

        // 2. Tick active Earthquake Slam v2 shockwave propagation for mob
        if (tag.contains("xebSlam2ShockwaveStep")) {
            int step = tag.getInt("xebSlam2ShockwaveStep");
            double lx = tag.getDouble("xebSlam2ShockwaveLookX");
            double lz = tag.getDouble("xebSlam2ShockwaveLookZ");

            Vec3 horizLook = new Vec3(lx, 0.0D, lz).normalize();
            Vec3 pos = mob.position();

            double dist = step * 1.0D;
            Vec3 center = pos.add(horizLook.scale(dist));
            Vec3 perp = new Vec3(-horizLook.z, 0.0D, horizLook.x).normalize();

            double width = step * 0.6D;
            for (double offset = -width; offset <= width; offset += 0.4D) {
                Vec3 point = center.add(perp.scale(offset));
                DustParticleOptions redDust = new DustParticleOptions(new org.joml.Vector3f(1.0F, 0.1F, 0.1F), 1.2F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(redDust, point.x, point.y + 0.1D, point.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                    if (mob.getRandom().nextFloat() < 0.3F) {
                        serverLevel.sendParticles(ParticleTypes.FLAME, point.x, point.y + 0.1D, point.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                    }
                }
            }

            if (step < 7) {
                tag.putInt("xebSlam2ShockwaveStep", step + 1);
            } else {
                tag.remove("xebSlam2ShockwaveStep");
                tag.remove("xebSlam2ShockwaveLookX");
                tag.remove("xebSlam2ShockwaveLookZ");
            }
        }

        long cdRocket     = tag.getLong("xebMobCD_RocketPunch");
        long cdSlam2      = tag.getLong("xebMobCD_Slam2");
        long cdPowerBlock = tag.getLong("xebMobCD_PowerBlock");
        long cdHandCannon = tag.getLong("xebMobCD_HandCannon");

        // ── A. Activa 2: Power Block (Adopta postura defensiva si su vida baja de 70% o si recibe daño a distancia) (7s CD) ──
        if (mob.getHealth() / mob.getMaxHealth() < 0.70F && gameTime - cdPowerBlock >= 140 && !tag.getBoolean("xebPowerBlocking")) {
            tag.putLong("xebMobCD_PowerBlock", gameTime);
            tag.putBoolean("xebPowerBlocking", true);
            tag.putInt("xebBlockTimer", 0);
            tag.putBoolean("xebUltraCharged", true); // Grants Ultra Charge!

            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 1.5F);
            return;
        }

        // ── B. Activa 1: Earthquake Slam v2 con Onda Expansiva de 7 Bloques & Bunny Hop (8s CD) ──
        if (distSq >= 16.0D && distSq <= 256.0D && gameTime - cdSlam2 >= 160) {
            tag.putLong("xebMobCD_Slam2", gameTime);

            Vec3 dir = target.position().subtract(mob.position()).normalize();
            mob.setDeltaMovement(dir.x * 1.6D, 0.8D, dir.z * 1.6D);
            mob.hurtMarked = true;

            // Start propagating shockwave
            tag.putInt("xebSlam2ShockwaveStep", 1);
            tag.putDouble("xebSlam2ShockwaveLookX", dir.x);
            tag.putDouble("xebSlam2ShockwaveLookZ", dir.z);

            // Deal Earthquake impact damage in 3.5 blocks radius
            AABB area = target.getBoundingBox().inflate(3.5D);
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != mob && e.isAlive() && !e.isAlliedTo(mob))) {
                nearby.hurt(level.damageSources().mobAttack(mob), 14.0F);
                nearby.setDeltaMovement(nearby.getDeltaMovement().x, 0.6D, nearby.getDeltaMovement().z);
                nearby.hurtMarked = true;
            }

            // Bunny Hop boost immediately after landing
            Vec3 hop = new Vec3(dir.x * 1.2D, 0.45D, dir.z * 1.2D);
            mob.setDeltaMovement(hop);

            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2F, 1.2F);
            return;
        }

        // ── C. Click Derecho: Empowered Rocket Punch (si tiene Ultra Charge) o Rocket Punch Normal (5s CD) ──
        if (distSq >= 4.0D && distSq <= 196.0D && gameTime - cdRocket >= 100) {
            tag.putLong("xebMobCD_RocketPunch", gameTime);

            boolean isUltra = tag.getBoolean("xebUltraCharged");
            if (isUltra) tag.remove("xebUltraCharged");

            double speed = isUltra ? 3.0D : 2.4D;
            Vec3 dir = target.position().subtract(mob.position()).normalize();
            mob.setDeltaMovement(dir.x * speed, 0.2D, dir.z * speed);
            mob.hurtMarked = true;

            float damage = isUltra ? 24.0F : 12.0F; // +100% damage if Ultra Charged!
            target.hurt(level.damageSources().mobAttack(mob), damage);
            target.setDeltaMovement(dir.x * 2.2D, 0.4D, dir.z * 2.2D);
            target.hurtMarked = true;

            if (isUltra && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY() + 0.5D, target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
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
