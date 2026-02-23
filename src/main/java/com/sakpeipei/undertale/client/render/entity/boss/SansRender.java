package com.sakpeipei.undertale.client.render.entity.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.client.model.entity.SansModel;
import com.sakpeipei.undertale.client.render.layer.SansEyesLayer;
import com.sakpeipei.undertale.client.render.layer.SansFatigueLayer;
import com.sakpeipei.undertale.entity.boss.sans.Sans;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SansRender extends GeoEntityRenderer<Sans> {

    private static final ResourceLocation PHANTOM_TEXTURE = ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "textures/entity/sans_phantom.png");

    public SansRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SansModel());
        this.renderLayers.addLayer(new SansEyesLayer(this, ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "textures/entity/sans_eyes.png")));
        this.renderLayers.addLayer(new SansFatigueLayer(this));
    }

    @Override
    public void renderFinal(PoseStack poseStack, Sans animatable, BakedGeoModel model, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);
        // 检查残影是否激活
        if (!animatable.isPhantomActive()) return;
        Vec3 startPos = animatable.getPhantomStartPos();
        int startTick = animatable.getPhantomStartTick();
        if (startPos == null) return;
        float currentTime = animatable.tickCount + partialTick;
        float age = currentTime - startTick;
        if (age >= Sans.PHANTOM_DURATION) {
            return;
        }
        float progress = age / Sans.PHANTOM_DURATION;
        progress = Math.max(0, Math.min(1, progress));
        Vec3 phantomPos = startPos.lerp(animatable.position(), progress);
        float alpha = Mth.lerp(progress,1,0);
        // 渲染残影
        renderPhantom(animatable, phantomPos, alpha, partialTick, poseStack, bufferSource, packedLight);
    }

    /**
     * 渲染单个残影
     */
    private void renderPhantom(Sans entity, Vec3 pos, float alpha, float partialTick,PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        Vec3 offset = pos.subtract(entity.getX(), entity.getY(), entity.getZ());
        poseStack.translate(offset.x, offset.y, offset.z);
        BakedGeoModel model = this.getGeoModel().getBakedModel(this.getGeoModel().getModelResource(entity));
        if (model != null) {
            RenderType renderType = RenderType.beaconBeam(PHANTOM_TEXTURE,true);
            VertexConsumer buffer = bufferSource.getBuffer(renderType);

            // 构建带透明度的颜色（ARGB）
            int color = (0xFFFFFF) | ((int)(alpha * 255) << 24);

            actuallyRender(poseStack, entity, model, renderType, bufferSource, buffer,
                    true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, color);
        }
        poseStack.popPose();
    }

}
