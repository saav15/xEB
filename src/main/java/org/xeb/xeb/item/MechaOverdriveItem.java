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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Mob;
import org.xeb.xeb.entity.MechaEggmanMissileEntity;
import org.xeb.xeb.entity.MechaLaserPelletEntity;
import org.xeb.xeb.item.capability.IMobWeaponCapability;
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

public class MechaOverdriveItem extends Item implements GeoItem, IMobWeaponCapability {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final double BASE_DAMAGE = 6.0D;
    public static final java.util.UUID STEP_HEIGHT_UUID = java.util.UUID.fromString("6a5bc382-7d2d-4f1b-8c8f-fbfa62e840d5");

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, net.minecraft.core.BlockPos pos, Player player) {
        return true;
    }

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
        return org.xeb.xeb.enchantment.ModEnchantments.isModEnchantment(enchantment) || super.canApplyAtEnchantingTable(stack, enchantment);
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
        CompoundTag pData = player.getPersistentData();
        if (pData.getInt("xebMechaOverheatedTicks") > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.5F);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }

        pData.putBoolean("xebMechaShotgunFiring", true);
        pData.putInt("xebMechaShotgunHoldTicks", 0);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide() && entity instanceof Player player) {
            CompoundTag pData = player.getPersistentData();

            int overheated = pData.getInt("xebMechaOverheatedTicks");
            if (overheated > 0) {
                player.stopUsingItem();
                return;
            }

            int holdTicks = pData.getInt("xebMechaShotgunHoldTicks") + 1;
            pData.putInt("xebMechaShotgunHoldTicks", holdTicks);

            // Overheat after 15 seconds (300 ticks) of continuous hold
            if (holdTicks >= 300) {
                pData.putInt("xebMechaOverheatedTicks", 80); // 4 seconds cooling lockout
                pData.putBoolean("xebMechaShotgunFiring", false);
                pData.putInt("xebMechaShotgunHoldTicks", 0);
                player.stopUsingItem();

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.2F, 0.8F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.5F);

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(),
                            20, 0.4D, 0.4D, 0.4D, 0.05D);
                    serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(),
                            10, 0.3D, 0.3D, 0.3D, 0.05D);
                }

                if (player instanceof ServerPlayer sp) syncToClient(sp);
                return;
            }

            // Firing cadence ramps up to +300% faster (from 10 ticks down to 3 ticks) over 6s (120 ticks)
            int interval = Math.max(3, 10 - (int) (7.0D * Math.min(1.0D, holdTicks / 120.0D)));

            if (holdTicks % interval == 0) {
                Vec3 lookDir = player.getLookAngle();
                Vec3 muzzlePos = player.getEyePosition(1.0F).add(lookDir.scale(0.5D));

                // Laser Scatter Shotgun: Fire 4 pellets per burst with small spread
                for (int i = 0; i < 4; i++) {
                    double spread = 0.12D;
                    Vec3 velocity = lookDir.add(
                            (player.getRandom().nextDouble() - 0.5D) * spread,
                            (player.getRandom().nextDouble() - 0.5D) * spread,
                            (player.getRandom().nextDouble() - 0.5D) * spread
                    ).normalize().scale(2.2D);

                    MechaLaserPelletEntity pellet = new MechaLaserPelletEntity(level, player);
                    pellet.moveTo(muzzlePos.x, muzzlePos.y, muzzlePos.z);
                    pellet.setDeltaMovement(velocity);
                    level.addFreshEntity(pellet);
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8F, 1.8F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5F, 2.0F);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        CompoundTag pData = entity.getPersistentData();
        pData.putBoolean("xebMechaShotgunFiring", false);
        pData.putInt("xebMechaShotgunHoldTicks", 0);
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

        CompoundTag pData = player.getPersistentData();
        boolean holdsMecha = player.getMainHandItem().getItem() == this || player.getOffhandItem().getItem() == this;
        boolean isDashing = pData.getBoolean("xebMechaOverdriveDashing") || pData.getBoolean("xebMechaTargetedDashing");
        int sdState = pData.getInt("xebMechaSpindashState");
        boolean isSliding = player.isCrouching() && (player.onGround() || level.getFluidState(player.blockPosition()).is(net.minecraft.tags.FluidTags.WATER));

        // ── Step Height Modifier Management (Only active during high-speed actions) ──
        boolean needsStepHeight = holdsMecha && (isDashing || sdState > 0 || isSliding);
        var attr = player.getAttribute(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (attr != null && !needsStepHeight && attr.getModifier(STEP_HEIGHT_UUID) != null) {
            attr.removeModifier(STEP_HEIGHT_UUID);
        }

        if (!holdsMecha) return;

        // ── Client-side Visuals & FX ──────────────────────────────────────────
        int overheated = pData.getInt("xebMechaOverheatedTicks");
        if (level.isClientSide()) {
            if (overheated > 0 && level.random.nextFloat() < 0.6F) {
                level.addParticle(ParticleTypes.LARGE_SMOKE,
                        player.getX() + (level.random.nextDouble() - 0.5D) * 0.5D,
                        player.getY() + 1.0D + level.random.nextDouble() * 0.4D,
                        player.getZ() + (level.random.nextDouble() - 0.5D) * 0.5D,
                        0.0D, 0.04D, 0.0D);
                if (level.random.nextFloat() < 0.2F) {
                    level.addParticle(ParticleTypes.FLAME,
                            player.getX() + (level.random.nextDouble() - 0.5D) * 0.4D,
                            player.getY() + 1.2D,
                            player.getZ() + (level.random.nextDouble() - 0.5D) * 0.4D,
                            0.0D, 0.02D, 0.0D);
                }
            }
            return; // All authoritative gameplay logic below runs ONLY on the Server!
        }

        // ── Server-side Game Logic ───────────────────────────────────────────

        // Cooldown timer decrements
        int cd1 = pData.getInt("xebMechaA1Cooldown");
        if (cd1 > 0) pData.putInt("xebMechaA1Cooldown", cd1 - 1);

        int cd2 = pData.getInt("xebMechaA2Cooldown");
        if (cd2 > 0) pData.putInt("xebMechaA2Cooldown", cd2 - 1);

        if (overheated > 0) pData.putInt("xebMechaOverheatedTicks", overheated - 1);

        // ── 1. Drowning Penalty & Water Surface Levitation ───────────────────
        if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) && player.getAirSupply() > 0) {
            player.setAirSupply(Math.max(0, player.getAirSupply() - 4));
        }

        BlockPos pos = player.blockPosition();
        boolean inWater = level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                || level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER);

        int momentumNum = pData.getInt("xebMechaMomentumNum");

        if (player.isCrouching() && inWater) {
            // Crouching allows passing through water / sinking smoothly
            Vec3 m = player.getDeltaMovement();
            player.setDeltaMovement(m.x, Math.min(m.y, -0.25D), m.z);
            player.hurtMarked = true;
        } else if (inWater && momentumNum > 0 && !player.isCrouching()) {
            // Levitating / Running ON TOP of water surface with momentum
            Vec3 m = player.getDeltaMovement();
            double lift = level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) ? 0.22D : Math.max(0.0D, m.y);
            player.setDeltaMovement(m.x * 1.05D, lift, m.z * 1.05D);
            player.hurtMarked = true;
        }

        // ── 2. Crosshair Lock-on Target Scanning with Sticky Lock Hysteresis ──
        Vec3 look = player.getLookAngle().normalize();
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24.0D),
                e -> e != player && e.isAlive() && !(e instanceof Player p && p.isAlliedTo(player)) && player.hasLineOfSight(e));
        LivingEntity bestTarget = null;
        double bestDot = 0.4D; // Cone angle (~66 degrees)
        for (LivingEntity t : potentialTargets) {
            Vec3 toTarget = t.getBoundingBox().getCenter().subtract(player.getEyePosition(1.0F)).normalize();
            double dot = look.dot(toTarget);
            if (dot > bestDot) {
                bestDot = dot;
                bestTarget = t;
            }
        }
        if (bestTarget != null) {
            pData.putInt("xebSpindashTargetId", bestTarget.getId());
        } else {
            int currentTargetId = pData.getInt("xebSpindashTargetId");
            if (currentTargetId != -1) {
                Entity curr = level.getEntity(currentTargetId);
                if (curr == null || !curr.isAlive() || player.distanceToSqr(curr) > 30 * 30 || !player.hasLineOfSight(curr)) {
                    pData.putInt("xebSpindashTargetId", -1);
                } else {
                    // Check if existing locked target is still within reasonable cone (dot > 0.1D) for sticky lock
                    Vec3 toCurr = curr.getBoundingBox().getCenter().subtract(player.getEyePosition(1.0F)).normalize();
                    if (look.dot(toCurr) <= 0.1D) {
                        pData.putInt("xebSpindashTargetId", -1);
                    }
                }
            }
        }

        // ── 3. O.Clock Momentum System (0..300 Max, 5 Charges of 60) ───────────
        isDashing = pData.getBoolean("xebMechaOverdriveDashing") || pData.getBoolean("xebMechaTargetedDashing");

        if (player.isCrouching()) {
            // Charging momentum while crouching (reaches 300 max in ~7.5s)
            int oldBars = momentumNum / 60;
            momentumNum = Math.min(300, momentumNum + 2);
            int newBars = momentumNum / 60;
            if (newBars > oldBars && newBars <= 5) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F + (newBars * 0.15F));
            }

            // Smooth Ground Sliding Impulse while crouching (bypasses sneak edge limit)
            if (player.onGround() || inWater) {
                Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
                double slideSpeed = 0.42D + (momentumNum / 300.0D * 0.25D);
                player.setDeltaMovement(forward.x * slideSpeed, player.getDeltaMovement().y, forward.z * slideSpeed);
                player.hurtMarked = true;

                if (level.isClientSide() && level.random.nextFloat() < 0.4F) {
                    level.addParticle(ParticleTypes.POOF, player.getX(), player.getY() + 0.1D, player.getZ(),
                            0.0D, 0.02D, 0.0D);
                }
            }
        } else if (pData.getInt("xebMechaSpindashState") != 1 && pData.getInt("xebSpindashSuspensionTicks") <= 0) {
            // Standing / Walking / Running: Momentum decays over time!
            // Forcefully impulsed standing decays 50% faster (-3 per tick vs -2 every 2 ticks)
            if (isDashing) {
                momentumNum = Math.max(0, momentumNum - 3);
            } else if (level.getGameTime() % 2 == 0) {
                momentumNum = Math.max(0, momentumNum - 2);
            }

            // Movement speed bonus when standing with momentum
            if (momentumNum > 0 && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.001D) {
                double speedBoost = 1.0D + (momentumNum / 300.0D) * 0.25D;
                Vec3 m = player.getDeltaMovement();
                player.setDeltaMovement(m.x * speedBoost, m.y, m.z * speedBoost);
            }
        }

        int bars = momentumNum / 60;
        pData.putInt("xebMechaMomentumNum", momentumNum);
        pData.putInt("xebMechaOClockBars", bars);
        pData.putDouble("xebMechaMomentum", momentumNum / 300.0D); // 0.0 to 1.0 for client rendering

        // Reset air combo when touching ground
        if (player.onGround()) {
            pData.putInt("xebMechaAirCombo", 0);
        }

        // ── 3. Passives & Air Levitating at Max O.Clock (300 Momentum) ───────────
        if (momentumNum >= 300) {
            // Air Hover Levitation: floating in mid-air when not on ground and not crouching
            if (!player.onGround() && !player.isCrouching()) {
                Vec3 m = player.getDeltaMovement();
                player.setDeltaMovement(m.x, Math.max(-0.02D, m.y * 0.15D), m.z);
                player.hurtMarked = true;
                pData.putBoolean("xebMechaLevitating", true);

                if (level instanceof ServerLevel serverLevel && level.getGameTime() % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() - 0.2D, player.getZ(),
                            4, 0.2D, 0.1D, 0.2D, 0.02D);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() - 0.2D, player.getZ(),
                            2, 0.2D, 0.1D, 0.2D, 0.05D);
                }
            } else {
                pData.putBoolean("xebMechaLevitating", false);
            }

            // Crush Passive: Stepping on enemies deals base attack damage
            int crushCd = pData.getInt("xebMechaCrushCooldown");
            if (crushCd > 0) {
                pData.putInt("xebMechaCrushCooldown", crushCd - 1);
            } else {
                AABB feetBox = player.getBoundingBox().inflate(0.5D, 0.2D, 0.5D);
                double baseDmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, feetBox, e -> e != player && e.isAlive())) {
                    target.hurt(player.damageSources().playerAttack(player), (float) baseDmg);
                    pData.putInt("xebMechaCrushCooldown", 10);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.2D, target.getZ(),
                                6, 0.2D, 0.2D, 0.2D, 0.1D);
                    }
                }
            }

            // Burn I Aura in 1.5 blocks radius
            AABB burnArea = player.getBoundingBox().inflate(1.5D);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, burnArea, e -> e != player && e.isAlive())) {
                target.setSecondsOnFire(3);
            }
        } else {
            pData.putBoolean("xebMechaLevitating", false);
        }

        // ── 4. Activa 1 (Mecha Drill Punch) Aura Ticks ─────────────────────────
        int drillAuraTicks = pData.getInt("xebMechaDrillAuraTicks");
        if (drillAuraTicks > 0) {
            pData.putInt("xebMechaDrillAuraTicks", drillAuraTicks - 1);
            // Every 30 ticks (1.5s), deal 13 constant damage in 1.5 blocks radius
            if (drillAuraTicks % 30 == 0) {
                AABB auraBox = player.getBoundingBox().inflate(1.5D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, auraBox, e -> e != player && e.isAlive())) {
                    target.hurt(player.damageSources().playerAttack(player), 13.0F);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 1.5F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5D, player.getZ(),
                            3, 0.5D, 0.5D, 0.5D, 0.1D);
                }
            }
        }

        // ── 5. Stationary Spindash Charge & Targeted Homing Spindash Air Combo ──
        sdState = pData.getInt("xebMechaSpindashState");
        if (sdState == 1) { // Stationary Spindash Ball Charge (0 bars mode)
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            player.hurtMarked = true;
            int chargeTicks = pData.getInt("xebMechaSpindashCharge") + 1;
            pData.putInt("xebMechaSpindashCharge", chargeTicks);
            // Every 20 ticks (1s) = +60 momentum (+1 bar) up to 300 momentum
            if (chargeTicks % 20 == 0 && momentumNum < 300) {
                momentumNum = Math.min(300, momentumNum + 60);
                pData.putInt("xebMechaMomentumNum", momentumNum);
                pData.putInt("xebMechaOClockBars", momentumNum / 60);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.5F);
            }
        }

        // Targeted Spindash Homing & Air Combo Collision Check
        boolean targetedDashing = pData.getBoolean("xebMechaTargetedDashing");
        int targetId = pData.getInt("xebSpindashTargetId");
        if (targetedDashing) {
            int dashTicks = pData.getInt("xebMechaTargetedDashTicks") + 1;
            pData.putInt("xebMechaTargetedDashTicks", dashTicks);

            Entity targetEntity = targetId != -1 ? level.getEntity(targetId) : null;
            if (targetEntity instanceof LivingEntity target && target.isAlive() && dashTicks < 16) {
                Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition(1.0F));
                double dist = toTarget.length();
                Vec3 dir = toTarget.normalize();

                // Smooth controllable homing speed towards target
                player.setDeltaMovement(dir.scale(1.8D));
                player.hurtMarked = true;

                if (level instanceof ServerLevel serverLevel && level.getGameTime() % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5D, player.getZ(),
                            5, 0.3D, 0.3D, 0.3D, 0.05D);
                    serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.5D, player.getZ(),
                            3, 0.2D, 0.2D, 0.2D, 0.1D);
                }

                // Check collision hit box
                AABB playerBox = player.getBoundingBox().inflate(1.2D);
                if (playerBox.intersects(target.getBoundingBox()) || dist < 2.0D) {
                    // Deal damage
                    float baseDmg = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    float finalDmg = baseDmg + 10.0F + (momentumNum / 300.0F) * 15.0F;
                    target.hurt(player.damageSources().playerAttack(player), finalDmg);

                    // End dash phase
                    pData.putBoolean("xebMechaTargetedDashing", false);
                    pData.putInt("xebMechaTargetedDashTicks", 0);

                    // Air Recoil / Bounce back up into air
                    Vec3 bounce = new Vec3(-dir.x * 0.4D, 0.75D, -dir.z * 0.4D);
                    player.setDeltaMovement(bounce);
                    player.hurtMarked = true;
                    pData.putInt("xebSpindashSuspensionTicks", 35); // hover suspension for next combo

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.6F);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.2F, 1.2F);

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0D, target.getZ(),
                                4, 0.4D, 0.4D, 0.4D, 0.1D);
                    }

                    // Air Combo counter
                    int airCombo = pData.getInt("xebMechaAirCombo") + 1;
                    if (airCombo >= 3) {
                        // 3rd hit finisher: restore 2 full O.Clock charges (+120 momentum) & apply Charred Burn 1 for 15s
                        int restoredMomentum = Math.min(300, momentumNum + 120);
                        pData.putInt("xebMechaMomentumNum", restoredMomentum);
                        pData.putInt("xebMechaOClockBars", restoredMomentum / 60);

                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                org.xeb.xeb.effect.ModEffects.CHARRED_BURN.get(), 300, 0));

                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.5F);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.2F);

                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + 1.0D, target.getZ(),
                                    15, 0.5D, 0.5D, 0.5D, 0.2D);
                            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0D, target.getZ(),
                                    20, 0.6D, 0.6D, 0.6D, 0.1D);
                        }

                        pData.putInt("xebMechaAirCombo", 0);
                    } else {
                        pData.putInt("xebMechaAirCombo", airCombo);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.4F + (airCombo * 0.2F));
                    }
                }
            } else {
                // Abandon search safety: target unreachable or timeout -> launch Eggman missile and stop spindash homing
                pData.putBoolean("xebMechaTargetedDashing", false);
                pData.putInt("xebMechaTargetedDashTicks", 0);

                org.xeb.xeb.entity.MechaEggmanMissileEntity missile = new org.xeb.xeb.entity.MechaEggmanMissileEntity(level, player, false);
                missile.moveTo(player.getX(), player.getY() + 1.0D, player.getZ());
                missile.setDeltaMovement(look.scale(1.5D));
                level.addFreshEntity(missile);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.PLAYERS, 1.2F, 1.0F);
            }
        }

        int suspension = pData.getInt("xebSpindashSuspensionTicks");
        if (suspension > 0) {
            pData.putInt("xebSpindashSuspensionTicks", suspension - 1);
            player.setDeltaMovement(player.getDeltaMovement().x * 0.85D, Math.max(0.0D, player.getDeltaMovement().y * 0.5D), player.getDeltaMovement().z * 0.85D);
            player.hurtMarked = true;
        }

        // Sync to client
        if (player instanceof ServerPlayer serverPlayer && level.getGameTime() % 2 == 0) {
            syncToClient(serverPlayer);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        CompoundTag pData = player.getPersistentData();
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new MechaSyncPacket(
                        player.getId(),
                        pData.getBoolean("xebMechaJetActive"),
                        pData.getBoolean("xebMechaShotgunFiring"),
                        pData.getInt("xebMechaSpindashState"),
                        pData.getBoolean("xebMechaOverdriveDashing"),
                        pData.getInt("xebMechaA1Cooldown"),
                        pData.getInt("xebMechaA2Cooldown"),
                        pData.getDouble("xebMechaKineticSpeed"),
                        pData.getDouble("xebMechaMomentum"),
                        pData.getInt("xebMechaMomentumNum") >= 300,
                        pData.getBoolean("xebMechaLevitating"),
                        pData.getInt("xebMechaSpindashCharge"),
                        pData.getInt("xebMechaMomentumNum"),
                        pData.getInt("xebSpindashTargetId"),
                        pData.getInt("xebMechaAirCombo"),
                        pData.getBoolean("xebMechaTargetedDashing"),
                        pData.getInt("xebMechaOverheatedTicks")
                )
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IMobWeaponCapability IMPLEMENTATION (MOB AI LOGIC FOR MECHA OVERDRIVE CORE)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public double getPreferredAttackDistance(Mob mob) {
        return 28.0D;
    }

    @Override
    public void tickMobAI(Mob mob, LivingEntity target, Level level, long gameTime, double distSq) {
        if (level.isClientSide()) return;

        CompoundTag tag = mob.getPersistentData();

        // 1. Overheat Lockout Check (4 seconds = 80 ticks cooldown)
        int overheated = tag.getInt("xebMechaOverheatedTicks");
        if (overheated > 0) {
            tag.putInt("xebMechaOverheatedTicks", overheated - 1);
            if (level.random.nextFloat() < 0.6F && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 1.2D, mob.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.02D);
                if (level.random.nextFloat() < 0.3F) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + 1.2D, mob.getZ(), 1, 0.1D, 0.1D, 0.1D, 0.02D);
                }
            }
            // Cannot use abilities or weapons while overheated
            return;
        }

        // 2. Momentum / O.Clock System for Mobs (0 to 300 points, 5 bars of 60)
        int momentumNum = tag.getInt("xebMechaMomentumNum");
        if (momentumNum < 300) {
            momentumNum = Math.min(300, momentumNum + 1);
            tag.putInt("xebMechaMomentumNum", momentumNum);
            tag.putInt("xebMechaOClockBars", momentumNum / 60);
        }

        // Max Momentum Passives for Mob
        if (momentumNum >= 300) {
            // Speed boost
            Vec3 vel = mob.getDeltaMovement();
            if (mob.onGround() && vel.horizontalDistanceSqr() > 0.001D) {
                mob.setDeltaMovement(vel.x * 1.15D, vel.y, vel.z * 1.15D);
            }
            // Soul flame particles
            if (gameTime % 4 == 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY() + 0.5D, mob.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            // Burn Aura in 1.5 blocks radius
            AABB burnArea = mob.getBoundingBox().inflate(1.5D);
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, burnArea, e -> e != mob && e.isAlive() && !e.isAlliedTo(mob))) {
                nearby.setSecondsOnFire(3);
            }
        }

        // 3. Activa 1 (Mecha Drill Punch) Aura ticking for mob
        int drillTicks = tag.getInt("xebMechaDrillAuraTicks");
        if (drillTicks > 0) {
            tag.putInt("xebMechaDrillAuraTicks", drillTicks - 1);
            if (drillTicks % 30 == 0) {
                AABB auraBox = mob.getBoundingBox().inflate(1.5D);
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, auraBox, e -> e != mob && e.isAlive() && !e.isAlliedTo(mob))) {
                    nearby.hurt(level.damageSources().mobAttack(mob), 13.0F);
                }
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8F, 1.5F);
            }
        }

        // Cooldown timers
        long cdSlam     = tag.getLong("xebMobCD_MechaSlam");
        long cdDrill    = tag.getLong("xebMobCD_MechaDrill");
        long cdMissile  = tag.getLong("xebMobCD_EggmanMissiles");
        long cdSpindash = tag.getLong("xebMobCD_Spindash");

        // ── A. Close Range (<= 4 blocks): Kinetic Impact Explosion (5s CD) ──
        if (distSq <= 16.0D && gameTime - cdSlam >= 100) {
            tag.putLong("xebMobCD_MechaSlam", gameTime);
            org.xeb.xeb.client.vfx.XebExplosions.spawnExplosion(target.position(), 3.0F, 5.0F, 0, 229, 255, 20);
            target.hurt(level.damageSources().mobAttack(mob), 16.0F);
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2F, 1.2F);
            return;
        }

        // ── B. Activa 1: Mecha Drill Punch (Impulse Charge) (4 - 16 blocks, 10s CD) ──
        if (distSq >= 16.0D && distSq <= 256.0D && gameTime - cdDrill >= 200) {
            tag.putLong("xebMobCD_MechaDrill", gameTime);

            // Grant +60 momentum (+1 bar)
            momentumNum = Math.min(300, momentumNum + 60);
            tag.putInt("xebMechaMomentumNum", momentumNum);
            tag.putInt("xebMechaOClockBars", momentumNum / 60);
            tag.putInt("xebMechaDrillAuraTicks", 120); // 6 seconds aura

            Vec3 dir = target.position().subtract(mob.position()).normalize();
            mob.setDeltaMovement(dir.x * 1.6D, 0.3D, dir.z * 1.6D);
            mob.hurtMarked = true;

            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.HOSTILE, 1.2F, 1.0F);
            return;
        }

        // ── C. Activa 2 (Spindash Teledirigido & Combo Aéreo) (4 - 16 blocks, 6s CD) ──
        if (distSq >= 16.0D && distSq <= 256.0D && gameTime - cdSpindash >= 120 && momentumNum >= 60) {
            tag.putLong("xebMobCD_Spindash", gameTime);

            // Spend 60 momentum
            momentumNum = Math.max(0, momentumNum - 60);
            tag.putInt("xebMechaMomentumNum", momentumNum);
            tag.putInt("xebMechaOClockBars", momentumNum / 60);

            Vec3 dir = target.getEyePosition().subtract(mob.getEyePosition()).normalize();
            mob.setDeltaMovement(dir.x * 1.8D, 0.35D, dir.z * 1.8D);
            mob.hurtMarked = true;

            // 50% chance to launch Eggman Missile during launch
            if (level.random.nextFloat() < 0.5F) {
                MechaEggmanMissileEntity missile = new MechaEggmanMissileEntity(level, mob, false);
                missile.moveTo(mob.getX(), mob.getEyeY(), mob.getZ());
                missile.setDeltaMovement(dir.scale(1.5D));
                level.addFreshEntity(missile);
            }

            // Deal Spindash impact damage
            float finalDmg = 12.0F + (momentumNum / 300.0F) * 10.0F;
            target.hurt(level.damageSources().mobAttack(mob), finalDmg);

            // Air combo & Charred Burn check
            int airCombo = tag.getInt("xebMobAirCombo") + 1;
            if (airCombo >= 3) {
                tag.putInt("xebMobAirCombo", 0);
                momentumNum = Math.min(300, momentumNum + 120); // restore 2 bars
                tag.putInt("xebMechaMomentumNum", momentumNum);
                tag.putInt("xebMechaOClockBars", momentumNum / 60);

                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        org.xeb.xeb.effect.ModEffects.CHARRED_BURN.get(), 300, 0));
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.2F);
            } else {
                tag.putInt("xebMobAirCombo", airCombo);
            }

            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 1.8F);
            return;
        }

        // ── D. Activa 2 (Larga Distancia: Misiles Teledirigidos Eggman) (12 - 35 blocks, 8s CD) ──
        if (distSq >= 144.0D && distSq <= 1225.0D && gameTime - cdMissile >= 160) {
            tag.putLong("xebMobCD_EggmanMissiles", gameTime);

            Vec3 look = target.position().subtract(mob.position()).normalize();
            Vec3 rightOffset = new Vec3(-look.z, 0, look.x).normalize().scale(0.8D);

            MechaEggmanMissileEntity leftMissile = new MechaEggmanMissileEntity(level, mob, true);
            leftMissile.moveTo(mob.getX() - rightOffset.x, mob.getEyeY(), mob.getZ() - rightOffset.z);
            leftMissile.setDeltaMovement(look.scale(1.5D));
            level.addFreshEntity(leftMissile);

            MechaEggmanMissileEntity rightMissile = new MechaEggmanMissileEntity(level, mob, false);
            rightMissile.moveTo(mob.getX() + rightOffset.x, mob.getEyeY(), mob.getZ() + rightOffset.z);
            rightMissile.setDeltaMovement(look.scale(1.5D));
            level.addFreshEntity(rightMissile);

            momentumNum = Math.min(300, momentumNum + 60);
            tag.putInt("xebMechaMomentumNum", momentumNum);
            tag.putInt("xebMechaOClockBars", momentumNum / 60);

            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.HOSTILE, 1.2F, 1.0F);
            return;
        }

        // ── E. Ranged Minigun / Laser Shotgun Burst (6 - 25 blocks) ──
        if (distSq >= 36.0D && distSq <= 625.0D) {
            int shotgunHoldTicks = tag.getInt("xebMobMechaShotgunHoldTicks") + 1;
            tag.putInt("xebMobMechaShotgunHoldTicks", shotgunHoldTicks);

            // OVERHEAT CHECK FOR MOBS AT EXACTLY 5 SECONDS (100 TICKS)!
            if (shotgunHoldTicks >= 100) {
                tag.putInt("xebMechaOverheatedTicks", 80); // 4 seconds cooling lockout
                tag.putInt("xebMobMechaShotgunHoldTicks", 0);

                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.2F, 0.8F);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 0.5F);

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 1.2D, mob.getZ(), 20, 0.4D, 0.4D, 0.4D, 0.05D);
                    serverLevel.sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + 1.2D, mob.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.05D);
                }
                return;
            }

            // Cadence ramp-up: fires every 4 to 8 ticks
            int cadence = Math.max(4, 8 - (shotgunHoldTicks / 20));
            if (gameTime % cadence == 0) {
                Vec3 eyePos = mob.getEyePosition(1.0F);
                Vec3 look = target.getEyePosition().subtract(eyePos).normalize();

                for (int i = 0; i < 4; i++) {
                    double spread = 0.12D;
                    Vec3 velocity = look.add(
                            (level.random.nextDouble() - 0.5D) * spread,
                            (level.random.nextDouble() - 0.5D) * spread,
                            (level.random.nextDouble() - 0.5D) * spread
                    ).normalize().scale(2.2D);

                    MechaLaserPelletEntity pellet = new MechaLaserPelletEntity(level, mob);
                    pellet.moveTo(eyePos.x, eyePos.y - 0.1D, eyePos.z);
                    pellet.setDeltaMovement(velocity);
                    level.addFreshEntity(pellet);
                }

                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 0.8F, 1.8F);
            }
        } else {
            // Decay hold ticks when target not in ranged minigun zone
            int shotgunHoldTicks = tag.getInt("xebMobMechaShotgunHoldTicks");
            if (shotgunHoldTicks > 0) {
                tag.putInt("xebMobMechaShotgunHoldTicks", Math.max(0, shotgunHoldTicks - 2));
            }
        }
    }
}
