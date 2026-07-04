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
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.client.DoomfistRenderContext;

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

        // Model rendering and hand-rendering events are now fully delegated to GeckoLib's GeoItemRenderer!
    }

    // ── Render context tracking ──────────────────────────────────────────────
    // GeckoLib's GeoItem animation controllers run inside the BEWLR pipeline, where
    // DataTickets.ENTITY is null. We capture the entity being rendered around the call so the
    // Doomfist items can resolve their wielder and pick the correct animation.

    /** Third-person (and other players / mobs): track the living entity around its render. */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        DoomfistRenderContext.setCurrentEntity(event.getEntity());
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        DoomfistRenderContext.clearCurrentEntity();
    }

    /**
     * First-person hand render: RenderLivingEvent does NOT fire for the local player's own hand,
     * so we track the local player here. The hand render nests inside this event.
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            DoomfistRenderContext.setCurrentEntity(mc.player);
        }
    }
}
