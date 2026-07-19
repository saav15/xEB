package org.xeb.xeb.buff.impl.defense;
import java.util.Set;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class ProtectedBuff extends EliteBuff {
    private static final String HOLY_SHIELD_KEY = "xebHolyShield";
    private static final UUID HEALTH_PENALTY_UUID = UUID.fromString("fb41b716-e41c-4b68-b80c-7833de08eeee");

    public ProtectedBuff() {
        super("protected", "Protected", BuffType.UNIVERSAL, 0xFFD700, 5.0D, false);
    }

    
    @Override
    public Set<String> getConflicts() {
        return Set.of("shielded","undying");
    }
    @Override
    public void onAttach(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        tag.putInt(HOLY_SHIELD_KEY, 1);
        entity.addEffect(new MobEffectInstance(ModEffects.HOLY_SHIELD.get(), -1, 0, false, false, true));

        // Max health penalty of -20%
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            if (maxHealth.getModifier(HEALTH_PENALTY_UUID) == null) {
                AttributeModifier modifier = new AttributeModifier(HEALTH_PENALTY_UUID, "Protected Health Penalty", -0.20D, AttributeModifier.Operation.MULTIPLY_BASE);
                maxHealth.addTransientModifier(modifier);
            }
            // Clamp current health
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }
    }

    @Override
    public void onDetach(LivingEntity entity) {
        entity.getPersistentData().remove(HOLY_SHIELD_KEY);
        entity.getPersistentData().remove("xebHolyShieldRegenTimer");
        entity.removeEffect(ModEffects.HOLY_SHIELD.get());
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(HEALTH_PENALTY_UUID);
        }
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        CompoundTag tag = entity.getPersistentData();
        if (tag.contains("xebHolyShieldRegenTimer")) {
            int timer = tag.getInt("xebHolyShieldRegenTimer");
            if (timer > 0) {
                tag.putInt("xebHolyShieldRegenTimer", timer - 1);
            } else {
                tag.remove("xebHolyShieldRegenTimer");
                tag.putInt(HOLY_SHIELD_KEY, 1);
                entity.addEffect(new MobEffectInstance(ModEffects.HOLY_SHIELD.get(), -1, 0, false, false, true));
            }
        } else {
            // If there's no timer and shield is inactive (e.g., cleared by command or effect expired), restore it
            int holyShield = tag.contains(HOLY_SHIELD_KEY) ? tag.getInt(HOLY_SHIELD_KEY) : 0;
            if (holyShield == 0) {
                tag.putInt(HOLY_SHIELD_KEY, 1);
                entity.addEffect(new MobEffectInstance(ModEffects.HOLY_SHIELD.get(), -1, 0, false, false, true));
            }
        }
    }

    @Override
    public void onDamageTaken(LivingEntity entity, LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof net.minecraft.world.entity.player.Player player) {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty() && org.xeb.xeb.enchantment.ModEnchantments.isSpecialItem(held.getItem())) {
                if (held.getEnchantmentLevel(org.xeb.xeb.enchantment.ModEnchantments.AEGIS_SHATTER.get()) > 0) {
                    return;
                }
            }
        }

        if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                || event.getAmount() >= 1000.0F
                || entity.getPersistentData().getBoolean("xebDelayedPainTriggering")) {
            return;
        }
        CompoundTag tag = entity.getPersistentData();
        int holyShield = tag.contains(HOLY_SHIELD_KEY) ? tag.getInt(HOLY_SHIELD_KEY) : 0;
        if (holyShield > 0) {
            // Absorb the full hit!
            tag.putInt(HOLY_SHIELD_KEY, 0);

            // Determine cooldown based on medallion tier
            MedallionType tier = MedallionType.COMMON; // default fallback
            for (MedallionData m : MedallionManager.getMedallions(entity)) {
                if (m.getBuff().getId().equals(this.getId())) {
                    tier = m.getTier();
                    break;
                }
            }

            int cooldownTicks = switch (tier) {
                case COMMON -> 600;      // Bronze: 30 seconds
                case RARE -> 400;        // Silver: 20 seconds
                case LEGENDARY -> 200;   // Gold: 10 seconds
            };

            tag.putInt("xebHolyShieldRegenTimer", cooldownTicks);
            entity.removeEffect(ModEffects.HOLY_SHIELD.get());
            
            // Glass breaking sound
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.0F, 1.0F);

            event.setAmount(0.0F);
            event.setCanceled(true);
            
            // Spawn totem style particles
            if (!entity.level().isClientSide()) {
                BuffParticlePacket packet = new BuffParticlePacket(entity.getX(), entity.getY(), entity.getZ(), "revival", 15);
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
            }
        }
    }
}
