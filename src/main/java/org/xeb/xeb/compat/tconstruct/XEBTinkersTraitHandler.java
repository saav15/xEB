package org.xeb.xeb.compat.tconstruct;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class XEBTinkersTraitHandler {

    private static int getModifierLevel(ItemStack stack, String modifierId) {
        if (stack == null || stack.isEmpty()) return 0;
        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains("tic_modifiers", 9)) { // 9 is ListTag type
            ListTag list = nbt.getList("tic_modifiers", 10); // 10 is CompoundTag type
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                if (modifierId.equals(tag.getString("name"))) {
                    return tag.getInt("level");
                }
            }
        }
        return 0;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Spiked Conditioning Trait (Damage taken)
        if (event.getEntity() instanceof Player player) {
            ItemStack held = player.getMainHandItem();
            int spikedLvl = getModifierLevel(held, "tconstruct:spiked_conditioning");
            if (spikedLvl > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, spikedLvl - 1, true, false, true));
            }
        }

        // Attacker Modifiers (Essences)
        if (event.getSource().getEntity() instanceof Player attacker) {
            ItemStack held = attacker.getMainHandItem();
            
            // Flaming
            int flamingLvl = getModifierLevel(held, "tconstruct:flaming");
            if (flamingLvl > 0) {
                event.getEntity().setSecondsOnFire(4 * flamingLvl);
            }
            
            // Sandy
            int sandyLvl = getModifierLevel(held, "tconstruct:sandy");
            if (sandyLvl > 0) {
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }

            // Depressing
            int depLvl = getModifierLevel(held, "tconstruct:depressing");
            if (depLvl > 0) {
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, depLvl - 1));
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, depLvl - 1));
            }

            // Slightly Depressing
            int sdepLvl = getModifierLevel(held, "tconstruct:slightly_depressing");
            if (sdepLvl > 0) {
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            }

            // Sticky
            int stickyLvl = getModifierLevel(held, "tconstruct:sticky");
            if (stickyLvl > 0) {
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            }
        }

        // Victim Modifiers (Essences)
        if (event.getEntity() instanceof Player victim) {
            ItemStack held = victim.getMainHandItem();

            // Spiky
            int spikyLvl = getModifierLevel(held, "tconstruct:spiky");
            if (spikyLvl > 0 && event.getSource().getEntity() instanceof LivingEntity attackerEntity) {
                if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
                    attackerEntity.hurt(victim.damageSources().thorns(victim), 2.0f * spikyLvl);
                }
            }

            // Protected
            int protLvl = getModifierLevel(held, "tconstruct:protected");
            if (protLvl > 0 && victim.getRandom().nextFloat() < (0.15f * protLvl)) {
                event.setCanceled(true);
                return;
            }

            // Shielded
            int shieldLvl = getModifierLevel(held, "tconstruct:shielded");
            if (shieldLvl > 0) {
                boolean isProjectile = event.getSource().getDirectEntity() instanceof Projectile;
                boolean isFall = event.getSource().getMsgId().equals("fall");
                if (isProjectile || isFall) {
                    event.setAmount(event.getAmount() * (1f - 0.25f * shieldLvl));
                }
            }

            // Reactive
            int reactLvl = getModifierLevel(held, "tconstruct:reactive");
            if (reactLvl > 0) {
                boolean isProjectile = event.getSource().getDirectEntity() instanceof Projectile;
                boolean isMagic = event.getSource().getMsgId().equals("magic") || event.getSource().getMsgId().equals("indirectMagic");
                if (isProjectile || isMagic) {
                    victim.level().explode(victim, victim.getX(), victim.getY(), victim.getZ(), 2.0f, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                }
            }

            // Undying (Cheat Death)
            int undyingLvl = getModifierLevel(held, "tconstruct:undying");
            if (undyingLvl > 0 && victim.getHealth() - event.getAmount() <= 0) {
                if (victim.getRandom().nextFloat() < (0.3f * undyingLvl)) {
                    event.setCanceled(true);
                    victim.setHealth(2.0f); // 1 heart
                    victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                    victim.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                    victim.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                    victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), 
                            net.minecraft.sounds.SoundEvents.TOTEM_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            Player player = event.player;
            ItemStack held = player.getMainHandItem();

            // Elite Reflexes Trait
            int reflexesLvl = getModifierLevel(held, "tconstruct:elite_reflexes");
            if (reflexesLvl > 0 && player.getHealth() < player.getMaxHealth() / 2f) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 39, reflexesLvl - 1, true, false, true));
            }

            // Speedy Essence
            int speedLvl = getModifierLevel(held, "tconstruct:speedy");
            if (speedLvl > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 39, speedLvl - 1, true, false, true));
            }

            // Healthy Essence
            int healthLvl = getModifierLevel(held, "tconstruct:healthy");
            if (healthLvl > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 39, healthLvl - 1, true, false, true));
            }

            // Tough Essence
            int toughLvl = getModifierLevel(held, "tconstruct:tough");
            if (toughLvl > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 39, toughLvl - 1, true, false, true));
            }

            // Lucky Essence
            int luckyLvl = getModifierLevel(held, "tconstruct:lucky");
            if (luckyLvl > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 39, luckyLvl - 1, true, false, true));
            }

            // Bouncy Essence
            int bouncyLvl = getModifierLevel(held, "tconstruct:bouncy");
            if (bouncyLvl > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 39, bouncyLvl - 1, true, false, true));
            }

            // Mad Essence
            int madLvl = getModifierLevel(held, "tconstruct:mad");
            if (madLvl > 0 && player.getHealth() < player.getMaxHealth() * 0.3f) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 39, madLvl - 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 39, madLvl - 1, true, false, true));
            }
        }
    }

    @SubscribeEvent
    public static void onEliteSlayerDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack held = player.getMainHandItem();
            
            // Elite Slayer Trait
            int slayerLvl = getModifierLevel(held, "tconstruct:elite_slayer");
            if (slayerLvl > 0) {
                boolean isElite = false;
                if (event.getEntity() != null) {
                    isElite = !org.xeb.xeb.medallion.MedallionManager.getMedallions(event.getEntity()).isEmpty();
                }
                if (isElite) {
                    event.setAmount(event.getAmount() * (1f + 0.25f * slayerLvl));
                } else {
                    event.setAmount(event.getAmount() * 0.9f);
                }
            }

            // Damaging Essence
            int dmgLvl = getModifierLevel(held, "tconstruct:damaging");
            if (dmgLvl > 0) {
                event.setAmount(event.getAmount() * (1f + 0.15f * dmgLvl));
            }

            // Twin Essence
            int twinLvl = getModifierLevel(held, "tconstruct:twin");
            if (twinLvl > 0 && player.getRandom().nextFloat() < (0.3f * twinLvl)) {
                event.setAmount(event.getAmount() * 2.0f);
            }
        }
    }
}
