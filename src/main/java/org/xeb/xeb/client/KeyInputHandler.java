package org.xeb.xeb.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.item.MechaOverdriveItem;
import org.xeb.xeb.item.HolyDualityBladeItem;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.network.ActuarKeyPacket;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KeyInputHandler {
    private static boolean wasBlockKeyHeld = false;

    // Optic Blast hold state tracking
    private static boolean wasOpticActiva1Held = false;
    private static boolean wasOpticActiva2Held = false;

    // Mecha and Holy hold state tracking
    private static boolean wasMechaActiva1Held = false;
    private static boolean wasMechaActiva2Held = false;
    private static boolean wasHolyActiva2Held = false;

    // Track last weapon type to flush inputs on switch
    private static int lastHeldWeapon = 0; // 0 = none, 1 = V1, 2 = V2, 3 = Optic, etc.

    @SubscribeEvent
    public static void onInteraction(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null && mc.screen == null) {
                Player player = mc.player;
                if (player.getMainHandItem().is(ModItems.BROKEN_DIAMOND.get())) {
                    event.setCanceled(true);
                    event.setSwingHand(true);
                    
                    boolean inPhase3 = player.getPersistentData().getInt("xebCDA1State") == 3;
                    if (!inPhase3 && player.getCooldowns().isOnCooldown(ModItems.BROKEN_DIAMOND.get())) {
                        return;
                    }
                    
                    double reach = 6.0D;
                    net.minecraft.world.phys.HitResult hit = getPlayerPOVHitResult(player, reach);
                    if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
                        net.minecraft.world.phys.EntityHitResult entityHit = (net.minecraft.world.phys.EntityHitResult) hit;
                        if (entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity target) {
                            XEBNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                                    new org.xeb.xeb.network.CrazyDiamondAttackPacket(target.getId()));
                        } else {
                            XEBNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                                    new org.xeb.xeb.network.CrazyDiamondAttackPacket(-1));
                        }
                    } else {
                        XEBNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                                new org.xeb.xeb.network.CrazyDiamondAttackPacket(-1));
                    }
                } else if (player.getMainHandItem().is(ModItems.THE_TEARS.get())) {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                    XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.TearsLeftClickPacket());
                } else if (player.getMainHandItem().is(ModItems.MECHA_OVERDRIVE.get())) {
                    event.setCanceled(true);
                    event.setSwingHand(true);
                    XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.ActuarKeyPacket(5, true));
                } else if (player.getMainHandItem().is(ModItems.HOLY_DUALITY_BLADE.get())) {
                    event.setCanceled(true);
                    event.setSwingHand(true);
                    XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.ActuarKeyPacket(5, true));
                }
            }
        }
    }

    private static net.minecraft.world.phys.HitResult getPlayerPOVHitResult(Player player, double reach) {
        net.minecraft.world.phys.Vec3 start = player.getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = start.add(look.scale(reach));
        
        net.minecraft.world.phys.BlockHitResult blockHit = player.level().clip(new net.minecraft.world.level.ClipContext(
                start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        
        double maxDist = blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? blockHit.getLocation().distanceToSqr(start) : reach * reach;
        
        net.minecraft.world.phys.AABB area = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        net.minecraft.world.phys.EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, start, end, area,
                (entity) -> entity instanceof net.minecraft.world.entity.LivingEntity && entity.isAlive() && entity != player && !(entity instanceof org.xeb.xeb.entity.CrazyDiamondEntity),
                maxDist);
                
        if (entityHit != null) {
            return entityHit;
        }
        return blockHit;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && mc.screen == null) {
            Player player = mc.player;
            
            // Client-side ticking of NBT cooldowns
            net.minecraft.nbt.CompoundTag cdTag = player.getPersistentData();
            if (cdTag.contains("xebCDA1CooldownTicks")) {
                int cd = cdTag.getInt("xebCDA1CooldownTicks");
                if (cd > 0) cdTag.putInt("xebCDA1CooldownTicks", cd - 1);
            }
            if (cdTag.contains("xebCDA2CooldownTicks")) {
                int cd = cdTag.getInt("xebCDA2CooldownTicks");
                if (cd > 0) cdTag.putInt("xebCDA2CooldownTicks", cd - 1);
            }
            
            boolean holdsV1 = player.getMainHandItem().is(ModItems.DOOMFIST.get())
                    || player.getOffhandItem().is(ModItems.DOOMFIST.get());
            boolean holdsV2 = player.getMainHandItem().is(ModItems.DOOMFIST_V2.get())
                    || player.getOffhandItem().is(ModItems.DOOMFIST_V2.get());
            boolean holdsOpticBlast = player.getMainHandItem().is(ModItems.OPTIC_BLAST.get())
                    || player.getOffhandItem().is(ModItems.OPTIC_BLAST.get());
            boolean holdsGoldenFlower = player.getMainHandItem().is(ModItems.GOLDEN_FLOWER.get())
                    || player.getOffhandItem().is(ModItems.GOLDEN_FLOWER.get());
            boolean holdsCD = player.getMainHandItem().is(ModItems.BROKEN_DIAMOND.get())
                    || player.getOffhandItem().is(ModItems.BROKEN_DIAMOND.get());
            boolean holdsTears = player.getMainHandItem().is(ModItems.THE_TEARS.get())
                    || player.getOffhandItem().is(ModItems.THE_TEARS.get());
            boolean holdsMecha = player.getMainHandItem().getItem() instanceof MechaOverdriveItem
                    || player.getOffhandItem().getItem() instanceof MechaOverdriveItem;
            boolean holdsHoly = player.getMainHandItem().getItem() instanceof HolyDualityBladeItem
                    || player.getOffhandItem().getItem() instanceof HolyDualityBladeItem;

            int currentWeapon = 0;
            if (holdsV1) currentWeapon = 1;
            else if (holdsV2) currentWeapon = 2;
            else if (holdsOpticBlast) currentWeapon = 3;
            else if (holdsGoldenFlower) currentWeapon = 4;
            else if (holdsCD) currentWeapon = 5;
            else if (holdsTears) currentWeapon = 6;
            else if (holdsMecha) currentWeapon = 7;
            else if (holdsHoly) currentWeapon = 8;

            if (currentWeapon != lastHeldWeapon) {
                // Weapon switched! Flush click queue buffer completely
                while (ModKeyMappings.ACTIVA_1_KEY.consumeClick());
                while (ModKeyMappings.ACTIVA_2_KEY.consumeClick());
                while (ModKeyMappings.ACTIVA_3_KEY.consumeClick());
                while (ModKeyMappings.FLOURISH_KEY.consumeClick());
                wasBlockKeyHeld = false;
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
                wasMechaActiva1Held = false;
                wasMechaActiva2Held = false;
                wasHolyActiva2Held = false;
                lastHeldWeapon = currentWeapon;
            }

            if (holdsV1) {
                if (ModKeyMappings.ACTIVA_1_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(1, true));
                }
                if (ModKeyMappings.ACTIVA_2_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, true));
                }
                wasBlockKeyHeld = false;
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
            } else if (holdsV2) {
                if (ModKeyMappings.ACTIVA_1_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(1, true));
                }
                
                boolean isBlockKeyDown = ModKeyMappings.ACTIVA_2_KEY.isDown();
                if (isBlockKeyDown && !wasBlockKeyHeld) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, true));
                    wasBlockKeyHeld = true;
                } else if (!isBlockKeyDown && wasBlockKeyHeld) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, false));
                    wasBlockKeyHeld = false;
                }
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
            } else if (holdsOpticBlast) {
                // Optic Blast: both activas support hold-to-fire (press/release)

                // Activa 1 — Cyclone Push (hold)
                boolean isActiva1Down = ModKeyMappings.ACTIVA_1_KEY.isDown();
                if (isActiva1Down && !wasOpticActiva1Held) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(1, true));
                    wasOpticActiva1Held = true;
                } else if (!isActiva1Down && wasOpticActiva1Held) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(1, false));
                    wasOpticActiva1Held = false;
                }

                // Activa 2 — Gene Splice (hold)
                boolean isActiva2Down = ModKeyMappings.ACTIVA_2_KEY.isDown();
                if (isActiva2Down && !wasOpticActiva2Held) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, true));
                    wasOpticActiva2Held = true;
                } else if (!isActiva2Down && wasOpticActiva2Held) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, false));
                    wasOpticActiva2Held = false;
                }

                // Flourish (B) — spam click, envía un packet por cada click
                if (ModKeyMappings.FLOURISH_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(4, true));
                    org.xeb.xeb.client.renderer.BeamStruggleRenderer.recordLocalMash(mc.player.getId());
                }

                wasBlockKeyHeld = false;
            } else if (holdsGoldenFlower) {
                if (ModKeyMappings.ACTIVA_1_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(1, true));
                }
                if (ModKeyMappings.ACTIVA_2_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, true));
                }
                wasBlockKeyHeld = false;
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
            } else if (holdsCD) {
                if (ModKeyMappings.ACTIVA_1_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(1, true));
                }
                if (ModKeyMappings.ACTIVA_2_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, true));
                }
                wasBlockKeyHeld = false;
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
            } else if (holdsTears) {
                if (ModKeyMappings.ACTIVA_1_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(1, true));
                }
                if (ModKeyMappings.ACTIVA_2_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, true));
                }
                if (ModKeyMappings.FLOURISH_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(4, true));
                    org.xeb.xeb.client.renderer.BeamStruggleRenderer.recordLocalMash(mc.player.getId());
                }
                wasBlockKeyHeld = false;
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
            } else if (holdsMecha) {
                boolean isActiva1Down = ModKeyMappings.ACTIVA_1_KEY.isDown();
                if (isActiva1Down && !wasMechaActiva1Held) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(1, true));
                    wasMechaActiva1Held = true;
                } else if (!isActiva1Down && wasMechaActiva1Held) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(1, false));
                    wasMechaActiva1Held = false;
                }

                boolean isActiva2Down = ModKeyMappings.ACTIVA_2_KEY.isDown();
                if (isActiva2Down && !wasMechaActiva2Held) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, true));
                    wasMechaActiva2Held = true;
                } else if (!isActiva2Down && wasMechaActiva2Held) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, false));
                    wasMechaActiva2Held = false;
                }
                wasBlockKeyHeld = false;
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
            } else if (holdsHoly) {
                if (ModKeyMappings.ACTIVA_1_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(1, true));
                }

                boolean isActiva2Down = ModKeyMappings.ACTIVA_2_KEY.isDown();
                if (isActiva2Down && !wasHolyActiva2Held) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, true));
                    wasHolyActiva2Held = true;
                } else if (!isActiva2Down && wasHolyActiva2Held) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(2, false));
                    wasHolyActiva2Held = false;
                }
                wasBlockKeyHeld = false;
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
            } else {
                wasBlockKeyHeld = false;
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
            }

            // Ultimate Curio - Activa 3 (N) Key Input Handler (Global/Compatible with any weapon status)
            if (org.xeb.xeb.item.QuantumCatBarrageItem.hasUltimateCurio(player)) {
                if (ModKeyMappings.ACTIVA_3_KEY.consumeClick()) {
                    XEBNetwork.CHANNEL.sendToServer(new ActuarKeyPacket(3, true));
                }
            }
        }
    }
}
