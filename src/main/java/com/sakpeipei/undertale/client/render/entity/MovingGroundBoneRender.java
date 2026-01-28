package com.sakpeipei.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.client.model.entity.MovingGroundBoneModel;
import com.sakpeipei.undertale.entity.summon.MovingGroundBone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 20:58
 */
public class MovingGroundBoneRender extends ColorAttackRenderer<MovingGroundBone> {
    private static final Logger log = LogManager.getLogger(MovingGroundBoneRender.class);

    public MovingGroundBoneRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MovingGroundBoneModel());
    }

    @Override
    protected void applyRotations(MovingGroundBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.translate(0,-0.01f,0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }
}
