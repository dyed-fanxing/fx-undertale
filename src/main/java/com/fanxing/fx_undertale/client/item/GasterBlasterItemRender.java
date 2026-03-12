package com.fanxing.fx_undertale.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.fanxing.fx_undertale.client.model.item.GasterBlasterItemModel;
import com.fanxing.fx_undertale.item.GasterBlasterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import static com.fanxing.fx_undertale.client.entity.summon.GasterBlasterRender.EYES_GLOW;

public class GasterBlasterItemRender extends GeoItemRenderer<GasterBlasterItem> {
    public GasterBlasterItemRender() {
        super(new GasterBlasterItemModel());
        this.addRenderLayer(new GasterBlasterEyesLayer<>(this));
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


    public static class GasterBlasterEyesLayer<T extends Item & GeoAnimatable> extends GeoRenderLayer<T> {
        public GasterBlasterEyesLayer(GeoRenderer<T> entityRendererIn) {
            super(entityRendererIn);
        }
        @Override
        public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,EYES_GLOW, bufferSource.getBuffer(EYES_GLOW), partialTick, LightTexture.FULL_SKY, packedOverlay, -1);
        }
    }
}
