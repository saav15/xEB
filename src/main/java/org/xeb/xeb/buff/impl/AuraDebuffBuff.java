package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.util.MedallionTierHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base class for buffs that periodically apply a mob effect to all nearby
 * entities within a configurable range. Eliminates the near-identical code
 * between DepressingBuff and SlightlyDepressingBuff.
 */
public abstract class AuraDebuffBuff extends EliteBuff {

    private final double range;
    private final Supplier<MobEffect> effectSupplier;
    private final int effectDuration;
    private final int tickInterval;

    /**
     * @param id              buff id
     * @param displayName     display name
     * @param buffType        buff type
     * @param color           colour
     * @param weight          weight
     * @param range           radius in blocks for the aura
     * @param effectSupplier  supplier for the mob effect to apply
     * @param effectDuration  duration in ticks for the applied effect
     * @param tickInterval    how often (in ticks) to refresh the aura
     */
    protected AuraDebuffBuff(String id, String displayName, BuffType buffType, int color,
                             double weight, double range, Supplier<MobEffect> effectSupplier,
                             int effectDuration, int tickInterval) {
        super(id, displayName, buffType, color, weight);
        this.range = range;
        this.effectSupplier = effectSupplier;
        this.effectDuration = effectDuration;
        this.tickInterval = tickInterval;
    }

    @Override
    public void onAttach(LivingEntity entity) {}

    @Override
    public void onDetach(LivingEntity entity) {}

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        if (entity.tickCount % tickInterval != 0) return;

        int amplifier = MedallionTierHelper.getTierValue(entity, this.getId(), tier -> switch (tier) {
            case COMMON -> 0;
            case RARE -> 1;
            case LEGENDARY -> 2;
        }, 0);

        AABB aabb = entity.getBoundingBox().inflate(range);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aabb);

        for (LivingEntity target : targets) {
            if (target != entity) {
                target.addEffect(new MobEffectInstance(
                        effectSupplier.get(), effectDuration, amplifier, false, true, true));
            }
        }
    }
}
