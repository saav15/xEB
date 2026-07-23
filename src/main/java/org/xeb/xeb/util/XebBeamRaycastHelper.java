package org.xeb.xeb.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class XebBeamRaycastHelper {

    public record BeamRaycastResult(Vec3 start, Vec3 end, BlockHitResult hitResult, boolean hitSolidBlock) {}

    /**
     * Performs a precise stepped raycast up to maxRange blocks with block resistance checking and optional block destruction.
     */
    public static BeamRaycastResult raycastAndDestroyBlocks(Level level, Entity attacker, Vec3 start, Vec3 dir,
                                                            double maxRange, float maxBlastResistance, boolean destroyBlocks) {
        Vec3 dirN = dir.normalize();
        Vec3 maxEnd = start.add(dirN.scale(maxRange));

        // Primary ClipContext check
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker
        ));

        Vec3 actualEnd = blockHit.getType() != BlockHitResult.Type.MISS ? blockHit.getLocation() : maxEnd;
        boolean hitSolid = blockHit.getType() != BlockHitResult.Type.MISS;

        // Block destruction along ray path if enabled
        if (destroyBlocks && level instanceof ServerLevel serverLevel) {
            double rayLength = start.distanceTo(actualEnd);
            double step = 0.5D; // 0.5 block steps to prevent skipping thin blocks
            for (double d = 0.5D; d < rayLength; d += step) {
                Vec3 p = start.add(dirN.scale(d));
                BlockPos pos = BlockPos.containing(p);
                BlockState state = level.getBlockState(pos);

                if (!state.isAir()) {
                    float res = state.getBlock().getExplosionResistance();
                    if (res <= maxBlastResistance) {
                        level.destroyBlock(pos, true);
                    }
                }
            }
        }

        return new BeamRaycastResult(start, actualEnd, blockHit, hitSolid);
    }
}
