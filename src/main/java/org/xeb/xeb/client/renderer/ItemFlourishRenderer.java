package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;

/**
 * Renders custom first-person item inspection ("Flourish") animations, micro-vibrations, and visual glow effects when pressing B.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ItemFlourishRenderer {

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // Smart Halberd first-person cybernetic micro-vibration pulse
        if (stack.is(ModItems.SMART_HALBERD.get())) {
            float intensity = FlourishAnimationManager.getSmartHalberdGlowIntensity();
            int level = FlourishAnimationManager.getSmartHalberdGlowLevel();
            if (intensity > 0.0F && level > 0) {
                renderSmartHalberdVibration(event.getPoseStack(), intensity, level);
            }
            return;
        }

        // Motion-based flourishes (e.g. The Tears)
        if (!FlourishAnimationManager.isFlourishActive()) return;
        Item activeItem = FlourishAnimationManager.getActiveItem();
        if (activeItem == null || !stack.is(activeItem)) return;

        float progress = FlourishAnimationManager.getProgress(event.getPartialTick());
        PoseStack poseStack = event.getPoseStack();

        boolean isRightHand = mc.player.getMainArm() == HumanoidArm.RIGHT;
        float handSide = isRightHand ? 1.0F : -1.0F;

        if (activeItem == ModItems.THE_TEARS.get()) {
            renderTheTearsFlourish(poseStack, progress, handSide);
        }
    }

    /**
     * Smart Halberd First-Person Cybernetic Micro-Vibration:
     * Vibrates hand and item matrix proportionally to glow level and pulse intensity.
     */
    private static void renderSmartHalberdVibration(PoseStack poseStack, float intensity, int level) {
        long time = System.currentTimeMillis();

        float vibX = (float) Math.sin(time * 0.08D) * 0.0035F * level * intensity;
        float vibY = (float) Math.cos(time * 0.09D) * 0.0035F * level * intensity;
        float vibZ = (float) Math.sin(time * 0.10D) * 0.0025F * level * intensity;

        float scalePulse = 1.0F + (0.02F * level * intensity);

        poseStack.translate(vibX, vibY, vibZ);
        poseStack.scale(scalePulse, scalePulse, scalePulse);
    }

    /**
     * The Tears Flourish:
     * Player hand impulses the crystal sphere upwards into the air.
     * The sphere spins on Y & Z axis in mid-air before smoothly dropping back into hand grip.
     */
    private static void renderTheTearsFlourish(PoseStack poseStack, float progress, float handSide) {
        float verticalArc = (float) Math.sin(progress * Math.PI); // 0 -> 1 -> 0
        float spinY = progress * 720.0F; // 2 full 360-degree spins
        float tiltZ = (float) Math.sin(progress * Math.PI * 2.0F) * 35.0F * handSide;

        // Upward toss & forward float
        poseStack.translate(0.04F * handSide * verticalArc, 0.35F * verticalArc, -0.15F * verticalArc);

        // Spin crystal sphere around Y and Z axis
        poseStack.mulPose(Axis.YP.rotationDegrees(spinY * handSide));
        poseStack.mulPose(Axis.ZP.rotationDegrees(tiltZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(verticalArc * 15.0F));
    }
}
