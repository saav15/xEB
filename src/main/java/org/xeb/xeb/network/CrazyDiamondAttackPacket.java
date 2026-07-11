package org.xeb.xeb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.network.NetworkEvent;
import org.xeb.xeb.entity.CrazyDiamondEntity;
import org.xeb.xeb.item.ModItems;
import java.util.function.Supplier;
import net.minecraftforge.network.PacketDistributor;

public class CrazyDiamondAttackPacket {
    private final int targetId;

    public CrazyDiamondAttackPacket(int targetId) {
        this.targetId = targetId;
    }

    public static void encode(CrazyDiamondAttackPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.targetId);
    }

    public static CrazyDiamondAttackPacket decode(FriendlyByteBuf buf) {
        return new CrazyDiamondAttackPacket(buf.readInt());
    }

    public static void handle(CrazyDiamondAttackPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.getMainHandItem().is(ModItems.BROKEN_DIAMOND.get())) {
                ServerLevel level = player.serverLevel();
                
                // State machine override check: only trigger patada kick when in Phase 4 (unlocked by second press of DORA!)
                CompoundTag tag = player.getPersistentData();
                if (tag.contains("xebCDA1State")) {
                    int state = tag.getInt("xebCDA1State");
                    if (state == 4) {
                        int targetId = tag.getInt("xebCDA1TargetId");
                        
                        // Trigger Phase 3 Kick!
                        org.xeb.xeb.event.BuffTickHandler.executeCDA1Phase3(player, targetId);
                        
                        CrazyDiamondEntity stand = null;
                        for (Entity e : level.getAllEntities()) {
                            if (e instanceof CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                                stand = cds;
                                break;
                            }
                        }
                        if (stand != null) {
                            stand.setAnimState(CrazyDiamondEntity.STATE_KICKING, 8);
                        }
                        
                        // Set full cooldown (15s = 300 ticks)
                        tag.putInt("xebCDA1CooldownTicks", 300);
                        XEBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                new org.xeb.xeb.network.CrazyDiamondSyncPacket(
                                    300,
                                    tag.getInt("xebCDA2CooldownTicks"),
                                    tag.getInt("xebCDPunches"),
                                    tag.getInt("xebCDChargeTimer")
                                ));
                        
                        tag.remove("xebCDA1State");
                        tag.remove("xebCDA1Timer");
                        tag.remove("xebCDA1TargetId");
                        return;
                    } else {
                        // Combo is in progress (Phases 1, 2, or 3): block normal punches
                        return;
                    }
                }
                
                // Standard punch animation & target impact
                CrazyDiamondEntity stand = null;
                for (Entity e : level.getAllEntities()) {
                    if (e instanceof CrazyDiamondEntity cds && player.getUUID().equals(cds.getOwnerUUID())) {
                        stand = cds;
                        break;
                    }
                }
                
                if (stand != null) {
                    stand.setAnimState(CrazyDiamondEntity.STATE_PUNCHING, 6);
                }
                
                // Set cooldown (20 ticks = 1s)
                player.getCooldowns().addCooldown(ModItems.BROKEN_DIAMOND.get(), 20);
                
                // Server-side player swing animation to sync to other clients
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.3F);
                
                if (msg.targetId != -1) {
                    Entity target = level.getEntity(msg.targetId);
                    if (target instanceof LivingEntity living && target.isAlive() && target != player) {
                        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        float punchDmg = 12.0F + (baseDamage * 0.5F); // Stand 12 + 50% player damage
                        
                        living.getPersistentData().putString("xebLastAttackWeapon", "crazy_diamond");
                        living.getPersistentData().putString("xebLastAttackType", "active1");
                        living.getPersistentData().putLong("xebLastAttackTime", player.level().getGameTime());
                        living.hurt(player.damageSources().playerAttack(player), punchDmg);
                        
                        level.sendParticles(ParticleTypes.CRIT, living.getX(), living.getY(0.5D), living.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.1D);
                        
                        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 0.8F, 1.2F);
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
