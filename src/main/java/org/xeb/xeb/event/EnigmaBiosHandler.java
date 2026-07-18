package org.xeb.xeb.event;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnigmaBiosHandler {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!org.xeb.xeb.Config.enabled) return;

        LivingEntity deadEntity = event.getEntity();
        if (deadEntity == null || deadEntity.level().isClientSide) return;

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer serverPlayer)) return;

        // Check if the dead entity has medallion data
        List<MedallionData> medallions = MedallionManager.getMedallions(deadEntity);
        if (medallions.isEmpty()) return;

        // Find the highest medallion tier
        MedallionType highestTier = MedallionType.COMMON;
        for (MedallionData m : medallions) {
            if (m.getTier() == MedallionType.LEGENDARY) {
                highestTier = MedallionType.LEGENDARY;
                break; // Gold is the highest
            } else if (m.getTier() == MedallionType.RARE) {
                highestTier = MedallionType.RARE;
            }
        }

        // Drop chance check: Bronze (0.5%), Silver (1.0%), Gold (2.5%)
        double chance = switch (highestTier) {
            case COMMON -> 0.005;      // 0.5%
            case RARE -> 0.01;         // 1.0%
            case LEGENDARY -> 0.025;   // 2.5%
        };

        // Enchantment Medallero extra chance check: Level I (5%), II (8%), III (14%)
        net.minecraft.world.item.ItemStack weapon = serverPlayer.getMainHandItem();
        int medalleroLvl = weapon.getEnchantmentLevel(org.xeb.xeb.enchantment.ModEnchantments.MEDALLERO.get());
        double extraChance = switch (medalleroLvl) {
            case 1 -> 0.05;
            case 2 -> 0.08;
            case 3 -> 0.14;
            default -> 0.0;
        };

        double finalChance = chance + extraChance;

        if (RANDOM.nextDouble() < finalChance) {
            unlockRandomLog(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLootingLevel(net.minecraftforge.event.entity.living.LootingLevelEvent event) {
        if (!org.xeb.xeb.Config.enabled) return;
        if (event.getDamageSource() != null && event.getDamageSource().getEntity() instanceof ServerPlayer player) {
            if (event.getEntity() != null && !MedallionManager.getMedallions(event.getEntity()).isEmpty()) {
                net.minecraft.world.item.ItemStack weapon = player.getMainHandItem();
                int lvl = weapon.getEnchantmentLevel(org.xeb.xeb.enchantment.ModEnchantments.MEDALLERO.get());
                if (lvl > 0) {
                    event.setLootingLevel(event.getLootingLevel() + lvl);
                }
            }
        }
    }

    public static void unlockRandomLog(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        List<Integer> lockedLogs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            if (!tag.getBoolean("xebUnlockedBitacora" + i)) {
                lockedLogs.add(i);
            }
        }

        // If the player still has locked logs, unlock one randomly
        if (!lockedLogs.isEmpty()) {
            int toUnlock = lockedLogs.get(RANDOM.nextInt(lockedLogs.size()));
            tag.putBoolean("xebUnlockedBitacora" + toUnlock, true);
            syncBitacoras(player, toUnlock);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncBitacoras(serverPlayer, -1);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncBitacoras(serverPlayer, -1);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncBitacoras(serverPlayer, -1);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            CompoundTag oldData = event.getOriginal().getPersistentData();
            CompoundTag newData = event.getEntity().getPersistentData();
            for (int i = 1; i <= 5; i++) {
                String key = "xebUnlockedBitacora" + i;
                if (oldData.contains(key)) {
                    newData.putBoolean(key, oldData.getBoolean(key));
                }
            }
        }
    }

    public static void syncBitacoras(ServerPlayer player, int newlyUnlocked) {
        boolean[] unlocked = new boolean[5];
        CompoundTag tag = player.getPersistentData();
        for (int i = 0; i < 5; i++) {
            unlocked[i] = tag.getBoolean("xebUnlockedBitacora" + (i + 1));
        }
        XEBNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new org.xeb.xeb.network.EnigmaBiosSyncPacket(unlocked, newlyUnlocked)
        );
    }
}
