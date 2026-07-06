package org.xeb.xeb.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.xeb.xeb.entity.RestoreProjectileEntity;

public class RestoreProjectileRenderer extends EntityRenderer<RestoreProjectileEntity> {
    public RestoreProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RestoreProjectileEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockState state = entity.getBlockState();
        if (state != null) {
            poseStack.pushPose();
            poseStack.translate(-0.5D, 0.0D, -0.5D);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY, null);
            poseStack.popPose();
        }
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RestoreProjectileEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
