package org.xeb.xeb.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.entity.HomingMissileEntity;
import org.xeb.xeb.entity.MechaVulcanProjectileEntity;
import org.xeb.xeb.network.MechaSyncPacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MechaOverdriveItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Placeholder constants
    public static final double BASE_DAMAGE = 6.0D;
    public static final double DRILL_BASE_DAMAGE = 8.0D;
    public static final double SPIN_BASE_DAMAGE = 5.0D;
    public static final double JET_BASE_DAMAGE = 7.0D;
    public static final double MAX_SPEED = 1.6D;

    public MechaOverdriveItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, event -> {
            return event.setAndContinue(RawAnimation.begin().thenLoop("Idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.mecha_overdrive.desc1"));
        tooltip.add(Component.translatable("item.xeb.mecha_overdrive.desc2"));
        tooltip.add(Component.translatable("item.xeb.mecha_overdrive.desc_damage"));
        tooltip.add(Component.translatable("item.xeb.mecha_overdrive.desc4", "G"));
        tooltip.add(Component.translatable("item.xeb.mecha_overdrive.desc5", "H"));
        tooltip.add(Component.translatable("item.xeb.mecha_overdrive.desc3"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.getPersistentData().putBoolean("xebMechaVulcanFiring", true);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide() && entity instanceof Player player) {
            int ticksUsed = this.getUseDuration(stack) - count;
            if (ticksUsed % 3 == 0) {
                Vec3 lookDir = player.getLookAngle();
                Vec3 mouthPos = player.getEyePosition(1.0F).subtract(0, 0.3D, 0);

                // Add minimal random spread
                double spread = 0.05D;
                Vec3 velocity = lookDir.add(
                        (player.getRandom().nextDouble() - 0.5D) * spread,
                        (player.getRandom().nextDouble() - 0.5D) * spread,
                        (player.getRandom().nextDouble() - 0.5D) * spread
                ).normalize().scale(2.5D);

                MechaVulcanProjectileEntity projectile = new MechaVulcanProjectileEntity(level, player);
                projectile.moveTo(mouthPos.x, mouthPos.y, mouthPos.z);
                projectile.setDeltaMovement(velocity);
                level.addFreshEntity(projectile);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_LAUNCH, SoundSource.PLAYERS, 0.5F, 1.8F);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        entity.getPersistentData().putBoolean("xebMechaVulcanFiring", false);
        if (entity instanceof ServerPlayer player) {
            syncToClient(player);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) return;

        boolean holdsMecha = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!holdsMecha) {
            // Clean up state if not held
            CompoundTag pData = player.getPersistentData();
            if (pData.getBoolean("xebMechaVulcanFiring") || pData.getDouble("xebMechaMomentum") > 0.0D || pData.getBoolean("xebMechaLevitating")) {
                pData.putBoolean("xebMechaVulcanFiring", false);
                pData.putDouble("xebMechaMomentum", 0.0D);
                pData.putBoolean("xebMechaLevitating", false);
                pData.putBoolean("xebMechaOvercharged", false);
                pData.putInt("xebMechaOverchargeTicks", 0);
                if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    syncToClient(serverPlayer);
                }
            }
            return;
        }

        CompoundTag pData = player.getPersistentData();

        if (!level.isClientSide()) {
            int mechaA1CD = pData.getInt("xebMechaA1Cooldown");
            int mechaA2CD = pData.getInt("xebMechaA2Cooldown");
            if (mechaA1CD > 0) pData.putInt("xebMechaA1Cooldown", mechaA1CD - 1);
            if (mechaA2CD > 0) pData.putInt("xebMechaA2Cooldown", mechaA2CD - 1);
        }

        Vec3 currentMotion = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(currentMotion.x * currentMotion.x + currentMotion.z * currentMotion.z);
        boolean isMovingForward = player.zza > 0;
        boolean onGround = player.onGround();
        boolean jetActive = false;

        if (!level.isClientSide()) {
            double momentum = pData.getDouble("xebMechaMomentum");

            if (onGround) {
                if (isMovingForward) {
                    jetActive = true;
                    // Build momentum: 0.015 per tick
                    momentum = Math.min(1.0D, momentum + 0.015D);
                    
                    Vec3 moveDir = new Vec3(currentMotion.x, 0.0D, currentMotion.z).normalize();
                    if (moveDir.lengthSqr() < 0.01D) {
                        Vec3 look = player.getLookAngle();
                        moveDir = new Vec3(look.x, 0.0D, look.z).normalize();
                    }
                    double accel = 0.015D + (momentum * 0.03D);
                    player.setDeltaMovement(currentMotion.x + moveDir.x * accel, currentMotion.y, currentMotion.z + moveDir.z * accel);
                    player.hurtMarked = true;
                } else {
                    // Decay momentum: 0.03 per tick
                    momentum = Math.max(0.0D, momentum - 0.03D);
                    // High artificial friction when stopping
                    player.setDeltaMovement(currentMotion.multiply(0.4D, 1.0D, 0.4D));
                    if (level instanceof ServerLevel serverLevel && level.getGameTime() % 2 == 0) {
                        double speed = Math.sqrt(currentMotion.x * currentMotion.x + currentMotion.z * currentMotion.z);
                        if (speed > 0.05D) {
                            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 0.1D, player.getZ(),
                                    2, 0.2D, 0.0D, 0.2D, 0.01D);
                            serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.1D, player.getZ(),
                                    3, 0.2D, 0.0D, 0.2D, 0.05D);
                        }
                    }
                }
            } else {
                // Air hover: Maintain horizontal momentum, reduce gravity (neutralize 0.06 of 0.08 vanilla)
                jetActive = true;
                player.setDeltaMovement(currentMotion.x, currentMotion.y + 0.06D, currentMotion.z);
                player.hurtMarked = true;
            }

            pData.putDouble("xebMechaMomentum", momentum);
            pData.putBoolean("xebMechaJetActive", jetActive);
            pData.putDouble("xebMechaKineticSpeed", horizontalSpeed);

            // Levitating state
            boolean levitating = momentum > 0.7D;
            pData.putBoolean("xebMechaLevitating", levitating);

            // Pasiva 3 - Overcharge Core
            if (momentum >= 1.0D) {
                int overchargeTicks = pData.getInt("xebMechaOverchargeTicks") + 1;
                pData.putInt("xebMechaOverchargeTicks", overchargeTicks);
                if (overchargeTicks >= 60) {
                    pData.putBoolean("xebMechaOvercharged", true);
                }
            } else {
                pData.putInt("xebMechaOverchargeTicks", 0);
            }

            // Handle Jet Dash tick updates
            if (pData.getBoolean("xebMechaOverdriveDashing")) {
                int dashTicks = pData.getInt("xebMechaDashTicks");
                if (dashTicks > 0) {
                    pData.putInt("xebMechaDashTicks", dashTicks - 1);
                    player.invulnerableTime = 999;

                    Vec3 lookDir = player.getLookAngle();
                    Vec3 forwardDir = new Vec3(lookDir.x, 0.0D, lookDir.z).normalize();
                    
                    // Move forward step-by-step
                    double stepSize = 0.5D;
                    Vec3 nextPos = player.position().add(forwardDir.scale(stepSize));
                    player.setPos(nextPos.x, player.getY(), nextPos.z);
                    player.setDeltaMovement(forwardDir.x * 2.0D, player.getDeltaMovement().y, forwardDir.z * 2.0D);
                    player.hurtMarked = true;

                    BlockPos targetPos = player.blockPosition().relative(player.getDirection());
                    BlockState blockState = level.getBlockState(targetPos);
                    if (!blockState.isAir() && blockState.getDestroySpeed(level, targetPos) >= 0 && blockState.getDestroySpeed(level, targetPos) < 50.0F) {
                        level.destroyBlock(targetPos, true);
                    }

                    // Entity Sweep
                    AABB sweep = player.getBoundingBox().inflate(1.5D);
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != player && e.isAlive())) {
                        String nbtKey = "xebHitByJetDash_" + target.getUUID().toString();
                        if (!pData.getBoolean(nbtKey)) {
                            pData.putBoolean(nbtKey, true);
                            double dmg = JET_BASE_DAMAGE;
                            target.hurt(player.damageSources().playerAttack(player), (float) dmg);
                            target.setDeltaMovement(target.getDeltaMovement().add(lookDir.x * 2.5D, 0.8D, lookDir.z * 2.5D));
                            target.hurtMarked = true;
                        }
                    }
                } else {
                    pData.putBoolean("xebMechaOverdriveDashing", false);
                    player.invulnerableTime = 0;
                    
                    // Clean hit keys
                    List<String> keysToRemove = new ArrayList<>();
                    for (String key : pData.getAllKeys()) {
                        if (key.startsWith("xebHitByJetDash_")) {
                            keysToRemove.add(key);
                        }
                    }
                    for (String key : keysToRemove) {
                        pData.remove(key);
                    }
                }
            }

            // Handle Spindash charging or attacking
            int sdState = pData.getInt("xebMechaSpindashState");
            if (sdState == 1) {
                // CHARGING
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                player.hurtMarked = true;
                int charge = pData.getInt("xebMechaSpindashCharge");
                if (charge < 40) {
                    pData.putInt("xebMechaSpindashCharge", charge + 1);
                }
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.5D, player.getZ(),
                            2, 0.3D, 0.3D, 0.3D, 0.1D);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.5D, player.getZ(),
                            2, 0.3D, 0.3D, 0.3D, 0.05D);
                }
            } else if (sdState == 3) {
                // ATTACKING
                int sdTicks = pData.getInt("xebMechaSpindashTicks");
                int hitCooldown = pData.getInt("xebMechaSpindashHitCooldown");
                if (hitCooldown > 0) {
                    pData.putInt("xebMechaSpindashHitCooldown", hitCooldown - 1);
                }

                if (sdTicks > 0) {
                    pData.putInt("xebMechaSpindashTicks", sdTicks - 1);

                    if (hitCooldown <= 0) {
                        AABB sweep = player.getBoundingBox().inflate(1.5D);
                        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != player && e.isAlive());
                        
                        for (LivingEntity target : targets) {
                            String nbtKey = "xebHitBySpindash_" + target.getUUID().toString();
                            if (!pData.getBoolean(nbtKey)) {
                                pData.putBoolean(nbtKey, true);
                                double baseDmg = SPIN_BASE_DAMAGE;
                                double mult = 1.0D + pData.getInt("xebMechaSpindashCharge") * 0.08D;
                                double dmg = baseDmg * mult;
                                target.hurt(player.damageSources().playerAttack(player), (float) dmg);

                                int totalHits = pData.getInt("xebSpindashHitCount") + 1;
                                pData.putInt("xebSpindashHitCount", totalHits);

                                if (totalHits >= 3) {
                                    // Hit 3: Pass through without knockback, let player keep moving
                                    Vec3 look = player.getLookAngle().normalize().scale(1.5D);
                                    player.moveTo(target.getX() + look.x, target.getY(), target.getZ() + look.z);
                                } else {
                                    // Hit 1 or 2: rebound player and standard knockback target
                                    target.setDeltaMovement(player.getLookAngle().normalize().scale(1.2D).add(0, 0.3D, 0));
                                    target.hurtMarked = true;
                                    
                                    // Bounce player backwards
                                    Vec3 motion = player.getDeltaMovement();
                                    player.setDeltaMovement(-motion.x * 0.8D, 0.3D, -motion.z * 0.8D);
                                    player.hurtMarked = true;
                                    
                                    pData.putInt("xebMechaSpindashHitCooldown", 5);
                                    pData.putBoolean(nbtKey, false);
                                    break;
                                }
                            }
                        }
                    }

                    // Check wall hit to stop early
                    Vec3 nextPos = player.position().add(player.getDeltaMovement());
                    net.minecraft.world.phys.BlockHitResult wallClip = level.clip(new net.minecraft.world.level.ClipContext(
                            player.position(), nextPos, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player
                    ));
                    if (wallClip.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                        pData.putInt("xebMechaSpindashTicks", 0);
                    }
                } else {
                    // Reset spindash state
                    pData.putInt("xebMechaSpindashState", 0);
                    pData.putInt("xebSpindashHitCount", 0);
                    // Clear NBT hit keys
                    List<String> keysToRemove = new ArrayList<>();
                    for (String key : pData.getAllKeys()) {
                        if (key.startsWith("xebHitBySpindash_")) {
                            keysToRemove.add(key);
                        }
                    }
                    for (String key : keysToRemove) {
                        pData.remove(key);
                    }
                }
            }

            // Homing Missile ticking
            if (pData.getBoolean("xebMechaMissileSalvoFiring")) {
                int salvoTicks = pData.getInt("xebMechaMissileSalvoTicks");
                if (salvoTicks > 0) {
                    pData.putInt("xebMechaMissileSalvoTicks", salvoTicks - 1);
                    if (salvoTicks % 4 == 0) {
                        // Fire homing missile at next target
                        String targetsStr = pData.getString("xebMechaMissileTargets");
                        if (!targetsStr.isEmpty()) {
                            String[] uuids = targetsStr.split(",");
                            int index = (salvoTicks / 4) % uuids.length;
                            try {
                                UUID targetUUID = UUID.fromString(uuids[index]);
                                Entity target = ((ServerLevel) level).getEntity(targetUUID);
                                if (target instanceof LivingEntity living && living.isAlive()) {
                                    HomingMissileEntity missile = new HomingMissileEntity(level, player, living);
                                    missile.moveTo(player.getX(), player.getY() + 1.2D, player.getZ());
                                    level.addFreshEntity(missile);

                                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                            SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
                                }
                            } catch (Exception e) {
                                // target lost
                            }
                        }
                    }
                } else {
                    pData.putBoolean("xebMechaMissileSalvoFiring", false);
                    pData.remove("xebMechaMissileTargets");
                }
            }

            // Sync packet to client when state changes
            if (player instanceof ServerPlayer serverPlayer && level.getGameTime() % 2 == 0) {
                syncToClient(serverPlayer);
            }
        }
    }

    public static void syncToClient(ServerPlayer player) {
        CompoundTag pData = player.getPersistentData();
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new MechaSyncPacket(
                        player.getId(),
                        pData.getBoolean("xebMechaJetActive"),
                        pData.getBoolean("xebMechaVulcanFiring"),
                        pData.getInt("xebMechaSpindashState"),
                        pData.getBoolean("xebMechaOverdriveDashing"),
                        pData.getInt("xebMechaA1Cooldown"),
                        pData.getInt("xebMechaA2Cooldown"),
                        pData.getDouble("xebMechaKineticSpeed"),
                        pData.getDouble("xebMechaMomentum"),
                        pData.getBoolean("xebMechaOvercharged"),
                        pData.getBoolean("xebMechaLevitating"),
                        pData.getInt("xebMechaSpindashCharge")
                )
        );
    }
}
