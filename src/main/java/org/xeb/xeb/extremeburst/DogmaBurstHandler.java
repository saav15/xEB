package org.xeb.xeb.extremeburst;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.network.BrimstoneBeamPacket;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.opticblast.ActiveBeamManager;
import org.xeb.xeb.opticblast.BeamData;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the per-tick logic of Dogma's expanding brimstone beam.
 *
 * <p>Mechanics:
 * <ul>
 *   <li>Duration: {@value #DOGMA_DURATION} ticks (10 seconds)</li>
 *   <li>Damage: {@value #DOGMA_DAMAGE_PER_TICK} per tick to all entities in path</li>
 *   <li>Beam width: grows <b>exponentially</b> from {@value #DOGMA_START_WIDTH} →
 *       {@value #DOGMA_MAX_WIDTH} blocks (progress^{@value #GROWTH_EXPONENT} curve —
 *       stays narrow for the first ~70% then explodes in the final ~30%)</li>
 *   <li>Reach: {@value #DOGMA_MAX_BEAM_REACH} blocks</li>
 *   <li>TV-static particles perpendicular to the beam every tick</li>
 *   <li>Auto-wins any beam struggle via {@link ActiveBeamManager} priority</li>
 * </ul>
 */
public class DogmaBurstHandler {

    public static final double DOGMA_DAMAGE_PER_TICK = 9.99D;
    public static final int    DOGMA_DURATION        = 200;   // 10 seconds
    public static final double DOGMA_MAX_BEAM_REACH  = 40.0D;
    /** Beam starts at 3 blocks wide (1.5 half-width). */
    public static final double DOGMA_START_WIDTH     = 3.0D;
    /** Beam ends at 10 blocks wide (5.0 half-width). */
    public static final double DOGMA_MAX_WIDTH       = 10.0D;

    /** Exponent for the growth curve — high value = slow start, explosive end. */
    private static final double GROWTH_EXPONENT = 2.5;

    /**
     * Called every server tick from {@link org.xeb.xeb.event.BuffTickHandler}
     * when {@code xebDogmaBrimstoneTicks > 0}.
     */
    public static void tick(ServerPlayer player) {
        int dogmaTicks = player.getPersistentData().getInt("xebDogmaBrimstoneTicks");
        if (dogmaTicks <= 0) {
            // ── Cleanup ───────────────────────────────────────────────────────
            if (player.getPersistentData().contains("xebDogmaBrimstoneTicks")) {
                player.getPersistentData().remove("xebDogmaBrimstoneTicks");
                player.getPersistentData().remove("xebDogmaBrimstoneMaxTicks");
                // Clear shared burst flags so other bursts can activate afterwards
                player.getPersistentData().putBoolean("xebExtremeBurstActive", false);
                player.getPersistentData().remove("xebExtremeBurstId");
                ActiveBeamManager.get().removeBeam(player.getUUID());
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new BrimstoneBeamPacket(player.getId(), false, org.xeb.xeb.entity.TearsProjectileEntity.IMBUE_NONE, java.util.Collections.emptyList()));
            }
            return;
        }

        player.getPersistentData().putInt("xebDogmaBrimstoneTicks", dogmaTicks - 1);
        ServerLevel level = player.serverLevel();

        // ── Dogma recoil push back (from halfway point) ──────────────────────
        Vec3 lookDir  = player.getLookAngle();
        if (dogmaTicks <= 100) {
            double progressRatio = (100.0 - dogmaTicks) / 100.0; // 0.0 -> 1.0
            double recoilForce   = 0.05D + 0.15D * progressRatio; // starts small, grows
            Vec3 push = lookDir.scale(-recoilForce);
            player.setDeltaMovement(player.getDeltaMovement().add(push.x, 0.01D, push.z));
            player.hurtMarked = true;
        }

        // ── Exponential beam width (3 → 10 blocks, progress^GROWTH_EXPONENT) ─
        // progress goes 0→1 as the beam ages; exponential curve means it stays
        // narrow for most of the duration then explodes at the end.
        double progress    = 1.0 - ((double) dogmaTicks / DOGMA_DURATION); // 0→1
        double expProgress = Math.pow(progress, GROWTH_EXPONENT);           // slow start
        double currentWidth = DOGMA_START_WIDTH + (DOGMA_MAX_WIDTH - DOGMA_START_WIDTH) * expProgress;
        double halfWidth    = currentWidth / 2.0;

        // ── Beam geometry from mouth position ─────────────────────────────────
        Vec3 mouthPos = player.getEyePosition(1.0F).subtract(0, 0.15D, 0);
        Vec3 beamEnd  = mouthPos.add(lookDir.scale(DOGMA_MAX_BEAM_REACH));

        // ── Build two axes perpendicular to lookDir for TV-static scatter ─────
        Vec3 right;
        if (Math.abs(lookDir.y) < 0.99) {
            right = lookDir.cross(new Vec3(0, 1, 0)).normalize();
        } else {
            right = lookDir.cross(new Vec3(1, 0, 0)).normalize();
        }
        Vec3 beamUp = lookDir.cross(right).normalize();

        // ── Damage entities in beam path ──────────────────────────────────────
        AABB beamBox = new AABB(mouthPos, beamEnd).inflate(halfWidth);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, beamBox,
                e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable());

        for (LivingEntity target : targets) {
            AABB targetBox = target.getBoundingBox().inflate(halfWidth);
            if (targetBox.clip(mouthPos, beamEnd).isPresent()) {
                target.hurt(player.damageSources().playerAttack(player), (float) DOGMA_DAMAGE_PER_TICK);
            }
        }

        // ── Register in ActiveBeamManager for beam struggle auto-win ──────────
        BeamData dogmaBeam = new BeamData(
                player.getUUID(),
                player.getId(),
                mouthPos,
                beamEnd,
                0x444444, // gray color for dogma TV static struggle representation
                level.getGameTime(),
                level.getGameTime() + 2,
                "dogma_brimstone"
        );
        ActiveBeamManager.get().putBeam(player.getUUID(), dogmaBeam);

        // ── Send beam rendering packet to clients ─────────────────────────────
        List<Vec3> points = new ArrayList<>();
        points.add(mouthPos);
        points.add(beamEnd);
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BrimstoneBeamPacket(player.getId(), true, org.xeb.xeb.entity.TearsProjectileEntity.IMBUE_DOGMA, points, (float) currentWidth));

        // ── TV-static electricity particles scattered perpendicular to beam ───
        // Decreased density and added probability check to reduce particle spam significantly
        if (level.random.nextInt(3) == 0) {
            int particlesPerSegment = Math.max(1, (int) (halfWidth * 0.4));
            for (double d = 0.5; d < DOGMA_MAX_BEAM_REACH; d += 4.0) {
                Vec3 segBase = mouthPos.add(lookDir.scale(d));

                for (int i = 0; i < particlesPerSegment; i++) {
                    double scatterR = (level.random.nextDouble() - 0.5) * currentWidth;
                    double scatterU = (level.random.nextDouble() - 0.5) * currentWidth;
                    Vec3 staticPos = segBase
                            .add(right.scale(scatterR))
                            .add(beamUp.scale(scatterU));

                    // Sparse TV static spark particles
                    if (level.random.nextBoolean()) {
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                staticPos.x, staticPos.y, staticPos.z,
                                1, 0.0, 0.0, 0.0, 0.05);
                    } else {
                        level.sendParticles(ParticleTypes.CRIT,
                                staticPos.x, staticPos.y, staticPos.z,
                                1, 0.0, 0.0, 0.0, 0.02);
                    }
                }
            }
        }

        // ── Ambient sound every second ────────────────────────────────────────
        if (dogmaTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.5F);
        }
    }
}
