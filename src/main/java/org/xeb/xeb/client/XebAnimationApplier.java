package org.xeb.xeb.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

/**
 * Applies GeckoLib animations to a vanilla HumanoidModel.
 * Reads the animation from .animation.json and overrides the bone rotations.
 */
public class XebAnimationApplier {

    /**
     * Applies the appropriate animation to the player model.
     * Called at the tail of HumanoidModel.setupAnim via Mixin.
     */
    public static void applyAnimation(AbstractClientPlayer player, HumanoidModel<?> model, float ageInTicks) {
        String animName = XebPlayerAnimator.getAnimationName(player);
        if (animName == null) return;

        applyManualAnimation(player, model, animName, ageInTicks);
    }

    /**
     * Manual animation application without full GeckoLib pipeline.
     * Reads keyframes from the animation and interpolates.
     */
    private static void applyManualAnimation(AbstractClientPlayer player, HumanoidModel<?> model, String animName, float ageInTicks) {
        float time = ageInTicks / 20.0F; // seconds

        // Map animation name to bone rotations
        switch (animName) {
            case "animation.xeb_player.mecha_levitate" -> applyMechaLevitate(model, time, player);
            case "animation.xeb_player.mecha_vulcan" -> applyMechaVulcan(model, time);
            case "animation.xeb_player.mecha_dash" -> applyMechaDash(model, time);
            case "animation.xeb_player.mecha_spindash_charge" -> applyMechaSpindashCharge(model, time);
            case "animation.xeb_player.mecha_spindash_attack" -> applyMechaSpindashAttack(model, time);
            case "animation.xeb_player.holy_slash_right" -> applyHolySlashRight(model, time, player);
            case "animation.xeb_player.holy_slash_left" -> applyHolySlashLeft(model, time, player);
            case "animation.xeb_player.holy_slash_x" -> applyHolySlashX(model, time, player);
            case "animation.xeb_player.holy_annihilation" -> applyHolyAnnihilation(model, time, player);
            case "animation.xeb_player.holy_blast_charge" -> applyHolyBlastCharge(model, time, player);
            case "animation.xeb_player.holy_blast_cast" -> applyHolyBlastCast(model, time, player);
            case "animation.xeb_player.holy_idle" -> applyHolyIdle(model, time);
        }
    }

    private static void applyMechaLevitate(HumanoidModel<?> model, float time, Player player) {
        double momentum = player.getPersistentData().getDouble("xebMechaMomentum");
        float bob = Mth.sin(time * 3.0F) * 0.05F; // slight bobbing
        
        // Reset all limb sways completely to prevent walking sway
        model.rightArm.yRot = 0;
        model.rightArm.zRot = 0;
        model.leftArm.yRot = 0;
        model.leftArm.zRot = 0;
        model.rightLeg.yRot = 0;
        model.rightLeg.zRot = 0;
        model.leftLeg.yRot = 0;
        model.leftLeg.zRot = 0;
        
        if (momentum >= 1.0D) {
            // Mecha Sonic Mark 2 - Max power intimidating pose!
            model.body.xRot = (float) Math.toRadians(25 + bob * 10);
            model.body.yRot = 0;
            model.body.zRot = 0;
            model.head.xRot = (float) Math.toRadians(-15 - bob * 5); // head up
            
            // Arms swept far back
            model.rightArm.xRot = (float) Math.toRadians(75);
            model.rightArm.yRot = (float) Math.toRadians(-20);
            model.rightArm.zRot = (float) Math.toRadians(15);
            
            model.leftArm.xRot = (float) Math.toRadians(75);
            model.leftArm.yRot = (float) Math.toRadians(20);
            model.leftArm.zRot = (float) Math.toRadians(-15);
            
            // Legs floating and pointing back
            model.rightLeg.xRot = (float) Math.toRadians(-35 + bob * 5);
            model.rightLeg.yRot = (float) Math.toRadians(5);
            model.rightLeg.zRot = (float) Math.toRadians(2);
            model.leftLeg.xRot = (float) Math.toRadians(-30 - bob * 5);
            model.leftLeg.yRot = (float) Math.toRadians(-5);
            model.leftLeg.zRot = (float) Math.toRadians(-2);
        } else {
            // Medium power levitating pose
            model.body.xRot = (float) Math.toRadians(15 + bob * 5);
            model.body.yRot = 0;
            model.body.zRot = 0;
            model.head.xRot = (float) Math.toRadians(-10);
            
            model.rightArm.xRot = (float) Math.toRadians(45);
            model.rightArm.yRot = (float) Math.toRadians(-10);
            model.rightArm.zRot = (float) Math.toRadians(10);
            
            model.leftArm.xRot = (float) Math.toRadians(45);
            model.leftArm.yRot = (float) Math.toRadians(10);
            model.leftArm.zRot = (float) Math.toRadians(-10);
            
            model.rightLeg.xRot = (float) Math.toRadians(-20 + bob * 2);
            model.rightLeg.yRot = (float) Math.toRadians(3);
            model.rightLeg.zRot = (float) Math.toRadians(1);
            model.leftLeg.xRot = (float) Math.toRadians(-15 - bob * 2);
            model.leftLeg.yRot = (float) Math.toRadians(-3);
            model.leftLeg.zRot = (float) Math.toRadians(-1);
        }
    }

