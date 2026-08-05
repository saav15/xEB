package org.xeb.xeb.extremeburst;

import net.minecraft.world.entity.LivingEntity;

/**
 * Plantilla declarativa base para la creación estructurada de futuros Extreme Bursts en xEB.
 * Proporciona un ciclo de vida estandarizado (Invocación, Fase de Tick, Impacto y Limpieza).
 */
public abstract class AbstractExtremeBurstHandler {

    /**
     * Retorna el identificador único del Extreme Burst.
     */
    public abstract String getBurstId();

    /**
     * Se ejecuta en el tick exacto de activación cuando el jugador presiona la tecla de Extreme Burst (N).
     *
     * @param player Entidad que activa el burst.
     * @param entry Configuración registrada del burst.
     */
    public abstract void onActivate(LivingEntity player, ExtremeBurstRegistry.ExtremeBurstEntry entry);

    /**
     * Se ejecuta opcionalmente cada tick durante la transformación o canalización de la habilidad.
     *
     * @param player Entidad en estado Extreme Burst.
     * @param ticksRemaining Ticks restantes de duración.
     */
    public void onTickPhase(LivingEntity player, int ticksRemaining) {
        // Implementación opcional por defecto
    }

    /**
     * Se ejecuta en la culminación o clímax del impacto del Extreme Burst.
     *
     * @param player Entidad que ejecuta el impacto.
     */
    public void onImpact(LivingEntity player) {
        // Implementación opcional por defecto
    }

    /**
     * Se ejecuta al finalizar el Extreme Burst para limpiar NBTs y restaurar el estado normal de la entidad.
     *
     * @param player Entidad que finaliza la habilidad.
     */
    public void onEnd(LivingEntity player) {
        player.getPersistentData().putBoolean("xebExtremeBurstActive", false);
        player.getPersistentData().remove("xebExtremeBurstId");
    }
}
