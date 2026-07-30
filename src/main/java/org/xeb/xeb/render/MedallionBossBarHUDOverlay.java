package org.xeb.xeb.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.entity.StevenBossEntity;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MedallionBossBarHUDOverlay {

    private static final Map<UUID, MedallionBossBar> BOSS_BAR_INSTANCES = new ConcurrentHashMap<>();
    private static final Map<UUID, AnimTracker> ANIM_TRACKERS = new ConcurrentHashMap<>();

    private static class AnimTracker {
        int animTicks = 0;
        int lastMadnessStacks = 0;
        int lastMedallionCount = 0;

        AnimTracker(int madStacks, int medCount) {
            this.animTicks = 0;
            this.lastMadnessStacks = madStacks;
            this.lastMedallionCount = medCount;
        }

        void update(int currentMadStacks, int currentMedCount) {
            if (currentMadStacks > lastMadnessStacks || currentMedCount > lastMedallionCount) {
                animTicks = Math.min(animTicks, 3);
                lastMadnessStacks = currentMadStacks;
                lastMedallionCount = currentMedCount;
            }
            if (animTicks < 15) {
                animTicks++;
            }
        }

        float getScale() {
            return Math.min(1.0F, animTicks / 10.0F);
        }
    }

    private static class PrioritizedBoss {
        final LivingEntity entity;
        final int priority;

        PrioritizedBoss(LivingEntity entity, int priority) {
            this.entity = entity;
            this.priority = priority;
        }
    }

    private static List<LivingEntity> getActiveBossesSorted() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return Collections.emptyList();

        AABB area = mc.player.getBoundingBox().inflate(96.0D);
        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (!entity.isAlive() || entity instanceof org.xeb.xeb.entity.StevenCloneEntity) return false;

            if (entity instanceof StevenBossEntity) return true;

            List<MedallionData> medallions = MedallionManager.getMedallions(entity);
            int madStacks = entity.getPersistentData().getInt("xebMadStacks");
            boolean hasMadness = MedallionManager.hasBuff(entity, "mad") ||
                    (entity.hasEffect(org.xeb.xeb.effect.ModEffects.MADNESS.get()));

            int megaCount = 0;
            for (MedallionData m : medallions) {
                if (m.getBuff() != null && m.getBuff().getId().equals("mega")) {
                    megaCount++;
                }
            }

            boolean isMega = megaCount >= 2 || entity.getPersistentData().getBoolean("xeb_is_mega");
            boolean isMadnessBoss = madStacks >= 2;

            return isMega || isMadnessBoss;
        });

        if (candidates.isEmpty()) return Collections.emptyList();

        List<PrioritizedBoss> prioritized = new ArrayList<>();
        for (LivingEntity entity : candidates) {
            int priority = calculatePriority(entity);
            prioritized.add(new PrioritizedBoss(entity, priority));
        }

        // Sort descending by priority (highest priority boss first!)
        prioritized.sort((a, b) -> Integer.compare(b.priority, a.priority));

        List<LivingEntity> result = new ArrayList<>();
        for (PrioritizedBoss pb : prioritized) {
            result.add(pb.entity);
        }
        return result;
    }

    private static int calculatePriority(LivingEntity entity) {
        if (entity instanceof StevenBossEntity steven) {
            return 10000 + steven.getStevenCharges() * 100;
        }

        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        int madStacks = entity.getPersistentData().getInt("xebMadStacks");
        boolean isMega = medallions.stream().filter(m -> m.getBuff() != null && m.getBuff().getId().equals("mega")).count() >= 2
                || entity.getPersistentData().getBoolean("xeb_is_mega");

        if (isMega && madStacks > 0) return 5000 + madStacks * 100;
        if (isMega) return 3000 + medallions.size() * 10;
        if (madStacks >= 2) return 1000 + madStacks * 50;

        return 100 + medallions.size() * 10;
    }

    @SubscribeEvent
    public static void onBossBarProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        BossEvent bossEvent = event.getBossEvent();
        if (bossEvent == null || bossEvent.getName() == null) return;

        String name = bossEvent.getName().getString();
        String cleanName = name.replace(" ", "").toUpperCase();

        if (cleanName.contains("STEVEN") || name.contains("[MEGA") || name.contains("[MADNESS") || name.contains("[MEDALLION") || name.contains("[ELITE")) {
            List<LivingEntity> bosses = getActiveBossesSorted();
            int spacing = Math.max(1, bosses.size()) * 40;
            event.setCanceled(true);
            event.setIncrement(spacing);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        List<LivingEntity> bosses = getActiveBossesSorted();
        if (bosses.isEmpty()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // 1. RENDER MAIN PRIMARY BOSS BAR (TOP AT Y=18)
        LivingEntity primaryBoss = bosses.get(0);
        UUID primaryUuid = primaryBoss.getUUID();
        List<MedallionData> primaryMeds = MedallionManager.getMedallions(primaryBoss);
        int primaryMad = primaryBoss.getPersistentData().getInt("xebMadStacks");
        boolean primaryIsMega = primaryMeds.stream().filter(m -> m.getBuff() != null && m.getBuff().getId().equals("mega")).count() >= 2
                || primaryBoss.getPersistentData().getBoolean("xeb_is_mega");

        AnimTracker primaryTracker = ANIM_TRACKERS.computeIfAbsent(primaryUuid, k -> new AnimTracker(primaryMad, primaryMeds.size()));
        primaryTracker.update(primaryMad, primaryMeds.size());

        MedallionBossBar.MetalTier primaryTier = resolveMetalTier(primaryMeds, primaryMad, primaryIsMega);
        float primaryHpRatio = primaryBoss.getHealth() / primaryBoss.getMaxHealth();

        if (primaryBoss instanceof StevenBossEntity steven) {
            float targetHpRatio = steven.getHealth() / steven.getMaxHealth();
            int charges = steven.getStevenCharges();
            String phaseTag = targetHpRatio < 0.25F ? "OVERDRIVE" : "";
            StevenBossBarHUDOverlay.STEVEN_BOSS_BAR.render(
                    event.getGuiGraphics(),
                    mc.font,
                    18,
                    "S T E V E N",
                    targetHpRatio,
                    (int) steven.getHealth(),
                    (int) steven.getMaxHealth(),
                    charges,
                    phaseTag,
                    XebBossBar.Theme.OBSIDIAN_COSMIC
            );
        } else {
            String mobName = primaryBoss.hasCustomName() ? primaryBoss.getCustomName().getString() : primaryBoss.getType().getDescription().getString();
            int displayCharges = primaryMad > 0 ? primaryMad : primaryMeds.size();

            MedallionBossBar barRenderer = BOSS_BAR_INSTANCES.computeIfAbsent(primaryUuid, k -> new MedallionBossBar());
            barRenderer.render(
                    event.getGuiGraphics(),
                    mc.font,
                    18,
                    mobName,
                    primaryHpRatio,
                    (int) primaryBoss.getHealth(),
                    (int) primaryBoss.getMaxHealth(),
                    displayCharges,
                    "",
                    primaryBoss,
                    primaryTracker.getScale(),
                    primaryTier
            );
        }

        // 2. RENDER SECONDARY COMPACT MINI BOSS BARS HORIZONTALLY SIDE-BY-SIDE UNDER MAIN BAR (Y=72)
        if (bosses.size() <= 1) return;

        List<LivingEntity> secondaryBosses = bosses.subList(1, bosses.size());
        int miniWidth = 105;
        int miniGap = 32;
        int maxPerRow = 3;

        int rowY = 72;
        int rowCount = (secondaryBosses.size() + maxPerRow - 1) / maxPerRow;

        for (int r = 0; r < rowCount; r++) {
            int startIndex = r * maxPerRow;
            int endIndex = Math.min(startIndex + maxPerRow, secondaryBosses.size());
            int countInThisRow = endIndex - startIndex;

            int rowTotalWidth = countInThisRow * miniWidth + (countInThisRow - 1) * miniGap;
            int startX = (screenWidth - rowTotalWidth) / 2;

            for (int col = 0; col < countInThisRow; col++) {
                LivingEntity boss = secondaryBosses.get(startIndex + col);
                UUID uuid = boss.getUUID();

                List<MedallionData> medallions = MedallionManager.getMedallions(boss);
                int madStacks = boss.getPersistentData().getInt("xebMadStacks");
                boolean isMega = medallions.stream().filter(m -> m.getBuff() != null && m.getBuff().getId().equals("mega")).count() >= 2
                        || boss.getPersistentData().getBoolean("xeb_is_mega");

                AnimTracker tracker = ANIM_TRACKERS.computeIfAbsent(uuid, k -> new AnimTracker(madStacks, medallions.size()));
                tracker.update(madStacks, medallions.size());

                MedallionBossBar.MetalTier tier = resolveMetalTier(medallions, madStacks, isMega);
                float hpRatio = boss.getHealth() / boss.getMaxHealth();

                int miniX = startX + col * (miniWidth + miniGap);

                if (boss instanceof StevenBossEntity steven) {
                    float targetHpRatio = steven.getHealth() / steven.getMaxHealth();
                    int charges = steven.getStevenCharges();
                    StevenBossBarHUDOverlay.STEVEN_BOSS_BAR.renderMini(
                            event.getGuiGraphics(),
                            mc.font,
                            miniX,
                            rowY,
                            "STEVEN",
                            targetHpRatio,
                            (int) steven.getHealth(),
                            (int) steven.getMaxHealth(),
                            charges,
                            "",
                            XebBossBar.Theme.OBSIDIAN_COSMIC
                    );
                } else {
                    String mobName = boss.hasCustomName() ? boss.getCustomName().getString() : boss.getType().getDescription().getString();
                    int displayCharges = madStacks > 0 ? madStacks : medallions.size();

                    MedallionBossBar barRenderer = BOSS_BAR_INSTANCES.computeIfAbsent(uuid, k -> new MedallionBossBar());
                    barRenderer.renderMini(
                            event.getGuiGraphics(),
                            mc.font,
                            miniX,
                            rowY,
                            mobName,
                            hpRatio,
                            (int) boss.getHealth(),
                            (int) boss.getMaxHealth(),
                            displayCharges,
                            "",
                            boss,
                            tracker.getScale(),
                            tier
                    );
                }
            }

            rowY += 32;
        }
    }

    private static MedallionType getHighestTier(List<MedallionData> medallions) {
        if (medallions.isEmpty()) return MedallionType.COMMON;
        MedallionType highest = MedallionType.COMMON;
        for (MedallionData m : medallions) {
            if (m.getTier().ordinal() > highest.ordinal()) {
                highest = m.getTier();
            }
        }
        return highest;
    }

    private static MedallionBossBar.MetalTier resolveMetalTier(List<MedallionData> medallions, int madStacks, boolean isMega) {
        if (madStacks >= 2) {
            return MedallionBossBar.MetalTier.MADNESS;
        }
        MedallionType highest = getHighestTier(medallions);
        switch (highest) {
            case LEGENDARY:
                return MedallionBossBar.MetalTier.GOLD;
            case RARE:
                return MedallionBossBar.MetalTier.SILVER;
            default:
                return isMega ? MedallionBossBar.MetalTier.GOLD : MedallionBossBar.MetalTier.BRONZE;
        }
    }
}
