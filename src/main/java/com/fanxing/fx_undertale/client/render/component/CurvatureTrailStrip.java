package com.fanxing.fx_undertale.client.render.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * 基于瞬时曲率中心的刀光条带。
 * 根据轨迹点序列动态计算每个点的曲率中心（三点定圆），
 * 宽度方向为从曲率中心指向轨迹点的径向，适合任意曲线运动（如多关节挥砍）。
 */
public class CurvatureTrailStrip {
    public LinkedList<TrailPoint> points = new LinkedList<>();
    public float lifetime;               // 每个点的存活时间（秒）
    public float radialScale;            // 径向延伸长度（相对于轨迹点到曲率中心的距离的比例）
    public float r, g, b;
    public Function<Float, Float> progressCurve;  // 基于年龄的透明度曲线
    public Function<Float, Float> uAlphaCurve;     // 基于 u 的透明度曲线

    // 缓存上一帧的曲率中心，用于平滑
    private Vector3f prevCenter = null;

    public record TrailPoint(Vector3f position, float createTime) {}

    // 构造方法
    public CurvatureTrailStrip(float lifetime, float r, float g, float b,
                               Function<Float, Float> progressCurve,
                               Function<Float, Float> uAlphaCurve) {
        this.lifetime = lifetime;
        this.r = r;
        this.g = g;
        this.b = b;
        this.progressCurve = progressCurve;
        this.uAlphaCurve = uAlphaCurve;
    }

    public CurvatureTrailStrip(float lifetime, int color,
                               Function<Float, Float> progressCurve,
                               Function<Float, Float> uAlphaCurve) {
        this(lifetime,
                FastColor.ARGB32.red(color) / 255f,
                FastColor.ARGB32.green(color) / 255f,
                FastColor.ARGB32.blue(color) / 255f,
                progressCurve, uAlphaCurve);
    }

    public CurvatureTrailStrip(float lifetime, int color, Function<Float, Float> progressCurve) {
        this(lifetime, color, progressCurve, u -> u);
    }

    public CurvatureTrailStrip(float lifetime, int color) {
        this(lifetime, color, progress -> 1f - progress, u -> u);
    }

    public CurvatureTrailStrip radialScale(float radialScale) {
        this.radialScale = radialScale;
        return this;
    }

