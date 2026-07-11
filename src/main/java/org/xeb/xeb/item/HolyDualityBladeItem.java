package org.xeb.xeb.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.network.HolySyncPacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xeb.xeb.Xeb;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HolyDualityBladeItem extends SwordItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Blessed fire blocks system
    public static final java.util.Map<net.minecraft.core.BlockPos, Integer> BLESSED_FIRE_BLOCKS = new java.util.concurrent.ConcurrentHashMap<>();

    // Base damage constant
    public static final double BASE_DAMAGE = 8.0D;

    public HolyDualityBladeItem(Properties properties) {
        super(Tiers.IRON, 3, -2.4F, properties);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide()) {
            for (java.util.Map.Entry<net.minecraft.core.BlockPos, Integer> entry : BLESSED_FIRE_BLOCKS.entrySet()) {
                net.minecraft.core.BlockPos pos = entry.getKey();
                int ticks = entry.getValue() - 1;
                if (ticks <= 0) {
                    BLESSED_FIRE_BLOCKS.remove(pos);
                } else {
                    BLESSED_FIRE_BLOCKS.put(pos, ticks);
                    
                    // Damage entities walking on the block
                    AABB box = new AABB(pos);
                    List<LivingEntity> targets = event.level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive());
                    for (LivingEntity target : targets) {
                        target.hurt(event.level.damageSources().inFire(), 2.0F); // holy burn damage
                    }
                    
                    // Spawn soul fire particles on the block
                    if (event.level.getGameTime() % 10 == 0 && event.level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                                3, 0.2D, 0.0D, 0.2D, 0.02D);
                    }
                }
            }
        }
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
        tooltip.add(Component.translatable("item.xeb.holy_duality_blade.desc1"));
        tooltip.add(Component.translatable("item.xeb.holy_duality_blade.desc2"));
        tooltip.add(Component.translatable("item.xeb.holy_duality_blade.desc_damage"));
        tooltip.add(Component.translatable("item.xeb.holy_duality_blade.desc4", Component.keybind("key.xeb.activa_1")));
        tooltip.add(Component.translatable("item.xeb.holy_duality_blade.desc5", Component.keybind("key.xeb.activa_2")));
        tooltip.add(Component.translatable("item.xeb.holy_duality_blade.desc3"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(net.minecraft.ChatFormatting.RED);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag pData = player.getPersistentData();
        if (pData.getInt("xebHolyRCD") > 0) {
            return InteractionResultHolder.fail(stack);
        }
        pData.putInt("xebHolyBlastCharge", 0);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player) {
            int ticksUsed = this.getUseDuration(stack) - count;
            player.getPersistentData().putInt("xebHolyBlastCharge", Math.min(160, ticksUsed));
            
            if (level.isClientSide()) {
                // SUTIL: iluminar el suelo con quads dorados semitransparentes, NO partículas
                // La iluminación visual se maneja en el client handler via NBT sync
                // Solo spawn-ear 1 partícula sutil cada 10 ticks en el centro
                if (level.getGameTime() % 10 == 0) {
                    level.addParticle(ParticleTypes.END_ROD,
                            player.getX(), player.getY() + 0.1D, player.getZ(),
                            0.0D, 0.03D, 0.0D);
                }
            } else {
                if (level.getGameTime() % 2 == 0) {
                    syncToClient((ServerPlayer) player);
                }
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int ticksUsed = this.getUseDuration(stack) - timeLeft;
            player.getPersistentData().putInt("xebHolyBlastCharge", 0);
            
            if (player.getPersistentData().getInt("xebHolyRCD") <= 0 && ticksUsed >= 10) {
                player.getPersistentData().putInt("xebHolyBlastCastTicks", 10);
                if (!level.isClientSide()) {
                    triggerHolyBlast(level, player, ticksUsed);
                } else {
                    double radius = 1.5D + (Math.min(160, ticksUsed) / 160.0D) * 2.5D;
                    int radInt = (int) Math.ceil(radius);
                    for (int dx = -radInt; dx <= radInt; dx++) {
                        for (int dz = -radInt; dz <= radInt; dz++) {
                            if (Math.sqrt(dx*dx + dz*dz) > radius) continue;
                            double targetX = player.getX() + dx;
                            double targetZ = player.getZ() + dz;
                            double targetY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int)targetX, (int)targetZ);
                            org.xeb.xeb.client.HolyDualityClientHandler.ACTIVE_BEAMS.add(
                                    new org.xeb.xeb.client.HolyDualityClientHandler.HolyBeam(targetX, targetY, targetZ, 15)
                            );
                        }
                    }
                }
            }
            
            if (!level.isClientSide()) {
                syncToClient((ServerPlayer) player);
            }
        }
    }

    private void triggerHolyBlast(Level level, Player player, int ticksUsed) {
        CompoundTag pData = player.getPersistentData();
        pData.putInt("xebHolyRCD", 400); // 20s

        int radius = Math.min(4, ticksUsed / 20); // 0 a 4 bloques de radio (3x3 a 9x9)
        if (radius < 1) radius = 1;
        int hitCount = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx*dx + dz*dz);
                if (dist > radius) continue; // circular

                double dmg = 18.0D - dist * 3.0D;
                double targetX = player.getX() + dx;
                double targetZ = player.getZ() + dz;
                double targetY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int)targetX, (int)targetZ);

                // NUEVO: beams de luz dorada descendentes (solo visuales, manejados por client handler)
                // NO spawnear partículas de fuego ni explosión

                // Daño sutil en el centro, decae hacia los bordes
                if (level instanceof ServerLevel sl) {
                    // SOLO 1 partícula de END_ROD sutil por bloque
                    sl.sendParticles(ParticleTypes.END_ROD, targetX, targetY + 0.5D, targetZ,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }

                // Sonido sutil, NO trueno
                if (dx == 0 && dz == 0) {
                    level.playSound(null, targetX, targetY, targetZ,
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 1.8F);
                }

                AABB damageBox = new AABB(targetX - 0.8D, targetY - 1.0D, targetZ - 0.8D,
                                          targetX + 0.8D, targetY + 3.0D, targetZ + 0.8D);

                int blessedCharges = pData.getInt("xebHolyBlessedCharges");
                boolean isBlessed = blessedCharges > 0;
                if (isBlessed) {
                    dmg *= 3.0D;
                    pData.putInt("xebHolyBlessedCharges", blessedCharges - 1);
                }
                boolean isCrit = isBlessed || (player.getRandom().nextFloat() < 0.25F);

                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageBox, e -> e != player && e.isAlive());
                for (LivingEntity target : targets) {
                    float finalDmg = (float) dmg;
                    if (isCrit) finalDmg *= 1.5F;
                    target.hurt(level.damageSources().magic(), finalDmg);
                    hitCount++;
                }
            }
        }

        // Cooldown reduction
        if (hitCount > 0) {
            int currentRCD = pData.getInt("xebHolyRCD");
            int finalRCD = Math.max(0, currentRCD - (hitCount * 13));
            pData.putInt("xebHolyRCD", finalRCD);
            if (finalRCD > 0) {
                player.getCooldowns().addCooldown(this, finalRCD);
            }
        } else {
            player.getCooldowns().addCooldown(this, 400);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) return;

        boolean holdsBlade = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!holdsBlade) {
            // Clean up state if not held
            CompoundTag pData = player.getPersistentData();
            if (pData.getInt("xebHolyBlastCharge") > 0 || pData.getBoolean("xebHolyBlessedActive")) {
                pData.putInt("xebHolyBlastCharge", 0);
                pData.putBoolean("xebHolyBlessedActive", false);
                pData.putBoolean("xebHolyShieldActive", false);
                pData.putInt("xebHolyBlessedTicks", 0);
                if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    syncToClient(serverPlayer);
                }
            }
            return;
        }

        if (!level.isClientSide()) {
            CompoundTag pData = player.getPersistentData();
            int holyA1CD = pData.getInt("xebHolyA1Cooldown");
            int holyA2CD = pData.getInt("xebHolyA2Cooldown");
            int holyRCD = pData.getInt("xebHolyRCD");
            if (holyA1CD > 0) pData.putInt("xebHolyA1Cooldown", holyA1CD - 1);
            if (holyA2CD > 0) pData.putInt("xebHolyA2Cooldown", holyA2CD - 1);
            if (holyRCD > 0) pData.putInt("xebHolyRCD", holyRCD - 1);

            // Decrement blessed ticks
            int blessedTicks = pData.getInt("xebHolyBlessedTicks");
            if (blessedTicks > 0) {
                blessedTicks--;
                pData.putInt("xebHolyBlessedTicks", blessedTicks);
                if (blessedTicks <= 0) {
                    pData.putBoolean("xebHolyBlessedActive", false);
                    pData.putBoolean("xebHolyShieldActive", false);
                }
                
                // Divine Aura checking: Glowing + Weakness to hostile mobs within 3 blocks
                if (blessedTicks % 40 == 0) {
                    AABB area = player.getBoundingBox().inflate(3.0D);
                    List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, area,
                            e -> e != player && e.isAlive() && !e.isAlliedTo(player));
                    for (LivingEntity mob : mobs) {
                        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
                        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    }
                }
            }

            // Annihilation ticking — 3 vueltas con 7 daño cada una en 3x3
            if (pData.getBoolean("xebHolyAnnihilationActive")) {
                int annTicks = pData.getInt("xebHolyAnnihilationTicks");
                if (annTicks > 0) {
                    int hits = pData.getInt("xebHolyAnnihilationHits");

                    // Cada 10 ticks = 1 vuelta completa (3 vueltas total en 30 ticks)
                    if (annTicks % 10 == 0 && hits < 3) {
                        hits++;
                        pData.putInt("xebHolyAnnihilationHits", hits);

                        // Sonido de corte
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.5F);

                        // 3x3 AoE centrado en el player
                        AABB box = player.getBoundingBox().inflate(1.5D, 1.0D, 1.5D);
                        double baseDmg = 7.0D; // 7 daño por vuelta

                        int blessedCharges = pData.getInt("xebHolyBlessedCharges");
                        boolean isBlessed = blessedCharges > 0;
                        if (isBlessed) {
                            baseDmg *= 3.0D;
                            pData.putInt("xebHolyBlessedCharges", blessedCharges - 1);
                        }

                        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
                        for (LivingEntity target : targets) {
                            target.hurt(player.damageSources().playerAttack(player), (float) baseDmg);

                            // Knockback hacia afuera del player
                            Vec3 knockDir = target.position().subtract(player.position()).normalize();
                            target.setDeltaMovement(knockDir.x * 0.8D, 0.3D, knockDir.z * 0.8D);
                            target.hurtMarked = true;

                            // Partículas sutiles
                            if (level instanceof ServerLevel sl) {
                                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(),
                                        3, 0.2D, 0.2D, 0.2D, 0.05D);
                            }
                        }
                    }

                    pData.putInt("xebHolyAnnihilationTicks", annTicks - 1);
                } else {
                    pData.putBoolean("xebHolyAnnihilationActive", false);
                    pData.remove("xebHolyAnnihilationHits");
                }
            }

            if (player instanceof ServerPlayer serverPlayer && level.getGameTime() % 2 == 0) {
                syncToClient(serverPlayer);
            }
        }
    }

    public static void syncToClient(ServerPlayer player) {
        CompoundTag pData = player.getPersistentData();
        ItemStack stack = player.getMainHandItem().getItem() instanceof org.xeb.xeb.item.HolyDualityBladeItem
                ? player.getMainHandItem() : player.getOffhandItem();
        int combo = stack.isEmpty() ? 0 : stack.getOrCreateTag().getInt("xebHolyComboStage");

        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new HolySyncPacket(
                        player.getId(),
                        pData.getBoolean("xebHolyShieldActive"),
                        pData.getBoolean("xebHolyAnnihilationActive"),
                        pData.getBoolean("xebHolyBlessedActive"),
                        pData.getInt("xebHolyBlessedTicks"),
                        pData.getInt("xebHolyBlastCharge"),
                        1.5D + (Math.min(160, pData.getInt("xebHolyBlastCharge")) / 160.0D) * 2.5D,
                        pData.getInt("xebHolyA1Cooldown"),
                        pData.getInt("xebHolyA2Cooldown"),
                        combo
                )
        );
    }
}
