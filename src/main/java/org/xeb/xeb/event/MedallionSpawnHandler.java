package org.xeb.xeb.event;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.compat.ModCompatManager;
import org.xeb.xeb.medallion.MedallionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MedallionSpawnHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!org.xeb.xeb.Config.enabled) return;

        if (event.getLevel().isClientSide()) {
            if (event.getEntity() instanceof LivingEntity living) {
                net.minecraft.nbt.ListTag pending = org.xeb.xeb.network.ClientPacketHandler.getPendingSync(living.getId());
                if (pending != null) {
                    living.getPersistentData().put(org.xeb.xeb.medallion.MedallionManager.MEDALLIONS_KEY, pending);
                    try {
                        living.refreshDimensions();
                    } catch (Exception ignored) {}
                }
            }
            return;
        }
        
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            // Check if eligible
            if (ModCompatManager.isEligible(living)) {
                if (living.getPersistentData().contains(MedallionManager.MEDALLIONS_KEY)) {
                    java.util.List<org.xeb.xeb.medallion.MedallionData> medallions = MedallionManager.getMedallions(living);
                    for (org.xeb.xeb.medallion.MedallionData m : medallions) {
                        try {
                            m.getBuff().onAttach(living, m.getUniqueId());
                        } catch (Exception e) {
                            // safeguard
                        }
                    }
                    MedallionManager.refreshDimensionsIfNeeded(living, medallions);
                    MedallionManager.syncToTracking(living);
                } else {
                    MedallionManager.assignRandomMedallions(living, (ServerLevel) event.getLevel());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntitySize(net.minecraftforge.event.entity.EntityEvent.Size event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            try {
                java.util.List<org.xeb.xeb.medallion.MedallionData> medallions = MedallionManager.getMedallions(living);
                if (!medallions.isEmpty()) {
                    int megaCount = 0;
                    for (org.xeb.xeb.medallion.MedallionData m : medallions) {
                        if (m.getBuff().getId().equals("mega")) {
                            megaCount++;
                        }
                    }
                    if (megaCount > 0) {
                        float scaleFactor = 1.0F + 0.30F * megaCount;
                        net.minecraft.world.entity.EntityDimensions original = event.getNewSize();
                        event.setNewSize(original.scale(scaleFactor));
                        event.setNewEyeHeight(event.getNewEyeHeight() * scaleFactor);
                    }
                }
            } catch (Exception e) {
                // Safeguard against early initialization issues
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (event.getTarget() instanceof LivingEntity target) {
                MedallionManager.syncToPlayer(target, serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onAdvancementEarn(net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent event) {
        if (!org.xeb.xeb.Config.enabled || !org.xeb.xeb.Config.eliteMeterEnabled) return;
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            int currentLevel = MedallionManager.getEliteMeterLevel(serverPlayer, false);
            net.minecraft.nbt.CompoundTag data = serverPlayer.getPersistentData();
            int lastNotifiedLevel = data.contains("xebLastNotifiedLevel") ? data.getInt("xebLastNotifiedLevel") : 1;
            
            if (currentLevel > lastNotifiedLevel) {
                data.putInt("xebLastNotifiedLevel", currentLevel);
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.xeb.elitemeter.up", currentLevel));
            }
        }
    }

    public static void grantAdvancement(net.minecraft.server.level.ServerPlayer player, String advancementId) {
        net.minecraft.resources.ResourceLocation id = new net.minecraft.resources.ResourceLocation(advancementId);
        net.minecraft.advancements.Advancement adv = player.getServer().getAdvancements().getAdvancement(id);
        if (adv != null) {
            net.minecraft.server.PlayerAdvancements advancements = player.getAdvancements();
            for (String criteria : adv.getCriteria().keySet()) {
                advancements.award(adv, criteria);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            org.xeb.xeb.world.PermanightSavedData data = org.xeb.xeb.world.PermanightSavedData.get(serverPlayer.serverLevel());
            org.xeb.xeb.network.XEBNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer), 
                    new org.xeb.xeb.network.PermanightSyncPacket(data.isActive()));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if (!org.xeb.xeb.Config.enabled || !org.xeb.xeb.Config.permanightEnabled) return;
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.level instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                org.xeb.xeb.world.PermanightSavedData data = org.xeb.xeb.world.PermanightSavedData.get(serverLevel);
                
                if (data.isActive()) {
                    int remaining = data.getTicksRemaining();
                    if (remaining > 0) {
                        data.setTicksRemaining(remaining - 1);
                        serverLevel.setDayTime(18000L); // lock time visually to midnight
                    } else {
                        data.setActive(false);
                        
                        org.xeb.xeb.network.XEBNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), 
                                new org.xeb.xeb.network.PermanightSyncPacket(false));
                                
                        serverLevel.players().forEach(player -> {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.xeb.permanight.end"));
                            grantAdvancement(player, "xeb:survive_permanight");
                        });
                    }
                } else {
                    long dayTime = serverLevel.getDayTime() % 24000L;
                    if (dayTime == 13000L) {
                        boolean trigger = false;
                        if (data.isForceNextNight()) {
                            trigger = true;
                            data.setForceNextNight(false);
                        } else {
                            if (serverLevel.random.nextDouble() < org.xeb.xeb.Config.permanightChance) {
                                trigger = true;
                            }
                        }
                        
                        if (trigger) {
                            data.setActive(true);
                            data.setTicksRemaining(24000);
                            
                            serverLevel.playSound(null, serverLevel.players().isEmpty() ? 0 : serverLevel.players().get(0).getX(), 
                                    serverLevel.players().isEmpty() ? 0 : serverLevel.players().get(0).getY(), 
                                    serverLevel.players().isEmpty() ? 0 : serverLevel.players().get(0).getZ(), 
                                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 5.0F, 0.5F);
                                    
                            serverLevel.players().forEach(p -> {
                                p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.xeb.permanight.start"));
                            });
                            
                            org.xeb.xeb.network.XEBNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), 
                                    new org.xeb.xeb.network.PermanightSyncPacket(true));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(net.minecraftforge.event.entity.living.LivingExperienceDropEvent event) {
        if (!org.xeb.xeb.Config.enabled || !org.xeb.xeb.Config.permanightEnabled) return;
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            if (org.xeb.xeb.world.PermanightSavedData.get(serverLevel).isActive()) {
                event.setDroppedExperience(event.getDroppedExperience() * 2);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (!org.xeb.xeb.Config.enabled || !org.xeb.xeb.Config.permanightEnabled) return;
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            if (org.xeb.xeb.world.PermanightSavedData.get(serverLevel).isActive()) {
                for (net.minecraft.world.entity.item.ItemEntity itemEntity : event.getDrops()) {
                    net.minecraft.world.item.ItemStack stack = itemEntity.getItem();
                    int count = stack.getCount();
                    int extra = 0;
                    for (int k = 0; k < count; k++) {
                        if (serverLevel.random.nextDouble() < 0.30) {
                            extra++;
                        }
                    }
                    if (extra > 0) {
                        stack.setCount(count + extra);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer killer) {
            LivingEntity victim = event.getEntity();
            if (victim.getPersistentData().contains(MedallionManager.MEDALLIONS_KEY)) {
                java.util.List<org.xeb.xeb.medallion.MedallionData> meds = MedallionManager.getMedallions(victim);
                if (meds.size() >= 3) {
                    grantAdvancement(killer, "xeb:elite_slayer_3");
                }
            }
        }
    }
}