    public void addPoint(Vector3f point, float time) {
        if (!points.isEmpty()) {
            Vector3f last = points.getLast().position;
            if (last.distanceSquared(point) < 0.0025f) return;
        }
        points.addLast(new TrailPoint(point, time));
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

    public void breakStrip(PoseStack poseStack, VertexConsumer consumer, int packedLight) {
        if (points.isEmpty()) return;
        Vector3f lastPos = points.getLast().position;
        consumer.addVertex(poseStack.last().pose(), lastPos.x, lastPos.y, lastPos.z)
                .setColor(0, 0, 0, 0)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0, 1, 0);
        consumer.addVertex(poseStack.last().pose(), lastPos.x, lastPos.y, lastPos.z)
                .setColor(0, 0, 0, 0)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0, 1, 0);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, float currentTime) {
        render(null, poseStack, consumer, packedLight, currentTime);
    }

    public void render(Vector3f centerOverrideIgnored, PoseStack poseStack, VertexConsumer consumer,
                       int packedLight, float currentTime) {
        // 移除过期点
        points.removeIf(p -> currentTime - p.createTime > lifetime);
        if (points.size() < 2) return;

        List<TrailPoint> list = new LinkedList<>(points);
        int size = list.size();
        Matrix4f matrix = poseStack.last().pose();

        // 预计算每个点的年龄透明度
        float[] alphas = new float[size];
        for (int i = 0; i < size; i++) {
            float age = currentTime - list.get(i).createTime;
            float progress = Math.min(1f, Math.max(0f, age / lifetime));
            alphas[i] = progressCurve.apply(progress);
        }

        // 用于存储每个点的曲率中心和径向方向
        Vector3f[] centers = new Vector3f[size];
        Vector3f[] radials = new Vector3f[size];

        // 第一步：计算每个点的瞬时曲率中心（三点定圆）
        for (int i = 0; i < size; i++) {
            if (i == 0 || i == size - 1) {
                // 端点：使用相邻点的中心（如果存在）或暂用自身
                if (size > 1) {
                    if (i == 0 && size >= 2) centers[i] = (size >= 3) ? centers[1] : null;
                    else if (i == size - 1 && size >= 2) centers[i] = (size >= 3) ? centers[size-2] : null;
                }
                continue;
            }
            Vector3f p0 = list.get(i-1).position;
            Vector3f p1 = list.get(i).position;
            Vector3f p2 = list.get(i+1).position;

            // 计算圆心（在 XZ 平面投影，Y 轴取 p1.y）
            Vector3f center = computeCircleCenter(p0, p1, p2);
            if (center == null) {
                // 共线或退化：使用上一帧的中心
                if (prevCenter != null) center = new Vector3f(prevCenter);
                else center = new Vector3f(p1); // fallback: 圆心就是自身（径向为零，后续会退化）
            }
            centers[i] = center;
        }

        // 平滑曲率中心（指数移动平均）
        if (prevCenter == null && centers[1] != null) prevCenter = new Vector3f(centers[1]);
        for (int i = 0; i < size; i++) {
            if (centers[i] == null) continue;
            float smooth = 0.85f; // 平滑因子
            Vector3f smoothed = new Vector3f(prevCenter).mul(smooth).add(new Vector3f(centers[i]).mul(1 - smooth));
            centers[i] = smoothed;
            prevCenter = new Vector3f(smoothed);
        }

        // 第二步：计算径向方向（从曲率中心指向轨迹点）
        for (int i = 0; i < size; i++) {
            Vector3f p = list.get(i).position;
            if (centers[i] == null) {
                // 无中心点，径向为零向量，后续会 fallback
                radials[i] = new Vector3f(1, 0, 0);
                continue;
            }
            Vector3f radial = new Vector3f(p).sub(centers[i]);
            if (radial.lengthSquared() < 1e-6f) radial.set(1, 0, 0);
            else radial.normalize();
            radials[i] = radial;
        }

        // 第三步：生成顶点（每个点生成外点 p 和内点 inner）
        for (int i = 0; i < size; i++) {
            Vector3f p = list.get(i).position;
            Vector3f radial = radials[i];
            // 内点：从轨迹点沿径向的反方向（指向曲率中心）移动 radialScale * 距离
            // 注意：径向单位向量从中心指向外，所以向内移动需要取反
            Vector3f inner = new Vector3f(radial).mul(-radialScale).add(p);
            Vector3f outer = p;

            float u = i / (float) (size - 1);
            float alpha = alphas[i];
            float uFactor = uAlphaCurve.apply(u);
            float finalAlpha = alpha * uFactor;
            finalAlpha = Mth.clamp(finalAlpha, 0.0f, 1.0f);

            Vector3f dummyNormal = new Vector3f(0, 1, 0);
            consumer.addVertex(matrix, outer.x, outer.y, outer.z)
                    .setColor(r, g, b, finalAlpha)
                    .setUv(u, 0)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(packedLight)
                    .setNormal(poseStack.last(), dummyNormal.x, dummyNormal.y, dummyNormal.z);
            consumer.addVertex(matrix, inner.x, inner.y, inner.z)
                    .setColor(r, g, b, finalAlpha)
                    .setUv(u, 1)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(packedLight)
                    .setNormal(poseStack.last(), dummyNormal.x, dummyNormal.y, dummyNormal.z);
        }
    }

    /**
     * 计算过三点 p0, p1, p2 的圆心（在 XZ 平面投影，Y 坐标取 p1.y）。
     * 返回圆心向量，如果三点共线则返回 null。
     */
    private Vector3f computeCircleCenter(Vector3f p0, Vector3f p1, Vector3f p2) {
        // 投影到 XZ 平面
        float x0 = p0.x, z0 = p0.z;
        float x1 = p1.x, z1 = p1.z;
        float x2 = p2.x, z2 = p2.z;

        float a11 = 2 * (x1 - x0);
        float a12 = 2 * (z1 - z0);
        float b1 = x1*x1 - x0*x0 + z1*z1 - z0*z0;
        float a21 = 2 * (x2 - x1);
        float a22 = 2 * (z2 - z1);
        float b2 = x2*x2 - x1*x1 + z2*z2 - z1*z1;

        float det = a11 * a22 - a12 * a21;
        if (Math.abs(det) < 1e-6f) return null; // 共线

        float cx = (b1 * a22 - a12 * b2) / det;
        float cz = (a11 * b2 - b1 * a21) / det;
        return new Vector3f(cx, p1.y, cz);
    }

    public Vector3f getLastPosition() {
        return points.isEmpty() ? null : points.getLast().position;
    }

    public void setColor(int r, int g, int b) {
        this.r = r / 255f;
        this.g = g / 255f;
        this.b = b / 255f;
    }

    public void setColor(int color) {
        this.r = FastColor.ARGB32.red(color) / 255f;
        this.g = FastColor.ARGB32.green(color) / 255f;
        this.b = FastColor.ARGB32.blue(color) / 255f;
    }
}