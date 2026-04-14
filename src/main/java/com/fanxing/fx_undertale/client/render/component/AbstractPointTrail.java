package com.fanxing.fx_undertale.client.render.component;

import com.fanxing.fx_undertale.utils.Curve3DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractPointTrail<T extends AbstractPointTrail<T>> {
    public LinkedList<TrailPoint> points = new LinkedList<>();
    public float lifetime = 5;
    public float r=1, g=1, b=1,a=1;
    public Function<Float, Float> progressCurve = t -> 1-t;        // 基于生命周期控制点的透明度曲线，时间渐变
    //KEY 加法混合得启用u映射，不然只用生命周期透明度曲线，最后的效果只有整体淡出，没有尾部淡出，大概因为加法混合的原因丢掉了某个透明度因子。
    public Function<Float, Float> uAlphaCurve = u -> u;          // 基于纹理u坐标控制点的透明度曲线，空间渐变，两者可以组合使用
    public Vector3f center = new Vector3f();
    protected float interpolationSpacing = 0.06f; // 插值最大间距，可调整
    public boolean isAdditive = true; //是否为加法混合
    public AbstractPointTrail(float lifetime, float r, float g, float b,float a, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.lifetime = lifetime;
        this.progressCurve = progressCurve;
        this.uAlphaCurve = uAlphaCurve;
    }
    public AbstractPointTrail(float lifetime) {
        this.lifetime = lifetime;
    }

    public T color(float r,float g,float b,float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return (T) this;
    }
    public T color(float[] color){
        this.r = color[0];
        this.g = color[1];
        this.b = color[2];
        this.a = color[3];
        return (T) this;
    }
    public T color(int r, int g, int b,int a) {
        this.r = r/255f;
        this.g = g/255f;
        this.b = b/255f;
        this.a = a/255f;
        return (T) this;
    }
    public T color(int[] color) {
        this.r = color[0]/255f;
        this.g = color[1]/255f;
        this.b = color[2]/255f;
        this.a = color[3]/255f;
        return (T) this;
    }
    public T color(int color){
        this.r = FastColor.ARGB32.red(color)/255f;
        this.g = FastColor.ARGB32.green(color)/255f;
        this.b = FastColor.ARGB32.blue(color)/255f;
        this.a = FastColor.ARGB32.alpha(color)/255f;
        return (T) this;
    }
    public T progressCurve(Function<Float, Float> progressCurve){
        this.progressCurve = progressCurve;
        return (T) this;
    }
    public T uAlphaCurve(Function<Float, Float> uAlphaCurve){
        this.uAlphaCurve = uAlphaCurve;
        return (T) this;
    }
    public T isAdditive(boolean isAdditive) {
        this.isAdditive = isAdditive;
        return (T) this;
    }
    public T interpolationSpacing(float interpolationSpacing) {
        this.interpolationSpacing = interpolationSpacing;
        return (T) this;
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

    public void render(Vector3f centerOverride, PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer consumer,int packedLight, float currentTime) {
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
        if (list.size() < 2) return;
        render(list,poseStack,bufferSource,consumer,packedLight,currentTime);
    }
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer consumer, int packedLight, float currentTime) {
        render((Vector3f) null, poseStack,bufferSource, consumer, packedLight, currentTime);
    }

    protected abstract void render(List<TrailPoint> list,PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer consumer,int packedLight, float currentTime);

    protected void addVertex(VertexConsumer consumer, Matrix4f matrix, Vector3f pos, float alpha,
                           float u, float v, int packedLight, Vector3f normal) {
        // 预乘 alpha，支持加法混合下的淡出
        float rMul = r ;
        float gMul = g;
        float bMul = b;
        if(isAdditive){ // 预乘alpha 使加法混合也能达到透明渐变的效果
            rMul *= alpha;
            gMul *= alpha;
            bMul *= alpha;
        }
        consumer.addVertex(matrix, pos.x, pos.y, pos.z)
                .setColor(rMul, gMul, bMul, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(normal.x, normal.y, normal.z);
    }

    public void endBatch( MultiBufferSource bufferSource) {
        // 如果是原版 BufferSource，直接调用
        if (bufferSource instanceof MultiBufferSource.BufferSource bs)  bs.endBatch();
        // 如果是 Iris 的包装类，先解包
        if (bufferSource instanceof net.irisshaders.iris.layer.BufferSourceWrapper wrapper) {
            MultiBufferSource original = wrapper.getOriginal();
            if (original instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch();
            }
        }
    }





    public Vector3f getLastPosition() {
        return points.isEmpty() ? null : points.getLast().position;
    }


    public void setCenter(Vector3d center) {
        this.center = center.get(new Vector3f());
    }
    public void setColor(float[] color) {
        this.r = color[0];
        this.g = color[1];
        this.b = color[2];
        this.a = color[3];
    }
    public void setColor(int r, int g, int b,int a) {
        this.r = r / 255f;
        this.g = g / 255f;
        this.b = b / 255f;
        this.a =  a / 255f;
    }
    public void setColor(int color) {
        this.r = FastColor.ARGB32.red(color) / 255f;
        this.g = FastColor.ARGB32.green(color) / 255f;
        this.b = FastColor.ARGB32.blue(color) / 255f;
        this.a = FastColor.ARGB32.alpha(color) / 255f;
    }
}
