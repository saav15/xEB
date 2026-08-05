package org.xeb.xeb.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Máquina de estados declarativa para habilidades activas en xEB.
 * Maneja el ciclo de vida completo (WIND_UP, ACTIVE, RECOVERY, COOLDOWN, IDLE),
 * sincronización NBT, banderas de comportamiento y callbacks.
 */
public class AbilityStateMachine {

    public interface AbilityListener {
        default void onWindUp(LivingEntity entity) {}
        default void onActive(LivingEntity entity) {}
        default void onRecovery(LivingEntity entity) {}
        default void onEnd(LivingEntity entity) {}
        default void onCancel(LivingEntity entity, String reason) {}
    }

    private final String abilityId;
    private AbilityPhase currentPhase = AbilityPhase.IDLE;
    private int phaseTicksRemaining = 0;
    private int windUpTicks = 0;
    private int activeTicks = 0;
    private int recoveryTicks = 0;
    private int cooldownTicks = 0;

    // Banderas de comportamiento automático
    private boolean preventsMovement = false;
    private boolean preventsRotation = false;
    private boolean providesFallProtection = false;
    private boolean isInvulnerable = false;

    private AbilityListener listener;

    public AbilityStateMachine(String abilityId) {
        this.abilityId = abilityId;
    }

    public AbilityStateMachine configure(int windUpTicks, int activeTicks, int recoveryTicks, int cooldownTicks) {
        this.windUpTicks = windUpTicks;
        this.activeTicks = activeTicks;
        this.recoveryTicks = recoveryTicks;
        this.cooldownTicks = cooldownTicks;
        return this;
    }

    public AbilityStateMachine setFlags(boolean preventsMovement, boolean preventsRotation, boolean fallProtect, boolean invulnerable) {
        this.preventsMovement = preventsMovement;
        this.preventsRotation = preventsRotation;
        this.providesFallProtection = fallProtect;
        this.isInvulnerable = invulnerable;
        return this;
    }

    public AbilityStateMachine setListener(AbilityListener listener) {
        this.listener = listener;
        return this;
    }

    public boolean start(LivingEntity entity) {
        if (currentPhase != AbilityPhase.IDLE && currentPhase != AbilityPhase.COOLDOWN) {
            return false;
        }

        if (windUpTicks > 0) {
            transitionTo(entity, AbilityPhase.WIND_UP, windUpTicks);
            if (listener != null) listener.onWindUp(entity);
        } else if (activeTicks > 0) {
            transitionTo(entity, AbilityPhase.ACTIVE, activeTicks);
            if (listener != null) listener.onActive(entity);
        } else if (recoveryTicks > 0) {
            transitionTo(entity, AbilityPhase.RECOVERY, recoveryTicks);
            if (listener != null) listener.onRecovery(entity);
        } else {
            transitionTo(entity, AbilityPhase.COOLDOWN, cooldownTicks);
            if (listener != null) listener.onEnd(entity);
        }
        return true;
    }

    public void tick(LivingEntity entity) {
        if (currentPhase == AbilityPhase.IDLE) return;

        // Aplicar modificadores de estado continuos
        if (isExecuting()) {
            if (preventsMovement) {
                entity.setDeltaMovement(entity.getDeltaMovement().x * 0.1D, entity.getDeltaMovement().y, entity.getDeltaMovement().z * 0.1D);
            }
            if (providesFallProtection) {
                entity.getPersistentData().putBoolean("xebDoomfistFallProtect", true);
            }
        }

        phaseTicksRemaining--;
        if (phaseTicksRemaining <= 0) {
            advancePhase(entity);
        }
    }

    private void advancePhase(LivingEntity entity) {
        switch (currentPhase) {
            case WIND_UP:
                if (activeTicks > 0) {
                    transitionTo(entity, AbilityPhase.ACTIVE, activeTicks);
                    if (listener != null) listener.onActive(entity);
                } else if (recoveryTicks > 0) {
                    transitionTo(entity, AbilityPhase.RECOVERY, recoveryTicks);
                    if (listener != null) listener.onRecovery(entity);
                } else {
                    transitionTo(entity, AbilityPhase.COOLDOWN, cooldownTicks);
                    if (listener != null) listener.onEnd(entity);
                }
                break;
            case ACTIVE:
                if (recoveryTicks > 0) {
                    transitionTo(entity, AbilityPhase.RECOVERY, recoveryTicks);
                    if (listener != null) listener.onRecovery(entity);
                } else if (cooldownTicks > 0) {
                    transitionTo(entity, AbilityPhase.COOLDOWN, cooldownTicks);
                    if (listener != null) listener.onEnd(entity);
                } else {
                    transitionTo(entity, AbilityPhase.IDLE, 0);
                    if (listener != null) listener.onEnd(entity);
                }
                break;
            case RECOVERY:
                if (cooldownTicks > 0) {
                    transitionTo(entity, AbilityPhase.COOLDOWN, cooldownTicks);
                    if (listener != null) listener.onEnd(entity);
                } else {
                    transitionTo(entity, AbilityPhase.IDLE, 0);
                    if (listener != null) listener.onEnd(entity);
                }
                break;
            case COOLDOWN:
                transitionTo(entity, AbilityPhase.IDLE, 0);
                break;
            default:
                break;
        }
    }

    public void cancel(LivingEntity entity, String reason) {
        if (currentPhase == AbilityPhase.IDLE) return;
        currentPhase = AbilityPhase.IDLE;
        phaseTicksRemaining = 0;
        syncToNBT(entity);
        if (listener != null) listener.onCancel(entity, reason);
    }

    private void transitionTo(LivingEntity entity, AbilityPhase phase, int duration) {
        this.currentPhase = phase;
        this.phaseTicksRemaining = duration;
        syncToNBT(entity);
    }

    public void syncToNBT(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        tag.putString("xebAbility_" + abilityId + "_Phase", currentPhase.name());
        tag.putInt("xebAbility_" + abilityId + "_Ticks", phaseTicksRemaining);
    }

    public String getAbilityId() { return abilityId; }
    public AbilityPhase getCurrentPhase() { return currentPhase; }
    public int getPhaseTicksRemaining() { return phaseTicksRemaining; }
    public boolean isExecuting() { return currentPhase == AbilityPhase.WIND_UP || currentPhase == AbilityPhase.ACTIVE || currentPhase == AbilityPhase.RECOVERY; }
    public boolean isOnCooldown() { return currentPhase == AbilityPhase.COOLDOWN; }
    public boolean isPreventsMovement() { return preventsMovement; }
    public boolean isProvidesFallProtection() { return providesFallProtection; }
    public boolean isInvulnerable() { return isInvulnerable; }
}
