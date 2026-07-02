package org.xeb.xeb.medallion;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class MedallionTypeTest {

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

    @Test
    public void testEnumValuesExist() {
        MedallionType[] values = MedallionType.values();
        assertEquals(3, values.length);
        assertNotNull(MedallionType.valueOf("COMMON"));
        assertNotNull(MedallionType.valueOf("RARE"));
        assertNotNull(MedallionType.valueOf("LEGENDARY"));
    }

    @Test
    public void testCommonColorIsBronze() {
        assertEquals(0xCD7F32, MedallionType.COMMON.getColor());
    }

    @Test
    public void testRareColorIsSilver() {
        assertEquals(0xC0C0C0, MedallionType.RARE.getColor());
    }

    @Test
    public void testLegendaryColorIsGold() {
        assertEquals(0xFFD700, MedallionType.LEGENDARY.getColor());
    }

    @Test
    public void testDisplayNameIsTranslatableComponent() {
        assertNotNull(MedallionType.COMMON.getDisplayName());
        assertNotNull(MedallionType.RARE.getDisplayName());
        assertNotNull(MedallionType.LEGENDARY.getDisplayName());
    }

    @Test
    public void testColorsAreDistinct() {
        int common = MedallionType.COMMON.getColor();
        int rare = MedallionType.RARE.getColor();
        int legendary = MedallionType.LEGENDARY.getColor();

        assertNotEquals(common, rare);
        assertNotEquals(common, legendary);
        assertNotEquals(rare, legendary);
    }

    @Test
    public void testValueOfThrowsForInvalid() {
        assertThrows(IllegalArgumentException.class, () -> MedallionType.valueOf("MYTHICAL"));
    }
}
