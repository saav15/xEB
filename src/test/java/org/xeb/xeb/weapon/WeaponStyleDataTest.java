package org.xeb.xeb.weapon;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WeaponStyleDataTest {

    @Test
    public void testLegacyConstructorDefaults() {
        WeaponStyleData data = new WeaponStyleData(3.0, 2, 1.5, List.of("sweep"), false);

        assertEquals(3.0, data.getAttackRange());
        assertEquals(2, data.getComboLength());
        assertEquals(1.5, data.getAttackSpeed());
        assertEquals(List.of("sweep"), data.getSpecialAbilities());
        assertFalse(data.isTwoHanded());
        assertTrue(data.getPerStepSpeeds().isEmpty());
        assertTrue(data.getPerStepAbilities().isEmpty());
        assertFalse(data.hasPerStepData());
    }

    @Test
    public void testComboLengthClampedToMinimumOne() {
        WeaponStyleData data = new WeaponStyleData(3.0, 0, 1.5, null, false);
        assertEquals(1, data.getComboLength());

        WeaponStyleData negData = new WeaponStyleData(3.0, -5, 1.5, null, false);
        assertEquals(1, negData.getComboLength());
    }

    @Test
    public void testNullSpecialAbilitiesBecomesEmptyList() {
        WeaponStyleData data = new WeaponStyleData(3.0, 1, 1.5, null, false);
        assertNotNull(data.getSpecialAbilities());
        assertTrue(data.getSpecialAbilities().isEmpty());
    }

    @Test
    public void testGetSpeedForStepUsesPerStepWhenAvailable() {
        List<Double> stepSpeeds = Arrays.asList(1.0, 2.0, 3.0);
        WeaponStyleData data = new WeaponStyleData(3.0, 3, 5.0, null, false, stepSpeeds, null);

        assertEquals(1.0, data.getSpeedForStep(0));
        assertEquals(2.0, data.getSpeedForStep(1));
        assertEquals(3.0, data.getSpeedForStep(2));
    }

    @Test
    public void testGetSpeedForStepFallsBackToAverage() {
        List<Double> stepSpeeds = Arrays.asList(1.0, 2.0);
        WeaponStyleData data = new WeaponStyleData(3.0, 3, 5.0, null, false, stepSpeeds, null);

        // Index 2 is out of range for perStepSpeeds
        assertEquals(5.0, data.getSpeedForStep(2));
        // Negative index falls back too
        assertEquals(5.0, data.getSpeedForStep(-1));
    }

    @Test
    public void testGetSpeedForStepFallsBackWhenZero() {
        List<Double> stepSpeeds = Arrays.asList(0.0, 2.0);
        WeaponStyleData data = new WeaponStyleData(3.0, 2, 5.0, null, false, stepSpeeds, null);

        // Step 0 has speed 0.0, which is not > 0, so falls back
        assertEquals(5.0, data.getSpeedForStep(0));
        assertEquals(2.0, data.getSpeedForStep(1));
    }

    @Test
    public void testGetAbilityForStepReturnsCorrectValue() {
        List<String> abilities = Arrays.asList("leap", null, "thrust");
        WeaponStyleData data = new WeaponStyleData(3.0, 3, 1.0, null, false,
                Collections.emptyList(), abilities);

        assertEquals("leap", data.getAbilityForStep(0));
        assertNull(data.getAbilityForStep(1));
        assertEquals("thrust", data.getAbilityForStep(2));
    }

    @Test
    public void testGetAbilityForStepOutOfRangeReturnsNull() {
        List<String> abilities = List.of("sweep");
        WeaponStyleData data = new WeaponStyleData(3.0, 3, 1.0, null, false,
                Collections.emptyList(), abilities);

        assertNull(data.getAbilityForStep(1));
        assertNull(data.getAbilityForStep(-1));
        assertNull(data.getAbilityForStep(100));
    }

    @Test
    public void testHasPerStepDataTrueWhenSpeedsProvided() {
        List<Double> stepSpeeds = Arrays.asList(1.0, 2.0);
        WeaponStyleData data = new WeaponStyleData(3.0, 2, 1.0, null, false, stepSpeeds, null);
        assertTrue(data.hasPerStepData());
    }

    @Test
    public void testHasPerStepDataFalseWhenEmpty() {
        WeaponStyleData data = new WeaponStyleData(3.0, 2, 1.0, null, false,
                Collections.emptyList(), Collections.emptyList());
        assertFalse(data.hasPerStepData());
    }

    @Test
    public void testTwoHandedFlag() {
        WeaponStyleData twoHanded = new WeaponStyleData(3.0, 1, 1.0, null, true);
        assertTrue(twoHanded.isTwoHanded());

        WeaponStyleData oneHanded = new WeaponStyleData(3.0, 1, 1.0, null, false);
        assertFalse(oneHanded.isTwoHanded());
    }

    @Test
    public void testSpecialAbilitiesAreDefensivelyCopied() {
        List<String> original = new java.util.ArrayList<>(List.of("sweep"));
        WeaponStyleData data = new WeaponStyleData(3.0, 1, 1.0, original, false);

        original.add("parry");
        assertEquals(1, data.getSpecialAbilities().size());
    }
}
