package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.sakpeipei.mod.undertale.client.model.entity.FlyingBoneModel;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 20:58
 */
public class FlyingBoneRender extends GeoEntityRenderer<FlyingBone> {

    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(FlyingBoneRender.class);

    public FlyingBoneRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new FlyingBoneModel());
    }

    @Override
    protected void applyRotations(FlyingBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        log.info("{}：旧值({},{}),新值({},{}),{}",animatable.getId(),animatable.yRotO,animatable.xRotO,animatable.getYRot(),animatable.getXRot(),animatable.position());
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick,animatable.yRotO,animatable.getYRot()) ));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick,90f - animatable.xRotO,90f - animatable.getXRot()) ));
//         由于先旋转，绕X轴旋转90度，导致实体的局部+Y轴对齐到了世界+Z轴，+Z轴则对齐到了世界-Y轴，
//         所以下方的变换需要按照该实体目前在世界坐标系中的局部坐标系来变换（+x，+z，-y）
        poseStack.translate(0,-0.75f,-0.25f);
    }
}
