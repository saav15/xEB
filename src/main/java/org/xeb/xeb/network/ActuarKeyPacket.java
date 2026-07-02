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

    public ActuarKeyPacket(int button) {
        this.button = button;
    }

    public static void encode(ActuarKeyPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.button);
    }

    public static ActuarKeyPacket decode(FriendlyByteBuf buf) {
        return new ActuarKeyPacket(buf.readInt());
    }

    public static void handle(ActuarKeyPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (msg.button != 1 && msg.button != 2) return;
            ServerPlayer player = ctx.getSender();
            if (player != null && player.isAlive()) {
                // Ensure they hold Doomfist
                boolean holdsDoomfist = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.DOOMFIST.get())
                        || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.DOOMFIST.get());
                if (!holdsDoomfist) return;

                long time = player.level().getGameTime();

                if (msg.button == 1) {
                    // --- Rising Uppercut ---
                    // Cooldown check (5s = 100 ticks)
                    long lastUppercut = player.getPersistentData().getLong("xebUppercutLastTime");
                    if (time - lastUppercut < 100) return;
                    player.getPersistentData().putLong("xebUppercutLastTime", time);

                    // Impulse player up 5 blocks and slightly forward (increased by 50% from 0.8D/0.4D to 1.2D/0.6D)
                    Vec3 look = player.getLookAngle();
                    Vec3 motion = new Vec3(look.x * 0.6D, 1.2D, look.z * 0.6D);
                    player.setDeltaMovement(motion);
                    player.hurtMarked = true; // Sync velocity to client

                    // Set float ticks (40 ticks = 2 seconds) & fall protection
                    player.getPersistentData().putInt("xebUppercutFloatTicks", 40);
                    player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);

                    // Sync to clients with 100 tick (5s) cooldown
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new DoomfistAbilitySyncPacket(player.getId(), 40, 0, 0.0D, 0.0D, 0.0D, 100, 0));

                    // Deal area damage: 1.5 x 1.5 blocks around the player
                    AABB area = player.getBoundingBox().inflate(1.5D, 1.0D, 1.5D);
                    List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                            e -> e != player && e.isAlive() && !e.isAlliedTo(player));

                    double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

                    for (LivingEntity target : targets) {
                        // Deal damage (1.0x attack damage)
                        target.hurt(player.damageSources().playerAttack(player), (float) baseDamage);

                        // Launch target upwards (increased by 50% as well)
                        Vec3 targetMotion = new Vec3(look.x * 0.6D, 1.2D, look.z * 0.6D);
                        target.setDeltaMovement(targetMotion);
                        target.hurtMarked = true;

                        // Set target float ticks to 40 (2 seconds)
                        target.getPersistentData().putInt("xebUppercutFloatTicks", 40);

                        XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                                new DoomfistAbilitySyncPacket(target.getId(), 40, 0, 0.0D, 0.0D, 0.0D, 0, 0));
                    }

                    // Sounds and particles
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2F, 0.7F);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);

                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 10, 0.5D, 0.2D, 0.5D, 0.1D);
                    }

                } else if (msg.button == 2) {
                    // --- Seismic Slam ---
                    // Must be in the air
                    if (player.onGround()) return;

                    // Cooldown check (6s = 120 ticks)
                    long lastSlam = player.getPersistentData().getLong("xebSlamLastTime");
                    if (time - lastSlam < 120) return;
                    player.getPersistentData().putLong("xebSlamLastTime", time);

                    // Set state to casting (1) for 15 ticks
                    player.getPersistentData().putInt("xebSlamState", 1);
                    player.getPersistentData().putInt("xebSlamTimer", 15);
                    player.getPersistentData().putBoolean("xebDoomfistFallProtect", true);

                    // Stop current motion to hover in place
                    player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    player.hurtMarked = true;

                    // Sync to clients with 120 tick (6s) cooldown
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new DoomfistAbilitySyncPacket(player.getId(), 0, 1, 0.0D, 0.0D, 0.0D, 0, 120));

                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
