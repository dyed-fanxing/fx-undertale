package com.fanxing.fx_undertale.client.render.boss;

import com.fanxing.fx_undertale.client.model.entity.RotationBoneModel;
import com.fanxing.fx_undertale.client.render.RotationBoneRender;
import com.fanxing.fx_undertale.utils.RotUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.model.entity.SansModel;
import com.fanxing.fx_undertale.client.render.AbstractDeadUTMonsterDustRenderer;
import com.fanxing.fx_undertale.client.render.layer.SansFatigueLayer;
import com.fanxing.fx_undertale.common.RenderTypes;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SansRender extends AbstractDeadUTMonsterDustRenderer<Sans> {
    private static final RenderType EYES_EMISSIVE = RenderType.entityTranslucentEmissive(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "textures/entity/sans_eyes.png"));
    private static final ResourceLocation EYES_BLINK = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "textures/entity/sans_eyes_blink.png");
    private static final RenderType EYES_BLINK_EMISSIVE = RenderType.entityTranslucentEmissive(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "textures/entity/sans_eyes_blink.png"));
    RenderType whiteEntityType = RenderTypes.WHITE_ENTITY_TRANSLUCENT.apply(getTextureLocation(animatable), true);

    // 武器专用的 GeoRenderer
    private final RotationBoneRender weaponRenderer;

    public SansRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SansModel());
        addRenderLayer(new SansEyesLayer(this));
        addRenderLayer(new SansFatigueLayer(this));
        // 初始化武器渲染器，传入武器模型
        this.weaponRenderer = new RotationBoneRender(renderManager);
    }

    @Override
    public void renderFinal(PoseStack poseStack, Sans animatable, BakedGeoModel model, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);
        // 检查残影是否激活
        if (!animatable.isPhantomActive()) return;
        Vec3 startPos = animatable.getPhantomStartPos();
        int startTick = animatable.getPhantomStartTick();
        if (startPos == null) return;
        float animTick = animatable.tickCount + partialTick;
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
            actuallyRender(poseStack, animatable, model, whiteEntityType, bufferSource, bufferSource.getBuffer(whiteEntityType), true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, color);
        }
        poseStack.popPose();
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
