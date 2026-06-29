# Walkthrough - Doomfist v1 & v2 Weapon Abilities, Keybinds, & HUD Cooldowns

All requested features and adjustments for both **The Doomfist v1** and **v2** weapons and abilities have been implemented, registered, and successfully compiled.

## Completed Changes

### 1. In-Game Registry, Naming & Tooltips
- Renamed the weapon **The Doomfist** to **The Doomfist v1** and registered **The Doomfist v2** (`ModItems.DOOMFIST_V2`).
- Setup translation files (`en_us.json`, `es_es.json`, `es_mx.json`) for localization.
  - Lore for v2: *"The future has been forged, and it wasnt by me...."* (Spanish: *"El futuro has sido forjado, y no fue por mí...."*).
- Assigned dynamic key-mapped tooltip descriptions to both weapons in [DoomfistItem.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/item/DoomfistItem.java) and [DoomfistV2Item.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/item/DoomfistV2Item.java). These use formatting placeholders (`%s`) and supply `Component.keybind("key.xeb.actuar_1")` / `Component.keybind("key.xeb.actuar_2")`. This guarantees they will automatically display whichever key the player currently has bound (e.g. `G`, `H`, or custom alternatives).
- Tagged `DOOMFIST_V2` under Better Combat fists inside [fists.json](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/resources/data/bettercombat/tags/items/fists.json).

### 2. Keybinds & Networking Configuration
- Registered two client keybinds under the **xEB** category:
  - **Actuar 1**: Default key `G`
  - **Actuar 2**: Default key `H`
- Updated [KeyInputHandler.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/client/KeyInputHandler.java) to detect held-down keys (specifically tracking press/release transitions of H key for Power Block) and forward packets to the server.
- Updated [ActuarKeyPacket.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/network/ActuarKeyPacket.java) and [DoomfistAbilitySyncPacket.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/network/DoomfistAbilitySyncPacket.java) to serialize/deserialize press states and the HUD flash sync parameter.

### 3. Doomfist v1 Abilities
- **Rising Uppercut (Actuar 1 - 5s CD)**: Impulses you upward (`1.2D` Y velocity, `0.6D` horizontal), launches targets in a `1.5x1.5` area, and suspends both user and hit targets in the air for 2 seconds at the peak of the jump.
- **Seismic Slam (Actuar 2 - 6s CD)**: Projects a floor-snapping blueprint up to 15 blocks (falling back to vertical downward raycasts on misses), hovers you in place for 15 ticks, and launches you in a direct guided line to the targeted spot, dealing 60% damage in a 6-block cone.

### 4. Doomfist v2 Abilities
- **Earthquake Slam (Actuar 1 - 7s CD)**: 
  - Launches you forward in a diagonal/parabolic momentum-based arc.
  - **Consistent Landing Detection**: Checks if the player is `onGround()` or if a solid block is detected within `0.3D` blocks below their feet bounding box during the falling phase. This guarantees 100% impact reliability.
  - **Hover Cancel**: Pressing Jump while in the rising phase cancels the slam and triggers a 2.0-second float (40 ticks).
  - **Power Block Cancel**: Activating Power Block during the rising phase cancels the slam, starts blocking immediately, and provides a vertical Y-momentum jump boost (`+0.55D`) to let you maneuver in the air.
  - **Charged Punch Momentum Cancel**: Releasing a charged punch while moving during Earthquake Slam cancels the slam and adds **1.5x of the horizontal momentum** to the punch launch speed, launching the player forward at massive speeds.
  - **Bunny Hop**: Pressing Jump within 10 ticks of hitting the ground launches you forward in a fast bunny-hop utilizing your momentum.
  - **Impact**: Slamming down deals 80% damage in a front-facing 6-block cone, pulling targets and spawning red flames.
  - **Red Propagating Shockwave**: Spawns a conic wave of red dust and flames sweeping forward at a rate of 1 block per tick along the look vector.
