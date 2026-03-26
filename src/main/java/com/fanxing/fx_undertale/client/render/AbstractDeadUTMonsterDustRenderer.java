package com.fanxing.fx_undertale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.fanxing.fx_undertale.client.Shaders;
import com.fanxing.fx_undertale.common.RenderTypes;
import com.fanxing.fx_undertale.entity.AbstractUTMonster;
import com.fanxing.fx_undertale.registry.ParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Objects;

/**
 * 实现UT怪物死亡尘埃消散的效果
 */
public abstract class AbstractDeadUTMonsterDustRenderer<T extends AbstractUTMonster & GeoAnimatable> extends GeoEntityRenderer<T> {

    public AbstractDeadUTMonsterDustRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                               float partialTick, int packedLight, int packedOverlay, int colour) {
        if (animatable.isDeadOrDying()) {
            float progress = Math.min(1f, (animatable.deathTime + partialTick) / animatable.getDeathTime()); // 死亡时间
            RenderType type = RenderTypes.TOP_FADE.apply(getTextureLocation(animatable), true);
            VertexConsumer consumer = bufferSource.getBuffer(type);
            ShaderInstance shader = Shaders.getTopFadeShader();
            if (shader != null) {
                Objects.requireNonNull(shader.getUniform("Progress")).set(progress);
                // ****************************!!!important****************************************
                // 这里必须要传递相机Y坐标，没测试出为什么
                // 顶点着色器里的Position位置官方说是模型局部位置，但是测试时候发现这个位置会跟着玩家的相机位置变化，
                // 如果是模型视图位置的话，它又要在顶点着色器里乘上ModelViewMat（模型视图矩阵），目前没测试出什么原因
                // 我的理解如果顶点着色器Position是模型局部位置的话，直接传递进度和高度就可以了，但它好像不是，又好像是模型视图坐标
                Objects.requireNonNull(shader.getUniform("CameraY")).set((float) Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y);
                Objects.requireNonNull(shader.getUniform("bottomY")).set((float) animatable.getY());
                Objects.requireNonNull(shader.getUniform("bbHeight")).set(animatable.getBbHeight());
            }
            spawnDeathParticles(animatable, progress);
            super.actuallyRender(poseStack, animatable, model, type, bufferSource, consumer,
                    isReRender, partialTick, packedLight, packedOverlay, colour);
        } else {
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                    isReRender, partialTick, packedLight, packedOverlay, colour);
        }
    }

    /**
     * 生成死亡消散粒子
     *
     * @param animatable  实体
     * @param progress    消散进度 0~1
     */
    private void spawnDeathParticles(T animatable, float progress) {
        // 控制生成频率，避免每帧生成过多（比如每2帧生成一次）
        Level world = animatable.level();

        RandomSource random = animatable.getRandom();
        float bbWidth = animatable.getBbWidth();
        // 粒子数量随进度增加（例如 0~20 个/帧），但限制最高数量避免卡顿
        int count = Mth.ceil(bbWidth / 0.333334F);
        if (count <= 0) return;


        // 模型包围盒信息
        double x = animatable.getX();
        double z = animatable.getZ();
        double halfWidth = bbWidth / 2.0;
        double height = animatable.getBbHeight();

        // 根据进度确定有效生成区域：只从已经透明的部分生成粒子（顶部区域）
        // 透明区域是从顶部开始，即 Y 坐标大于 (y + height * (1 - progress)) 的部分
        double y = animatable.getY() + height * (1.0 - progress);

        for (int i = 0; i < count; i++) {
            // 在透明区域内随机生成位置
            double px = x + (random.nextDouble() - 0.5) * 2 * halfWidth;
            double pz = z + (random.nextDouble() - 0.5) * 2 * halfWidth;
            // 粒子速度：轻微随机，以向上为主
            double vx = (random.nextDouble() - 0.5) * 0.1;
            double vy = 0.03f; // 向上速度
            double vz = (random.nextDouble() - 0.5) * 0.1;
            // 可以选择多种粒子类型，这里先用默认的烟雾
            world.addParticle(ParticleTypes.CUSTOM_WHITE_ASH.get(), px, y, pz, vx, vy, vz);
        }
    }

    @Override
    public boolean shouldRender(T animatable, @NotNull Frustum frustum, double p_114493_, double p_114494_, double p_114495_) {
        if (animatable.isDeadOrDying()) {
            return true;
        }
        return super.shouldRender(animatable, frustum, p_114493_, p_114494_, p_114495_);
    }

    @Override
    public int getPackedOverlay(T animatable, float u, float partialTick) {
        // 死亡时不要红色闪烁
        if (animatable.isDeadOrDying()) {
            return OverlayTexture.NO_OVERLAY;
        }
        return super.getPackedOverlay(animatable, u, partialTick);
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        // 死亡时保持直立，只应用基础的Y轴旋转（避免倒下）
        if (animatable.isDeadOrDying()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - rotationYaw));
            return;
        }
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }
}
