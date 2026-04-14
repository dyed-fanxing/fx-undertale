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
 * 径向平面条带：宽度方向垂直于径向和切线（即 rightDir = radial × tangent），即副法线方向；
 * 效果类似于飘带;
 */
public class RadialPlaneTrailStrip extends AbstractPointTrail<RadialPlaneTrailStrip> {
    public float width;

    public RadialPlaneTrailStrip(float lifetime, float a, float r, float g, float b, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        super(lifetime, a, r, g, b, progressCurve, uAlphaCurve);
    }
    public RadialPlaneTrailStrip(float lifetime) {
        super(lifetime);
    }
    public RadialPlaneTrailStrip width(float width) {
        this.width = width;
        return this;
    }

    @Override
    public void render(List<TrailPoint> list,PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer consumer,
                       int packedLight, float currentTime) {
        Matrix4f matrix = poseStack.last().pose();
        int size = list.size();
        Vector3f lastTangent = null;
        for (int i = 0; i < size; i++) {
            TrailPoint tp = list.get(i);
            Vector3f p = tp.position;

            // 计算当前点的年龄透明度
            float age = currentTime - tp.createTime;
            float progress = Mth.clamp(age / lifetime,0f,1f);
            float alpha = a*progressCurve.apply(progress);

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
            float u = i / (float)(size - 1);
            float finalAlpha = alpha * uAlphaCurve.apply(u);
            Vector3f dummyNormal = new Vector3f(0, 1, 0);
            addVertex(consumer, matrix, left, finalAlpha, u, 0f, packedLight, dummyNormal);
            addVertex(consumer, matrix, right, finalAlpha, u, 1f, packedLight, dummyNormal);
        }
        endBatch(bufferSource);
    }


}