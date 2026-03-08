package com.sakpeipei.undertale.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.client.model.item.GasterBlasterItemModel;
import com.sakpeipei.undertale.item.GasterBlasterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GasterBlasterItemRender extends GeoItemRenderer<GasterBlasterItem> {
    public GasterBlasterItemRender() {
        super(new GasterBlasterItemModel());
    }

    @Override
    public void defaultRender(PoseStack poseStack, GasterBlasterItem animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType,
                             PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, int packedOverlay) {
        // 只在手持渲染时考虑隐藏
        if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
                transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
                transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ||
                transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {

            Player player = Minecraft.getInstance().player;
            // 如果玩家正在使用，且正在使用的物品栈就是当前要渲染的这个物品栈（引用相等），则隐藏渲染
            if (player != null && player.isUsingItem() && player.getUseItem() == stack) {
                return; // 不渲染
            }
        }
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
