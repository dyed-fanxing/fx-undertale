package com.fanxing.fx_undertale.utils;

public class CurvesUtils {
    public static float parametricHeight(float t, float holdTimeScale,float ease) {
        return parametricHeight(t, holdTimeScale, ease,ease);
    }
    /**
     * 分段参数方程实现
     * @param t 时间 (0到1)
     * @param holdTimeScale 停留时间比例 (0到1)
     * @return 高度 (0到1)
     */
    public static float parametricHeight(float t, float holdTimeScale,float riseEase,float fallEase) {
        // 定义分段
        float riseTime = (1.0f - holdTimeScale) / 2.0f;
        if (t < riseTime) {
            // 上升阶段：使用二次贝塞尔曲线
            return bezier(t / riseTime, 0, riseEase, 1);
        } else if (t < riseTime + holdTimeScale) {
            // 停留阶段
            return 1.0f;
        } else {
            // 下降阶段：也使用二次贝塞尔曲线（反向）
            float t2 = (t - riseTime - holdTimeScale) / riseTime;
            return bezier(t2, 1, fallEase, 0);  // 从1到0的贝塞尔曲线
        }
    }
    // 二次贝塞尔曲线
    private static float bezier(float t, float p0, float p1, float p2) {
        float oneMinusT = 1 - t;
        return oneMinusT * oneMinusT * p0 + 2 * oneMinusT * t * p1 + t * t * p2;
    }





}
