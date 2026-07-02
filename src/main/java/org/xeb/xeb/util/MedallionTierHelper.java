package org.xeb.xeb.util;

import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.Function;

/**
 * Extracts tier-based values from an entity's medallions, eliminating the
 * repeated pattern of iterating medallions, matching by buff ID, and switching
 * on the tier to produce an amplifier or other value.
 */
public final class MedallionTierHelper {

    private MedallionTierHelper() {}

    /**
     * Finds the first medallion with the given buff ID on the entity and maps
     * its tier to an integer via the supplied function.
     *
     * @param entity     the entity to inspect
     * @param buffId     the buff ID to match
     * @param tierMapper maps each MedallionType to the desired integer value
     * @param fallback   value returned if no matching medallion is found
     * @return the mapped value, or {@code fallback} if no match
     */
    public static int getTierValue(LivingEntity entity, String buffId,
                                   Function<MedallionType, Integer> tierMapper, int fallback) {
        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        for (MedallionData m : medallions) {
            if (m.getBuff().getId().equals(buffId)) {
                return tierMapper.apply(m.getTier());
            }
        }
        return fallback;
    }

    /**
     * Finds the tier of the first medallion matching the given buff ID.
     *
     * @param entity the entity to inspect
     * @param buffId the buff ID to match
     * @return the tier, or {@link MedallionType#COMMON} if not found
     */
    public static MedallionType getTier(LivingEntity entity, String buffId) {
        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        for (MedallionData m : medallions) {
            if (m.getBuff().getId().equals(buffId)) {
                return m.getTier();
            }
        }
        return MedallionType.COMMON;
    }
}
