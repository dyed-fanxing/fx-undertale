package com.fanxing.fx_undertale.client.render.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 径向平面条带：宽度方向垂直于径向和切线（即 rightDir = radial × tangent），即副法线方向；
 * 效果类似于飘带;
 */
public class RadialPlaneTrailStrip extends AbstractPointTrail {
    public float width;

    public RadialPlaneTrailStrip(float lifetime, float r, float g, float b, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        super(lifetime, r, g, b, progressCurve, uAlphaCurve);
    }

    public RadialPlaneTrailStrip(float lifetime, float r, float g, float b, Function<Float, Float> progressCurve) {
        super(lifetime, r, g, b, progressCurve);
    }

    public RadialPlaneTrailStrip(float lifetime, int color, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        super(lifetime, color, progressCurve, uAlphaCurve);
    }

    public RadialPlaneTrailStrip(float lifetime, int color, Function<Float, Float> progressCurve) {
        super(lifetime, color, progressCurve);
    }

    public RadialPlaneTrailStrip(float lifetime, int color) {
        super(lifetime, color);
    }
    public RadialPlaneTrailStrip width(float width) {
        this.width = width;
        return this;
    }


    public void render(Vector3f centerOverride, PoseStack poseStack, VertexConsumer consumer,
                       int packedLight, float currentTime) {
        if (centerOverride != null) this.center = centerOverride;
        points.removeIf(p -> currentTime - p.createTime > lifetime);
        if (points.size() < 2) return;
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
        float[] alphas = new float[size];
        for (int i = 0; i < size; i++) {
            float age = currentTime - list.get(i).createTime;
            float progress = Math.min(1f, Math.max(0f, age / lifetime));
            alphas[i] = progressCurve.apply(progress);
        }

        Vector3f lastTangent = null;
        for (int i = 0; i < size; i++) {
            TrailPoint tp = list.get(i);
            Vector3f p = tp.position;

            // 切线
            Vector3f tangent = new Vector3f();
            if (i == 0) tangent.set(list.get(1).position).sub(p).normalize();
            else if (i == size - 1) tangent.set(p).sub(list.get(i - 1).position).normalize();
            else tangent.set(list.get(i + 1).position).sub(list.get(i - 1).position).normalize();
            if (tangent.lengthSquared() < 1e-6f) {
                if (lastTangent != null) tangent.set(lastTangent);
                else continue;
            }
            lastTangent = tangent;

            // 径向向量
            Vector3f radial = new Vector3f(p).sub(this.center).normalize();
            // 宽度方向垂直于径向和切线
            Vector3f rightDir = radial.cross(tangent).normalize();
            if (rightDir.lengthSquared() < 1e-6f) {
                Vector3f up = new Vector3f(0, 1, 0);
                rightDir = up.cross(tangent).normalize();
                if (rightDir.lengthSquared() < 1e-6f) rightDir.set(1, 0, 0);
            }

            float halfWidth = width * 0.5f;
            Vector3f left = new Vector3f(rightDir).mul(halfWidth).add(p);
            Vector3f right = new Vector3f(rightDir).mul(-halfWidth).add(p);
            float u = i / (float) (size - 1);
            float alpha = alphas[i];
            Vector3f dummyNormal = new Vector3f(0, 1, 0);
            addVertex(consumer,matrix,left,alpha,u,0f,packedLight,dummyNormal);
            addVertex(consumer,matrix,right,alpha,u,0f,packedLight,dummyNormal);
        }
        breakStrip(poseStack,consumer);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, float currentTime) {
        render(null, poseStack, consumer, packedLight, currentTime);
    }
}