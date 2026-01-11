package com.sakpeipei.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.client.model.entity.GasterBlasterModel;
import com.sakpeipei.undertale.client.render.layer.GasterBlasterEyesLayer;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GasterBlasterRender extends GeoEntityRenderer<GasterBlaster> {
    public GasterBlasterRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GasterBlasterModel());
        this.addRenderLayer(new GasterBlasterEyesLayer<>(this, ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"textures/entity/gaster_blaster_eyes.png")));
    }

    @Override
    protected void applyRotations(GasterBlaster animatable, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

    @Override
    public void render(@NotNull GasterBlaster animatable, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(animatable, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        GasterBlasterBeamRenderer.render(animatable, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(@NotNull GasterBlaster animatable, @NotNull Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // 如果实体本身在视锥内，直接渲染
        if (super.shouldRender(animatable, frustum, cameraX, cameraY, cameraZ)) {
            return true;
        }
        if(!Vec3.ZERO.equals(animatable.getEnd())){
            return frustum.isVisible( new AABB(animatable.getStart(), animatable.getEnd()));
        }
        return false;
    }
}
