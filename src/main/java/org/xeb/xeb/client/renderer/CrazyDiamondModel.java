package org.xeb.xeb.client.renderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.xeb.xeb.entity.CrazyDiamondEntity;

public class CrazyDiamondModel extends HumanoidModel<CrazyDiamondEntity> {
    // Keep track of last frame rotation values to smoothly interpolate with Mth.lerp
    private float lastBodyX = -0.15F;
    private float lastBodyY = 0.0F;
    private float lastBodyZ = 0.0F;

    private float lastRightLegX = 0.0F;
    private float lastRightLegY = 0.0F;
    private float lastRightLegZ = 0.0F;

    private float lastLeftLegX = -0.78F;
    private float lastLeftLegY = 0.0F;
    private float lastLeftLegZ = 0.0F;

    private float lastRightArmX = -0.8F;
    private float lastRightArmY = -0.5F;
    private float lastRightArmZ = -0.3F;

    private float lastLeftArmX = -1.57F;
    private float lastLeftArmY = 0.0F;
    private float lastLeftArmZ = 0.15F;

    public CrazyDiamondModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(CrazyDiamondEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Cancel default biped animations (walking, swing progress, etc.)
        this.leftArm.xRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightArm.xRot = 0.0F;
        this.rightArm.yRot = 0.0F;
        this.rightArm.zRot = 0.0F;
        this.leftLeg.xRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        this.leftLeg.zRot = 0.0F;
        this.rightLeg.xRot = 0.0F;
        this.rightLeg.yRot = 0.0F;
        this.rightLeg.zRot = 0.0F;
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;

        // Head looking directions
        this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        this.head.xRot = headPitch * ((float)Math.PI / 180F);

        int state = entity.getAnimState();

        // 1. Define Default/Passive Pose Targets
        float targetBodyX = -0.15F;
        float targetBodyY = 0.0F;
        float targetBodyZ = 0.0F;

        float targetRightLegX = 0.0F;
        float targetRightLegY = 0.0F;
        float targetRightLegZ = 0.0F;

        float targetLeftLegX = -0.78F;
        float targetLeftLegY = 0.0F;
        float targetLeftLegZ = 0.0F;

        // Right Arm: Defensive position
        float targetRightArmX = -0.8F;
        float targetRightArmY = -0.5F;
        float targetRightArmZ = -0.3F;

        // Left Arm: Extended forward pointing target
        float targetLeftArmX = -1.57F;
        float targetLeftArmY = 0.0F;
        float targetLeftArmZ = 0.15F;

        float lerpFactor = 0.25F;

        // 2. Adjust Targets and Lerp factors based on states
        switch (state) {
            case CrazyDiamondEntity.STATE_PUNCHING: // Click Izquierdo (Jab)
                targetBodyX = 0.52F; // 30 degrees forward
                targetRightArmX = -2.8F; // -160 degrees offensive punch
                targetLeftArmX = 0.5F; // retracted defensive arm
                targetLeftArmZ = 0.2F;
                targetLeftLegX = 0.0F; // stretches
                targetRightLegX = 0.5F; // flexes
                lerpFactor = 0.4F; // snappy strike
                break;
                
            case CrazyDiamondEntity.STATE_CHARGING: // Click Derecho - Charging
                targetBodyX = -0.35F; // arched backward
                targetRightArmX = 2.1F; // 120 degrees back
                targetLeftArmX = 2.1F; // 120 degrees back
                targetRightArmY = 0.0F; targetRightArmZ = 0.0F;
                targetLeftArmY = 0.0F; targetLeftArmZ = 0.0F;
                lerpFactor = 0.2F;
                break;
                
            case CrazyDiamondEntity.STATE_BARRAGE: // Click Derecho - Barrage
                int bTick = entity.tickCount;
                if (bTick % 2 == 0) {
                    targetRightArmX = -2.8F;
                    targetLeftArmX = 2.1F;
                } else {
                    targetRightArmX = 2.1F;
                    targetLeftArmX = -2.8F;
                }
                targetRightArmY = 0.0F; targetRightArmZ = 0.0F;
                targetLeftArmY = 0.0F; targetLeftArmZ = 0.0F;
                targetBodyY = (float) Math.sin(bTick * 0.5D) * 0.035F; // vibration
                lerpFactor = 1.0F; // instant snaps (No Lerp)
                break;
                
            case CrazyDiamondEntity.STATE_EXHAUSTION: // Click Derecho - Exhaustion
                targetBodyX = 0.7F; // collapse body forward
                targetRightArmX = 0.2F; targetRightArmZ = 0.2F;
                targetLeftArmX = 0.2F; targetLeftArmZ = -0.2F;
                targetRightArmY = 0.0F; targetLeftArmY = 0.0F;
                targetRightLegX = 0.0F; targetLeftLegX = 0.0F;
                lerpFactor = 0.15F; // slow fall down
                break;
                
            case CrazyDiamondEntity.STATE_DASHING: // Activa 1 - Dash Phase
                targetBodyX = 0.6F;
                targetRightArmX = -2.8F;
                targetLeftArmX = 0.8F;
                targetRightLegX = -0.6F;
                targetLeftLegX = -0.6F;
                lerpFactor = 0.3F;
                break;
                
            case CrazyDiamondEntity.STATE_LOW_PUNCH: // Activa 1 - Low Punch
                targetBodyX = 0.9F;
                targetRightArmX = 1.2F; // 60-90 degrees down
                targetLeftArmX = 0.5F;
                lerpFactor = 0.35F;
                break;
                
            case CrazyDiamondEntity.STATE_FACE_PUNCH: // Activa 1 - Face Punch
                targetBodyX = -0.1F;
                targetBodyY = -0.5F; // twist
                targetRightArmX = -2.35F; // -120 to -150 degrees
                targetLeftArmX = 0.5F;
                lerpFactor = 0.35F;
                break;
                
            case CrazyDiamondEntity.STATE_KICKING: // Activa 1 - Kick Followup
                targetBodyX = -0.2F;
                targetRightLegX = -1.57F; // Front leg kick forward (-90 deg)
                targetLeftLegX = 0.6F; // Back leg weight flexed
                targetRightArmX = -0.8F; targetRightArmY = -0.5F; targetRightArmZ = -0.3F;
                targetLeftArmX = -1.57F; targetLeftArmZ = 0.15F;
                lerpFactor = 0.4F;
                break;
                
            case CrazyDiamondEntity.STATE_DIGGING: // Activa 2 - Digging
                targetBodyX = 1.0F;
                targetRightArmX = 1.57F;
                targetLeftArmX = 1.57F;
                lerpFactor = 0.25F;
                break;
                
            case CrazyDiamondEntity.STATE_WIND_UP: // Activa 2 - Wind Up
                targetBodyX = -0.3F;
                targetBodyY = 0.5F;
                targetRightArmX = -2.96F; // -170 degrees back
                targetLeftArmX = -1.57F; // aiming arm forward
                lerpFactor = 0.25F;
                break;
                
            case CrazyDiamondEntity.STATE_THROWING: // Activa 2 - Throwing
                targetRightArmX = -1.05F; // -60 degrees intermediate front
                targetLeftArmX = 0.5F;
                lerpFactor = 0.8F; // snappy throw snap
                break;
                
            case CrazyDiamondEntity.STATE_REELING_IN: // Activa 2 - Reeling In
                targetRightArmX = 1.0F;
                targetBodyX = -0.15F;
                lerpFactor = 0.4F;
                break;
        }

        // 3. Interpolate towards targets using Mth.lerp
        float bodyX = Mth.lerp(lerpFactor, lastBodyX, targetBodyX);
        float bodyY = Mth.lerp(lerpFactor, lastBodyY, targetBodyY);
        float bodyZ = Mth.lerp(lerpFactor, lastBodyZ, targetBodyZ);

        float rightLegX = Mth.lerp(lerpFactor, lastRightLegX, targetRightLegX);
        float rightLegY = Mth.lerp(lerpFactor, lastRightLegY, targetRightLegY);
        float rightLegZ = Mth.lerp(lerpFactor, lastRightLegZ, targetRightLegZ);

        float leftLegX = Mth.lerp(lerpFactor, lastLeftLegX, targetLeftLegX);
        float leftLegY = Mth.lerp(lerpFactor, lastLeftLegY, targetLeftLegY);
        float leftLegZ = Mth.lerp(lerpFactor, lastLeftLegZ, targetLeftLegZ);

        float rightArmX = Mth.lerp(lerpFactor, lastRightArmX, targetRightArmX);
        float rightArmY = Mth.lerp(lerpFactor, lastRightArmY, targetRightArmY);
        float rightArmZ = Mth.lerp(lerpFactor, lastRightArmZ, targetRightArmZ);

        float leftArmX = Mth.lerp(lerpFactor, lastLeftArmX, targetLeftArmX);
        float leftArmY = Mth.lerp(lerpFactor, lastLeftArmY, targetLeftArmY);
        float leftArmZ = Mth.lerp(lerpFactor, lastLeftArmZ, targetLeftArmZ);

        // 4. Save calculations for next frame transitions
        lastBodyX = bodyX;
        lastBodyY = bodyY;
        lastBodyZ = bodyZ;

        lastRightLegX = rightLegX;
        lastRightLegY = rightLegY;
        lastRightLegZ = rightLegZ;

        lastLeftLegX = leftLegX;
        lastLeftLegY = leftLegY;
        lastLeftLegZ = leftLegZ;

        lastRightArmX = rightArmX;
        lastRightArmY = rightArmY;
        lastRightArmZ = rightArmZ;

        lastLeftArmX = leftArmX;
        lastLeftArmY = leftArmY;
        lastLeftArmZ = leftArmZ;

        // 5. Apply to model parts
        this.body.xRot = bodyX;
        this.body.yRot = bodyY;
        this.body.zRot = bodyZ;

        this.rightLeg.xRot = rightLegX;
        this.rightLeg.yRot = rightLegY;
        this.rightLeg.zRot = rightLegZ;

        this.leftLeg.xRot = leftLegX;
        this.leftLeg.yRot = leftLegY;
        this.leftLeg.zRot = leftLegZ;

        this.rightArm.xRot = rightArmX;
        this.rightArm.yRot = rightArmY;
        this.rightArm.zRot = rightArmZ;

        this.leftArm.xRot = leftArmX;
        this.leftArm.yRot = leftArmY;
        this.leftArm.zRot = leftArmZ;
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Force 50% opacity/transparency for Stand aesthetic
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, 0.5F);
    }
}
