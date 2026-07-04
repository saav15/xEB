package org.xeb.xeb.buff;

import net.minecraft.util.RandomSource;
import java.util.*;

public class EliteBuffRegistry {
    private static final Map<String, EliteBuff> REGISTRY = new LinkedHashMap<>();

    public static void register(EliteBuff buff) {
        REGISTRY.put(buff.getId(), buff);
    }

    public static EliteBuff getById(String id) {
        return REGISTRY.get(id);
    }

    public static Collection<EliteBuff> getAll() {
        return REGISTRY.values();
    }

    public static List<EliteBuff> getEligible(boolean isBoss) {
        List<EliteBuff> eligible = new ArrayList<>();
        for (EliteBuff buff : REGISTRY.values()) {
            if (isBoss) {
                if (buff.getBuffType() == BuffType.BOSS_ONLY || buff.getBuffType() == BuffType.UNIVERSAL) {
                    eligible.add(buff);
                }
            } else {
                if (buff.getBuffType() == BuffType.ENEMY_ONLY || buff.getBuffType() == BuffType.UNIVERSAL) {
                    eligible.add(buff);
                }
            }
        }
        return eligible;
    }

    public static EliteBuff getRandomByWeight(RandomSource random, boolean isBoss, List<String> excludeIds) {
        List<EliteBuff> eligible = getEligible(isBoss);
        // Exclude already assigned ids
        eligible.removeIf(buff -> excludeIds.contains(buff.getId()));

        if (eligible.isEmpty()) return null;

        double totalWeight = 0;
        for (EliteBuff buff : eligible) {
            totalWeight += 1.0D / buff.getWeight();
        }

        if (totalWeight <= 0) return null;

        double roll = random.nextDouble() * totalWeight;
        double currentSum = 0;

        for (EliteBuff buff : eligible) {
            currentSum += 1.0D / buff.getWeight();
            if (roll <= currentSum) {
                return buff;
            }
        }

        return eligible.get(eligible.size() - 1);
    }
}
