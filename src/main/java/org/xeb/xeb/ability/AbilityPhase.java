package org.xeb.xeb.ability;

/**
 * Fases estandarizadas para la máquina de estados de habilidades activas en xEB.
 */
public enum AbilityPhase {
    /** Inactivo / Habilidad lista para usarse. */
    IDLE,
    /** Fase de Carga / Animación previa (Wind-up). */
    WIND_UP,
    /** Fase Activa / Ejecución de daño o efecto continuo (Active). */
    ACTIVE,
    /** Fase de Recuperación / Post-animación (Recovery). */
    RECOVERY,
    /** Fase de Enfriamiento (Cooldown). */
    COOLDOWN
}
