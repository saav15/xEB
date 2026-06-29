package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.item.ModItems;

import java.util.List;
import java.util.function.Supplier;

public class ActuarKeyPacket {
    private final int button;
    private final boolean press;

    public ActuarKeyPacket(int button) {
        this(button, true);
    }

    public ActuarKeyPacket(int button, boolean press) {
        this.button = button;
        this.press = press;
    }

    public static void encode(ActuarKeyPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.button);
        buf.writeBoolean(msg.press);
    }

    public static ActuarKeyPacket decode(FriendlyByteBuf buf) {
        return new ActuarKeyPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(ActuarKeyPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.isAlive()) {
                boolean holdsV1 = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.DOOMFIST.get())
                        || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.DOOMFIST.get());
                boolean holdsV2 = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.DOOMFIST_V2.get())
                        || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.DOOMFIST_V2.get());

                if (!holdsV1 && !holdsV2) return;

                long time = player.level().getGameTime();

                if (holdsV1) {
                    // --- The Doomfist v1 Abilities ---
                    if (msg.button == 1) {
                        // --- Rising Uppercut ---
                        long lastUppercut = player.getPersistentData().getLong("xebUppercutLastTime");
                        if (time - lastUppercut < 100) return;
                        player.getPersistentData().putLong("xebUppercutLastTime", time);

                        Vec3 look = player.getLookAngle();
                        Vec3 motion = new Vec3(look.x * 0.6D, 1.2D, look.z * 0.6D);
                        player.setDeltaMovement(motion);
                        player.hurtMarked = true;

                        player.getPersistentData().putInt("xebUppercutFloatTicks", 40);
                        player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);

                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new DoomfistAbilitySyncPacket(player.getId(), 40, 0, 0.0D, 0.0D, 0.0D, 100, 0));

                        AABB area = player.getBoundingBox().inflate(1.5D, 1.0D, 1.5D);
                        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                                e -> e != player && e.isAlive() && !e.isAlliedTo(player));

                        double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

                        for (LivingEntity target : targets) {
                            target.hurt(player.damageSources().playerAttack(player), (float) baseDamage);

                            Vec3 targetMotion = new Vec3(look.x * 0.6D, 1.2D, look.z * 0.6D);
                            target.setDeltaMovement(targetMotion);
                            target.hurtMarked = true;

                            target.getPersistentData().putInt("xebUppercutFloatTicks", 40);

                            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                                    new DoomfistAbilitySyncPacket(target.getId(), 40, 0, 0.0D, 0.0D, 0.0D, 0, 0));
                        }

                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2F, 0.7F);
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);

                        if (player.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 10, 0.5D, 0.2D, 0.5D, 0.1D);
                        }

                    } else if (msg.button == 2) {
                        // --- Seismic Slam ---
                        if (player.onGround()) return;

                        long lastSlam = player.getPersistentData().getLong("xebSlamLastTime");
                        if (time - lastSlam < 120) return;
                        player.getPersistentData().putLong("xebSlamLastTime", time);

                        player.getPersistentData().putInt("xebSlamState", 1);
                        player.getPersistentData().putInt("xebSlamTimer", 15);
                        player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);

                        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                        player.hurtMarked = true;

                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new DoomfistAbilitySyncPacket(player.getId(), 0, 1, 0.0D, 0.0D, 0.0D, 0, 120));

                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                    }
                } else {
                    // --- The Doomfist v2 Abilities ---
                    if (msg.button == 1) {
                        // --- Earthquake Slam (V2) ---
                        long lastSlam2 = player.getPersistentData().getLong("xebSlam2LastTime");
                        if (time - lastSlam2 < 140) return; // 7 seconds cooldown
                        player.getPersistentData().putLong("xebSlam2LastTime", time);

                        player.getPersistentData().putInt("xebSlam2State", 1); // Rising/momentum phase
                        player.getPersistentData().putInt("xebSlam2Timer", 25); // 25 ticks total path
                        player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);

                        Vec3 look = player.getLookAngle();
                        Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
                        
                        // Launch player in upward diagonal trajectory
                        player.setDeltaMovement(forward.x * 0.8D, 0.5D, forward.z * 0.8D);
                        player.hurtMarked = true;

                        // Sync to client with 140 tick cooldown
                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new DoomfistAbilitySyncPacket(player.getId(), 0, 0, 0.0D, 0.0D, 0.0D, 140, 0));

                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.2F);
                                
                    } else if (msg.button == 2) {
                        // --- Power Block (V2) ---
                         if (msg.press) {
                             long lastBlock = player.getPersistentData().getLong("xebBlockLastTime");
                             if (time - lastBlock < 140) return; // 7 seconds cooldown

                             // If blocking mid-Earthquake Slam, cancel slam and pop up with upward momentum
                             if (player.getPersistentData().contains("xebSlam2State")) {
                                 player.getPersistentData().remove("xebSlam2State");
                                 player.getPersistentData().remove("xebSlam2Timer");
                                 
                                 XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                         new DoomfistAbilitySyncPacket(player.getId(), 0, 0));
                                         
                                 player.setDeltaMovement(player.getDeltaMovement().x, 0.55D, player.getDeltaMovement().z);
                                 player.hurtMarked = true;
                             }

                             player.getPersistentData().putBoolean("xebPowerBlocking", true);
                             XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                     new DoomfistPowerBlockSyncPacket(player.getId(), true));
                             player.getPersistentData().putInt("xebBlockTimer", 0);
                             player.getPersistentData().putBoolean("xebBlockKeyHeld", true);
                             player.getPersistentData().putFloat("xebBlockDamageAccumulated", 0.0F);

                             // Apply speed penalty
                             net.minecraft.world.entity.ai.attributes.AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                             if (speed != null) {
                                 speed.removeModifier(java.util.UUID.fromString("6a04870c-26d9-4828-98e9-44d4715f606e"));
                                 speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                         java.util.UUID.fromString("6a04870c-26d9-4828-98e9-44d4715f606e"),
                                         "Power Block speed penalty",
                                         -0.35D,
                                         net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL
                                 ));
                             }

                             player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                     SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0F, 1.2F);
                         } else {
                             player.getPersistentData().putBoolean("xebBlockKeyHeld", false);
                         }
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
