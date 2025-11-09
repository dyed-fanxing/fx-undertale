package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sakpeipei.mod.undertale.client.model.entity.GroundBoneModel;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.mod.undertale.entity.projectile.GroundBoneProjectile;
import com.sakpeipei.mod.undertale.entity.summon.GroundBone;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 20:58
 */
public class GroundBoneRender extends ColorAttackRenderer<GroundBone> {

    public GroundBoneRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GroundBoneModel());
    }

    @Override
    protected void applyRotations(GroundBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.translate(0,-0.01f,0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot()));
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

    @Override
    protected int getBlockLightLevel(@NotNull GroundBone entity, @NotNull BlockPos pos) {
        return 15; // 最大亮度
    }

    @Override
    protected int getSkyLightLevel(@NotNull GroundBone entity, @NotNull BlockPos pos) {
        return 15; // 最大亮度
    }
}
