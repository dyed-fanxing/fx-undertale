package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sakpeipei.mod.undertale.client.model.entity.GroundBoneProjectileModel;
import com.sakpeipei.mod.undertale.entity.projectile.GroundBoneProjectile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    protected void applyRotations(GroundBoneProjectile animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.translate(0,-0.01f,0);
        poseStack.mulPose(Axis.YP.rotationDegrees(animatable.getYRot()));
    }

}
