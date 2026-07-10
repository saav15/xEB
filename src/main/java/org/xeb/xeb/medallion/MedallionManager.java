package org.xeb.xeb.medallion;

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

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class MedallionManager {
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
                        // ignore malformed tier
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



    /**
     * Computes the set of buff IDs that conflict with any buff already in {@code current}.
     * Each buff declares its own conflicts via {@link EliteBuff#getConflicts()} — no
     * hardcoded switch needed here.  Returns a {@link Set} for O(1) caller lookups.
     */
    public static Set<String> getConflictingBuffSet(List<MedallionData> current) {
        Set<String> conflicts = new HashSet<>();
        for (MedallionData m : current) {
            conflicts.addAll(m.getBuff().getConflicts());
        }
        return conflicts;
    }

    public static int getEliteMeterLevel(Player player) {
        return getEliteMeterLevel(player, true);
    }

    public static int getEliteMeterLevel(Player player, boolean includeModifiers) {
        if (!org.xeb.xeb.Config.eliteMeterEnabled) return 10;

        int manualLevel = player.getPersistentData().getInt("xebEliteMeterLevel");
        int baseLevel = manualLevel;

        if (player instanceof ServerPlayer serverPlayer) {
            int completedCount = 0;
            List<String> list = org.xeb.xeb.Config.eliteMeterAdvancements;
            PlayerAdvancements advancements = serverPlayer.getAdvancements();
            net.minecraft.server.MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                for (int i = 0; i < list.size(); i++) {
                    ResourceLocation id = new ResourceLocation(list.get(i));
                    Advancement advancement = server.getAdvancements().getAdvancement(id);
                    if (advancement != null && advancements.getOrStartProgress(advancement).isDone()) {
                        completedCount++;
                    }
                }
            }
            baseLevel = Math.max(manualLevel, completedCount);
        }

        // Add +2 levels if Permanight is active in this level and modifiers are enabled
        if (includeModifiers && player.level() instanceof ServerLevel serverLevel) {
            if (org.xeb.xeb.world.PermanightSavedData.get(serverLevel).isActive()) {
                baseLevel += 2;
            }
        }

        return Math.max(1, baseLevel);
    }

    public static int getProgressionMaxMedallions(int level, Difficulty difficulty) {
        int maxByLvl = 1;
        if (level >= 10) maxByLvl = 4;
        else if (level >= 7) maxByLvl = 3;
        else if (level >= 4) maxByLvl = 2;

        int limitByDiff = switch (difficulty) {
            case EASY -> 2;
            case NORMAL -> 3;
            default -> 4;
        };

        return Math.min(maxByLvl, limitByDiff);
    }

    public static void assignRandomMedallions(LivingEntity entity, ServerLevel level) {
        if (entity.level().isClientSide()) return;
        
        // Check if already assigned
        if (entity.getPersistentData().contains(MEDALLIONS_KEY)) return;

        Difficulty difficulty = level.getDifficulty();
        if (difficulty == Difficulty.PEACEFUL) return;

        boolean isBoss = isBoss(entity);
        RandomSource random = entity.getRandom();

        // 1. Scan for nearby players to calculate progression level
        int targetLevel = 10; // Default when disabled
        if (org.xeb.xeb.Config.eliteMeterEnabled) {
            double radius = org.xeb.xeb.Config.playerScanRadius;
            net.minecraft.world.phys.AABB searchBox = entity.getBoundingBox().inflate(radius);
            List<Player> players = level.getEntitiesOfClass(Player.class, searchBox, p -> !p.isSpectator() && !p.isCreative());
            
            if (!players.isEmpty()) {
                Player lowest = players.get(0);
                Player highest = players.get(0);
                int minLvl = getEliteMeterLevel(lowest);
                int maxLvl = minLvl;

                for (Player p : players) {
                    int pLvl = getEliteMeterLevel(p);
                    if (pLvl < minLvl) {
                        minLvl = pLvl;
                        lowest = p;
                    }
                    if (pLvl > maxLvl) {
                        maxLvl = pLvl;
                        highest = p;
                    }
                }

                double roll = random.nextDouble();
                if (roll < 0.60) {
                    targetLevel = minLvl;
                } else if (roll < 0.90) {
                    Player randPlayer = players.get(random.nextInt(players.size()));
                    targetLevel = getEliteMeterLevel(randPlayer);
                } else {
                    targetLevel = maxLvl;
                }
            } else {
                targetLevel = 1; // Default to lvl 1 if no players nearby
            }
        }

        // 2. Gate bosses based on Elite Meter level
        if (isBoss && org.xeb.xeb.Config.eliteMeterEnabled && targetLevel < org.xeb.xeb.Config.bossMinLevelRequired) {
            return; // No medallions for this boss
        }

        // 3. Compute progression-based spawn chance
        double spawnChance;
        if (org.xeb.xeb.Config.eliteMeterEnabled) {
            if (isBoss) {
                spawnChance = switch (difficulty) {
                    case EASY -> Math.min(0.60, 0.20 + targetLevel * 0.04);
                    case NORMAL -> Math.min(0.80, 0.30 + targetLevel * 0.05);
                    default -> Math.min(1.0, 0.40 + targetLevel * 0.06);
                };
            } else {
                spawnChance = switch (difficulty) {
                    case EASY -> Math.min(0.30, 0.05 + targetLevel * 0.02);
                    case NORMAL -> Math.min(0.50, 0.10 + targetLevel * 0.04);
                    default -> Math.min(0.80, 0.20 + targetLevel * 0.06);
                };
            }
        } else {
            spawnChance = getSpawnChance(isBoss, difficulty);
        }

        if (random.nextDouble() > Math.min(1.0D, spawnChance * 1.10D)) {
            return; // Did not pass spawn chance check
        }

        // 4. Compute maximum medallion count and roll
        int maxMedallions = org.xeb.xeb.Config.eliteMeterEnabled ?
                getProgressionMaxMedallions(targetLevel, difficulty) :
                getMaxMedallions(isBoss, difficulty);

        int count = 1;
        if (maxMedallions > 1) {
            double r = random.nextDouble();
            if (maxMedallions == 2) {
                count = r < 0.75 ? 1 : 2;
            } else if (maxMedallions == 3) {
                count = r < 0.60 ? 1 : r < 0.90 ? 2 : 3;
            } else { // 4
                count = r < 0.50 ? 1 : r < 0.80 ? 2 : r < 0.95 ? 3 : 4;
            }
        }

        List<MedallionData> rolled = new ArrayList<>();
        // Un único Set que crece con cada medallion asignado — O(1) lookups, sin copias por iteración
        Set<String> excludeSet = new HashSet<>();

        for (int i = 0; i < count; i++) {
            // Unir conflictos de los medallions ya rodados al excludeSet (operación única por iteración)
            excludeSet.addAll(getConflictingBuffSet(rolled));

            // Excluir buffs demasiado difíciles para el nivel actual del Elite Meter
            if (org.xeb.xeb.Config.eliteMeterEnabled) {
                for (EliteBuff b : EliteBuffRegistry.getAll()) {
                    double diff = b.getWeight();
                    if (targetLevel <= 2 && diff > 1.0D) {
                        excludeSet.add(b.getId());
                    } else if (targetLevel <= 5 && diff > 2.0D) {
                        excludeSet.add(b.getId());
                    } else if (targetLevel <= 8 && diff > 5.0D) {
                        excludeSet.add(b.getId());
                    }
                }
            }

            EliteBuff buff = EliteBuffRegistry.getRandomByWeight(random, isBoss, new ArrayList<>(excludeSet));
            if (buff != null) {
                MedallionType tier = rollTier(difficulty, random, targetLevel);
                rolled.add(new MedallionData(buff, tier, UUID.randomUUID()));
                if (!buff.isStackable()) {
                    excludeSet.add(buff.getId());
                }
            }
        }

        if (!rolled.isEmpty()) {
            saveMedallions(entity, rolled);
            for (MedallionData m : rolled) {
                m.getBuff().onAttach(entity, m.getUniqueId());
            }
            refreshDimensionsIfNeeded(entity, rolled);
            syncToTracking(entity);
        }
    }

    private static MedallionType rollTier(Difficulty difficulty, RandomSource random, int level) {
        if (!org.xeb.xeb.Config.eliteMeterEnabled) {
            double roll = random.nextDouble();
            if (difficulty == Difficulty.EASY) {
                return MedallionType.COMMON;
            } else if (difficulty == Difficulty.NORMAL) {
                if (roll < 0.80) return MedallionType.COMMON;
                return MedallionType.RARE;
            } else {
                if (roll < 0.60) return MedallionType.COMMON;
                if (roll < 0.90) return MedallionType.RARE;
                return MedallionType.LEGENDARY;
            }
        }

        double roll = random.nextDouble();
        double levelFactor = Math.min(1.0, (level - 1) / 9.0);
        
        double commonWeight = 1.0 - (0.60 * levelFactor); // 1.0 down to 0.40
        double rareWeight = 0.40 * levelFactor;           // 0.0 up to 0.40
        double legendaryWeight = 0.20 * levelFactor;      // 0.0 up to 0.20
        
        if (difficulty == Difficulty.EASY) {
            legendaryWeight = 0.0;
            rareWeight = rareWeight * 0.5;
            commonWeight = 1.0 - rareWeight;
        } else if (difficulty == Difficulty.NORMAL) {
            legendaryWeight = legendaryWeight * 0.5;
            commonWeight = 1.0 - rareWeight - legendaryWeight;
        }
        
        if (roll < commonWeight) return MedallionType.COMMON;
        if (roll < (commonWeight + rareWeight)) return MedallionType.RARE;
        return MedallionType.LEGENDARY;
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
        } catch (Exception ignored) {
            // Safety guard for edge cases during entity initialization
        }
    }

    public static boolean isBoss(LivingEntity entity) {
        if (entity instanceof WitherBoss || entity instanceof EnderDragon) {
            return true;
        }
        // Use the configurable health threshold (default 300.0, set in config/xeb-client.toml)
        if (entity.getMaxHealth() >= org.xeb.xeb.Config.bossHealthThreshold) {
            return true;
        }
        // Delegate to ModCompatManager for tag-based and mod-specific boss detection
        return org.xeb.xeb.compat.ModCompatManager.isBoss(entity);
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
