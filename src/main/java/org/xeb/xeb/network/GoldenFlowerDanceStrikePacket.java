package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GoldenFlowerDanceStrikePacket {
    private final int casterEntityId;
    private final int targetEntityId;
    private final float damageAmount;

    public GoldenFlowerDanceStrikePacket(int casterEntityId, int targetEntityId, float damageAmount) {
        this.casterEntityId = casterEntityId;
        this.targetEntityId = targetEntityId;
        this.damageAmount = damageAmount;
    }

    public static void encode(GoldenFlowerDanceStrikePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.casterEntityId);
        buf.writeInt(msg.targetEntityId);
        buf.writeFloat(msg.damageAmount);
    }

    public static GoldenFlowerDanceStrikePacket decode(FriendlyByteBuf buf) {
        return new GoldenFlowerDanceStrikePacket(buf.readInt(), buf.readInt(), buf.readFloat());
    }

    public static void handle(GoldenFlowerDanceStrikePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null && sender.isAlive()) {
                net.minecraft.server.level.ServerLevel level = sender.serverLevel();
                Entity caster = level.getEntity(msg.casterEntityId);
                if (caster == null) caster = sender;

                Entity target = level.getEntity(msg.targetEntityId);
                if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                    if (caster.distanceToSqr(livingTarget) <= 625.0D) {
                        DamageSource source;
                        if (caster instanceof net.minecraft.world.entity.player.Player p) {
                            source = level.damageSources().playerAttack(p);
                        } else if (caster instanceof LivingEntity mobCaster) {
                            source = level.damageSources().mobAttack(mobCaster);
                        } else {
                            source = level.damageSources().mobAttack(sender);
                        }

                        livingTarget.getPersistentData().putString("xebLastAttackWeapon", "golden_flower");
                        livingTarget.getPersistentData().putString("xebLastAttackType", "right_click");
                        livingTarget.getPersistentData().putLong("xebLastAttackTime", level.getGameTime());
                        livingTarget.hurt(source, msg.damageAmount);
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
