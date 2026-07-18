package org.xeb.xeb.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.client.renderer.BeamStruggleRenderer;
import org.xeb.xeb.client.renderer.BeamStruggleRenderer.ClientStruggleData;

/**
 * Cinematic camera handler for Beam Struggles.
 *
 * <p>When a struggle is active and the local player is a participant,
 * the camera smoothly transitions to a 3/4 angle view showing both
 * players and the collision point.
 */
public class BeamStruggleCameraHandler {

    private static boolean cameraActive = false;
    private static double currentOrbitAngle = 0.0D;
    private static double targetOrbitAngle = 0.0D;
    private static double currentZoom = 1.0D;
    private static double targetZoom = 1.0D;
    
    @FunctionalInterface
    public interface CameraSetter {
        void set(Vec3 position, float pitch, float yaw);
    }

    public static void overrideCamera(Camera camera, Vec3 currentPos, float currentPitch, float currentYaw, float partialTicks, CameraSetter setter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            cameraActive = false;
            return;
        }

        ClientStruggleData struggle = BeamStruggleRenderer.getLocalPlayerStruggle();
        if (struggle == null || struggle.phase != 1) { // 1 = ACTIVE
            cameraActive = false;
            return;
        }

        // Only override camera for participants
        int localId = mc.player.getId();
        if (localId != struggle.ownerAEntityId && localId != struggle.ownerBEntityId) {
            cameraActive = false;
            return;
        }

        cameraActive = true;

        // === CALCULATE CAMERA POSITION ===
        Entity entityA = mc.level.getEntity(struggle.ownerAEntityId);
        Entity entityB = mc.level.getEntity(struggle.ownerBEntityId);
        if (entityA == null || entityB == null) return;

        Vec3 posA = entityA.getPosition(partialTicks);
        Vec3 posB = entityB.getPosition(partialTicks);
        Vec3 midPoint = struggle.collisionPoint;

        // Calculate advantage (who's winning)
        boolean isPlayerA = localId == struggle.ownerAEntityId;
        float myPoints = isPlayerA ? struggle.pointsA : struggle.pointsB;
        float enemyPoints = isPlayerA ? struggle.pointsB : struggle.pointsA;
        float advantage = myPoints - enemyPoints; // positive = winning, negative = losing

        // Orbit angle based on advantage
        targetOrbitAngle = advantage * 0.3D; // radians, max ~0.9 rad

        // Zoom based on how close someone is to winning
        float totalPoints = myPoints + enemyPoints;
        float winProximity = totalPoints > 0 ? Math.abs(advantage) / totalPoints : 0.0F;
        targetZoom = 1.0D + winProximity * 0.5D; // zoom up to 1.5x when someone is dominating

        // Smooth interpolation
        double lerpFactor = 0.05D;
        currentOrbitAngle += (targetOrbitAngle - currentOrbitAngle) * lerpFactor;
        currentZoom += (targetZoom - currentZoom) * lerpFactor;

        // === APPLY CAMERA OFFSET ===
        Vec3 beamDir = posB.subtract(posA).normalize();

        // Perpendicular to beam (sideways view)
        Vec3 perp = beamDir.cross(new Vec3(0, 1, 0)).normalize();
        if (perp.lengthSqr() < 0.01) perp = beamDir.cross(new Vec3(1, 0, 0)).normalize();

        // Camera position: offset to the side + up, looking at midpoint
        double sideOffset = 6.0D * Math.cos(currentOrbitAngle) / currentZoom;
        double upOffset = 3.0D / currentZoom;
        double forwardOffset = 4.0D * Math.sin(currentOrbitAngle) / currentZoom;

        Vec3 desiredCamPos = midPoint
                .add(perp.scale(sideOffset))
                .add(0, upOffset, 0)
                .add(beamDir.scale(forwardOffset));

        // Smoothly move camera toward desired position
        double camLerp = 0.1D;
        double newCamX = currentPos.x + (desiredCamPos.x - currentPos.x) * camLerp;
        double newCamY = currentPos.y + (desiredCamPos.y - currentPos.y) * camLerp;
        double newCamZ = currentPos.z + (desiredCamPos.z - currentPos.z) * camLerp;

        Vec3 newCamPos = new Vec3(newCamX, newCamY, newCamZ);

        // Look at midpoint
        Vec3 lookDir = midPoint.subtract(newCamPos).normalize();
        float newYaw = (float) Math.toDegrees(Math.atan2(-lookDir.x, lookDir.z));
        float newPitch = (float) -Math.toDegrees(Math.asin(lookDir.y));

        setter.set(newCamPos, newPitch, newYaw);
    }
}
