package com.fanxing.fx_undertale.client.render.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;

/**
 * 线条拖尾 - 在相邻轨迹点之间绘制线段，不生成面片。
 * 适用于需要显示光轨、能量线等效果。
 */
public class LineTrailStrip extends AbstractPointTrail<LineTrailStrip> {
    public LineTrailStrip(float lifetime, float r, float g, float b, float a, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        super(lifetime, r, g, b, a, progressCurve, uAlphaCurve);
    }
    public LineTrailStrip(float lifetime) {
        super(lifetime);
    }
    @Override
    public void render(List<TrailPoint> list,PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer consumer,
                       int packedLight, float currentTime) {
        int size = list.size();
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < size; i++) {
            TrailPoint tp = list.get(i);
            Vector3f p = tp.position;
            float age = currentTime - tp.createTime;
            float progress = Mth.clamp(age/lifetime,0f,1f);
            float alpha = progressCurve.apply(progress);
            float u = i / (float)(size - 1);
            float finalAlpha = alpha * uAlphaCurve.apply(u);
            // 注意：法线随意（线条不受光照影响）
            addVertex(consumer, matrix, p, finalAlpha, u, 0, packedLight, new Vector3f(0, 1, 0));
        }
        endBatch(bufferSource);
    }
}