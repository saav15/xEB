package org.xeb.xeb.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.xeb.xeb.entity.CrazyDiamondEntity;
import org.xeb.xeb.entity.ModEntities;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.network.CrazyDiamondSyncPacket;
import net.minecraftforge.network.PacketDistributor;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.entity.Mob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.entity.RestoreProjectileEntity;
import org.xeb.xeb.item.capability.IMobWeaponCapability;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BrokenDiamondItem extends Item implements GeoItem, IMobWeaponCapability {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BrokenDiamondItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || stack.isEnchanted();
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, net.minecraft.core.BlockPos pos, Player player) {
        return true;
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
        return org.xeb.xeb.enchantment.ModEnchantments.isModEnchantment(enchantment) || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.getPersistentData().getInt("xebCDForceCD") > 0) {
            return InteractionResultHolder.fail(stack);
        }
        
        // Spend punches to activate Dorarara! barrage
        int punches = player.getPersistentData().getInt("xebCDPunches");
        if (punches > 0) {
            int duration = punches * 40; // 2s (40 ticks) per charge
            player.getPersistentData().putInt("xebCDActiveBarrages", punches);
            player.getPersistentData().putInt("xebCDBarrageTimer", duration);
            player.getPersistentData().putInt("xebCDPunches", 0); // reset
            player.getPersistentData().putInt("xebCDChargeTimer", 0); // reset
            
            if (!level.isClientSide()) {
                player.getPersistentData().putInt("xebCDForceCD", duration);
                player.getCooldowns().addCooldown(this, duration); // gray cooldown visual
                
                if (!player.onGround()) {
                    player.getPersistentData().putInt("xebCDLevitateTicks", duration);
                }
                
                CrazyDiamondEntity stand = findStand(player, level);
                if (stand != null) {
                    stand.setAnimState(CrazyDiamondEntity.STATE_BARRAGE, duration);
                }
                
                level.playSound(null, player, net.minecraft.sounds.SoundEvents.IRON_GOLEM_ATTACK, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.2F);
                
                // Sync immediately to client
                XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) player),
                        new CrazyDiamondSyncPacket(
                            player.getPersistentData().getInt("xebCDA1CooldownTicks"),
                            player.getPersistentData().getInt("xebCDA2CooldownTicks"),
                            0,
                            0
                        ));
            }
            return InteractionResultHolder.consume(stack);
        }
        
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (entity instanceof Player player) {
            if (!level.isClientSide()) {
                boolean hasMain = player.getMainHandItem().is(this);
                boolean hasOff = player.getOffhandItem().is(this);
                boolean isEquipped = hasMain || hasOff;
                
                if (player.getPersistentData().contains("xebCDForceCD")) {
                    int fc = player.getPersistentData().getInt("xebCDForceCD");
                    if (fc > 0) player.getPersistentData().putInt("xebCDForceCD", fc - 1);
                }
                
                CrazyDiamondEntity stand = null;
                if (player.getPersistentData().contains("xebCrazyDiamondEntityId")) {
                    int id = player.getPersistentData().getInt("xebCrazyDiamondEntityId");
                    Entity e = level.getEntity(id);
                    if (e instanceof CrazyDiamondEntity cds && cds.isAlive()) {
                        stand = cds;
                    }
                }
                
                if (isEquipped) {
                    if (stand == null) {
                        stand = findStand(player, level);
                        if (stand == null) {
                            stand = new CrazyDiamondEntity(ModEntities.CRAZY_DIAMOND.get(), level);
                            stand.setOwnerUUID(player.getUUID());
                            stand.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                            level.addFreshEntity(stand);
                        }
                        player.getPersistentData().putInt("xebCrazyDiamondEntityId", stand.getId());
                    }
                    
                    if (stand != null) {
                        net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
                        boolean isBarrageActive = tag.getInt("xebCDBarrageTimer") > 0;
                        int activeBarrages = tag.getInt("xebCDActiveBarrages");
                        
                        double searchRange = activeBarrages == 1 ? 3.0D : (activeBarrages == 2 ? 5.0D : 8.0D);
                        List<LivingEntity> enemies = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(searchRange),
                                (e) -> e != player && e.isAlive() && !e.isAlliedTo(player) && !(e instanceof org.xeb.xeb.entity.CrazyDiamondEntity));
                        
                        boolean shouldChase = isBarrageActive && !enemies.isEmpty();
                        
                        if (!shouldChase) {
                            net.minecraft.world.phys.Vec3 lookVec = player.getLookAngle();
                            net.minecraft.world.phys.Vec3 upVec = new net.minecraft.world.phys.Vec3(0, 1, 0);
                            net.minecraft.world.phys.Vec3 leftVec = lookVec.cross(upVec); 

                            double targetX = player.getX() + leftVec.x * 0.6D - lookVec.x * 0.3D;
                            double targetZ = player.getZ() + leftVec.z * 0.6D - lookVec.z * 0.3D;
                            
                            double targetY = player.getY() + player.getEyeHeight() - 0.8D;
                            if (player.isCrouching()) {
                                targetY -= 0.3D;
                            }
                            
                            double finalX = net.minecraft.util.Mth.lerp(0.5D, stand.getX(), targetX);
                            double finalY = net.minecraft.util.Mth.lerp(0.5D, stand.getY(), targetY);
                            double finalZ = net.minecraft.util.Mth.lerp(0.5D, stand.getZ(), targetZ);
                            
                            stand.moveTo(finalX, finalY, finalZ, player.getYRot(), player.getXRot());
                            stand.setYRot(player.getYRot());
                            stand.setXRot(player.getXRot());
                            stand.yHeadRot = player.getYRot();
                            stand.yBodyRot = player.getYRot();
                        } else {
                            LivingEntity firstEnemy = enemies.get(0);
                            double targetX = firstEnemy.getX();
                            double targetY = firstEnemy.getY();
                            double targetZ = firstEnemy.getZ();
                            
                            double finalX = net.minecraft.util.Mth.lerp(0.35D, stand.getX(), targetX);
                            double finalY = net.minecraft.util.Mth.lerp(0.35D, stand.getY(), targetY);
                            double finalZ = net.minecraft.util.Mth.lerp(0.35D, stand.getZ(), targetZ);
                            
                            stand.moveTo(finalX, finalY, finalZ, stand.getYRot(), stand.getXRot());
                            stand.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, firstEnemy.getEyePosition());
                        }
                    }
                    
                    // Server-side automatic charging of fists
                    net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
                    int punches = tag.getInt("xebCDPunches");
                    if (punches < 3) {
                        if (tag.getInt("xebCDBarrageTimer") <= 0) {
                            int chargeTimer = tag.getInt("xebCDChargeTimer") + 1;
                            if (chargeTimer >= 60) { // 3 seconds
                                tag.putInt("xebCDPunches", punches + 1);
                                tag.putInt("xebCDChargeTimer", 0);
                                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS,
                                        0.3F, 1.6F);
                                XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) player),
                                        new CrazyDiamondSyncPacket(
                                            tag.getInt("xebCDA1CooldownTicks"),
                                            tag.getInt("xebCDA2CooldownTicks"),
                                            punches + 1,
                                            0
                                        ));
                            } else {
                                tag.putInt("xebCDChargeTimer", chargeTimer);
                                if (chargeTimer % 5 == 0) { // periodically sync to keep HUD smooth
                                    XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) player),
                                            new CrazyDiamondSyncPacket(
                                                tag.getInt("xebCDA1CooldownTicks"),
                                                tag.getInt("xebCDA2CooldownTicks"),
                                                punches,
                                                chargeTimer
                                            ));
                                }
                            }
                        }
                    } else {
                        tag.putInt("xebCDChargeTimer", 0);
                    }
                } else {
                    if (stand != null) {
                        stand.discard();
                        player.getPersistentData().remove("xebCrazyDiamondEntityId");
                    }
                    
                    if (player.getPersistentData().contains("xebCDPunches") || player.getPersistentData().contains("xebCDChargeTimer")) {
                        player.getPersistentData().remove("xebCDPunches");
                        player.getPersistentData().remove("xebCDChargeTimer");
                        XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) player),
                                new CrazyDiamondSyncPacket(
                                    player.getPersistentData().getInt("xebCDA1CooldownTicks"),
                                    player.getPersistentData().getInt("xebCDA2CooldownTicks"),
                                    0,
                                    0
                                ));
                    }
                }
            }
        }
    }
    
    public static CrazyDiamondEntity findStand(LivingEntity entity, Level level) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (Entity e : serverLevel.getAllEntities()) {
                if (e instanceof CrazyDiamondEntity stand && entity.getUUID().equals(stand.getOwnerUUID())) {
                    return stand;
                }
            }
        }
        return null;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(net.minecraft.ChatFormatting.RED);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.broken_diamond.desc1"));
        tooltip.add(Component.translatable("item.xeb.broken_diamond.desc2"));
        tooltip.add(Component.translatable("item.xeb.broken_diamond.desc3"));
        tooltip.add(Component.translatable("item.xeb.broken_diamond.desc_damage"));
        tooltip.add(Component.translatable("item.xeb.broken_diamond.activa1", Component.keybind("key.xeb.activa_1")));
        tooltip.add(Component.translatable("item.xeb.broken_diamond.activa2", Component.keybind("key.xeb.activa_2")));
        tooltip.add(Component.translatable("item.xeb.broken_diamond.lore"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return event.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("Idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private org.xeb.xeb.client.renderer.BrokenDiamondItemGeoRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new org.xeb.xeb.client.renderer.BrokenDiamondItemGeoRenderer();
                }
                return this.renderer;
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IMobWeaponCapability IMPLEMENTATION (MOB AI LOGIC FOR BROKEN DIAMOND)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public double getPreferredAttackDistance(Mob mob) {
        return 16.0D;
    }

    @Override
    public void tickMobAI(Mob mob, LivingEntity target, Level level, long gameTime, double distSq) {
        if (level.isClientSide()) return;

        CompoundTag tag = mob.getPersistentData();

        // 1. Ensure Stand (CrazyDiamondEntity) is spawned next to the mob!
        CrazyDiamondEntity stand = findStand(mob, level);
        if (stand == null) {
            stand = new CrazyDiamondEntity(ModEntities.CRAZY_DIAMOND.get(), level);
            stand.setOwnerUUID(mob.getUUID());
            stand.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
            level.addFreshEntity(stand);
            tag.putInt("xebCrazyDiamondEntityId", stand.getId());
        }

        long cdBarrage = tag.getLong("xebMobCD_CDBarrage");
        long cdTrap    = tag.getLong("xebMobCD_CDRockTrap");
        long cdRestore = tag.getLong("xebMobCD_CDRestore");

        // ── A. Dorarara Barrage (4 - 12 blocks, 5s CD): Stand chases target and delivers rapid punch flurry ──
        if (distSq <= 144.0D && gameTime - cdBarrage >= 100) {
            tag.putLong("xebMobCD_CDBarrage", gameTime);
            tag.putInt("xebCDActiveBarrages", 3);
            tag.putInt("xebCDBarrageTimer", 60); // 3 seconds barrage

            if (stand != null) {
                stand.setAnimState(CrazyDiamondEntity.STATE_BARRAGE, 60);
                stand.moveTo(target.getX(), target.getY(), target.getZ(), mob.getYRot(), mob.getXRot());
            }

            // Deal barrage damage to target + heal mob allies nearby
            target.hurt(level.damageSources().mobAttack(mob), 15.0F);
            mob.heal(4.0F);

            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.2F, 1.2F);
            return;
        }

        // ── B. Activa 1: Rock Burial Trap & Kick Combo (4 - 16 blocks, 8s CD) ──
        if (distSq <= 256.0D && gameTime - cdTrap >= 160) {
            tag.putLong("xebMobCD_CDRockTrap", gameTime);

            target.addEffect(new MobEffectInstance(org.xeb.xeb.effect.ModEffects.PETRIFY.get(), 60, 0));

            if (stand != null) {
                stand.moveTo(target.getX(), target.getY(), target.getZ(), mob.getYRot(), mob.getXRot());
                stand.setAnimState(CrazyDiamondEntity.STATE_KICKING, 20);
            }

            target.hurt(level.damageSources().mobAttack(mob), 12.0F);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.2F, 0.8F);
            return;
        }

        // ── C. Activa 2: Digging & Restore Homing Projectile (8 - 24 blocks, 10s CD) ──
        if (distSq >= 64.0D && distSq <= 576.0D && gameTime - cdRestore >= 200) {
            tag.putLong("xebMobCD_CDRestore", gameTime);

            if (stand != null) {
                stand.setAnimState(CrazyDiamondEntity.STATE_DIGGING, 12);
            }

            RestoreProjectileEntity restoreProj = new RestoreProjectileEntity(level, mob, Blocks.STONE.defaultBlockState());
            Vec3 eyePos = mob.getEyePosition(1.0F);
            Vec3 look = target.getEyePosition().subtract(eyePos).normalize();
            restoreProj.moveTo(eyePos.x, eyePos.y - 0.2D, eyePos.z, mob.getYRot(), mob.getXRot());
            restoreProj.setDeltaMovement(look.scale(1.6D));
            level.addFreshEntity(restoreProj);

            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.IRON_GOLEM_HURT, SoundSource.HOSTILE, 1.0F, 1.4F);
        }
    }
}
