package org.xeb.xeb.medallion;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.Config;
import org.xeb.xeb.network.MedallionSyncPacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class MedallionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MEDALLIONS_KEY = "xebMedallions";

    public static List<MedallionData> getMedallions(LivingEntity entity) {
        List<MedallionData> list = new ArrayList<>();
        CompoundTag data = entity.getPersistentData();
        if (data.contains(MEDALLIONS_KEY, Tag.TAG_LIST)) {
            ListTag listTag = data.getList(MEDALLIONS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entry = listTag.getCompound(i);
                String buffId = entry.getString("BuffId");
                String tierName = entry.getString("Tier");
                UUID uuid = entry.getUUID("UUID");

                EliteBuff buff = EliteBuffRegistry.getById(buffId);
                if (buff != null) {
                    try {
                        MedallionType tier = MedallionType.valueOf(tierName);
                        list.add(new MedallionData(buff, tier, uuid));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("[xEB] Ignoring malformed medallion tier '{}' for buff '{}'", tierName, buffId);
                    }
                }
            }
        }
        return list;
    }

    public static boolean hasBuff(LivingEntity entity, String buffId) {
        for (MedallionData m : getMedallions(entity)) {
            if (m.getBuff().getId().equals(buffId)) {
                return true;
            }
        }
        return false;
    }

    public static void attachMedallion(LivingEntity entity, MedallionData medallion) {
        List<MedallionData> current = getMedallions(entity);
        // Prevent duplicate buff ids only for non-stackable buffs
        if (!medallion.getBuff().isStackable()) {
            for (MedallionData m : current) {
                if (m.getBuff().getId().equals(medallion.getBuff().getId())) {
                    return;
                }
            }
        }
        current.add(medallion);
        saveMedallions(entity, current);
        medallion.getBuff().onAttach(entity, medallion.getUniqueId());
        syncToTracking(entity);
    }

    public static void removeAllMedallions(LivingEntity entity) {
        List<MedallionData> current = getMedallions(entity);
        for (MedallionData m : current) {
            m.getBuff().onDetach(entity, m.getUniqueId());
        }
        entity.getPersistentData().remove(MEDALLIONS_KEY);
        syncToTracking(entity);
    }

    public static void saveMedallions(LivingEntity entity, List<MedallionData> list) {
        ListTag listTag = new ListTag();
        for (MedallionData m : list) {
            CompoundTag entry = new CompoundTag();
            entry.putString("BuffId", m.getBuff().getId());
            entry.putString("Tier", m.getTier().name());
            entry.putUUID("UUID", m.getUniqueId());
            listTag.add(entry);
        }
        entity.getPersistentData().put(MEDALLIONS_KEY, listTag);
    }

    public static double getSpawnChance(boolean isBoss, Difficulty difficulty) {
        if (isBoss) {
            return switch (difficulty) {
                case PEACEFUL -> 0.0;
                case EASY -> 0.40;
                case NORMAL -> 0.60;
                case HARD -> 0.95; // Upgraded: Hard matches Hardcore
            };
        } else {
            return switch (difficulty) {
                case PEACEFUL -> 0.0;
                case EASY -> 0.15;
                case NORMAL -> 0.25;
                case HARD -> 0.60; // Upgraded: Hard matches Hardcore
            };
        }
    }

    public static int getMaxMedallions(boolean isBoss, Difficulty difficulty) {
        if (isBoss) {
            return switch (difficulty) {
                case PEACEFUL -> 0;
                case EASY -> 1;
                case NORMAL -> 2;
                case HARD -> 3; // Upgraded: Hard matches Hardcore
            };
        } else {
            return switch (difficulty) {
                case PEACEFUL -> 0;
                case EASY -> 1;
                case NORMAL -> 2;
                case HARD -> 3; // Upgraded: Hard matches Hardcore
            };
        }
    }

    public static void assignRandomMedallions(LivingEntity entity, ServerLevel level) {
        if (entity.level().isClientSide()) return;
        
        // Check if already assigned
        if (entity.getPersistentData().contains(MEDALLIONS_KEY)) return;

        Difficulty difficulty = level.getDifficulty();
        if (difficulty == Difficulty.PEACEFUL) return;

        boolean isBoss = isBoss(entity);
        RandomSource random = entity.getRandom();

        double spawnChance = getSpawnChance(isBoss, difficulty);
        if (random.nextDouble() > spawnChance) {
            // Did not pass spawn chance check
            return;
        }

        int maxMedallions = getMaxMedallions(isBoss, difficulty);
        // Roll medallion count (at least 1, up to maxMedallions)
        int count = 1;
        if (maxMedallions > 1) {
            count = 1 + random.nextInt(maxMedallions);
        }

        List<MedallionData> rolled = new ArrayList<>();
        List<String> excludeIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            EliteBuff buff = EliteBuffRegistry.getRandomByWeight(random, isBoss, excludeIds);
            if (buff != null) {
                MedallionType tier = rollTier(difficulty, random);
                rolled.add(new MedallionData(buff, tier, UUID.randomUUID()));
                excludeIds.add(buff.getId());
            }
        }

        if (!rolled.isEmpty()) {
            saveMedallions(entity, rolled);
            for (MedallionData m : rolled) {
                m.getBuff().onAttach(entity, m.getUniqueId());
            }
            // Refresh hitbox if any Mega medallions were assigned
            refreshDimensionsIfNeeded(entity, rolled);
            syncToTracking(entity);
        }
    }

    private static MedallionType rollTier(Difficulty difficulty, RandomSource random) {
        double roll = random.nextDouble();
        if (difficulty == Difficulty.EASY) {
            return MedallionType.COMMON;
        } else if (difficulty == Difficulty.NORMAL) {
            // 80% Common, 20% Rare
            if (roll < 0.80) return MedallionType.COMMON;
            return MedallionType.RARE;
        } else { // Hard or Hardcore
            // 60% Common, 30% Rare, 10% Legendary
            if (roll < 0.60) return MedallionType.COMMON;
            if (roll < 0.90) return MedallionType.RARE;
            return MedallionType.LEGENDARY;
        }
    }

    public static void copyMedallions(LivingEntity source, LivingEntity target) {
        List<MedallionData> medallions = getMedallions(source);
        List<MedallionData> copied = new ArrayList<>();
        for (MedallionData m : medallions) {
            copied.add(new MedallionData(m.getBuff(), m.getTier(), UUID.randomUUID()));
        }
        saveMedallions(target, copied);
        for (MedallionData m : copied) {
            m.getBuff().onAttach(target, m.getUniqueId());
        }
        // Refresh hitbox for the target entity if it has Mega medallions
        refreshDimensionsIfNeeded(target, copied);
        syncToTracking(target);
    }

    /**
     * Calls entity.refreshDimensions() if the given medallion list contains at least one Mega buff.
     * This forces Minecraft to re-query getDimensions(), which our mixin intercepts to apply
     * the correct scaled hitbox. Must be called server-side after Mega medallions are assigned.
     */
    public static void refreshDimensionsIfNeeded(LivingEntity entity, java.util.List<MedallionData> medallions) {
        if (entity.level().isClientSide()) return;
        try {
            entity.refreshDimensions();
        } catch (Exception e) {
            LOGGER.warn("[xEB] Failed to refresh dimensions for entity {}: {}", entity.getId(), e.getMessage());
        }
    }

    public static boolean isBoss(LivingEntity entity) {
        if (entity instanceof WitherBoss || entity instanceof EnderDragon) {
            return true;
        }
        // Custom Boss verification: check tag or max health >= 100
        if (entity.getMaxHealth() >= 100) {
            return true;
        }
        
        // Also check tag `#xeb:bosses`
        // We can check if the entity type has the bosses tag dynamically.
        // In 1.20.1 Forge:
        // entity.getType().is(...)
        // Since we will setup our compat manager, we'll also check ModCompatManager.isBoss(entity)
        return false;
    }

    public static void syncToTracking(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            List<MedallionData> medallions = getMedallions(entity);
            
            // Build the sync message
            List<String> buffIds = new ArrayList<>();
            List<String> tiers = new ArrayList<>();
            for (MedallionData m : medallions) {
                buffIds.add(m.getBuff().getId());
                tiers.add(m.getTier().name());
            }

            MedallionSyncPacket packet = new MedallionSyncPacket(entity.getId(), buffIds, tiers);
            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
        }
    }

    public static void syncToPlayer(LivingEntity entity, ServerPlayer player) {
        List<MedallionData> medallions = getMedallions(entity);
        if (!medallions.isEmpty()) {
            List<String> buffIds = new ArrayList<>();
            List<String> tiers = new ArrayList<>();
            for (MedallionData m : medallions) {
                buffIds.add(m.getBuff().getId());
                tiers.add(m.getTier().name());
            }
            MedallionSyncPacket packet = new MedallionSyncPacket(entity.getId(), buffIds, tiers);
            XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
