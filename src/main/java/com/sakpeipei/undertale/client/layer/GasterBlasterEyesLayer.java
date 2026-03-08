package com.sakpeipei.undertale.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.entity.summon.IGasterBlaster;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * @author Sakqiongzi
 * @since 2025-12-25 22:11
 */
public class GasterBlasterEyesLayer<T extends Entity & GeoAnimatable & IGasterBlaster> extends AnimatedGlowingLayer<T>{
    public GasterBlasterEyesLayer(GeoRenderer<T> entityRendererIn, ResourceLocation texture) {
        super(entityRendererIn, texture);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if(animatable.isFire()){
            super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
        }
    }
}
