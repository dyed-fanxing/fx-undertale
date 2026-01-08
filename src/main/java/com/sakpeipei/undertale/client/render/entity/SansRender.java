package com.sakpeipei.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.client.model.entity.SansModel;
import com.sakpeipei.undertale.client.render.layer.SansEyesLayer;
import com.sakpeipei.undertale.entity.boss.Sans;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SansRender extends GeoEntityRenderer<Sans> {
    public SansRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SansModel());
        this.renderLayers.addLayer(new SansEyesLayer(this,ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"textures/entity/sans_eyes.png")));
        this.renderLayers.addLayer(new SansSweatLayer(this));
    }

    static class SansSweatLayer extends GeoRenderLayer<Sans> {

        private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
                ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "sans_sweat_3"),
                ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "sans_sweat_2"),
                ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "sans_sweat_1"),
        };
        public SansSweatLayer(GeoRenderer<Sans> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void render(PoseStack poseStack, Sans animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int sweat = animatable.getStamina() / (animatable.getMaxStamina() / 4);
            if(sweat < 3){
                renderType = RenderType.entityCutoutNoCull(TEXTURES[sweat]);
                buffer = bufferSource.getBuffer(renderType);
                super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            }
        }
    }
}