    private static void applyMechaVulcan(HumanoidModel<?> model, float time) {
        float shake = Mth.sin(time * 30.0F) * 0.05F;
        model.body.xRot = (float) Math.toRadians(15) + shake;
        model.rightArm.xRot = (float) Math.toRadians(-90);
        model.rightArm.yRot = (float) Math.toRadians(-5);
        model.leftArm.xRot = (float) Math.toRadians(-90);
        model.leftArm.yRot = (float) Math.toRadians(5);
        
        // Sweep legs backward and lock yaw/roll
        model.rightLeg.xRot = (float) Math.toRadians(-40);
        model.rightLeg.yRot = 0;
        model.rightLeg.zRot = 0;
        
        model.leftLeg.xRot = (float) Math.toRadians(-40);
        model.leftLeg.yRot = 0;
        model.leftLeg.zRot = 0;
    }

    private static void applyMechaDash(HumanoidModel<?> model, float time) {
        // Reset all limb sways completely to prevent walking sway during dash
        model.rightArm.yRot = 0;
        model.rightArm.zRot = 0;
        model.leftArm.yRot = 0;
        model.leftArm.zRot = 0;
        model.rightLeg.yRot = 0;
        model.rightLeg.zRot = 0;
        model.leftLeg.yRot = 0;
        model.leftLeg.zRot = 0;

        // Aerodynamic rocket pose (not horizontal, body tilted at 45 degrees)
        model.body.xRot = (float) Math.toRadians(45);
        model.body.yRot = 0;
        model.body.zRot = 0;
        
        // Head tilted up to look forward
        model.head.xRot = (float) Math.toRadians(-35);
        model.head.yRot = 0;
        model.head.zRot = 0;
        
        // Arms swept straight back
        model.rightArm.xRot = (float) Math.toRadians(85);
        model.rightArm.zRot = (float) Math.toRadians(5);
        model.leftArm.xRot = (float) Math.toRadians(85);
        model.leftArm.zRot = (float) Math.toRadians(-5);
        
        // Legs trailing back together (swept backward)
        model.rightLeg.xRot = (float) Math.toRadians(-50);
        model.rightLeg.zRot = 0;
        model.leftLeg.xRot = (float) Math.toRadians(-50);
        model.leftLeg.zRot = 0;
    }

