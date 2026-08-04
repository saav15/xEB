package org.xeb.xeb.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Xeb.MODID, value = Dist.CLIENT)
public class MeteorStrikeClientHandler {

    public static class StrikeData {
        public final int entityId;
        public int state;
        public boolean isV2;
        public double tx, ty, tz;
        public int targetCount;
        public long lastUpdate;

        public StrikeData(int entityId, int state, boolean isV2, double tx, double ty, double tz, int targetCount) {
            this.entityId = entityId;
            this.state = state;
            this.isV2 = isV2;
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.targetCount = targetCount;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    public static final Map<Integer, StrikeData> ACTIVE_METEOR_STRIKES = new ConcurrentHashMap<>();
    private static boolean zoomWide = false;
    private static int clientTargetingTicks = 200;

    public static void updateClientStrike(int entityId, int state, boolean isV2, double tx, double ty, double tz, int count) {
        if (state == 0) {
            StrikeData oldData = ACTIVE_METEOR_STRIKES.remove(entityId);
            if (oldData != null && (oldData.state == 2 || oldData.state == 3)) {
                Vec3 impactPos = new Vec3(oldData.tx, oldData.ty, oldData.tz);
                int r = oldData.isV2 ? 255 : 0;
                int g = oldData.isV2 ? 90 : 200;
                int b = oldData.isV2 ? 0 : 255;

                // Invoca 5 ondas concéntricas terrestres limpias y gigantes (grosor 1.5F)
                org.xeb.xeb.render.XebWaves.spawnWave(impactPos, 3.5F, 0.18F, 1.5F, r, g, b);
                org.xeb.xeb.render.XebWaves.spawnWave(impactPos, 7.0F, 0.22F, 1.5F, r, g, b);
                org.xeb.xeb.render.XebWaves.spawnWave(impactPos, 11.0F, 0.26F, 1.5F, r, g, b);
                org.xeb.xeb.render.XebWaves.spawnWave(impactPos, 14.0F, 0.30F, 1.5F, r, g, b);
                org.xeb.xeb.render.XebWaves.spawnWave(impactPos, 17.0F, 0.34F, 1.5F, r, g, b);
            }
        } else {
            StrikeData prev = ACTIVE_METEOR_STRIKES.get(entityId);
            if (prev == null || prev.state != 2) {
                clientTargetingTicks = 200; // Reset 10s timer (200 ticks) upon entering targeting phase
            }
            ACTIVE_METEOR_STRIKES.put(entityId, new StrikeData(entityId, state, isV2, tx, ty, tz, count));
        }
    }
    public static double clientTargetX = 0.0;
    public static double clientTargetY = 0.0;
    public static double clientTargetZ = 0.0;

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int state = mc.player.getPersistentData().getInt("xebMeteorStrikeState");
        if (state == 1 || state == 2) {
            if (clientTargetingTicks > 0) {
                clientTargetingTicks--;
            }

            StrikeData data = ACTIVE_METEOR_STRIKES.get(mc.player.getId());
            double startX = mc.player.getPersistentData().getDouble("xebMeteorStrikeStartX");
            double startY = mc.player.getPersistentData().getDouble("xebMeteorStrikeStartY");
            double startZ = mc.player.getPersistentData().getDouble("xebMeteorStrikeStartZ");

            if (startX == 0.0 && startZ == 0.0) {
                startX = mc.player.getX();
                startY = mc.player.getY();
                startZ = mc.player.getZ();
                mc.player.getPersistentData().putDouble("xebMeteorStrikeStartX", startX);
                mc.player.getPersistentData().putDouble("xebMeteorStrikeStartY", startY);
                mc.player.getPersistentData().putDouble("xebMeteorStrikeStartZ", startZ);
            }

            // Inicializar coordenadas del objetivo al suelo de despegue
            if (clientTargetX == 0.0 && clientTargetZ == 0.0) {
                clientTargetX = startX;
                clientTargetY = startY;
                clientTargetZ = startZ;
            }

            if (zoomWide) {
                // ── MODO 2: CONTROL WASD (POSICIONAR AL JUGADOR CLIENTE SOBRE EL RETÍCULO) ──
                boolean moveW = mc.options.keyUp.isDown();
                boolean moveS = mc.options.keyDown.isDown();
                boolean moveA = mc.options.keyLeft.isDown();
                boolean moveD = mc.options.keyRight.isDown();

                if (moveW || moveS || moveA || moveD) {
                    double speed = 1.35D; // Velocidad de movimiento WASD
                    double fwd = (moveW ? 1.0D : 0.0D) - (moveS ? 1.0D : 0.0D);
                    double strafe = (moveD ? 1.0D : 0.0D) - (moveA ? 1.0D : 0.0D);

                    float yawRad = (float) Math.toRadians(mc.player.getYRot());
                    double cos = Math.cos(yawRad);
                    double sin = Math.sin(yawRad);

                    double dx = (-sin * fwd - cos * strafe) * speed;
                    double dz = (cos * fwd - sin * strafe) * speed;

                    double nextX = clientTargetX + dx;
                    double nextZ = clientTargetZ + dz;

                    // Limitar suavemente a 48m de distancia máxima sin tirones
                    double offsetDx = nextX - startX;
                    double offsetDz = nextZ - startZ;
                    double distSq = offsetDx * offsetDx + offsetDz * offsetDz;
                    if (distSq > 48.0D * 48.0D) {
                        double dist = Math.sqrt(distSq);
                        nextX = startX + (offsetDx / dist) * 48.0D;
                        nextZ = startZ + (offsetDz / dist) * 48.0D;
                    }

                    clientTargetX = nextX;
                    clientTargetZ = nextZ;
                }

                // Buscar el suelo sólido en (clientTargetX, clientTargetZ) respetando cúpulas y cuevas
                int bx = (int) Math.floor(clientTargetX);
                int bz = (int) Math.floor(clientTargetZ);
                int scanStartY = Math.min(mc.level.getMaxBuildHeight() - 1, (int) Math.floor(startY + 6.0D));
                net.minecraft.core.BlockPos cursor = new net.minecraft.core.BlockPos(bx, scanStartY, bz);

                while (cursor.getY() > mc.level.getMinBuildHeight() && mc.level.getBlockState(cursor).getCollisionShape(mc.level, cursor).isEmpty()) {
                    cursor = cursor.below();
                }

                double targetY = cursor.getY() + 1.0D;
                if (!mc.level.getBlockState(cursor).getCollisionShape(mc.level, cursor).isEmpty()) {
                    targetY = cursor.getY() + mc.level.getBlockState(cursor).getCollisionShape(mc.level, cursor).max(net.minecraft.core.Direction.Axis.Y);
                }
                clientTargetY = targetY;

                // Calcular techo sólido overhead para evitar que la cámara atraviese techos de cuevas/casas
                int ceilY = (int) Math.floor(clientTargetY + 1.0D);
                while (ceilY < mc.level.getMaxBuildHeight() - 1 && ceilY < (int) Math.floor(clientTargetY + 14.0D)) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(bx, ceilY, bz);
                    if (!mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty()) {
                        break;
                    }
                    ceilY++;
                }

                double safeCamY = Math.min(clientTargetY + 14.0D, ceilY - 0.6D);
                safeCamY = Math.max(clientTargetY + 2.5D, safeCamY);

                if (state == 2) {
                    mc.player.setPos(clientTargetX, safeCamY, clientTargetZ);
                    mc.player.setDeltaMovement(0, 0, 0);
                    mc.player.fallDistance = 0.0F;
                }

                if (data != null) {
                    data.tx = clientTargetX;
                    data.ty = clientTargetY;
                    data.tz = clientTargetZ;
                }

                mc.player.getPersistentData().putDouble("xebMeteorStrikeTargetX", clientTargetX);
                mc.player.getPersistentData().putDouble("xebMeteorStrikeTargetY", clientTargetY);
                mc.player.getPersistentData().putDouble("xebMeteorStrikeTargetZ", clientTargetZ);

                org.xeb.xeb.network.XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.MeteorStrikeMovePacket(clientTargetX, clientTargetY, clientTargetZ));
            } else {
                // ── MODO 1: CONTROL POR RATÓN (POSICIONAR AL JUGADOR EN ORIGEN TERRESTRE A 12M) ──
                int launchCeilY = (int) Math.floor(startY + 1.0D);
                while (launchCeilY < mc.level.getMaxBuildHeight() - 1 && launchCeilY < (int) Math.floor(startY + 14.0D)) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos((int) Math.floor(startX), launchCeilY, (int) Math.floor(startZ));
                    if (!mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty()) {
                        break;
                    }
                    launchCeilY++;
                }

