package org.xeb.xeb.buff;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EliteBuffRegistryTest {

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

    @BeforeEach
    public void clearRegistry() throws Exception {
        Field registryField = EliteBuffRegistry.class.getDeclaredField("REGISTRY");
        registryField.setAccessible(true);
        ((Map<?, ?>) registryField.get(null)).clear();
    }

    private EliteBuff createBuff(String id, BuffType type, double weight) {
        return new EliteBuff(id, id, type, 0xFFFFFF, weight) {
            @Override public void onAttach(LivingEntity entity) {}
            @Override public void onDetach(LivingEntity entity) {}
            @Override public void onServerTick(LivingEntity entity, ServerLevel level) {}
        };
    }

    @Test
    public void testRegisterAndGetById() {
        EliteBuff buff = createBuff("test_buff", BuffType.UNIVERSAL, 1.0);
        EliteBuffRegistry.register(buff);

        assertSame(buff, EliteBuffRegistry.getById("test_buff"));
        assertNull(EliteBuffRegistry.getById("nonexistent"));
    }

    @Test
    public void testGetAllReturnsAllRegistered() {
        EliteBuff a = createBuff("a", BuffType.UNIVERSAL, 1.0);
        EliteBuff b = createBuff("b", BuffType.ENEMY_ONLY, 1.0);
        EliteBuffRegistry.register(a);
        EliteBuffRegistry.register(b);

        Collection<EliteBuff> all = EliteBuffRegistry.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(a));
        assertTrue(all.contains(b));
    }

    @Test
    public void testGetEligibleForBossIncludesBossOnlyAndUniversal() {
        EliteBuff bossOnly = createBuff("boss_only", BuffType.BOSS_ONLY, 1.0);
        EliteBuff enemyOnly = createBuff("enemy_only", BuffType.ENEMY_ONLY, 1.0);
        EliteBuff universal = createBuff("universal", BuffType.UNIVERSAL, 1.0);
        EliteBuffRegistry.register(bossOnly);
        EliteBuffRegistry.register(enemyOnly);
        EliteBuffRegistry.register(universal);

        List<EliteBuff> eligible = EliteBuffRegistry.getEligible(true);

        assertTrue(eligible.contains(bossOnly));
        assertTrue(eligible.contains(universal));
        assertFalse(eligible.contains(enemyOnly));
    }

    @Test
    public void testGetEligibleForNonBossIncludesEnemyOnlyAndUniversal() {
        EliteBuff bossOnly = createBuff("boss_only", BuffType.BOSS_ONLY, 1.0);
        EliteBuff enemyOnly = createBuff("enemy_only", BuffType.ENEMY_ONLY, 1.0);
        EliteBuff universal = createBuff("universal", BuffType.UNIVERSAL, 1.0);
        EliteBuffRegistry.register(bossOnly);
        EliteBuffRegistry.register(enemyOnly);
        EliteBuffRegistry.register(universal);

        List<EliteBuff> eligible = EliteBuffRegistry.getEligible(false);

        assertFalse(eligible.contains(bossOnly));
        assertTrue(eligible.contains(enemyOnly));
        assertTrue(eligible.contains(universal));
    }

    @Test
    public void testGetRandomByWeightReturnsNullWhenNoEligible() {
        RandomSource random = RandomSource.create(42L);
        EliteBuff result = EliteBuffRegistry.getRandomByWeight(random, true, Collections.emptyList());
        assertNull(result);
    }

    @Test
    public void testGetRandomByWeightExcludesIds() {
        EliteBuff a = createBuff("a", BuffType.UNIVERSAL, 1.0);
        EliteBuff b = createBuff("b", BuffType.UNIVERSAL, 1.0);
        EliteBuffRegistry.register(a);
        EliteBuffRegistry.register(b);

        RandomSource random = RandomSource.create(42L);
        EliteBuff result = EliteBuffRegistry.getRandomByWeight(random, false, List.of("a"));

        assertSame(b, result);
    }

    @Test
    public void testGetRandomByWeightReturnsNullWhenAllExcluded() {
        EliteBuff a = createBuff("a", BuffType.UNIVERSAL, 1.0);
        EliteBuffRegistry.register(a);

        RandomSource random = RandomSource.create(42L);
        EliteBuff result = EliteBuffRegistry.getRandomByWeight(random, false, List.of("a"));

        assertNull(result);
    }

    @Test
    public void testGetRandomByWeightRespectsWeights() {
        EliteBuff heavy = createBuff("heavy", BuffType.UNIVERSAL, 100.0);
        EliteBuff light = createBuff("light", BuffType.UNIVERSAL, 0.001);
        EliteBuffRegistry.register(heavy);
        EliteBuffRegistry.register(light);

        int heavyCount = 0;
        for (int i = 0; i < 200; i++) {
            RandomSource random = RandomSource.create(i);
            EliteBuff result = EliteBuffRegistry.getRandomByWeight(random, false, Collections.emptyList());
            if (result == heavy) heavyCount++;
        }

        assertTrue(heavyCount > 150, "Heavy buff (weight 100) should be selected far more often, got " + heavyCount + "/200");
    }

    @Test
    public void testRegisterOverwritesSameId() {
        EliteBuff first = createBuff("same_id", BuffType.UNIVERSAL, 1.0);
        EliteBuff second = createBuff("same_id", BuffType.BOSS_ONLY, 2.0);
        EliteBuffRegistry.register(first);
        EliteBuffRegistry.register(second);

        assertSame(second, EliteBuffRegistry.getById("same_id"));
        assertEquals(1, EliteBuffRegistry.getAll().size());
    }
}
