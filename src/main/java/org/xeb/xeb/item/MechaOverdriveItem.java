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
    public static final java.util.UUID STEP_HEIGHT_UUID = java.util.UUID.fromString("6a5bc382-7d2d-4f1b-8c8f-fbfa62e840d5");

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
        tooltip.add(Component.translatable("item.xeb.mecha_overdrive.desc4", Component.keybind("key.xeb.activa_1")));
        tooltip.add(Component.translatable("item.xeb.mecha_overdrive.desc5", Component.keybind("key.xeb.activa_2")));
        tooltip.add(Component.translatable("item.xeb.mecha_overdrive.desc3"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(net.minecraft.ChatFormatting.RED);
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
                
                var attr = player.getAttribute(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get());
                if (attr != null && attr.getModifier(STEP_HEIGHT_UUID) != null) {
                    attr.removeModifier(STEP_HEIGHT_UUID);
                }
                
                if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    syncToClient(serverPlayer);
                }
            } else {
                var attr = player.getAttribute(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get());
                if (attr != null && attr.getModifier(STEP_HEIGHT_UUID) != null) {
                    attr.removeModifier(STEP_HEIGHT_UUID);
                }
            }
            return;
        }

        // Apply auto-step height modifier (1.25F step height total)
        var attr = player.getAttribute(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (attr != null && attr.getModifier(STEP_HEIGHT_UUID) == null) {
            attr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    STEP_HEIGHT_UUID, "Mecha Overdrive Step Height", 0.65D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        }

        CompoundTag pData = player.getPersistentData();

        if (level.isClientSide()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (player == mc.player) {
                // Client-side Camera Turn Resistance (steering sluggishness scales with momentum)
                double momentumVal = pData.getDouble("xebMechaMomentum");
                float maxTurn = (float) (16.0F / (1.0F + momentumVal * 4.0F)); // clamp delta rotation per tick
                
                float deltaYaw = net.minecraft.util.Mth.wrapDegrees(player.getYRot() - player.yRotO);
                if (Math.abs(deltaYaw) > maxTurn) {
                    player.setYRot(player.yRotO + Math.signum(deltaYaw) * maxTurn);
                }
                
                float deltaPitch = player.getXRot() - player.xRotO;
                float maxTurnPitch = (float) (12.0F / (1.0F + momentumVal * 4.0F));
                if (Math.abs(deltaPitch) > maxTurnPitch) {
                    player.setXRot(player.xRotO + Math.signum(deltaPitch) * maxTurnPitch);
                }

                // Spindash Charge Air Jump / Burst
                boolean isJumping = mc.options.keyJump.isDown();
                boolean wasJumping = pData.getBoolean("xebClientWasJumping");
                
                if (isJumping && !wasJumping && !player.onGround() && !player.isInWater() && !player.isPassenger()) {
                    int charges = pData.getInt("xebSpindashCharges");
                    if (charges > 0) {
                        // Apply client velocity burst instantly to avoid roundtrip network lag feeling
                        Vec3 mot = player.getDeltaMovement();
                        player.setDeltaMovement(mot.x * 1.25D, 0.58D, mot.z * 1.25D);
                        
                        // Send packet to server to consume charge and trigger visual particle burst
                        org.xeb.xeb.network.XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.ActuarKeyPacket(6, true));
                    }
                }
                pData.putBoolean("xebClientWasJumping", isJumping);
                
                // Client-side local target lock-on highlight (spinning red particle ring around targeted entity)
                int targetId = pData.getInt("xebSpindashTargetId");
                if (targetId != -1) {
                    Entity targetEntity = level.getEntity(targetId);
                    if (targetEntity instanceof LivingEntity && targetEntity.isAlive()) {
                        double radius = targetEntity.getBbWidth() * 0.7D;
                        double time = (double) mc.level.getGameTime() * 0.15D;
                        for (int i = 0; i < 4; i++) {
                            double angle = time + (i * Math.PI / 2.0D);
                            double px = targetEntity.getX() + Math.cos(angle) * radius;
                            double py = targetEntity.getY() + (targetEntity.getBbHeight() * 0.2D) + (i * 0.25D);
                            double pz = targetEntity.getZ() + Math.sin(angle) * radius;
                            mc.level.addParticle(
                                    new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.0F, 0.0F), 1.2F),
                                    px, py, pz, 0.0D, 0.0D, 0.0D
                            );
                        }
                    }
                }
            }
        }

        if (!level.isClientSide()) {
            int mechaA1CD = pData.getInt("xebMechaA1Cooldown");
            int mechaA2CD = pData.getInt("xebMechaA2Cooldown");
            if (mechaA1CD > 0) pData.putInt("xebMechaA1Cooldown", mechaA1CD - 1);
            if (mechaA2CD > 0) pData.putInt("xebMechaA2Cooldown", mechaA2CD - 1);
            
            int airBurst = pData.getInt("xebMechaAirBurstTicks");
            if (airBurst > 0) pData.putInt("xebMechaAirBurstTicks", airBurst - 1);
        }

        Vec3 currentMotion = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(currentMotion.x * currentMotion.x + currentMotion.z * currentMotion.z);
        
        // Detección de agua y hovering sobre la superficie
        net.minecraft.core.BlockPos pos = player.blockPosition();
        net.minecraft.world.level.material.FluidState fluid = level.getFluidState(pos);
        net.minecraft.world.level.material.FluidState fluidBelow = level.getFluidState(pos.below());
        boolean inOrAboveWater = fluid.is(net.minecraft.tags.FluidTags.WATER) || fluidBelow.is(net.minecraft.tags.FluidTags.WATER);
        
        boolean onGround = player.onGround() || inOrAboveWater;
        boolean jetActive = false;

        // Determinar isMovingForward y isMovingBackward de forma compatible en cliente y servidor
        boolean isMovingForward;
        boolean isMovingBackward;
        if (level.isClientSide()) {
            isMovingForward = player.zza > 0;
            isMovingBackward = player.zza < 0;
        } else {
            Vec3 look = player.getLookAngle();
            double dot = currentMotion.x * look.x + currentMotion.z * look.z;
            isMovingForward = player.zza > 0 || dot > 0.01D;
            isMovingBackward = player.zza < 0 || dot < -0.05D;
        }

        double momentum = pData.getDouble("xebMechaMomentum");
        boolean hasAirBurst = pData.getInt("xebMechaAirBurstTicks") > 0;

        // FÍSICAS DE MOMENTUM Y MOVIMIENTO (Ejecutar en ambos lados, cliente y servidor)
        if (isMovingBackward) {
            // Freno con S: decaer momentum al instante y frenar drásticamente
            momentum = Math.max(0.0D, momentum - 0.08D);
            player.setDeltaMovement(
                    currentMotion.x * 0.75D,
                    currentMotion.y,
                    currentMotion.z * 0.75D
            );
            player.hurtMarked = true;
        } else if (onGround) {
            if (isMovingForward) {
                // Caminar acumula momentum rápido (ahora hasta 2.0D)
                momentum = Math.min(2.0D, momentum + 0.025D);
                jetActive = true;

                // Aplicar aceleración gradual con resistencia al giro por momentum (drifting)
                Vec3 look = player.getLookAngle();
                Vec3 moveDir = new Vec3(look.x, 0.0D, look.z).normalize();
                double maxSkatingSpeed = 0.35D + (momentum * (MAX_SPEED - 0.35D));
                if (hasAirBurst) {
                    maxSkatingSpeed += 0.4D;
                }
                
                double steerFactor = 0.25D / (1.0D + momentum * 2.5D);
                if (hasAirBurst) steerFactor *= 2.0D;
                
                Vec3 targetVel = moveDir.scale(maxSkatingSpeed);
                double newX = net.minecraft.util.Mth.lerp(steerFactor, currentMotion.x, targetVel.x);
                double newZ = net.minecraft.util.Mth.lerp(steerFactor, currentMotion.z, targetVel.z);
                
                double speed = Math.sqrt(newX * newX + newZ * newZ);
                if (speed > maxSkatingSpeed) {
                    newX = (newX / speed) * maxSkatingSpeed;
                    newZ = (newZ / speed) * maxSkatingSpeed;
                }

                // Levitación leve sobre el suelo o agua (hovering leve)
                double newY = currentMotion.y;
                if (inOrAboveWater) {
                    double waterY = pos.getY() + (fluid.is(net.minecraft.tags.FluidTags.WATER) ? fluid.getHeight(level, pos) : 0.0D);
                    if (player.getY() < waterY + 0.15D) {
                        newY = Math.max(0.12D, currentMotion.y + 0.08D);
                    }
                } else if (currentMotion.y < 0.0D) {
                    newY = currentMotion.y + 0.05D;
                }

                player.setDeltaMovement(newX, newY, newZ);
                player.hurtMarked = true;
            } else {
                // No presionando W — decaer momentum más lento y deslizarse como en hielo (fricción suave)
                momentum = Math.max(0.0D, momentum - 0.005D);

                // Fricción de hielo: mantener el 95.5% de la velocidad
                player.setDeltaMovement(
                        currentMotion.x * 0.955D,
                        currentMotion.y,
                        currentMotion.z * 0.955D
                );
                player.hurtMarked = true;
            }
        } else {
            // EN EL AIRE: hover pasivo genera momentum SIN presionar W (ahora hasta 2.0D)
            momentum = Math.min(2.0D, momentum + 0.015D);
            jetActive = true;

            // Hover: reducir gravedad drásticamente
            if (currentMotion.y < 0.0D) {
                player.setDeltaMovement(
                        currentMotion.x,
                        currentMotion.y + 0.07D, // casi cancela gravedad
                        currentMotion.z
                );
            }

            // Mantener velocidad horizontal — NO aplicar fricción en el aire, aplicar resistencia al giro por momentum
            if (isMovingForward) {
                Vec3 look = player.getLookAngle();
                Vec3 moveDir = new Vec3(look.x, 0.0D, look.z).normalize();
                double maxAirSpeed = 0.4D + (momentum * 0.3D);
                if (hasAirBurst) {
                    maxAirSpeed += 0.4D;
                }
                
                double airSteerFactor = 0.15D / (1.0D + momentum * 2.5D);
                if (hasAirBurst) airSteerFactor *= 2.0D;
                
                Vec3 targetAirVel = moveDir.scale(maxAirSpeed);
                double newAirX = net.minecraft.util.Mth.lerp(airSteerFactor, currentMotion.x, targetAirVel.x);
                double newAirZ = net.minecraft.util.Mth.lerp(airSteerFactor, currentMotion.z, targetAirVel.z);
                
                player.setDeltaMovement(
                        newAirX,
                        player.getDeltaMovement().y,
                        newAirZ
                );
            }

            // Cap de velocidad horizontal en el aire (escalando con el cap a 2.0)
            double maxAirSpeed = 0.4D + (momentum * 0.3D);
            if (hasAirBurst) {
                maxAirSpeed += 0.4D;
            }
            double currentAirSpeed = Math.sqrt(
                    player.getDeltaMovement().x * player.getDeltaMovement().x +
                    player.getDeltaMovement().z * player.getDeltaMovement().z
            );
            if (currentAirSpeed > maxAirSpeed) {
                double scale = maxAirSpeed / currentAirSpeed;
                player.setDeltaMovement(
                        player.getDeltaMovement().x * scale,
                        player.getDeltaMovement().y,
                        player.getDeltaMovement().z * scale
                );
            }

            player.hurtMarked = true;
        }

        // Guardar variables actualizadas localmente en NBT de ambos lados
        pData.putDouble("xebMechaMomentum", momentum);
        pData.putBoolean("xebMechaJetActive", jetActive);
        pData.putDouble("xebMechaKineticSpeed", Math.sqrt(
                player.getDeltaMovement().x * player.getDeltaMovement().x +
                player.getDeltaMovement().z * player.getDeltaMovement().z
        ));

        boolean levitating = momentum > 0.7D;
        pData.putBoolean("xebMechaLevitating", levitating);

        if (!level.isClientSide()) {
            // Replenish Spindash charges when on the ground
            int sdState = pData.getInt("xebMechaSpindashState");
            if (player.onGround() && sdState == 0 && pData.getInt("xebSpindashSuspensionTicks") <= 0) {
                pData.putInt("xebSpindashCharges", 3);
                pData.putInt("xebSpindashRechargeTimer", 0);
            } else {
                int spindashCharges = pData.getInt("xebSpindashCharges");
                int spindashTimer = pData.getInt("xebSpindashRechargeTimer");
                if (spindashCharges < 3 && sdState == 0) {
                    spindashTimer++;
                    if (spindashTimer >= 120) {
                        spindashCharges++;
                        spindashTimer = 0;
                    }
                    pData.putInt("xebSpindashCharges", spindashCharges);
                    pData.putInt("xebSpindashRechargeTimer", spindashTimer);
                }
            }

            // Handle air suspension gravity lift
            int suspension = pData.getInt("xebSpindashSuspensionTicks");
            if (suspension > 0) {
                pData.putInt("xebSpindashSuspensionTicks", suspension - 1);
                player.setDeltaMovement(player.getDeltaMovement().x * 0.85D, Math.max(0.0D, player.getDeltaMovement().y * 0.5D), player.getDeltaMovement().z * 0.85D);
                player.hurtMarked = true;
            }

            // Pasiva: Auto-feed at max momentum
            if (momentum >= 1.0D && level.getGameTime() % 20 == 0) {
                player.getFoodData().eat(1, 0.5F);
            }

            // Pasiva 3 - Overcharge Core
            int overchargeTicks = pData.getInt("xebMechaOverchargeTicks");
            boolean isSkating = isMovingForward || !onGround;
            
            if (isSkating && momentum >= 0.9D) {
                overchargeTicks = Math.min(100, overchargeTicks + 1);
                if (overchargeTicks >= 60) {
                    pData.putBoolean("xebMechaOvercharged", true);
                }
            } else {
                // Decay overcharge ticks slowly
                overchargeTicks = Math.max(0, overchargeTicks - 1);
                if (overchargeTicks < 60) {
                    pData.putBoolean("xebMechaOvercharged", false);
                }
            }
            pData.putInt("xebMechaOverchargeTicks", overchargeTicks);
            
            // Momentum protection: if overcharged, momentum is held at least at 1.0D + scaled portion
            if (pData.getBoolean("xebMechaOvercharged")) {
                double protection = 1.0D + (overchargeTicks - 60) * 0.025D;
                momentum = Math.max(protection, momentum);
            }

            // Overcharge Aura Damage (Damages entities passed through)
            if (pData.getBoolean("xebMechaOvercharged") && horizontalSpeed > 0.1D) {
                double baseDamage = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                double auraDmg = baseDamage + 5.0D;
                
                AABB sweep = player.getBoundingBox().inflate(1.2D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != player && e.isAlive())) {
                    CompoundTag targetData = target.getPersistentData();
                    int cd = targetData.getInt("xebOverchargeHitCooldown");
                    if (cd <= 0) {
                        target.hurt(player.damageSources().playerAttack(player), (float) auraDmg);
                        targetData.putInt("xebOverchargeHitCooldown", 15); // 0.75s immunity
                        
                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(),
                                    8, 0.2D, 0.4D, 0.2D, 0.1D);
                        }
                    }
                }
            }

            // Cooldown ticks decrement for nearby entities
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12.0D), e -> e != player && e.isAlive())) {
                int cd = target.getPersistentData().getInt("xebOverchargeHitCooldown");
                if (cd > 0) {
                    target.getPersistentData().putInt("xebOverchargeHitCooldown", cd - 1);
                }
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

                    // Entity Sweep (Damage = base attack damage + momentum scale + 5 flat damage)
                    double baseDamage = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                    double momentumScale = momentum * 4.0D;
                    double dmg = baseDamage + momentumScale + 5.0D;
                    
                    AABB sweep = player.getBoundingBox().inflate(1.5D);
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != player && e.isAlive())) {
                        String nbtKey = "xebHitByJetDash_" + target.getUUID().toString();
                        if (!pData.getBoolean(nbtKey)) {
                            pData.putBoolean(nbtKey, true);
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
            sdState = pData.getInt("xebMechaSpindashState");
            if (sdState == 1) {
                // CHARGING
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                player.hurtMarked = true;
                int charge = pData.getInt("xebMechaSpindashCharge");
                if (charge < 40) {
                    pData.putInt("xebMechaSpindashCharge", charge + 1);
                }
                
                // Track and select lock-on target dynamically while charging
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16.0D),
                        e -> e != player && e.isAlive() && !(e instanceof Player p && p.isAlliedTo(player)));
                LivingEntity nearest = null;
                double minDistance = Double.MAX_VALUE;
                Vec3 playerLook = player.getLookAngle().normalize();
                for (LivingEntity t : targets) {
                    double dist = player.distanceToSqr(t);
                    if (dist < minDistance) {
                        Vec3 toTarget = t.position().subtract(player.position()).normalize();
                        if (playerLook.dot(toTarget) > 0.3D) { // within front cone
                            minDistance = dist;
                            nearest = t;
                        }
                    }
                }
                
                if (nearest != null) {
                    pData.putInt("xebSpindashTargetId", nearest.getId());
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, nearest.getX(), nearest.getY() + nearest.getBbHeight() / 2.0D, nearest.getZ(),
                                3, 0.4D, 0.4D, 0.4D, 0.05D);
                        serverLevel.sendParticles(ParticleTypes.FLAME, nearest.getX(), nearest.getY() + nearest.getBbHeight() / 2.0D, nearest.getZ(),
                                2, 0.4D, 0.4D, 0.4D, 0.0D);
                    }
                } else {
                    pData.putInt("xebSpindashTargetId", -1);
                }

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.5D, player.getZ(),
                            2, 0.3D, 0.3D, 0.3D, 0.1D);
                    serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5D, player.getZ(),
                            3, 0.3D, 0.3D, 0.3D, 0.05D);
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

                    double vx = pData.getDouble("xebSpindashVectorX");
                    double vy = pData.getDouble("xebSpindashVectorY");
                    double vz = pData.getDouble("xebSpindashVectorZ");
                    double charge = pData.getInt("xebMechaSpindashCharge");
                    double mult = 1.0D + charge * 0.08D;
                    player.setDeltaMovement(vx * mult * 1.5D, vy * mult * 1.5D, vz * mult * 1.5D);
                    player.hurtMarked = true;

                    if (hitCooldown <= 0) {
                        AABB sweep = player.getBoundingBox().inflate(1.5D);
                        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != player && e.isAlive());
                        
                        for (LivingEntity target : targets) {
                            String nbtKey = "xebHitBySpindash_" + target.getUUID().toString();
                            if (!pData.getBoolean(nbtKey)) {
                                pData.putBoolean(nbtKey, true);
                                double baseDmg = SPIN_BASE_DAMAGE;
                                double dmg = baseDmg * mult;
                                target.hurt(player.damageSources().playerAttack(player), (float) dmg);

                                int totalHits = pData.getInt("xebSpindashHitCount") + 1;
                                pData.putInt("xebSpindashHitCount", totalHits);

                                boolean isThird = pData.getInt("xebSpindashCharges") == 0;
                                if (isThird) {
                                    // 3rd use Spindash: pierces/traspasar through target, do not rebound!
                                    if (level instanceof ServerLevel serverLevel) {
                                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.5D, target.getZ(),
                                                1, 0.0D, 0.0D, 0.0D, 0.0D);
                                    }
                                    Vec3 look = player.getLookAngle().normalize();
                                    target.setDeltaMovement(target.getDeltaMovement().add(look.x * 1.5D, 0.4D, look.z * 1.5D));
                                    target.hurtMarked = true;
                                } else {
                                    // 1st or 2nd use Spindash: rebound off target and float/suspend in the air
                                    target.setDeltaMovement(player.getLookAngle().normalize().scale(1.2D).add(0, 0.3D, 0));
                                    target.hurtMarked = true;
                                    
                                    // Bounce player up and backward slightly
                                    Vec3 lookVec = player.getLookAngle().normalize();
                                    player.setDeltaMovement(-lookVec.x * 0.5D, 0.4D, -lookVec.z * 0.5D);
                                    player.hurtMarked = true;
                                    
                                    // Set air suspension ticks so player stays suspended
                                    pData.putInt("xebSpindashSuspensionTicks", 15);
                                    
                                    // Stop current spindash ticks
                                    pData.putInt("xebMechaSpindashTicks", 0);
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