                double safeOriginCamY = Math.min(startY + 14.0D, launchCeilY - 0.6D);
                safeOriginCamY = Math.max(startY + 2.5D, safeOriginCamY);

                if (state == 2) {
                    mc.player.setPos(startX, safeOriginCamY, startZ);
                    mc.player.setDeltaMovement(0, 0, 0);
                }

                Vec3 launchEye = new Vec3(startX, safeOriginCamY, startZ);
                Vec3 lookVec = mc.player.getLookAngle();
                Vec3 rayEnd = launchEye.add(lookVec.scale(160.0D));

                BlockHitResult hit = mc.level.clip(new ClipContext(
                        launchEye, rayEnd,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        mc.player
                ));

                double hitX, hitZ;
                if (hit.getType() != HitResult.Type.MISS) {
                    hitX = hit.getLocation().x;
                    hitZ = hit.getLocation().z;
                } else {
                    double dy = Math.min(-0.05D, lookVec.y);
                    double t = (startY - safeOriginCamY) / dy;
                    t = Math.min(48.0D, Math.max(0.0D, t));
                    hitX = startX + lookVec.x * t;
                    hitZ = startZ + lookVec.z * t;
                }

                double offsetDx = hitX - startX;
                double offsetDz = hitZ - startZ;
                double distSq = offsetDx * offsetDx + offsetDz * offsetDz;
                if (distSq > 48.0D * 48.0D) {
                    double dist = Math.sqrt(distSq);
                    hitX = startX + (offsetDx / dist) * 48.0D;
                    hitZ = startZ + (offsetDz / dist) * 48.0D;
                }

