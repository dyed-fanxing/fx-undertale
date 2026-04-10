package com.fanxing.fx_undertale.client.render.entity.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.fanxing.fx_undertale.client.model.entity.GroundBoneOBBModel;
import com.fanxing.fx_undertale.client.render.ColorAttackRenderer;
import com.fanxing.fx_undertale.entity.summon.GroundBoneOBB;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.util.Color;

/**
 * @author FanXing
 * @since 2025-08-18 20:58
 */
public class GroundBoneOBBRender extends ColorAttackRenderer<GroundBoneOBB> {

    public GroundBoneOBBRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GroundBoneOBBModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, GroundBoneOBB animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getScale(), animatable.getScale(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected int getBlockLightLevel(GroundBoneOBB entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected void applyRotations(GroundBoneOBB animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(animatable.getOrientation());
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

    @Override
    public Color getRenderColor(GroundBoneOBB animatable, float partialTick, int packedLight) {
        Color original = super.getRenderColor(animatable, partialTick, packedLight);
        return Color.ofARGB((int)(original.getAlpha() * 0.75f),original.getRed(),original.getGreen(),original.getBlue());
    }
}
