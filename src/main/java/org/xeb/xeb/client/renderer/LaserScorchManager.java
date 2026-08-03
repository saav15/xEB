package org.xeb.xeb.client.renderer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class LaserScorchManager {

    private static final Random RANDOM = new Random();

    public static class ScorchMark {
        public final Vec3 pos;
        public final Direction face;
        public final float radius;
        public final long spawnTimeMs;
        public final float r, g, b;
        public final long seed;
        public final float rotationRad;

        public ScorchMark(Vec3 pos, Direction face, float radius, float r, float g, float b, long spawnTimeMs, long seed, float rotationRad) {
            this.pos = pos;
            this.face = face;
            this.radius = radius;
            this.r = r;
            this.g = g;
            this.b = b;
            this.spawnTimeMs = spawnTimeMs;
            this.seed = seed;
            this.rotationRad = rotationRad;
        }
    }

    private static final List<ScorchMark> MARKS = new ArrayList<>();
    private static final int MAX_MARKS = 64;

    /**
     * Adds an organic floor/wall/ceiling scorch mark flush against a solid block surface.
     * NEVER spawns marks in open air.
     */
    public static synchronized void addScorchMarkOnBlock(Level level, Vec3 start, Vec3 end, float radius, float r, float g, float b) {
        if (level == null) return;

        Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < 0.01D) return;

        BlockHitResult hit = level.clip(new ClipContext(
                start, end.add(dir.normalize().scale(0.5D)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos bpos = hit.getBlockPos();
            BlockState state = level.getBlockState(bpos);
            if (!state.isAir() && state.blocksMotion()) {
                addScorchMark(hit.getLocation(), hit.getDirection(), radius, r, g, b);
            }
        }
    }

    public static synchronized void addScorchMark(Vec3 hitPos, Direction face, float radius, float r, float g, float b) {
        long now = System.currentTimeMillis();
        pruneExpired(now);

        // Avoid duplicate overlapping marks in the same spot
        for (ScorchMark existing : MARKS) {
            if (existing.face == face && existing.pos.distanceToSqr(hitPos) < 0.25D) {
                return;
            }
        }

        if (MARKS.size() >= MAX_MARKS) {
            MARKS.remove(0); // Fixed memory budget
        }

        long seed = RANDOM.nextLong();
        float rotationRad = (float) (RANDOM.nextDouble() * Math.PI * 2.0D);

        MARKS.add(new ScorchMark(hitPos, face, radius, r, g, b, now, seed, rotationRad));
    }

    public static synchronized List<ScorchMark> getActiveMarks() {
        long now = System.currentTimeMillis();
        pruneExpired(now);
        return new ArrayList<>(MARKS);
    }

    private static void pruneExpired(long nowMs) {
        Iterator<ScorchMark> it = MARKS.iterator();
        while (it.hasNext()) {
            ScorchMark mark = it.next();
            if (nowMs - mark.spawnTimeMs > 5000L) {
                it.remove();
            }
        }
    }
}
