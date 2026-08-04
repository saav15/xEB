package org.xeb.xeb.extremeburst;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.network.SovereignSyncPacket;
import org.xeb.xeb.network.XEBNetwork;

import java.util.List;
import java.util.Random;

/**
 * Server handler for Sovereign Arsenal Universal Extreme Burst.
 *
 * <p>Phase 1 (Ticks 60..46 / 0.75s): 0.75s Telegraph Warning Zone + 24 Golden Spatial Portals Open.
 * Phase 2 (Ticks 45..15 / 1.50s): Launches 24 weapon projectiles (2/3 held weapon damage).
 * Phase 3 (Ticks 14..0 / 0.75s): 24 Stuck Weapons Detonation + Golden XebWaves.</p>
 */
public class SovereignArsenalHandler {

    public static final double TARGET_RADIUS = 20.0D;
    public static final int TOTAL_DURATION_TICKS = 60; // 3.0 seconds

    public static void activate(LivingEntity player, ExtremeBurstRegistry.ExtremeBurstEntry entry) {
        if (!(player.level() instanceof ServerLevel level)) return;
        CompoundTag nbt = player.getPersistentData();

        Vec3 anchor = player.position().add(0, 1.0D, 0);

        nbt.putBoolean("xebSovereignActive", true);
        nbt.putInt("xebSovereignTicks", TOTAL_DURATION_TICKS);
        nbt.putDouble("xebSovereignAnchorX", anchor.x);
        nbt.putDouble("xebSovereignAnchorY", anchor.y);
        nbt.putDouble("xebSovereignAnchorZ", anchor.z);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.5F, 1.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);

        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SovereignSyncPacket(player.getId(), true, anchor, TOTAL_DURATION_TICKS));
    }

    public static void onServerTick(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.getBoolean("xebSovereignActive")) return;

        int ticks = nbt.getInt("xebSovereignTicks");
        if (ticks <= 0) {
            finishSovereign(player);
            return;
        }

        ticks--;
        nbt.putInt("xebSovereignTicks", ticks);

        Vec3 anchor = new Vec3(
                nbt.getDouble("xebSovereignAnchorX"),
                nbt.getDouble("xebSovereignAnchorY"),
                nbt.getDouble("xebSovereignAnchorZ")
        );

        AABB targetBounds = new AABB(anchor, anchor).inflate(TARGET_RADIUS);

        // ── PHASE 1: Telegraph Warning Zone (Ticks 60..46 / 0.75s) ─────────────
        if (ticks > 45) {
            if (ticks % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, anchor.x, anchor.y + 0.2, anchor.z, 15, 2.5, 0.2, 2.5, 0.05);
            }
            return;
        }

        // ── PHASE 2: 24 Weapon Projectile Travel Damage (Ticks 45..15 / 1.5s) ───
        if (ticks >= 15 && ticks <= 45) {
            if (ticks % 1 == 0) {
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, targetBounds,
                        e -> e != player && e.isAlive() && e.position().distanceTo(anchor) <= TARGET_RADIUS);

                ItemStack mainhand = player.getMainHandItem();
                float travelDmg;

                if (!mainhand.isEmpty()) {
                    double mainhandDmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    if (mainhandDmg <= 0) mainhandDmg = 12.0D;
                    travelDmg = (float) (mainhandDmg * (2.0D / 3.0D)); // 2/3 weapon damage on travel impact
                } else {
                    net.minecraft.util.RandomSource rand = player.getRandom();
                    float[] swordDamages = new float[]{8.0F, 7.0F, 4.0F, 6.0F, 5.0F}; // Netherite, Diamond, Gold, Iron, Stone
                    travelDmg = swordDamages[rand.nextInt(swordDamages.length)]; // Full 100% damage on travel
                }

                for (LivingEntity target : targets) {
                    target.hurt(player instanceof ServerPlayer sp ? level.damageSources().playerAttack(sp) : level.damageSources().mobAttack((net.minecraft.world.entity.Mob) player), travelDmg);
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 6, 0.4, 0.4, 0.4, 0.15);
                }
            }
        }

        // ── PHASE 3: Golden Spatial Detonation Explosion (Tick 10) ───────────
        if (ticks == 10) {
            level.playSound(null, anchor.x, anchor.y, anchor.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5F, 1.4F);
            level.playSound(null, anchor.x, anchor.y, anchor.z,
                    SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 2.0F, 1.2F);

            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, anchor.x, anchor.y + 1.0, anchor.z, 3, 1.5, 1.5, 1.5, 0.0);

            // Detonation Explosion Damage: 1/3 for held weapon, 2/3 for barehanded vanilla swords
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, targetBounds,
                    e -> e != player && e.isAlive() && e.position().distanceTo(anchor) <= TARGET_RADIUS);

            ItemStack mainhand = player.getMainHandItem();
            float explosionDmg;

            if (!mainhand.isEmpty()) {
                double mainhandDmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                if (mainhandDmg <= 0) mainhandDmg = 12.0D;
                explosionDmg = (float) (mainhandDmg * (1.0D / 3.0D)); // 1/3 weapon damage on explosion
            } else {
                explosionDmg = 5.0F * (2.0F / 3.0F); // 2/3 vanilla sword damage on explosion
            }

            for (LivingEntity target : targets) {
                target.hurt(player instanceof ServerPlayer sp ? level.damageSources().playerAttack(sp) : level.damageSources().mobAttack((net.minecraft.world.entity.Mob) player), explosionDmg);
            }
        }
    }

    public static void finishSovereign(LivingEntity player) {
        CompoundTag nbt = player.getPersistentData();
        nbt.putBoolean("xebSovereignActive", false);
        nbt.remove("xebSovereignTicks");

        Vec3 anchor = new Vec3(
                nbt.getDouble("xebSovereignAnchorX"),
                nbt.getDouble("xebSovereignAnchorY"),
                nbt.getDouble("xebSovereignAnchorZ")
        );

        nbt.remove("xebSovereignAnchorX");
        nbt.remove("xebSovereignAnchorY");
        nbt.remove("xebSovereignAnchorZ");

        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SovereignSyncPacket(player.getId(), false, anchor, 0));
    }
}
