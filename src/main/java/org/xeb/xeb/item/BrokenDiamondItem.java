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

public class BrokenDiamondItem extends Item {
    public BrokenDiamondItem(Properties properties) {
        super(properties.stacksTo(1));
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
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
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
                                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                        new CrazyDiamondSyncPacket(
                                            tag.getInt("xebCDA1CooldownTicks"),
                                            tag.getInt("xebCDA2CooldownTicks"),
                                            punches + 1,
                                            0
                                        ));
                            } else {
                                tag.putInt("xebCDChargeTimer", chargeTimer);
                                if (chargeTimer % 5 == 0) { // periodically sync to keep HUD smooth
                                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
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
                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
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
    
    private CrazyDiamondEntity findStand(Player player, Level level) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (Entity e : serverLevel.getAllEntities()) {
                if (e instanceof CrazyDiamondEntity stand && player.getUUID().equals(stand.getOwnerUUID())) {
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
}
