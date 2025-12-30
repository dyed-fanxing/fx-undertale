package com.sakpeipei.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.client.model.entity.GasterBlasterProModel;
import com.sakpeipei.undertale.entity.summon.GasterBlasterPro;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GasterBlasterProRender extends GeoEntityRenderer<GasterBlasterPro> {
    public GasterBlasterProRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GasterBlasterProModel());
    }

    @Override
    protected void applyRotations(GasterBlasterPro animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, GasterBlasterPro animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getWidth(), animatable.getWidth(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    public void render(@NotNull GasterBlasterPro entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        if(entity.isFire()) {
            GasterBlasterBeamRenderer.render(entity,partialTick,poseStack,bufferSource,packedLight);
        }
    }

}
