package org.xeb.xeb.extremeburst;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.network.JudgementCutSyncPacket;
import org.xeb.xeb.network.XEBNetwork;

import java.util.List;

/**
 * Server handler for Judgement Cut End Universal Extreme Burst.
 *
 * <p>Phase 1 (Ticks 60..46 / 0.75s): Telegraph Raycast Zone + Forcefield Barrier (0.75s dodge window).
 * Phase 2 (Ticks 45..16 / 1.50s): Subspace Time Freeze + 36 Slashes Mesh + Impenetrable Dome.
 * Phase 3 (Tick 15 / 0.05s): Sheath Click Silence.
 * Phase 4 (Ticks 14..0 / 0.70s): 12 Slashes x (2.0x Mainhand Damage) Detonation + XebWaves.</p>
 */
public class JudgementCutHandler {

    public static final double DOMAIN_RADIUS = 24.0D;
    public static final int TOTAL_DURATION_TICKS = 60; // 3.0 seconds

    public static void activate(LivingEntity player, ExtremeBurstRegistry.ExtremeBurstEntry entry) {
        if (!(player.level() instanceof ServerLevel level)) return;
        CompoundTag nbt = player.getPersistentData();

        // 1. Domain Anchor: Centered directly around the casting player
        Vec3 anchor = player.position().add(0, 1.0D, 0);

        // 2. Set persistent NBT tracking
        nbt.putBoolean("xebJudgementCutActive", true);
        nbt.putInt("xebJudgementCutTicks", TOTAL_DURATION_TICKS);
        nbt.putDouble("xebJudgementCutAnchorX", anchor.x);
        nbt.putDouble("xebJudgementCutAnchorY", anchor.y);
        nbt.putDouble("xebJudgementCutAnchorZ", anchor.z);

        // 3. Audio cue & camera recoil
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.5F, 1.8F);
        level.playSound(null, anchor.x, anchor.y, anchor.z,
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.6F);

        // 4. Sync client renderers
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new JudgementCutSyncPacket(player.getId(), true, anchor, TOTAL_DURATION_TICKS));
    }

    public static void onServerTick(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.getBoolean("xebJudgementCutActive")) return;

        int ticks = nbt.getInt("xebJudgementCutTicks");
        if (ticks <= 0) {
            finishJudgementCut(player);
            return;
        }

        ticks--;
        nbt.putInt("xebJudgementCutTicks", ticks);

        Vec3 anchor = new Vec3(
                nbt.getDouble("xebJudgementCutAnchorX"),
                nbt.getDouble("xebJudgementCutAnchorY"),
                nbt.getDouble("xebJudgementCutAnchorZ")
        );

        AABB domainBounds = new AABB(anchor, anchor).inflate(DOMAIN_RADIUS);

        // ── IMPENETRABLE FORCEFIELD BARRIER: Prevent outside entities from entering ──
        List<LivingEntity> allEntities = level.getEntitiesOfClass(LivingEntity.class, domainBounds.inflate(6.0D), LivingEntity::isAlive);
        for (LivingEntity e : allEntities) {
            if (e == player) continue;
            double dist = e.position().distanceTo(anchor);
            if (dist > DOMAIN_RADIUS && dist <= DOMAIN_RADIUS + 4.0D) {
                // Push back outside entities attempting to cross dome boundary
                Vec3 pushDir = e.position().subtract(anchor).normalize();
                e.setDeltaMovement(pushDir.x * 0.8D, 0.2D, pushDir.z * 0.8D);
                e.hasImpulse = true;
            }
        }

        // ── PHASE 1: Telegraph Raycast Warning Zone (Ticks 60..46 / 0.75s) ─────────
        if (ticks > 45) {
            if (ticks % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, anchor.x, anchor.y + 0.2, anchor.z, 20, 3.0, 0.2, 3.0, 0.05);
            }
            return;
        }

        // ── PHASE 2: Subspace Time Freeze & 36 Slashes Mesh (Ticks 45..16 / 1.50s) ─
        List<LivingEntity> targetsInside = level.getEntitiesOfClass(LivingEntity.class, domainBounds,
                e -> e != player && e.isAlive() && e.position().distanceTo(anchor) <= DOMAIN_RADIUS);

        for (LivingEntity target : targetsInside) {
            // Apply Subspace Time Freeze paralysis
            target.setDeltaMovement(Vec3.ZERO);
            target.hasImpulse = true;
            target.fallDistance = 0.0F;
        }

        // ── PHASE 3: Sheath Click Silence (Tick 15 / 0.05s) ────────────────────────
        if (ticks == 15) {
            level.playSound(null, anchor.x, anchor.y, anchor.z,
                    SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 3.0F, 2.0F);
            level.playSound(null, anchor.x, anchor.y, anchor.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 1.8F);
        }

        // ── PHASE 4: 12 Slashes x 2.0x Weapon Damage Detonation (Ticks 14..2) ──────
        if (ticks >= 2 && ticks <= 13) {
            double mainhandDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (mainhandDamage <= 0) mainhandDamage = 10.0D;
            float slashDmg = (float) (mainhandDamage * 2.0D);

            for (LivingEntity target : targetsInside) {
                target.hurt(player instanceof ServerPlayer sp ? level.damageSources().playerAttack(sp) : level.damageSources().mobAttack((net.minecraft.world.entity.Mob) player), slashDmg);

                // Particles on target
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.5, 0.5, 0.5, 0.2);
            }

            // Spawn XebWaves shockwaves
            if (ticks == 10 && net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
                org.xeb.xeb.render.XebWaves.spawnWave(anchor, 24.0F, 0.50F, 0.8F, 0.40F, 0, 229, 255);
            }
        }
    }

    public static void finishJudgementCut(LivingEntity player) {
        CompoundTag nbt = player.getPersistentData();
        nbt.putBoolean("xebJudgementCutActive", false);
        nbt.remove("xebJudgementCutTicks");

        Vec3 anchor = new Vec3(
                nbt.getDouble("xebJudgementCutAnchorX"),
                nbt.getDouble("xebJudgementCutAnchorY"),
                nbt.getDouble("xebJudgementCutAnchorZ")
        );

        nbt.remove("xebJudgementCutAnchorX");
        nbt.remove("xebJudgementCutAnchorY");
        nbt.remove("xebJudgementCutAnchorZ");

        // Sync client cleanup
        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new JudgementCutSyncPacket(player.getId(), false, anchor, 0));
    }
}
