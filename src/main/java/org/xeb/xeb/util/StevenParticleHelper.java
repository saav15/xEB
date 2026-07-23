package org.xeb.xeb.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class StevenParticleHelper {

    public static void spawnFistChargeParticles(ServerLevel level, Vec3 fistPos) {
        // Partículas optimizadas en vórtice hacia el puño
        for (int i = 0; i < 4; i++) {
            double rx = (level.random.nextDouble() - 0.5D) * 0.8D;
            double ry = (level.random.nextDouble() - 0.5D) * 0.8D;
            double rz = (level.random.nextDouble() - 0.5D) * 0.8D;

            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    fistPos.x + rx, fistPos.y + ry, fistPos.z + rz,
                    1, -rx * 0.5, -ry * 0.5, -rz * 0.5, 0.05);

            level.sendParticles(ParticleTypes.WITCH,
                    fistPos.x + rx * 0.5, fistPos.y + ry * 0.5, fistPos.z + rz * 0.5,
                    1, 0, 0, 0, 0.01);
        }
    }

    public static void spawnDashTrail(ServerLevel level, Vec3 pos, Vec3 motion) {
        // Estela vacía cinética
        level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 1.0D, pos.z, 3, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y + 1.0D, pos.z, 2, 0.2, 0.2, 0.2, 0.01);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 1.0D, pos.z, 1, 0.1, 0.1, 0.1, 0.01);
    }

    public static void spawnMetaTapParticles(ServerLevel level, Vec3 headPos) {
        // Destello meta-lógico al cambiar medallones
        Vec3 top = headPos.add(0, 1.2D, 0);
        level.sendParticles(ParticleTypes.FLASH, top.x, top.y, top.z, 1, 0, 0, 0, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, top.x, top.y, top.z, 10, 0.3, 0.3, 0.3, 0.08);
        level.sendParticles(ParticleTypes.WITCH, top.x, top.y, top.z, 8, 0.2, 0.2, 0.2, 0.05);
    }

    public static void spawnPortalLoop(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1.2D, pos.z, 3, 0.4, 0.6, 0.4, 0.02);
        level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 1.2D, pos.z, 2, 0.3, 0.4, 0.3, 0.01);
    }

    public static void spawnLaserImpactParticles(ServerLevel level, Vec3 impactPos) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impactPos.x, impactPos.y, impactPos.z, 3, 0.1D, 0.1D, 0.1D, 0.04D);
        level.sendParticles(ParticleTypes.SMOKE, impactPos.x, impactPos.y, impactPos.z, 2, 0.08D, 0.08D, 0.08D, 0.01D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, impactPos.x, impactPos.y, impactPos.z, 2, 0.12D, 0.12D, 0.12D, 0.02D);
    }
}
