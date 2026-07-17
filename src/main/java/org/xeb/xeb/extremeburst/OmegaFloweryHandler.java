package org.xeb.xeb.extremeburst;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.network.OmegaFlowerySyncPacket;
import org.xeb.xeb.network.XEBNetwork;

/**
 * Handles the per-tick logic of the Omega Flowery instance.
 *
 * <p>Mechanics during 20 seconds ({@value #OMEGA_DURATION} ticks):
 * <ul>
 *   <li>Zero gravity (motion.y clamped to 0 if falling)</li>
 *   <li>Resistance II + All Stats Up I, refreshed every 2 s</li>
 *   <li>Jarona (Activa 2) deals +100% damage ("Final Jarona")</li>
 *   <li>Blue trail sticks to enemies on first Final Jarona hit → explodes for +50% on second</li>
 *   <li>Flower Dance (Activa 1) heals 20% max HP instead of spawning clones</li>
 *   <li>Golden Flower charges recharge 50% faster (interval ÷ 1.5)</li>
 *   <li>Ability cooldowns tick down 2× per tick</li>
 *   <li>Rainbow particle aura sent to clients</li>
 * </ul>
 */
public class OmegaFloweryHandler {

    public static final int    OMEGA_DURATION                  = 400;  // 20 seconds
    public static final double FINAL_JARONA_DAMAGE_MULTIPLIER  = 2.0D; // +100%
    public static final double TRAIL_EXPLOSION_DAMAGE_BONUS    = 0.5D; // +50%
    public static final float  FLOWER_DANCE_HEAL_PERCENT       = 0.20F; // 20% HP
    public static final int    CHARGE_RECHARGE_INTERVAL_NORMAL = 30;   // ticks
    public static final int    CHARGE_RECHARGE_INTERVAL_OMEGA  = 20;   // 50% faster
    public static final int    COOLDOWN_TICKS_PER_TICK_NORMAL  = 1;
    public static final int    COOLDOWN_TICKS_PER_TICK_OMEGA   = 2;    // 50% shorter

    /**
     * Called every server tick from {@link org.xeb.xeb.event.BuffTickHandler}
     * when {@code xebOmegaFloweryTicks > 0}.
     */
    public static void tick(ServerPlayer player) {
        int omegaTicks = player.getPersistentData().getInt("xebOmegaFloweryTicks");
        if (omegaTicks <= 0) {
            if (player.getPersistentData().contains("xebOmegaFloweryTicks")) {
                cleanupOmegaFlowery(player);
            }
            return;
        }

        player.getPersistentData().putInt("xebOmegaFloweryTicks", omegaTicks - 1);
        ServerLevel level = player.serverLevel();

        // ── Zero gravity ─────────────────────────────────────────────────────
        Vec3 motion = player.getDeltaMovement();
        if (motion.y < 0) {
            player.setDeltaMovement(motion.x, 0, motion.z);
            player.hurtMarked = true;
        }
        player.fallDistance = 0.0F;

        // ── Refresh buffs every 40 ticks (2 s) ──────────────────────────────
        if (omegaTicks % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, false, false, false));
            player.addEffect(new MobEffectInstance(ModEffects.ALL_STATS_UP.get(),     80, 0, false, false, false));
        }

        // ── Rainbow particles (pure-int HSB → RGB, no java.awt dependency) ─
        if (level.getGameTime() % 3 == 0) {
            int rgb = hsbToRgb((float) ((level.getGameTime() % 360) / 360.0), 1.0F, 1.0F);
            double r = ((rgb >> 16) & 0xFF) / 255.0;
            double g = ((rgb >>  8) & 0xFF) / 255.0;
            double b = ( rgb        & 0xFF) / 255.0;

            for (int i = 0; i < 3; i++) {
                double ox = (level.random.nextDouble() - 0.5) * 1.0;
                double oy =  level.random.nextDouble() * 2.0;
                double oz = (level.random.nextDouble() - 0.5) * 1.0;
                level.sendParticles(ParticleTypes.END_ROD,
                        player.getX() + ox, player.getY() + oy, player.getZ() + oz,
                        1, r * 0.1, g * 0.1, b * 0.1, 0.02);
            }
        }

        // ── Sync to clients every 2 ticks ────────────────────────────────────
        if (level.getGameTime() % 2 == 0) {
            XEBNetwork.CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new OmegaFlowerySyncPacket(player.getId(), true, omegaTicks));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────

    private static void cleanupOmegaFlowery(ServerPlayer player) {
        player.getPersistentData().remove("xebOmegaFloweryTicks");
        player.getPersistentData().remove("xebOmegaFloweryMaxTicks");
        player.getPersistentData().putBoolean("xebOmegaFloweryActive", false);
        player.getPersistentData().putBoolean("xebExtremeBurstActive", false);
        player.getPersistentData().remove("xebExtremeBurstInstanceTicks");
        player.getPersistentData().remove("xebExtremeBurstInstanceMaxTicks");

        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(ModEffects.ALL_STATS_UP.get());

        // Sync cleanup to clients
        XEBNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new OmegaFlowerySyncPacket(player.getId(), false, 0));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Final Jarona helpers (called from BuffTickHandler Jarona damage block)
    // ─────────────────────────────────────────────────────────────────────────

    public static boolean isOmegaFloweryActive(ServerPlayer player) {
        return player.getPersistentData().getBoolean("xebOmegaFloweryActive");
    }

    /**
     * Applies Final Jarona multiplier and trail-explosion mechanic.
     * Returns the modified damage value.
     *
     * <ul>
     *   <li>First hit on a target: sticks a blue trail (marks NBT) → damage ×2.0</li>
     *   <li>Second hit on a marked target: clears mark, explodes → damage ×2.0 ×1.5</li>
     * </ul>
     */
    public static float applyFinalJaronaDamage(ServerPlayer player, LivingEntity target, float baseDamage) {
        if (!isOmegaFloweryActive(player)) return baseDamage;

        // Base Final Jarona: +100% damage
        float finalDamage = (float) (baseDamage * FINAL_JARONA_DAMAGE_MULTIPLIER);

        String trailKey = "xebOmegaTrailStuck_" + player.getUUID().toString();
        if (target.getPersistentData().getBoolean(trailKey)) {
            // Explosion hit: +50% bonus, clear mark
            finalDamage *= (1.0F + (float) TRAIL_EXPLOSION_DAMAGE_BONUS);
            target.getPersistentData().remove(trailKey);

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION,
                        target.getX(), target.getY() + 1, target.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                sl.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY() + 1, target.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
            }
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.5F);
        } else {
            // First hit: stick trail mark
            target.getPersistentData().putBoolean(trailKey, true);
            target.getPersistentData().putLong("xebOmegaTrailStuckTime", player.level().getGameTime());

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            }
        }

        return finalDamage;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility: HSB → RGB without java.awt dependency
    // ─────────────────────────────────────────────────────────────────────────

    private static int hsbToRgb(float hue, float saturation, float brightness) {
        if (saturation == 0) {
            int v = (int) (brightness * 255);
            return (v << 16) | (v << 8) | v;
        }
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1.0F - saturation);
        float q = brightness * (1.0F - saturation * f);
        float t = brightness * (1.0F - saturation * (1.0F - f));
        return switch ((int) h) {
            case 0  -> rgb(brightness, t,          p);
            case 1  -> rgb(q,          brightness, p);
            case 2  -> rgb(p,          brightness, t);
            case 3  -> rgb(p,          q,          brightness);
            case 4  -> rgb(t,          p,          brightness);
            default -> rgb(brightness, p,          q);
        };
    }

    private static int rgb(float r, float g, float b) {
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }
}
