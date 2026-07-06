package org.xeb.xeb.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xeb.xeb.network.GoldenFlowerDanceStrikePacket;
import org.xeb.xeb.network.XEBNetwork;

import java.util.ArrayList;
import java.util.List;

public class PhantomAttackManager {
    public static final List<PhantomClone> activeClones = new ArrayList<>();
    public static int globalAttackTimer = 0;
    public static int currentAttackerIndex = 0;
    public static float ticksLived = 0;

    public static void startAttack(int[] targetEntityIds, int cloneCount) {
        activeClones.clear();
        globalAttackTimer = 0;
        currentAttackerIndex = 0;
        ticksLived = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (int i = 0; i < cloneCount; i++) {
            int tId = (i < targetEntityIds.length) ? targetEntityIds[i] : targetEntityIds[0];
            Entity target = mc.level.getEntity(tId);
            if (target != null) {
                PhantomClone clone = new PhantomClone();
                clone.index = i;
                clone.totalClones = cloneCount;
                clone.targetEntityId = tId;
                clone.targetPos = target.position();
                clone.orbitAngle = (float) ((2 * Math.PI / cloneCount) * i);
                clone.hue = (float) i / cloneCount;
                activeClones.add(clone);
            }
        }

        // The first clone attacks immediately
        if (!activeClones.isEmpty()) {
            activeClones.get(0).state = PhantomClone.CloneState.STRIKING;
            activeClones.get(0).attackTimer = 0;
            currentAttackerIndex = 1;
        }
    }

    public static void tick() {
        if (activeClones.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            activeClones.clear();
            return;
        }

        ticksLived++;
        globalAttackTimer++;

        // Attack queue processing: every 15 ticks (0.75 seconds) the next clone attacks
        if (globalAttackTimer >= 15) {
            globalAttackTimer = 0;

            // Previous striking clone returns
            for (PhantomClone c : activeClones) {
                if (c.state == PhantomClone.CloneState.STRIKING) {
                    c.state = PhantomClone.CloneState.RETURNING;
                    c.returnTimer = 0;
                }
            }

            if (currentAttackerIndex < activeClones.size()) {
                PhantomClone next = activeClones.get(currentAttackerIndex);
                next.state = PhantomClone.CloneState.STRIKING;
                next.attackTimer = 0;
                currentAttackerIndex++;
            }
        }

        // Tick active clones states and check individual target validity
        boolean anyStrikingOrReturning = false;
        for (PhantomClone clone : activeClones) {
            if (clone.state == PhantomClone.CloneState.RETURNED) continue;

            Entity target = mc.level.getEntity(clone.targetEntityId);
            boolean targetDeadOrNull = (target == null || !target.isAlive());

            if (clone.state == PhantomClone.CloneState.ORBITING || clone.state == PhantomClone.CloneState.STRIKING) {
                if (targetDeadOrNull) {
                    // Target died! Force return immediately
                    clone.state = PhantomClone.CloneState.RETURNING;
                    clone.returnTimer = 0;
                } else {
                    clone.targetPos = target.position();
                }
            }

            if (clone.state == PhantomClone.CloneState.STRIKING) {
                clone.attackTimer++;
                anyStrikingOrReturning = true;
                // Send strike packet to server at exactly tick 6 (visual impact point)
                if (clone.attackTimer == 6) {
                    clone.hasStruck = true;
                    XEBNetwork.CHANNEL.sendToServer(new GoldenFlowerDanceStrikePacket(clone.targetEntityId, 10.0F));
                }
            } else if (clone.state == PhantomClone.CloneState.RETURNING) {
                clone.returnTimer++;
                if (clone.returnTimer >= 12) {
                    clone.state = PhantomClone.CloneState.RETURNED;
                    if (clone.hasStruck) {
                        XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.GoldenFlowerDanceReturnPacket());
                    }
                } else {
                    anyStrikingOrReturning = true;
                }
            } else if (clone.state == PhantomClone.CloneState.ORBITING) {
                anyStrikingOrReturning = true;
            }
        }

        // Clear when everything has returned
        if (!anyStrikingOrReturning) {
            activeClones.clear();
        }
    }
}
