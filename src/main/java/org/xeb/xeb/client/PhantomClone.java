package org.xeb.xeb.client;

import net.minecraft.world.phys.Vec3;

public class PhantomClone {
    public int index;
    public int totalClones;
    public int targetEntityId;
    public Vec3 targetPos;
    public float orbitAngle;
    public float orbitRadius = 2.5F;
    public CloneState state = CloneState.ORBITING;
    public int attackTimer = 0;
    public int returnTimer = 0;
    public float hue;
    public boolean hasStruck = false;

    public Vec3 getOrbitPosition(float ticksLived) {
        float currentAngle = orbitAngle + (ticksLived * 0.12F); // slightly slower orbit speed for better visibility
        double x = targetPos.x + Math.cos(currentAngle) * orbitRadius;
        double z = targetPos.z + Math.sin(currentAngle) * orbitRadius;
        double y = targetPos.y;
        return new Vec3(x, y, z);
    }

    public enum CloneState {
        ORBITING, STRIKING, RETURNING, RETURNED
    }
}
