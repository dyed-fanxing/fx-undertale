package com.fanxing.fx_undertale.client.render.entity.summon;

import com.fanxing.fx_undertale.client.model.entity.DisplayBoneModel;
import com.fanxing.fx_undertale.common.RenderTypes;
import com.fanxing.fx_undertale.entity.summon.DisplayBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

import java.util.Optional;

/**
 * @author FanXing
 * @since 2025-08-18 20:58
 */
public class DisplayBoneRenderer extends GeoEntityRenderer<DisplayBone> {

    private static final Logger log = LoggerFactory.getLogger(DisplayBoneRenderer.class);

    public DisplayBoneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DisplayBoneModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, DisplayBone animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(widthScale*animatable.getScaleProgress(partialTick), heightScale*animatable.getScaleProgress(partialTick),poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected void applyRotations(DisplayBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

    @Override
    public void renderFinal(PoseStack poseStack, DisplayBone animatable, BakedGeoModel model, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int colour) {
        Optional<GeoBone> down = this.getGeoModel().getBone("down");
        Optional<GeoBone> top = this.getGeoModel().getBone("top");
        float currentTime = animatable.tickCount + partialTick;
        RenderType trailType = RenderTypes.ENERGY_TRIANGLE_FAN_WHITE;
        if (down.isPresent()) {
            Vector3d localPos = down.get().getLocalPosition();
            animatable.trail1.addPoint(localPos, currentTime);
            VertexConsumer consumer = bufferSource.getBuffer(trailType);
            animatable.trail1.render(poseStack, (MultiBufferSource.BufferSource) bufferSource, consumer, packedLight, currentTime);
        }
        if(top.isPresent()) {
            Vector3d localPos = top.get().getLocalPosition();
            animatable.trail2.addPoint(localPos, currentTime);
            VertexConsumer consumer = bufferSource.getBuffer(trailType);
            animatable.trail2.render(poseStack,(MultiBufferSource.BufferSource) bufferSource, consumer, packedLight, currentTime);
        }
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public Color getRenderColor(DisplayBone animatable, float partialTick, int packedLight) {
        Color original = super.getRenderColor(animatable, partialTick, packedLight);
        return Color.ofARGB((int)(original.getAlpha() * 0.75f),original.getRed(),original.getGreen(),original.getBlue());
    }
    @Override
    protected int getBlockLightLevel(DisplayBone entity, BlockPos pos) {
        return 15;
    }
}