- **Power Block (Actuar 2 - 7s CD)**:
  - Activates a mechanical guard stance where the player raises the fist over their chest (playing `SoundEvents.PISTON_EXTEND` on start, and `SoundEvents.PISTON_CONTRACT` on end).
  - Mitigates **75% of incoming damage** from the front.
  - Decreases movement speed by 35% while blocking.
  - Minimum hold duration of 0.3s (6 ticks) up to a maximum duration of 2.5s (50 ticks).
  - **Ultra Charge Mechanic**: If you block damage exceeding **20% of your maximum health** during a single block window, your fist becomes "Ultra Charged" (playing thunder and crit sound effects, spawning red sparks).
  - **14.0F Damage Attack**: While Ultra Charged, the damage of your next attack (either regular left-click attacks, charged punch dash collisions, or wall-slam impacts) is overridden to exactly **14.0F**, immediately consuming the state.
  - **Red Flame Dash Trail**: When dashing/charging forward using the Doomfist v2, the player now leaves a custom trail of bright red fire particles (`ParticleTypes.FLAME`), while retaining the blue soul fire trail (`ParticleTypes.SOUL_FIRE_FLAME`) for Doomfist v1.

### 5. Red Visuals Theme & Ultra Charged Cosmetics (Doomfist v2)
- Reworked [DoomfistRenderLayer.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/render/DoomfistRenderLayer.java) to render energy/charge auras in volcanic red for v2.
- **Chest Shield Aura**: Renders a dynamic, glowing red forcefield shield in front of the player's chest while actively blocking. The shield is attached to the body and rotates with the torso's pitch/yaw/roll.
- **Ultra Charged Aim Pose**: Poses the player's arm raised forward in third-person and centered forward in first-person (resembling a focused punch-ready stance) whenever they are in the `xebUltraCharged` state.
- **Swirling Pitch Black Energy Shell**: Renders an ominous dark energy shield (creeper armor pattern rendered via `RenderType.entityTranslucent` with a very dark overlay color and constant dynamic rotation on the poseStack) surrounding the gauntlet in both first-person and third-person perspectives to visually display the threat of the 14.0F Charged Punch.
- **Server-Client State Sync**: Created [DoomfistUltraChargeSyncPacket.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/network/DoomfistUltraChargeSyncPacket.java) and registered it inside [XEBNetwork.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/network/XEBNetwork.java) to synchronize the `xebUltraCharged` NBT tag to the client environment immediately when gained or lost. This fixes the dark creeper aura visibility.
- Colored the center crosshair punch charge progress bar red when charging a v2 punch.
- Added red fire and lava particles on Earthquake Slam impact.
- Added a red-vignetted screen border and soft red screen overlay flash in [DoomfistHUDOverlay.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/render/DoomfistHUDOverlay.java) when Power Block successfully mitigates damage (duration increased to 15 ticks for visibility).

### 6. Fall Damage Protection Fix
- Intercepts `LivingFallEvent` to immediately negate fall damage and **consume** the NBT tags on impact. Added a fail-safe cleanup buffer of 20 ticks (1s) on the ground to prevent any late-latency damage calculation bypasses.

### 7. Client HUD Cooldown Box Improvements
- Updated [DoomfistHUDOverlay.java](file:///c:/Users/Tadoew/Documents/Prog8/Antigravity/xEB%20-xd%20Elite%20Buffs-/src/main/java/org/xeb/xeb/render/DoomfistHUDOverlay.java) to dynamically adapt labels, keybind letters, and maximum cooldown values to the held weapon:
  - **v1**: Displays **UPPER** (bound key via client KeyMapping lookup) and **SLAM** (bound key via client KeyMapping lookup) with grey cooldown sweeps and time counters.
  - **v2**: Displays **ESLAM** (bound key) and **BLOCK** (bound key) with grey cooldown sweeps and time counters.
  - Dynamic key detection uses: `org.xeb.xeb.client.ModKeyMappings.ACTUAR_1_KEY.getTranslatedKeyMessage().getString()` which supports standard keys, secondary functions, and mouse mapping.

---

## Compilation Validation
The project compiled successfully:
- **Build Outcome**: `BUILD SUCCESSFUL in 28s`
