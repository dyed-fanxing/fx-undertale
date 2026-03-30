package com.fanxing.fx_undertale.client.render;

import com.fanxing.fx_undertale.client.model.entity.RotationBoneModel;
import com.fanxing.fx_undertale.entity.summon.RotationBone;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

/**
 * @author FanXing
 * @since 2025-08-18 20:58
 */
public class RotationBoneRenderer extends GeoEntityRenderer<RotationBone> {

    public RotationBoneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RotationBoneModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, RotationBone animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getScale(), animatable.getScale(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected void applyRotations(RotationBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(animatable.getLerpOrientation(partialTick));
    }

    @Override
    public Color getRenderColor(RotationBone animatable, float partialTick, int packedLight) {
        Color original = super.getRenderColor(animatable, partialTick, packedLight);
        return Color.ofARGB((int)(original.getAlpha() * 0.75f),original.getRed(),original.getGreen(),original.getBlue());
    }
}