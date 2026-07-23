package org.xeb.xeb.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MadnessEffect extends MobEffect {
    public MadnessEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Mob mob)) return;

        // Block Madness ticks if entity is blacklisted
        if (org.xeb.xeb.boss.UniversalBossDetector.isBlacklisted(mob)) {
            return;
        }

        // Ensure skeletons / wither skeletons have their weapons if they spawned without them
        if (mob instanceof net.minecraft.world.entity.monster.WitherSkeleton ws) {
            if (!(ws instanceof org.xeb.xeb.entity.WitherfistEntity) && !(ws instanceof org.xeb.xeb.entity.TankWitherfistEntity) && ws.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).isEmpty()) {
                ws.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_SWORD));
            }
        } else if (mob instanceof net.minecraft.world.entity.monster.Skeleton sk) {
            if (sk.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).isEmpty()) {
                sk.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
            }
        }

        // Tick recovery trackers, target buffer, and candidate expander
        try {
            org.xeb.xeb.boss.TargetRejectionBuffer.tick(mob);
            org.xeb.xeb.boss.BossTargetCandidateExpander.tick(mob);
            org.xeb.xeb.boss.FrozenBossRecoverySystem.tick(mob);
        } catch (Throwable ignored) {}

        double range = 16.0D + amplifier * 4.0D;
        AABB searchBox = mob.getBoundingBox().inflate(range);
        List<LivingEntity> potentialTargets = mob.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> {
                    if (target == mob || !target.isAlive() || !mob.hasLineOfSight(target)) return false;
                    if (target instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) return false;
                    if (org.xeb.xeb.boss.UniversalBossDetector.isBlacklisted(target)) return false;
                    if (org.xeb.xeb.boss.TargetRejectionBuffer.isRejected(mob, target.getId())) return false;

                    return true;
                });

        LivingEntity currentTarget = mob.getTarget();
        boolean shouldSwitch = currentTarget == null || !currentTarget.isAlive()
                || mob.getRandom().nextFloat() < 0.15F;

        if (shouldSwitch && !potentialTargets.isEmpty()) {
            LivingEntity newTarget = potentialTargets.get(mob.getRandom().nextInt(potentialTargets.size()));
            // Store target ID in persistent NBT for event interception
            mob.getPersistentData().putInt("xebMadnessTargetId", newTarget.getId());
            // setTarget alone is sometimes ignored by custom boss AI (e.g. Ender Cataclysm bosses).
            // Also set last-hurt references to trigger their internal aggro systems.
            mob.setTarget(newTarget);
            mob.setLastHurtByMob(newTarget);
            mob.setLastHurtMob(newTarget);
        }

        // Frantic movement — random yaw jitter and lateral impulses
        applyFranticMovement(mob, amplifier);
    }

    private void applyFranticMovement(Mob mob, int amplifier) {
        float rand = mob.getRandom().nextFloat();

        // 45% chance: sudden yaw jerk
        if (rand < 0.45F) {
            float jitter = (mob.getRandom().nextFloat() - 0.5F) * (30.0F + amplifier * 10.0F);
            mob.setYRot(mob.getYRot() + jitter);
            mob.yRotO = mob.getYRot();
            mob.yHeadRot = mob.getYRot();
        }

        // 20% chance: lateral impulse (strafe)
        if (rand < 0.20F && mob.onGround()) {
            float strafeX = (mob.getRandom().nextFloat() - 0.5F) * 0.3F;
            float strafeZ = (mob.getRandom().nextFloat() - 0.5F) * 0.3F;
            Vec3 current = mob.getDeltaMovement();
            mob.setDeltaMovement(current.x + strafeX, current.y, current.z + strafeZ);
            mob.hurtMarked = true;
        }

        // 10% chance: random lunge
        if (rand < 0.10F && mob.onGround()) {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                Vec3 dir = target.position().subtract(mob.position()).normalize();
                double sign = mob.getRandom().nextBoolean() ? 1.0 : -1.0;
                mob.setDeltaMovement(
                        mob.getDeltaMovement().x + dir.x * sign * 0.4,
                        0.42,
                        mob.getDeltaMovement().z + dir.z * sign * 0.4
                );
                mob.hurtMarked = true;
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }
}
