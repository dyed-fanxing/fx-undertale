package com.fanxing.fx_undertale.utils;

import net.minecraft.world.phys.Vec3;
import java.util.function.Function;

public class ParametricCurveUtils {
    public static Function<Float, Vec3> reverse(Function<Float, Vec3> curve) {
        return t -> curve.apply(1 - t);
    }

    /**
     * 1. 径向直刺 (Radial Spear) / 蛇形波动 (完全控制版)
     *
     * @param waves      波浪数量 (频率)。
     *                   建议范围：
     *                   - 1.0f ~ 3.0f: 舒缓的大弯曲 (适合点数 20-30)
     *                   - 4.0f ~ 8.0f: 密集的蛇形/电流 (需要点数 40+)
     *                   - > 10.0f: 高频震动 (需要点数 60+，否则会变成折线)
     *
     * @param amplitude  最大摆动幅度 (相对于半径 1 的比例)。
     *                   建议范围：
     *                   - 0.05f: 微颤
     *                   - 0.20f: 标准蛇形
     *                   - 0.50f+: 剧烈狂舞 (可能会自我交叉)
     */
    public static Function<Float, Vec3> radial(float waves, float amplitude) {
        return t -> {
            float x = t;
            float z = 0.0f;

            if (amplitude > 0.001f && waves > 0.001f) {
                // 1. 计算相位：t * 波数 * 2PI
                float phase = t * waves * 2.0f * (float) Math.PI;

                // 2. 基础正弦波
                float sine = (float) Math.sin(phase);

                // 3. 包络 (Envelope)：让摆动随半径线性增长 (甩鞭子效果)
                // 如果你希望从头到尾幅度一致，可以把这里改成 1.0f
                float envelope = t;

                // 4. 最终偏移
                z = sine * amplitude * envelope;
            }

            return new Vec3(x, 0, z);
        };
    }
    // --- 便捷重载方法 ---
    // 只指定振幅，频率默认为最平滑的 2.5
    public static Function<Float, Vec3> radial(float amplitude) {
        return radial(2.5f, amplitude);
    }
    // 无参，默认直线
    public static Function<Float, Vec3> radial() {
        return radial(0.0f);
    }

    /**
     * 2. 经典螺旋 (Classic Spiral)
     * 描述：阿基米德螺旋，半径随角度线性增加。
     * @param turns 螺旋的圈数 (例如 3.0f 表示转 3 圈)
     */
    public static Function<Float, Vec3> spiral(float turns) {
        return t -> {
            // 角度 = t * 2PI * 圈数
            float angle = t * 2.0f * (float) Math.PI * turns;
            // 半径 = t (线性增长)
            float r = t;
            return new Vec3(r * Math.cos(angle), 0, r * Math.sin(angle));
        };
    }
    /**
     * 3. 分形折叠 (Fractal Fold) / 闪电效果
     * 描述：模拟折线或闪电。半径线性增长，角度在分段节点处发生突变。
     *
     * @param segments  折叠的段数 (例如 5-8 段)。段数越多，折线越细碎。
     * @param sharpness 折叠的锐度系数 (0.0 - 1.0)。
     *                  0.0 = 直线
     *                  0.5 = 适度曲折
     *                  1.0 = 剧烈随机转折 (可能回头)
     */
    public static Function<Float, Vec3> fractal(float segments, float sharpness) {
        return t -> {
            float r = t;
            float segmentIdx = Math.min(t * segments, segments - 0.0001f);
            int currentSeg = (int) segmentIdx;
            float segProgress = segmentIdx - currentSeg;

            // 计算当前段起始角度和结束角度
            float startAngle = 0;
            float endAngle = 0;

            // 计算起始角度（前面所有段的累积）
            for (int i = 0; i < currentSeg; i++) {
                float pseudoRandom = (float) Math.sin(i * 12.9898f) * 2.0f - 1.0f;
                startAngle += pseudoRandom * sharpness * (float) Math.PI / 3.0f;
            }

            // 计算结束角度（包含当前段）
            for (int i = 0; i <= currentSeg; i++) {
                float pseudoRandom = (float) Math.sin(i * 12.9898f) * 2.0f - 1.0f;
                endAngle += pseudoRandom * sharpness * (float) Math.PI / 3.0f;
            }

            // 段内角度线性插值（这就是关键！）
            float totalAngle = startAngle + (endAngle - startAngle) * segProgress;

            float x = r * (float) Math.cos(totalAngle);
            float z = r * (float) Math.sin(totalAngle);

            return new Vec3(x, 0, z);
        };
    }