    private static void applyMechaSpindashCharge(HumanoidModel<?> model, float time) {
        float pulse = Mth.sin(time * 12.0F) * 0.1F;
        
        // Curl body forward significantly
        model.body.xRot = (float) Math.toRadians(45 + pulse * 15);
        model.body.yRot = 0;
        model.body.zRot = 0;
        
        // Head tucked down
        model.head.xRot = (float) Math.toRadians(20);
        model.head.yRot = 0;
        model.head.zRot = 0;
        
        // Arms tucked back/inwards
        model.rightArm.xRot = (float) Math.toRadians(60);
        model.rightArm.yRot = 0;
        model.rightArm.zRot = (float) Math.toRadians(25);
        model.leftArm.xRot = (float) Math.toRadians(60);
        model.leftArm.yRot = 0;
        model.leftArm.zRot = (float) Math.toRadians(-25);
        
        // Legs swept far backward (not forward)
        model.rightLeg.xRot = (float) Math.toRadians(-75);
        model.rightLeg.yRot = 0;
        model.rightLeg.zRot = 0;
        
        model.leftLeg.xRot = (float) Math.toRadians(-75);
        model.leftLeg.yRot = 0;
        model.leftLeg.zRot = 0;
    }

    private static void applyMechaSpindashAttack(HumanoidModel<?> model, float time) {
        // Esconder extremidades para simular bola
        model.rightArm.visible = false;
        model.leftArm.visible = false;
        model.rightLeg.visible = false;
        model.leftLeg.visible = false;
        model.head.visible = false;
        if (model instanceof PlayerModel<?> pm) {
            pm.hat.visible = false;
        }
    }

    private static void applyHolySlashRight(HumanoidModel<?> model, float time, Player player) {
        float swingProgress = model.attackTime;
        float eased = 1.0F - (1.0F - swingProgress) * (1.0F - swingProgress);
        model.body.yRot = (float) Math.toRadians(-20 - 10 * eased);
        model.rightArm.xRot = (float) Math.toRadians(-30 - 70 * eased);
        model.rightArm.zRot = (float) Math.toRadians(-20 + 10 * eased);
        model.leftArm.xRot = (float) Math.toRadians(15);
        model.leftArm.zRot = (float) Math.toRadians(-10);
    }

    private static void applyHolySlashLeft(HumanoidModel<?> model, float time, Player player) {
        float swingProgress = model.attackTime;
        float eased = 1.0F - (1.0F - swingProgress) * (1.0F - swingProgress);
        model.body.yRot = (float) Math.toRadians(20 + 10 * eased);
        model.leftArm.xRot = (float) Math.toRadians(-30 - 70 * eased);
        model.leftArm.zRot = (float) Math.toRadians(20 - 10 * eased);
        model.rightArm.xRot = (float) Math.toRadians(15);
        model.rightArm.zRot = (float) Math.toRadians(10);
    }

    private static void applyHolySlashX(HumanoidModel<?> model, float time, Player player) {
        float swingProgress = model.attackTime;
        float eased = 1.0F - (1.0F - swingProgress) * (1.0F - swingProgress);
        
        // Inclinar torso ligeramente hacia adelante al golpear
        model.body.xRot = (float) Math.toRadians(15 * eased);
        
        // Brazo derecho: tajo diagonal descendente (de arriba-derecha a abajo-izquierda)
        model.rightArm.xRot = (float) Math.toRadians(-80 + 130 * eased);
        model.rightArm.yRot = (float) Math.toRadians(-40 + 70 * eased);
        model.rightArm.zRot = (float) Math.toRadians(-30 + 20 * eased);
        
        // Brazo izquierdo: tajo diagonal descendente (de arriba-izquierda a abajo-derecha)
        model.leftArm.xRot = (float) Math.toRadians(-80 + 130 * eased);
        model.leftArm.yRot = (float) Math.toRadians(40 - 70 * eased);
        model.leftArm.zRot = (float) Math.toRadians(30 - 20 * eased);
    }

