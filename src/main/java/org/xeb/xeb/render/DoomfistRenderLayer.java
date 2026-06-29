package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DoomfistRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Xeb.MODID, "textures/entity/doomfist_gauntlet.png");
    private final ModelPart gauntletPart;
    private final ModelPart shieldAuraPart;

    public DoomfistRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
        
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("main", CubeListBuilder.create()
                .addBox(-2.6F, 3.0F, -2.6F, 5.2F, 9.0F, 5.2F)
                .addBox(-3.4F, 11.0F, -3.4F, 6.8F, 5.5F, 6.8F),
                PartPose.ZERO);
        
        this.gauntletPart = LayerDefinition.create(mesh, 64, 64).bakeRoot();

        MeshDefinition meshAura = new MeshDefinition();
        PartDefinition rootAura = meshAura.getRoot();
        rootAura.addOrReplaceChild("shield", CubeListBuilder.create()
                .addBox(-8.0F, -8.0F, -0.2F, 16.0F, 16.0F, 0.4F),
                PartPose.ZERO);
        this.shieldAuraPart = LayerDefinition.create(meshAura, 64, 64).bakeRoot();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);

        boolean hasMain = mainHand.is(org.xeb.xeb.item.ModItems.DOOMFIST.get()) || mainHand.is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get());
        boolean hasOff = offHand.is(org.xeb.xeb.item.ModItems.DOOMFIST.get()) || offHand.is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get());

        if (!hasMain && !hasOff) return;

        M model = this.getParentModel();

        // Posing the arm raised forward for Ultra Charged fists
        if (entity.getPersistentData().getBoolean("xebUltraCharged") && !entity.getPersistentData().getBoolean("xebPowerBlocking")) {
            if (model instanceof net.minecraft.client.model.HumanoidModel<?> humanoidModel) {
                HumanoidArm mainArm = entity.getMainArm();
                ModelPart chargingArm = (mainArm == HumanoidArm.RIGHT) ? humanoidModel.rightArm : humanoidModel.leftArm;
                
                chargingArm.xRot = -1.2F;
                chargingArm.yRot = (mainArm == HumanoidArm.RIGHT) ? -0.4F : 0.4F;
                chargingArm.zRot = 0.0F;
            }
        }

        // Posing the arm and rendering chest shield aura in third person for Power Block
        if (entity.getPersistentData().getBoolean("xebPowerBlocking")) {
            if (model instanceof net.minecraft.client.model.HumanoidModel<?> humanoidModel) {
                HumanoidArm mainArm = entity.getMainArm();
                ModelPart blockingArm = (mainArm == HumanoidArm.RIGHT) ? humanoidModel.rightArm : humanoidModel.leftArm;
                
                // Cross arm raise and rotate towards center chest
                blockingArm.xRot = -0.8F;
                blockingArm.yRot = (mainArm == HumanoidArm.RIGHT) ? -0.9F : 0.9F;
                blockingArm.zRot = 0.0F;
            }

            // Draw glowing red chest forcefield shield
            poseStack.pushPose();
            if (model instanceof net.minecraft.client.model.HumanoidModel<?> humanoidModel) {
                poseStack.mulPose(com.mojang.math.Axis.YN.rotation(humanoidModel.body.yRot));
                poseStack.mulPose(com.mojang.math.Axis.XN.rotation(humanoidModel.body.xRot));
                poseStack.mulPose(com.mojang.math.Axis.ZN.rotation(humanoidModel.body.zRot));
            }
            poseStack.translate(0.0D, 0.25D, -0.35D);

            float time = (entity.tickCount + partialTick) * 0.05F;
            float u = time * 0.3F;
            float v = time * 0.3F;
            ResourceLocation energyTexture = new ResourceLocation("textures/entity/creeper/creeper_armor.png");
            VertexConsumer auraConsumer = buffer.getBuffer(RenderType.energySwirl(energyTexture, u, v));

            this.shieldAuraPart.render(poseStack, auraConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 0.0F, 0.0F, 0.85F);
            poseStack.popPose();
        }

        if (model instanceof ArmedModel armedModel) {
            if (hasMain) {
                HumanoidArm arm = entity.getMainArm();
                renderGauntletForArm(poseStack, buffer, packedLight, armedModel, arm, entity);
            }
            if (hasOff) {
                HumanoidArm arm = entity.getMainArm().getOpposite();
                renderGauntletForArm(poseStack, buffer, packedLight, armedModel, arm, entity);
            }
        }
    }

    private void renderGauntletForArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ArmedModel armedModel, HumanoidArm arm, T entity) {
        poseStack.pushPose();
        
        armedModel.translateToHand(arm, poseStack);
        
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.gauntletPart.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        float chargeRatio = 0.0F;
        boolean isDashing = entity.getPersistentData().getBoolean("xebDoomfistDashing");
        boolean isUsing = entity.isUsingItem() && (entity.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST.get()) || entity.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get()));
        
        if (isUsing) {
            int ticksCharged = entity.getTicksUsingItem();
            chargeRatio = Math.min(50.0F, ticksCharged) / 50.0F;
        } else if (isDashing) {
            chargeRatio = entity.getPersistentData().getFloat("xebDoomfistChargeRatio");
        }

        boolean isBlocking = entity.getPersistentData().getBoolean("xebPowerBlocking");
        if (chargeRatio > 0.0F || isDashing || isBlocking) {
            poseStack.pushPose();
            poseStack.scale(1.06F, 1.06F, 1.06F);
            
            float time = (entity.tickCount + Minecraft.getInstance().getFrameTime()) * 0.05F;
            float alpha = isBlocking ? 0.85F : (isDashing ? 0.75F : (chargeRatio * 0.45F + 0.1F * (float)Math.sin(time * 10.0F)));
            alpha = Math.max(0.1F, Math.min(0.95F, alpha));
            
            // Check if holding v2 or blocking to apply red color overlay, else blue theme
            ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);
            boolean isV2 = mainHand.is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get()) || offHand.is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get());
            
            float r = (isV2 || isBlocking) ? 1.0F : 0.0F;
            float g = (isV2 || isBlocking) ? 0.0F : (0.4F + chargeRatio * 0.4F);
            float b = (isV2 || isBlocking) ? 0.0F : 1.0F;
            
            ResourceLocation energyTexture = new ResourceLocation("textures/entity/creeper/creeper_armor.png");
            float u = time * 0.5F;
            float v = time * 0.5F;
            
            VertexConsumer auraConsumer = buffer.getBuffer(RenderType.energySwirl(energyTexture, u, v));
            this.gauntletPart.render(poseStack, auraConsumer, 15728880, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
            poseStack.popPose();
         }

          // Render black aura overlay if Ultra Charged
          boolean isUltra = entity.getPersistentData().getBoolean("xebUltraCharged");
          if (isUltra) {
              poseStack.pushPose();
              poseStack.scale(1.06F, 1.06F, 1.06F);
              
              float age = (entity.tickCount + Minecraft.getInstance().getFrameTime());
              // Pulse alpha - entityTranslucent uses alpha blending so dark color actually darkens
              float pulse = 0.6F + 0.2F * (float) Math.sin(age * 0.2F);
              
              ResourceLocation energyTexture = new ResourceLocation("textures/entity/creeper/creeper_armor.png");
              // Use entityTranslucent (alpha-blend) WITHOUT rotation so model shape stays correct
              VertexConsumer auraConsumer = buffer.getBuffer(RenderType.entityTranslucent(energyTexture));
              this.gauntletPart.render(poseStack, auraConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0.0F, 0.0F, 0.0F, pulse);
              poseStack.popPose();
          }

         poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        boolean isV1 = event.getItemStack().is(org.xeb.xeb.item.ModItems.DOOMFIST.get());
        boolean isV2 = event.getItemStack().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get());
        
        if (isV1 || isV2) {
            event.setCanceled(true);

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();

            boolean isRightHand = event.getHand() == InteractionHand.MAIN_HAND == (Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT);
            
            net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
            boolean isBlocking = player != null && player.getPersistentData().getBoolean("xebPowerBlocking");
            boolean isUltra = player != null && player.getPersistentData().getBoolean("xebUltraCharged");

            if (isBlocking) {
                // Positioned flat across the chest horizontally (pointing left/right depending on hand) raised further above the hotbar level
                poseStack.translate(isRightHand ? 0.1D : -0.1D, -0.2D, -0.35D);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-20.0F));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(isRightHand ? 25.0F : -25.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(isRightHand ? 80.0F : -80.0F));
            } else {
                if (isRightHand) {
                    poseStack.translate(0.55D, -0.65D, -0.7D);
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-45.0F));
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(15.0F));
                } else {
                    poseStack.translate(-0.55D, -0.65D, -0.7D);
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45.0F));
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(15.0F));
                }
            }

            poseStack.scale(0.65F, 0.65F, 0.65F);

            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("main", CubeListBuilder.create()
                    .addBox(-2.5F, -6.0F, -2.5F, 5.0F, 10.0F, 5.0F)
                    .addBox(-3.0F, 3.5F, -3.0F, 6.0F, 5.0F, 6.0F),
                    PartPose.ZERO);
            root.addOrReplaceChild("shield", CubeListBuilder.create()
                    .addBox(-8.0F, -6.0F, -4.5F, 16.0F, 12.0F, 0.1F),
                    PartPose.ZERO);
            ModelPart firstPersonPart = LayerDefinition.create(mesh, 64, 64).bakeRoot();

            ModelPart mainPart = firstPersonPart.getChild("main");
            VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            mainPart.render(poseStack, consumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            if (player != null) {
                float chargeRatio = 0.0F;
                boolean isDashing = player.getPersistentData().getBoolean("xebDoomfistDashing");
                boolean isUsing = player.isUsingItem() && (player.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST.get()) || player.getUseItem().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get()));
                
                if (isUsing) {
                    int ticksCharged = player.getTicksUsingItem();
                    chargeRatio = Math.min(50.0F, ticksCharged) / 50.0F;
                } else if (isDashing) {
                    chargeRatio = player.getPersistentData().getFloat("xebDoomfistChargeRatio");
                }

                if (chargeRatio > 0.0F || isDashing || isBlocking) {
                    poseStack.pushPose();
                    poseStack.scale(1.06F, 1.06F, 1.06F);

                    float time = (player.tickCount + event.getPartialTick()) * 0.05F;
                    float alpha = isBlocking ? 0.85F : (isDashing ? 0.75F : (chargeRatio * 0.45F + 0.1F * (float)Math.sin(time * 10.0F)));
                    alpha = Math.max(0.1F, Math.min(0.95F, alpha));

                    float r = (isV2 || isBlocking) ? 1.0F : 0.0F;
                    float g = (isV2 || isBlocking) ? 0.0F : (0.4F + chargeRatio * 0.4F);
                    float b = (isV2 || isBlocking) ? 0.0F : 1.0F;

                    ResourceLocation energyTexture = new ResourceLocation("textures/entity/creeper/creeper_armor.png");
                    float u = time * 0.5F;
                    float v = time * 0.5F;

                    VertexConsumer auraConsumer = event.getMultiBufferSource().getBuffer(RenderType.energySwirl(energyTexture, u, v));
                    mainPart.render(poseStack, auraConsumer, 15728880, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
                    
                    if (isBlocking) {
                        ModelPart shieldPart = firstPersonPart.getChild("shield");
                        shieldPart.render(poseStack, auraConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 0.0F, 0.0F, 0.85F);
                    }
                    poseStack.popPose();
                }

                if (isUltra) {
                    poseStack.pushPose();
                    poseStack.scale(1.06F, 1.06F, 1.06F);

                    float ageUltra = (player.tickCount + event.getPartialTick());
                    float pulseUltra = 0.6F + 0.2F * (float) Math.sin(ageUltra * 0.2F);

                    ResourceLocation ultraTexture = new ResourceLocation("textures/entity/creeper/creeper_armor.png");
                    VertexConsumer ultraConsumer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(ultraTexture));
                    mainPart.render(poseStack, ultraConsumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0.0F, 0.0F, 0.0F, pulseUltra);
                    poseStack.popPose();
                }
            }

            poseStack.popPose();
        }
    }
}
