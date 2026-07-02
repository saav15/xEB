package org.xeb.xeb.util;

import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

/**
 * Centralizes the repeated pattern of creating a BuffParticlePacket and sending
 * it to all players tracking an entity (plus the entity itself).
 */
public final class BuffParticleHelper {

    private BuffParticleHelper() {}

    /**
     * Sends a particle effect to all players tracking the given entity.
     *
     * @param entity the entity at whose position the particles spawn
     * @param type   particle type identifier (e.g. "flame", "crit", "dodge")
     * @param count  number of particles
     */
    public static void sendParticles(LivingEntity entity, String type, int count) {
        sendParticles(entity.getX(), entity.getY(), entity.getZ(), entity, type, count);
    }

    /**
     * Sends a particle effect at an arbitrary position, tracked relative to an entity.
     *
     * @param x      x-coordinate for the particles
     * @param y      y-coordinate for the particles
     * @param z      z-coordinate for the particles
     * @param entity the entity used for tracking (determines which clients receive the packet)
     * @param type   particle type identifier
     * @param count  number of particles
     */
    public static void sendParticles(double x, double y, double z, LivingEntity entity, String type, int count) {
        if (entity.level().isClientSide()) return;
        BuffParticlePacket packet = new BuffParticlePacket(x, y, z, type, count);
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }
}
