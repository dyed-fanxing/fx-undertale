package com.fanxing.fx_undertale.client.render.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.fanxing.fx_undertale.client.model.entity.LateralBoneModel;
import com.fanxing.fx_undertale.common.phys.AxisQuaternionf;
import com.fanxing.fx_undertale.entity.summon.LateralBone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

/**
 * @author FanXing
 * @since 2025-08-18 20:58
 */
public class LateralBoneRender extends GeoEntityRenderer<LateralBone> {

    private static final Logger log = LoggerFactory.getLogger(LateralBoneRender.class);


    public LateralBoneRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LateralBoneModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, LateralBone animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getScale(), animatable.getScale(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected void applyRotations(LateralBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot()));
        poseStack.mulPose(AxisQuaternionf.Z_NEG_90);
        poseStack.translate(-0.15625,-0.5f*animatable.getScale()*animatable.getGrowScale(),0);
    }

    @Override
    public Color getRenderColor(LateralBone animatable, float partialTick, int packedLight) {
        Color original = super.getRenderColor(animatable, partialTick, packedLight);
        return Color.ofARGB((int)(original.getAlpha() * 0.75f),original.getRed(),original.getGreen(),original.getBlue());
    }
}
