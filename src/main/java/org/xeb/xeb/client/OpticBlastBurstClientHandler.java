package org.xeb.xeb.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;

/**
 * Client-side camera and visual handler for the Full-Aperture Supernova Extreme Burst.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OpticBlastBurstClientHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int state = mc.player.getPersistentData().getInt("xebOpticBurstState");
        if (state > 0) {
            int timer = mc.player.getPersistentData().getInt("xebOpticBurstTimer");
            float startPitch = mc.player.getPersistentData().getFloat("xebOpticBurstStartPitch");

            if (timer <= 75 && timer > 0) {
                float progress = (75 - timer) / 75.0F; // 0.0F a 1.0F
                float accel = (float) Math.pow(progress, 1.4D);
                float targetPitch = startPitch - (accel * (startPitch + 85.0F)); // Retroceso masivo hacia el cielo (-85°)

                mc.player.setXRot(targetPitch);
                mc.player.xRotO = targetPitch;
            }

            // Disparar XebWaves continuos a lo largo del radio del rayo cada 8 ticks
            if (timer % 8 == 0) {
                net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
                net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();

                // Spawnear ondas en 3 puntos a lo largo de la trayectoria del rayo (0m, 12m, 28m)
                double[] dists = {2.0D, 12.0D, 28.0D};
                float waveRad = 4.0F + ((80 - timer) / 80.0F) * 8.0F; // Expande de 4m a 12m

                for (double d : dists) {
                    net.minecraft.world.phys.Vec3 p = eye.add(look.scale(d));
                    net.minecraft.core.BlockPos pBlock = net.minecraft.core.BlockPos.containing(p);
                    net.minecraft.core.BlockPos ground = mc.level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pBlock);
                    net.minecraft.world.phys.Vec3 wavePos = net.minecraft.world.phys.Vec3.atCenterOf(ground);

                    org.xeb.xeb.render.XebWaves.spawnWave(wavePos, waveRad, 0.40F, 1.8F, 255, 15, 30);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int state = mc.player.getPersistentData().getInt("xebOpticBurstState");
        if (state > 0) {
            int timer = mc.player.getPersistentData().getInt("xebOpticBurstTimer");
            float startPitch = mc.player.getPersistentData().getFloat("xebOpticBurstStartPitch");

            if (timer <= 75 && timer > 0) {
                float progress = (75 - timer) / 75.0F;
                float accel = (float) Math.pow(progress, 1.4D);
                float targetPitch = startPitch - (accel * (startPitch + 85.0F));

                // Hard-lock del ángulo de cámara en el renderizado cliente
                event.setPitch(targetPitch);
            }

            // Subefecto cinematográfico de sacudida de cámara durante el Mega-Láser (12x12)
            if (timer > 0 && timer <= 25) {
                float shake = (float) ((Math.random() - 0.5D) * 2.5F);
                event.setPitch(event.getPitch() + shake);
                event.setYaw(event.getYaw() + shake);
            }
        }
    }
}

