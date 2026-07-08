# xd Elite Buffs (xEB)

A feature-rich **Minecraft Forge 1.20.1** mod that introduces a dynamic **Elite Buff and Medallion System** inspired by Mewgenics. Mobs spawn with unique visual medallions rotating and bobbing above their heads, granting them dangerous special abilities, custom color tints, glowing eyes, and custom particle effects.

---

## 🌟 Key Features

### 🏅 The Medallion & Buff System
Mobs can receive visual medallions of different tiers that grant unique abilities:
- 🟫 **Bronze Medallion (Common):** Grants standard elite buffs.
- ⬜ **Silver Medallion (Rare):** Grants enhanced, tricky, or tactical buffs.
- 🟨 **Gold Medallion (Legendary):** Grants high-tier, extremely dangerous buffs.

### ⚔️ Over 28 Unique Elite Buffs
Each medallion represents an active buff with its own mechanical properties, including:
*   **Spiky:** Thorns-like damage reflection to attackers.
*   **Reactive:** Emits a sonic shockwave when damaged.
*   **Speedy / Speedy:** Amplifies movement and attack speed.
*   **Infested:** Spawns hostile Elite Flies on attack or death.
*   **Sandy:** Blinds nearby attackers with sandstorms.
*   **Mirror / Reflect:** Reflects incoming damage and projectiles.
*   **Undying:** Surges back to life once upon taking fatal damage.
*   **Evolving:** gains additional medallions mid-combat.

### 🔮 Signature Items & Ultimate Weapons
*   **The Doomfist v1 & v2:** Devastating gauntlets channeling kinetic energy. Includes **Rising Uppercut**, **Seismic Slam**, and v2 features like **Earthquake Slam**, **Power Block**, and **Ultra Charged** strikes.
*   **The Tears:** Isaac-inspired tearful artifact firing Brimstone lasers.
*   **Quantum Kitty Barrage:** Accumulates damage to release a global phantom-thief barrage.
*   **The Crazy Diamond:** Stand-summoning diamond that strikes at double reach and heals nearby allies.
*   **Tinfoil Hat:** Protects the wearer from mental control/debuffs.
*   **Demon Core:** Deploys a radioactive core, applying Doomed status to nearby entities.
*   **Hot Potato:** Explodes and burns targets on contact when thrown.
*   **Moon Tear:** Triggers the Elite Permanight event forcing total darkness.

---

## 🛠️ Requirements

- **Minecraft:** `1.20.1`
- **Mod Loader:** **Minecraft Forge** (version `47.2.0` or higher recommended, tested up to `47.4.20`)
- **Core Dependency:** **GeckoLib** (`4.x` for 1.20.1)

---

## 📦 Installation

### For Players
1. Ensure you have installed **Minecraft Forge 1.20.1**.
2. Download and install **GeckoLib** for Forge 1.20.1 and place it in your `mods` folder.
3. Drop the `xd-Elite-Buffs` JAR file into your `.minecraft/mods` folder.
4. Launch the game and enjoy the chaos!

### For Developers
1. Clone this repository:
   ```bash
   git clone https://github.com/saav15/xEB.git
   ```
2. Open the directory in your preferred Java IDE (IntelliJ IDEA recommended).
3. Import the project as a Gradle project.
4. Run the Gradle setup tasks to generate IDE launch configurations:
   - For IntelliJ: `./gradlew genIntellijRuns`
   - For Eclipse: `./gradlew genEclipseRuns`
5. Run the game client using the generated `runClient` run configuration.
6. Build a production jar using:
   ```bash
   ./gradlew build
   ```

---

## 📄 License & Credits

- **Authors:** xd Team, xd Tadoew
- **Inspiration:** Mewgenics Elite Medallion System
- **License:** LGPL-3.0 (GNU Lesser General Public License v3.0)
