package com.sakpeipei.undertale.client.entity.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.client.layer.AnimatedGlowingLayer;
import com.sakpeipei.undertale.client.model.entity.GasterBlasterLivingModel;
import com.sakpeipei.undertale.client.model.entity.GasterBlasterModel;
import com.sakpeipei.undertale.entity.summon.GasterBlasterLiving;
import com.sakpeipei.undertale.entity.summon.GasterBlasterLiving;
import com.sakpeipei.undertale.entity.summon.IGasterBlaster;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

@EventBusSubscriber
public class GasterBlasterLivingRender extends GeoEntityRenderer<GasterBlasterLiving> {
    public static ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID,"textures/entity/gaster_blaster_eyes.png");
    public static RenderType EYES_GLOW = RenderType.entityTranslucentEmissive(EYES);
    public static ResourceLocation SHOT_EYES = ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID,"textures/entity/gaster_blaster_shot_eyes.png");


    public GasterBlasterLivingRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GasterBlasterLivingModel());
        this.addRenderLayer(new GasterBlasterEyesLayer<>(this));
    }

    @Override
    protected void applyRotations(GasterBlasterLiving animatable, PoseStack poseStack,float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        // 获取实体的精确中心点

        double halfHeight = animatable.getBbHeight()*0.5f;
        poseStack.translate(0, halfHeight, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick,-animatable.yRotO,-animatable.getYRot())));
        if(!animatable.isMountable()){
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick,animatable.xRotO,animatable.getXRot())));
        }
        poseStack.translate(0, -halfHeight, 0);
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }
    // 在客户端总线上注册
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        // 检查玩家是否骑在你的GB上
        if (player.getVehicle() instanceof GasterBlasterLiving gb) {
            PoseStack poseStack = event.getPoseStack();
            Vec3 vec3 = gb.getAttachments().get(EntityAttachment.PASSENGER, 0, 0);
            poseStack.translate(vec3.x,vec3.y,vec3.z);
        }
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, GasterBlasterLiving animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getSize(), animatable.getSize(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    public void preRender(PoseStack poseStack, GasterBlasterLiving animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if(!isReRender && !animatable.isMountable()) {
            GasterBlasterBeamRenderer.render(animatable, partialTick, poseStack, bufferSource, packedLight,GasterBlasterBeamRenderer.SANS_BLUE);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public boolean shouldRender(@NotNull GasterBlasterLiving animatable, @NotNull Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // 如果实体本身在视锥内，直接渲染
        if (super.shouldRender(animatable, frustum, cameraX, cameraY, cameraZ)) {
            return true;
        }
        if(frustum.isVisible( new AABB(animatable.getEyePosition(), animatable.getLookAngle().scale(animatable.getLength())))) {
            return true;
        }
        return animatable.position().subtract(cameraX, cameraY, cameraZ).lengthSqr() < 1024;
    }



    public static class GasterBlasterEyesLayer<T extends Entity & GeoAnimatable & IGasterBlaster> extends AnimatedGlowingLayer<T> {
        public GasterBlasterEyesLayer(GeoRenderer<T> entityRendererIn) {
            super(entityRendererIn, SHOT_EYES);
        }
        @Override
        public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if(animatable.isFire() && !animatable.isMountable()){
                super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            }else{
                this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,EYES_GLOW, bufferSource.getBuffer(EYES_GLOW), partialTick, LightTexture.FULL_SKY, packedOverlay, -1);
            }
        }
    }
}
