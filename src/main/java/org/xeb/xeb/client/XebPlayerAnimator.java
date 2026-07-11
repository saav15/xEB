package org.xeb.xeb.client;

import net.minecraft.world.entity.player.Player;
import org.xeb.xeb.item.HolyDualityBladeItem;
import org.xeb.xeb.item.MechaOverdriveItem;

/**
 * Decides which GeckoLib animation to play on the player based on NBT state.
 * Called from XebPlayerGeoModel via AnimationController.
 */
public class XebPlayerAnimator {

    /**
     * Returns the animation name to play, or null for vanilla idle/default.
     */
    public static String getAnimationName(Player player) {
        boolean holdsMecha = player.getMainHandItem().getItem() instanceof MechaOverdriveItem
                || player.getOffhandItem().getItem() instanceof MechaOverdriveItem;
        boolean holdsHoly = player.getMainHandItem().getItem() instanceof HolyDualityBladeItem
                || player.getOffhandItem().getItem() instanceof HolyDualityBladeItem;

        if (holdsMecha) {
            return getMechaAnimation(player);
        }
        if (holdsHoly) {
            return getHolyAnimation(player);
        }
        return null; // vanilla idle
    }

    private static String getMechaAnimation(Player player) {
        var data = player.getPersistentData();
        boolean dashing = data.getBoolean("xebMechaOverdriveDashing");
        int sdState = data.getInt("xebMechaSpindashState");
        boolean vulcan = data.getBoolean("xebMechaVulcanFiring");
        boolean levitating = data.getBoolean("xebMechaLevitating");
        double momentum = data.getDouble("xebMechaMomentum");

        if (dashing) return "animation.xeb_player.mecha_dash";
        if (sdState == 1) return "animation.xeb_player.mecha_spindash_charge";
        if (sdState == 3) return "animation.xeb_player.mecha_spindash_attack";
        if (vulcan) return "animation.xeb_player.mecha_vulcan";
        if (levitating || momentum > 0.5) return "animation.xeb_player.mecha_levitate";
        return null; // vanilla idle
    }

    private static String getHolyAnimation(Player player) {
        var data = player.getPersistentData();
        boolean annihilation = data.getBoolean("xebHolyAnnihilationActive");
        int combo = data.getInt("xebHolyComboStage");
        boolean isAttacking = player.swingTime > 0;
        int blastCharge = data.getInt("xebHolyBlastCharge");
        int blastCast = data.getInt("xebHolyBlastCastTicks");

        if (annihilation) return "animation.xeb_player.holy_annihilation";
        if (isAttacking) {
            if (combo == 0) return "animation.xeb_player.holy_slash_right";
            if (combo == 1) return "animation.xeb_player.holy_slash_left";
            return "animation.xeb_player.holy_slash_x";
        }
        if (blastCast > 0) return "animation.xeb_player.holy_blast_cast";
        if (blastCharge > 0) return "animation.xeb_player.holy_blast_charge";
        return "animation.xeb_player.holy_idle";
    }
}
