package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.xeb.xeb.entity.FlowerPelletEntity;
import org.xeb.xeb.entity.FlowerProjectileEntity;
import org.xeb.xeb.network.GoldenFlowerSyncPacket;
import org.xeb.xeb.network.XEBNetwork;

import java.util.List;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

public class GoldenFlowerItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private long lastCheckTime = 0;
    private long faceAnimEndTime = 0;
    private String currentFaceAnim = null;

    public GoldenFlowerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 1. Idle Controller (Always active and looping)
        controllers.add(new AnimationController<>(this, "idle_controller", 5, event -> {
            return event.setAndContinue(RawAnimation.begin().thenLoop("Idle"));
        }));

        // 2. Face Controller (5s duration every 1m with 50% chance)
        controllers.add(new AnimationController<>(this, "face_controller", 5, event -> {
            long now = System.currentTimeMillis();
            if (now - lastCheckTime >= 60000) {
                lastCheckTime = now;
                if (Math.random() < 0.5) {
                    int choice = (int) (Math.random() * 3);
                    if (choice == 0) currentFaceAnim = "Wink";
                    else if (choice == 1) currentFaceAnim = "Scary";
                    else currentFaceAnim = "Dots";
                    faceAnimEndTime = now + 5000;
                } else {
                    currentFaceAnim = null;
                }
            }

            if (currentFaceAnim != null && now < faceAnimEndTime) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop(currentFaceAnim));
                return software.bernie.geckolib.core.object.PlayState.CONTINUE;
            } else {
                return software.bernie.geckolib.core.object.PlayState.STOP;
            }
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
        return enchantment == org.xeb.xeb.enchantment.ModEnchantments.MEDALLERO.get() || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private org.xeb.xeb.client.renderer.GoldenFlowerGeoRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new org.xeb.xeb.client.renderer.GoldenFlowerGeoRenderer();
                }
                return this.renderer;
            }
        });
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int charges = player.getPersistentData().getInt("xebGoldenFlowerCharges");
        if (!player.getPersistentData().contains("xebGoldenFlowerCharges")) {
            charges = 6;
            player.getPersistentData().putInt("xebGoldenFlowerCharges", 6);
        }

        if (charges > 0) {
            player.startUsingItem(hand);
            player.getPersistentData().putInt("xebGoldenFlowerLoadedCount", 0);
            return InteractionResultHolder.consume(stack);
        } else {
            return InteractionResultHolder.fail(stack);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player && !level.isClientSide()) {
            int ticksUsed = 72000 - count;
            if (ticksUsed > 0 && ticksUsed % 10 == 0) {
                int charges = player.getPersistentData().getInt("xebGoldenFlowerCharges");
                int loaded = player.getPersistentData().getInt("xebGoldenFlowerLoadedCount");

                if (loaded < charges && loaded < 6) {
                    loaded++;
                    player.getPersistentData().putInt("xebGoldenFlowerLoadedCount", loaded);

                    // Sync to client and tracking clients
                    if (player instanceof ServerPlayer serverPlayer) {
                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new GoldenFlowerSyncPacket(
                                        player.getId(),
                                        charges,
                                        player.getPersistentData().getInt("xebGoldenFlowerRechargeTimer"),
                                        loaded,
                                        player.getPersistentData().getInt("xebJaronaCharges"),
                                        player.getPersistentData().getInt("xebJaronaRechargeTimer"),
                                        player.getPersistentData().getInt("xebGoldenFlowerDanceCooldown")
                                ));
                    }

                    // Play chime sound with increasing pitch
                    level.playSound(null, player, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.0F + (loaded * 0.15F));
                }
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player && !level.isClientSide()) {
            int loaded = player.getPersistentData().getInt("xebGoldenFlowerLoadedCount");
            int charges = player.getPersistentData().getInt("xebGoldenFlowerCharges");

            if (loaded > 0) {
                // Consume charges
                charges = Math.max(0, charges - loaded);
                player.getPersistentData().putInt("xebGoldenFlowerCharges", charges);

                // Find a target ahead
                LivingEntity target = null;
                Vec3 eyePos = player.getEyePosition(1.0F);
                Vec3 look = player.getLookAngle();
                Vec3 beamEnd = eyePos.add(look.scale(25.0D));
                net.minecraft.world.phys.BlockHitResult blockHit = player.level().clip(new net.minecraft.world.level.ClipContext(
                        eyePos, beamEnd, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player
                ));
                Vec3 effectiveEnd = blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? blockHit.getLocation() : beamEnd;
                AABB sweepBox = new AABB(eyePos, effectiveEnd).inflate(2.0D);
                net.minecraft.world.phys.EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                        player, eyePos, effectiveEnd, sweepBox,
                        (e) -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator() && e.isPickable(),
                        625.0D
                );
                if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
                    target = living;
                }

                // If no direct target, pick closest within 15 blocks
                if (target == null) {
                    List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(15.0D),
                            e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator() && e.isPickable());
                    if (!list.isEmpty()) {
                        double closestDist = Double.MAX_VALUE;
                        for (LivingEntity e : list) {
                            double dist = player.distanceToSqr(e);
                            if (dist < closestDist) {
                                closestDist = dist;
                                target = e;
                            }
                        }
                    }
                }

                // Semicircle behind player
                Vec3 right = new Vec3(-look.z, 0.0D, look.x).normalize();
                for (int i = 0; i < loaded; i++) {
                    FlowerProjectileEntity proj = new FlowerProjectileEntity(level, player, i);
                    
                    // Math to place them in an arc behind player shoulders
                    double offsetMultiplier = (i - (loaded - 1) / 2.0D) * 0.45D;
                    Vec3 spawnPos = eyePos.subtract(look.scale(0.8D))
                            .add(right.scale(offsetMultiplier))
                            .subtract(0.0D, 0.25D, 0.0D); // slightly lower than eyes

                    proj.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0.0F, 0.0F);
                    proj.setDeltaMovement(look.scale(0.2D)); // slow initial speed
                    if (target != null) {
                        proj.setTarget(target);
                    }
                    level.addFreshEntity(proj);
                }

                level.playSound(null, player, SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.2F, 0.6F);
            }

            player.getPersistentData().putInt("xebGoldenFlowerLoadedCount", 0);

            // Sync final state
            if (player instanceof ServerPlayer serverPlayer) {
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new GoldenFlowerSyncPacket(
                                player.getId(),
                                charges,
                                player.getPersistentData().getInt("xebGoldenFlowerRechargeTimer"),
                                0,
                                player.getPersistentData().getInt("xebJaronaCharges"),
                                player.getPersistentData().getInt("xebJaronaRechargeTimer"),
                                player.getPersistentData().getInt("xebGoldenFlowerDanceCooldown")
                        ));
            }
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && !entity.level().isClientSide()) {
            long currentTick = player.level().getGameTime();
            long lastPellet = player.getPersistentData().getLong("xebFlowerPelletLastTime");

            if (currentTick - lastPellet >= 40) { // 2 seconds cooldown
                player.getPersistentData().putLong("xebFlowerPelletLastTime", currentTick);

                FlowerPelletEntity pellet = new FlowerPelletEntity(player.level(), player);
                Vec3 eyePos = player.getEyePosition(1.0F);
                Vec3 look = player.getLookAngle();

                pellet.moveTo(eyePos.x, eyePos.y - 0.1D, eyePos.z, 0.0F, 0.0F);
                pellet.setDeltaMovement(look.scale(0.35D)); // very slow speed

                player.level().addFreshEntity(pellet);

                player.level().playSound(null, player, SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.golden_flower.desc1"));
        tooltip.add(Component.translatable("item.xeb.golden_flower.desc2"));
        tooltip.add(Component.translatable("item.xeb.golden_flower.desc_damage"));
        tooltip.add(Component.translatable("item.xeb.golden_flower.desc4", Component.keybind("key.xeb.activa_1")));
        tooltip.add(Component.translatable("item.xeb.golden_flower.desc5", Component.keybind("key.xeb.activa_2")));
        tooltip.add(Component.translatable("item.xeb.golden_flower.desc3"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(net.minecraft.ChatFormatting.RED);
    }
}
