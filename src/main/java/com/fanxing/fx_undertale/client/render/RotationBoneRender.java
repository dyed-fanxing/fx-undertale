package com.fanxing.fx_undertale.client.render;

import com.fanxing.fx_undertale.client.model.entity.RotationBoneModel;
import com.fanxing.fx_undertale.entity.projectile.RotationBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

/**
 * @author FanXing
 * @since 2025-08-18 20:58
 */
public class RotationBoneRender extends GeoEntityRenderer<RotationBone> {

    public RotationBoneRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RotationBoneModel());
    }

    @Override
    public void preRender(PoseStack poseStack, RotationBone animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        poseStack.translate(0,0.5f,0);

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    protected void applyRotations(RotationBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick,animatable.yRotO,animatable.getYRot()) ));
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        //         由于先旋转，绕X轴旋转90度，导致实体的局部+Y轴对齐到了世界+Z轴，+Z轴则对齐到了世界-Y轴，
//         所以下方的变换需要按照该实体目前在世界坐标系中的局部坐标系来变换（+x，+z，-y）
        poseStack.translate(0,-0.5f,-0.15625f);
    }

    @Override
    public Color getRenderColor(RotationBone animatable, float partialTick, int packedLight) {
        Color original = super.getRenderColor(animatable, partialTick, packedLight);
        return Color.ofARGB((int)(original.getAlpha() * 0.75f),original.getRed(),original.getGreen(),original.getBlue());
    }
}