    /**
     * 4. 布朗漂移 (Brownian Drift)
     * 描述：模拟随机游走。半径总体向外，但角度带有累积的随机噪声。
     * 注意：由于 Function 是纯函数的，不能使用 Math.random() (否则每帧重绘轨迹会变)。
     * 我们使用确定性的伪随机噪声 (基于 t 的正弦叠加) 来模拟随机性。
     * @param intensity 扰动强度 (0.0 - 1.0)
     */
    public static Function<Float, Vec3> brownian(float intensity) {
        return t -> {
            float r = t;
            // 基础角度均匀分布
            float baseAngle = t * 2.0f * (float) Math.PI;
            // 确定性噪声模拟布朗运动
            // 叠加多个不同频率的正弦波，制造“无序”感
            // 系数 123.4, 456.7 等是随意选取的质数倍率，用于去相关
            float noise = 0.0f;
            noise += (float) (Math.sin(t * 13.0f) * 0.5f);
            noise += (float) (Math.sin(t * 37.0f) * 0.25f);
            noise += (float) (Math.sin(t * 89.0f) * 0.125f);

            // 噪声累积效应：越往外 (t 越大)，累积偏移可能越大，但也受强度控制
            // 这里做一个简单的映射，让角度产生抖动
            float angleOffset = noise * intensity * 2.0f * (float) Math.PI;

            // 限制最大偏移，防止绕回中心太乱
            angleOffset = angleOffset % (2.0f * (float) Math.PI);

            float finalAngle = baseAngle + angleOffset;

            return new Vec3(r * Math.cos(finalAngle), 0, r * Math.sin(finalAngle));
        };
    }



    /** 星形，半径 1 */
    public static Function<Float, Vec3> star(float points, float depth) {
        return t -> {
            float angle = t * 2 * (float) Math.PI;
            float r = t;
            float offset = depth * (float) Math.cos(points * angle);
            float finalR = r * (1 - depth + offset);
            return new Vec3(finalR * Math.cos(angle), 0, finalR * Math.sin(angle));
        };
    }

    /** 花瓣，半径 1 */
    public static Function<Float, Vec3> flower(float petals) {
        return t -> {
            float angle = t * 2 * (float) Math.PI;
            float r = t;
            float factor = (float) Math.abs(Math.cos(petals * angle / 2));
            float finalR = r * (0.5f + 0.5f * factor);
            return new Vec3(finalR * Math.cos(angle), 0, finalR * Math.sin(angle));
        };
    }

    /** 爱心，半径 1（最宽处为 1） */
    public static Function<Float, Vec3> heart() {
        return t -> {
            float angle = t * 2 * (float) Math.PI;
            float x = 16 * (float) Math.pow(Math.sin(angle), 3);
            float z = 13 * (float) Math.cos(angle) - 5 * (float) Math.cos(2 * angle)
                    - 2 * (float) Math.cos(3 * angle) - (float) Math.cos(4 * angle);
            // 归一化到半径 1（爱心最宽处约 16，最深约 13）
            return new Vec3(x * t / 16, 0, z * t / 13);
        };
    }

    /** 正弦波，长度 1，振幅 1 */
    public static Function<Float, Vec3> sineWave(float waves) {
        return t -> {
            float x = (t - 0.5f); // 范围 -0.5 到 0.5，总长 1
            float z = t * (float) Math.sin(t * Math.PI * 2 * waves);
            return new Vec3(x, 0, z);
        };
    }

    /** 方形，半径 1 */
    public static Function<Float, Vec3> square() {
        return t -> {
            float angle = t * 2 * (float) Math.PI;
            double x = t * Math.cos(angle);
            double z = t * Math.sin(angle);
            double max = Math.max(Math.abs(x), Math.abs(z));
            if (max > 0) {
                x = x / max * t;
                z = z / max * t;
            }
            return new Vec3(x, 0, z);
        };
    }



}