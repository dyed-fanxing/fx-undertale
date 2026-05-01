package com.fanxing.fx_undertale.client.render.entity.summon;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.model.entity.GasterBlasterModel;
import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.fx_undertale.entity.summon.IGasterBlaster;
import com.fanxing.lib.integration.gecklib.client.render.layer.AnimatedGlowingLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

public class GasterBlasterRender extends GeoEntityRenderer<GasterBlaster> {
    public static ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID,"textures/entity/gaster_blaster_eyes.png");
    public static RenderType EYES_GLOW = RenderType.EYES.apply(EYES, RenderStateShard.TRANSLUCENT_TRANSPARENCY);
    public static ResourceLocation SHOT_EYES = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID,"textures/entity/gaster_blaster_shot_eyes.png");


    public GasterBlasterRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GasterBlasterModel());
        this.addRenderLayer(new GasterBlasterEyesLayer<>(this));
    }

    @Override
    public void preRender(PoseStack poseStack, GasterBlaster animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {

        if(!isReRender && !animatable.isMountable()){
            try {
                GasterBlasterBeamRenderer.render(animatable, partialTick, poseStack, bufferSource, packedLight, animatable.color);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    protected void applyRotations(GasterBlaster animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        if(animatable.isDeadOrDying()){
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - rotationYaw));
        }else{
            super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        }
        if (!animatable.isMountable()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
        }
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, GasterBlaster animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getSize(), animatable.getSize(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    public boolean shouldRender(@NotNull GasterBlaster animatable, @NotNull Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // 如果实体本身在视锥内，直接渲染
        if (super.shouldRender(animatable, frustum, cameraX, cameraY, cameraZ)) {
            return true;
        }
        if(frustum.isVisible( new AABB(animatable.getEyePosition(), animatable.getLookAngle().scale(animatable.getLength())))) {
            return true;
        }
        return animatable.position().subtract(cameraX, cameraY, cameraZ).lengthSqr() < 1024;
    }

    @Override
    public boolean shouldShowName(@NotNull GasterBlaster animatable) {
        if(animatable.shouldShowName()){
            return super.shouldShowName(animatable);
        }else{
            return false;
        }
    }

    @Override
    public int getPackedOverlay(GasterBlaster animatable, float u, float partialTick) {
        if (animatable.hurtTime > 0) {
            int hurtDuration = animatable.hurtDuration;
            int overlayU = OverlayTexture.u((float) animatable.hurtTime / hurtDuration);
            return OverlayTexture.pack(overlayU, 10);
        }
        return OverlayTexture.NO_OVERLAY;
    }
    @Override
    protected int getBlockLightLevel(GasterBlaster entity, BlockPos pos) {
        return 15;
    }
    public static class GasterBlasterEyesLayer<T extends Entity & GeoAnimatable & IGasterBlaster> extends AnimatedGlowingLayer<T> {
        public GasterBlasterEyesLayer(GeoRenderer<T> entityRendererIn) {
            super(entityRendererIn, SHOT_EYES);
        }
        @Override
        public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if(animatable.isFire() && !animatable.isMountable()){
                super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            }else{
                this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,EYES_GLOW, bufferSource.getBuffer(EYES_GLOW), partialTick, LightTexture.FULL_SKY, packedOverlay, -1);
            }
        }
    }
}
