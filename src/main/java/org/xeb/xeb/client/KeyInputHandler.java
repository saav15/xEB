package org.xeb.xeb.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.network.ActuarKeyPacket;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KeyInputHandler {
    private static boolean wasBlockKeyHeld = false;

    // Optic Blast hold state tracking
    private static boolean wasOpticActiva1Held = false;
    private static boolean wasOpticActiva2Held = false;

    // Track last weapon type to flush inputs on switch
    private static int lastHeldWeapon = 0; // 0 = none, 1 = V1, 2 = V2, 3 = Optic

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && mc.screen == null) {
            Player player = mc.player;
            
            boolean holdsV1 = player.getMainHandItem().is(ModItems.DOOMFIST.get())
                    || player.getOffhandItem().is(ModItems.DOOMFIST.get());
            boolean holdsV2 = player.getMainHandItem().is(ModItems.DOOMFIST_V2.get())
                    || player.getOffhandItem().is(ModItems.DOOMFIST_V2.get());
            boolean holdsOpticBlast = player.getMainHandItem().is(ModItems.OPTIC_BLAST.get())
                    || player.getOffhandItem().is(ModItems.OPTIC_BLAST.get());

            int currentWeapon = 0;
            if (holdsV1) currentWeapon = 1;
            else if (holdsV2) currentWeapon = 2;
            else if (holdsOpticBlast) currentWeapon = 3;

            if (currentWeapon != lastHeldWeapon) {
                // Weapon switched! Flush click queue buffer completely
                while (ModKeyMappings.ACTIVA_1_KEY.consumeClick());
                while (ModKeyMappings.ACTIVA_2_KEY.consumeClick());
                while (ModKeyMappings.ACTIVA_3_KEY.consumeClick());
                wasBlockKeyHeld = false;
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
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

                wasBlockKeyHeld = false;
            } else {
                wasBlockKeyHeld = false;
                wasOpticActiva1Held = false;
                wasOpticActiva2Held = false;
            }
        }
    }
}
