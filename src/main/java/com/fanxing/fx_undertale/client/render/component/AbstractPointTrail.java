package com.fanxing.fx_undertale.client.render.component;

import com.fanxing.fx_undertale.utils.Curve3DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractPointTrail {
    private static final Logger log = LoggerFactory.getLogger(AbstractPointTrail.class);
    public LinkedList<TrailPoint> points = new LinkedList<>();
    public float lifetime;

    public float r, g, b;
    public Function<Float, Float> progressCurve;        // 基于生命周期控制点的透明度曲线，时间渐变
    public Function<Float, Float> uAlphaCurve;          // 基于纹理u坐标控制点的透明度曲线，空间渐变，两着可以组合使用
    public Vector3f center = new Vector3f();
    protected float interpolationSpacing = 0.06f; // 插值最大间距，可调整
    public boolean isAdditive = true; //是否为加法混合

    public AbstractPointTrail(float lifetime, float r, float g, float b, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.lifetime = lifetime;
        this.progressCurve = progressCurve;
        this.uAlphaCurve = uAlphaCurve;
    }

    // 兼容旧构造（默认 u 曲线为线性 u -> u）
    public AbstractPointTrail(float lifetime, float r, float g, float b, Function<Float, Float> progressCurve) {
        this(lifetime, r, g, b, progressCurve, u -> 1f);

    }
    public AbstractPointTrail(float lifetime,int color, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        this(lifetime, FastColor.ARGB32.red(color) / 255f, FastColor.ARGB32.green(color) / 255f, FastColor.ARGB32.blue(color) / 255f, progressCurve, uAlphaCurve);
    }
    public AbstractPointTrail(float lifetime,int color, Function<Float, Float> progressCurve) {
        this(lifetime, color, progressCurve, u -> u);
    }
    public AbstractPointTrail(float lifetime,int color) {
        this(lifetime,color, progress -> 1f - progress, u -> 1f);
    }
    public AbstractPointTrail isAdditive(boolean isAdditive) {
        this.isAdditive = isAdditive;
        return this;
    }

    public AbstractPointTrail interpolationSpacing(float interpolationSpacing) {
        this.interpolationSpacing = interpolationSpacing;
        return this;
    }



    public void addPoint(Vector3f point, float time) {
        if (!points.isEmpty()) {
            Vector3f last = points.getLast().position;
            if (last.distanceSquared(point) < 0.0025F) return;
        }
        points.addLast(new TrailPoint(point, time));
    }
    public void addPoint(Vector3d point, float time) {
        addPoint(point.get(new Vector3f()), time);
    }
    public List<TrailPoint> getSmoothPoints() {
        // 原始点列表
        List<TrailPoint> rawPoints = new ArrayList<>(points);
        // 自适应 Catmull-Rom 插值
        List<TrailPoint> smoothPoints = new ArrayList<>();
        for (int i = 0; i < rawPoints.size() - 1; i++) {
            TrailPoint tp0 = rawPoints.get(i);
            TrailPoint tp1 = rawPoints.get(i + 1);
            Vector3f p0 = tp0.position, p1 = tp1.position;
            float t0 = tp0.createTime, t1 = tp1.createTime;
            float dist = p0.distance(p1);
            int needed = (int) Math.ceil(dist / interpolationSpacing) - 1;
            int subdivisions = Math.min(needed, 20);
            // 获取前后控制点
            TrailPoint tpPrev = (i > 0) ? rawPoints.get(i - 1) : tp0;
            TrailPoint tpNext = (i + 2 < rawPoints.size()) ? rawPoints.get(i + 2) : tp1;
            Vector3f pPrev = tpPrev.position, pNext = tpNext.position;
            smoothPoints.add(tp0);
            if (subdivisions > 0) {
                for (int s = 1; s <= subdivisions; s++) {
                    float t = (float) s / (subdivisions + 1);
                    Vector3f pos = Curve3DUtils.catmullRom(pPrev, p0, p1, pNext, t);
                    float time = t0 + (t1 - t0) * t;
                    smoothPoints.add(new TrailPoint(pos, time));
                }
            }
        }
        smoothPoints.add(rawPoints.getLast());
        return smoothPoints;
    }
    
    public void breakStrip(PoseStack poseStack, VertexConsumer consumer) {
        if (points.isEmpty()) return;
//        TrailPoint last = points.getLast();
//        Matrix4f matrix4f = poseStack.last().pose();
//        addVertex(consumer, matrix4f,last.position, 0, 0, 0, 0, new Vector3f());
//        addVertex(consumer, matrix4f,last.position, 0, 0, 0, 0, new Vector3f());

        // 利用单位矩阵直接将两个透明的顶点提交到世界坐标原点上，因为距离过远超出视锥体范围，会被剔除或遮挡的原理，隐藏和下一个条带形成的三角形
        // 把这个两个用来断开的点叫做 AE1和A2E，而另一个条带的两个起点叫做BS1,BS2，
        // 组成三角形为：AE1 AE2 BS1（退化成线段，不渲染），AE2 BS1 BS2（三角形，会渲染，但因为超出视锥体范围，被剔除或遮挡，所以也不渲染）
        // 必须用单位矩阵，且用原点(0,0,0)，且必须完全透明才行，三个差一个都不对
        Vector3f vector3f = new Vector3f(0, 0, 0);
        Matrix4f matrix4f = new Matrix4f();
        addVertex(consumer, matrix4f, vector3f, 0F, 0, 0, 0, new Vector3f());
        addVertex(consumer, matrix4f, new Vector3f(0, 0, 0), 0F, 0, 0, 0, new Vector3f());
    }



    protected void addVertex(VertexConsumer consumer, Matrix4f matrix, Vector3f pos, float alpha,
                           float u, float v, int packedLight, Vector3f normal) {
        // 预乘 alpha，支持加法混合下的淡出
        float rMul = r ;
        float gMul = g;
        float bMul = b;
        if(isAdditive){
            rMul *= alpha;
            gMul *= alpha;
            bMul *= alpha;
        }

        consumer.addVertex(matrix, pos.x, pos.y, pos.z)
                .setColor(1.0f, 0.0f, 0.0f, 1.0f) // 完全不透明的红色
//                .setColor(rMul, gMul, bMul, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(normal.x, normal.y, normal.z);
    }


    public Vector3f getLastPosition() {
        return points.isEmpty() ? null : points.getLast().position;
    }


    public void setCenter(Vector3d center) {
        this.center = center.get(new Vector3f());
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
