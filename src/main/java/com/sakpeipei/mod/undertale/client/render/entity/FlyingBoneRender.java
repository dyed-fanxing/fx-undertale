package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.sakpeipei.mod.undertale.client.model.entity.FlyingBoneModel;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
        poseStack.mulPose(Axis.YP.rotationDegrees( animatable.getYRot() ));
        poseStack.mulPose(Axis.XP.rotationDegrees(90f - animatable.getXRot() ));


        // 调试信息
        if (animatable.tickCount % 20 == 0) {
            LogUtils.getLogger().info("渲染旋转 - Yaw: {}, Pitch: {}",
                    animatable.getYRot(), animatable.getXRot());
        }
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

}
