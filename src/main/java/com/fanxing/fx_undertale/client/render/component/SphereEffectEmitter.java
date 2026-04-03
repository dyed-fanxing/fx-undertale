package com.fanxing.fx_undertale.client.render.component;

import com.fanxing.fx_undertale.utils.CurvesUtils;
import com.fanxing.fx_undertale.utils.RenderUtils;
import com.fanxing.fx_undertale.utils.RotUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

/**
 * 球体特效发射器 - 从球体表面向外发射拉伸条带
 * 支持自定义长度变化曲线。
 */
public class SphereEffectEmitter {
    // 特效数据记录
    private record EffectData(
            Vec3 startPos,
            Vec3 direction,
            float length,
            float startTime,
            int lifeTime,
            float width
    ) {}

    private final List<EffectData> effects = new LinkedList<>();
    private int nextIndex = 0;
    private final Random random = new Random();

    // 可配置参数
    public float density = 0.15f;
    public int lifetime = 1;
    public int lifetimeRandomRange = 10;
    public float width = 0.03f;
    public float widthRandomRange = 0.03f;
    public float outerLength = 1.5f;
    public float outerLengthRandomRange = 3.0f;

    // 长度曲线函数：输入 progress (0~1)，输出长度因子 (0~1)
    public Function<Float, Float> widthCurve = (t) -> 1.0f;;

    public void setSeed(UUID uuid) {
        long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        random.setSeed(seed);
        clear();
    }

    public void clear() {
        effects.clear();
        nextIndex = 0;
    }

    public void stretchCircleRender(PoseStack poseStack, VertexConsumer consumer, float animTick,float radius, float innerRadius, int[] color) {
        while (true) {
            float startTime = nextIndex * density / radius;
            if (startTime > animTick) break;
            int lifeTime = lifetime + random.nextInt(lifetimeRandomRange + 1);
            float rayOuterLength = radius * (outerLength + random.nextFloat() * outerLengthRandomRange);
            float rayWidth = radius * (this.width + random.nextFloat() * widthRandomRange);
            float rayLength = rayOuterLength - innerRadius;
            if (rayLength <= 0) rayLength = 0.01f;
            double theta = random.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * random.nextDouble() - 1) - Math.PI / 2;
            Vec3 direction = new Vec3(
                    Math.cos(theta) * Math.cos(phi),
                    Math.sin(phi),
                    Math.sin(theta) * Math.cos(phi)
            );
            Vec3 startPos = direction.scale(rayOuterLength);
            Vec3 dirIn = direction.scale(-1);
            effects.add(new EffectData(startPos, dirIn, rayLength, startTime, lifeTime, rayWidth));
            nextIndex++;
        }

        // 渲染并删除过期
        Iterator<EffectData> it = effects.iterator();
        while (it.hasNext()) {
            EffectData ef = it.next();
            float progress = (animTick - ef.startTime) / ef.lifeTime;
            if (progress >= 1.0f) {
                it.remove();
                continue;
            }
            // 移动：线性向内
            float moveDist = ef.length * progress;
            Vec3 currentPos = ef.startPos.add(ef.direction.scale(moveDist));
            // 拉伸量：由曲线决定（0→1→0）
            float stretch = (1 - progress) * (ef.width + ef.length * progress);
            // 宽度：恒定或由宽度曲线决定
            float currentWidth = ef.width * widthCurve.apply(progress);

            poseStack.pushPose();
            poseStack.translate(currentPos.x, currentPos.y, currentPos.z);
            poseStack.mulPose(RotUtils.rotation(new Vec3(0, 0, 1), ef.direction));
            poseStack.scale(currentWidth, currentWidth, stretch);
            RenderUtils.renderCircle(poseStack.last(), consumer, new Vec3(0, 0, -0.5), 1.0f, 16, new Vec3(0, 0, 1),
                    color[0], color[1], color[2], color[3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
    }
}