    private static void applyHolyAnnihilation(HumanoidModel<?> model, float time, Player player) {
        net.minecraft.nbt.CompoundTag pData = player.getPersistentData();
        int clientTicks = pData.getInt("xebHolyAnnihilationTicksClient");
        
        // Ninja landing pose para los primeros 8 ticks
        if (clientTicks <= 8) {
            // Piernas dobladas (agachado ninja)
            model.rightLeg.xRot = (float) Math.toRadians(-45);
            model.leftLeg.xRot = (float) Math.toRadians(-45);
            model.rightLeg.yRot = (float) Math.toRadians(15);
            model.leftLeg.yRot = (float) Math.toRadians(-15);
            
            // Torso inclinado hacia adelante
            model.body.xRot = (float) Math.toRadians(40);
            
            // Cabeza mirando al frente/arriba
            model.head.xRot = (float) Math.toRadians(-20);
            
            // Espadas sostenidas hacia atrás/afuera (posición de guardia baja/sigilo)
            model.rightArm.xRot = (float) Math.toRadians(60);
            model.rightArm.yRot = (float) Math.toRadians(-30);
            model.leftArm.xRot = (float) Math.toRadians(60);
            model.leftArm.yRot = (float) Math.toRadians(30);
        } else {
            // Giro de tornado después del tick 8
            // Brazos totalmente extendidos horizontalmente sosteniendo ambas espadas
            model.rightArm.xRot = (float) Math.toRadians(-90);
            model.rightArm.zRot = (float) Math.toRadians(-75);
            model.leftArm.xRot = (float) Math.toRadians(-90);
            model.leftArm.zRot = (float) Math.toRadians(75);
            
            // De pie derecho mientras gira
            model.rightLeg.xRot = 0.0F;
            model.leftLeg.xRot = 0.0F;
            model.body.xRot = 0.0F;
        }
    }

    private static void applyHolyIdle(HumanoidModel<?> model, float time) {
        model.rightArm.xRot = (float) Math.toRadians(15);
        model.rightArm.zRot = (float) Math.toRadians(5);
        model.leftArm.xRot = (float) Math.toRadians(15);
        model.leftArm.zRot = (float) Math.toRadians(-5);
    }

    private static void applyHolyBlastCharge(HumanoidModel<?> model, float time, Player player) {
        int charge = player.getPersistentData().getInt("xebHolyBlastCharge");
        float progress = Math.min(1.0F, charge / 160.0F);

        // Los brazos se van cruzando/acercando progresivamente hacia el pecho
        model.rightArm.xRot = (float) Math.toRadians(-15 - 35 * progress);
        model.rightArm.yRot = (float) Math.toRadians(-45 * progress);
        model.rightArm.zRot = (float) Math.toRadians(-10 * progress);

        model.leftArm.xRot = (float) Math.toRadians(-15 - 35 * progress);
        model.leftArm.yRot = (float) Math.toRadians(45 * progress);
        model.leftArm.zRot = (float) Math.toRadians(10 * progress);
    }

    private static void applyHolyBlastCast(HumanoidModel<?> model, float time, Player player) {
        int castTicks = player.getPersistentData().getInt("xebHolyBlastCastTicks");
        float progress = Math.min(1.0F, castTicks / 10.0F); // 1.0 down to 0.0

        // Brazos estirados con gran fuerza hacia afuera (recoil del cast)
        model.rightArm.xRot = (float) Math.toRadians(-10 * progress);
        model.rightArm.yRot = (float) Math.toRadians(80 * progress);
        model.rightArm.zRot = (float) Math.toRadians(85 * progress);

        model.leftArm.xRot = (float) Math.toRadians(-10 * progress);
        model.leftArm.yRot = (float) Math.toRadians(-80 * progress);
        model.leftArm.zRot = (float) Math.toRadians(-85 * progress);

        // Cuerpo ligeramente inclinado hacia atrás
        model.body.xRot = (float) Math.toRadians(-15 * progress);
    }
}
