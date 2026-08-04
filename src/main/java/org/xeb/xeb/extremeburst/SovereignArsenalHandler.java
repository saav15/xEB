package org.xeb.xeb.extremeburst;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.network.SovereignSyncPacket;
import org.xeb.xeb.network.XEBNetwork;

import java.util.List;
import java.util.Random;

/**
 * Server handler for Sovereign Arsenal Universal Extreme Burst.
 *
 * <p>Phase 1 (Ticks 90..75 / 0.75s): Telegraph Warning Zone + 24 Golden Spatial Portals Open.
 * Phase 2 & 3 (Ticks 74..10 / 3.20s):
 * - Holding weapon: 2/3 weapon damage on travel impact + 1/3 weapon damage on 1.5x1.5 block floor explosion.
 * - Barehanded: 100% full vanilla sword damage on travel impact + 1/3 vanilla sword damage on 1.5x1.5 block floor explosion.
 * - Ground Hazard (1.5s): Enemies stepping on/touching stuck blades take 1/4 weapon damage (does not affect caster).</p>
 */
public class SovereignArsenalHandler {

    public static final double TARGET_RADIUS = 20.0D;
    public static final int TOTAL_DURATION_TICKS = 90; // 4.5 seconds

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

        ItemStack castItem = player.getMainHandItem().copy();

        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SovereignSyncPacket(player.getId(), true, anchor, TOTAL_DURATION_TICKS, castItem));
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

        // ── PHASE 1: Telegraph Warning Zone (Ticks 90..75 / 0.75s) ─────────────
        if (ticks > 75) {
            return;
        }

        // ── PHASE 2 & 3: Staggered 24 Weapon Travel, 1.5x1.5 Block Explosions & 1.5s Ground Hazard ──
        if (ticks >= 5 && ticks <= 75) {
            ItemStack mainhand = player.getMainHandItem();
            boolean hasWeapon = !mainhand.isEmpty();

            double weaponDmg = 12.0D;
            if (hasWeapon) {
                double attrDmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                if (attrDmg > 0) weaponDmg = attrDmg;
            }

            float[] vanillaSwordDamages = new float[]{8.0F, 7.0F, 4.0F, 6.0F, 5.0F}; // Netherite, Diamond, Gold, Iron, Stone

            net.minecraft.world.damagesource.DamageSource source = player instanceof ServerPlayer sp
                    ? level.damageSources().playerAttack(sp)
                    : (player instanceof net.minecraft.world.entity.Mob mob
                        ? level.damageSources().mobAttack(mob)
                        : level.damageSources().generic());

            for (int w = 0; w < 24; w++) {
                int travelTick = 74 - (int) (w * 40.0D / 23.0D); // Staggered travel tick 74..34
                int explosionTick = travelTick - 4; // Floor explosion 4 ticks after travel
                int stickEndTick = travelTick - 34; // 30 ticks (1.5s) ground stick hazard window

                Random rand = new Random(player.getId() * 7777L + w * 101L);
                double angle = w * (Math.PI * 2.0D / 24.0D) + (rand.nextDouble() - 0.5D) * 0.3D;
                double dist = 2.5D + (w % 4) * 3.2D + rand.nextDouble() * 1.5D;
                double wx = anchor.x + Math.cos(angle) * dist;
                double wz = anchor.z + Math.sin(angle) * dist;

                // Raycast floor Y position
                Vec3 gStart = new Vec3(wx, anchor.y + 4.0D, wz);
                Vec3 gEnd = new Vec3(wx, anchor.y - 30.0D, wz);
                BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(
                        gStart, gEnd, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, player
                ));
                double wy = (hit.getType() != HitResult.Type.MISS) ? hit.getLocation().y : anchor.y;

                // 1. TRAVEL IMPACT DAMAGE (2/3 weapon damage if holding weapon, 100% full damage if barehanded)
                if (ticks == travelTick) {
                    float travelDmg;
                    if (hasWeapon) {
                        travelDmg = (float) (weaponDmg * (2.0D / 3.0D)); // 2/3 weapon damage on travel
                    } else {
                        travelDmg = vanillaSwordDamages[w % vanillaSwordDamages.length]; // 100% full vanilla sword damage
                    }

                    AABB travelBox = new AABB(wx - 2.0D, wy, wz - 2.0D, wx + 2.0D, anchor.y + 15.0D, wz + 2.0D);
                    List<LivingEntity> travelTargets = level.getEntitiesOfClass(LivingEntity.class, travelBox,
                            e -> e != player && e.isAlive());

                    for (LivingEntity target : travelTargets) {
                        target.hurt(source, travelDmg);
                    }
                }

                // 2. FLOOR EXPLOSION DETONATION DAMAGE (1/3 damage in 1.5 x 1.5 block floor area)
                if (ticks == explosionTick) {
                    float explosionDmg;
                    if (hasWeapon) {
                        explosionDmg = (float) (weaponDmg * (1.0D / 3.0D)); // 1/3 weapon damage on floor explosion
                    } else {
                        explosionDmg = (float) (vanillaSwordDamages[w % vanillaSwordDamages.length] * (1.0D / 3.0D)); // 1/3 vanilla sword damage
                    }

                    // 1.5 x 1.5 block floor explosion area (0.75m radius around impact center)
                    AABB explosionBox = new AABB(wx - 0.75D, wy - 0.5D, wz - 0.75D, wx + 0.75D, wy + 1.5D, wz + 0.75D);
                    List<LivingEntity> explosionTargets = level.getEntitiesOfClass(LivingEntity.class, explosionBox,
                            e -> e != player && e.isAlive());

                    for (LivingEntity target : explosionTargets) {
                        target.hurt(source, explosionDmg);
                    }
                }

                // 3. 1.5s GROUND STICK HAZARD / TOUCH DAMAGE (1/4 weapon damage if enemies step on/touch stuck blade)
                if (ticks < travelTick && ticks >= stickEndTick) {
                    float touchDmg = hasWeapon ? (float) (weaponDmg * 0.25D) : (float) (vanillaSwordDamages[w % vanillaSwordDamages.length] * 0.25D);
                    AABB touchBox = new AABB(wx - 0.6D, wy - 0.2D, wz - 0.6D, wx + 0.6D, wy + 1.4D, wz + 0.6D);
                    List<LivingEntity> touchTargets = level.getEntitiesOfClass(LivingEntity.class, touchBox,
                            e -> e != player && e.isAlive());

                    for (LivingEntity target : touchTargets) {
                        // Does not affect the player/caster. Deals 1/4 weapon damage if enemy steps on blade
                        if (target.hurtTime <= 0) {
                            target.hurt(source, touchDmg);
                        }
                    }
                }
            }

            // Staggered spatial audio burst pulses every 3 ticks
            if (ticks % 3 == 0) {
                float pitch = 1.0F + ((75 - ticks) / 60.0F) * 0.6F;
                level.playSound(null, anchor.x, anchor.y, anchor.z,
                        SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.2F, pitch);
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
                new SovereignSyncPacket(player.getId(), false, anchor, 0, ItemStack.EMPTY));
    }
}
