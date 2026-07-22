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

    public static final ForgeConfigSpec.IntValue PERMANIGHT_VOTE_THRESHOLD_PERCENT = BUILDER
            .comment("Percentage of online players required to approve Moon Tear Permanight activation in multiplayer (0 to 100). Default: 50%.")
            .defineInRange("permanight.voteThresholdPercent", 50, 0, 100);

    public static final ForgeConfigSpec.IntValue PERMANIGHT_VOTE_DURATION_SECONDS = BUILDER
            .comment("Duration in seconds of the Moon Tear vote in multiplayer.")
            .defineInRange("permanight.voteDurationSeconds", 30, 5, 120);

    // HUD Group
    public static final ForgeConfigSpec.IntValue OPTIC_BLAST_HUD_X = BUILDER
            .comment("X offset of the active ability cooldowns HUD (Default 10, relative to bottom-left).")
            .defineInRange("hud.opticBlastHudX", 10, 0, 10000);

    public static final ForgeConfigSpec.IntValue OPTIC_BLAST_HUD_Y = BUILDER
            .comment("Y offset of the active ability cooldowns HUD (Default 42, relative to bottom-left, subtracted from screen height).")
            .defineInRange("hud.opticBlastHudY", 42, 0, 10000);

    // Elite Loot Group
    public static final ForgeConfigSpec.BooleanValue LOOT_DROPS_ENABLED = BUILDER
            .comment("Whether elite mobs drop Bits and Essences.")
            .define("eliteLoot.enabled", true);

    public static final ForgeConfigSpec.DoubleValue BRONZE_BIT_DROP_CHANCE = BUILDER
            .comment("Chance for Bronze Bit drop from Bronze medallion mobs (0.0 to 1.0).")
            .defineInRange("eliteLoot.bronzeBitChance", 0.40D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue SILVER_BIT_DROP_CHANCE = BUILDER
            .comment("Chance for Silver Bit drop from Silver medallion mobs (0.0 to 1.0).")
            .defineInRange("eliteLoot.silverBitChance", 0.55D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue GOLD_BIT_DROP_CHANCE = BUILDER
            .comment("Chance for Gold Bit drop from Gold medallion mobs (0.0 to 1.0).")
            .defineInRange("eliteLoot.goldBitChance", 0.75D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue ESSENCE_DROP_CHANCE = BUILDER
            .comment("Base chance for Essence drop (modified by medallion tier).")
            .defineInRange("eliteLoot.essenceChance", 0.08D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.BooleanValue BOSS_BIT_GUARANTEED = BUILDER
            .comment("If true, bosses have +20% chance to drop bits.")
            .define("eliteLoot.bossBitGuaranteed", true);

    public static final ForgeConfigSpec.BooleanValue TCONSTRUCT_INTEGRATION_ENABLED = BUILDER
            .comment("If true and Tinkers' Construct is loaded, register elite materials and modifiers.")
            .define("eliteLoot.tconstructIntegration", true);

    // Weapons & Relics Group
    public static final ForgeConfigSpec.DoubleValue MECHA_VULCAN_DAMAGE = BUILDER
            .comment("Base damage dealt by Mecha Vulcan projectiles.")
            .defineInRange("weaponsAndRelics.mechaVulcanDamage", 2.0D, 0.5D, 100.0D);

    public static final ForgeConfigSpec.DoubleValue HOMING_MISSILE_DAMAGE = BUILDER
            .comment("Base explosion damage dealt by Homing Missiles.")
            .defineInRange("weaponsAndRelics.homingMissileDamage", 6.0D, 1.0D, 200.0D);

    public static final ForgeConfigSpec.DoubleValue DOOMFIST_SLAM_RADIUS = BUILDER
            .comment("Impact radius (in blocks) of the Doomfist Seismic Slam.")
            .defineInRange("weaponsAndRelics.doomfistSlamRadius", 4.5D, 1.0D, 15.0D);

    public static final ForgeConfigSpec.DoubleValue DEMON_CORE_RADIATION_RADIUS = BUILDER
            .comment("Lethal radiation radius (in blocks) of the Demon Core when open.")
            .defineInRange("weaponsAndRelics.demonCoreRadiationRadius", 8.0D, 2.0D, 32.0D);

    public static final ForgeConfigSpec.IntValue DEMON_CORE_DOOMED_DURATION = BUILDER
            .comment("Duration in seconds of the Doomed debuff applied by Demon Core.")
            .defineInRange("weaponsAndRelics.demonCoreDoomedDurationSeconds", 10, 1, 120);

    public static final ForgeConfigSpec.DoubleValue CRAZY_DIAMOND_REACH_DISTANCE = BUILDER
            .comment("Extended reach distance (in blocks) of the Crazy Diamond Stand.")
            .defineInRange("weaponsAndRelics.crazyDiamondReachDistance", 6.0D, 3.0D, 12.0D);

    // Medallion Buffs Group
    public static final ForgeConfigSpec.DoubleValue SPIKY_REFLECT_PERCENTAGE = BUILDER
            .comment("Damage reflection percentage for the Spiky elite buff (0.25 = 25%).")
            .defineInRange("medallionBuffs.spikyReflectPercentage", 0.25D, 0.05D, 2.0D);

    public static final ForgeConfigSpec.DoubleValue UNDYING_REVIVE_HEALTH_PERCENT = BUILDER
            .comment("Health percentage granted upon reviving with Undying buff (0.50 = 50%).")
            .defineInRange("medallionBuffs.undyingReviveHealthPercent", 0.50D, 0.10D, 1.0D);

    public static final ForgeConfigSpec.IntValue INFESTED_FLIES_SPAWN_COUNT = BUILDER
            .comment("Number of elite flies spawned on hurt/death with Infested buff.")
            .defineInRange("medallionBuffs.infestedFliesSpawnCount", 2, 1, 10);

    public static final ForgeConfigSpec.DoubleValue MIRROR_PROJECTILE_REFLECT_CHANCE = BUILDER
            .comment("Chance to reflect incoming projectiles with Mirror buff (0.75 = 75%).")
            .defineInRange("medallionBuffs.mirrorProjectileReflectChance", 0.75D, 0.10D, 1.0D);

    // Beam Struggle Group
    public static final ForgeConfigSpec.IntValue BEAM_STRUGGLE_MAX_DURATION = BUILDER
            .comment("Maximum duration in seconds of a Beam Struggle clash.")
            .defineInRange("beamStruggle.maxDurationSeconds", 15, 5, 60);

    public static final ForgeConfigSpec.DoubleValue BEAM_STRUGGLE_CLICK_POWER = BUILDER
            .comment("Power multiplier per click for players during Beam Struggle clashes.")
            .defineInRange("beamStruggle.clickPowerMultiplier", 1.0D, 0.1D, 10.0D);

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
    public static int permanightVoteThresholdPercent = 50;
    public static int permanightVoteDurationSeconds = 30;

    // HUD position & scale config
    public static int doomfistHudX = 10;
    public static int doomfistHudY = 42;
    public static float doomfistHudScale = 1.0f;

    public static int opticBlastHudX = 10;
    public static int opticBlastHudY = 42;
    public static float opticBlastHudScale = 1.0f;

    public static int mechaHudX = 10;
    public static int mechaHudY = 42;
    public static float mechaHudScale = 1.0f;

    public static int holyHudX = 10;
    public static int holyHudY = 42;
    public static float holyHudScale = 1.0f;

    public static int goldenFlowerHudX = 10;
    public static int goldenFlowerHudY = 42;
    public static float goldenFlowerHudScale = 1.0f;

    public static int crazyDiamondHudX = 10;
    public static int crazyDiamondHudY = 42;
    public static float crazyDiamondHudScale = 1.0f;

    public static int theTearsHudX = 10;
    public static int theTearsHudY = 42;
    public static float theTearsHudScale = 1.0f;

    // Elite Loot config static variables
    public static boolean lootDropsEnabled = true;
    public static double bronzeBitDropChance = 0.40D;
    public static double silverBitDropChance = 0.55D;
    public static double goldBitDropChance = 0.75D;
    public static double essenceDropChance = 0.08D;
    public static boolean bossBitGuaranteed = true;
    public static boolean tconstructIntegrationEnabled = true;

    // Weapons & Relics static variables
    public static double mechaVulcanDamage = 2.0D;
    public static double homingMissileDamage = 6.0D;
    public static double doomfistSlamRadius = 4.5D;
    public static double demonCoreRadiationRadius = 8.0D;
    public static int demonCoreDoomedDurationSeconds = 10;
    public static double crazyDiamondReachDistance = 6.0D;

    // Medallion Buffs static variables
    public static double spikyReflectPercentage = 0.25D;
    public static double undyingReviveHealthPercent = 0.50D;
    public static int infestedFliesSpawnCount = 2;
    public static double mirrorProjectileReflectChance = 0.75D;

    // Beam Struggle static variables
    public static int beamStruggleMaxDurationSeconds = 15;
    public static double beamStruggleClickPower = 1.0D;

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
        permanightVoteThresholdPercent = PERMANIGHT_VOTE_THRESHOLD_PERCENT.get();
        permanightVoteDurationSeconds = PERMANIGHT_VOTE_DURATION_SECONDS.get();

        // Load HUD config
        opticBlastHudX = OPTIC_BLAST_HUD_X.get();
        opticBlastHudY = OPTIC_BLAST_HUD_Y.get();

        // Load Elite Loot config
        lootDropsEnabled = LOOT_DROPS_ENABLED.get();
        bronzeBitDropChance = BRONZE_BIT_DROP_CHANCE.get();
        silverBitDropChance = SILVER_BIT_DROP_CHANCE.get();
        goldBitDropChance = GOLD_BIT_DROP_CHANCE.get();
        essenceDropChance = ESSENCE_DROP_CHANCE.get();
        bossBitGuaranteed = BOSS_BIT_GUARANTEED.get();
        tconstructIntegrationEnabled = TCONSTRUCT_INTEGRATION_ENABLED.get();

        // Load Weapons & Relics config
        mechaVulcanDamage = MECHA_VULCAN_DAMAGE.get();
        homingMissileDamage = HOMING_MISSILE_DAMAGE.get();
        doomfistSlamRadius = DOOMFIST_SLAM_RADIUS.get();
        demonCoreRadiationRadius = DEMON_CORE_RADIATION_RADIUS.get();
        demonCoreDoomedDurationSeconds = DEMON_CORE_DOOMED_DURATION.get();
        crazyDiamondReachDistance = CRAZY_DIAMOND_REACH_DISTANCE.get();

        // Load Medallion Buffs config
        spikyReflectPercentage = SPIKY_REFLECT_PERCENTAGE.get();
        undyingReviveHealthPercent = UNDYING_REVIVE_HEALTH_PERCENT.get();
        infestedFliesSpawnCount = INFESTED_FLIES_SPAWN_COUNT.get();
        mirrorProjectileReflectChance = MIRROR_PROJECTILE_REFLECT_CHANCE.get();

        // Load Beam Struggle config
        beamStruggleMaxDurationSeconds = BEAM_STRUGGLE_MAX_DURATION.get();
        beamStruggleClickPower = BEAM_STRUGGLE_CLICK_POWER.get();
    }
}
