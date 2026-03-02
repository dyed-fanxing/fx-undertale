package com.sakpeipei.undertale.client.render.entity.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.client.model.entity.GasterBlasterModel;
import com.sakpeipei.undertale.client.render.layer.GasterBlasterEyesLayer;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GasterBlasterRender extends GeoEntityRenderer<GasterBlaster> {
    public GasterBlasterRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GasterBlasterModel());
        this.addRenderLayer(new GasterBlasterEyesLayer<>(this, ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID,"textures/entity/gaster_blaster_eyes.png")));
    }

    @Override
    protected void applyRotations(GasterBlaster animatable, PoseStack poseStack,float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick,-animatable.yRotO,-animatable.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick,animatable.xRotO,animatable.getXRot())));
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, GasterBlaster animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getSize(), animatable.getSize(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    public void preRender(PoseStack poseStack, GasterBlaster animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if(!isReRender) {
            GasterBlasterBeamRenderer.render(animatable, partialTick, poseStack, bufferSource, packedLight,GasterBlasterBeamRenderer.SANS_BLUE);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public boolean shouldRender(@NotNull GasterBlaster animatable, @NotNull Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // 如果实体本身在视锥内，直接渲染
        if (super.shouldRender(animatable, frustum, cameraX, cameraY, cameraZ)) {
            return true;
        }
        if(frustum.isVisible( new AABB(animatable.getEyePosition(), animatable.getLookAngle().scale(animatable.getLength())))) {
            return true;
        }
        return animatable.position().subtract(cameraX, cameraY, cameraZ).lengthSqr() < 1024;
    }
}
