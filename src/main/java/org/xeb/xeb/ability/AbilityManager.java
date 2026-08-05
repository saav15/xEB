package org.xeb.xeb.ability;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor global de máquinas de estados de habilidades para entidades en xEB.
 */
public class AbilityManager {

    private static final Map<UUID, Map<String, AbilityStateMachine>> ENTITY_ABILITIES = new ConcurrentHashMap<>();

    public static AbilityStateMachine getOrCreate(LivingEntity entity, String abilityId) {
        UUID uuid = entity.getUUID();
        Map<String, AbilityStateMachine> map = ENTITY_ABILITIES.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(abilityId, AbilityStateMachine::new);
    }

    public static void tickAll(LivingEntity entity) {
        UUID uuid = entity.getUUID();
        Map<String, AbilityStateMachine> map = ENTITY_ABILITIES.get(uuid);
        if (map == null || map.isEmpty()) return;

        for (AbilityStateMachine sm : map.values()) {
            sm.tick(entity);
        }
    }

    public static void cancelAll(LivingEntity entity, String reason) {
        UUID uuid = entity.getUUID();
        Map<String, AbilityStateMachine> map = ENTITY_ABILITIES.get(uuid);
        if (map == null || map.isEmpty()) return;

        for (AbilityStateMachine sm : map.values()) {
            sm.cancel(entity, reason);
        }
    }

    public static boolean isAnyAbilityExecuting(LivingEntity entity) {
        UUID uuid = entity.getUUID();
        Map<String, AbilityStateMachine> map = ENTITY_ABILITIES.get(uuid);
        if (map == null) return false;

        for (AbilityStateMachine sm : map.values()) {
            if (sm.isExecuting()) return true;
        }
        return false;
    }
}
