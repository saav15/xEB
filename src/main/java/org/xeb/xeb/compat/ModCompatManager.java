package org.xeb.xeb.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.compat.hooks.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModCompatManager {
    private static final Set<EntityType<?>> ELIGIBLE_TYPES = new HashSet<>();
    private static final Set<EntityType<?>> BOSS_TYPES = new HashSet<>();

    private static final TagKey<EntityType<?>> ELITE_ELIGIBLE_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(Xeb.MODID, "elite_eligible"));
    private static final TagKey<EntityType<?>> BOSSES_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(Xeb.MODID, "bosses"));

    private static final List<ModCompatAdapter> ADAPTERS = new ArrayList<>();

    public static List<ModCompatAdapter> getAdapters() {
        return ADAPTERS;
    }

    public static void registerEligible(EntityType<?> type) {
        if (type != null) ELIGIBLE_TYPES.add(type);
    }

    public static void registerBoss(EntityType<?> type) {
        if (type != null) {
            BOSS_TYPES.add(type);
            ELIGIBLE_TYPES.add(type); // bosses are also eligible
        }
    }

    public static boolean isEligible(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        // Check dynamic set
        if (ELIGIBLE_TYPES.contains(type)) return true;
        // Check JSON tags
        return type.is(ELITE_ELIGIBLE_TAG);
    }

    public static boolean isBoss(LivingEntity entity) {
        return org.xeb.xeb.boss.UniversalBossDetector.isBoss(entity);
    }

    public static boolean hasCurioOrOffhand(LivingEntity entity, net.minecraft.world.item.Item item) {
        if (entity == null) return false;
        if (entity.getOffhandItem().is(item)) {
            return true;
        }
        for (ModCompatAdapter adapter : ADAPTERS) {
            if (adapter instanceof org.xeb.xeb.compat.adapter.CuriosAdapter curiosAdapter) {
                if (curiosAdapter.isLoaded()) {
                    for (net.minecraft.world.item.ItemStack stack : curiosAdapter.getCuriosItems(entity)) {
                        if (stack.is(item)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static java.util.List<net.minecraft.world.item.ItemStack> getCuriosItems(LivingEntity entity) {
        java.util.List<net.minecraft.world.item.ItemStack> list = new java.util.ArrayList<>();
        if (entity != null) {
            for (ModCompatAdapter adapter : ADAPTERS) {
                if (adapter instanceof org.xeb.xeb.compat.adapter.CuriosAdapter curiosAdapter) {
                    if (curiosAdapter.isLoaded()) {
                        list.addAll(curiosAdapter.getCuriosItems(entity));
                    }
                }
            }
        }
        return list;
    }

    public static boolean hasHelmetOrCurio(LivingEntity entity, net.minecraft.world.item.Item item) {
        if (entity == null) return false;
        if (entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(item)) {
            return true;
        }
        for (ModCompatAdapter adapter : ADAPTERS) {
            if (adapter instanceof org.xeb.xeb.compat.adapter.CuriosAdapter curiosAdapter) {
                if (curiosAdapter.isLoaded()) {
                    for (net.minecraft.world.item.ItemStack stack : curiosAdapter.getCuriosItems(entity)) {
                        if (stack.is(item)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasHotPotato(LivingEntity entity) {
        if (entity == null) return false;
        for (ModCompatAdapter adapter : ADAPTERS) {
            if (adapter instanceof org.xeb.xeb.compat.adapter.CuriosAdapter curiosAdapter) {
                if (curiosAdapter.isLoaded()) {
                    for (net.minecraft.world.item.ItemStack stack : curiosAdapter.getCuriosItems(entity)) {
                        if (!stack.isEmpty() && stack.is(org.xeb.xeb.item.ModItems.HOT_POTATO.get())) {
                            return true;
                        }
                    }
                }
            }
        }
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (entity.getItemBySlot(slot).is(org.xeb.xeb.item.ModItems.HOT_POTATO.get())) {
                return true;
            }
        }
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(org.xeb.xeb.item.ModItems.HOT_POTATO.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isBossLegacy(LivingEntity entity) {
        if (entity == null) return false;
        EntityType<?> type = entity.getType();
        // Check dynamic set
        if (BOSS_TYPES.contains(type)) return true;
        // Check JSON tags
        if (type.is(BOSSES_TAG)) return true;
        // Fallback: Wither/Dragon or max health >= 100
        return entity.getMaxHealth() >= 100.0F;
    }

    public static boolean isBossInTransition(LivingEntity entity) {
        for (ModCompatAdapter adapter : ADAPTERS) {
            if (adapter instanceof org.xeb.xeb.compat.adapter.CataclysmAdapter cataclysmAdapter) {
                if (cataclysmAdapter.isLoaded() && cataclysmAdapter.isTransitioning(entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void init() {
        // Register adapters
        ADAPTERS.add(new org.xeb.xeb.bettercombat.BetterCombatCompatAdapter());
        ADAPTERS.add(new org.xeb.xeb.compat.adapter.CataclysmAdapter());
        ADAPTERS.add(new org.xeb.xeb.compat.adapter.EpicFightAdapter());
        ADAPTERS.add(new org.xeb.xeb.compat.adapter.IronSpellsAdapter());
        ADAPTERS.add(new org.xeb.xeb.compat.adapter.ApotheosisAdapter());
        ADAPTERS.add(new org.xeb.xeb.compat.adapter.TetraAdapter());
        ADAPTERS.add(new org.xeb.xeb.compat.adapter.TConstructAdapter());
        ADAPTERS.add(new org.xeb.xeb.compat.adapter.ArtifactsAdapter());
        ADAPTERS.add(new org.xeb.xeb.compat.adapter.AlexsMobsAdapter());
        ADAPTERS.add(new org.xeb.xeb.compat.adapter.CuriosAdapter());

        // Vanilla is always loaded
        new VanillaMobHook().registerTypes();

        if (ModList.get().isLoaded("twilightforest")) {
            new TwilightForestHook().registerTypes();
        }
        if (ModList.get().isLoaded("mowziesmobs")) {
            new MowziesMobsHook().registerTypes();
        }
        if (ModList.get().isLoaded("alexsmobs")) {
            new AlexMobsHook().registerTypes();
        }
        if (ModList.get().isLoaded("alexscaves")) {
            new AlexCavesHook().registerTypes();
        }
        if (ModList.get().isLoaded("mutantmonsters")) {
            new MutantMonstersHook().registerTypes();
        }
        if (ModList.get().isLoaded("cataclysm")) {
            new CataclysmHook().registerTypes();
        }
        if (ModList.get().isLoaded("born_in_chaos_v1")) {
            new BornInChaosHook().registerTypes();
        }
        if (ModList.get().isLoaded("aquamirae")) {
            new AquamiraeHook().registerTypes();
        }
        if (ModList.get().isLoaded("aether")) {
            new AetherHook().registerTypes();
        }
        if (ModList.get().isLoaded("bettercombat")) {
            new BetterCombatHook().registerTypes();
        }

        // Initialize external beam registry
        org.xeb.xeb.beamstruggle.ExternalBeamRegistry.init();
    }
}
