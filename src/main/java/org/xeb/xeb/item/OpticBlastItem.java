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
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

import java.util.List;

/**
 * Optic Blast weapon item.
 * <p>
 * Right-click (hold): fires a continuous red laser beam from the player's eyes.
 * Uses an energy resource system — the beam drains energy while firing and energy
 * regenerates passively when not firing. If energy hits 0 ("overheat"), the weapon
 * enters a forced cooldown. If the player releases before overheat, no cooldown is applied.
 * <p>
 * Left-click: fires a small mini-laser projectile from eye level.
 * <p>
 * Activa 1 (Cyclone Push): Laser beam that pushes the player in the opposite direction.
 * Activa 2 (Gene Splice): Chain laser that jumps between nearby entities.
 */
public class OpticBlastItem extends Item implements GeoItem {

    // ── Energy Resource System ──────────────────────────────────────────────────
    /** Maximum energy the weapon can hold. */
    public static final float MAX_ENERGY = 100.0F;
    /** Energy drained per tick while firing the beam. */
    public static final float ENERGY_DRAIN_PER_TICK = 1.5F;
    /** Energy regenerated per tick while NOT firing. */
    public static final float ENERGY_REGEN_PER_TICK = 1.0F;
    /** Forced cooldown ticks when energy hits 0 (overheat). 6 seconds. */
    public static final int OVERHEAT_COOLDOWN = 120;

    // ── Damage ──────────────────────────────────────────────────────────────────
    /** Damage per tick for the continuous beam (right-click). */
    public static final float BEAM_DAMAGE_PER_TICK = 5.0F;

    // ── Mini-laser ──────────────────────────────────────────────────────────────
    /** Cooldown ticks for the mini-laser left click. */
    public static final int MINI_LASER_COOLDOWN = 10; // 0.5 seconds

    // ── Ability Cooldowns ───────────────────────────────────────────────────────
    /** Cyclone Push cooldown: 10 seconds. */
    public static final int CYCLONE_PUSH_COOLDOWN = 200;
    /** Cyclone Push max duration: 4 seconds. */
    public static final int CYCLONE_PUSH_DURATION = 80;
    /** Gene Splice cooldown: 8 seconds. */
    public static final int GENE_SPLICE_COOLDOWN = 160;
    /** Gene Splice max duration: 3 seconds. */
    public static final int GENE_SPLICE_DURATION = 60;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OpticBlastItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            // If the controller is currently playing glare, let it finish.
            if (event.getController().getCurrentAnimation() != null && event.getController().getCurrentAnimation().animation().name().equals("glare")) {
                return software.bernie.geckolib.core.object.PlayState.CONTINUE;
            }
            // 0.5% chance per tick (on average once every 10 seconds / 200 ticks) to play glare randomly
            if (Math.random() < 0.005) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("glare").thenLoop("idle"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private org.xeb.xeb.client.renderer.OpticBlastGeoRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new org.xeb.xeb.client.renderer.OpticBlastGeoRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // effectively infinite hold
    }

    /**
     * Gets the current energy for a player. Initializes to MAX if not set.
     */
    public static float getEnergy(Player player) {
        if (!player.getPersistentData().contains("xebOpticEnergy")) {
            player.getPersistentData().putFloat("xebOpticEnergy", MAX_ENERGY);
        }
        return player.getPersistentData().getFloat("xebOpticEnergy");
    }

    /**
     * Sets the current energy for a player, clamped to [0, MAX_ENERGY].
     */
    public static void setEnergy(Player player, float energy) {
        player.getPersistentData().putFloat("xebOpticEnergy", Math.max(0.0F, Math.min(MAX_ENERGY, energy)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Don't start if on cooldown (overheat)
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // Don't start if energy is 0
        float energy = getEnergy(player);
        if (energy <= 0.0F) {
            return InteractionResultHolder.fail(stack);
        }

        // Exclusivity: Don't allow using primary laser if either ability is active
        if (player.getPersistentData().getBoolean("xebCyclonePushFiring")
                || player.getPersistentData().getBoolean("xebGeneSpliceFiring")) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);

        if (!level.isClientSide()) {
            // Beam charge-up sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.8F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof ServerPlayer player)) return;

        // Drain energy
        float energy = getEnergy(player);
        energy -= ENERGY_DRAIN_PER_TICK;
        setEnergy(player, energy);

        // If energy depleted, force stop and enter overheat cooldown
        if (energy <= 0.0F) {
            setEnergy(player, 0.0F);
            player.releaseUsingItem();
            return;
        }

        // The actual beam logic (raycast, damage, etc.) is handled by OpticBlastTickHandler
        // to keep this class clean. The tick handler reads the player's using-item state.
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof ServerPlayer player && !level.isClientSide()) {
            float energy = getEnergy(player);

            if (energy <= 0.0F) {
                // Overheat: apply forced cooldown
                player.getCooldowns().addCooldown(this, OVERHEAT_COOLDOWN);
            }
            // If energy > 0: NO cooldown — player managed their resource well

            // Stop beam sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.5F);

            // Notify clients to stop rendering beam
            org.xeb.xeb.network.XEBNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new org.xeb.xeb.network.OpticBlastBeamPacket(player.getId(), false, 0, 0, 0, 0, 0, 0, (byte) 0)
            );

            // Remove from beam manager
            org.xeb.xeb.opticblast.ActiveBeamManager.get().removeBeam(player.getUUID());
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        // Left-click: fire mini-laser projectile
        if (entity instanceof ServerPlayer player && !entity.level().isClientSide()) {
            // Exclusivity: Don't allow firing mini lasers if either ability is active or primary beam is firing
            if (player.getPersistentData().getBoolean("xebCyclonePushFiring")
                    || player.getPersistentData().getBoolean("xebGeneSpliceFiring")
                    || (player.isUsingItem() && player.getUseItem().is(this))) {
                return false;
            }

            long currentTick = player.level().getGameTime();
            long lastShot = player.getPersistentData().getLong("xebMiniLaserLastShot");

            if (currentTick - lastShot >= MINI_LASER_COOLDOWN) {
                player.getPersistentData().putLong("xebMiniLaserLastShot", currentTick);

                org.xeb.xeb.entity.MiniLaserProjectileEntity laser =
                        new org.xeb.xeb.entity.MiniLaserProjectileEntity(player.level(), player);

                net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition(1.0F);
                net.minecraft.world.phys.Vec3 look = player.getLookAngle();

                laser.moveTo(eyePos.x, eyePos.y - 0.1D, eyePos.z, 0.0F, 0.0F);
                laser.setDeltaMovement(look.scale(2.5D)); // Fast projectile

                player.level().addFreshEntity(laser);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.PLAYERS, 0.7F, 1.8F);
            }
        }
        return false; // Don't cancel the swing animation
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.optic_blast.desc1"));
        tooltip.add(Component.translatable("item.xeb.optic_blast.desc2"));
        tooltip.add(Component.translatable("item.xeb.optic_blast.desc3"));
        tooltip.add(Component.translatable("item.xeb.optic_blast.desc4", Component.keybind("key.xeb.activa_1")));
        tooltip.add(Component.translatable("item.xeb.optic_blast.desc5", Component.keybind("key.xeb.activa_2")));
        tooltip.add(Component.translatable("item.xeb.optic_blast.desc6"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
