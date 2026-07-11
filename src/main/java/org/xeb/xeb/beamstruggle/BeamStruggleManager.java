package org.xeb.xeb.beamstruggle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.network.BeamStruggleEndPacket;
import org.xeb.xeb.network.BeamStrugglePacket;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.opticblast.ActiveBeamManager;
import org.xeb.xeb.opticblast.BeamData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BeamStruggleManager {

    public enum StrugglePhase {
        PREP, ACTIVE, RESOLVED
    }

    private static final Map<UUID, BeamStruggle> ACTIVE_STRUGGLES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> OWNER_TO_STRUGGLE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();

    public static final int MAX_STRUGGLE_TICKS = 320;
    public static final double MASH_POINT_PER_CLICK = 1.0;
    public static final double MASH_DECAY_PER_TICK = 0.05;
    public static final double COLLISION_MOVE_PER_POINT = 0.15;
    public static final int PREP_DURATION_TICKS = 40;
    public static final double POINT_BLANK_DISTANCE = 4.0;
    public static final double WIN_DISTANCE_NORMAL = 6.0;
    public static final double WIN_DISTANCE_POINT_BLANK = 2.0;
    public static final int COOLDOWN_TICKS = 30;

    // Concentration loss thresholds
    public static final double LOOK_DEVIATION_THRESHOLD = 0.35D; // ~70 grados de desviación del look original
    public static final double LOOK_DEVIATION_CRITICAL = 0.55D; // ~110 grados = pérdida instantánea

    public static class BeamStruggle {
        public final UUID struggleId;
        public final UUID ownerA;
        public final UUID ownerB;
        public final int ownerAEntityId;
        public final int ownerBEntityId;
        public final Vec3 startPosA;
        public final Vec3 startPosB;
        public Vec3 midpoint;
        public Vec3 currentCollision;
        public double pointsA;
        public double pointsB;
        public int ticksElapsed;
        public final long startTick;
        public StrugglePhase phase;
        public final double initialDistance;
        public final double winDistance;
        public final double mashMultiplier;
        public Vec3 initialLookA; // dirección de mira original de A
        public Vec3 initialLookB; // dirección de mira original de B
        public int lastConcentrationWarningA;
        public int lastConcentrationWarningB;

        public BeamStruggle(UUID id, UUID a, UUID b, int idA, int idB, Vec3 startA, Vec3 startB,
                            Vec3 collision, long tick, StrugglePhase phase, double initialDistance,
                            double winDistance, double mashMultiplier, Vec3 lookA, Vec3 lookB) {
            this.struggleId = id;
            this.ownerA = a; this.ownerB = b;
            this.ownerAEntityId = idA; this.ownerBEntityId = idB;
            this.startPosA = startA; this.startPosB = startB;
            this.midpoint = collision;
            this.currentCollision = new Vec3(collision.x, collision.y, collision.z);
            this.startTick = tick;
            this.phase = phase;
            this.initialDistance = initialDistance;
            this.winDistance = winDistance;
            this.mashMultiplier = mashMultiplier;
            this.initialLookA = lookA;
            this.initialLookB = lookB;
            this.ticksElapsed = 0;
            this.lastConcentrationWarningA = 0;
            this.lastConcentrationWarningB = 0;
        }
    }

    public static boolean onBeamCollision(UUID ownerA, UUID ownerB, Vec3 startA, Vec3 startB,
                                          Vec3 collisionPoint, long currentTick, ServerLevel level) {
        // Cooldown anti-fantasma
        Long cdA = COOLDOWN_UNTIL.get(ownerA);
        Long cdB = COOLDOWN_UNTIL.get(ownerB);
        if (cdA != null && currentTick < cdA) return false;
        if (cdB != null && currentTick < cdB) return false;

        BeamStruggle struggle = findActiveStruggle(ownerA, ownerB);

        if (struggle == null) {
            UUID id = UUID.randomUUID();
            double distance = startA.distanceTo(startB);
            double winDist = distance < POINT_BLANK_DISTANCE ? WIN_DISTANCE_POINT_BLANK : WIN_DISTANCE_NORMAL;
            double mashMult = 1.0 + (distance / 40.0);
            if (distance < POINT_BLANK_DISTANCE) mashMult = 0.5;

            StrugglePhase initialPhase = (distance < POINT_BLANK_DISTANCE) ? StrugglePhase.ACTIVE : StrugglePhase.PREP;
            int initialTicks = (distance < POINT_BLANK_DISTANCE) ? PREP_DURATION_TICKS : 0;

            int idA = getEntityIdFromUUID(level, ownerA);
            int idB = getEntityIdFromUUID(level, ownerB);

            // Capturar look directions iniciales para detección de concentración
            Vec3 lookA = getLookDirection(level, ownerA);
            Vec3 lookB = getLookDirection(level, ownerB);

            struggle = new BeamStruggle(id, ownerA, ownerB, idA, idB, startA, startB, collisionPoint,
                    currentTick, initialPhase, distance, winDist, mashMult, lookA, lookB);
            struggle.ticksElapsed = initialTicks;
            ACTIVE_STRUGGLES.put(id, struggle);
            OWNER_TO_STRUGGLE.put(ownerA, id);
            OWNER_TO_STRUGGLE.put(ownerB, id);

            broadcastStruggleStart(struggle, level);

            level.playSound(null, collisionPoint.x, collisionPoint.y, collisionPoint.z,
                    net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.5F);
        }

        // Actualizar midpoint y currentCollision a la posición REAL de este tick
        struggle.midpoint = collisionPoint;

        struggle.ticksElapsed++;

        if (struggle.phase == StrugglePhase.PREP) {
            if (struggle.ticksElapsed >= PREP_DURATION_TICKS) {
                struggle.phase = StrugglePhase.ACTIVE;
                struggle.ticksElapsed = 0;
                broadcastStruggleUpdate(struggle, level);
            }
            return true;
        }

        if (struggle.phase == StrugglePhase.ACTIVE) {
            double advantage = struggle.pointsA - struggle.pointsB;
            Vec3 dirAtoB = struggle.startPosB.subtract(struggle.startPosA).normalize();
            Vec3 moveVector = dirAtoB.scale(advantage * COLLISION_MOVE_PER_POINT);
            struggle.currentCollision = struggle.midpoint.add(moveVector);

            double decay = MASH_DECAY_PER_TICK * struggle.mashMultiplier;
            struggle.pointsA = Math.max(0, struggle.pointsA - decay);
            struggle.pointsB = Math.max(0, struggle.pointsB - decay);

            double distFromMid = struggle.currentCollision.distanceTo(struggle.midpoint);
            double effectiveWinDistance = struggle.winDistance * (1.0 + (double) struggle.ticksElapsed / 120.0);
            if (distFromMid >= effectiveWinDistance || struggle.ticksElapsed >= MAX_STRUGGLE_TICKS) {
                resolveStruggle(struggle, level, currentTick);
                return false;
            }

            if (struggle.ticksElapsed % 20 == 0) {
                level.playSound(null, struggle.currentCollision.x, struggle.currentCollision.y, struggle.currentCollision.z,
                        net.minecraft.sounds.SoundEvents.BEACON_AMBIENT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 2.0F);
            }

            broadcastStruggleUpdate(struggle, level);
            return true;
        }

        return false;
    }

    public static void handleFlourishPress(ServerPlayer player) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(player.getUUID());
        if (struggleId == null) return;

        BeamStruggle struggle = ACTIVE_STRUGGLES.get(struggleId);
        if (struggle == null) return;
        if (struggle.phase != StrugglePhase.ACTIVE) return;

        double points = MASH_POINT_PER_CLICK * struggle.mashMultiplier;
        if (player.getUUID().equals(struggle.ownerA)) {
            struggle.pointsA += points;
        } else if (player.getUUID().equals(struggle.ownerB)) {
            struggle.pointsB += points;
        }
    }

    public static boolean isInActiveStruggle(UUID playerUUID) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(playerUUID);
        if (struggleId == null) return false;
        BeamStruggle s = ACTIVE_STRUGGLES.get(struggleId);
        return s != null && s.phase != StrugglePhase.RESOLVED;
    }

    public static boolean isInAnyStruggle(UUID playerUUID) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(playerUUID);
        if (struggleId == null) return false;
        BeamStruggle s = ACTIVE_STRUGGLES.get(struggleId);
        return s != null && s.phase != StrugglePhase.RESOLVED;
    }

    /**
     * FIX CRÍTICO: solo retornar collision point si el struggle está ACTIVO (no RESOLVED).
     * Esto previene que los beams se queden pegados en posiciones viejas.
     */
    public static Vec3 getCollisionPointFor(UUID owner) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(owner);
        if (struggleId != null) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(struggleId);
            if (s != null && s.phase != StrugglePhase.RESOLVED) {
                return s.currentCollision;
            }
            // Limpiar entrada stale
            OWNER_TO_STRUGGLE.remove(owner);
        }
        return null;
    }

    public static void updateCollisionPoint(UUID owner, Vec3 colPoint) {
        UUID struggleId = OWNER_TO_STRUGGLE.get(owner);
        if (struggleId != null) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(struggleId);
            if (s != null && s.phase != StrugglePhase.RESOLVED) {
                s.midpoint = colPoint;
            }
        }
    }

    private static BeamStruggle findActiveStruggle(UUID a, UUID b) {
        UUID idA = OWNER_TO_STRUGGLE.get(a);
        if (idA != null) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(idA);
            if (s != null && s.phase != StrugglePhase.RESOLVED && (s.ownerA.equals(b) || s.ownerB.equals(b))) return s;
        }
        UUID idB = OWNER_TO_STRUGGLE.get(b);
        if (idB != null) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(idB);
            if (s != null && s.phase != StrugglePhase.RESOLVED && (s.ownerA.equals(a) || s.ownerB.equals(a))) return s;
        }
        return null;
    }

    /**
     * Tick principal — llamado desde OpticBlastTickHandler.onServerTick().
     * Detecta:
     * 1. Struggles que expiraron por MAX_TICKS
     * 2. Struggles donde un beam dejó de existir (player soltó right-click, Brimstone se acabó, etc.)
     * 3. Struggles donde un player perdió concentración (volteó mucho o fue golpeado)
     */
    public static void tickStruggles(ServerLevel level, long currentTick) {
        List<UUID> toResolve = new ArrayList<>();

        for (BeamStruggle s : ACTIVE_STRUGGLES.values()) {
            if (s.phase == StrugglePhase.RESOLVED) continue;

            // Check 1: max ticks
            if (s.phase == StrugglePhase.ACTIVE && s.ticksElapsed >= MAX_STRUGGLE_TICKS) {
                toResolve.add(s.struggleId);
                continue;
            }

            // Check 2: beam death — si alguno de los dos beams ya no existe, ese player pierde
            boolean beamAAlive = isBeamStillActive(s.ownerA);
            boolean beamBAlive = isBeamStillActive(s.ownerB);

            if (!beamAAlive && !beamBAlive) {
                // Ambos perdieron el beam — draw
                toResolve.add(s.struggleId);
            } else if (!beamAAlive) {
                // A perdió el beam — B gana por abandono
                resolveByForfeit(s, level, s.ownerB, s.ownerA, "beam_lost");
                continue;
            } else if (!beamBAlive) {
                // B perdió el beam — A gana por abandono
                resolveByForfeit(s, level, s.ownerA, s.ownerB, "beam_lost");
                continue;
            }

            // Check 3: concentration loss (solo en phase ACTIVE)
            if (s.phase == StrugglePhase.ACTIVE) {
                checkConcentrationLoss(s, level, currentTick);
            }
        }

        for (UUID id : toResolve) {
            BeamStruggle s = ACTIVE_STRUGGLES.get(id);
            if (s != null) resolveStruggle(s, level, currentTick);
        }
    }

    /**
     * Verifica si el beam de un owner sigue activo.
     * Un beam está activo si:
     * - Está en ActiveBeamManager con tickExpires > currentTick, O
     * - El player sigue disparando (Optic Blast: isUsingItem, Brimstone: firingTicks > 0)
     */
    private static boolean isBeamStillActive(UUID ownerUUID) {
        // Check ActiveBeamManager — si el beam existe y no expiró, está activo
        BeamData beam = ActiveBeamManager.get().getBeam(ownerUUID);
        if (beam != null) {
            long currentTick = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld().getGameTime();
            if (beam.getTickExpires() > currentTick) {
                return true;
            }
        }
        // Si no hay beam en el manager, el beam está muerto
        return false;
    }

    /**
     * Detecta si un player perdió la concentración durante el struggle.
     * Pierde concentración si:
     * 1. Su look direction se desvió más de LOOK_DEVIATION_THRESHOLD del original por más de 15 ticks
     * 2. Su look direction se desvió más de LOOK_DEVIATION_CRITICAL instantáneamente
     * 3. Fue golpeado por una fuente externa (mob, otro player, etc.) — detectado via hurtTime
     */
    private static void checkConcentrationLoss(BeamStruggle struggle, ServerLevel level, long currentTick) {
        // Check owner A
        ConcentrationResult resultA = checkPlayerConcentration(struggle, struggle.ownerA, struggle.initialLookA, level);
        if (resultA.lost) {
            resolveByForfeit(struggle, level, struggle.ownerB, struggle.ownerA, resultA.reason);
            return;
        }

        // Check owner B
        ConcentrationResult resultB = checkPlayerConcentration(struggle, struggle.ownerB, struggle.initialLookB, level);
        if (resultB.lost) {
            resolveByForfeit(struggle, level, struggle.ownerA, struggle.ownerB, resultB.reason);
            return;
        }
    }

    private static ConcentrationResult checkPlayerConcentration(BeamStruggle struggle, UUID ownerUUID,
                                                                  Vec3 initialLook, ServerLevel level) {
        net.minecraft.world.entity.Entity entity = level.getEntity(getEntityIdFromUUID(level, ownerUUID));
        if (!(entity instanceof LivingEntity living)) {
            return new ConcentrationResult(true, "disconnected");
        }

        // Check 1: hurt by external source (hurtTime > 0 means recently hit)
        if (living.hurtTime > 0) {
            // Verificar que el daño no sea del beam struggle mismo (el cual no inflige daño continuo durante el struggle)
            return new ConcentrationResult(true, "hit_by_external");
        }

        // Check 2: look direction deviation
        Vec3 currentLook = living.getLookAngle();
        double dot = initialLook.dot(currentLook);
        // dot = 1 = mirando igual, dot = 0 = 90 grados, dot = -1 = mirando al revés
        double deviation = 1.0 - dot; // 0 = no deviation, 2 = 180 grados

        if (deviation >= LOOK_DEVIATION_CRITICAL) {
            return new ConcentrationResult(true, "looked_away_critical");
        }

        // Warning si está cerca del threshold
        if (deviation >= LOOK_DEVIATION_THRESHOLD) {
            // Incrementar warning counter
            if (living.getUUID().equals(struggle.ownerA)) {
                struggle.lastConcentrationWarningA++;
                // Después de 15 ticks (~0.75s) de desviación sostenida, pierde
                if (struggle.lastConcentrationWarningA > 15) {
                    return new ConcentrationResult(true, "looked_away_sustained");
                }
            } else {
                struggle.lastConcentrationWarningB++;
                if (struggle.lastConcentrationWarningB > 15) {
                    return new ConcentrationResult(true, "looked_away_sustained");
                }
            }
        } else {
            // Reset warning counter si volvió a mirar bien
            if (living.getUUID().equals(struggle.ownerA)) {
                struggle.lastConcentrationWarningA = 0;
            } else {
                struggle.lastConcentrationWarningB = 0;
            }
        }

        return new ConcentrationResult(false, "");
    }

    private record ConcentrationResult(boolean lost, String reason) {}

    /**
     * Resuelve el struggle porque un player perdió por abandono/concentración.
     * El perdedor recibe el blast impact completo.
     */
    private static void resolveByForfeit(BeamStruggle struggle, ServerLevel level, UUID winner, UUID loser, String reason) {
        // El perdedor recibe daño加倍 por perder concentración
        applyBlastImpact(level, loser, struggle.currentCollision, 2.5F, true, winner);

        // El ganador no recibe daño, solo sonido de victoria
        applyBlastImpact(level, winner, struggle.currentCollision, 0.0F, false, loser);

        // Log del reason para debug
        System.out.println("[xEB] Beam Struggle resolved by forfeit: " + reason + " | winner=" + winner + " loser=" + loser);

        cleanupStruggle(struggle);
        broadcastStruggleEnd(struggle, level, winner, loser);
    }

    private static void resolveStruggle(BeamStruggle struggle, ServerLevel level, long currentTick) {
        double advantage = struggle.pointsA - struggle.pointsB;
        UUID winner, loser;

        if (advantage > 0.1) {
            winner = struggle.ownerA; loser = struggle.ownerB;
        } else if (advantage < -0.1) {
            winner = struggle.ownerB; loser = struggle.ownerA;
        } else {
            // Draw
            applyBlastImpact(level, struggle.ownerA, struggle.currentCollision, 1.0F, false, struggle.ownerB);
            applyBlastImpact(level, struggle.ownerB, struggle.currentCollision, 1.0F, false, struggle.ownerA);
            cleanupStruggle(struggle);
            return;
        }

        applyBlastImpact(level, loser, struggle.currentCollision, 2.0F, true, winner);
        applyBlastImpact(level, winner, struggle.currentCollision, 0.0F, false, loser);

        cleanupStruggle(struggle);
        broadcastStruggleEnd(struggle, level, winner, loser);
    }

    private static void applyBlastImpact(ServerLevel level, UUID targetId, Vec3 explosionPos, float multiplier, boolean loser, UUID opponentId) {
        net.minecraft.world.entity.Entity target = level.getEntity(targetId);
        net.minecraft.world.entity.Entity opponent = opponentId != null ? level.getEntity(opponentId) : null;
        if (target instanceof LivingEntity living) {
            if (multiplier > 0) {
                if (opponent instanceof LivingEntity opponentLiving) {
                    living.setLastHurtByMob(opponentLiving);

                    String weapon = "optic_blast";
                    BeamData winnerBeam = ActiveBeamManager.get().getBeam(opponentId);
                    if (winnerBeam != null) {
                        weapon = winnerBeam.getBeamSource();
                    } else {
                        for (ExternalBeamRegistry.ExternalBeamData ext : ExternalBeamRegistry.getCurrentTickBeams()) {
                            if (ext.ownerUUID().equals(opponentId)) {
                                weapon = "external";
                                break;
                            }
                        }
                    }
                    living.getPersistentData().putString("xebLastAttackWeapon", "beam_struggle");
                    living.getPersistentData().putString("xebLastAttackType", weapon);
                    living.getPersistentData().putLong("xebLastAttackTime", level.getGameTime());
                }

                net.minecraft.world.damagesource.DamageSource source = opponent != null ?
                        level.damageSources().explosion(opponent, opponent) :
                        level.damageSources().explosion(null, null);

                living.hurt(source, 30.0F * multiplier);
                Vec3 knockback = living.position().subtract(explosionPos).normalize().scale(1.5 * multiplier);
                living.setDeltaMovement(knockback.x, knockback.y + 0.5, knockback.z);
                living.hurtMarked = true;
            }
        }

        if (multiplier > 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    explosionPos.x, explosionPos.y, explosionPos.z, 3, 0.5, 0.5, 0.5, 0.0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    explosionPos.x, explosionPos.y, explosionPos.z, 30, 1.0, 1.0, 1.0, 0.2);

            if (loser) {
                level.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                        net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.8F);
                level.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                        net.minecraft.sounds.SoundEvents.WITHER_HURT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.8F);
            }
        } else {
            level.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.0F);
            if (opponentId != null) {
                level.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                        net.minecraft.sounds.SoundEvents.WITHER_DEATH,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 2.0F);
            }
        }
    }

    private static void cleanupStruggle(BeamStruggle struggle) {
        struggle.phase = StrugglePhase.RESOLVED;
        ACTIVE_STRUGGLES.remove(struggle.struggleId);
        OWNER_TO_STRUGGLE.remove(struggle.ownerA);
        OWNER_TO_STRUGGLE.remove(struggle.ownerB);
        // Cooldown anti-fantasma
        long currentTick = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld().getGameTime();
        COOLDOWN_UNTIL.put(struggle.ownerA, currentTick + COOLDOWN_TICKS);
        COOLDOWN_UNTIL.put(struggle.ownerB, currentTick + COOLDOWN_TICKS);
    }

    private static Vec3 getLookDirection(ServerLevel level, UUID ownerUUID) {
        net.minecraft.world.entity.Entity entity = level.getEntity(getEntityIdFromUUID(level, ownerUUID));
        if (entity instanceof LivingEntity living) {
            return living.getLookAngle();
        }
        return new Vec3(0, 0, 1);
    }

    private static int getEntityIdFromUUID(ServerLevel level, UUID uuid) {
        // Buscar en players primero
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p.getUUID().equals(uuid)) return p.getId();
        }
        // Buscar en todas las entidades
        for (net.minecraft.world.entity.Entity ent : level.getAllEntities()) {
            if (ent.getUUID().equals(uuid)) return ent.getId();
        }
        return -1;
    }

    // Packet broadcasting
    private static void broadcastStruggleStart(BeamStruggle s, ServerLevel level) {
        byte phaseByte = (byte) s.phase.ordinal();
        XEBNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new BeamStrugglePacket(true, s.struggleId, s.ownerAEntityId, s.ownerBEntityId,
                        s.startPosA, s.startPosB, s.currentCollision,
                        (float) s.pointsA, (float) s.pointsB, s.ticksElapsed, phaseByte, s.initialDistance)
        );
    }

    private static void broadcastStruggleUpdate(BeamStruggle s, ServerLevel level) {
        byte phaseByte = (byte) s.phase.ordinal();
        XEBNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new BeamStrugglePacket(false, s.struggleId, s.ownerAEntityId, s.ownerBEntityId,
                        s.startPosA, s.startPosB, s.currentCollision,
                        (float) s.pointsA, (float) s.pointsB, s.ticksElapsed, phaseByte, s.initialDistance)
        );
    }

    private static void broadcastStruggleEnd(BeamStruggle s, ServerLevel level, UUID winner, UUID loser) {
        int idWinner = -1, idLoser = -1;
        net.minecraft.world.entity.Entity entWinner = level.getEntity(winner);
        if (entWinner != null) idWinner = entWinner.getId();
        net.minecraft.world.entity.Entity entLoser = level.getEntity(loser);
        if (entLoser != null) idLoser = entLoser.getId();

        XEBNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new BeamStruggleEndPacket(idWinner, idLoser)
        );
    }
}
