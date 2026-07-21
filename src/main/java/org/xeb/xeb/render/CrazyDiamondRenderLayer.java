package org.xeb.xeb.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.xeb.xeb.item.ModItems;

public class CrazyDiamondRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public CrazyDiamondRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof Player player)) return;
        if (player.isInvisible()) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean hasMain = mainHand.getItem() == ModItems.BROKEN_DIAMOND.get();
        boolean hasOff = offHand.getItem() == ModItems.BROKEN_DIAMOND.get();

        if (!hasMain && !hasOff) return;

        M model = this.getParentModel();
        if (!(model instanceof HumanoidModel<?> humanoidModel)) return;

        // Render Crazy Diamond head attachment
        poseStack.pushPose();
        humanoidModel.head.translateAndRotate(poseStack);
        
        // Rotate Z by 180 degrees to flip right-side up
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));

        // Apply visual positioning offsets and scaling
        poseStack.translate(org.xeb.xeb.item.CrazyDiamondHeadItem.TX, org.xeb.xeb.item.CrazyDiamondHeadItem.TY, org.xeb.xeb.item.CrazyDiamondHeadItem.TZ);
        float scale = org.xeb.xeb.item.CrazyDiamondHeadItem.SCALE;
        poseStack.scale(scale, scale, scale);

        ItemStack headStack = new ItemStack(ModItems.CRAZY_DIAMOND_HEAD.get());
        ItemStack held = hasMain ? mainHand : offHand;

        // Copy enchant glint if item is enchanted
        if (held.isEnchanted()) {
            headStack.getOrCreateTag().put("Enchantments", held.getOrCreateTag().getList("Enchantments", 10));
        }

        Minecraft.getInstance().getItemRenderer().renderStatic(
                player,
                headStack,
                ItemDisplayContext.HEAD,
                false,
                poseStack,
                buffer,
                player.level(),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                player.getId()
        );
        poseStack.popPose();
    }
}
