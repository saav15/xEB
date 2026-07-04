package org.xeb.xeb;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Whether the xEB Elite Buffs system is enabled.")
            .define("enabled", true);

    public static final ForgeConfigSpec.BooleanValue MEDALLION_RENDER_ENABLED = BUILDER
            .comment("Whether to render the medallion models above elite mob heads.")
            .define("render.medallionEnabled", true);

    public static final ForgeConfigSpec.DoubleValue MEDALLION_SIZE_SCALE = BUILDER
            .comment("Size scale for the medallion models.")
            .defineInRange("render.medallionSizeScale", 1.0D, 0.5D, 2.0D);

    public static final ForgeConfigSpec.BooleanValue COLOR_OVERLAY_ENABLED = BUILDER
            .comment("Whether to overlay colors corresponding to active buffs on elite mobs.")
            .define("render.colorOverlayEnabled", true);

    public static final ForgeConfigSpec.BooleanValue GLOW_EYES_ENABLED = BUILDER
            .comment("Whether to render glowing eyes matching the elite buff color.")
            .define("render.glowEyesEnabled", true);

    public static final ForgeConfigSpec.DoubleValue MEDALLION_RENDER_DISTANCE = BUILDER
            .comment("Maximum distance in blocks to render the medallion models above elite mob heads (in blocks). 3 chunks = 48 blocks.")
            .defineInRange("render.medallionRenderDistance", 48.0D, 1.0D, 256.0D);

    // Weapon Detection Group
    public static final ForgeConfigSpec.DoubleValue WEAPON_ATTRIBUTE_CONFIDENCE_THRESHOLD = BUILDER
            .comment("Minimum confidence score (0.0-1.0) for attribute-based weapon classification.")
            .defineInRange("weaponDetection.attributeConfidenceThreshold", 0.6, 0.0, 1.0);

    public static final ForgeConfigSpec.ConfigValue<String> MOD_MATERIAL_TIER_MAPPING = BUILDER
            .comment("Maps mod material names to tier values (0-10). Format: 'material=tier,...'")
            .define("weaponDetection.modMaterialTierMapping", "adamantine=5,cobalt=3,manyullyn=4,queens_slime=4,fiery=4,nebular=5");

    public static final ForgeConfigSpec.IntValue HOTBAR_SCAN_INTERVAL_TICKS = BUILDER
            .comment("How often (in ticks) the hotbar is rescanned for weapon selection.")
            .defineInRange("weaponDetection.hotbarScanIntervalTicks", 10, 1, 100);

    public static final ForgeConfigSpec.DoubleValue LOW_DURABILITY_THRESHOLD = BUILDER
            .comment("Weapons below this durability percentage get a score penalty.")
            .defineInRange("weaponDetection.lowDurabilityThreshold", 0.1, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue NO_AMMO_RANGED_PENALTY = BUILDER
            .comment("Score penalty for ranged weapons when no ammo is available.")
            .defineInRange("weaponDetection.noAmmoRangedPenalty", -50.0, -100.0, 0.0);

    // Boss AI Group
    public static final ForgeConfigSpec.DoubleValue BOSS_HEALTH_THRESHOLD = BUILDER
            .comment("Entities with max health >= this value are potential bosses.")
            .defineInRange("bossAI.healthThreshold", 300.0, 50.0, 10000.0);

    public static final ForgeConfigSpec.BooleanValue FORCE_ATTACK_ON_FROZEN_BOSS = BUILDER
            .comment("If true, frozen bosses deal damage directly to targets as a last resort.")
            .define("bossAI.forceAttackOnFrozenBoss", false);

    public static final ForgeConfigSpec.BooleanValue BOSS_ATTACK_ALL_MOBS = BUILDER
            .comment("If true, bosses under Madness attack any living entity (not just players/bosses/elites).")
            .define("bossAI.bossAttackAllMobs", false);

    public static final ForgeConfigSpec.IntValue FROZEN_DETECTION_TIMEOUT_TICKS = BUILDER
            .comment("How long (ticks) a boss must be idle before being marked as 'frozen'.")
            .defineInRange("bossAI.frozenDetectionTimeoutTicks", 60, 20, 600);

    public static final ForgeConfigSpec.IntValue FORCED_MOVEMENT_DURATION_TICKS = BUILDER
            .comment("How long (ticks) forced goal injection lasts before restoring original AI.")
            .defineInRange("bossAI.forcedMovementDurationTicks", 30, 10, 100);

    public static final ForgeConfigSpec.IntValue TARGET_EXPANSION_DURATION_TICKS = BUILDER
            .comment("How long (ticks) to expand boss target candidates to all mobs when no valid targets found.")
            .defineInRange("bossAI.targetExpansionDurationTicks", 100, 20, 600);

    public static final ForgeConfigSpec.IntValue BOSS_DETECTION_CACHE_TICKS = BUILDER
            .comment("How long (ticks) to cache boss detection results per entity type.")
            .defineInRange("bossAI.bossDetectionCacheTicks", 600, 100, 6000);

    public static final ForgeConfigSpec.ConfigValue<String> MADNESS_BLACKLISTED_ENTITIES = BUILDER
            .comment("Registry names of entities blacklisted from Madness targeting, comma-separated.")
            .define("bossAI.madnessBlacklistedEntities", "");

    // Better Combat Group
    public static final ForgeConfigSpec.BooleanValue ENABLE_BETTER_COMBAT_INTEGRATION = BUILDER
            .comment("Enable Better Combat-aware weapon styles, combo timing, and special abilities.")
            .define("betterCombat.enableIntegration", true);

    public static final ForgeConfigSpec.BooleanValue AUTO_TRIGGER_LEAP = BUILDER
            .comment("Automatically jump before attacking with leap weapons.")
            .define("betterCombat.autoTriggerLeap", true);

    public static final ForgeConfigSpec.BooleanValue AUTO_TRIGGER_THRUST = BUILDER
            .comment("Automatically sprint toward target before attacking with thrust weapons.")
            .define("betterCombat.autoTriggerThrust", true);

    public static final ForgeConfigSpec.DoubleValue EMERGENCY_SWITCH_SCORE_THRESHOLD = BUILDER
            .comment("If new weapon's score exceeds current by this much, switch immediately even mid-combo.")
            .defineInRange("betterCombat.emergencySwitchScoreThreshold", 20.0, 5.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue MELEE_RANGE_BUFFER_BLOCKS = BUILDER
            .comment("Extra blocks added to weapon reach for the melee/ranged decision buffer zone.")
            .defineInRange("combat.meleeRangeBufferBlocks", 1.5, 0.0, 5.0);

    // Right-Click Special Abilities Group (for modded weapons like Cataclysm, etc.)
    public static final ForgeConfigSpec.ConfigValue<String> RIGHT_CLICK_MODE = BUILDER
            .comment("How Madness triggers right-click specials on modded weapons.",
                    "PERIODIC = use on a fixed cooldown timer.",
                    "TACTICAL = use only when tactically advantageous (target low HP, multi-target, player hurt).",
                    "DISABLED = never use right-click specials.")
            .define("betterCombat.rightClickMode", "TACTICAL");

    public static final ForgeConfigSpec.IntValue RIGHT_CLICK_PERIODIC_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown (in ticks) between right-click special uses in PERIODIC mode. 100 ticks = 5 seconds.")
            .defineInRange("betterCombat.rightClickPeriodicCooldownTicks", 100, 20, 600);

    public static final ForgeConfigSpec.DoubleValue RIGHT_CLICK_TACTICAL_LOW_HP_THRESHOLD = BUILDER
            .comment("Target HP percentage below which TACTICAL mode will fire a right-click special (finisher).")
            .defineInRange("betterCombat.rightClickTacticalLowHpThreshold", 0.4, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue TACTICAL_MULTI_TARGET_RADIUS = BUILDER
            .comment("Radius in blocks to scan for additional enemies. If >= 2 are nearby, TACTICAL mode fires.")
            .defineInRange("betterCombat.tacticalMultiTargetRadius", 4.0D, 1.0D, 8.0D);

    // Progression Group
    public static final ForgeConfigSpec.BooleanValue ELITE_METER_ENABLED = BUILDER
            .comment("Whether the Elite Meter progression system is enabled.")
            .define("progression.eliteMeterEnabled", true);

    public static final ForgeConfigSpec.DoubleValue PLAYER_SCAN_RADIUS = BUILDER
            .comment("Search radius for players around the spawning mob (in blocks).")
            .defineInRange("progression.playerScanRadius", 64.0D, 8.0D, 256.0D);

    public static final ForgeConfigSpec.IntValue BOSS_MIN_LEVEL_REQUIRED = BUILDER
            .comment("Minimum Elite Meter level required for bosses to spawn with medallions.")
            .defineInRange("progression.bossMinLevelRequired", 7, 1, 100);

    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> ELITE_METER_ADVANCEMENTS = BUILDER
            .comment("Ordered list of advancement IDs for Elite Meter level progression (levels 1 to N).")
            .defineList("progression.eliteMeterAdvancements", java.util.List.of(
                    "minecraft:story/root",
                    "minecraft:story/mine_stone",
                    "minecraft:story/smelt_iron",
                    "minecraft:story/mine_diamond",
                    "minecraft:story/enchant_item",
                    "minecraft:story/enter_the_nether",
                    "minecraft:story/obtain_blaze_rod",
                    "minecraft:story/follow_ender_eye",
                    "minecraft:story/enter_the_end",
                    "minecraft:story/kill_dragon"
            ), obj -> obj instanceof String);

    // Permanight Group
    public static final ForgeConfigSpec.BooleanValue PERMANIGHT_ENABLED = BUILDER
            .comment("Whether the Elite Permanight random event is enabled.")
            .define("permanight.enabled", true);

    public static final ForgeConfigSpec.DoubleValue PERMANIGHT_CHANCE = BUILDER
            .comment("Chance per night for the Elite Permanight event to trigger (0.0 to 1.0).")
            .defineInRange("permanight.triggerChance", 0.03D, 0.0D, 1.0D);

    // HUD Group
    public static final ForgeConfigSpec.IntValue OPTIC_BLAST_HUD_X = BUILDER
            .comment("X offset of the active ability cooldowns HUD (Default 10, relative to bottom-left).")
            .defineInRange("hud.opticBlastHudX", 10, 0, 10000);

    public static final ForgeConfigSpec.IntValue OPTIC_BLAST_HUD_Y = BUILDER
            .comment("Y offset of the active ability cooldowns HUD (Default 42, relative to bottom-left, subtracted from screen height).")
            .defineInRange("hud.opticBlastHudY", 42, 0, 10000);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled = true;
    public static boolean medallionRenderEnabled = true;
    public static double medallionSizeScale = 1.0D;
    public static boolean colorOverlayEnabled = true;
    public static boolean glowEyesEnabled = true;
    public static double medallionRenderDistance = 48.0D;

    // Static variables for configuration
    public static double weaponAttributeConfidenceThreshold = 0.6;
    public static String modMaterialTierMapping = "adamantine=5,cobalt=3,manyullyn=4,queens_slime=4,fiery=4,nebular=5";
    public static int hotbarScanIntervalTicks = 10;
    public static double lowDurabilityThreshold = 0.1;
    public static double noAmmoRangedPenalty = -50.0;

    public static double bossHealthThreshold = 300.0;
    public static boolean forceAttackOnFrozenBoss = false;
    public static boolean bossAttackAllMobs = false;
    public static int frozenDetectionTimeoutTicks = 60;
    public static int forcedMovementDurationTicks = 30;
    public static int targetExpansionDurationTicks = 100;
    public static int bossDetectionCacheTicks = 600;
    public static String madnessBlacklistedEntities = "";

    public static boolean enableBetterCombatIntegration = true;
    public static boolean autoTriggerLeap = true;
    public static boolean autoTriggerThrust = true;
    public static double emergencySwitchScoreThreshold = 20.0;
    public static double meleeRangeBufferBlocks = 1.5;

    // Right-click special ability config
    public static String rightClickMode = "TACTICAL";
    public static int rightClickPeriodicCooldownTicks = 100;
    public static double rightClickTacticalLowHpThreshold = 0.4;
    public static double tacticalMultiTargetRadius = 4.0;

    // Progression config
    public static boolean eliteMeterEnabled = true;
    public static double playerScanRadius = 64.0D;
    public static int bossMinLevelRequired = 7;
    public static java.util.List<String> eliteMeterAdvancements = java.util.List.of();

    // Permanight config
    public static boolean permanightEnabled = true;
    public static double permanightChance = 0.03D;

    // HUD position config
    public static int opticBlastHudX = 10;
    public static int opticBlastHudY = 42;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enabled = ENABLED.get();
        medallionRenderEnabled = MEDALLION_RENDER_ENABLED.get();
        medallionSizeScale = MEDALLION_SIZE_SCALE.get();
        colorOverlayEnabled = COLOR_OVERLAY_ENABLED.get();
        glowEyesEnabled = GLOW_EYES_ENABLED.get();
        medallionRenderDistance = MEDALLION_RENDER_DISTANCE.get();

        // Load new configurations
        weaponAttributeConfidenceThreshold = WEAPON_ATTRIBUTE_CONFIDENCE_THRESHOLD.get();
        modMaterialTierMapping = MOD_MATERIAL_TIER_MAPPING.get();
        hotbarScanIntervalTicks = HOTBAR_SCAN_INTERVAL_TICKS.get();
        lowDurabilityThreshold = LOW_DURABILITY_THRESHOLD.get();
        noAmmoRangedPenalty = NO_AMMO_RANGED_PENALTY.get();

        bossHealthThreshold = BOSS_HEALTH_THRESHOLD.get();
        forceAttackOnFrozenBoss = FORCE_ATTACK_ON_FROZEN_BOSS.get();
        bossAttackAllMobs = BOSS_ATTACK_ALL_MOBS.get();
        frozenDetectionTimeoutTicks = FROZEN_DETECTION_TIMEOUT_TICKS.get();
        forcedMovementDurationTicks = FORCED_MOVEMENT_DURATION_TICKS.get();
        targetExpansionDurationTicks = TARGET_EXPANSION_DURATION_TICKS.get();
        bossDetectionCacheTicks = BOSS_DETECTION_CACHE_TICKS.get();
        madnessBlacklistedEntities = MADNESS_BLACKLISTED_ENTITIES.get();

        enableBetterCombatIntegration = ENABLE_BETTER_COMBAT_INTEGRATION.get();
        autoTriggerLeap = AUTO_TRIGGER_LEAP.get();
        autoTriggerThrust = AUTO_TRIGGER_THRUST.get();
        emergencySwitchScoreThreshold = EMERGENCY_SWITCH_SCORE_THRESHOLD.get();
        meleeRangeBufferBlocks = MELEE_RANGE_BUFFER_BLOCKS.get();

        // Load right-click special ability config
        rightClickMode = RIGHT_CLICK_MODE.get();
        rightClickPeriodicCooldownTicks = RIGHT_CLICK_PERIODIC_COOLDOWN_TICKS.get();
        rightClickTacticalLowHpThreshold = RIGHT_CLICK_TACTICAL_LOW_HP_THRESHOLD.get();
        tacticalMultiTargetRadius = TACTICAL_MULTI_TARGET_RADIUS.get();

        // Load progression config
        eliteMeterEnabled = ELITE_METER_ENABLED.get();
        playerScanRadius = PLAYER_SCAN_RADIUS.get();
        bossMinLevelRequired = BOSS_MIN_LEVEL_REQUIRED.get();
        // Safe cast to List<String>
        java.util.List<?> rawList = ELITE_METER_ADVANCEMENTS.get();
        java.util.List<String> list = new java.util.ArrayList<>();
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof String s) {
                    list.add(s);
                }
            }
        }
        eliteMeterAdvancements = list;

        // Load permanight config
        permanightEnabled = PERMANIGHT_ENABLED.get();
        permanightChance = PERMANIGHT_CHANCE.get();

        // Load HUD config
        opticBlastHudX = OPTIC_BLAST_HUD_X.get();
        opticBlastHudY = OPTIC_BLAST_HUD_Y.get();
    }
}
