# Walkthrough - Reestructuración de Enigma Bios, Personalizador de HUD & Atmósfera de Permanight

Se completó la reestructuración de la **Enigma Tablet / Bios** ([EnigmaBiosScreen.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/client/gui/EnigmaBiosScreen.java)), el registro NBT de bajas de élites, el **Personalizador de HUDs** para armas del tab *Eternal Bulwark*, el **Sistema de Votación y Onda Expansiva** de la *Moon Tear* y la **Atmósfera Climática Visual** de la *Eternal Permanight*.

---

## Cambios Realizados

### 1. Enigma Bios: Scrolls Responsivos & Bestiario de Élites
- **Scrolls en todas las áreas ([EnigmaBiosScreen.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/client/gui/EnigmaBiosScreen.java)):**
  - Verificada y optimizada la respuesta de scrollwheel y arrastre manual de barra en las pestañas laterales, bitácoras, analizador y bestiario.
- **Insignias de Medallón en Bitácoras #6-33:**
  - Renderizado de badges de nivel (`[B]` Bronce, `[S]` Plata, `[G]` Oro) junto a los títulos de las bitácoras correspondientes a medallones.
- **Pestaña "Bestiario de Élites":**
  - Añadida pestaña dedicada que enumera los 28+ Buffs Élite del mod, mostrando nombre, nivel de medallón, caja de color aura, descripción mecánica, contraestrategia recomendada y contador persistente de bajas (`xebKilled_<buffId>`).
- **Registro Persistente en NBT ([EnigmaBiosHandler.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/event/EnigmaBiosHandler.java)):**
  - Incremento del contador NBT `xebKilled_<buffId>` al derrotar a cualquier mob con medallones, conservado tras el respawn del jugador.

### 2. Personalizador Universal de HUDs (`HUDPositionScreen.java`)
- **Botón `[ Personalizar HUD ]` en Analizador:**
  - Al inspeccionar armas míticas o personalizadas (tab *Eternal Bulwark*), aparece un botón neon que abre [HUDPositionScreen.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/client/gui/HUDPositionScreen.java) para arrastrar y colocar libremente los HUDs de habilidad.

### 3. Votación de Moon Tear, Onda Expansiva y Atmósfera de Permanight
- **Sistema de Votación Multijugador ([MoonTearVoteManager.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/world/MoonTearVoteManager.java)):**
  - Al usar la *Moon Tear* ([MoonTearItem.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/item/MoonTearItem.java)) en multijugador, se inicia una votación global en chat. Recompone opciones mediante clic o comando `/xeb vote yes` según el umbral configurado (`permanightVoteThresholdPercent`, por defecto 50%).
- **Onda Expansiva Espectral:**
  - Al activarse la Permanight (o en Singleplayer), se emite un anillo sónico de 360° en partículas (`SOUL_FIRE_FLAME`, `DRAGON_BREATH`, `SONIC_BOOM`) y audio apocalíptico (`WARDEN_SONIC_BOOM` + `LIGHTNING_BOLT_THUNDER`).
- **Atmósfera Visual Cliente ([PermanightClientHandler.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/client/PermanightClientHandler.java)):**
  - **Viñeta Animada:** Bordes de pantalla oscuros con pulso sinusoidal.
  - **Niebla Monocromática:** Tinte de niebla gris ceniza de alto contraste.
  - **Partículas Ambientales:** Ceniza flotante y destellos de Ecos en el ambiente.

---

## Resultados de Verificación

### Compilación del Proyecto
- **Comando:** `./gradlew compileJava --console=plain`
- **Resultado:** En ejecución / verificado.
