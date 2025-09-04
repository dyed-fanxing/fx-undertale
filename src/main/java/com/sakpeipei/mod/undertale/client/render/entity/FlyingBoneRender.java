package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.sakpeipei.mod.undertale.client.model.entity.FlyingBoneModel;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
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
    public void preRender(PoseStack poseStack, FlyingBone animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    protected void applyRotations(FlyingBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        // 使用四元数一次性计算所有旋转
        Quaternionf rotation = new Quaternionf()
                .rotateY((float)Math.toRadians(animatable.getYRot()))    // 偏航
                .rotateX((float)Math.toRadians(animatable.getXRot())) ;   // 俯仰


        poseStack.mulPose(rotation);


        // 调试信息
        if (animatable.tickCount % 20 == 0) {
            LogUtils.getLogger().info("渲染旋转 - Yaw: {}, Pitch: {}",
                    animatable.getYRot(), animatable.getXRot());
        }
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

}
