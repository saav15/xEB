package org.xeb.xeb.damagenumber;

import net.minecraft.resources.ResourceLocation;

/**
 * Configuración cliente-side y parámetros visuales del sistema de Damage Numbers.
 */
public class DamageNumberConfig {
    private static DamageNumberMode mode = DamageNumberMode.COMBINE;

    // Parámetros de Agrupación y Timers
    public static long combineWindowMs = 650L;
    public static int maxStackedNumbers = 8;
    public static int lifetimeTicks = 35;
    public static int fadeStartTicks = 22;

    // Parámetros de Animación y Posición 3D
    public static float floatSpeed = 0.035F;
    public static float gravity = 0.0008F;
    public static float heightOffset = 0.45F;
    public static float scale = 1.0F;
    public static float horizontalJitter = 0.18F;

    // Umbrales de Color para Combine / Híbrido (Blanco -> Amarillo -> Naranja -> Rojo)
    public static float thresholdYellow = 20.0F;
    public static float thresholdOrange = 50.0F;
    public static float thresholdRed = 100.0F;

    // Indicadores Críticos
    public static boolean critIndicatorEnabled = true;
    public static String critIcon = "★";
    public static int critColor = 0xFFFFD700; // Dorado reluciente

    // Fuente Custom opcional (null usa la fuente vanilla por defecto)
    public static ResourceLocation fontResource = null;

    public static DamageNumberMode getMode() {
        return mode;
    }

    public static void setMode(DamageNumberMode newMode) {
        if (newMode != null) {
            mode = newMode;
        }
    }

    /**
     * Calcula el color ARGB según el daño acumulado cruzando umbrales.
     */
    public static int getColorForTotalDamage(float totalDamage) {
        if (totalDamage >= thresholdRed) {
            return 0xFFFF3333; // Rojo brillante
        } else if (totalDamage >= thresholdOrange) {
            return 0xFFFF8C00; // Naranja intenso
        } else if (totalDamage >= thresholdYellow) {
            return 0xFFFFE436; // Amarillo dorado
        }
        return 0xFFFFFFFF; // Blanco puro
    }

    /**
     * Devuelve el color según el tipo de daño (DamageSource category).
     */
    public static int getColorForSourceCategory(String category) {
        if (category == null) return 0xFFFFFFFF;
        return switch (category.toLowerCase()) {
            case "fire", "lava", "in_fire", "on_fire", "hot_floor" -> 0xFFFF4500; // Naranja fuego
            case "poison", "wither" -> 0xFF32CD32; // Verde tóxico
            case "magic", "dragon_breath", "indirect_magic" -> 0xFFBA55D3; // Orquídea mágico
            case "lightning", "lightning_bolt" -> 0xFF00FFFF; // Aqua relámpago
            case "explosion", "player_explosion" -> 0xFFFF2222; // Rojo explosión
            case "fall", "fly_into_wall" -> 0xFFAAAAAA; // Gris impacto
            default -> 0xFFFFFFFF; // Blanco por defecto
        };
    }
}
