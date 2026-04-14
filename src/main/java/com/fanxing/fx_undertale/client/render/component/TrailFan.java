package com.fanxing.fx_undertale.client.render.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 三角扇拖尾 - 以圆心为共享顶点，按时间顺序添加历史点，形成连续扇形。
 * 适用于绕固定中心旋转的杆子端点拖尾。
 */
public class TrailFan extends AbstractPointTrail<TrailFan> {
    public TrailFan(float lifetime, float r, float g, float b, float a, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        super(lifetime, r, g, b, a, progressCurve, uAlphaCurve);
    }

    public TrailFan(float lifetime) {
        super(lifetime);
    }

    @Override
    public void render(List<TrailPoint> list,PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer consumer,
                       int packedLight, float currentTime) {
        int size = list.size();
        Matrix4f matrix = poseStack.last().pose();
        // 三角扇：第一个顶点是圆心（透明度固定为1，可调整）
        Vector3f normal = new Vector3f(0, 1, 0);
        addVertex(consumer, matrix, center, 1.0f,0,1, packedLight,normal);
        // 按时间顺序添加所有历史点
        for (int i = 0; i < size; i++) {
            TrailPoint p = list.get(i);
            float age = currentTime - p.createTime;
            float progress = Mth.clamp(age / lifetime, 0, 1);
            float alpha = progressCurve.apply(progress);
            float u = i / (float)(size - 1);
            float finalAlpha = alpha * uAlphaCurve.apply(u);
            addVertex(consumer, matrix, p.position,finalAlpha, 0,1, packedLight,normal);
        }
        addVertex(consumer, matrix, center, 0F,0,1, packedLight,normal);
        endBatch(bufferSource);
    }
}