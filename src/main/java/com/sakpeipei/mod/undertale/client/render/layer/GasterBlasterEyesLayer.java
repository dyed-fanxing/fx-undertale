package com.sakpeipei.mod.undertale.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * @author Sakqiongzi
 * @since 2025-12-25 22:11
 */
public class GasterBlasterEyesLayer extends AnimatedGlowingLayer<GasterBlasterFixed>{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"textures/entity/gaster_blaster_eyes.png");
    public GasterBlasterEyesLayer(GeoRenderer<GasterBlasterFixed> entityRendererIn) {
        super(entityRendererIn, TEXTURE);
    }

    @Override
    public void render(PoseStack poseStack, GasterBlasterFixed animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if(animatable.tickCount > animatable.getCharge()){
            super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
        }
    }
}
