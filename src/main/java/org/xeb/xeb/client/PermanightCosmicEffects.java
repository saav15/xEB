package org.xeb.xeb.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.medallion.MedallionManager;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Xeb.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PermanightCosmicEffects {

    // Toggle flags for easy enabling/disabling
    public static boolean ENABLE_SENTINEL_SWEEP = false;
    public static boolean ENABLE_ELITE_THREADS = true;
    public static boolean ENABLE_GRAVITY_SHARDS = true;

    private static final Random RANDOM = new Random();
    private static List<LivingEntity> cachedElites = new java.util.ArrayList<>();
    private static long lastEliteCheckTime = 0;

    private static float findCeilingY(net.minecraft.world.level.Level level, double x, double startY, double z, float maxDistance) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos(x, startY + 0.5D, z);
        double maxY = startY + maxDistance;
        while (pos.getY() < maxY && pos.getY() < level.getMaxBuildHeight()) {
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos).blocksMotion()) {
                return (float) pos.getY() - 0.05F;
            }
            pos.move(net.minecraft.core.Direction.UP);
        }
        return (float) maxY;
    }

    private static double findGroundY(net.minecraft.world.level.Level level, double x, double startY, double z) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos(x, startY, z);
        for (int i = 0; i < 30; i++) {
            if (pos.getY() <= level.getMinBuildHeight()) break;
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos).blocksMotion()) {
                return pos.getY() + 1.05D;
            }
            pos.move(net.minecraft.core.Direction.DOWN);
        }
        return level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, net.minecraft.core.BlockPos.containing(x, startY, z)).getY() + 0.05D;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!PermanightClientHandler.isPermanightActive()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        long time = mc.level.getGameTime();

        // 1. Feature 2: Elite Energy Threads / Filaments into the sky
        if (ENABLE_ELITE_THREADS) {
            renderEliteThreads(buffer, tesselator, matrix, mc, camPos, time);
        }

        // 2. Feature 1: Sentinel Radar Sweep Light Ray (Disabled by request)
        if (ENABLE_SENTINEL_SWEEP) {
            renderSentinelSweep(buffer, tesselator, matrix, mc, camPos, time);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderEliteThreads(BufferBuilder buffer, Tesselator tesselator, Matrix4f matrix, Minecraft mc, Vec3 camPos, long time) {
        if (time - lastEliteCheckTime > 10) {
            lastEliteCheckTime = time;
            AABB area = mc.player.getBoundingBox().inflate(64.0D);
            cachedElites = mc.level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && !MedallionManager.getMedallions(e).isEmpty());
        }

        for (LivingEntity elite : cachedElites) {
            if (elite == null || !elite.isAlive() || !mc.player.hasLineOfSight(elite)) continue;
            Vec3 pos = elite.getPosition(0.0F).add(0, elite.getBbHeight() * 0.7D, 0);
            float ceilingY = findCeilingY(mc.level, pos.x, pos.y, pos.z, 80.0F);

            float width = 0.08F;
            float pulse = (float) (Math.sin((time + elite.getId() * 10) * 0.1D) * 0.3D + 0.7D);

            boolean isGolden = MedallionManager.getMedallions(elite).stream().anyMatch(m -> m.getTier() == org.xeb.xeb.medallion.MedallionType.LEGENDARY);
            int red = isGolden ? 255 : 160;
            int green = isGolden ? 30 : 20;
            int blue = isGolden ? 64 : 240;
            int alpha = (int) (200 * pulse);

            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            buffer.vertex(matrix, (float) pos.x - width, (float) pos.y, (float) pos.z - width).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, (float) pos.x + width, (float) pos.y, (float) pos.z + width).color(red, green, blue, alpha).endVertex();
            buffer.vertex(matrix, (float) pos.x - width, ceilingY, (float) pos.z - width).color(red, green, blue, 0).endVertex();
            buffer.vertex(matrix, (float) pos.x + width, ceilingY, (float) pos.z + width).color(red, green, blue, 0).endVertex();
            tesselator.end();
        }
    }

    private static void renderSentinelSweep(BufferBuilder buffer, Tesselator tesselator, Matrix4f matrix, Minecraft mc, Vec3 camPos, long time) {
        long cycle = time % 600L; // 30-second cycle (600 ticks)
        if (cycle >= 100L) return; // Sweep lasts 5 seconds (100 ticks)

        float progress = cycle / 100.0F;
        float sweepAngle = progress * (float) Math.PI * 2.0F;

        Vec3 pPos = mc.player.position();
        float radius = 40.0F;

        float startX = (float) pPos.x + (float) Math.cos(sweepAngle) * radius;
        float startZ = (float) pPos.z + (float) Math.sin(sweepAngle) * radius;

        float topY = (float) pPos.y + 90.0F;
        float width = 3.5F;
        int alpha = (int) (160 * Math.sin(progress * Math.PI));

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, (float) pPos.x - width, topY, (float) pPos.z - width).color(180, 40, 255, alpha).endVertex();
        buffer.vertex(matrix, (float) pPos.x + width, topY, (float) pPos.z + width).color(180, 40, 255, alpha).endVertex();
        buffer.vertex(matrix, startX - width, (float) pPos.y, startZ - width).color(255, 30, 64, 0).endVertex();
        buffer.vertex(matrix, startX + width, (float) pPos.y, startZ + width).color(255, 30, 64, 0).endVertex();
        tesselator.end();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!PermanightClientHandler.isPermanightActive()) return;
        if (!ENABLE_GRAVITY_SHARDS) return;
        if (event.phase != TickEvent.Phase.END) return;

        net.minecraft.world.entity.player.Player player = event.player;
        if (player == null || !player.level().isClientSide()) return;

        // Feature 3: Dimensional Gravity Shards (Rising Particles from actual ground)
        if (RANDOM.nextFloat() < 0.50F) {
            double rx = player.getX() + (RANDOM.nextDouble() - 0.5D) * 16.0D;
            double rz = player.getZ() + (RANDOM.nextDouble() - 0.5D) * 16.0D;
            double groundY = findGroundY(player.level(), rx, player.getY(), rz);
            double ry = groundY + RANDOM.nextDouble() * 0.5D;

            // Enchantment Glyphs & Portal particles rising upward against gravity
            player.level().addParticle(ParticleTypes.ENCHANT, rx, ry, rz, 0.0D, 0.15D, 0.0D);
            if (RANDOM.nextFloat() < 0.30F) {
                player.level().addParticle(ParticleTypes.REVERSE_PORTAL, rx, ry, rz, 0.0D, 0.08D, 0.0D);
            }
        }
    }
}
