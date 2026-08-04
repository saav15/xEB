package org.xeb.xeb.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.client.renderer.BeamStruggleRenderer;
import org.xeb.xeb.client.renderer.BeamStruggleRenderer.ClientStruggleData;

/**
 * Manejador cinemático avanzado de cámara para duelos de rayos (Beam Struggles).
 * Incluye protección anti-colisión contra bloques (evita atravesar paredes)
 * y ángulos cinemáticos adaptativos para enfrentamientos a larga distancia.
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

        int localId = mc.player.getId();
        if (localId != struggle.ownerAEntityId && localId != struggle.ownerBEntityId) {
            cameraActive = false;
            currentLerpedPos = null;
            currentLerpedLookAt = null;
            return;
        }

        Entity entityA = mc.level.getEntity(struggle.ownerAEntityId);
        Entity entityB = mc.level.getEntity(struggle.ownerBEntityId);
        if (entityA == null || entityB == null) return;

        Vec3 posA = entityA.getPosition(partialTicks).add(0, entityA.getBbHeight() * 0.5D, 0);
        Vec3 posB = entityB.getPosition(partialTicks).add(0, entityB.getBbHeight() * 0.5D, 0);
        Vec3 stableCenter = posA.add(posB).scale(0.5D);
        Vec3 collisionPoint = struggle.collisionPoint;

        boolean isPlayerA = localId == struggle.ownerAEntityId;
        Vec3 myPos = isPlayerA ? posA : posB;
        Vec3 enemyPos = isPlayerA ? posB : posA;

        float myPoints = isPlayerA ? struggle.pointsA : struggle.pointsB;
        float enemyPoints = isPlayerA ? struggle.pointsB : struggle.pointsA;
        float advantage = myPoints - enemyPoints;

        // Ángulo de órbita y zoom dinámicos
        targetOrbitAngle = advantage * 0.3D;
        float totalPoints = myPoints + enemyPoints;
        float winProximity = totalPoints > 0 ? Math.abs(advantage) / totalPoints : 0.0F;
        targetZoom = 1.0D + winProximity * 0.5D;

        double lerpFactor = 0.05D;
        currentOrbitAngle += (targetOrbitAngle - currentOrbitAngle) * lerpFactor;
        currentZoom += (targetZoom - currentZoom) * lerpFactor;

        Vec3 beamDir = posB.subtract(posA).normalize();
        double beamDistance = posA.distanceTo(posB);
        double clampedBeamDist = net.minecraft.util.Mth.clamp((float) beamDistance, 4.0F, 16.0F);

        // Proyectar y sujetar el punto de mira para que JAMÁS sobrepase el cuerpo del oponente ni del jugador
        Vec3 desiredLookAt = collisionPoint;
        if (beamDistance > 0.001D) {
            Vec3 toCol = collisionPoint.subtract(posA);
            double proj = toCol.dot(beamDir);
            proj = net.minecraft.util.Mth.clamp(proj, 0.8D, Math.max(0.8D, beamDistance - 0.8D));
            desiredLookAt = posA.add(beamDir.scale(proj));
        }

        Vec3 perp = beamDir.cross(new Vec3(0, 1, 0)).normalize();
        if (perp.lengthSqr() < 0.01) perp = beamDir.cross(new Vec3(1, 0, 0)).normalize();

        // Selección de modos cinemáticos compactos
        boolean isLongDistance = beamDistance > 14.0D;
        int totalModes = isLongDistance ? 6 : 4;
        int mode = (struggle.ticksElapsed / 80) % totalModes;

        Vec3 desiredCamPos;

        Vec3 viewDirToEnemy = enemyPos.subtract(myPos).normalize();
        Vec3 rightDir = viewDirToEnemy.cross(new Vec3(0, 1, 0)).normalize();
        if (rightDir.lengthSqr() < 0.01) rightDir = viewDirToEnemy.cross(new Vec3(1, 0, 0)).normalize();

        switch (mode) {
            case 1: { // Modo 1: Cenital Aéreo Suave sobre el centro del combate
                double timeAngle = (struggle.ticksElapsed + partialTicks) * 0.015D;
                Vec3 spinDir = new Vec3(Math.cos(timeAngle), 0, Math.sin(timeAngle));
                double height = 4.5D + clampedBeamDist * 0.12D;
                desiredCamPos = stableCenter.add(0, height, 0).add(spinDir.scale(3.0D));
                break;
            }
            case 2: { // Modo 2: Sobre el Hombro del Jugador Local (Estable)
                desiredCamPos = myPos.add(viewDirToEnemy.scale(-1.8D)).add(rightDir.scale(0.8D)).add(0, 1.6D, 0);
                break;
            }
            case 3: { // Modo 3: Contrapicado Bajo Enfocado desde el centro
                double sideDist = 3.2D + (clampedBeamDist * 0.08D);
                desiredCamPos = stableCenter.subtract(perp.scale(sideDist)).add(0, -0.4D, 0);
                desiredLookAt = collisionPoint.add(0, 0.3D, 0);
                break;
            }
            case 4: { // Modo 4 (Larga Distancia): Panorama Controlado
                double arenaSide = 5.0D + clampedBeamDist * 0.15D;
                double arenaHeight = 2.5D + clampedBeamDist * 0.10D;
                desiredCamPos = stableCenter.add(perp.scale(arenaSide)).add(0, arenaHeight, 0);
                break;
            }
            case 5: { // Modo 5 (Larga Distancia): Corredor Longitudinal
                desiredCamPos = myPos.add(viewDirToEnemy.scale(-2.5D)).add(0, 1.8D, 0);
                break;
            }
            case 0:
            default: { // Modo 0: Órbita Lateral 3/4 Centrada
                double distMult = 1.0D + (clampedBeamDist - 4.0D) * 0.03D;
                double sideOffset = 5.0D * distMult * Math.cos(currentOrbitAngle) / currentZoom;
                double upOffset = 2.4D * distMult / currentZoom;
                double forwardOffset = 3.0D * distMult * Math.sin(currentOrbitAngle) / currentZoom;
                desiredCamPos = stableCenter
                        .add(perp.scale(sideOffset))
                        .add(0, upOffset, 0)
                        .add(beamDir.scale(forwardOffset));
                break;
            }
        }

        // === PROTECCIÓN ANTI-COLISIÓN DE BLOQUES (RAYTRACE CONTRA TERRENO/ESTRUCTURAS) ===
        ClipContext clipContext = new ClipContext(
                desiredLookAt,
                desiredCamPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        );
        BlockHitResult hitResult = mc.level.clip(clipContext);
        if (hitResult.getType() != HitResult.Type.MISS) {
            Vec3 hitVec = hitResult.getLocation();
            Vec3 dir = desiredCamPos.subtract(desiredLookAt).normalize();
            // Desplaza la posición de la cámara a 0.35 bloques antes del punto de impacto con la pared
            desiredCamPos = hitVec.subtract(dir.scale(0.35D));
        }

        // Inicialización suave
        if (!cameraActive || currentLerpedPos == null || currentLerpedLookAt == null) {
            currentLerpedPos = currentPos;
            currentLerpedLookAt = currentPos.add(mc.player.getLookAngle().scale(4.0D));
            cameraActive = true;
        }

        // Interpolación fluida de movimiento (Lerp)
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

        Vec3 lookDir = currentLerpedLookAt.subtract(currentLerpedPos).normalize();
        float newYaw = (float) Math.toDegrees(Math.atan2(-lookDir.x, lookDir.z));
        float newPitch = (float) -Math.toDegrees(Math.asin(lookDir.y));

        setter.set(currentLerpedPos, newPitch, newYaw);
    }
}
