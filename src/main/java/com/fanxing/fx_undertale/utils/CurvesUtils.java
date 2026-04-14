package com.fanxing.fx_undertale.utils;

import net.minecraft.util.Mth;

import java.util.function.Function;

public class CurvesUtils {
    public static Function<Float, Float> riseHoldFallBezierCurve(float holdTimeScale,float riseEase,float fallEase) {
        return (t) -> riseHoldFallBezier(t,holdTimeScale,riseEase,fallEase);
    }

    public static float riseHoldFallBezier(float t, float holdTimeScale,float ease) {
        return riseHoldFallBezier(t, holdTimeScale, ease,ease);
    }
    /**
     * 分段参数方程实现
     * @param t 时间 (0到1)
     * @param holdTimeScale 停留时间比例 (0到1)
     * @param riseEase 值0 缓入	值1 缓出
     * @param fallEase 值0 缓出	值1 缓入
     */
    public static float riseHoldFallBezier(float t, float holdTimeScale,float riseEase,float fallEase) {
        // 定义分段
        float riseTime = (1.0f - holdTimeScale) / 2.0f;
        if (t < riseTime) {
            return bezier(t / riseTime, 0, riseEase, 1);
        } else if (t < riseTime + holdTimeScale) {
            return 1.0f;
        } else {
            float t2 = (t - riseTime - holdTimeScale) / riseTime;
            return bezier(t2, 1, fallEase, 0);  // 从1到0的贝塞尔曲线
        }
    }
    public static float riseHoldFallDerivative(float t, float holdTimeScale, float ease) {
        return riseHoldFallDerivative(t, holdTimeScale, ease,ease);
    }
    public static float riseHoldFallDerivative(float t, float holdTimeScale, float riseEase, float fallEase) {
        float riseTime = (1.0f - holdTimeScale) / 2.0f;
        if (t < riseTime) {
            // 上升阶段：h(u) = bezier(u, 0, riseEase, 1), u = t/riseTime
            float u = t / riseTime;
            // 对 bezier(u, 0, riseEase, 1) 求导得 2*(1-u)*(riseEase-0) + 2*u*(1-riseEase)
            // 更准确：bezier'(u) = 2*(1-u)*(p1-p0) + 2*u*(p2-p1)
            float du_dt = 1.0f / riseTime;
            float hDerivU = 2 * (1 - u) * (riseEase - 0) + 2 * u * (1 - riseEase);
            return hDerivU * du_dt;
        } else if (t < riseTime + holdTimeScale) {
            // 停留阶段：高度恒为1，导数为0
            return 0f;
        } else {
            // 下降阶段：h(v) = bezier(v, 1, fallEase, 0), v = (t - riseTime - holdTimeScale)/riseTime
            float v = (t - riseTime - holdTimeScale) / riseTime;
            float dv_dt = 1.0f / riseTime;
            float hDerivV = 2 * (1 - v) * (fallEase - 1) + 2 * v * (0 - fallEase);
            return hDerivV * dv_dt;
        }
    }


    // 二次贝塞尔曲线
    public static float bezier(float t, float p0, float p1, float p2) {
        float oneMinusT = 1 - t;
        return oneMinusT * oneMinusT * p0 + 2 * oneMinusT * t * p1 + t * t * p2;
    }
    /**
     * 二次贝塞尔曲线的导数。
     * @param t 参数 [0,1]
     * @param p0 起点
     * @param p1 控制点
     * @param p2 终点
     * @return 导数值
     */
    public static float bezierDerivative(float t, float p0, float p1, float p2) {
        float oneMinusT = 1 - t;
        return 2 * (oneMinusT * (p1 - p0) + t * (p2 - p1));
    }

    // ==================== 三次贝塞尔 ====================
    /**
     * 三次贝塞尔曲线求值。
     * @param t 参数 [0,1]
     * @param p0 起点
     * @param p1 控制点1
     * @param p2 控制点2
     * @param p3 终点
     * @return 曲线值
     */
    public static float bezierCubic(float t, float p0, float p1, float p2, float p3) {
        float oneMinusT = 1 - t;
        return oneMinusT * oneMinusT * oneMinusT * p0
                + 3 * oneMinusT * oneMinusT * t * p1
                + 3 * oneMinusT * t * t * p2
                + t * t * t * p3;
    }

    /**
     * 三次贝塞尔曲线的导数。
     * @param t 参数 [0,1]
     * @param p0 起点
     * @param p1 控制点1
     * @param p2 控制点2
     * @param p3 终点
     * @return 导数值
     */
    public static float bezierCubicDerivative(float t, float p0, float p1, float p2, float p3) {
        float oneMinusT = 1 - t;
        return 3 * oneMinusT * oneMinusT * (p1 - p0)
                + 6 * oneMinusT * t * (p2 - p1)
                + 3 * t * t * (p3 - p2);
    }

    // ==================== 四次贝塞尔 ====================

    /**
     * 四次贝塞尔曲线求值。
     * @param t 参数 [0,1]
     * @param p0 起点
     * @param p1 控制点1
     * @param p2 控制点2
     * @param p3 控制点3
     * @param p4 终点
     * @return 曲线值
     */
    public static float bezierQuartic(float t, float p0, float p1, float p2, float p3, float p4) {
        float oneMinusT = 1 - t;
        return oneMinusT * oneMinusT * oneMinusT * oneMinusT * p0
                + 4 * oneMinusT * oneMinusT * oneMinusT * t * p1
                + 6 * oneMinusT * oneMinusT * t * t * p2
                + 4 * oneMinusT * t * t * t * p3
                + t * t * t * t * p4;
    }

