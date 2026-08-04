package org.xeb.xeb.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.client.vfx.*;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.entity.FlowerPelletEntity;
import org.xeb.xeb.entity.FlowerProjectileEntity;
import org.xeb.xeb.entity.SpikeProjectileEntity;
import org.xeb.xeb.item.*;
import org.xeb.xeb.network.*;

import java.util.List;

/**
 * Universal Mob Weapon AI Execution Handler.
 *
 * <p>Enables ANY Minecraft Mob (Vanilla or Modded) holding an xEB weapon in its mainhand
 * to dynamically evaluate and execute its held weapon's EXACT Left Click, Right Click,
 * Activa 1, and Activa 2 abilities with proper cooldowns, visual passives, and sound effects.</p>
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobWeaponAIHandler {

    @SubscribeEvent
    public static void onMobTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Mob mob) || !mob.isAlive()) return;

        // Tick active mob Extreme Burst sequences
        if (mob.getPersistentData().getBoolean("xebJudgementCutActive")) {
            org.xeb.xeb.extremeburst.JudgementCutHandler.onServerTick(mob);
        }
        if (mob.getPersistentData().getBoolean("xebSovereignActive")) {
            org.xeb.xeb.extremeburst.SovereignArsenalHandler.onServerTick(mob);
        }
        if (mob.getPersistentData().getInt("xebDogmaBrimstoneTicks") > 0) {
            org.xeb.xeb.extremeburst.DogmaBurstHandler.tick(mob);
        }
        if (mob.getPersistentData().getInt("xebOmegaFloweryTicks") > 0) {
            org.xeb.xeb.extremeburst.OmegaFloweryHandler.tick(mob);
        }

        ItemStack mainhand = mob.getMainHandItem();
        ItemStack offhand = mob.getOffhandItem();

        Item mainItem = mainhand.getItem();
        Item offItem = offhand.getItem();
        Item item = mainItem;

        boolean hasXebWeapon = isXebWeapon(mainItem) || isXebWeapon(offItem);
        boolean hasXebCurio = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(offItem) != null
                || org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(mainItem) != null
                || org.xeb.xeb.extremeburst.ExtremeBurstRegistry.findActiveBurst(mob) != null;

        if (!hasXebWeapon && !hasXebCurio) return;

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || target.isSpectator()) return;

        double distSq = mob.distanceToSqr(target);
        if (distSq > 1024.0D) return; // Beyond 32 blocks

        boolean hasLOS = mob.getSensing().hasLineOfSight(target);
        if (!hasLOS) return;

        Level level = mob.level();
        long gameTime = level.getGameTime();

        // ── 0. EXTREME BURST MOB AI (EQUIPPED IN OFFHAND / MAINHAND / CURIOS) ────
        org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry burstEntry =
                org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(offhand.getItem());
        if (burstEntry == null) {
            burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.getEntry(mainhand.getItem());
        }
        if (burstEntry == null) {
            burstEntry = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.findActiveBurst(mob);
        }

        if (burstEntry != null) {
            long lastBurstTime = mob.getPersistentData().getLong("xebMobExtremeBurstCD");
            if (gameTime - lastBurstTime >= burstEntry.cooldownTicks) {
                if (org.xeb.xeb.extremeburst.ExtremeBurstRegistry.canActivate(mob, burstEntry)) {
                    mob.getPersistentData().putLong("xebMobExtremeBurstCD", gameTime);
                    org.xeb.xeb.extremeburst.ExtremeBurstHandler.handleActivation(mob, burstEntry);
                    return;
                }
            }
        }

        // ── 1. GOLDEN FLOWER MOB AI ──────────────────────────────────────────
        if (item instanceof GoldenFlowerItem) {
            long cdPellets = mob.getPersistentData().getLong("xebMobCD_Pellets");
            long cdHoming = mob.getPersistentData().getLong("xebMobCD_Homing");
            long cdDance = mob.getPersistentData().getLong("xebMobCD_Dance");
            long cdJarona = mob.getPersistentData().getLong("xebMobCD_Jarona");

            // Activa 1: Flower Dance (12s cd) at 5-25 blocks distance
            if (distSq >= 25.0D && distSq <= 625.0D && gameTime - cdDance >= 240) {
                mob.getPersistentData().putLong("xebMobCD_Dance", gameTime);
                mob.getPersistentData().putBoolean("xebFlowerDanceActive", true);
                mob.getPersistentData().putInt("xebFlowerDanceTicksRemaining", 85);

                int[] targetIds = new int[5];
                for (int i = 0; i < 5; i++) targetIds[i] = target.getId();

                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                        new GoldenFlowerDanceStartPacket(mob.getId(), targetIds, 5));
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F, 1.0F);
                return;
            }

            // Activa 2: Jarona Flying Dash (8s cd) at 4-15 blocks distance
            if (distSq >= 16.0D && distSq <= 225.0D && gameTime - cdJarona >= 160) {
                mob.getPersistentData().putLong("xebMobCD_Jarona", gameTime);
                Vec3 dir = target.position().subtract(mob.position()).normalize();
                mob.setDeltaMovement(dir.x * 1.8D, 0.35D, dir.z * 1.8D);
                mob.hurtMarked = true;
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 1, false, false));
                mob.getPersistentData().putInt("xebJaronaDashTicks", 15);

                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                        new JaronaDashPacket(mob.getId(), 2));
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.0F, 1.2F);
                return;
            }

            // Click Derecho: Homing Spore Flowers (6s cd)
            if (distSq <= 400.0D && gameTime - cdHoming >= 120) {
                mob.getPersistentData().putLong("xebMobCD_Homing", gameTime);
                for (int i = 0; i < 5; i++) {
                    FlowerProjectileEntity flower = new FlowerProjectileEntity(level, mob, i);
                    double spreadX = (mob.getRandom().nextDouble() - 0.5D) * 1.5D;
                    double spreadZ = (mob.getRandom().nextDouble() - 0.5D) * 1.5D;
                    flower.setPos(mob.getX() + spreadX, mob.getEyeY() + 0.5D, mob.getZ() + spreadZ);
                    flower.setTarget(target);
                    level.addFreshEntity(flower);
                }
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.2F);
                return;
            }

            // Click Izquierdo: 3 Friendliness Pellets (2s cd)
            if (distSq <= 256.0D && gameTime - cdPellets >= 40) {
                mob.getPersistentData().putLong("xebMobCD_Pellets", gameTime);
                Vec3 eyePos = mob.getEyePosition();
                Vec3 targetPos = target.getEyePosition();
                Vec3 dir = targetPos.subtract(eyePos).normalize();

                for (int i = -1; i <= 1; i++) {
                    FlowerPelletEntity pellet = new FlowerPelletEntity(level, mob);
                    pellet.setPos(eyePos.x, eyePos.y - 0.2D, eyePos.z);
                    Vec3 spreadDir = dir.add((mob.getRandom().nextDouble() - 0.5D) * 0.1D, 0, (mob.getRandom().nextDouble() - 0.5D) * 0.1D);
                    pellet.shoot(spreadDir.x, spreadDir.y, spreadDir.z, 1.6F, 2.0F);
                    level.addFreshEntity(pellet);
                }
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.HOSTILE, 0.8F, 1.5F);
            }
        }

        // ── 2. DOOMFIST V1 & V2 MOB AI ───────────────────────────────────────
        else if (item instanceof DoomfistItem || item instanceof DoomfistV2Item) {
            long cdRocket = mob.getPersistentData().getLong("xebMobCD_RocketPunch");
            long cdSlam = mob.getPersistentData().getLong("xebMobCD_Slam");
            long cdHandCannon = mob.getPersistentData().getLong("xebMobCD_HandCannon");

            // Activa 1: Seismic Slam (10s cd)
            if (distSq >= 9.0D && distSq <= 225.0D && gameTime - cdSlam >= 200) {
                mob.getPersistentData().putLong("xebMobCD_Slam", gameTime);
                mob.getPersistentData().putInt("xebSlamState", 1);
                mob.setDeltaMovement(0, 0.85D, 0);
                mob.hurtMarked = true;
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.5F, 0.5F);
                return;
            }

            // Click Derecho: Rocket Punch (5s cd)
            if (distSq >= 4.0D && distSq <= 196.0D && gameTime - cdRocket >= 100) {
                mob.getPersistentData().putLong("xebMobCD_RocketPunch", gameTime);
                mob.getPersistentData().putBoolean("xebDoomfistDashing", true);
                mob.getPersistentData().putBoolean("xebDoomfistFallProtect", true);
                mob.getPersistentData().putInt("xebDoomfistDashTimer", 15);
                mob.getPersistentData().putFloat("xebDoomfistChargeRatio", 1.0F);

                Vec3 dir = target.position().subtract(mob.position()).normalize();
                mob.setDeltaMovement(dir.x * 2.4D, 0.2D, dir.z * 2.4D);
                mob.hurtMarked = true;

                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                        new DoomfistDashPacket(mob.getId(), true, 1.0F));
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.2F, 0.6F);
                return;
            }

            // Primary: Kinetic Hand Cannon Ranged Shot (2s cd)
            if (distSq > 9.0D && distSq <= 144.0D && gameTime - cdHandCannon >= 40) {
                mob.getPersistentData().putLong("xebMobCD_HandCannon", gameTime);
                Vec3 dir = target.getEyePosition().subtract(mob.getEyePosition()).normalize();
                target.hurt(level.damageSources().mobAttack(mob), 8.0F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(0.5), target.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                }
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.4F);
            }
        }

        // ── 3. OPTIC BLAST WEAPON MOB AI (EXACT WEAPON, NOT EXTREME BURST) ────
        // ── 3. OPTIC BLAST WEAPON MOB AI (BEAM STRUGGLE COMPATIBLE) ────
        else if (item instanceof OpticBlastItem) {
            long cdOpticShot = mob.getPersistentData().getLong("xebMobCD_OpticShot");
            long cdOpticBeam = mob.getPersistentData().getLong("xebMobCD_OpticBeam");

            // Tick active beam channeling
            int beamTicks = mob.getPersistentData().getInt("xebMobBeamTicks");
            if (beamTicks > 0) {
                mob.getPersistentData().putInt("xebMobBeamTicks", beamTicks - 1);
                Vec3 eyePos = mob.getEyePosition();
                Vec3 targetPos = target.getEyePosition();

                // Register in ActiveBeamManager for Beam Struggle collision detection
                org.xeb.xeb.opticblast.ActiveBeamManager.get().putBeam(mob.getUUID(),
                        new org.xeb.xeb.opticblast.BeamData(mob.getUUID(), mob.getId(), eyePos, targetPos, 0xFFFF0033, gameTime, gameTime + 2L, "optic_blast"));

                XebPillars.spawnPillar(targetPos, 0.5F, 1.2F, 255, 0, 50, 5);
                if (beamTicks % 5 == 0) {
                    target.hurt(level.damageSources().mobAttack(mob), 4.0F);
                    target.setSecondsOnFire(2);
                }
                return;
            }

            // Click Derecho: Continuous Red Optic Plasma Beam (6s cd)
            if (distSq <= 400.0D && gameTime - cdOpticBeam >= 120) {
                mob.getPersistentData().putLong("xebMobCD_OpticBeam", gameTime);
                mob.getPersistentData().putInt("xebMobBeamTicks", 40); // 2s channel
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.2F, 1.8F);
                return;
            }

            // Click Izquierdo: Optic Plasma Pulse Shot (1.5s cd)
            if (distSq <= 256.0D && gameTime - cdOpticShot >= 30) {
                mob.getPersistentData().putLong("xebMobCD_OpticShot", gameTime);
                target.hurt(level.damageSources().mobAttack(mob), 10.0F);
                target.setSecondsOnFire(2);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5), target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
                }
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 1.5F);
            }
        }

        // ── 4. SMART HALBERD MOB AI ─────────────────────────────────────────
        else if (item instanceof SmartHalberdItem) {
            long cdIceSpikes = mob.getPersistentData().getLong("xebMobCD_IceSpikes");

            // Click Derecho: Zigzag Ice Spikes (4s cd)
            if (distSq <= 225.0D && gameTime - cdIceSpikes >= 80) {
                mob.getPersistentData().putLong("xebMobCD_IceSpikes", gameTime);
                Vec3 dir = target.position().subtract(mob.position()).normalize();

                for (int i = 1; i <= 5; i++) {
                    SpikeProjectileEntity spike = new SpikeProjectileEntity(level, mob);
                    Vec3 spikePos = mob.position().add(dir.scale(i * 1.5D));
                    spike.moveTo(spikePos.x, spikePos.y + 0.1D, spikePos.z, mob.getYRot(), mob.getXRot());
                    spike.setDeltaMovement(dir.scale(1.2D));
                    spike.setSpikeIndex(i);
                    level.addFreshEntity(spike);
                }
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2F, 1.4F);
            }
        }

        // ── 5. HOLY DUALITY BLADE MOB AI ────────────────────────────────────
        else if (item instanceof HolyDualityBladeItem) {
            long cdHolySlash = mob.getPersistentData().getLong("xebMobCD_HolySlash");

            // Primary: Holy Energy Arc Slash (2.5s cd)
            if (distSq <= 64.0D && gameTime - cdHolySlash >= 50) {
                mob.getPersistentData().putLong("xebMobCD_HolySlash", gameTime);
                Vec3 dir = target.position().subtract(mob.position()).normalize();
                XebSlashes.spawnSlash(mob.position().add(0, 1.0D, 0), dir, 3.5D, 0.8D, 255, 215, 0, 15);
                target.hurt(level.damageSources().mobAttack(mob), 12.0F);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.8F);
            }
        }

        // ── 6. MECHA OVERDRIVE MOB AI ───────────────────────────────────────
        else if (item instanceof MechaOverdriveItem) {
            long cdMechaSlam = mob.getPersistentData().getLong("xebMobCD_MechaSlam");

            // Secondary: Overdrive Kinetic Impact (5s cd)
            if (distSq <= 36.0D && gameTime - cdMechaSlam >= 100) {
                mob.getPersistentData().putLong("xebMobCD_MechaSlam", gameTime);
                XebExplosions.spawnExplosion(target.position(), 3.0F, 5.0F, 0, 229, 255, 20);
                target.hurt(level.damageSources().mobAttack(mob), 16.0F);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2F, 1.2F);
            }
        }

        // ── 7. BROKEN DIAMOND MOB AI ────────────────────────────────────────
        else if (item instanceof BrokenDiamondItem) {
            long cdDora = mob.getPersistentData().getLong("xebMobCD_Dora");
            long cdTrap = mob.getPersistentData().getLong("xebMobCD_RockTrap");

            // Activa 1: Rock Burial Trap (8s cd)
            if (distSq <= 144.0D && gameTime - cdTrap >= 160) {
                mob.getPersistentData().putLong("xebMobCD_RockTrap", gameTime);
                level.setBlockAndUpdate(target.blockPosition(), Blocks.STONE.defaultBlockState());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5));
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.2F, 0.8F);
                return;
            }

            // Primary: Rapid DORA Barrage (3s cd)
            if (distSq <= 16.0D && gameTime - cdDora >= 60) {
                mob.getPersistentData().putLong("xebMobCD_Dora", gameTime);
                target.hurt(level.damageSources().mobAttack(mob), 15.0F);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.5F, 1.8F);
            }
        }

        // ── 8. THE TEARS MOB AI (BRIMSTONE BEAM STRUGGLE COMPATIBLE) ───────
        else if (item instanceof TheTearsItem) {
            long cdBrimstone = mob.getPersistentData().getLong("xebMobCD_Brimstone");

            // Tick active Brimstone beam
            int brimstoneTicks = mob.getPersistentData().getInt("xebMobBrimstoneTicks");
            if (brimstoneTicks > 0) {
                mob.getPersistentData().putInt("xebMobBrimstoneTicks", brimstoneTicks - 1);
                Vec3 eyePos = mob.getEyePosition();
                Vec3 targetPos = target.getEyePosition();

                // Register in ActiveBeamManager for Beam Struggle collision detection
                org.xeb.xeb.opticblast.ActiveBeamManager.get().putBeam(mob.getUUID(),
                        new org.xeb.xeb.opticblast.BeamData(mob.getUUID(), mob.getId(), eyePos, targetPos, 0xFFCC0000, gameTime, gameTime + 2L, "brimstone"));

                XebPillars.spawnPillar(targetPos, 0.5F, 1.8F, 200, 0, 0, 5);
                if (brimstoneTicks % 5 == 0) {
                    target.hurt(level.damageSources().mobAttack(mob), 5.0F);
                }
                return;
            }

            // Secondary: 40-Block Brimstone Laser Beam (7s cd)
            if (distSq <= 400.0D && gameTime - cdBrimstone >= 140) {
                mob.getPersistentData().putLong("xebMobCD_Brimstone", gameTime);
                mob.getPersistentData().putInt("xebMobBrimstoneTicks", 40); // 2s channel
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.2F, 0.5F);
            }
        }
    }

    private static boolean isXebWeapon(Item item) {
        return item instanceof GoldenFlowerItem ||
               item instanceof DoomfistItem ||
               item instanceof DoomfistV2Item ||
               item instanceof OpticBlastItem ||
               item instanceof SmartHalberdItem ||
               item instanceof HolyDualityBladeItem ||
               item instanceof MechaOverdriveItem ||
               item instanceof BrokenDiamondItem ||
               item instanceof TheTearsItem;
    }
}
