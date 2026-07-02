package org.xeb.xeb.medallion;

import net.minecraft.world.Difficulty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class MedallionManagerTest {

    @BeforeAll
    public static void initRegistry() {
        try {
            net.minecraft.SharedConstants.tryDetectVersion();
            Field field = net.minecraft.server.Bootstrap.class.getDeclaredField("isBootstrapped");
            field.setAccessible(true);
            field.set(null, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- getSpawnChance tests ---

    @Test
    public void testSpawnChancePeacefulAlwaysZero() {
        assertEquals(0.0, MedallionManager.getSpawnChance(true, Difficulty.PEACEFUL));
        assertEquals(0.0, MedallionManager.getSpawnChance(false, Difficulty.PEACEFUL));
    }

    @Test
    public void testSpawnChanceBossHigherThanNonBoss() {
        for (Difficulty d : new Difficulty[]{Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD}) {
            double bossChance = MedallionManager.getSpawnChance(true, d);
            double nonBossChance = MedallionManager.getSpawnChance(false, d);
            assertTrue(bossChance > nonBossChance,
                "Boss spawn chance should exceed non-boss on " + d + ": " + bossChance + " vs " + nonBossChance);
        }
    }

    @Test
    public void testSpawnChanceIncreasesWithDifficulty() {
        double easyBoss = MedallionManager.getSpawnChance(true, Difficulty.EASY);
        double normalBoss = MedallionManager.getSpawnChance(true, Difficulty.NORMAL);
        double hardBoss = MedallionManager.getSpawnChance(true, Difficulty.HARD);

        assertTrue(hardBoss > normalBoss, "Hard boss chance should exceed normal");
        assertTrue(normalBoss > easyBoss, "Normal boss chance should exceed easy");

        double easyMob = MedallionManager.getSpawnChance(false, Difficulty.EASY);
        double normalMob = MedallionManager.getSpawnChance(false, Difficulty.NORMAL);
        double hardMob = MedallionManager.getSpawnChance(false, Difficulty.HARD);

        assertTrue(hardMob > normalMob, "Hard mob chance should exceed normal");
        assertTrue(normalMob > easyMob, "Normal mob chance should exceed easy");
    }

    @Test
    public void testSpawnChanceEasyValues() {
        assertEquals(0.40, MedallionManager.getSpawnChance(true, Difficulty.EASY));
        assertEquals(0.15, MedallionManager.getSpawnChance(false, Difficulty.EASY));
    }

    @Test
    public void testSpawnChanceNormalValues() {
        assertEquals(0.60, MedallionManager.getSpawnChance(true, Difficulty.NORMAL));
        assertEquals(0.25, MedallionManager.getSpawnChance(false, Difficulty.NORMAL));
    }

    @Test
    public void testSpawnChanceHardValues() {
        assertEquals(0.95, MedallionManager.getSpawnChance(true, Difficulty.HARD));
        assertEquals(0.60, MedallionManager.getSpawnChance(false, Difficulty.HARD));
    }

    // --- getMaxMedallions tests ---

    @Test
    public void testMaxMedallionsPeacefulAlwaysZero() {
        assertEquals(0, MedallionManager.getMaxMedallions(true, Difficulty.PEACEFUL));
        assertEquals(0, MedallionManager.getMaxMedallions(false, Difficulty.PEACEFUL));
    }

    @Test
    public void testMaxMedallionsIncreasesWithDifficulty() {
        int easyBoss = MedallionManager.getMaxMedallions(true, Difficulty.EASY);
        int normalBoss = MedallionManager.getMaxMedallions(true, Difficulty.NORMAL);
        int hardBoss = MedallionManager.getMaxMedallions(true, Difficulty.HARD);

        assertTrue(hardBoss >= normalBoss, "Hard max medallions should be >= normal");
        assertTrue(normalBoss >= easyBoss, "Normal max medallions should be >= easy");
        assertTrue(easyBoss >= 1, "Easy should allow at least 1 medallion");
    }

    @Test
    public void testMaxMedallionsExactValues() {
        assertEquals(1, MedallionManager.getMaxMedallions(true, Difficulty.EASY));
        assertEquals(2, MedallionManager.getMaxMedallions(true, Difficulty.NORMAL));
        assertEquals(3, MedallionManager.getMaxMedallions(true, Difficulty.HARD));

        assertEquals(1, MedallionManager.getMaxMedallions(false, Difficulty.EASY));
        assertEquals(2, MedallionManager.getMaxMedallions(false, Difficulty.NORMAL));
        assertEquals(3, MedallionManager.getMaxMedallions(false, Difficulty.HARD));
    }
}
