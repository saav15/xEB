package org.xeb.xeb.damagenumber;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Instancia de número de daño flotante con movimiento suave equilibrado tipo "Confetti".
 * Tanto los números grandes (STACK, COMBINE, total acumulado Híbrido) como los números pequeños
 * poseen una física de flotamiento y oscilación fluida (baja intensidad en los grandes, moderada en los pequeños).
 */
public class DamageNumberInstance {
    private static final Random RANDOM = new Random();

    public final int entityId;
    public float amount;
    public boolean isCrit;
    public String sourceType;

    public final long spawnTimeMs;
    public long lastHitTimeMs;
    public int ticksExisted;
    public final int maxLifetimeTicks;

    // Ángulo y vectores de dispersión en 3D
    public final float angle;
    public final float radialDist;
    public final float offsetX;
    public final float offsetY;
    public final float offsetZ;

    // Física de trayectoria tipo "Confetti" (Impulso suave + Aleteo fluido)
    public final float velX;
    public final float velY;
    public final float velZ;
    public final float swayFreq;
    public final float swayAmp;

    // Animación de "Punch" / Rebote elástico
    public float pulseScale = 1.0F;

    // Flag para identificar el total acumulado en modo Híbrido
    public final boolean isHybridCombined;

    // Posición inicial del objetivo
    public Vec3 initialPos;

    public DamageNumberInstance(int entityId, float amount, boolean isCrit, String sourceType, Vec3 initialPos, boolean isHybridCombined, int stackIndex) {
        this.entityId = entityId;
        this.amount = amount;
        this.isCrit = isCrit;
        this.sourceType = sourceType;
        this.initialPos = initialPos;
        this.isHybridCombined = isHybridCombined;

        long now = System.currentTimeMillis();
        this.spawnTimeMs = now;
        this.lastHitTimeMs = now;
        this.ticksExisted = 0;
        this.maxLifetimeTicks = DamageNumberConfig.lifetimeTicks;

        if (isHybridCombined) {
            // BANNER / TOTAL ACUMULADO GRANDE: Confetti suave y elegante
            this.angle = 0.35F;
            this.radialDist = 0.95F;
            this.offsetX = 0.75F;
            this.offsetY = 0.35F;
            this.offsetZ = 0.20F;
            
            this.velX = 0.008F;
            this.velY = 0.025F; // Flotamiento constante suave
            this.velZ = 0.008F;
            this.swayFreq = 0.16F;
            this.swayAmp = 0.008F; // Balanceo sutil de baja intensidad
            this.pulseScale = 1.40F;
        } else {
            // NÚMEROS INDIVIDUALES / STACK: Confetti de intensidad moderada equilibrada
            this.angle = (float) ((stackIndex * 1.45D + RANDOM.nextDouble() * 0.8D) * Math.PI * 2.0D);
            this.radialDist = 0.75F + RANDOM.nextFloat() * 0.45F; // 0.75 a 1.20 bloques fuera del centro

            this.offsetX = (float) Math.cos(this.angle) * this.radialDist;
            this.offsetZ = (float) Math.sin(this.angle) * this.radialDist;
            this.offsetY = (RANDOM.nextFloat() * 0.60F) - 0.30F;

            // Velocidad de expulsión moderada
            float speedFactor = 0.025F + RANDOM.nextFloat() * 0.015F;
            this.velX = (float) Math.cos(this.angle) * speedFactor;
            this.velZ = (float) Math.sin(this.angle) * speedFactor;
            this.velY = 0.045F + RANDOM.nextFloat() * 0.020F;

            this.swayFreq = 0.20F + RANDOM.nextFloat() * 0.10F;
            this.swayAmp = 0.012F + RANDOM.nextFloat() * 0.008F; // Aleteo fluido moderado

            this.pulseScale = isCrit ? 1.45F : 1.15F;
        }
    }

    public void addDamage(float addAmount, boolean crit) {
        this.amount += addAmount;
        if (crit) {
            this.isCrit = true;
        }
        this.ticksExisted = 0; // Reinicia timer de desvanecimiento
        this.lastHitTimeMs = System.currentTimeMillis();
        this.pulseScale = 1.50F; // Dispara efecto "punch" de rebote
    }

    public void tick() {
        this.ticksExisted++;
        if (this.pulseScale > 1.0F) {
            this.pulseScale = Math.max(1.0F, this.pulseScale - 0.07F);
        }
    }

    public boolean isExpired() {
        return this.ticksExisted >= this.maxLifetimeTicks;
    }

    /**
     * Travesía Confetti Horizontal X (expulsión lateral + suave oscilación).
     */
    public float getAnimX(float partialTick) {
        float age = this.ticksExisted + partialTick;
        float sway = (float) Math.sin(age * this.swayFreq) * this.swayAmp;
        return this.offsetX + (this.velX * age) + sway;
    }

    /**
     * Travesía Confetti Horizontal Z (expulsión lateral + suave oscilación).
     */
    public float getAnimZ(float partialTick) {
        float age = this.ticksExisted + partialTick;
        float sway = (float) Math.cos(age * this.swayFreq) * this.swayAmp;
        return this.offsetZ + (this.velZ * age) + sway;
    }

    /**
     * Travesía Confetti Vertical Y (elevación inicial + aceleración por gravedad).
     */
    public float getAnimY(float partialTick) {
        float age = this.ticksExisted + partialTick;
        if (isHybridCombined) {
            return this.offsetY + (this.velY * age);
        }
        return this.offsetY + (this.velY * age) - (0.0012F * age * age);
    }

    /**
     * Escala dinámica con animación Pop-in (overshoot elástico).
     */
    public float getScale(float partialTick) {
        float age = this.ticksExisted + partialTick;
        float popInTicks = 5.0F;

        float baseScale;
        if (age < popInTicks) {
            float progress = age / popInTicks;
            baseScale = (float) Math.sin(progress * Math.PI * 0.5D) * 1.25F;
        } else {
            baseScale = 1.0F;
        }

        if (isHybridCombined) {
            baseScale *= 1.35F; // Banner acumulado más grande
        }

        return baseScale * this.pulseScale * DamageNumberConfig.scale;
    }

    /**
     * Opacidad Alpha (Fade-out progresivo).
     */
    public float getAlpha(float partialTick) {
        float age = this.ticksExisted + partialTick;
        int fadeStart = DamageNumberConfig.fadeStartTicks;

        if (age <= fadeStart) {
            return 1.0F;
        }

        float fadeLength = this.maxLifetimeTicks - fadeStart;
        if (fadeLength <= 0) return 0.0F;

        float alpha = 1.0F - ((age - fadeStart) / fadeLength);
        return Mth.clamp(alpha, 0.0F, 1.0F);
    }
}
