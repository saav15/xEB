package org.xeb.xeb.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.effect.ModEffects;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CharmedHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        handleCharmedDamage(event);
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            if (attacker.hasEffect(ModEffects.CHARMED.get())) {
                UUID ownerUUID = attacker.getPersistentData().contains("xebCharmedOwner") ? 
                        attacker.getPersistentData().getUUID("xebCharmedOwner") : null;
                if (ownerUUID != null && event.getEntity().getUUID().equals(ownerUUID)) {
                    event.setCanceled(true);
                    if (attacker instanceof Player player) {
                        String ownerName = player.getPersistentData().getString("xebCharmedOwnerName");
                        player.displayClientMessage(Component.literal("§c§l[!] Estás encantado por " + ownerName + " y no puedes atacarle."), true);
                    }
                }
            }
        }
    }

    private static void handleCharmedDamage(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity sourceEntity = event.getSource().getEntity();

        // 1. Prevent Charmed Entity (Player or Mob) from harming their Owner
        if (sourceEntity instanceof LivingEntity attacker && attacker.hasEffect(ModEffects.CHARMED.get())) {
            UUID ownerUUID = attacker.getPersistentData().contains("xebCharmedOwner") ? 
                    attacker.getPersistentData().getUUID("xebCharmedOwner") : null;
            if (ownerUUID != null && victim.getUUID().equals(ownerUUID)) {
                event.setCanceled(true);
                if (attacker instanceof Mob mob) {
                    mob.setTarget(null);
                }
                return;
            }
        }

        // 2. If Owner was hurt by an enemy, alert all nearby Charmed allies to attack that enemy!
        if (sourceEntity instanceof LivingEntity enemy && !victim.level().isClientSide()) {
            AABB area = victim.getBoundingBox().inflate(16.0);
            List<LivingEntity> allies = victim.level().getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity ally : allies) {
                if (ally.hasEffect(ModEffects.CHARMED.get()) && ally != enemy) {
                    UUID ownerUUID = ally.getPersistentData().contains("xebCharmedOwner") ? 
                            ally.getPersistentData().getUUID("xebCharmedOwner") : null;
                    if (ownerUUID != null && ownerUUID.equals(victim.getUUID())) {
                        if (ally instanceof Mob mobAlly) {
                            mobAlly.setTarget(enemy);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            Player charmedPlayer = event.player;
            if (charmedPlayer.hasEffect(ModEffects.CHARMED.get())) {
                UUID ownerUUID = charmedPlayer.getPersistentData().contains("xebCharmedOwner") ? 
                        charmedPlayer.getPersistentData().getUUID("xebCharmedOwner") : null;
                if (ownerUUID == null) return;

                ServerLevel level = (ServerLevel) charmedPlayer.level();
                Entity ownerEntity = level.getEntity(ownerUUID);
                if (ownerEntity instanceof LivingEntity owner && owner.isAlive()) {
                    // Find target enemy attacking owner
                    LivingEntity enemy = owner.getLastHurtByMob();
                    if (enemy == null || !enemy.isAlive() || enemy == charmedPlayer) {
                        enemy = owner.getLastHurtMob();
                    }

                    if (enemy != null && enemy.isAlive() && enemy != charmedPlayer && enemy != owner) {
                        // Auto-move player towards enemy
                        Vec3 pPos = charmedPlayer.position();
                        Vec3 ePos = enemy.position();
                        Vec3 dir = ePos.subtract(pPos).normalize();

                        if (pPos.distanceToSqr(ePos) > 2.5) {
                            charmedPlayer.setDeltaMovement(dir.x * 0.28, charmedPlayer.getDeltaMovement().y, dir.z * 0.28);
                            charmedPlayer.hasImpulse = true;
                        }

                        // Auto-attack enemy when close (< 3.5 blocks)
                        if (pPos.distanceToSqr(ePos) <= 12.25) {
                            charmedPlayer.attack(enemy);
                            charmedPlayer.swing(InteractionHand.MAIN_HAND, true);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMobTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob mob && mob.isAlive() && mob.hasEffect(ModEffects.CHARMED.get())) {
                    UUID ownerUUID = mob.getPersistentData().contains("xebCharmedOwner") ? 
                            mob.getPersistentData().getUUID("xebCharmedOwner") : null;
                    if (ownerUUID == null) continue;

                    Entity ownerEntity = level.getEntity(ownerUUID);
                    if (ownerEntity instanceof LivingEntity owner && owner.isAlive()) {
                        // Clear target if mob is targeting owner
                        if (mob.getTarget() != null && mob.getTarget().getUUID().equals(ownerUUID)) {
                            mob.setTarget(null);
                        }

                        // Target owner's enemies
                        LivingEntity enemy = owner.getLastHurtByMob();
                        if (enemy == null || !enemy.isAlive() || enemy == mob) {
                            enemy = owner.getLastHurtMob();
                        }

                        if (enemy != null && enemy.isAlive() && enemy != mob && enemy != owner) {
                            mob.setTarget(enemy);
                        } else if (mob.getTarget() == null && mob.distanceToSqr(owner) > 25.0) {
                            mob.getNavigation().moveTo(owner, 1.25D);
                        }
                    }
                }
            }
        }
    }
}
