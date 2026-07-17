package org.xeb.xeb.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.particles.ParticleTypes;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPacketHandler {
    /**
     * Pending medallion sync data for entities that haven't been loaded on the client yet.
     *
     * <p><b>N9 fix:</b> The original plain {@code ConcurrentHashMap} had no expiry — entries for
     * entities that were never loaded (e.g. a mob that despawned before the chunk loaded) would
     * accumulate indefinitely.  Each entry now carries a creation timestamp; entries older than
     * {@link #PENDING_TTL_MS} are pruned on each write to cap steady-state memory use.</p>
     */
    private static final long PENDING_TTL_MS = 30_000L; // 30 s
    private static final ConcurrentHashMap<Integer, long[]> PENDING_TIMES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ListTag>  PENDING_SYNCS = new ConcurrentHashMap<>();

    public static ListTag getPendingSync(int entityId) {
        PENDING_TIMES.remove(entityId);
        return PENDING_SYNCS.remove(entityId);
    }

    public static void addPendingSync(int entityId, ListTag tag) {
        long now = System.currentTimeMillis();
        // Prune stale entries before inserting so the map stays bounded.
        PENDING_TIMES.entrySet().removeIf(e -> {
            if (now - e.getValue()[0] > PENDING_TTL_MS) {
                PENDING_SYNCS.remove(e.getKey());
                return true;
            }
            return false;
        });
        PENDING_SYNCS.put(entityId, tag);
        PENDING_TIMES.put(entityId, new long[]{now});
    }

    public static void handleMedallionSync(MedallionSyncPacket msg) {
        Entity entity = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(msg.getEntityId()) : null;
        List<MedallionData> list = new ArrayList<>();
        for (int i = 0; i < msg.getBuffIds().size(); i++) {
            String id = msg.getBuffIds().get(i);
            String tierName = msg.getTiers().get(i);
            EliteBuff buff = EliteBuffRegistry.getById(id);
            if (buff != null) {
                try {
                    MedallionType tier = MedallionType.valueOf(tierName);
                    list.add(new MedallionData(buff, tier, UUID.randomUUID()));
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        }
        
        ListTag listTag = new ListTag();
        for (MedallionData m : list) {
            CompoundTag entry = new CompoundTag();
            entry.putString("BuffId", m.getBuff().getId());
            entry.putString("Tier", m.getTier().name());
            entry.putUUID("UUID", m.getUniqueId());
            listTag.add(entry);
        }

        if (entity instanceof LivingEntity living) {
            living.getPersistentData().put(MedallionManager.MEDALLIONS_KEY, listTag);
            try {
                living.refreshDimensions();
            } catch (Exception ignored) {}
        } else {
            addPendingSync(msg.getEntityId(), listTag);
        }
    }

    public static void handleBuffParticle(BuffParticlePacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (int i = 0; i < msg.getCount(); i++) {
                double ox = (mc.level.random.nextDouble() - 0.5) * 0.5;
                double oy = mc.level.random.nextDouble() * 0.5;
                double oz = (mc.level.random.nextDouble() - 0.5) * 0.5;
                
                switch (msg.getParticleName()) {
                    case "sonic_boom" -> mc.level.addParticle(ParticleTypes.SONIC_BOOM, msg.getX(), msg.getY() + 1.0, msg.getZ(), 0, 0, 0);
                    case "flame"   -> mc.level.addParticle(ParticleTypes.FLAME, msg.getX() + ox, msg.getY() + oy, msg.getZ() + oz, 0, 0.05, 0);
                    case "creepy"  -> mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER, msg.getX() + ox, msg.getY() + oy, msg.getZ() + oz, 0, 0.02, 0);
                    case "dodge"   -> mc.level.addParticle(ParticleTypes.POOF, msg.getX() + ox, msg.getY() + oy + 0.5, msg.getZ() + oz, 0, 0, 0);
                    case "crit"    -> mc.level.addParticle(ParticleTypes.CRIT, msg.getX() + ox, msg.getY() + oy + 0.5, msg.getZ() + oz, 0, 0.1, 0);
                    case "revival" -> {
                        if (mc.level.random.nextBoolean()) {
                            mc.level.addParticle(ParticleTypes.END_ROD, msg.getX() + ox, msg.getY() + oy + 1.0, msg.getZ() + oz, 0, 0.05, 0);
                        } else {
                            mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, msg.getX() + ox, msg.getY() + oy + 1.0, msg.getZ() + oz, 0, 0.05, 0);
                        }
                    }
                    case "sandstorm" -> mc.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, msg.getX() + ox * 4, msg.getY() + oy, msg.getZ() + oz * 4, 0, 0.02, 0);
                    case "evolve"  -> mc.level.addParticle(ParticleTypes.END_ROD, msg.getX() + ox, msg.getY() + oy + 1.0, msg.getZ() + oz, 0, 0.05, 0);
                    case "mega"    -> mc.level.addParticle(ParticleTypes.DRAGON_BREATH, msg.getX() + ox, msg.getY() + oy + 0.5, msg.getZ() + oz, 0, 0.02, 0);
                    case "static"  -> mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, msg.getX() + ox, msg.getY() + oy + 0.5, msg.getZ() + oz, 0, 0, 0);
                    case "tarred"  -> mc.level.addParticle(ParticleTypes.SQUID_INK, msg.getX() + ox, msg.getY() + oy + 0.5, msg.getZ() + oz, 0, 0, 0);

                    // ── Dodge wave: subtle expanding ring of white smoke ──────────────────
                    case "dodge_wave" -> {
                        // Each call spawns one ring-point; count=12 gives a full ring
                        double angle = (i / (double) msg.getCount()) * Math.PI * 2.0;
                        double radius = 0.6;
                        double rx = Math.cos(angle) * radius;
                        double rz = Math.sin(angle) * radius;
                        mc.level.addParticle(ParticleTypes.POOF,
                                msg.getX() + rx, msg.getY() + 0.05, msg.getZ() + rz,
                                rx * 0.03, 0.0, rz * 0.03);
                    }

                    // ── Blind: dark splotch of missed impact ─────────────────────────────
                    case "blind" -> mc.level.addParticle(ParticleTypes.SMOKE,
                            msg.getX() + ox, msg.getY() + oy + 0.5, msg.getZ() + oz, 0, 0.01, 0);

                    // ── Mana Leech: purple/blue drain wisps ─────────────────────────────
                    case "mana_leech" -> mc.level.addParticle(ParticleTypes.ENCHANTED_HIT,
                            msg.getX() + ox, msg.getY() + oy + 0.5, msg.getZ() + oz, 0, 0.05, 0);

                    // ── Marked: crimson crit sparks ──────────────────────────────────────
                    case "marked" -> mc.level.addParticle(ParticleTypes.DAMAGE_INDICATOR,
                            msg.getX() + ox * 0.5, msg.getY() + oy + 0.8, msg.getZ() + oz * 0.5, 0, 0.02, 0);

                    // ── Doomed: dark portal particles circling the victim ────────────────
                    case "doomed" -> mc.level.addParticle(ParticleTypes.PORTAL,
                            msg.getX() + ox, msg.getY() + oy + 1.0, msg.getZ() + oz, 0, -0.05, 0);

                    default -> mc.level.addParticle(ParticleTypes.PORTAL,
                            msg.getX() + ox, msg.getY() + oy, msg.getZ() + oz, 0, 0, 0);
                }
            }
        }
    }

    public static void handleDoomfistDash(DoomfistDashPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(msg.getEntityId());
            if (entity instanceof LivingEntity living) {
                CompoundTag tag = living.getPersistentData();
                if (msg.isDashing()) {
                    tag.putBoolean("xebDoomfistDashing", true);
                    tag.putInt("xebDoomfistDashTimer", 15);
                    tag.putFloat("xebDoomfistChargeRatio", msg.getChargeRatio());
                } else {
                    tag.remove("xebDoomfistDashing");
                    tag.remove("xebDoomfistDashTimer");
                    tag.remove("xebDoomfistChargeRatio");
                }
            }
        }
    }

    public static void handleDoomfistAbility(DoomfistAbilitySyncPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(msg.getEntityId());
            if (entity instanceof LivingEntity living) {
                CompoundTag tag = living.getPersistentData();
                tag.putInt("xebUppercutFloatTicks", msg.getUppercutFloatTicks());
                tag.putInt("xebSlamState", msg.getSlamState());
                
                // Sync remaining cooldown values
                if (msg.getUppercutCooldown() > 0) {
                    tag.putInt("xebUppercutCooldownTicks", msg.getUppercutCooldown());
                }
                if (msg.getSlamCooldown() > 0) {
                    tag.putInt("xebSlamCooldownTicks", msg.getSlamCooldown());
                }

                // Sync block HUD flash
                if (msg.isFlashHUD()) {
                    tag.putInt("xebBlockFlashTicks", 25);
                }

                if (msg.getSlamState() == 1) {
                    tag.putInt("xebSlamTimer", 15);
                } else if (msg.getSlamState() == 2) {
                    // Sync target coordinates during slam phase
                    tag.putDouble("xebSlamTargetX", msg.getTargetX());
                    tag.putDouble("xebSlamTargetY", msg.getTargetY());
                    tag.putDouble("xebSlamTargetZ", msg.getTargetZ());
                    tag.putDouble("xebSlamStartY", living.getY()); // Store starting Y coordinate on client
                } else if (msg.getSlamState() == 0) {
                    tag.remove("xebSlamTimer");
                    tag.remove("xebSlamState");
                    tag.remove("xebSlamTargetX");
                    tag.remove("xebSlamTargetY");
                    tag.remove("xebSlamTargetZ");
                }
            }
        }
    }

    public static void handleDoomfistUltraCharge(DoomfistUltraChargeSyncPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(msg.getEntityId());
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                if (msg.isUltraCharged()) {
                    player.getPersistentData().putBoolean("xebUltraCharged", true);
                } else {
                    player.getPersistentData().remove("xebUltraCharged");
                }
            }
        }
    }

    public static void handleDoomfistPowerBlock(DoomfistPowerBlockSyncPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(msg.getEntityId());
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                if (msg.isPowerBlocking()) {
                    living.getPersistentData().putBoolean("xebPowerBlocking", true);
                } else {
                    living.getPersistentData().remove("xebPowerBlocking");
                }
            }
        }
    }

    public static void handlePermanightSync(PermanightSyncPacket msg) {
        org.xeb.xeb.client.PermanightClientRenderer.isPermanightActive = msg.isActive();
    }

    public static void handleMechaSync(MechaSyncPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(msg.getEntityId());
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
                tag.putBoolean("xebMechaJetActive", msg.isJetActive());
                tag.putBoolean("xebMechaVulcanFiring", msg.isVulcanFiring());
                tag.putInt("xebMechaSpindashState", msg.getSpindashState());
                tag.putBoolean("xebMechaOverdriveDashing", msg.isDashing());
                tag.putInt("xebMechaA1Cooldown", msg.getA1Cooldown());
                tag.putInt("xebMechaA2Cooldown", msg.getA2Cooldown());
                tag.putDouble("xebMechaKineticSpeed", msg.getKineticSpeed());
                tag.putDouble("xebMechaMomentum", msg.getMomentum());
                tag.putBoolean("xebMechaOvercharged", msg.isOvercharged());
                tag.putBoolean("xebMechaLevitating", msg.isLevitating());
                tag.putInt("xebMechaSpindashCharge", msg.getSpindashCharge());
            }
        }
    }

    public static void handleHolySync(HolySyncPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(msg.getEntityId());
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                net.minecraft.nbt.CompoundTag tag = player.getPersistentData();
                tag.putBoolean("xebHolyShieldActive", msg.isShieldActive());
                tag.putBoolean("xebHolyAnnihilationActive", msg.isAnnihilationActive());
                tag.putBoolean("xebHolyBlessedActive", msg.isBlessedActive());
                tag.putInt("xebHolyBlessedTicks", msg.getBlessedTicks());
                tag.putInt("xebHolyBlastCharge", msg.getBlastCharge());
                tag.putDouble("xebHolyBlastRadius", msg.getBlastRadius());
                tag.putInt("xebHolyA1Cooldown", msg.getA1Cooldown());
                tag.putInt("xebHolyA2Cooldown", msg.getA2Cooldown());
                tag.putInt("xebHolyComboStage", msg.getComboStage());
            }
        }
    }

    public static void handleOmegaFlowerySync(OmegaFlowerySyncPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(msg.getEntityId());
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                player.getPersistentData().putBoolean("xebOmegaFloweryActive", msg.isActive());
                player.getPersistentData().putInt("xebOmegaFloweryTicks", msg.getTicksRemaining());
            }
        }
    }
}
