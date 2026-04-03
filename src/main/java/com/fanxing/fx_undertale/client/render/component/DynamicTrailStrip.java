package com.fanxing.fx_undertale.client.render.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * 动态拖尾条带 - 每个点有独立生命周期，自动消失
 * 使用 TRIANGLE_STRIP 模式渲染。
 * 动画播放时每帧调用 {@link #addPoint(Vector3f, float)} 添加点，传入当前时间戳（tick）。
 * 每帧调用 {@link #render(PoseStack, VertexConsumer, int, float)} 渲染（内部自动管理过期点）。
 * 如果需要分开条带，可以主动调用：
 * - {@link #breakStrip()} 插入两个重复点，在后续渲染中自然形成退化三角形断开；
 * - {@link #breakStrip(PoseStack, VertexConsumer, int)} 直接向缓冲区写入透明顶点，立即断开。
 * <p>
 * 支持基于距离的 Catmull-Rom 插值：当相邻两点距离超过 {@link #maxSegmentLength} 时，自动插入插值点。
 */
public class DynamicTrailStrip {
    private static final Logger log = LoggerFactory.getLogger(DynamicTrailStrip.class);
    private final LinkedList<TrailPoint> points = new LinkedList<>(); // 旧 → 新
    private final int maxPoints;
    private final float width;
    private float r;
    private float g;
    private float b;
    private final float lifetime;          // 生命周期（tick）
    private final Function<Float, Float> progressCurve;
    private float maxSegmentLength = 0.5f;  // 最大线段长度，超过则插值（默认0.5）

    public record TrailPoint(Vector3f position, float createTime) {
    }

    // 浮点颜色构造方法（主构造）
    public DynamicTrailStrip(int maxPoints, float width, float r, float g, float b, float lifetime,
                             Function<Float, Float> progressCurve) {
        this.maxPoints = maxPoints;
        this.width = width;
        this.r = r;
        this.g = g;
        this.b = b;
        this.lifetime = lifetime;
        this.progressCurve = progressCurve;
    }

    // 整数颜色构造方法（兼容旧代码）
    public DynamicTrailStrip(int maxPoints, float width, int color, float lifetime,
                             Function<Float, Float> progressCurve) {
        this(maxPoints, width,
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                lifetime, progressCurve);
    }

    // 简化构造（使用默认线性曲线）
    public DynamicTrailStrip(int maxPoints, float width, int color, float lifetime) {
        this(maxPoints, width, color, lifetime, progress -> 1f - progress);
    }

    /**
     * 设置最大线段长度（用于插值），超过此距离将在两点间插入 Catmull-Rom 插值点。
     * @param maxSegmentLength 最大允许长度，默认 0.5
     */
    public void setMaxSegmentLength(float maxSegmentLength) {
        this.maxSegmentLength = maxSegmentLength;
    }

    /**
     * 添加新点（最新点添加到末尾）
     */
    public void addPoint(Vector3f point, float time) {
        if (!points.isEmpty()) {
            Vector3f last = points.getLast().position;
            // 去重阈值 1e-6
            if (last.distanceSquared(point) < 0.0025f) return;
        }
        points.addLast(new TrailPoint(point, time));
        while (points.size() > maxPoints) {
            points.removeFirst();
        }
    }

    public void addPoint(Vector3d point, float time) {
        addPoint(new Vector3f((float) point.x, (float) point.y, (float) point.z), time);
    }

    /**
     * 视觉上断开条带（插入两个重复点，形成退化三角形），在后续 render 中自然断开。
     */
    public void breakStrip() {
        if (points.isEmpty()) return;
        TrailPoint last = points.getLast();
        // 插入两个相同的点，复制位置和时间戳
        points.addLast(new TrailPoint(new Vector3f(last.position), last.createTime));
        points.addLast(new TrailPoint(new Vector3f(last.position), last.createTime));
    }

    /**
     * 视觉上断开条带（直接向缓冲区写入两个透明顶点，立即断开）
     * 注意：此方法会直接向当前 VertexConsumer 写入顶点，应放在两次 render 调用之间使用。
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

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, float currentTime) {
        // 移除过期点
        points.removeIf(p -> currentTime - p.createTime > lifetime);
        if (points.size() < 2) return;

        // 获取原始点列表（已按时间排序）
        List<TrailPoint> rawPoints = new ArrayList<>(points);

        // ========== 基于距离的 Catmull-Rom 插值 ==========
        List<TrailPoint> interpolatedPoints = new ArrayList<>();
        for (int i = 0; i < rawPoints.size() - 1; i++) {
            TrailPoint p0 = rawPoints.get(i);
            TrailPoint p1 = rawPoints.get(i + 1);
            Vector3f pos0 = p0.position;
            Vector3f pos1 = p1.position;
            float time0 = p0.createTime;
            float time1 = p1.createTime;

            float distance = pos0.distance(pos1);
            // 计算需要插入的插值点数量（至少1个点，如果距离超过阈值）
            int segments = (int) Math.ceil(distance / maxSegmentLength);
            int insertCount = segments - 1; // 需要插入的点数

            // 添加当前点（p0）
            interpolatedPoints.add(p0);

            // 获取 Catmull-Rom 所需的辅助点（边界处用自身代替）
            TrailPoint prev = (i > 0) ? rawPoints.get(i - 1) : p0;
            TrailPoint next = (i + 2 < rawPoints.size()) ? rawPoints.get(i + 2) : p1;

            // 插入插值点
            for (int s = 1; s <= insertCount; s++) {
                float t = (float) s / (insertCount + 1); // 插值参数 0~1 之间
                Vector3f interpPos = catmullRom(prev.position, pos0, pos1, next.position, t);
                float interpTime = time0 + (time1 - time0) * t;
                interpolatedPoints.add(new TrailPoint(interpPos, interpTime));
            }
        }
        // 添加最后一个点
        interpolatedPoints.add(rawPoints.get(rawPoints.size() - 1));

        // ========== 可选：再次过滤距离过近的点（避免退化） ==========
        List<TrailPoint> finalPoints = new ArrayList<>();
        Vector3f lastPos = null;
        for (TrailPoint tp : interpolatedPoints) {
            if (lastPos == null || lastPos.distanceSquared(tp.position) >= 0.0025f) {
                finalPoints.add(tp);
                lastPos = tp.position;
            }
        }
        int size = finalPoints.size();
        if (size < 2) return;

        Matrix4f matrix = poseStack.last().pose();
        Vector3f lastTangent = null;
        Vector3f lastNormal = null;

        for (int i = 0; i < size; i++) {
            TrailPoint tp = finalPoints.get(i);
            Vector3f p = tp.position;

            // 计算切线（中央差分）
            Vector3f tangent;
            if (i == 0) {
                tangent = new Vector3f(finalPoints.get(1).position).sub(p).normalize();
            } else if (i == size - 1) {
                tangent = new Vector3f(p).sub(finalPoints.get(i - 1).position).normalize();
            } else {
                tangent = new Vector3f(finalPoints.get(i + 1).position).sub(finalPoints.get(i - 1).position).normalize();
            }
            if (tangent.lengthSquared() < 1e-6) {
                if (lastTangent != null) tangent.set(lastTangent);
                else continue;
            }

            // 法线计算（四元数累积）
            Vector3f normal;
            if (lastTangent == null) {
                Vector3f up = new Vector3f(0, 1, 0);
                normal = up.cross(tangent).normalize();
                if (normal.lengthSquared() < 1e-6) normal.set(1, 0, 0);
            } else {
                float dot = lastTangent.dot(tangent);
                float angle = (float) Math.acos(Math.min(1, Math.max(-1, dot)));
                if (angle < 1e-6) {
                    normal = lastNormal;
                } else {
                    Vector3f rotAxis = lastTangent.cross(tangent).normalize();
                    Quaternionf quat = new Quaternionf().rotateAxis(angle, rotAxis);
                    normal = new Vector3f(lastNormal).rotate(quat);
                    // 投影到切平面
                    normal.sub(tangent.mul(normal.dot(tangent))).normalize();
                    if (normal.lengthSquared() < 1e-6) {
                        Vector3f up = new Vector3f(0, 1, 0);
                        normal = up.cross(tangent).normalize();
                        if (normal.lengthSquared() < 1e-6) normal.set(1, 0, 0);
                    }
                }
            }
            lastNormal = normal;
            lastTangent = tangent;

            // 左右偏移
            float halfWidth = width * 0.5f;
            Vector3f left = new Vector3f(normal).mul(halfWidth).add(p);
            Vector3f right = new Vector3f(normal).mul(-halfWidth).add(p);

            // 透明度计算
            float age = currentTime - tp.createTime;
            float progress = Math.min(1f, Math.max(0f, age / lifetime));
            float alpha = progressCurve.apply(progress);
            float u = i / (float) (size - 1);

            consumer.addVertex(matrix, left.x, left.y, left.z)
                    .setColor(r, g, b, alpha)
                    .setUv(u, 0)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(packedLight)
                    .setNormal(poseStack.last(), normal.x, normal.y, normal.z);
            consumer.addVertex(matrix, right.x, right.y, right.z)
                    .setColor(r, g, b, alpha)
                    .setUv(u, 1)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(packedLight)
                    .setNormal(poseStack.last(), normal.x, normal.y, normal.z);
        }
    }

    private Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float x = Mth.catmullrom(t, p0.x, p1.x, p2.x, p3.x);
        float y = Mth.catmullrom(t, p0.y, p1.y, p2.y, p3.y);
        float z = Mth.catmullrom(t, p0.z, p1.z, p2.z, p3.z);
        return new Vector3f(x, y, z);
    }

    public void clear() {
        points.clear();
    }

    public int getPointCount() {
        return points.size();
    }
}