package org.xeb.xeb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GoldenFlowerClientHandler {
    private static final ResourceLocation WHITE_TEX = new ResourceLocation(Xeb.MODID, "textures/entity/white.png");
    private static final java.util.Map<UUID, Deque<TrailSnapshot>> afterimagesMap = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        java.util.Set<UUID> activeUuids = new java.util.HashSet<>();
        for (Player p : mc.level.players()) {
            boolean holdsFlower = p.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.GOLDEN_FLOWER.get())
                    || p.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.GOLDEN_FLOWER.get());
            if (holdsFlower) {
                UUID uuid = p.getUUID();
                activeUuids.add(uuid);
                boolean isMoving = p.walkAnimation.speed() > 0.02F || p.getDeltaMovement().horizontalDistanceSqr() > 0.001D;
                Deque<TrailSnapshot> trail = afterimagesMap.computeIfAbsent(uuid, k -> new ArrayDeque<>());
                if (isMoving) {
                    trail.addLast(new TrailSnapshot(
                            p.position(),
                            p.getYRot(),
                            p.getXRot(),
                            p.yBodyRot,
                            p.walkAnimation.position()
                    ));
                    if (trail.size() > 15) {
                        trail.removeFirst();
                    }
                } else {
                    if (!trail.isEmpty()) {
                        trail.removeFirst();
                    }
                }
            }
        }
        afterimagesMap.keySet().retainAll(activeUuids);

        // Tick actives
        PhantomAttackManager.tick();
        JaronaAttack.tick();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        LocalPlayer player = mc.player;
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        float partialTicks = event.getPartialTick();

        // 1. RENDER WALK AFTERIMAGES (Right-side up & correct player skin texture) (N20)
        for (Player p : mc.level.players()) {
            UUID uuid = p.getUUID();
            Deque<TrailSnapshot> trail = afterimagesMap.get(uuid);
            if (trail != null && !trail.isEmpty()) {
                boolean isLocal = uuid.equals(player.getUUID());
                if (isLocal && mc.options.getCameraType().isFirstPerson()) {
                    continue;
                }

                EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
                net.minecraft.client.renderer.entity.EntityRenderer<?> untypedRenderer = dispatcher.getRenderer(p);
                if (untypedRenderer instanceof PlayerRenderer renderer) {
                    PlayerModel<?> model = renderer.getModel();

                    // Store current model configurations to restore later
                    float prevRightLegX = model.rightLeg.xRot;
                    float prevLeftLegX = model.leftLeg.xRot;
                    float prevRightArmX = model.rightArm.xRot;
                    float prevLeftArmX = model.leftArm.xRot;
                    float prevBodyX = model.body.xRot;
                    boolean prevYoung = model.young;

                    int index = 0;
                    int total = trail.size();
                    int light = dispatcher.getRenderer(p).getPackedLightCoords(p, partialTicks);
                    ResourceLocation skin = (p instanceof net.minecraft.client.player.AbstractClientPlayer cp) ? cp.getSkinTextureLocation() : WHITE_TEX;
                    RenderType renderType = RenderType.entityTranslucentCull(skin);

                    model.young = false; // Always false, no baby shape

                    for (TrailSnapshot snapshot : trail) {
                        poseStack.pushPose();

                        // Position relative to camera in world coordinates
                        poseStack.translate(snapshot.pos.x - camPos.x, snapshot.pos.y - camPos.y, snapshot.pos.z - camPos.z);

                        // Yaw rotation
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - snapshot.bodyYaw));

                        // Standard scale/translate to render model right-side up
                        poseStack.scale(-1.0F, -1.0F, 1.0F);
                        poseStack.translate(0.0F, -1.501F, 0.0F);

                        // Force walk animation posture on the model
                        ((PlayerModel) model).setupAnim(p, snapshot.walkAnimPosition, 0.8F, 0.0F, 0.0F, 0.0F);

                        // Color: Blue trail if Jarona is active, otherwise rainbow HSB
                        float r, g, b;
                        boolean isJarona = JaronaAttack.getKick(p) != null;
                        if (isJarona) {
                            r = 0.1F; g = 0.4F; b = 1.0F; // blue
                        } else {
                            float progress = (float) index / total;
                            int colorRGB = java.awt.Color.HSBtoRGB(progress, 1.0F, 1.0F);
                            r = ((colorRGB >> 16) & 0xFF) / 255.0F;
                            g = ((colorRGB >> 8) & 0xFF) / 255.0F;
                            b = (colorRGB & 0xFF) / 255.0F;
                        }

                        // Fading alpha
                        float alpha = Math.max(0.05F, 1.0F - ((float) index / total)) * 0.35F;

                        // Bind player skin texture to avoid deformed checkerboards
                        VertexConsumer consumer = bufferSource.getBuffer(renderType);
                        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, r, g, b, alpha);

                        poseStack.popPose();
                        index++;
                    }

                    // Restore model configuration
                    model.young = prevYoung;
                    model.rightLeg.xRot = prevRightLegX;
                    model.leftLeg.xRot = prevLeftLegX;
                    model.rightArm.xRot = prevRightArmX;
                    model.leftArm.xRot = prevLeftArmX;
                    model.body.xRot = prevBodyX;
                }
            }
        }

        // 1.5. RENDER LOADED FLOWERS AS HALO BEHIND PLAYER
        for (Player p : mc.level.players()) {
            boolean holdsFlower = p.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.GOLDEN_FLOWER.get())
                    || p.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.GOLDEN_FLOWER.get());
            int loaded = p.getPersistentData().getInt("xebGoldenFlowerLoadedCount");

            boolean isLocal = p.getUUID().equals(mc.player.getUUID());
            boolean skipLocalFP = isLocal && mc.options.getCameraType().isFirstPerson();

            if (holdsFlower && loaded > 0 && !skipLocalFP) {
                RenderType renderType = RenderType.entityTranslucent(WHITE_TEX);
                VertexConsumer consumer = bufferSource.getBuffer(renderType);
                float spinAngle = (p.tickCount + partialTicks) * 4.0F;

                // Interpolated body yaw and position
                float bodyYaw = Mth.lerp(partialTicks, p.yBodyRotO, p.yBodyRot);
                double px = Mth.lerp(partialTicks, p.xo, p.getX());
                double py = Mth.lerp(partialTicks, p.yo, p.getY());
                double pz = Mth.lerp(partialTicks, p.zo, p.getZ());

                int packedLight = 15728880;

                for (int i = 0; i < loaded; i++) {
                    poseStack.pushPose();

                    // Halo vertical en la espalda del player
                    float yawRad = (float) Math.toRadians(bodyYaw);
                    double backX = Math.sin(yawRad) * 0.40D;
                    double backZ = -Math.cos(yawRad) * 0.40D;
                    double backY = 1.05D; // A la altura de la espalda/pecho

                    // Ejes local derecho (rx, ry, rz) y local arriba (ux, uy, uz)
                    double rx = Math.cos(yawRad);
                    double rz = Math.sin(yawRad);
                    double uy = 1.0D;

                    double haloAngle = (2.0D * Math.PI / Math.max(1, loaded)) * i;
                    double spinOffset = (p.tickCount + partialTicks) * 0.12D; // Smooth spin rotation
                    double finalAngle = haloAngle + spinOffset;

                    double ringRadius = 0.65D; // Hermoso tamaño de halo vertical
                    double dx = backX + rx * Math.cos(finalAngle) * ringRadius;
                    double dy = backY + uy * Math.sin(finalAngle) * ringRadius;
                    double dz = backZ + rz * Math.cos(finalAngle) * ringRadius;

                    Vec3 renderPos = new Vec3(px + dx, py + dy, pz + dz);

                    poseStack.translate(renderPos.x - camPos.x, renderPos.y - camPos.y, renderPos.z - camPos.z);

                    // Billboard to face camera
                    poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

                    // Spin individual de la flor
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(spinAngle));

                    Matrix3f normalMatrix = poseStack.last().normal();

                    // Colores de las 6 flores
                    float r = 1.0F, g = 1.0F, b = 1.0F;
                    switch (i % 6) {
                        case 0 -> { r = 0.0F; g = 1.0F; b = 1.0F; }   // Cyan
                        case 1 -> { r = 0.6F; g = 0.1F; b = 0.9F; }   // Purple
                        case 2 -> { r = 1.0F; g = 0.6F; b = 0.0F; }   // Orange
                        case 3 -> { r = 0.0F; g = 1.0F; b = 0.0F; }   // Green
                        case 4 -> { r = 0.1F; g = 0.1F; b = 1.0F; }   // Blue
                        case 5 -> { r = 1.0F; g = 1.0F; b = 0.0F; }   // Yellow
                    }

                    // === RENDER FLORES EN 3D SENCILLO Y BONITO (1.5 bloques de escala) ===
                    // 1. Cáliz / Soporte trasero de la flor (verde oscuro)
                    poseStack.pushPose();
                    poseStack.translate(0.0F, 0.0F, 0.04F);
                    drawFlowerQuadTextured(consumer, poseStack.last().pose(), normalMatrix, 0.0F, 0.0F, 0.28F, r * 0.1F, g * 0.5F + 0.1F, b * 0.1F, 0.8F, packedLight);
                    poseStack.popPose();
                    // 2. Anillo de 5 Pétalos (inclinados hacia afuera)
                    for (int pIndex = 0; pIndex < 5; pIndex++) {
                        poseStack.pushPose();
                        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(pIndex * 72.0F));
                        poseStack.translate(0.0F, 0.22F, 0.01F); // Desplazamiento radial hacia afuera
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(15.0F)); // Inclinación
                        drawFlowerQuadTextured(consumer, poseStack.last().pose(), normalMatrix, 0.0F, 0.0F, 0.32F, r, g, b, 0.9F, packedLight);
                        poseStack.popPose();
                    }

                    // 4. Centro Brillante Extruido y Aura de Brillo
                    poseStack.pushPose();
                    poseStack.translate(0.0F, 0.0F, -0.04F); // Extruido hacia adelante
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-spinAngle * 0.6F)); // Giro lento inverso del núcleo
                    // Núcleo blanco brillante
                    drawFlowerQuadTextured(consumer, poseStack.last().pose(), normalMatrix, 0.0F, 0.0F, 0.22F, 1.0F, 1.0F, 1.0F, 0.95F, packedLight);
                    // Brillo/Glow pulsante alrededor del centro
                    float glowPulse = 0.35F + 0.1F * (float) Math.sin((p.tickCount + partialTicks) * 0.2F);
                    drawFlowerQuadTextured(consumer, poseStack.last().pose(), normalMatrix, 0.0F, 0.0F, 0.45F, r, g, b, glowPulse, packedLight);
                    poseStack.popPose();

                    poseStack.popPose();
                }
            }
        }

        // 2. RENDER FLOWER DANCE PHANTOM CLONES (Right-side up & correct player skin texture)
        if (!PhantomAttackManager.activeClones.isEmpty()) {
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            
            // Retrieve caster player (N20)
            PhantomClone firstClone = PhantomAttackManager.activeClones.get(0);
            Player caster = player; // default fallback
            if (mc.level != null) {
                net.minecraft.world.entity.Entity casterEnt = mc.level.getEntity(firstClone.casterId);
                if (casterEnt instanceof Player p) {
                    caster = p;
                }
            }

            net.minecraft.client.renderer.entity.EntityRenderer<?> untypedRenderer = dispatcher.getRenderer(caster);
            if (untypedRenderer instanceof PlayerRenderer renderer) {
                PlayerModel<?> model = renderer.getModel();

                // Store angles
                float prevRightLegX = model.rightLeg.xRot;
                float prevLeftLegX = model.leftLeg.xRot;
                float prevRightArmX = model.rightArm.xRot;
                float prevLeftArmX = model.leftArm.xRot;
                float prevRightArmZ = model.rightArm.zRot;
                float prevBodyX = model.body.xRot;
                boolean prevYoung = model.young;

                int light = dispatcher.getRenderer(caster).getPackedLightCoords(caster, partialTicks);
                ResourceLocation skin = (caster instanceof net.minecraft.client.player.AbstractClientPlayer cp) ? cp.getSkinTextureLocation() : WHITE_TEX;
                RenderType renderType = RenderType.entityTranslucentCull(skin);

                model.young = false; // Always false, no baby shape

                for (PhantomClone clone : PhantomAttackManager.activeClones) {
                    if (clone.state == PhantomClone.CloneState.RETURNED) continue;

                    Vec3 renderPos = clone.getOrbitPosition(PhantomAttackManager.ticksLived);
                    boolean isStriking = false;

                    if (clone.state == PhantomClone.CloneState.STRIKING) {
                        isStriking = true;
                        float progress = Math.min(1.0F, clone.attackTimer / 6.0F);
                        renderPos = new Vec3(
                                Mth.lerp(progress, renderPos.x, clone.targetPos.x),
                                Mth.lerp(progress, renderPos.y, clone.targetPos.y),
                                Mth.lerp(progress, renderPos.z, clone.targetPos.z)
                        );
                    } else if (clone.state == PhantomClone.CloneState.RETURNING) {
                        float progress = Math.min(1.0F, (float) clone.returnTimer / 12.0F);
                        // Interpolated linear target
                        Vec3 lerpedPos = new Vec3(
                                Mth.lerp(progress, clone.targetPos.x, caster.getX()),
                                Mth.lerp(progress, clone.targetPos.y, caster.getY()),
                                Mth.lerp(progress, clone.targetPos.z, caster.getZ())
                        );

                        // Curve dispersion path outwards based on index if the clone hit the target
                        Vec3 dirToPlayer = caster.position().subtract(clone.targetPos);
                        Vec3 rightVec = new Vec3(-dirToPlayer.z, 0.0D, dirToPlayer.x);
                        if (rightVec.lengthSqr() > 0.001D) {
                            rightVec = rightVec.normalize();
                        }
                        double curveOffset = 0.0D;
                        if (clone.hasStruck) {
                            double curveDirection = (clone.index % 2 == 0) ? 1.0D : -1.0D;
                            double curveSpread = 1.0D + (clone.index * 0.5D);
                            curveOffset = Math.sin(progress * Math.PI) * curveDirection * curveSpread;
                        }
                        
                        renderPos = lerpedPos.add(rightVec.scale(curveOffset));
                    }

                    poseStack.pushPose();
                    poseStack.translate(renderPos.x - camPos.x, renderPos.y - camPos.y, renderPos.z - camPos.z);

                    // Face target or player (Add 180 degrees so they face inwards!)
                    float yawToTarget;
                    if (clone.state == PhantomClone.CloneState.RETURNING) {
                        yawToTarget = (float) Math.toDegrees(Math.atan2(caster.getX() - renderPos.x, caster.getZ() - renderPos.z)) + 180.0F;
                    } else {
                        yawToTarget = (float) Math.toDegrees(Math.atan2(clone.targetPos.x - renderPos.x, clone.targetPos.z - renderPos.z)) + 180.0F;
                    }
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yawToTarget));

                    // Standard scale/translate to render model right-side up
                    poseStack.scale(-1.0F, -1.0F, 1.0F);
                    poseStack.translate(0.0F, -1.501F, 0.0F);

                    // Pose manipulation
                    if (clone.state == PhantomClone.CloneState.RETURNING) {
                        float walkPos = (PhantomAttackManager.ticksLived + partialTicks) * 0.4F;
                        ((PlayerModel) model).setupAnim(caster, walkPos, 0.8F, 0.0F, 0.0F, 0.0F);
                    } else if (isStriking && clone.attackTimer >= 4 && clone.attackTimer <= 11) {
                        int strikeIndex = clone.index % 5;
                        
                        // Default limbs before special moves
                        model.rightArm.xRot = (float) Math.toRadians(-45);
                        model.leftArm.xRot = (float) Math.toRadians(-45);
                        model.rightArm.yRot = 0.0F;
                        model.leftArm.yRot = 0.0F;
                        model.rightArm.zRot = 0.0F;
                        model.leftArm.zRot = 0.0F;
                        model.rightLeg.xRot = 0.0F;
                        model.leftLeg.xRot = 0.0F;
                        model.body.xRot = 0.0F;
                        model.body.yRot = 0.0F;
                        model.body.zRot = 0.0F;

                        switch (strikeIndex) {
                            case 0 -> { // Uppercut punch
                                model.rightArm.xRot = (float) Math.toRadians(-130);
                                model.rightArm.yRot = (float) Math.toRadians(-20);
                                model.leftArm.xRot = (float) Math.toRadians(40);
                                model.body.xRot = (float) Math.toRadians(-10);
                            }
                            case 1 -> { // Straight jab punch
                                model.leftArm.xRot = (float) Math.toRadians(-100);
                                model.leftArm.yRot = (float) Math.toRadians(10);
                                model.rightArm.xRot = (float) Math.toRadians(30);
                                model.body.yRot = (float) Math.toRadians(15);
                            }
                            case 2 -> { // Double Palm Slam
                                model.rightArm.xRot = (float) Math.toRadians(-95);
                                model.leftArm.xRot = (float) Math.toRadians(-95);
                                model.body.xRot = (float) Math.toRadians(15);
                            }
                            case 3 -> { // High Kick
                                model.rightLeg.xRot = (float) Math.toRadians(-90);
                                model.leftLeg.xRot = (float) Math.toRadians(20);
                                model.body.xRot = (float) Math.toRadians(-15);
                                model.rightArm.zRot = (float) Math.toRadians(35);
                                model.leftArm.zRot = (float) Math.toRadians(-35);
                            }
                            case 4 -> { // Axe Kick / Slam
                                model.leftLeg.xRot = (float) Math.toRadians(-105);
                                model.rightLeg.xRot = (float) Math.toRadians(15);
                                model.rightArm.xRot = (float) Math.toRadians(-140);
                                model.leftArm.xRot = (float) Math.toRadians(-140);
                                model.body.xRot = (float) Math.toRadians(20);
                            }
                        }
                    } else {
                        // Fight stance
                        model.rightArm.xRot = (float) Math.toRadians(-45);
                        model.leftArm.xRot = (float) Math.toRadians(-45);
                        model.rightArm.zRot = (float) Math.toRadians(-10);
                        model.body.xRot = 0.0F;
                    }

                    // Copy rotations to outer layer (sleeves, pants, jacket) for PlayerModel
                    PlayerModel pm = (PlayerModel) model;
                    pm.rightSleeve.copyFrom(pm.rightArm);
                    pm.leftSleeve.copyFrom(pm.leftArm);
                    pm.rightPants.copyFrom(pm.rightLeg);
                    pm.leftPants.copyFrom(pm.leftLeg);
                    pm.jacket.copyFrom(pm.body);

                    // Rainbow HSB colors
                    int colorRGB = java.awt.Color.HSBtoRGB(clone.hue, 1.0F, 1.0F);
                    float r = ((colorRGB >> 16) & 0xFF) / 255.0F;
                    float g = ((colorRGB >> 8) & 0xFF) / 255.0F;
                    float b = (colorRGB & 0xFF) / 255.0F;
                    float alpha = 0.65F;

                    // Bind skin texture to prevent checkerboards
                    VertexConsumer consumer = bufferSource.getBuffer(renderType);
                    model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, r, g, b, alpha);

                    poseStack.popPose();
                }

                // Restore
                model.young = prevYoung;
                model.rightLeg.xRot = prevRightLegX;
                model.leftLeg.xRot = prevLeftLegX;
                model.rightArm.xRot = prevRightArmX;
                model.leftArm.xRot = prevLeftArmX;
                model.rightArm.zRot = prevRightArmZ;
                model.body.xRot = prevBodyX;
            }
        }

        // 3. RENDER JARONA DASH CLONES (Right-side up & correct skin texture)
        JaronaAttack.ActiveKick kick = JaronaAttack.getKick(player);
        if (kick != null && !mc.options.getCameraType().isFirstPerson()) {
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            net.minecraft.client.renderer.entity.EntityRenderer<?> untypedRenderer = dispatcher.getRenderer(player);
            if (untypedRenderer instanceof PlayerRenderer renderer) {
                PlayerModel<?> model = renderer.getModel();

                // Store angles
                float prevRightLegX = model.rightLeg.xRot;
                float prevLeftLegX = model.leftLeg.xRot;
                float prevRightArmX = model.rightArm.xRot;
                float prevLeftArmX = model.leftArm.xRot;
                float prevBodyX = model.body.xRot;

                Vec3 currentPos = player.position();
                Vec3 movement = player.getDeltaMovement();
                int light = dispatcher.getRenderer(player).getPackedLightCoords(player, partialTicks);
                ResourceLocation skin = ((net.minecraft.client.player.AbstractClientPlayer) player).getSkinTextureLocation();
                RenderType renderType = RenderType.entityTranslucentCull(skin);

                // 3 shades of blue afterimage trails
                float[][] blues = {
                        {0.0F, 0.0F, 0.8F, 0.65F}, // Close
                        {0.2F, 0.2F, 0.9F, 0.40F}, // Mid
                        {0.5F, 0.5F, 1.0F, 0.20F}  // Far
                };

                for (int i = 0; i < 3; i++) {
                    poseStack.pushPose();

                    float delay = (i + 1) * 1.4F;
                    Vec3 clonePos = currentPos.subtract(movement.x * delay, movement.y * delay, movement.z * delay);

                    // Position relative to camera
                    poseStack.translate(clonePos.x - camPos.x, clonePos.y - camPos.y, clonePos.z - camPos.z);

                    // Yaw rotation
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - player.getYRot()));

                    // Standard scale/translate to render model right-side up
                    poseStack.scale(-1.0F, -1.0F, 1.0F);
                    poseStack.translate(0.0F, -1.501F, 0.0F);

                    // Less exaggerated poses for farther clones
                    float cloneStepModifier = 1.0F - (i * 0.2F);
                    model.rightLeg.xRot = (float) Math.toRadians(-120 * kick.comboStep * 0.8F * cloneStepModifier);
                    model.leftLeg.xRot = (float) Math.toRadians(45 * kick.comboStep * 0.8F * cloneStepModifier);
                    model.body.xRot = (float) Math.toRadians(25 * kick.comboStep * 0.8F * cloneStepModifier);
                    model.rightArm.xRot = (float) Math.toRadians(60 * kick.comboStep * 0.8F * cloneStepModifier);
                    model.leftArm.xRot = (float) Math.toRadians(60 * kick.comboStep * 0.8F * cloneStepModifier);

                    // Copy rotations to outer layer (sleeves, pants, jacket) for PlayerModel
                    PlayerModel pm = (PlayerModel) model;
                    pm.rightSleeve.copyFrom(pm.rightArm);
                    pm.leftSleeve.copyFrom(pm.leftArm);
                    pm.rightPants.copyFrom(pm.rightLeg);
                    pm.leftPants.copyFrom(pm.leftLeg);
                    pm.jacket.copyFrom(pm.body);

                    float r = blues[i][0];
                    float g = blues[i][1];
                    float b = blues[i][2];
                    float a = blues[i][3];

                    // Bind skin texture to prevent checkerboards
                    VertexConsumer consumer = bufferSource.getBuffer(renderType);
                    model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, r, g, b, a);

                    poseStack.popPose();
                }

                // Restore
                model.rightLeg.xRot = prevRightLegX;
                model.leftLeg.xRot = prevLeftLegX;
                model.rightArm.xRot = prevRightArmX;
                model.leftArm.xRot = prevLeftArmX;
                model.body.xRot = prevBodyX;
            }
        }
    }

    @SubscribeEvent
    public static void renderJaronaPose(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        JaronaAttack.ActiveKick kick = JaronaAttack.getKick(player);
        if (kick != null) {
            player.walkAnimation.setSpeed(0.0F);
        }
    }

    @SubscribeEvent
    public static void renderJaronaClones(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        JaronaAttack.ActiveKick kick = JaronaAttack.getKick(player);
        if (kick != null) {
            player.walkAnimation.setSpeed(player.getSpeed());
        }
    }

    private static void drawFlowerQuadTextured(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, float cx, float cy, float size,
                                               float r, float g, float b, float a, int light) {
        consumer.vertex(matrix, cx - size, cy - size, 0.0F).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, cx + size, cy - size, 0.0F).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, cx + size, cy + size, 0.0F).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, cx - size, cy + size, 0.0F).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).endVertex();
    }

    private static class TrailSnapshot {
        public final Vec3 pos;
        public final float yRot;
        public final float xRot;
        public final float bodyYaw;
        public final float walkAnimPosition;

        public TrailSnapshot(Vec3 pos, float yRot, float xRot, float bodyYaw, float walkAnimPosition) {
            this.pos = pos;
            this.yRot = yRot;
            this.xRot = xRot;
            this.bodyYaw = bodyYaw;
            this.walkAnimPosition = walkAnimPosition;
        }
    }
}
