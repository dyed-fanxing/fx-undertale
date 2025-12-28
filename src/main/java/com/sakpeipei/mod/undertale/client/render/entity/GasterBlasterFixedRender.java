package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.client.model.entity.GasterBlasterFixedModel;
import com.sakpeipei.mod.undertale.client.render.layer.GasterBlasterEyesLayer;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GasterBlasterFixedRender extends GeoEntityRenderer<GasterBlasterFixed> {
    public GasterBlasterFixedRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GasterBlasterFixedModel());
        this.addRenderLayer(new GasterBlasterEyesLayer<>(this, ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"textures/entity/gaster_blaster_eyes.png")));
    }

    @Override
    protected void applyRotations(GasterBlasterFixed animatable, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

    @Override
    public void render(@NotNull GasterBlasterFixed entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        GasterBlasterBeamRenderer.render(entity, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(@NotNull GasterBlasterFixed entity, @NotNull Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // 如果实体本身在视锥内，直接渲染
        if (super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ)) {
            return true;
        }
        // 检查激光束路径是否在视锥内
        Vec3 start = entity.position();
        Vec3 end = start.add(entity.getLookAngle().scale(entity.getLength()));
        AABB beamAABB = new AABB(start, end).inflate(entity.getWidth() * 0.5);
        return frustum.isVisible(beamAABB);
    }
}
