package com.fanxing.fx_undertale.client.render.entity.summon;

import com.fanxing.fx_undertale.client.model.entity.RotationBoneModel;
import com.fanxing.fx_undertale.common.RenderTypes;
import com.fanxing.fx_undertale.entity.summon.RotationBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
public class RotationBoneRenderer extends GeoEntityRenderer<RotationBone> {

    private static final Logger log = LoggerFactory.getLogger(RotationBoneRenderer.class);

    public RotationBoneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RotationBoneModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, RotationBone animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getScale(), animatable.getScale(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected int getBlockLightLevel(RotationBone entity, BlockPos pos) {
        return 15;
    }
    
    @Override
    protected void applyRotations(RotationBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(animatable.getLerpOrientation(partialTick));
    }

    @Override
    public void renderFinal(PoseStack poseStack, RotationBone animatable, BakedGeoModel model, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int colour) {
        Optional<GeoBone> down = this.getGeoModel().getBone("top");
        float currentTime = animatable.tickCount + partialTick;
        if (down.isPresent()) {
            Vector3d localPos = down.get().getLocalPosition();
            animatable.trail1.addPoint(localPos, currentTime);
            VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.ENERGY_TRIANGLE_FAN_WHITE);
            animatable.trail1.render(poseStack,(MultiBufferSource.BufferSource) bufferSource, consumer, packedLight, currentTime);
        }
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public Color getRenderColor(RotationBone animatable, float partialTick, int packedLight) {
        Color original = super.getRenderColor(animatable, partialTick, packedLight);
        return Color.ofARGB((int)(original.getAlpha() * 0.75f),original.getRed(),original.getGreen(),original.getBlue());
    }
}