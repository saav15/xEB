package org.xeb.xeb.client;

import net.minecraft.client.player.LocalPlayer;
import org.xeb.xeb.client.renderer.BrimstoneBeamRenderer;
import org.xeb.xeb.entity.TearsProjectileEntity;

public class LaserHeavyAimHandler {

    /**
     * Calculates turn speed multiplier for mouse sensitivity when firing heavy lasers.
     * - Brimstone: 50% heavier (0.50x mouse sensitivity)
     * - Steven Mega Laser: 50% heavier (0.50x mouse sensitivity)
     * - Dogma Burst: 80% heavier (0.20x mouse sensitivity)
     * 
     * @return 1.0F for normal sensitivity, or < 1.0F when heavy aim is active.
     */
    public static float getTurnMultiplier(LocalPlayer player) {
        if (player == null) return 1.0F;

        int playerId = player.getId();
        long now = System.currentTimeMillis();

        // 1. Check client active Brimstone / Dogma beams map
        BrimstoneBeamRenderer.ClientBrimstoneData brimstoneData = BrimstoneBeamRenderer.CLIENT_BRIMSTONES.get(playerId);
        if (brimstoneData != null && (now - brimstoneData.lastUpdate <= 600)) {
            if (brimstoneData.imbueType == TearsProjectileEntity.IMBUE_DOGMA || player.getPersistentData().getBoolean("xebDogmaBurstActive")) {
                return 0.20F; // Dogma: 80% heavier mouse sensitivity (0.20x speed)
            } else {
                return 0.50F; // Brimstone: 50% heavier mouse sensitivity (0.50x speed)
            }
        }

        // 2. Check if player is holding and charging/using The Tears item
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof org.xeb.xeb.item.TheTearsItem) {
            if (player.getPersistentData().getBoolean("xebDogmaBurstActive")) {
                return 0.20F; // Dogma: 80% heavier mouse sensitivity
            } else {
                return 0.50F; // Brimstone: 50% heavier mouse sensitivity
            }
        }

        // 3. Check Steven Mega Laser
        if (player.getPersistentData().getInt("xebStevenLaserFiringTicks") > 0
                || player.getPersistentData().getBoolean("xebStevenLaserActive")) {
            return 0.50F; // Steven Mega Laser: 50% heavier
        }

        // 4. Check Full-Aperture Supernova (Optic Blast Extreme Burst)
        if (player.getPersistentData().getInt("xebOpticBurstState") > 0) {
            return 0.15F; // Supernova: 85% heavy camera weight (0.15x turn speed)
        }

        return 1.0F;
    }
}
