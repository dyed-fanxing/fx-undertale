package com.fanxing.fx_undertale.client.render.summon;

import com.fanxing.fx_undertale.entity.summon.GroundBoneOBB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.fanxing.fx_undertale.client.model.entity.GroundBoneModel;
import com.fanxing.fx_undertale.client.render.ColorAttackRenderer;
import com.fanxing.fx_undertale.entity.summon.GroundBone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.util.Color;

/**
 * @author FanXing
 * @since 2025-08-18 20:58
 */
public class GroundBoneRender extends ColorAttackRenderer<GroundBone> {
    private static final Logger log = LogManager.getLogger(GroundBoneRender.class);

    public GroundBoneRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GroundBoneModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, GroundBone animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getScale(), animatable.getScale(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected void applyRotations(GroundBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.translate(0,-0.01f,0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

    @Override
    public Color getRenderColor(GroundBone animatable, float partialTick, int packedLight) {
        Color original = super.getRenderColor(animatable, partialTick, packedLight);
        return Color.ofARGB((int)(original.getAlpha() * 0.75f),original.getRed(),original.getGreen(),original.getBlue());
    }

    @Override
    protected int getBlockLightLevel(GroundBone entity, BlockPos pos) {
        return 15;
    }
}
