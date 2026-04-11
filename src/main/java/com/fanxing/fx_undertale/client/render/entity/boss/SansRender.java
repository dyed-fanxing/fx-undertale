package com.fanxing.fx_undertale.client.render.entity.boss;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.model.entity.SansModel;
import com.fanxing.fx_undertale.client.render.AbstractDeadUTMonsterDustRenderer;
import com.fanxing.fx_undertale.client.render.layer.SansFatigueLayer;
import com.fanxing.fx_undertale.common.RenderTypes;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SansRender extends AbstractDeadUTMonsterDustRenderer<Sans> {
    private static final RenderType EYES_EMISSIVE = RenderType.entityTranslucentEmissive(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "textures/entity/sans_eyes.png"));
    private static final ResourceLocation EYES_BLINK = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "textures/entity/sans_eyes_blink.png");
    private static final ResourceLocation SANS_WHITE = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "textures/entity/sans_white.png");
    private static final RenderType EYES_BLINK_EMISSIVE = RenderType.entityTranslucentEmissive(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "textures/entity/sans_eyes_blink.png"));
    private static final Logger log = LoggerFactory.getLogger(SansRender.class);
//    RenderType whiteEntityType = RenderTypes.WHITE_ENTITY_TRANSLUCENT.apply(getTextureLocation(animatable), true);
    RenderType WHITE = RenderType.ENTITY_TRANSLUCENT.apply(SANS_WHITE, false);


    public SansRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SansModel());
        addRenderLayer(new SansEyesLayer(this));
        addRenderLayer(new SansFatigueLayer(this));
    }

    @Override
    public void renderFinal(PoseStack poseStack, Sans animatable, BakedGeoModel model, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);
        float animTick = animatable.tickCount + partialTick;

        // 先渲染光条（在实体之前，光条会在实体后面，更自然）
        renderSwipeTrail(poseStack, animatable, model, bufferSource, animTick, packedLight);
        // 检查残影是否激活
        if (!animatable.isPhantomActive()) return;
        Vec3 startPos = animatable.getPhantomStartPos();
        int startTick = animatable.getPhantomStartTick();
        if (startPos == null) return;
        float age = animTick - startTick;
        if (age >= Sans.PHANTOM_DURATION) {
            return;
        }
        float progress = age / Sans.PHANTOM_DURATION;
        progress = Math.max(0, Math.min(1, progress));
        Vec3 phantomPos = startPos.lerp(animatable.position(), progress);
        float alpha = Mth.lerp(progress, 0.8f, 0);
        // 渲染残影
        poseStack.pushPose();
        Vec3 offset = phantomPos.subtract(animatable.getX(), animatable.getY(), animatable.getZ());
        poseStack.translate(offset.x, offset.y, offset.z);
        if (model != null) {
            int color = (0xFFFFFF) | ((int) (alpha * 255) << 24);
            actuallyRender(poseStack, animatable, model, WHITE, bufferSource, bufferSource.getBuffer(WHITE), true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, color);
        }
        poseStack.popPose();
    }



    // 在 renderFinal 或 render 中调用
    private void renderSwipeTrail(PoseStack poseStack, Sans animatable, BakedGeoModel model,MultiBufferSource bufferSource, float animTick, int packedLight) {
        // 1. 获取动画控制器并判断是否正在播放攻击动画
        AnimationController<Sans> attackController = animatable.getAttackAnimController();
        RawAnimation currentRawAnimation = attackController.getCurrentRawAnimation();
        // 5. 渲染拖尾（使用 TRIANGLE_STRIP 模式的 RenderType）
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_STRIP_WHITE);
        Vector3d leftPos = new Vector3d();
        if(attackController.getAnimationState()== AnimationController.State.RUNNING && (Sans.isSameRawAnimation(currentRawAnimation,Sans.ANIM_BONE_PROJECTILE_LEFT)||Sans.isSameRawAnimation(currentRawAnimation,Sans.THROW_ANIMATIONS))){
            if(Sans.isSameRawAnimation(currentRawAnimation,Sans.ANIM_BONE_PROJECTILE_LEFT)){
                animatable.leftHandTrail.setColor(255,255,255);
            }
            if(Sans.isSameRawAnimation(currentRawAnimation,Sans.THROW_ANIMATIONS)){
                animatable.leftHandTrail.setColor(Sans.ENERGY_AQUA);
            }
            GeoBone leftHand = attackController.getBoneAnimationQueues().get("left_hand").bone();
            // 使用平滑后的位置添加点
            animatable.leftHandTrail.addPoint( leftHand.getLocalPosition(),animTick);
            animatable.leftAnimPlaying=true;
        }else if(animatable.leftAnimPlaying){
            animatable.leftHandTrail.breakStrip();
            animatable.leftAnimPlaying = false;
        }
        Vector3d localPosition = new Vector3d();
        if(attackController.getAnimationState()== AnimationController.State.RUNNING && Sans.isSameRawAnimation(currentRawAnimation,Sans.ANIM_BONE_PROJECTILE)){
            animatable.leftHandTrail.setColor(255,255,255);
            animatable.rightHandTrail.setColor(255,255,255);
            GeoBone leftHand = attackController.getBoneAnimationQueues().get("left_hand").bone();
            animatable.leftHandTrail.addPoint(leftHand.getLocalPosition(),animTick);
            GeoBone rightHand = attackController.getBoneAnimationQueues().get("right_hand").bone();
            animatable.rightHandTrail.addPoint(rightHand.getLocalPosition(),animTick);
            animatable.allAnimPlaying = true;
        }else if(animatable.allAnimPlaying){
            animatable.leftHandTrail.breakStrip();
            animatable.rightHandTrail.breakStrip();
            animatable.allAnimPlaying = false;
        }

        animatable.leftHandTrail.render(new Vector3f((float) leftPos.x, (float) leftPos.y, (float) leftPos.z),poseStack, consumer,packedLight,animTick);
        animatable.leftHandTrail.breakStrip(poseStack,consumer,packedLight);
        animatable.rightHandTrail.render(new Vector3f((float) localPosition.x, (float) localPosition.y, (float) localPosition.z),poseStack, consumer,packedLight,animTick);
    }


    public static class SansEyesLayer extends GeoRenderLayer<Sans> {
        public SansEyesLayer(GeoRenderer<Sans> entityRendererIn) {
            super(entityRendererIn);
            Minecraft.getInstance().getTextureManager().register(EYES_BLINK, new AnimatableTexture(EYES_BLINK));
        }

        @Override
        public void render(PoseStack poseStack, Sans animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if (animatable.getIsEyeBlink()) {
                AnimatableTexture.setAndUpdate(EYES_BLINK);
                this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, EYES_BLINK_EMISSIVE, bufferSource.getBuffer(EYES_BLINK_EMISSIVE), partialTick, LightTexture.FULL_SKY, packedOverlay, -1);
            } else {
                this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, EYES_EMISSIVE, bufferSource.getBuffer(EYES_EMISSIVE), partialTick, LightTexture.FULL_SKY, packedOverlay, -1);
            }
        }
    }

}
