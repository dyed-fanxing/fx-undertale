package com.fanxing.fx_undertale.client.render.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 * 简化版径向拖尾条带 - 专用于规则圆周运动（如绕中心自旋的刀光），由刀柄向着刀尖方向
 * Blade 形式：宽度方向就是径向方向，即在水平面内垂直于切线方向（运动方向）
 * 因径向方向就是宽度方向所以不用取两个点计算切线方向再计算出宽度方向，直接用径向方向
 */
public class RadialBladeTrailStrip extends AbstractPointTrail{
    private static final Logger log = LoggerFactory.getLogger(RadialBladeTrailStrip.class);
    public float rootOffset = 0.0f;   // 根部偏移因子 (0=中心, 1=轨迹点)
    public float tipOffset = 1.0f;    // 尖端偏移因子


    public RadialBladeTrailStrip(float lifetime, float r, float g, float b, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        super(lifetime, r, g, b, progressCurve, uAlphaCurve);
    }

    public RadialBladeTrailStrip(float lifetime, float r, float g, float b, Function<Float, Float> progressCurve) {
        super(lifetime, r, g, b, progressCurve);
    }

    public RadialBladeTrailStrip(float lifetime, int color, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        super(lifetime, color, progressCurve, uAlphaCurve);
    }

    public RadialBladeTrailStrip(float lifetime, int color, Function<Float, Float> progressCurve) {
        super(lifetime, color, progressCurve);
    }

    public RadialBladeTrailStrip(float lifetime, int color) {
        super(lifetime, color);
    }

    public RadialBladeTrailStrip offset(float rootOffset, float tipOffset) {
        this.rootOffset = rootOffset;
        this.tipOffset = tipOffset;
        return this;
    }

    public RadialBladeTrailStrip tipInflate(float width) {
        this.rootOffset = 1.0f - width;
        this.tipOffset = 1.0f + width;
        return this;
    }


    public void render(Vector3f centerOverride, PoseStack poseStack, VertexConsumer consumer, int packedLight, float currentTime) {
        if (centerOverride != null) this.center = centerOverride;
        points.removeIf(p -> currentTime - p.createTime > lifetime);
        if (points.size() < 2) return;

        List<TrailPoint> list = getSmoothPoints();
        int size = list.size();
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < size; i++) {
            TrailPoint tp = list.get(i);
            Vector3f p = tp.position;
            float age = currentTime - tp.createTime;
            float progress = Math.min(1f, Math.max(0f, age / lifetime));
            float alpha = progressCurve.apply(progress);
            float u = i / (float)(size - 1);  // 直接使用索引比例

            Vector3f radial = new Vector3f(p).sub(this.center);
            Vector3f inner = new Vector3f(this.center).add(radial.mul(rootOffset, new Vector3f()));
            Vector3f outer = new Vector3f(this.center).add(radial.mul(tipOffset, new Vector3f()));
            float finalAlpha = alpha * uAlphaCurve.apply(u);
            finalAlpha = Mth.clamp(finalAlpha, 0.0f, 1.0f);

            Vector3f dummyNormal = new Vector3f(0, 1, 0);
            addVertex(consumer, matrix, outer, finalAlpha, u, 0, packedLight, dummyNormal);
            addVertex(consumer, matrix, inner, finalAlpha, u, 1, packedLight, dummyNormal);
        }
        breakStrip(poseStack,consumer);
    }


    @Override
    protected void addVertex(VertexConsumer consumer, Matrix4f matrix, Vector3f pos, float alpha, float u, float v, int packedLight, Vector3f normal) {
        // 添加：将 finalAlpha 作为亮度因子，同时保留 alpha 为 1（或也保留 finalAlpha 但 RGB 预乘）
        // 亮度从 1 到 0
        float finalR = r * alpha;
        float finalG = g * alpha;
        float finalB = b * alpha;
        consumer.addVertex(matrix, pos.x, pos.y, pos.z)
                .setColor(finalR, finalG, finalB, alpha)
                .setUv(u, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(normal.x, normal.y, normal.z);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, float currentTime) {
        render(null, poseStack, consumer, packedLight, currentTime);
    }
}