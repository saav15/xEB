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
    private static final java.util.Map<Integer, ListTag> PENDING_SYNCS = new ConcurrentHashMap<>();

    public static ListTag getPendingSync(int entityId) {
        return PENDING_SYNCS.remove(entityId);
    }

    public static void addPendingSync(int entityId, ListTag tag) {
        PENDING_SYNCS.put(entityId, tag);
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
}
