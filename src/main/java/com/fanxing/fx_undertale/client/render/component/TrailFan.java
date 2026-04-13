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
public class TrailFan extends AbstractPointTrail {
    public TrailFan(float lifetime, float r, float g, float b, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        super(lifetime, r, g, b, progressCurve, uAlphaCurve);
    }

    public TrailFan(float lifetime, float r, float g, float b, Function<Float, Float> progressCurve) {
        super(lifetime, r, g, b, progressCurve);
    }

    public TrailFan(float lifetime, int color, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        super(lifetime, color, progressCurve, uAlphaCurve);
    }

    public TrailFan(float lifetime, int color, Function<Float, Float> progressCurve) {
        super(lifetime, color, progressCurve);
    }

    public TrailFan(float lifetime, int color) {
        super(lifetime, color);
    }

    public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, VertexConsumer consumer, int packedLight, float currentTime) {
        // 移除过期点
        points.removeIf(p -> currentTime - p.createTime > lifetime);
        if (points.isEmpty()) return;
        List<TrailPoint> list = getSmoothPoints();
        // 过滤过近点
        List<TrailPoint> filtered = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0 && filtered.getLast().position.distanceSquared(list.get(i).position) < 1e-6f)
                continue;
            filtered.add(list.get(i));
        }
        list = filtered;
        int size = list.size();
        if (size < 2) return;
        Matrix4f matrix = poseStack.last().pose();
        // 三角扇：第一个顶点是圆心（透明度固定为1，可调整）
        Vector3f normal = new Vector3f(0, 1, 0);
        addVertex(consumer, matrix, center, 1.0f,0,1, packedLight,normal);
        // 按时间顺序添加所有历史点
        for (TrailPoint p : list) {
            float age = currentTime - p.createTime;
            float progress = Mth.clamp(age / lifetime, 0, 1);
            float alpha = progressCurve.apply(progress);
            addVertex(consumer, matrix, p.position,alpha, 0,1, packedLight,normal);
        }
        addVertex(consumer, matrix, center, 0F,0,1, packedLight,normal);
        bufferSource.endBatch();
    }
}