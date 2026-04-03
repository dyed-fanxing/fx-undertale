package com.fanxing.fx_undertale.client.render.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * 径向刀光条带：宽度方向沿径向（从中心点指向外）。
 * 效果类似于从中心点甩出的光刃。
 */
public class RadialBladeTrailStrip {
    private static final Logger log = LoggerFactory.getLogger(RadialBladeTrailStrip.class);
    public LinkedList<TrailPoint> points = new LinkedList<>();
    public float lifetime;
    public float width;
    public int maxPoints;
    public float r, g, b;
    public Function<Float, Float> progressCurve;
    public Vector3f center = new Vector3f();

    public record TrailPoint(Vector3f position, float createTime) {
    }

    public RadialBladeTrailStrip(float lifetime, float width, int maxPoints,
                                 float r, float g, float b,
                                 Function<Float, Float> progressCurve) {
        this.maxPoints = maxPoints;
        this.width = width;
        this.r = r;
        this.g = g;
        this.b = b;
        this.lifetime = lifetime;
        this.progressCurve = progressCurve;
    }

    public RadialBladeTrailStrip(float lifetime, float width, int maxPoints, int color,Function<Float, Float> progressCurve) {
        this(lifetime, width, maxPoints,FastColor.ARGB32.red(color) / 255f,FastColor.ARGB32.green(color) / 255f,FastColor.ARGB32.blue(color) / 255f,progressCurve);
    }

    public RadialBladeTrailStrip(float lifetime, float width, int maxPoints, int color) {
        this(lifetime, width, maxPoints, color, progress -> 1f - progress);
    }

    public void addPoint(Vector3f point, float time) {
        if (!points.isEmpty()) {
            Vector3f last = points.getLast().position;
            if (last.distanceSquared(point) < 0.0025f) return;
        }
        points.addLast(new TrailPoint(point, time));
        while (points.size() > maxPoints) points.removeFirst();
    }

    public void addPoint(Vector3d point, float time) {
        addPoint(new Vector3f((float) point.x, (float) point.y, (float) point.z), time);
    }

    public void breakStrip() {
        if (points.isEmpty()) return;
        TrailPoint last = points.getLast();
        points.addLast(new TrailPoint(new Vector3f(last.position), last.createTime));
        points.addLast(new TrailPoint(new Vector3f(last.position), last.createTime));
    }

    /**
     * 视觉上断开条带（插入两个重复点，形成退化三角形）
     */
    public void breakStrip(PoseStack poseStack, VertexConsumer consumer, int packedLight) {
        if (points.isEmpty()) return;
        Vector3f lastPos = points.getLast().position();
        consumer.addVertex(poseStack.last().pose(), lastPos.x, lastPos.y, lastPos.z)
                .setColor(0, 0, 0, 0)           // 完全透明
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0, 1, 0);
        consumer.addVertex(poseStack.last().pose(), lastPos.x, lastPos.y, lastPos.z)
                .setColor(0, 0, 0, 0)           // 完全透明
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0, 1, 0);
    }

    public void render(Vector3f centerOverride, PoseStack poseStack, VertexConsumer consumer,
                       int packedLight, float currentTime) {
        if (centerOverride != null) this.center = centerOverride;
        points.removeIf(p -> currentTime - p.createTime > lifetime);
        if (points.size() < 2) return;

        List<TrailPoint> rawPoints = new ArrayList<>(points);
        // 根据距离大小，自适应 Catmull-Rom 插值
        List<TrailPoint> smoothPoints = new ArrayList<>();
        float targetDistance = 0.06f;
        for (int i = 0; i < rawPoints.size() - 1; i++) {
            TrailPoint tp0 = rawPoints.get(i);
            TrailPoint tp1 = rawPoints.get(i + 1);
            Vector3f p0 = tp0.position, p1 = tp1.position;
            float t0 = tp0.createTime, t1 = tp1.createTime;
            float dist = p0.distance(p1);
            int needed = (int) Math.ceil(dist / targetDistance) - 1;
            int subdivisions = Math.min(needed, 20);
            TrailPoint tpPrev = (i > 0) ? rawPoints.get(i - 1) : tp0;
            TrailPoint tpNext = (i + 2 < rawPoints.size()) ? rawPoints.get(i + 2) : tp1;
            Vector3f pPrev = tpPrev.position, pNext = tpNext.position;
            smoothPoints.add(tp0);
            if (subdivisions > 0) {
                for (int s = 1; s <= subdivisions; s++) {
                    float t = (float) s / (subdivisions + 1);
                    Vector3f pos = catmullRom(pPrev, p0, p1, pNext, t);
                    float time = t0 + (t1 - t0) * t;
                    smoothPoints.add(new TrailPoint(pos, time));
                }
            }
        }
        smoothPoints.add(rawPoints.getLast());
        List<TrailPoint> list = smoothPoints;

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
            // 宽度方向沿径向（需要投影到切平面，保证垂直于切线）
            Vector3f rightDir = new Vector3f(radial);
            float dot = rightDir.dot(tangent);
            rightDir.sub(tangent.mul(dot)).normalize();
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

            // 光照法线使用世界向上（或简化），不影响视觉效果
            Vector3f dummyNormal = new Vector3f(0, 1, 0);
            consumer.addVertex(matrix, left.x, left.y, left.z)
                    .setColor(r, g, b, alpha)
                    .setUv(u, 0)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(packedLight)
                    .setNormal(poseStack.last(), dummyNormal.x, dummyNormal.y, dummyNormal.z);
            consumer.addVertex(matrix, right.x, right.y, right.z)
                    .setColor(r, g, b, alpha)
                    .setUv(u, 1)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(packedLight)
                    .setNormal(poseStack.last(), dummyNormal.x, dummyNormal.y, dummyNormal.z);
        }
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, float currentTime) {
        render(null, poseStack, consumer, packedLight, currentTime);
    }

    private Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float x = Mth.catmullrom(t, p0.x, p1.x, p2.x, p3.x);
        float y = Mth.catmullrom(t, p0.y, p1.y, p2.y, p3.y);
        float z = Mth.catmullrom(t, p0.z, p1.z, p2.z, p3.z);
        return new Vector3f(x, y, z);
    }

    public Vector3f getLastPosition() {
        return points.isEmpty() ? null : points.getLast().position;
    }
    public void setColor(int r,int g,int b) {
        this.r = r / 255f;
        this.g = g / 255f;
        this.b = b / 255f;
    }
    public void setColor(int color){
        this.r = FastColor.ARGB32.red(color) / 255f;
        this.g = FastColor.ARGB32.green(color) / 255f;
        this.b = FastColor.ARGB32.blue(color) / 255f;
    }
}