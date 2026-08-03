package org.xeb.xeb.compat.tconstruct;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.entity.EliteFlyEntity;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.render.XebWaves;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class XEBTinkersTraitHandler {

    private static final UUID HEALTHY_HP_UUID     = UUID.fromString("a18274d1-8723-4912-b912-182374981273");
    private static final UUID TOUGH_TOUGH_UUID   = UUID.fromString("b9182374-9812-4712-b912-748912374913");
    private static final UUID SHIELDED_ARMOR_UUID = UUID.fromString("c9182374-9812-4712-b912-748912374912");
    private static final UUID HARDY_KB_UUID       = UUID.fromString("d8127394-1234-4812-a912-748192374912");
    private static final UUID MEGA_KB_UUID        = UUID.fromString("e5743b10-2391-4c12-9c12-749182374812");
    private static final UUID MEGA_REACH_UUID     = UUID.fromString("e5743b10-2391-4c12-9c12-749182374813");
    private static final UUID SPEEDY_ATK_UUID     = UUID.fromString("f5743b10-2391-4c12-9c12-749182374814");

    // Cooldown trackers (Player UUID -> Game time in ticks)
    private static final Map<UUID, Long> RESONANT_CD = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TWIN_CD     = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BOUNCY_CD   = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> MAD_CD      = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LUCKY_CD    = new ConcurrentHashMap<>();

    // Twin wave pending tasks
    private static final List<PendingTwinWave> PENDING_TWINS = new ArrayList<>();

    private static class PendingTwinWave {
        final ServerLevel level;
        final UUID attackerUUID;
        final double x, y, z;
        final float damage;
        final long triggerTick;

        PendingTwinWave(ServerLevel level, UUID attackerUUID, double x, double y, double z, float damage, long triggerTick) {
            this.level = level;
            this.attackerUUID = attackerUUID;
            this.x = x;
            this.y = y;
            this.z = z;
            this.damage = damage;
            this.triggerTick = triggerTick;
        }
    }

    public static int getModifierLevel(ItemStack stack, String buffId) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return 0;
        CompoundTag nbt = stack.getTag();
        if (nbt == null) return 0;

        String targetXeb = "xeb:" + buffId;
        String targetTcon = "tconstruct:" + buffId;

        // 1. Native Tinkers 3 ToolStack API check via TinkersApiHelper if Tinkers is loaded
        if (net.minecraftforge.fml.ModList.get().isLoaded("tconstruct")) {
            int lvl = TinkersApiHelper.getModifierLevel(stack, buffId);
            if (lvl > 0) return lvl;
        }

        // 2. Comprehensive NBT fallback (ListTag type 9, CompoundTag type 10, etc.)
        String[] keys = new String[]{"tic_modifiers", "tic_upgrades", "tic_traits", "tic_persistent", "xeb_modifiers"};
        for (String key : keys) {
            if (nbt.contains(key)) {
                byte type = nbt.getTagType(key);
                if (type == 9) { // ListTag
                    ListTag list = nbt.getList(key, 10);
                    for (int i = 0; i < list.size(); i++) {
                        CompoundTag tag = list.getCompound(i);
                        String name = tag.contains("name") ? tag.getString("name") : tag.getString("id");
                        if (name.equals(targetXeb) || name.equals(targetTcon) || name.equals(buffId)) {
                            int lvl = tag.contains("level") ? tag.getInt("level") : 1;
                            if (lvl > 0) return lvl;
                        }
                    }
                } else if (type == 10) { // CompoundTag (Tinkers 3 ModifierNBT)
                    CompoundTag comp = nbt.getCompound(key);
                    for (String keyName : comp.getAllKeys()) {
                        if (keyName.equals(targetXeb) || keyName.equals(targetTcon) || keyName.equals(buffId)) {
                            int lvl = comp.getInt(keyName);
                            if (lvl > 0) return lvl;
                        }
                        if (comp.get(keyName) instanceof CompoundTag innerTag) {
                            String name = innerTag.contains("name") ? innerTag.getString("name") : innerTag.getString("id");
                            if (name.equals(targetXeb) || name.equals(targetTcon) || name.equals(buffId)) {
                                int lvl = innerTag.contains("level") ? innerTag.getInt("level") : 1;
                                if (lvl > 0) return lvl;
                            }
                        }
                    }
                }
            }
        }

        return 0;
    }

    /**
     * Checks both Mainhand and Offhand items for the player, including Evolving bonuses.
     */
    public static int getModifierLevel(Player player, String buffId) {
        if (player == null) return 0;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        int mainLvl = getModifierLevel(main, buffId);
        int offLvl = getModifierLevel(off, buffId);
        int baseLvl = Math.max(mainLvl, offLvl);

        // Add Evolving Essence bonus traits based on durability lost
        int evolvingBonus = getEvolvingBonus(player, buffId);
        return baseLvl + evolvingBonus;
    }

    private static int getEvolvingBonus(Player player, String buffId) {
        ItemStack[] stacks = new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()};
        int bonus = 0;
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty() && stack.isDamageableItem() && getModifierLevel(stack, "evolving") > 0) {
                float damageRatio = (float) stack.getDamageValue() / (float) stack.getMaxDamage();
                // Every 20% durability lost unlocks extra essence bonuses
                if (damageRatio >= 0.20f && (buffId.equals("flaming") || buffId.equals("sandy"))) bonus += 1;
                if (damageRatio >= 0.40f && (buffId.equals("damaging") || buffId.equals("healthy"))) bonus += 1;
                if (damageRatio >= 0.60f && (buffId.equals("shielded") || buffId.equals("tough"))) bonus += 1;
                if (damageRatio >= 0.80f && (buffId.equals("speedy") || buffId.equals("mad"))) bonus += 1;
            }
        }
        return bonus;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // VICTIM MODIFIERS & UNDYING (Player receives damage)
    // ─────────────────────────────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        long gameTime = victim.level().getGameTime();

        // 1. Undying Essence (Totem effect on fatal damage costing 20% max durability)
        int undyingLvl = getModifierLevel(victim, "undying");
        if (undyingLvl > 0 && (victim.getHealth() - event.getAmount() <= 0.0f)) {
            ItemStack toolToDamage = null;
            if (getModifierLevel(victim.getMainHandItem(), "undying") > 0) toolToDamage = victim.getMainHandItem();
            else if (getModifierLevel(victim.getOffhandItem(), "undying") > 0) toolToDamage = victim.getOffhandItem();

            if (toolToDamage != null && toolToDamage.isDamageableItem()) {
                int cost = (int) (toolToDamage.getMaxDamage() * 0.20f);
                if (toolToDamage.getMaxDamage() - toolToDamage.getDamageValue() > cost) {
                    toolToDamage.setDamageValue(toolToDamage.getDamageValue() + cost);
                    event.setCanceled(true);
                    victim.setHealth(4.0f); // 2 Hearts
                    victim.removeAllEffects();
                    victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 150, 1));
                    victim.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                    victim.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                    victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (victim.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, victim.getX(), victim.getY() + 1.0, victim.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
                    }
                    return;
                }
            }
        }

        // 2. Spiky Essence (Reflect 2.5 direct thorn damage)
        int spikyLvl = getModifierLevel(victim, "spiky");
        if (spikyLvl > 0 && event.getSource().getEntity() instanceof LivingEntity attacker) {
            attacker.hurt(victim.damageSources().thorns(victim), 2.5f * spikyLvl);
        }

        // 3. Spiked Conditioning (Bronze trait: reflect thorns + Slowness & Weakness)
        int spikedCondLvl = getModifierLevel(victim, "spiked_conditioning");
        if (spikedCondLvl > 0 && event.getSource().getEntity() instanceof LivingEntity attacker) {
            attacker.hurt(victim.damageSources().thorns(victim), 3.0f * spikedCondLvl);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }

        // 4. Elite Reflexes (Silver trait: Speed II for 3s on hit)
        int reflexesLvl = getModifierLevel(victim, "elite_reflexes");
        if (reflexesLvl > 0) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        }

        // 5. Bouncy Essence (+50% KB received, Speed I for 4s on hit/KB)
        int bouncyLvl = getModifierLevel(victim, "bouncy");
        if (bouncyLvl > 0) {
            long lastBouncy = BOUNCY_CD.getOrDefault(victim.getUUID(), 0L);
            if (gameTime - lastBouncy >= 600) { // 30s CD
                BOUNCY_CD.put(victim.getUUID(), gameTime);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ATTACK MODIFIERS (Player deals damage)
    // ─────────────────────────────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        LivingEntity victim = event.getEntity();
        long gameTime = attacker.level().getGameTime();

        // 1. Flaming Essence (10%/lvl chance for Charred Burn for 1.5s, max 4)
        int flamingLvl = getModifierLevel(attacker, "flaming");
        if (flamingLvl > 0) {
            float chance = Math.min(0.40f, 0.10f * flamingLvl);
            if (attacker.getRandom().nextFloat() < chance) {
                victim.addEffect(new MobEffectInstance(ModEffects.CHARRED_BURN.get(), 30, 0));
            }
        }

        // 2. Sandy Essence (10%/lvl chance for Slowness II for 1.5s, max 3)
        int sandyLvl = getModifierLevel(attacker, "sandy");
        if (sandyLvl > 0) {
            float chance = Math.min(0.30f, 0.10f * sandyLvl);
            if (attacker.getRandom().nextFloat() < chance) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
        }

        // 3. Creepy Essence (Hit inflicts Poison I 3s)
        int creepyLvl = getModifierLevel(attacker, "creepy");
        if (creepyLvl > 0) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        }

        // 4. Resonant Essence (Check 5x5 medallions -> 5s ALL_STATS_UP II, 30s CD)
        int resonantLvl = getModifierLevel(attacker, "resonant");
        if (resonantLvl > 0) {
            long lastRes = RESONANT_CD.getOrDefault(attacker.getUUID(), 0L);
            if (gameTime - lastRes >= 600) {
                AABB area = attacker.getBoundingBox().inflate(2.5D);
                List<LivingEntity> nearby = attacker.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != attacker && !MedallionManager.getMedallions(e).isEmpty());
                if (!nearby.isEmpty()) {
                    RESONANT_CD.put(attacker.getUUID(), gameTime);
                    attacker.addEffect(new MobEffectInstance(ModEffects.ALL_STATS_UP.get(), 100, 1));
                }
            }
        }

        // 5. Infested Essence (Spawns Elite Fly or Silverfish eternally Charmed by spawner and Doomed for 30s)
        int infestedLvl = getModifierLevel(attacker, "infested");
        if (infestedLvl > 0 && !attacker.level().isClientSide) {
            float chance = Math.min(0.25f, 0.10f + 0.05f * (infestedLvl - 1));
            if (attacker.getRandom().nextFloat() < chance) {
                LivingEntity spawned = null;
                if (attacker.getRandom().nextBoolean()) {
                    EliteFlyEntity fly = new EliteFlyEntity(org.xeb.xeb.entity.ModEntities.ELITE_FLY.get(), attacker.level());
                    fly.moveTo(attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), attacker.getYRot(), 0.0F);
                    spawned = fly;
                } else {
                    Silverfish silverfish = EntityType.SILVERFISH.create(attacker.level());
                    if (silverfish != null) {
                        silverfish.moveTo(attacker.getX(), attacker.getY(), attacker.getZ(), attacker.getYRot(), 0.0F);
                        spawned = silverfish;
                    }
                }

                if (spawned != null) {
                    // Eternally Charmed by the attacker
                    spawned.addEffect(new MobEffectInstance(ModEffects.CHARMED.get(), MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
                    spawned.getPersistentData().putUUID("xebCharmedOwner", attacker.getUUID());
                    spawned.getPersistentData().putString("xebCharmedOwnerName", attacker.getName().getString());

                    // Doomed for 30 seconds (600 ticks)
                    spawned.addEffect(new MobEffectInstance(ModEffects.DOOMED.get(), 600, 0, false, true, true));

                    if (spawned instanceof Mob mob) {
                        mob.setTarget(victim);
                    }

                    attacker.level().addFreshEntity(spawned);
                }
            }
        }

        // 6. Damaging Essence (5% + 3%*(lvl-1) chance for Bruise I for 3s, max lvl 10)
        int dmgLvl = getModifierLevel(attacker, "damaging");
        if (dmgLvl > 0) {
            float chance = Math.min(0.32f, 0.05f + 0.03f * (dmgLvl - 1));
            if (attacker.getRandom().nextFloat() < chance) {
                victim.addEffect(new MobEffectInstance(ModEffects.BRUISE.get(), 60, 0));
            }
        }

        // 7. Twin Essence (Miniature XebWave echo after 2s, 70% damage, 5s CD)
        int twinLvl = getModifierLevel(attacker, "twin");
        if (twinLvl > 0 && attacker.level() instanceof ServerLevel serverLevel) {
            long lastTwin = TWIN_CD.getOrDefault(attacker.getUUID(), 0L);
            if (gameTime - lastTwin >= 100) { // 5s CD
                TWIN_CD.put(attacker.getUUID(), gameTime);
                float waveDamage = event.getAmount() * 0.70f;
                PENDING_TWINS.add(new PendingTwinWave(serverLevel, attacker.getUUID(), victim.getX(), victim.getY(), victim.getZ(), waveDamage, gameTime + 40L));
            }
        }

        // 8. Absorbent Essence (Lifesteal: 30% + 5%*(lvl-1), max 4 = 45%)
        int absLvl = getModifierLevel(attacker, "absorbent");
        if (absLvl > 0) {
            float percent = Math.min(0.45f, 0.30f + 0.05f * (absLvl - 1));
            float healAmount = event.getAmount() * percent;
            attacker.heal(healAmount);
        }

        // 9. Elite Slayer (50% bonus damage vs Elites/Bosses + Wither II & Weakness II)
        int slayerLvl = getModifierLevel(attacker, "elite_slayer");
        if (slayerLvl > 0 && (!MedallionManager.getMedallions(victim).isEmpty() || MedallionManager.isBoss(victim))) {
            event.setAmount(event.getAmount() * 1.50f);
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // KILL MODIFIERS
    // ─────────────────────────────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        ItemStack held = killer.getMainHandItem();
        long gameTime = killer.level().getGameTime();

        // 1. Mad Essence (Repair 10% durability + Strength I, Regen I, Abs II 20s, 30s CD)
        int madLvl = getModifierLevel(killer, "mad");
        if (madLvl > 0) {
            if (held.isDamageableItem()) {
                int repair = Math.max(1, (int) (held.getMaxDamage() * 0.10f));
                held.setDamageValue(Math.max(0, held.getDamageValue() - repair));
            }
            long lastMad = MAD_CD.getOrDefault(killer.getUUID(), 0L);
            if (gameTime - lastMad >= 600) { // 30s CD
                MAD_CD.put(killer.getUUID(), gameTime);
                killer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
                killer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 0));
                killer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // LOOT MODIFIERS
    // ─────────────────────────────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof Player killer) {
            int stickyLvl = getModifierLevel(killer, "sticky");
            if (stickyLvl > 0) {
                // Auto-collect drops into inventory
                List<ItemEntity> drops = new ArrayList<>(event.getDrops());
                for (ItemEntity drop : drops) {
                    if (killer.getInventory().add(drop.getItem())) {
                        event.getDrops().remove(drop);
                    }
                }

                // Double drops for Slimes and Magma Cubes
                if (event.getEntity() instanceof net.minecraft.world.entity.monster.Slime) {
                    List<ItemEntity> extraDrops = new ArrayList<>();
                    for (ItemEntity drop : event.getDrops()) {
                        ItemEntity extra = new ItemEntity(killer.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
                        extraDrops.add(extra);
                    }
                    for (ItemEntity extra : extraDrops) {
                        if (killer.getInventory().add(extra.getItem())) {
                            // Added directly
                        } else {
                            event.getDrops().add(extra);
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TICK MODIFIERS (Auras, Attributes, Pasivos)
    // ─────────────────────────────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        long gameTime = player.level().getGameTime();

        // --- Process Pending Twin Waves ---
        if (!PENDING_TWINS.isEmpty()) {
            Iterator<PendingTwinWave> it = PENDING_TWINS.iterator();
            while (it.hasNext()) {
                PendingTwinWave tw = it.next();
                if (gameTime >= tw.triggerTick) {
                    Vec3 wavePos = new Vec3(tw.x, tw.y, tw.z);
                    XebWaves.spawnWave(wavePos, 2.5f, 0.3f, 255, 0, 255);
                    AABB waveArea = new AABB(tw.x - 2.5, tw.y - 1.0, tw.z - 2.5, tw.x + 2.5, tw.y + 1.0, tw.z + 2.5);
                    Player waveAttacker = tw.attackerUUID != null ? tw.level.getPlayerByUUID(tw.attackerUUID) : null;
                    for (LivingEntity target : tw.level.getEntitiesOfClass(LivingEntity.class, waveArea, e -> e.isAlive() && (tw.attackerUUID == null || !e.getUUID().equals(tw.attackerUUID)))) {
                        if (waveAttacker != null) {
                            target.hurt(tw.level.damageSources().playerAttack(waveAttacker), tw.damage);
                        } else {
                            target.hurt(tw.level.damageSources().generic(), tw.damage);
                        }
                    }
                    tw.level.playSound(null, tw.x, tw.y, tw.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
                    it.remove();
                }
            }
        }

        // 1. Depressing Essence (In hand: ALL_STATS_DOWN I; 4x4 area: ALL_STATS_DOWN II)
        int depLvl = getModifierLevel(player, "depressing");
        if (depLvl > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.ALL_STATS_DOWN.get(), 40, 0, false, false, true));
            AABB area = player.getBoundingBox().inflate(2.0D); // 4x4
            for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
                nearby.addEffect(new MobEffectInstance(ModEffects.ALL_STATS_DOWN.get(), 40, 1));
            }
        }

        // 2. Slightly Depressing Essence (In hand: ALL_STATS_DOWN I; 1.5x1.5 area: ALL_STATS_DOWN III)
        int sDepLvl = getModifierLevel(player, "slightly_depressing");
        if (sDepLvl > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.ALL_STATS_DOWN.get(), 40, 0, false, false, true));
            AABB area = player.getBoundingBox().inflate(0.75D); // 1.5x1.5
            for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
                nearby.addEffect(new MobEffectInstance(ModEffects.ALL_STATS_DOWN.get(), 40, 2));
            }
        }

        // 3. Creepy Essence (Leaves green poison trail particles on ground + Poison immunity)
        int creepyLvl = getModifierLevel(player, "creepy");
        if (creepyLvl > 0) {
            player.removeEffect(MobEffects.POISON);
            if (player.onGround() && player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.COMPOSTER, player.getX(), player.getY() + 0.1, player.getZ(), 3, 0.2, 0.0, 0.2, 0.01);
            }
        }

        // 4. Mirror Essence (Reflect I)
        int mirrorLvl = getModifierLevel(player, "mirror");
        if (mirrorLvl > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.REFLECT.get(), 40, 0, false, false, true));
        }

        // 5. Hardy Essence (Resistance I/II, +40% knockback immunity)
        int hardyLvl = getModifierLevel(player, "hardy");
        if (hardyLvl > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, Math.min(1, hardyLvl - 1), false, false, true));
        }
        updateAttribute(player, Attributes.KNOCKBACK_RESISTANCE, HARDY_KB_UUID, "Hardy Knockback Resistance", hardyLvl > 0 ? 0.40D : 0.0D, AttributeModifier.Operation.ADDITION);

        // 6. Protected Essence (Holy Shield)
        int protLvl = getModifierLevel(player, "protected");
        if (protLvl > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.HOLY_SHIELD.get(), 40, 0, false, false, true));
        }

        // 7. Shielded Essence (+2.0 + 1.5*(lvl-1) Armor)
        int shieldedLvl = getModifierLevel(player, "shielded");
        double shieldedArmor = shieldedLvl > 0 ? (2.0D + 1.5D * (shieldedLvl - 1)) : 0.0D;
        updateAttribute(player, Attributes.ARMOR, SHIELDED_ARMOR_UUID, "Shielded Armor", shieldedArmor, AttributeModifier.Operation.ADDITION);

        // 8. Reactive Essence (Kinetic Spikes pasivo)
        int reactLvl = getModifierLevel(player, "reactive");
        if (reactLvl > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.KINETIC_SPIKES.get(), 40, reactLvl - 1, false, false, true));
        }

        // 9. Healthy Essence (+8 HP / +4 hearts & Regeneration I)
        int healthyLvl = getModifierLevel(player, "healthy");
        updateAttribute(player, Attributes.MAX_HEALTH, HEALTHY_HP_UUID, "Healthy Essence HP", healthyLvl > 0 ? 8.0D : 0.0D, AttributeModifier.Operation.ADDITION);
        if (healthyLvl > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false, true));
        }

        // 10. Tough Essence (+1.0 + 0.5*(lvl-1) Armor Toughness)
        int toughLvl = getModifierLevel(player, "tough");
        double toughVal = toughLvl > 0 ? (1.0D + 0.5D * (toughLvl - 1)) : 0.0D;
        updateAttribute(player, Attributes.ARMOR_TOUGHNESS, TOUGH_TOUGH_UUID, "Tough Essence Toughness", toughVal, AttributeModifier.Operation.ADDITION);

        // 11. Mega Essence (+2 Reach, 100% KB immunity, Strength I, Slowness I)
        int megaLvl = getModifierLevel(player, "mega");
        updateAttribute(player, Attributes.KNOCKBACK_RESISTANCE, MEGA_KB_UUID, "Mega KB Immunity", megaLvl > 0 ? 1.0D : 0.0D, AttributeModifier.Operation.ADDITION);
        if (ForgeMod.ENTITY_REACH.get() != null) {
            updateAttribute(player, ForgeMod.ENTITY_REACH.get(), MEGA_REACH_UUID, "Mega Reach", megaLvl > 0 ? 2.0D : 0.0D, AttributeModifier.Operation.ADDITION);
        }
        if (megaLvl > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
        }

        // 12. Speedy Essence (+5% + 2%*(lvl-1) Atk Speed, Speed I..III, Haste I..III)
        int speedyLvl = getModifierLevel(player, "speedy");
        double speedyAtk = speedyLvl > 0 ? (0.05D + 0.02D * (speedyLvl - 1)) : 0.0D;
        updateAttribute(player, Attributes.ATTACK_SPEED, SPEEDY_ATK_UUID, "Speedy Attack Speed", speedyAtk, AttributeModifier.Operation.MULTIPLY_BASE);
        if (speedyLvl > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, speedyLvl - 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, speedyLvl - 1, false, false, true));
        }

        // 13. Mad Essence (Madness in hand)
        int madLvl = getModifierLevel(player, "mad");
        if (madLvl > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.MADNESS.get(), 40, 0, false, false, true));
        }

        // 14. Lucky Essence (Delayed Pain when HP < 30%, 3 min CD)
        int luckyLvl = getModifierLevel(player, "lucky");
        if (luckyLvl > 0 && player.getHealth() < player.getMaxHealth() * 0.30f) {
            long lastLucky = LUCKY_CD.getOrDefault(player.getUUID(), 0L);
            if (gameTime - lastLucky >= 3600) { // 3 min CD
                LUCKY_CD.put(player.getUUID(), gameTime);
                player.addEffect(new MobEffectInstance(ModEffects.DELAYED_PAIN.get(), 800, 0));
            }
        }

        // 15. Plow Essence (Walking damage 10% + 5%*(lvl-1) of weapon damage)
        int plowLvl = getModifierLevel(player, "plow");
        if (plowLvl > 0 && (player.getDeltaMovement().x != 0 || player.getDeltaMovement().z != 0)) {
            float percent = Math.min(0.30f, 0.10f + 0.05f * (plowLvl - 1));
            AttributeInstance atkInst = player.getAttribute(Attributes.ATTACK_DAMAGE);
            float baseDmg = atkInst != null ? (float) atkInst.getValue() : 4.0f;
            float contactDmg = Math.max(1.0f, baseDmg * percent);

            AABB contactBox = player.getBoundingBox().inflate(0.5D);
            for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class, contactBox, e -> e != player && e.isAlive())) {
                nearby.hurt(player.damageSources().playerAttack(player), contactDmg);
            }
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
}
