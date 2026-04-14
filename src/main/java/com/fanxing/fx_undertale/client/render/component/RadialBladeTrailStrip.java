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
 * 简化版径向拖尾条带 - 专用于规则圆周运动（如绕中心自旋的刀光），由刀柄向着刀尖方向
 * Blade 形式：宽度方向就是径向方向，即在水平面内垂直于切线方向（运动方向）
 * 因径向方向就是宽度方向所以不用取两个点计算切线方向再计算出宽度方向，直接用径向方向
 */
public class RadialBladeTrailStrip extends AbstractPointTrail<RadialBladeTrailStrip>{
    public float rootOffset = 0.0f;   // 根部偏移因子 (0=中心, 1=轨迹点)
    public float tipOffset = 1.0f;    // 尖端偏移因子

    public RadialBladeTrailStrip(float lifetime, float r, float g, float b, float a, Function<Float, Float> progressCurve, Function<Float, Float> uAlphaCurve) {
        super(lifetime, r, g, b, a, progressCurve, uAlphaCurve);
    }
    public RadialBladeTrailStrip(float lifetime) {
        super(lifetime);
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


    @Override
    public void render(List<TrailPoint> list,PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer consumer,
                       int packedLight, float currentTime) {
        int size = list.size();
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < size; i++) {
            TrailPoint tp = list.get(i);
            Vector3f p = tp.position;
            float age = currentTime - tp.createTime;
            float progress = Mth.clamp(age/lifetime,0f,1f);
            float alpha = progressCurve.apply(progress);
            float u = i / (float)(size - 1);  // 直接使用索引比例，空间渐变

            Vector3f radial = new Vector3f(p).sub(this.center);
            Vector3f inner = new Vector3f(this.center).add(radial.mul(rootOffset, new Vector3f()));
            Vector3f outer = new Vector3f(this.center).add(radial.mul(tipOffset, new Vector3f()));
            float finalAlpha = alpha * uAlphaCurve.apply(u);
            Vector3f dummyNormal = new Vector3f(0, 1, 0);
            addVertex(consumer, matrix, outer, finalAlpha, u, 0, packedLight, dummyNormal);
            addVertex(consumer, matrix, inner, finalAlpha, u, 1, packedLight, dummyNormal);
        }
        endBatch(bufferSource);
    }
}