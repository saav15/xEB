package org.xeb.xeb.item.capability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Componente de Capacidades para Mobs (xEB).
 *
 * <p>Esta interfaz permite que cualquier arma existente o futura defina de manera autónoma
 * su propia lógica de Inteligencia Artificial para Mobs. Cuando un Mob sostiene un arma que implemente
 * esta interfaz, el sistema despacha automáticamente su IA de combate sin requerir bloques if-else en
 * el manejador global.</p>
 */
public interface IMobWeaponCapability {

    /**
     * Se ejecuta cada tick en el servidor cuando un Mob sostiene esta arma y tiene un objetivo válido con línea de visión.
     *
     * @param mob El mob que sostiene el arma.
     * @param target El objetivo (jugador o entidad enemiga).
     * @param level El nivel del servidor.
     * @param gameTime El tiempo del juego en ticks.
     * @param distanceSq La distancia al cuadrado hacia el objetivo.
     */
    void tickMobAI(Mob mob, LivingEntity target, Level level, long gameTime, double distanceSq);

    /**
     * Verifica si el Mob cumple las condiciones para empuñar y usar el arma.
     *
     * @param mob El mob evaluado.
     * @return {@code true} si el mob puede usar el arma.
     */
    default boolean canMobUseWeapon(Mob mob) {
        return true;
    }

    /**
     * Distancia preferida de combate para este tipo de arma (en bloques).
     *
     * @param mob El mob evaluado.
     * @return Distancia preferida de combate.
     */
    default double getPreferredAttackDistance(Mob mob) {
        return 24.0D;
    }

    /**
     * Callback invocado cuando el Mob es interrumpido, aturdido o entra en un Beam Struggle.
     *
     * @param mob El mob interrumpido.
     */
    default void onMobInterrupted(Mob mob) {
        // Implementación opcional por defecto
    }
}
