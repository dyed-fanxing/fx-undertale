package com.sakpeipei.undertale.client.entity.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.client.model.entity.MovingBoneModel;
import com.sakpeipei.undertale.client.entity.ColorAttackRenderer;
import com.sakpeipei.undertale.entity.summon.MovingGroundBone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.util.Color;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 20:58
 */
public class MovingGroundBoneRender extends ColorAttackRenderer<MovingGroundBone> {
    private static final Logger log = LogManager.getLogger(MovingGroundBoneRender.class);

    public MovingGroundBoneRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MovingBoneModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, MovingGroundBone animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getScale(), animatable.getScale(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected void applyRotations(MovingGroundBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.translate(0,-0.01f,0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

    @Override
    public Color getRenderColor(MovingGroundBone animatable, float partialTick, int packedLight) {
        Color original = super.getRenderColor(animatable, partialTick, packedLight);
        return Color.ofARGB((int)(original.getAlpha() * 0.75f),original.getRed(),original.getGreen(),original.getBlue());
    }
}
