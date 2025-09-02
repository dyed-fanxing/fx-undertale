package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.mod.undertale.client.model.entity.FlyingBoneModel;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 20:58
 */
public class FlyingBoneRender extends GeoEntityRenderer<FlyingBone> {

    public FlyingBoneRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new FlyingBoneModel());
    }

    @Override
    protected void applyRotations(FlyingBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(90f));
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }
}
