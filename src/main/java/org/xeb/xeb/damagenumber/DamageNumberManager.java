package org.xeb.xeb.damagenumber;

import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor Cliente-Side de las instancias activas de marcadores de daño flotantes.
 */
public class DamageNumberManager {
    private static final DamageNumberManager INSTANCE = new DamageNumberManager();

    // Registro de daño: Entity ID -> Lista de instancias activas
    private final Map<Integer, List<DamageNumberInstance>> activeNumbers = new ConcurrentHashMap<>();

    public static DamageNumberManager getInstance() {
        return INSTANCE;
    }

    /**
     * Procesa un golpe de daño en el cliente según el modo seleccionado.
     */
    public void onDamageReceived(int entityId, float amount, boolean isCrit, String sourceType, Vec3 entityPos) {
        DamageNumberMode mode = DamageNumberConfig.getMode();
        if (mode == DamageNumberMode.OFF || amount <= 0.0F) {
            return;
        }

        List<DamageNumberInstance> list = activeNumbers.computeIfAbsent(entityId, k -> new ArrayList<>());
        long now = System.currentTimeMillis();

        if (mode == DamageNumberMode.STACK) {
            int stackIndex = list.size();
            list.add(new DamageNumberInstance(entityId, amount, isCrit, sourceType, entityPos, false, stackIndex));

            // Eliminar instancias viejas si supera maxStackedNumbers
            while (list.size() > DamageNumberConfig.maxStackedNumbers) {
                list.remove(0);
            }
        } else if (mode == DamageNumberMode.COMBINE) {
            DamageNumberInstance existingCombined = findRecentCombinedInstance(list, now);
            if (existingCombined != null) {
                existingCombined.addDamage(amount, isCrit);
                existingCombined.initialPos = entityPos;
            } else {
                list.clear();
                list.add(new DamageNumberInstance(entityId, amount, isCrit, sourceType, entityPos, true, 0));
            }
        } else if (mode == DamageNumberMode.HYBRID) {
            // 1. Número individual (STACK)
            int stackIndex = list.size();
            list.add(new DamageNumberInstance(entityId, amount, isCrit, sourceType, entityPos, false, stackIndex));

            // 2. Banner acumulado total (COMBINE)
            DamageNumberInstance existingCombined = findRecentCombinedInstance(list, now);
            if (existingCombined != null) {
                existingCombined.addDamage(amount, isCrit);
                existingCombined.initialPos = entityPos;
            } else {
                list.add(new DamageNumberInstance(entityId, amount, isCrit, sourceType, entityPos, true, 0));
            }

            // Mantener límite de instancias
            while (list.size() > DamageNumberConfig.maxStackedNumbers + 1) {
                for (int i = 0; i < list.size(); i++) {
                    if (!list.get(i).isHybridCombined) {
                        list.remove(i);
                        break;
                    }
                }
            }
        }
    }

    private DamageNumberInstance findRecentCombinedInstance(List<DamageNumberInstance> list, long nowMs) {
        for (int i = list.size() - 1; i >= 0; i--) {
            DamageNumberInstance inst = list.get(i);
            if (inst.isHybridCombined || DamageNumberConfig.getMode() == DamageNumberMode.COMBINE) {
                if (nowMs - inst.lastHitTimeMs <= DamageNumberConfig.combineWindowMs && !inst.isExpired()) {
                    return inst;
                }
            }
        }
        return null;
    }

    /**
     * Actualiza el timer de todas las instancias y limpia las expiradas.
     */
    public void tick() {
        Iterator<Map.Entry<Integer, List<DamageNumberInstance>>> it = activeNumbers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, List<DamageNumberInstance>> entry = it.next();
            List<DamageNumberInstance> list = entry.getValue();

            Iterator<DamageNumberInstance> listIt = list.iterator();
            while (listIt.hasNext()) {
                DamageNumberInstance inst = listIt.next();
                inst.tick();
                if (inst.isExpired()) {
                    listIt.remove();
                }
            }

            if (list.isEmpty()) {
                it.remove();
            }
        }
    }

    public Map<Integer, List<DamageNumberInstance>> getActiveNumbers() {
        return activeNumbers;
    }

    public void clearAll() {
        activeNumbers.clear();
    }
}
