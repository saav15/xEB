# Custom Rules for xEB (xd Elite Buffs)

## Item Rarity & Tooltip Color System
- **Rojo Mítico (Mythic Red):** Reservado para las Armas Míticas del mod (`Golden Flower`, `Doomfist v1`, `Doomfist v2`, `Optic Blast`, `Holy Duality Blade`, `Mecha II Overdrive Core`, `Broken Diamond`, `The Tears`).
- **Aqua Legendario (Legendary Aqua):** Reservado para los Objetos/Armas Legendarios del mod (`Smart Halberd`).
- **Morado Épico (Epic Purple):** Reservado para las Reliquias Épicas y Artefactos de Extreme Burst del mod (`Omega Flowery`, `Dogma`, `Quantum Cat Barrage`).

## Sistema de Tooltips Dinámicos (XebTooltip)
Cuando el usuario o un ítem requiera el sistema **XebTooltip**, se implementa el patrón de tooltip interactivo con detección de tecla Shift (`Screen.hasShiftDown()`):

1. **Estado Normal (Sin presionar SHIFT):**
   - **Título Animado:** Nombre del ítem con efecto de onda de color animado (*Wave Name*) correspondiente a su rareza (Rojo Mítico, Aqua Legendario, Morado Épico, Dorado Enigma).
   - **Lore del Ítem:** Texto de ambientación / lore en itálico celeste (`§b`/`§3`) o gris.
   - **Separador:** Línea vacía.
   - **Prompt de Tecla:** `§8[Mantén SHIFT para más detalles]` (`gui.xeb.tooltip.shift_prompt`).

2. **Estado Expandido (Con SHIFT Presionado):**
   - **Encabezado Descripción (`§6Descripción`):** Texto explicativo del ítem en gris (`§7`).
   - **Encabezado Estadísticas (`§6Estadísticas`):** Atributos como daño base, velocidad, rango, DPS, enfriamiento/duración.
   - **Encabezado Habilidades (`§6Habilidades`):**
     - Click Derecho (`§eClick Derecho: §7...`)
     - Habilidades Activas asignadas a Keybinds (`  §b• ...`)
   - **Encabezado Encantamientos:** Si aplica, viñetas limpias (`  §d• ...`).
   - **Lore final.**

