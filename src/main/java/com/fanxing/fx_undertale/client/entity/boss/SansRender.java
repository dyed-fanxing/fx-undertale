package com.fanxing.fx_undertale.client.entity.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.model.entity.SansModel;
import com.fanxing.fx_undertale.client.entity.AbstractDeadUTMonsterDustRenderer;
import com.fanxing.fx_undertale.client.layer.SansEyesLayer;
import com.fanxing.fx_undertale.client.layer.SansFatigueLayer;
import com.fanxing.fx_undertale.common.RenderTypes;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class SansRender extends AbstractDeadUTMonsterDustRenderer<Sans> {
    RenderType whiteEntityType = RenderTypes.WHITE_ENTITY_TRANSLUCENT.apply(getTextureLocation(animatable), true);
    public SansRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SansModel());
        addRenderLayer(new SansEyesLayer(this, ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "textures/entity/sans_eyes.png")));
        addRenderLayer(new SansFatigueLayer(this));
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
        float alpha = Mth.lerp(progress,0.8f,0);
        // 渲染残影
        poseStack.pushPose();
        Vec3 offset = phantomPos.subtract(animatable.getX(), animatable.getY(), animatable.getZ());
        poseStack.translate(offset.x, offset.y, offset.z);
        if (model != null) {
            int color = (0xFFFFFF) | ((int)(alpha * 255) << 24);
            actuallyRender(poseStack, animatable, model, whiteEntityType, bufferSource, bufferSource.getBuffer(whiteEntityType),true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, color);
        }
        poseStack.popPose();
    }
}
