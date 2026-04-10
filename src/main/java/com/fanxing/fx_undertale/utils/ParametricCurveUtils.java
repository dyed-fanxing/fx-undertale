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
     * 4.花瓣，半径 1
     * @param petals 花瓣数量
     */
    public static Function<Float, Vec3> flower(float petals) {
        return t -> {
            float angle = t * 2 * (float) Math.PI;
            float r = t;
            float factor = (float) Math.abs(Math.cos(petals * angle / 2));
            float finalR = r * (0.5f + 0.5f * factor);
            return new Vec3(finalR * Math.cos(angle), 0, finalR * Math.sin(angle));
        };
    }

    /** 5.爱心，半径 1（最宽处为 1） */
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

    /**
     * 6. 锯齿尖刺 (Sawtooth Radial)
     * 描述：径向直刺基础上叠加三角波，产生锐利折线攻击感，适合闪电或锯齿切割特效。
     *
     * @param waves     锯齿波数量（频率）。建议 3.0 ~ 8.0。
     * @param amplitude 摆动幅度（相对于半径 1）。建议 0.2 ~ 0.6。
     */
    public static Function<Float, Vec3> sawtoothRadial(float waves, float amplitude) {
        return t -> {
            float x = t;
            float z = 0.0f;
            if (amplitude > 0.001f && waves > 0.001f) {
                float phase = t * waves * 2.0f * (float) Math.PI;
                // 三角波：在 0~2PI 范围内产生 -1..1 的尖锐折线
                float frac = (phase % (2.0f * (float) Math.PI)) / (2.0f * (float) Math.PI);
                float tri = 1.0f - Math.abs(2.0f * (frac - 0.5f));
                float sharpWave = tri * 2.0f - 1.0f;
                float envelope = t;  // 甩鞭效果
                z = sharpWave * amplitude * envelope;
            }
            return new Vec3(x, 0, z);
        };
    }

    /**
     * 7. 星爆脉冲 (StarBurst)
     * 描述：径向角度随 t 线性增长，同时半径受正弦调制，形成星形辐射爆裂效果。
     *
     * @param spikes    星芒数量（即瓣数）。建议 5 ~ 12。
     * @param sharpness 锐度系数（0~1），控制星形凹陷程度。建议 0.4 ~ 0.9。
     */
    public static Function<Float, Vec3> starburst(float spikes, float sharpness) {
        return t -> {
            float angle = t * 2.0f * (float) Math.PI;
            // 径向调制：基础半径 t，叠加正弦绝对值产生的星芒
            float radialMod = 1.0f + sharpness * Math.abs((float) Math.sin(spikes * angle / 2.0f)) * 0.8f;
            float r = t * radialMod;
            r = Math.min(r, 1.0f); // 限制最大半径
            return new Vec3(r * (float) Math.cos(angle), 0, r * (float) Math.sin(angle));
        };
    }

    /**
     * 8. 折叠波刃 (WaveFold)
     * 描述：沿 X 轴方向叠加多层正弦波，产生类似折叠刀锋或波纹剑刃的形态，所有点位于 Y=0 平面。
     *
     * @param freq1     主波频率。建议 4 ~ 8。
     * @param freq2     次波频率（可为 freq1 的倍数）。建议 6 ~ 12。
     * @param amplitude 波动幅度。建议 0.2 ~ 0.6。
     */
    public static Function<Float, Vec3> wavefold(float freq1, float freq2, float amplitude) {
        return t -> {
            float x = t;
            float phase1 = t * freq1 * 2.0f * (float) Math.PI;
            float phase2 = t * freq2 * 2.0f * (float) Math.PI;
            // 主波动 + 次波动，保留折叠感
            float wave = (float) Math.sin(phase1) * amplitude * t
                    + (float) Math.sin(phase2) * amplitude * 0.6f * t;
            // 可选：增加轻微扭转，但将其合并到 Z 轴，Y 轴保持为 0
            float twist = (float) Math.sin(t * Math.PI * 3) * 0.08f * (1.0f - t);
            float finalZ = wave + twist;
            return new Vec3(x, 0, finalZ);
        };
    }
    public static Function<Float, Vec3> wavefold(float... params) {
        return wavefold(params[0],params[1],params[2]);
    }











}