                clientTargetX = hitX;
                clientTargetZ = hitZ;

                double dx = clientTargetX - startX;
                double dz = clientTargetZ - startZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 48.0D) {
                    clientTargetX = startX + (dx / dist) * 48.0D;
                    clientTargetZ = startZ + (dz / dist) * 48.0D;
                }

                int bx = (int) Math.floor(clientTargetX);
                int bz = (int) Math.floor(clientTargetZ);
                net.minecraft.core.BlockPos scanPos = new net.minecraft.core.BlockPos(bx, (int) Math.floor(startY + 30.0D), bz);
                net.minecraft.core.BlockPos groundBlock = mc.level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, scanPos);

                double targetY = groundBlock.getY() + 1.0D;
                if (!mc.level.getBlockState(groundBlock).getCollisionShape(mc.level, groundBlock).isEmpty()) {
                    targetY = groundBlock.getY() + mc.level.getBlockState(groundBlock).getCollisionShape(mc.level, groundBlock).max(net.minecraft.core.Direction.Axis.Y);
                }
                clientTargetY = targetY;

                if (data != null) {
                    data.tx = clientTargetX;
                    data.ty = clientTargetY;
                    data.tz = clientTargetZ;
                }

                mc.player.getPersistentData().putDouble("xebMeteorStrikeTargetX", clientTargetX);
                mc.player.getPersistentData().putDouble("xebMeteorStrikeTargetY", clientTargetY);
                mc.player.getPersistentData().putDouble("xebMeteorStrikeTargetZ", clientTargetZ);

                org.xeb.xeb.network.XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.MeteorStrikeMovePacket(clientTargetX, clientTargetY, clientTargetZ));
            }
        } else {
            // Reiniciar objetivos cuando acaba la fase
            clientTargetX = 0.0;
            clientTargetY = 0.0;
            clientTargetZ = 0.0;
        }
    }

    // ── OCULTAR MODELO DEL JUGADOR ÚNICAMENTE EN ESTADO 2 (APUNTADO AÉREO) ────────────
    @SubscribeEvent
    public static void onRenderPlayer(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
        if (event.getEntity().getPersistentData().getInt("xebMeteorStrikeState") == 2) {
            event.setCanceled(true); // Ocultar modelo solo cuando ya está arriba en órbita
        }
    }

    @SubscribeEvent
    public static void onRenderHand(net.minecraftforge.client.event.RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int state = mc.player.getPersistentData().getInt("xebMeteorStrikeState");
            if (state == 1 || state == 2) {
                event.setCanceled(true);
            }
        }
    }

    // ── OCULTAR OVERLAYS VANILLA EN ESTADOS 1 Y 2 ──────────────────────────────
    @SubscribeEvent
    public static void onRenderGuiOverlayPre(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int state = mc.player.getPersistentData().getInt("xebMeteorStrikeState");
            if (state == 1 || state == 2) {
                net.minecraftforge.client.gui.overlay.NamedGuiOverlay overlay = event.getOverlay();
                if (overlay == net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.type()
                        || overlay == net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR.type()
                        || overlay == net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.ITEM_NAME.type()
                        || overlay == net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.PLAYER_HEALTH.type()
                        || overlay == net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.ARMOR_LEVEL.type()
                        || overlay == net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.FOOD_LEVEL.type()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    // ── INTERCEPT MOUSE CLICKS (LEFT CLICK -> PLUNGE; RIGHT CLICK -> TOGGLE CAMERA MODE) ──
    @SubscribeEvent
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getPersistentData().getInt("xebMeteorStrikeState") == 2) {
            if (event.getButton() == 0 && event.getAction() == 1) { // Left Click -> Plunge
                event.setCanceled(true);
                org.xeb.xeb.network.XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.ActuarKeyPacket(3, true));
            } else if (event.getButton() == 1 && event.getAction() == 1) { // Right Click -> Toggle Mode
                event.setCanceled(true);
                zoomWide = !zoomWide;
                org.xeb.xeb.network.XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.ActuarKeyPacket(5, true));
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInputTriggered(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int state = mc.player.getPersistentData().getInt("xebMeteorStrikeState");
        if (state == 2) {
            if (event.isUseItem()) {
                event.setCanceled(true);
                event.setSwingHand(false);
                zoomWide = !zoomWide;
                org.xeb.xeb.network.XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.ActuarKeyPacket(5, true));
            } else if (event.isAttack()) {
                event.setCanceled(true);
                event.setSwingHand(false);
                org.xeb.xeb.network.XEBNetwork.CHANNEL.sendToServer(new org.xeb.xeb.network.ActuarKeyPacket(3, true));
            }
        }
    }


    // ── 1. CÁMARAS DUALES DE METEOR STRIKE (ACTIVA DESDE EL DESPEGUE EN ESTADO 1) ─────────
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int state = mc.player.getPersistentData().getInt("xebMeteorStrikeState");
        if (state == 1 || state == 2) { // Active camera during flight launch and targeting phase
            if (zoomWide) {
                // MODO 2 WASD: Vista cenital aérea inclinada (85° pitch) anclada al telegraph
                event.setPitch(85.0F);
                event.setYaw(mc.player.getYRot());
            } else {
                // MODO 1 RATÓN: Vista aérea desde el origen
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int state = mc.player.getPersistentData().getInt("xebMeteorStrikeState");
        if (state == 2 && zoomWide) {
            event.setFOV(event.getFOV() + 20.0D); // Ampliar FOV para vista panorámica WASD
        }
    }

    // ── 2. RENDERIZADO DE TELEGRAPH 3D EN EL SUELO (TORRE + ANILLOS) ───────────
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;

        long now = System.currentTimeMillis();
        ACTIVE_METEOR_STRIKES.entrySet().removeIf(e -> (now - e.getValue().lastUpdate) > 3000);

        CompoundTag localTag = localPlayer.getPersistentData();
        int localState = localTag.getInt("xebMeteorStrikeState");
        if (localState > 0) {
            updateClientStrike(
                    localPlayer.getId(), localState, localTag.getBoolean("xebMeteorStrikeIsV2"),
                    localTag.getDouble("xebMeteorStrikeTargetX"), localTag.getDouble("xebMeteorStrikeTargetY"), localTag.getDouble("xebMeteorStrikeTargetZ"),
                    localTag.getInt("xebMeteorStrikeTargetCount")
            );
        }

        if (ACTIVE_METEOR_STRIKES.isEmpty()) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        for (StrikeData data : ACTIVE_METEOR_STRIKES.values()) {
            if (data.tx == 0.0 && data.ty == 0.0 && data.tz == 0.0) continue;

            double rx = data.tx - cam.x;
            double ry = data.ty + 0.05D - cam.y;
            double rz = data.tz - cam.z;

            poseStack.pushPose();
            poseStack.translate(rx, ry, rz);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();

            // Doomfist v1 = Pure Cyan-Blue (0% Red); Doomfist v2 = Fiery Overdrive Orange-Gold
            float r = data.isV2 ? 1.0F : 0.0F;
            float g = data.isV2 ? 0.35F : 0.75F;
            float b = data.isV2 ? 0.02F : 1.0F;

            long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
            float warningPulse = (data.state == 3)
                    ? (0.5F + 0.5F * (float) Math.sin(time * 0.9F))
                    : (0.85F + 0.15F * (float) Math.sin(time * 0.2F));

            float innerRadius = 2.0F;  // Zona de Epicentro 4x4
            float outerRadius = 6.0F;  // Zona de Onda 12x12
            float towerHeight = 12.0F; // 3D Tower of Light height
            int segments = 48;

            // ── Outer Ring Fill (Shockwave Zone) ──
            buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            buffer.vertex(poseStack.last().pose(), 0.0F, 0.0F, 0.0F).color(r, g, b, 0.25F * warningPulse).endVertex();
            for (int i = 0; i <= segments; i++) {
                double angle = (2 * Math.PI / segments) * i;
                float vx = (float) (outerRadius * Math.cos(angle));
                float vz = (float) (outerRadius * Math.sin(angle));
                buffer.vertex(poseStack.last().pose(), vx, 0.0F, vz).color(r, g, b, 0.08F * warningPulse).endVertex();
            }
            tesselator.end();

            // ── Inner Ring Fill (Epicenter Letal) ──
            buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            buffer.vertex(poseStack.last().pose(), 0.0F, 0.0F, 0.0F).color(r, g, b, 0.60F * warningPulse).endVertex();
            for (int i = 0; i <= segments; i++) {
                double angle = (2 * Math.PI / segments) * i;
                float vx = (float) (innerRadius * Math.cos(angle));
                float vz = (float) (innerRadius * Math.sin(angle));
                buffer.vertex(poseStack.last().pose(), vx, 0.0F, vz).color(r, g, b, 0.30F * warningPulse).endVertex();
            }
            tesselator.end();

            // ── 3D ENERGY TOWER / CYLINDER BEAM (TORRE DE LUZ ELEVADA) ────────
            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i <= segments; i++) {
                double angle = (2 * Math.PI / segments) * i;
                float vx = (float) (innerRadius * Math.cos(angle));
                float vz = (float) (innerRadius * Math.sin(angle));

                buffer.vertex(poseStack.last().pose(), vx, 0.0F, vz).color(r, g, b, 0.35F * warningPulse).endVertex();
                buffer.vertex(poseStack.last().pose(), vx, towerHeight, vz).color(r, g, b, 0.02F * warningPulse).endVertex();
            }
            tesselator.end();

            // ── Outer Line Ring ──
            buffer.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i <= segments; i++) {
                double angle = (2 * Math.PI / segments) * i;
                float vx = (float) (outerRadius * Math.cos(angle));
                float vz = (float) (outerRadius * Math.sin(angle));
                buffer.vertex(poseStack.last().pose(), vx, 0.0F, vz).color(r, g, b, 0.95F * warningPulse).endVertex();
            }
            tesselator.end();

            // ── Inner Line Ring ──
            buffer.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i <= segments; i++) {
                double angle = (2 * Math.PI / segments) * i;
                float vx = (float) (innerRadius * Math.cos(angle));
                float vz = (float) (innerRadius * Math.sin(angle));
                buffer.vertex(poseStack.last().pose(), vx, 0.0F, vz).color(r, g, b, 1.0F * warningPulse).endVertex();
            }
            tesselator.end();

            // ── Crosshair Lines ──
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            buffer.vertex(poseStack.last().pose(), -1.8F, 0.0F, 0.0F).color(r, g, b, 1.0F).endVertex();
            buffer.vertex(poseStack.last().pose(), 1.8F, 0.0F, 0.0F).color(r, g, b, 1.0F).endVertex();
            buffer.vertex(poseStack.last().pose(), 0.0F, 0.0F, -1.8F).color(r, g, b, 1.0F).endVertex();
            buffer.vertex(poseStack.last().pose(), 0.0F, 0.0F, 1.8F).color(r, g, b, 1.0F).endVertex();
            tesselator.end();

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            poseStack.popPose();
        }
    }

    // ── 3. INDICADOR DE MODO EN ESQUINA SUPERIOR DERECHA + METER DISCHARGING EN 10S ────────
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        CompoundTag tag = mc.player.getPersistentData();
        int state = tag.getInt("xebMeteorStrikeState");
        if (state != 2) return;

        boolean isV2 = tag.getBoolean("xebMeteorStrikeIsV2");
        StrikeData strikeData = ACTIVE_METEOR_STRIKES.get(mc.player.getId());
        int targetCount = (strikeData != null) ? strikeData.targetCount : tag.getInt("xebMeteorStrikeTargetCount");

        GuiGraphics g = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        int fillColor = isV2 ? 0xFFFF5500 : 0xFF00E5FF; // Fiery Gold/Orange for v2, Electric Cyan Blue for v1

        // ── A. INDICADOR DE MODO DE CÁMARA (ESQUINA SUPERIOR DERECHA CON I18N) ────────────
        String modeTitle = zoomWide
                ? "§e§l" + net.minecraft.network.chat.Component.translatable("gui.xeb.meteor_strike.mode_wasd").getString()
                : "§b§l" + net.minecraft.network.chat.Component.translatable("gui.xeb.meteor_strike.mode_mouse").getString();
        String modeSub = "§8" + net.minecraft.network.chat.Component.translatable("gui.xeb.meteor_strike.mode_toggle").getString();

        int titleW = mc.font.width(modeTitle);
        int subW = mc.font.width(modeSub);
        int maxW = Math.max(titleW, subW);

        int boxMargin = 12;
        int boxX = width - maxW - boxMargin - 12;
        int boxY = 10;
        int boxW = maxW + 16;
        int boxH = 26;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Fondo semi-transparente elegante para el indicador de modo
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xAA000000);
        g.fill(boxX, boxY, boxX + 2, boxY + boxH, fillColor); // Barra lateral de color de rareza

        g.drawString(mc.font, modeTitle, boxX + 8, boxY + 4, 0xFFFFFFFF, true);
        g.drawString(mc.font, modeSub, boxX + 8, boxY + 15, 0xAAAAAAAA, true);

        // ── B. CENTRO INFERIOR: CONTADOR [ N ] Y METRO DESCARGADOR DE 10S ────────────────
        float progress = Math.max(0.0F, Math.min(1.0F, clientTargetingTicks / 200.0F));

        // 1. Contador de Objetivos [ N ] sobre el cargador
        String countText = "[ " + targetCount + " ]";
        int countW = mc.font.width(countText);
        g.drawString(mc.font, countText, width / 2 - countW / 2, height / 2 + 8, fillColor, true);

        // 2. Cargador inclinado de 4 segmentos (Discharging Gauntlet Meter en 10s)
        int barW = 49;
        int barH = 4;
        int x = width / 2 - barW / 2;
        int y = height / 2 + 20;

        int segmentCount = 4;
        int segmentW = 10;
        int segmentSpacing = 3;

        for (int i = 0; i < segmentCount; i++) {
            float segmentThreshold = (i + 1) / (float) segmentCount;
            int segX = x + i * (segmentW + segmentSpacing);

            org.xeb.xeb.render.DoomfistHUDOverlay.drawSlantedBar(g, segX - 1, y - 1, segmentW + 2, barH + 2, 0xFF000000);
            org.xeb.xeb.render.DoomfistHUDOverlay.drawSlantedBar(g, segX, y, segmentW, barH, 0x88222222);

            if (progress >= segmentThreshold) {
                org.xeb.xeb.render.DoomfistHUDOverlay.drawSlantedBar(g, segX, y, segmentW, barH, fillColor);
            } else if (progress > i / (float) segmentCount) {
                float segmentProgress = (progress - (i / (float) segmentCount)) * segmentCount;
                int partialW = (int) (segmentW * segmentProgress);
                org.xeb.xeb.render.DoomfistHUDOverlay.drawSlantedBar(g, segX, y, partialW, barH, fillColor);
            }
        }

        RenderSystem.disableBlend();
    }
}
