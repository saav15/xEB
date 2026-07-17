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
 *   <li>Beam width: grows from {@value #DOGMA_START_WIDTH} → {@value #DOGMA_MAX_WIDTH} blocks</li>
 *   <li>Reach: {@value #DOGMA_MAX_BEAM_REACH} blocks</li>
 *   <li>Auto-wins any beam struggle via {@link ActiveBeamManager} priority</li>
 * </ul>
 */
public class DogmaBurstHandler {

    public static final double DOGMA_DAMAGE_PER_TICK = 9.99D;
    public static final int    DOGMA_DURATION        = 200;  // 10 seconds
    public static final double DOGMA_MAX_BEAM_REACH  = 40.0D;
    public static final double DOGMA_START_WIDTH     = 1.5D;
    public static final double DOGMA_MAX_WIDTH       = 6.0D;

    /**
     * Called every server tick from {@link org.xeb.xeb.event.BuffTickHandler}
     * when {@code xebDogmaBrimstoneTicks > 0}.
     */
    public static void tick(ServerPlayer player) {
        int dogmaTicks = player.getPersistentData().getInt("xebDogmaBrimstoneTicks");
        if (dogmaTicks <= 0) {
            // Cleanup
            if (player.getPersistentData().contains("xebDogmaBrimstoneTicks")) {
                player.getPersistentData().remove("xebDogmaBrimstoneTicks");
                player.getPersistentData().remove("xebDogmaBrimstoneMaxTicks");
                player.getPersistentData().putBoolean("xebExtremeBurstActive", false);
                ActiveBeamManager.get().removeBeam(player.getUUID());
            }
            return;
        }

        player.getPersistentData().putInt("xebDogmaBrimstoneTicks", dogmaTicks - 1);
        ServerLevel level = player.serverLevel();

        // ── Beam width interpolation (grows 1.5 → 6.0 over duration) ──────────
        double progress    = 1.0 - ((double) dogmaTicks / DOGMA_DURATION); // 0 → 1
        double currentWidth = DOGMA_START_WIDTH + (DOGMA_MAX_WIDTH - DOGMA_START_WIDTH) * progress;
        double halfWidth    = currentWidth / 2.0;

        // ── Beam geometry from mouth position ─────────────────────────────────
        Vec3 mouthPos = player.getEyePosition(1.0F).subtract(0, 0.15D, 0);
        Vec3 lookDir  = player.getLookAngle();
        Vec3 beamEnd  = mouthPos.add(lookDir.scale(DOGMA_MAX_BEAM_REACH));

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
        // Dogma beam has "dogma_brimstone" source — highest priority in the system
        BeamData dogmaBeam = new BeamData(
                player.getUUID(),
                player.getId(),
                mouthPos,
                beamEnd,
                0xFF00FF, // purple static color
                level.getGameTime(),
                level.getGameTime() + 2, // refreshed every tick
                "dogma_brimstone"
        );
        ActiveBeamManager.get().putBeam(player.getUUID(), dogmaBeam);

        // ── Send beam rendering packet to clients ─────────────────────────────
        List<Vec3> points = new ArrayList<>();
        points.add(mouthPos);
        points.add(beamEnd);
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BrimstoneBeamPacket(player.getId(), true, 1, points)); // imbue=1 purple

        // ── Static electricity particles along beam ───────────────────────────
        if (level.getGameTime() % 2 == 0) {
            for (double d = 1.0; d < DOGMA_MAX_BEAM_REACH; d += 3.0) {
                Vec3 particlePos = mouthPos.add(lookDir.scale(d));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        particlePos.x, particlePos.y, particlePos.z,
                        3, halfWidth * 0.5, halfWidth * 0.5, halfWidth * 0.5, 0.05);
            }
        }

        // ── Ambient sound every second ────────────────────────────────────────
        if (dogmaTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.5F);
        }
    }
}
