package org.xeb.xeb.compat.tconstruct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class XEBTinkersTraitHandler {

    private static final UUID HEALTHY_UUID        = UUID.fromString("a18274d1-8723-4912-b912-182374981273");
    private static final UUID TOUGH_ARMOR_UUID    = UUID.fromString("c9182374-9812-4712-b912-748912374912");
    private static final UUID TOUGH_TOUGHNESS_UUID= UUID.fromString("b9182374-9812-4712-b912-748912374913");
    private static final UUID MEGA_REACH_UUID     = UUID.fromString("d8127394-1234-4812-a912-748192374912");
    private static final UUID SPEEDY_MOVE_UUID    = UUID.fromString("e5743b10-2391-4c12-9c12-749182374812");
    private static final UUID SPEEDY_ATTACK_UUID  = UUID.fromString("f5743b10-2391-4c12-9c12-749182374813");

    // Map to track Evolving kill stacks per player
    private static final Map<UUID, EvolvingTracker> EVOLVING_MAP = new HashMap<>();

    private static class EvolvingTracker {
        int stacks;
        long expireTime;
    }

    /**
     * Checks tool NBT across tic_modifiers, tic_upgrades, tic_traits, and tic_persistent
     * for xeb:<buffId>, tconstruct:<buffId>, or <buffId>.
     */
    public static int getModifierLevel(ItemStack stack, String buffId) {
        if (stack == null || stack.isEmpty()) return 0;
        CompoundTag nbt = stack.getTag();
        if (nbt == null) return 0;

        String targetXeb = "xeb:" + buffId;
        String targetTcon = "tconstruct:" + buffId;

        String[] keys = new String[]{"tic_modifiers", "tic_upgrades", "tic_traits", "tic_persistent"};
        for (String key : keys) {
            if (nbt.contains(key, 9)) { // 9 = ListTag
                ListTag list = nbt.getList(key, 10); // 10 = CompoundTag
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag tag = list.getCompound(i);
                    String name = tag.getString("name");
                    if (name.equals(targetXeb) || name.equals(targetTcon) || name.equals(buffId)) {
                        return tag.getInt("level");
                    }
                }
            }
        }
        return 0;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // --- VICTIM MODIFIERS (Player receives damage) ---
        if (event.getEntity() instanceof Player victim) {
            ItemStack held = victim.getMainHandItem();

            // 1. Hardy Essence (Flat Damage Reduction)
            int hardyLvl = getModifierLevel(held, "hardy");
            if (hardyLvl > 0) {
                float reduced = Math.max(0.5f, event.getAmount() - (1.0f * hardyLvl));
                event.setAmount(reduced);
            }

            // 2. Spiky Essence (Direct Thorns Reflection)
            int spikyLvl = getModifierLevel(held, "spiky");
            if (spikyLvl > 0 && event.getSource().getEntity() instanceof LivingEntity attacker) {
                if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
                    attacker.hurt(victim.damageSources().thorns(victim), 2.5f * spikyLvl);
                    if (victim.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CRIT, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                    }
                    victim.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.THORNS_HIT, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }

            // 3. Protected Essence (Ward Damage Nullification)
            int protLvl = getModifierLevel(held, "protected");
            if (protLvl > 0 && victim.getRandom().nextFloat() < (0.15f * protLvl)) {
                event.setCanceled(true);
                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.2f);
                if (victim.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY() + 1.0, victim.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }
                return;
            }

            // 4. Shielded Essence (Deflect Projectiles & Fall Damage)
            int shieldLvl = getModifierLevel(held, "shielded");
            if (shieldLvl > 0) {
                boolean isProjectile = event.getSource().getDirectEntity() instanceof Projectile;
                boolean isFall = event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL);
                if (isProjectile || isFall) {
                    event.setAmount(event.getAmount() * (1.0f - 0.30f * shieldLvl));
                }
            }

            // 5. Reactive Essence (Kinetic Counter Blast)
            int reactLvl = getModifierLevel(held, "reactive");
            if (reactLvl > 0) {
                boolean isProjectile = event.getSource().getDirectEntity() instanceof Projectile;
                boolean isMagic = event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                               || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
                if (isProjectile || isMagic) {
                    float radius = 2.0f * reactLvl;
                    for (LivingEntity nearby : victim.level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(radius))) {
                        if (nearby != victim) {
                            nearby.hurt(victim.damageSources().playerAttack(victim), 3.0f * reactLvl);
                            double dx = nearby.getX() - victim.getX();
                            double dz = nearby.getZ() - victim.getZ();
                            double dist = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                            nearby.knockback(1.5 * reactLvl, -dx / dist, -dz / dist);
                        }
                    }
                    victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.4f);
                    if (victim.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 1.0, victim.getZ(), 1, 0, 0, 0, 0);
                    }
                }
            }

            // 6. Absorbent Essence (Convert Impact to Absorption Hearts)
            int absLvl = getModifierLevel(held, "absorbent");
            if (absLvl > 0) {
                float absorbed = event.getAmount() * (0.25f * absLvl);
                victim.setAbsorptionAmount(Math.min(victim.getAbsorptionAmount() + absorbed, 4.0f * absLvl));
            }

            // 7. Undying Essence (Cheat Death Salvation)
            int undyingLvl = getModifierLevel(held, "undying");
            if (undyingLvl > 0 && victim.getHealth() - event.getAmount() <= 0) {
                if (victim.getRandom().nextFloat() < (0.35f * undyingLvl)) {
                    event.setCanceled(true);
                    victim.setHealth(2.0f); // 1 heart
                    victim.setAbsorptionAmount(4.0f);
                    victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (victim.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, victim.getX(), victim.getY() + 1.0, victim.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
                    }
                    return;
                }
            }

            // 8. Bouncy Essence (Elastic Repulsion)
            int bouncyLvl = getModifierLevel(held, "bouncy");
            if (bouncyLvl > 0 && event.getSource().getEntity() instanceof LivingEntity attacker) {
                double dx = attacker.getX() - victim.getX();
                double dz = attacker.getZ() - victim.getZ();
                double dist = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                attacker.knockback(1.2 * bouncyLvl, -dx / dist, -dz / dist);
            }

            // 9. Infested Essence (Hive Swarm Counter)
            int infestedLvl = getModifierLevel(held, "infested");
            if (infestedLvl > 0 && victim.getRandom().nextFloat() < (0.35f * infestedLvl) && !victim.level().isClientSide) {
                if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                    Silverfish silverfish = EntityType.SILVERFISH.create(victim.level());
                    if (silverfish != null) {
                        silverfish.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
                        silverfish.setTarget(attacker);
                        victim.level().addFreshEntity(silverfish);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            ItemStack held = attacker.getMainHandItem();
            LivingEntity victim = event.getEntity();

            // 1. Damaging Essence (Damage Multiplier + Armor Penetration)
            int dmgLvl = getModifierLevel(held, "damaging");
            if (dmgLvl > 0) {
                float baseDamage = event.getAmount() * (1.0f + 0.20f * dmgLvl);
                float trueDamage = 1.5f * dmgLvl;
                event.setAmount(baseDamage + trueDamage);
                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
                }
            }

            // 2. Flaming Essence (Ignite & Scorch Burning Foes)
            int flamingLvl = getModifierLevel(held, "flaming");
            if (flamingLvl > 0) {
                if (victim.isOnFire()) {
                    event.setAmount(event.getAmount() + 3.0f * flamingLvl);
                }
                victim.setSecondsOnFire(4 * flamingLvl);
                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY() + 1.0, victim.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
                }
            }

            // 3. Sandy Essence (Desert Grit Blinding)
            int sandyLvl = getModifierLevel(held, "sandy");
            if (sandyLvl > 0) {
                victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.7, 1.0, 0.7));
                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, victim.getX(), victim.getY() + 1.0, victim.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }
            }

            // 4. Depressing Essence (Miasma Strength & Speed Drain)
            int depLvl = getModifierLevel(held, "depressing");
            if (depLvl > 0) {
                victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, victim.getX(), victim.getY() + 1.0, victim.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
                }
            }

            // 5. Slightly Depressing Essence
            int sdepLvl = getModifierLevel(held, "slightly_depressing");
            if (sdepLvl > 0) {
                victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.8, 1.0, 0.8));
            }

            // 6. Sticky Essence (Viscous Tar & Web Trap)
            int stickyLvl = getModifierLevel(held, "sticky");
            if (stickyLvl > 0) {
                victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.5, 1.0, 0.5));
                if (attacker.getRandom().nextFloat() < (0.30f * stickyLvl) && !victim.level().isClientSide) {
                    BlockPos pos = victim.blockPosition();
                    if (victim.level().isEmptyBlock(pos)) {
                        victim.level().setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
                    }
                }
            }

            // 7. Static Essence (Electrical Discharges)
            int staticLvl = getModifierLevel(held, "static");
            if (staticLvl > 0) {
                event.setAmount(event.getAmount() + 2.5f * staticLvl);
                int chainCount = staticLvl + 1;
                for (LivingEntity nearby : attacker.level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(5.0))) {
                    if (nearby != attacker && nearby != victim && chainCount > 0) {
                        nearby.hurt(attacker.damageSources().lightningBolt(), 2.0f * staticLvl);
                        chainCount--;
                    }
                }
                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + 1.0, victim.getZ(), 12, 0.4, 0.4, 0.4, 0.1);
                }
            }

            // 8. Resonant Essence (Sonic Shockwave on Hit)
            int resonantLvl = getModifierLevel(held, "resonant");
            if (resonantLvl > 0) {
                for (LivingEntity nearby : attacker.level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(4.0))) {
                    if (nearby != attacker && nearby != victim) {
                        nearby.hurt(attacker.damageSources().playerAttack(attacker), 3.0f * resonantLvl);
                    }
                }
                attacker.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.5f);
                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, victim.getX(), victim.getY() + 1.0, victim.getZ(), 1, 0, 0, 0, 0);
                }
            }

            // 9. Plow Essence (Shield Shatter & Armor Breach)
            int plowLvl = getModifierLevel(held, "plow");
            if (plowLvl > 0) {
                if (victim.isBlocking()) {
                    if (victim instanceof Player victimPlayer) {
                        victimPlayer.disableShield(true);
                    }
                    event.setAmount(event.getAmount() * (1.0f + 0.40f * plowLvl));
                    attacker.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 0.9f);
                } else if (victim.getArmorValue() >= 10) {
                    event.setAmount(event.getAmount() * (1.0f + 0.35f * plowLvl));
                }
            }

            // 10. Creepy Essence (Creep Blast Counter)
            int creepyLvl = getModifierLevel(held, "creepy");
            if (creepyLvl > 0 && attacker.getRandom().nextFloat() < (0.25f * creepyLvl)) {
                for (LivingEntity nearby : attacker.level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(3.0))) {
                    if (nearby != attacker) {
                        nearby.hurt(attacker.damageSources().playerAttack(attacker), 3.0f * creepyLvl);
                    }
                }
                attacker.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.CREEPER_PRIMED, SoundSource.PLAYERS, 0.8f, 1.5f);
            }

            // 11. Mirror Essence (Target Strength Mimic)
            int mirrorLvl = getModifierLevel(held, "mirror");
            if (mirrorLvl > 0 && attacker.getRandom().nextFloat() < (0.20f * mirrorLvl)) {
                AttributeInstance targetAtk = victim.getAttribute(Attributes.ATTACK_DAMAGE);
                if (targetAtk != null) {
                    double bonus = targetAtk.getValue() * (0.50D * mirrorLvl);
                    event.setAmount(event.getAmount() + (float) bonus);
                }
            }

            // 12. Twin Essence (Phantom Echo Strike)
            int twinLvl = getModifierLevel(held, "twin");
            if (twinLvl > 0 && attacker.getRandom().nextFloat() < (0.35f * twinLvl)) {
                event.setAmount(event.getAmount() * 2.0f);
                attacker.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.4f);
                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + 1.0, victim.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                }
            }

            // 13. Mad Essence (Low-HP Fury Damage Multiplier)
            int madLvl = getModifierLevel(held, "mad");
            if (madLvl > 0 && attacker.getHealth() < attacker.getMaxHealth() * 0.35f) {
                event.setAmount(event.getAmount() * (1.0f + 0.35f * madLvl));
                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, attacker.getX(), attacker.getY() + 1.8, attacker.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                }
            }

            // 14. Evolving Essence Stacks Damage Boost
            EvolvingTracker tracker = EVOLVING_MAP.get(attacker.getUUID());
            if (tracker != null && System.currentTimeMillis() < tracker.expireTime) {
                float mult = 1.0f + (0.08f * tracker.stacks);
                event.setAmount(event.getAmount() * mult);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            Player player = event.player;
            ItemStack held = player.getMainHandItem();

            // 1. Healthy Essence (+4.0 HP / +2 hearts per level)
            int healthLvl = getModifierLevel(held, "healthy");
            updateAttribute(player, Attributes.MAX_HEALTH, HEALTHY_UUID, "Healthy Essence", healthLvl * 4.0D, AttributeModifier.Operation.ADDITION);

            // 2. Tough Essence (+3.0 Armor Toughness, +1.0 Armor per level)
            int toughLvl = getModifierLevel(held, "tough");
            updateAttribute(player, Attributes.ARMOR, TOUGH_ARMOR_UUID, "Tough Essence Armor", toughLvl * 1.0D, AttributeModifier.Operation.ADDITION);
            updateAttribute(player, Attributes.ARMOR_TOUGHNESS, TOUGH_TOUGHNESS_UUID, "Tough Essence Toughness", toughLvl * 3.0D, AttributeModifier.Operation.ADDITION);

            // 3. Mega Essence (+1.0 Reach per level)
            int megaLvl = getModifierLevel(held, "mega");
            if (ForgeMod.ENTITY_REACH.get() != null) {
                updateAttribute(player, ForgeMod.ENTITY_REACH.get(), MEGA_REACH_UUID, "Mega Essence Reach", megaLvl * 1.0D, AttributeModifier.Operation.ADDITION);
            }

            // 4. Speedy Essence (+15% Movement Speed, +20% Attack Speed per level)
            int speedLvl = getModifierLevel(held, "speedy");
            updateAttribute(player, Attributes.MOVEMENT_SPEED, SPEEDY_MOVE_UUID, "Speedy Essence Speed", speedLvl * 0.15D, AttributeModifier.Operation.MULTIPLY_BASE);
            updateAttribute(player, Attributes.ATTACK_SPEED, SPEEDY_ATTACK_UUID, "Speedy Essence Attack Speed", speedLvl * 0.20D, AttributeModifier.Operation.MULTIPLY_BASE);
        }
    }

    private static void updateAttribute(Player player, Attribute attribute, UUID uuid, String name, double value, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(uuid);
        if (value <= 0) {
            if (existing != null) {
                inst.removeModifier(uuid);
            }
        } else {
            if (existing == null || existing.getAmount() != value) {
                if (existing != null) inst.removeModifier(uuid);
                inst.addTransientModifier(new AttributeModifier(uuid, name, value, op));
            }
        }
    }

    @SubscribeEvent
    public static void onLooting(LootingLevelEvent event) {
        if (event.getDamageSource() != null && event.getDamageSource().getEntity() instanceof Player player) {
            int luckyLvl = getModifierLevel(player.getMainHandItem(), "lucky");
            if (luckyLvl > 0) {
                event.setLootingLevel(event.getLootingLevel() + luckyLvl);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player killer) {
            int evolvingLvl = getModifierLevel(killer.getMainHandItem(), "evolving");
            if (evolvingLvl > 0) {
                EvolvingTracker tracker = EVOLVING_MAP.computeIfAbsent(killer.getUUID(), k -> new EvolvingTracker());
                tracker.stacks = Math.min(5, tracker.stacks + 1);
                tracker.expireTime = System.currentTimeMillis() + 15000L; // 15 seconds
            }
        }
    }
}


