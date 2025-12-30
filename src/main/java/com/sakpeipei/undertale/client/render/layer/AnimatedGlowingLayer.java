package com.sakpeipei.undertale.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * @author Sakqiongzi
 * @since 2025-12-24 21:18
 */
public class AnimatedGlowingLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {

    private final ResourceLocation texture;
    public AnimatedGlowingLayer(GeoRenderer<T> entityRendererIn, ResourceLocation texture) {
        super(entityRendererIn);
        this.texture = texture;
        Minecraft.getInstance().getTextureManager().register(texture, new AnimatableTexture(texture));
    }
    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        AnimatableTexture.setAndUpdate(texture);
        RenderType glowType = RenderType.entityTranslucentEmissive(texture);
        this.getRenderer().reRender(
                bakedModel, poseStack, bufferSource, animatable,
                glowType, bufferSource.getBuffer(glowType), partialTick,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1
        );
    }


}