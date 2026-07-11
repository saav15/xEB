package org.xeb.xeb.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.xeb.xeb.compat.ModCompatManager;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;

import java.util.List;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DemonCoreEntity extends ItemEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int ticksAlive = 0;
    private boolean playedLandedSound = false;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return event.setAndContinue(RawAnimation.begin().thenLoop("open"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public DemonCoreEntity(EntityType<? extends DemonCoreEntity> type, Level level) {
        super(type, level);
        this.setPickUpDelay(32767); // Cannot be picked up
    }

    public DemonCoreEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(ModEntities.DEMON_CORE.get(), level);
        this.setPos(x, y, z);
        this.setItem(stack);
        this.setPickUpDelay(32767); // Cannot be picked up
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Play landing sound when it hits the ground
            if (this.onGround() && !playedLandedSound) {
                playedLandedSound = true;
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.2F);
            }

            ticksAlive++;
            if (ticksAlive >= 28) { // 1.4 seconds (28 ticks)
                activate();
            }
        }
    }

    private void activate() {
        // Wither spawn sound ONLY in this local area (not global)
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

        boolean isRed = this.getItem().getOrCreateTag().getBoolean("RedCore");
        double inflateRange = isRed ? 10.0D : 1.5D;

        // Apply doomed to all entities in range.
        AABB area = this.getBoundingBox().inflate(inflateRange);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            // Do not apply to creative players
            if (target instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().instabuild) continue;

            // Do not apply to pets
            if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) continue;
            if (target instanceof TamableAnimal tamable && tamable.isTame()) continue;

            // Do not apply to bosses
            if (ModCompatManager.isBoss(target)) continue;

            // Do not apply to entities with golden medallions
            boolean hasGoldMedallion = false;
            for (MedallionData m : MedallionManager.getMedallions(target)) {
                if (m.getTier() == MedallionType.LEGENDARY) {
                    hasGoldMedallion = true;
                    break;
                }
            }
            if (hasGoldMedallion) continue;

            if (isRed) {
                // Apply Doomed for 200 ticks (10 seconds)
                target.addEffect(new MobEffectInstance(ModEffects.DOOMED.get(), 200, 0));
                // Instantly kill
                target.hurt(target.damageSources().magic(), Float.MAX_VALUE);
            } else {
                // Apply Doomed for 200 ticks (10 seconds)
                target.addEffect(new MobEffectInstance(ModEffects.DOOMED.get(), 200, 0));
            }
        }

        this.discard();
    }
}
