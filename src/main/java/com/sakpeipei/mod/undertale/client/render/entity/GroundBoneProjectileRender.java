package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sakpeipei.mod.undertale.client.model.entity.GroundBoneProjectileModel;
import com.sakpeipei.mod.undertale.entity.projectile.GroundBoneProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import javax.annotation.Nullable;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 20:58
 */
public class GroundBoneProjectileRender extends ColorAttackRenderer<GroundBoneProjectile> {
    private static final Logger log = LogManager.getLogger(GroundBoneProjectileRender.class);

    public GroundBoneProjectileRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GroundBoneProjectileModel());
    }

    @Override
    public void preRender(PoseStack poseStack, GroundBoneProjectile animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        GeoBone up = model.getBone("edge-up").get();
        GeoBone body = model.getBone("body").get();
        BoneSnapshot upInitial = up.getInitialSnapshot();
        BoneSnapshot bodyInitial = body.getInitialSnapshot();
        float addHeight = animatable.getHeight() * 16;

        double ySize = bodyInitial.getBone().getCubes().getFirst().size().y;
        body.setScaleY(1.0f + addHeight/ (float)ySize);
        up.setPosY(upInitial.getOffsetY() + addHeight);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    protected void applyRotations(GroundBoneProjectile animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.translate(0,-0.01f,0);
        poseStack.mulPose(Axis.YP.rotationDegrees(animatable.getYRot()));
    }

}
