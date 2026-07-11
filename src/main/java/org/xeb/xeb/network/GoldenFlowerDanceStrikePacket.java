package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GoldenFlowerDanceStrikePacket {
    private final int targetEntityId;
    private final float damageAmount;

    public GoldenFlowerDanceStrikePacket(int targetEntityId, float damageAmount) {
        this.targetEntityId = targetEntityId;
        this.damageAmount = damageAmount;
    }

    public static void encode(GoldenFlowerDanceStrikePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.targetEntityId);
        buf.writeFloat(msg.damageAmount);
    }

    public static GoldenFlowerDanceStrikePacket decode(FriendlyByteBuf buf) {
        return new GoldenFlowerDanceStrikePacket(buf.readInt(), buf.readFloat());
    }

    public static void handle(GoldenFlowerDanceStrikePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.isAlive()) {
                Entity target = player.level().getEntity(msg.targetEntityId);
                if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                    // Check distance to verify target is within reasonable range (say 20 blocks) to prevent exploit
                    if (player.distanceToSqr(livingTarget) <= 400.0D) {
                        // Apply damage!
                        DamageSource source = player.damageSources().mobAttack(player);
                        livingTarget.getPersistentData().putString("xebLastAttackWeapon", "golden_flower");
                        livingTarget.getPersistentData().putString("xebLastAttackType", "right_click");
                        livingTarget.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
                        livingTarget.hurt(source, msg.damageAmount);
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
