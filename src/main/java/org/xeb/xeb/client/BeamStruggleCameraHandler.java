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
    
    private static Vec3 currentLerpedPos = null;
    private static Vec3 currentLerpedLookAt = null;
    
    @FunctionalInterface
    public interface CameraSetter {
        void set(Vec3 position, float pitch, float yaw);
    }

    public static void overrideCamera(Camera camera, Vec3 currentPos, float currentPitch, float currentYaw, float partialTicks, CameraSetter setter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            cameraActive = false;
            currentLerpedPos = null;
            currentLerpedLookAt = null;
            return;
        }

        ClientStruggleData struggle = BeamStruggleRenderer.getLocalPlayerStruggle();
        if (struggle == null || struggle.phase != 1) { // 1 = ACTIVE
            cameraActive = false;
            currentLerpedPos = null;
            currentLerpedLookAt = null;
            return;
        }

        // Only override camera for participants
        int localId = mc.player.getId();
        if (localId != struggle.ownerAEntityId && localId != struggle.ownerBEntityId) {
            cameraActive = false;
            currentLerpedPos = null;
            currentLerpedLookAt = null;
            return;
        }

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

        Vec3 beamDir = posB.subtract(posA).normalize();
        Vec3 perp = beamDir.cross(new Vec3(0, 1, 0)).normalize();
        if (perp.lengthSqr() < 0.01) perp = beamDir.cross(new Vec3(1, 0, 0)).normalize();

        // Cycle camera mode every 80 ticks (4 seconds)
        int mode = (struggle.ticksElapsed / 80) % 4;

        Vec3 desiredCamPos;
        Vec3 desiredLookAt = midPoint;

        switch (mode) {
            case 1: { // Mode 1: High Aerial Overhead (Slow Spin)
                double timeAngle = (struggle.ticksElapsed + partialTicks) * 0.015D;
                Vec3 spinDir = new Vec3(Math.cos(timeAngle), 0, Math.sin(timeAngle));
                desiredCamPos = midPoint.add(0, 7.5D, 0).add(spinDir.scale(3.5D));
                break;
            }
            case 2: { // Mode 2: Over-The-Shoulder
                boolean behindA = (struggle.ticksElapsed / 40) % 2 == 0;
                Vec3 sourcePos = behindA ? posA : posB;
                Vec3 destPos = behindA ? posB : posA;
                Vec3 viewDir = destPos.subtract(sourcePos).normalize();
                Vec3 rightDir = viewDir.cross(new Vec3(0, 1, 0)).normalize();
                if (rightDir.lengthSqr() < 0.01) rightDir = viewDir.cross(new Vec3(1, 0, 0)).normalize();
                desiredCamPos = sourcePos.add(viewDir.scale(-1.5D)).add(rightDir.scale(0.8D)).add(0, 1.8D, 0);
                break;
            }
            case 3: { // Mode 3: Low Angle Closeup looking up
                desiredCamPos = midPoint.subtract(perp.scale(3.2D)).add(0, -0.6D, 0);
                desiredLookAt = midPoint.add(0, 0.4D, 0);
                break;
            }
            case 0:
            default: { // Mode 0: Sideways 3/4 Orbiting
                double sideOffset = 6.0D * Math.cos(currentOrbitAngle) / currentZoom;
                double upOffset = 3.0D / currentZoom;
                double forwardOffset = 4.0D * Math.sin(currentOrbitAngle) / currentZoom;
                desiredCamPos = midPoint
                        .add(perp.scale(sideOffset))
                        .add(0, upOffset, 0)
                        .add(beamDir.scale(forwardOffset));
                break;
            }
        }

        // Initialize lerp positions smoothly on first activation
        if (!cameraActive || currentLerpedPos == null || currentLerpedLookAt == null) {
            currentLerpedPos = currentPos;
            currentLerpedLookAt = currentPos.add(mc.player.getLookAngle().scale(4.0D));
            cameraActive = true;
        }

        // Interpolate smoothly
        double camLerp = 0.08D;
        currentLerpedPos = new Vec3(
                currentLerpedPos.x + (desiredCamPos.x - currentLerpedPos.x) * camLerp,
                currentLerpedPos.y + (desiredCamPos.y - currentLerpedPos.y) * camLerp,
                currentLerpedPos.z + (desiredCamPos.z - currentLerpedPos.z) * camLerp
        );
        currentLerpedLookAt = new Vec3(
                currentLerpedLookAt.x + (desiredLookAt.x - currentLerpedLookAt.x) * camLerp,
                currentLerpedLookAt.y + (desiredLookAt.y - currentLerpedLookAt.y) * camLerp,
                currentLerpedLookAt.z + (desiredLookAt.z - currentLerpedLookAt.z) * camLerp
        );

        // Apply orientation looking at the lerped target point
        Vec3 lookDir = currentLerpedLookAt.subtract(currentLerpedPos).normalize();
        float newYaw = (float) Math.toDegrees(Math.atan2(-lookDir.x, lookDir.z));
        float newPitch = (float) -Math.toDegrees(Math.asin(lookDir.y));

        setter.set(currentLerpedPos, newPitch, newYaw);
    }
}
