package org.xeb.xeb.client;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JaronaAttack {
    public static final int ANIM_DURATION = 15;
    public static final Map<Integer, ActiveKick> activeKicks = new ConcurrentHashMap<>();

    public static void startKick(Player player, int comboStep) {
        activeKicks.put(player.getId(), new ActiveKick(comboStep));
    }

    public static void tick() {
        activeKicks.forEach((id, kick) -> {
            kick.animTimer++;
            if (kick.animTimer >= ANIM_DURATION) {
                activeKicks.remove(id);
            }
        });
    }

    public static ActiveKick getKick(Player player) {
        return activeKicks.get(player.getId());
    }

    public static class ActiveKick {
        public final int comboStep;
        public int animTimer;

        public ActiveKick(int comboStep) {
            this.comboStep = comboStep;
            this.animTimer = 0;
        }

        public float getProgress() {
            return (float) animTimer / ANIM_DURATION;
        }
    }
}
