package org.xeb.xeb.entity.ai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.beamstruggle.BeamStruggleManager;
import org.xeb.xeb.opticblast.ActiveBeamManager;
import org.xeb.xeb.opticblast.BeamData;
import org.xeb.xeb.util.StevenParticleHelper;
import org.xeb.xeb.util.XebBeamRaycastHelper;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class BossBeamAttackGoal extends Goal {

    public enum Phase {
        CHARGING,
        FIRING,
        COOLDOWN
    }

    private final Mob mob;
    private final int chargeTicks;
    private final int fireTicks;
    private final int cooldownTicks;
    private final double maxRange;
    private final float damagePerTick;
    private final float maxBlastResistance;
    private final float trackingDegreesPerTick;
    private final boolean destroyBlocks;

    private Phase currentPhase = Phase.CHARGING;
    private int phaseTicks = 0;
    private Vec3 currentBeamDir = Vec3.ZERO;

    public BossBeamAttackGoal(Mob mob, int chargeTicks, int fireTicks, int cooldownTicks,
                              double maxRange, float damagePerTick, float maxBlastResistance,
                              float trackingDegreesPerTick, boolean destroyBlocks) {
        this.mob = mob;
        this.chargeTicks = chargeTicks;
        this.fireTicks = fireTicks;
        this.cooldownTicks = cooldownTicks;
        this.maxRange = maxRange;
        this.damagePerTick = damagePerTick;
        this.maxBlastResistance = maxBlastResistance;
        this.trackingDegreesPerTick = trackingDegreesPerTick;
        this.destroyBlocks = destroyBlocks;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.distanceToSqr(target) <= maxRange * maxRange;
    }

    @Override
    public void start() {
        this.currentPhase = Phase.CHARGING;
        this.phaseTicks = 0;
        LivingEntity target = mob.getTarget();
        if (target != null) {
            this.currentBeamDir = target.getEyePosition().subtract(mob.getEyePosition()).normalize();
        } else {
            this.currentBeamDir = mob.getLookAngle().normalize();
        }
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        this.phaseTicks++;

        Vec3 origin = mob.getEyePosition();

        switch (currentPhase) {
            case CHARGING -> {
                mob.getNavigation().stop();
                if (target != null) {
                    mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }

                if (mob.level() instanceof ServerLevel serverLevel) {
                    // Charging particles converging onto firing point
                    serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, origin.x, origin.y, origin.z,
                            4, 0.4, 0.4, 0.4, 0.05);
                }

                if (phaseTicks >= chargeTicks) {
                    currentPhase = Phase.FIRING;
                    phaseTicks = 0;
                }
            }

            case FIRING -> {
                mob.getNavigation().stop();

                // Recalculate target direction with tracking limit (trackingDegreesPerTick)
                if (target != null) {
                    Vec3 idealDir = target.getEyePosition().subtract(origin).normalize();
                    if (trackingDegreesPerTick > 0.0F) {
                        double angleBetween = Math.acos(Math.max(-1.0, Math.min(1.0, currentBeamDir.dot(idealDir))));
                        double maxRad = Math.toRadians(trackingDegreesPerTick);
                        if (angleBetween > maxRad) {
                            double step = maxRad / angleBetween;
                            currentBeamDir = currentBeamDir.scale(1.0 - step).add(idealDir.scale(step)).normalize();
                        } else {
                            currentBeamDir = idealDir;
                        }
                    } else {
                        // Fixed beam direction during firing
                    }
                }

                // Stepped raycast with block resistance and optional block destruction
                XebBeamRaycastHelper.BeamRaycastResult rayResult = XebBeamRaycastHelper.raycastAndDestroyBlocks(
                        mob.level(), mob, origin, currentBeamDir, maxRange, maxBlastResistance, destroyBlocks
                );

                Vec3 beamEnd = rayResult.end();

                if (mob.level() instanceof ServerLevel serverLevel) {
                    long gameTick = serverLevel.getGameTime();

                    boolean inStruggle = BeamStruggleManager.isEntityInStruggle(mob.getUUID());
                    if (inStruggle) {
                        Vec3 struggleCol = BeamStruggleManager.getCollisionPointFor(mob.getUUID());
                        if (struggleCol != null) {
                            beamEnd = struggleCol;
                        }
                    }

                    BeamData beam = new BeamData(mob.getUUID(), mob.getId(), origin, beamEnd, 0xFF050505, gameTick, gameTick + 5, "boss_beam");
                    ActiveBeamManager.get().putBeam(mob.getUUID(), beam);

                    // Impact particles (only when NOT in struggle)
                    if (rayResult.hitSolidBlock() && !inStruggle) {
                        StevenParticleHelper.spawnLaserImpactParticles(serverLevel, beamEnd);
                    }

                    if (!inStruggle) {
                        // Check Beam Struggle collision vs other beams
                        Vec3 collision = ActiveBeamManager.get().checkBeamVsBeamCollision(mob.getUUID(), origin, beamEnd, 0.8D);
                        if (collision != null) {
                            UUID otherOwner = ActiveBeamManager.get().findCollidingBeamOwner(mob.getUUID(), origin, beamEnd, 0.8D);
                            if (otherOwner == null && target != null) {
                                otherOwner = target.getUUID();
                            }
                            if (otherOwner != null) {
                                Vec3 otherStart = origin;
                                if (serverLevel.getEntity(otherOwner) instanceof LivingEntity otherLiving) {
                                    otherStart = otherLiving.getEyePosition();
                                }
                                BeamStruggleManager.onBeamCollision(mob.getUUID(), otherOwner, origin, otherStart, collision, gameTick, serverLevel);
                            }
                        }

                        // Deal damage per tick along the beam line segment (ONLY when NOT in active struggle)
                        AABB searchBox = new AABB(origin, beamEnd).inflate(1.2D);
                        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
                                e -> e != mob && e.isAlive());

                        for (LivingEntity victim : victims) {
                            if (victim.getBoundingBox().inflate(0.4D).clip(origin, beamEnd).isPresent()) {
                                victim.hurt(mob.damageSources().indirectMagic(mob, mob), damagePerTick);
                            }
                        }
                    }

                    if (phaseTicks % 5 == 0) {
                        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                                SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.2F, 0.5F);
                    }
                }

                if (phaseTicks >= fireTicks) {
                    if (mob.level() instanceof ServerLevel) {
                        ActiveBeamManager.get().removeBeam(mob.getUUID());
                    }
                    currentPhase = Phase.COOLDOWN;
                    phaseTicks = 0;
                }
            }

            case COOLDOWN -> {
                if (phaseTicks >= cooldownTicks) {
                    stop();
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return currentPhase != Phase.COOLDOWN || phaseTicks < cooldownTicks;
    }

    @Override
    public void stop() {
        if (mob.level() instanceof ServerLevel) {
            ActiveBeamManager.get().removeBeam(mob.getUUID());
        }
        this.phaseTicks = 0;
        this.currentPhase = Phase.CHARGING;
    }
}
