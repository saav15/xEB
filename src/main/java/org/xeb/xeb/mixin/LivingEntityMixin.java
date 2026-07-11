package org.xeb.xeb.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.xeb.xeb.item.ModItems;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Redirect(
        method = "travel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getFriction(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F",
            remap = false
        )
    )
    private float redirectGetFriction(BlockState instance, LevelReader level, BlockPos pos, Entity entity) {
        if (entity instanceof Player player) {
            boolean holdsMecha = player.getMainHandItem().is(ModItems.MECHA_OVERDRIVE.get())
                    || player.getOffhandItem().is(ModItems.MECHA_OVERDRIVE.get());
            if (holdsMecha) {
                return 0.98F; // Ice slipperiness on all blocks!
            }
        }
        return instance.getFriction(level, pos, entity);
    }
}
