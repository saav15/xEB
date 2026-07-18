package org.xeb.xeb.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.enchantment.ModEnchantments;
import org.xeb.xeb.entity.FlowerProjectileEntity;
import org.xeb.xeb.entity.SpikeProjectileEntity;
import org.xeb.xeb.item.ModItems;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RiftBossAIHandler {
    private static final Map<UUID, ServerBossEvent> BOSS_BARS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Mob boss)) return;

        // Check if it is a rift boss
        if (!boss.getPersistentData().getBoolean("xebRiftBoss")) return;

        UUID uuid = boss.getUUID();
        int difficulty = boss.getPersistentData().getInt("xebRiftDifficulty");

        // 1. Manage Boss Bar
        ServerBossEvent bossEvent = BOSS_BARS.computeIfAbsent(uuid, id -> {
            Component displayName = boss.getDisplayName();
            BossEvent.BossBarColor color = switch (difficulty) {
                case 1 -> BossEvent.BossBarColor.GREEN;
                case 2 -> BossEvent.BossBarColor.RED;
                case 3 -> BossEvent.BossBarColor.PINK; // Rainbow
                case 0 -> BossEvent.BossBarColor.BLUE;
                default -> BossEvent.BossBarColor.WHITE;
            };
            ServerBossEvent bar = new ServerBossEvent(displayName, color, BossEvent.BossBarOverlay.NOTCHED_10);
            bar.setVisible(true);
            return bar;
        });

        // Update progress
        bossEvent.setProgress(boss.getHealth() / boss.getMaxHealth());

        // Update players seeing it (within 32 blocks)
        List<ServerPlayer> nearbyPlayers = boss.level().getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(32.0D));
        Set<ServerPlayer> currentPlayers = new HashSet<>(bossEvent.getPlayers());

        for (ServerPlayer player : nearbyPlayers) {
            if (!currentPlayers.contains(player)) {
                bossEvent.addPlayer(player);
            }
        }
        for (ServerPlayer player : currentPlayers) {
            if (!nearbyPlayers.contains(player)) {
                bossEvent.removePlayer(player);
            }
        }

        // Cleanup if boss is dead/removed
        if (!boss.isAlive() || boss.isRemoved()) {
            removeBossBar(uuid);
            return;
        }

        // 2. Specialized Counter-AI logic
        // Counter slow: Break out of movement slowdown instantly and gain burst speed
        if (boss.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            boss.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false, true));
            boss.level().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
        }

        // Counter beams: Dodge linear lasers by side-stepping perpendicular to player sightline
        int dodgeCooldown = boss.getPersistentData().getInt("xebBossDodgeCooldown");
        if (dodgeCooldown > 0) {
            boss.getPersistentData().putInt("xebBossDodgeCooldown", dodgeCooldown - 1);
        } else {
            // Find nearby players looking at the boss while active/aiming
            List<ServerPlayer> aimingPlayers = boss.level().getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(20.0D),
                    p -> p.isUsingItem() && p.getLookAngle().dot(boss.position().subtract(p.getEyePosition(1.0F)).normalize()) > 0.97D);
            if (!aimingPlayers.isEmpty()) {
                ServerPlayer threat = aimingPlayers.get(0);
                Vec3 threatLook = threat.getLookAngle();
                Vec3 perp = threatLook.cross(new Vec3(0, 1, 0)).normalize();
                if (perp.lengthSqr() < 0.01) perp = threatLook.cross(new Vec3(1, 0, 0)).normalize();

                // Side step velocity jump (random left or right)
                double side = boss.getRandom().nextBoolean() ? 1.0D : -1.0D;
                boss.setDeltaMovement(perp.scale(side * 0.85D).add(0.0D, 0.15D, 0.0D));
                boss.getPersistentData().putInt("xebBossDodgeCooldown", 80); // 4 seconds CD

                boss.level().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                        SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.0F, 1.5F);
            }
        }

        // Spikes/Projectile rapid hit ticking cooldown
        int projTimer = boss.getPersistentData().getInt("xebProjHitTimer");
        if (projTimer > 0) {
            boss.getPersistentData().putInt("xebProjHitTimer", projTimer - 1);
        } else {
            boss.getPersistentData().putInt("xebProjHits", 0);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Mob boss)) return;
        if (!boss.getPersistentData().getBoolean("xebRiftBoss")) return;

        // Counter projectile barrages (e.g. flower pellets, halberd spikes)
        Entity directSource = event.getSource().getDirectEntity();
        if (directSource instanceof Projectile || directSource instanceof FlowerProjectileEntity || directSource instanceof SpikeProjectileEntity) {
            int hits = boss.getPersistentData().getInt("xebProjHits") + 1;
            boss.getPersistentData().putInt("xebProjHits", hits);
            boss.getPersistentData().putInt("xebProjHitTimer", 40); // 2 second window

            if (hits >= 3) {
                // Trigger shockwave shield
                boss.getPersistentData().putInt("xebProjHits", 0);
                boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false, true)); // Resistance V (Immune)

                // Sonic boom shockwave sound
                boss.level().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.5F, 1.2F);

                // Push back nearby players
                List<LivingEntity> targets = boss.level().getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(6.0D));
                for (LivingEntity target : targets) {
                    if (target != boss) {
                        Vec3 push = target.position().subtract(boss.position()).normalize().scale(1.3D);
                        target.setDeltaMovement(push.x, 0.45D, push.z);
                        target.hurtMarked = true;
                    }
                }

                // Destroy nearby projectiles in range
                List<Entity> projectiles = boss.level().getEntitiesOfClass(Entity.class, boss.getBoundingBox().inflate(6.0D),
                        p -> p instanceof FlowerProjectileEntity || p instanceof SpikeProjectileEntity || p instanceof Projectile);
                for (Entity proj : projectiles) {
                    proj.discard();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBossDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Mob boss)) return;
        if (!boss.getPersistentData().getBoolean("xebRiftBoss")) return;

        UUID uuid = boss.getUUID();
        removeBossBar(uuid);

        int difficulty = boss.getPersistentData().getInt("xebRiftDifficulty");
        
        // Find Medallero level from killer
        int medallistLvl = 0;
        if (event.getSource().getEntity() instanceof LivingEntity killer) {
            medallistLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.MEDALLERO.get(), killer.getMainHandItem());
        }

        double multiplier = 1.0D + (medallistLvl * 0.35D);
        double spawnX = boss.getX();
        double spawnY = boss.getY() + 0.5D;
        double spawnZ = boss.getZ();

        // 1. Smart Halberd drop chance (1% base + 2% per Medallero level)
        if (boss.getItemBySlot(EquipmentSlot.MAINHAND).is(ModItems.SMART_HALBERD.get())) {
            float dropChance = 0.01F + (medallistLvl * 0.02F);
            if (boss.getRandom().nextFloat() < dropChance) {
                event.getDrops().add(new ItemEntity(boss.level(), spawnX, spawnY, spawnZ, new ItemStack(ModItems.SMART_HALBERD.get())));
            }
        }

        // 2. Extra minerals / gems drop based on difficulty and medallist level
        int emeraldCount = 0;
        int diamondCount = 0;
        int goldCount = 0;
        int netheriteCount = 0;
        boolean dropNetheriteIngot = false;

        switch (difficulty) {
            case 3: // Rainbow
                diamondCount = (int) ((8 + boss.getRandom().nextInt(9)) * multiplier);
                emeraldCount = (int) ((8 + boss.getRandom().nextInt(9)) * multiplier);
                goldCount = (int) ((12 + boss.getRandom().nextInt(13)) * multiplier);
                netheriteCount = (int) ((1 + boss.getRandom().nextInt(3)) * multiplier);
                dropNetheriteIngot = true;
                break;
            case 2: // Red
                diamondCount = (int) ((4 + boss.getRandom().nextInt(5)) * multiplier);
                emeraldCount = (int) ((4 + boss.getRandom().nextInt(5)) * multiplier);
                goldCount = (int) ((6 + boss.getRandom().nextInt(7)) * multiplier);
                netheriteCount = (int) ((1 + boss.getRandom().nextInt(2)) * multiplier);
                break;
            case 1: // Green
                diamondCount = (int) ((2 + boss.getRandom().nextInt(4)) * multiplier);
                emeraldCount = (int) ((2 + boss.getRandom().nextInt(4)) * multiplier);
                goldCount = (int) ((4 + boss.getRandom().nextInt(5)) * multiplier);
                if (boss.getRandom().nextFloat() < 0.15F * multiplier) {
                    netheriteCount = 1;
                }
                break;
            case 0:
            default: // Blue
                diamondCount = (int) ((1 + boss.getRandom().nextInt(3)) * multiplier);
                emeraldCount = (int) ((1 + boss.getRandom().nextInt(3)) * multiplier);
                goldCount = (int) ((2 + boss.getRandom().nextInt(4)) * multiplier);
                break;
        }

        if (diamondCount > 0) {
            event.getDrops().add(new ItemEntity(boss.level(), spawnX, spawnY, spawnZ, new ItemStack(Items.DIAMOND, diamondCount)));
        }
        if (emeraldCount > 0) {
            event.getDrops().add(new ItemEntity(boss.level(), spawnX, spawnY, spawnZ, new ItemStack(Items.EMERALD, emeraldCount)));
        }
        if (goldCount > 0) {
            event.getDrops().add(new ItemEntity(boss.level(), spawnX, spawnY, spawnZ, new ItemStack(Items.GOLD_INGOT, goldCount)));
        }
        if (netheriteCount > 0) {
            event.getDrops().add(new ItemEntity(boss.level(), spawnX, spawnY, spawnZ, new ItemStack(
                    dropNetheriteIngot ? Items.NETHERITE_INGOT : Items.NETHERITE_SCRAP, netheriteCount)));
        }

        // 3. One highly enchanted random weapon / armor piece
        ItemStack gear;
        if (difficulty == 3) {
            // Netherite piece
            gear = new ItemStack(boss.getRandom().nextBoolean() ? Items.NETHERITE_SWORD : Items.NETHERITE_CHESTPLATE);
            EnchantmentHelper.enchantItem(boss.getRandom(), gear, 30, true);
        } else if (difficulty == 2) {
            // Diamond piece
            gear = new ItemStack(boss.getRandom().nextBoolean() ? Items.DIAMOND_SWORD : Items.DIAMOND_CHESTPLATE);
            EnchantmentHelper.enchantItem(boss.getRandom(), gear, 25, true);
        } else {
            // Iron piece
            gear = new ItemStack(boss.getRandom().nextBoolean() ? Items.IRON_SWORD : Items.IRON_CHESTPLATE);
            EnchantmentHelper.enchantItem(boss.getRandom(), gear, 15, false);
        }
        event.getDrops().add(new ItemEntity(boss.level(), spawnX, spawnY, spawnZ, gear));
    }

    private static void removeBossBar(UUID uuid) {
        ServerBossEvent event = BOSS_BARS.remove(uuid);
        if (event != null) {
            event.setVisible(false);
            event.removeAllPlayers();
        }
    }
}
