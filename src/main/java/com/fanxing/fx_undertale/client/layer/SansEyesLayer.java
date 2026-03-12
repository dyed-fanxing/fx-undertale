package com.fanxing.fx_undertale.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * Sans 审判眼层
 * @author Sakpeipei
 * @since 2025/12/31 10:30
 */
public class SansEyesLayer extends AnimatedGlowingLayer<Sans>{
    public SansEyesLayer(GeoRenderer<Sans> entityRendererIn, ResourceLocation texture) {
        super(entityRendererIn, texture);
    }

    @Override
    public void render(PoseStack poseStack, Sans animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if(animatable.getIsEyeBlink()){
            super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
        }
    }
}