    /**
     * 四次贝塞尔曲线的导数。
     * @param t 参数 [0,1]
     * @param p0 起点
     * @param p1 控制点1
     * @param p2 控制点2
     * @param p3 控制点3
     * @param p4 终点
     * @return 导数值
     */
    public static float bezierQuarticDerivative(float t, float p0, float p1, float p2, float p3, float p4) {
        float oneMinusT = 1 - t;
        return 4 * oneMinusT * oneMinusT * oneMinusT * (p1 - p0)
                + 12 * oneMinusT * oneMinusT * t * (p2 - p1)
                + 12 * oneMinusT * t * t * (p3 - p2)
                + 4 * t * t * t * (p4 - p3);
    }

    // ==================== 幂函数（先慢后快） ====================

    /**
     * 幂函数上升曲线：y = t^n
     * @param t 参数 [0,1]
     * @param power 指数，越大前期越平坦（先慢后快）
     * @return 高度值
     */
    public static float powerRise(float t, float power) {
        return (float) Math.pow(t, power);
    }

    /**
     * 幂函数上升曲线的导数（高度变化率）
     * @param t 参数 [0,1]
     * @param power 指数，>0
     * @return 导数值 dy/dt = power * t^(power-1)
     */
    public static float powerRiseDerivative(float t, float power) {
        if (t <= 0) return 0f;               // t=0 时导数为0（power>1时）
        if (t >= 1) return power;            // t=1 时导数为 power
        return power * (float) Math.pow(t, power - 1);
    }
    // ==================== 幂函数（先快后慢） ====================
    /**
     * 幂函数上升曲线（先快后慢）：y = 1 - (1 - t)^n
     * @param t 参数 [0,1]
     * @param pow 指数，>0。越大，前期越快、后期越平坦
     * @return 高度值
     */
    public static float powRiseEaseOut(float t, float pow) {
        return 1 - (float) Math.pow(1 - t, pow);
    }

    /**
     * 幂函数上升曲线的导数（先快后慢）
     * @param t 参数 [0,1]
     * @param power 指数，>0
     * @return 导数值 dy/dt = power * (1 - t)^(power-1)
     */
    public static float powerRiseFastFirstDerivative(float t, float power) {
        if (t >= 1) return 0f;
        return power * (float) Math.pow(1 - t, power - 1);
    }
    // ==================== 幂函数（极缓后快） ====================

    /**
     * 幂函数下降曲线：y = 1 - t^n
     * @param t 参数 [0,1]
     * @param power 指数，越大前期越平坦
     * @return 高度值
     */
    public static float powerFallEaseIn(float t, float power) {
        return 1 - (float) Math.pow(t, power);
    }
    /**
     * 幂函数下降曲线的导数（高度变化率）
     * @param t 参数 [0,1]
     * @param power 指数，>0
     * @return 导数值 dy/dt = -power * t^(power-1)
     */
    public static float powerFallDerivative(float t, float power) {
        if (t <= 0) return 0f;          // t=0 时导数恒为0（只要 power>1）
        if (t >= 1) return -power;      // t=1 时导数为 -power
        return -power * (float) Math.pow(t, power - 1);
    }
    // ==================== 幂函数（先快后慢）下降 ====================
    public static float powerFallEaseOut(float t, float power) {
        return (float) Math.pow(1 - t, power);
    }
    public static float powerFallFastFirstDerivative(float t, float power) {
        if (t >= 1) return 0f;
        return -power * (float) Math.pow(1 - t, power - 1);
    }

    // ==================== 分段贝塞尔曲线 ====================

    /**
     * 分段贝塞尔曲线：两段都是二次贝塞尔
     * 第一段：从 (0,0) 快速上升到 (midX, 1)
     * 第二段：从 (midX, 1) 缓慢下降到 (1, 0)
     * @param t 输入时间 [0,1]
     * @param midX 分段点，第一段从0到midX，第二段从midX到1
     * @param p10y 第一段控制点的y值（x自动为midX/2）
     * @param p20y 第二段控制点的y值
     * @param p20xRatio 第二段控制点的x坐标比例 [0,1]，控制点x = midX + (1-midX) * p20xRatio
     * @return 曲线值 [0,1]
     */
    public static float piecewiseBezier(
            float t,
            float midX,      // 分段点
            float p10y,      // 第一段控制点y值
            float p20y,      // 第二段控制点y值
            float p20xRatio  // 第二段控制点x比例
    ) {
        if (t < midX) {
            // 第一段：二次贝塞尔 (0,0) → (midX/2, p10y) → (midX, 1)
            float t1 = t / midX;  // 映射到 [0,1]
            float oneMinusT1 = 1 - t1;
            return oneMinusT1 * oneMinusT1 * 0 + 2 * oneMinusT1 * t1 * p10y + t1 * t1 * 1;
        } else {
            // 第二段：二次贝塞尔 (midX,1) → (midX + (1-midX)*p20xRatio, p20y) → (1, 0)
            float t2 = (t - midX) / (1 - midX);  // 映射到 [0,1]
            float oneMinusT2 = 1 - t2;
            return oneMinusT2 * oneMinusT2 * 1 + 2 * oneMinusT2 * t2 * p20y + t2 * t2 * 0;
        }
    }
}
