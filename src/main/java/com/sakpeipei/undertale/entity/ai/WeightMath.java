package com.sakpeipei.undertale.entity.ai;

/**
 * 权重计算工具类，提供各种连续函数，用于根据自变量值计算权重。
 */
public class WeightMath {

    public static double linearIncrease(double x,double minX, double maxX) {
        return linearIncrease(x, minX, maxX, minX, maxX);
    }
    /**
     * 线性递增：x 越大，权重越大。
     * @param x 当前值
     * @param minWeight 权重最小值
     * @param maxWeight 权重最大值
     * @param minX x 的最小值（当 x ≤ minX 时返回 minWeight）
     * @param maxX x 的最大值（当 x ≥ maxX 时返回 maxWeight）
     */
    public static double linearIncrease(double x, double minWeight, double maxWeight, double minX, double maxX) {
        if (x <= minX) return minWeight;
        if (x >= maxX) return maxWeight;
        return minWeight + (maxWeight - minWeight) * (x - minX) / (maxX - minX);
    }

    public static double linearDecrease(double x, double minX, double maxX) {
        return linearDecrease(x, minX, maxX, minX, maxX);
    }
    /**
     * 线性递减：x 越大，权重越小。
     * @param x 当前值
     * @param minWeight 权重最小值
     * @param maxWeight 权重最大值
     * @param minX x 的最小值（当 x ≤ minX 时返回 maxWeight）
     * @param maxX x 的最大值（当 x ≥ maxX 时返回 minWeight）
     */
    public static double linearDecrease(double x, double minWeight, double maxWeight, double minX, double maxX) {
        if (x <= minX) return maxWeight;
        if (x >= maxX) return minWeight;
        return maxWeight - (maxWeight - minWeight) * (x - minX) / (maxX - minX);
    }

    /**
     * 线性峰值：在 target 处权重最大，向两侧线性衰减。
     * @param x 当前值
     * @param minWeight 权重最小值
     * @param maxWeight 权重最大值
     * @param target 峰值位置
     * @param k 衰减斜率（每偏离1单位，原始权重减少的量）
     */
    public static double linearPeak(double x, double minWeight, double maxWeight, double target, double k) {
        double raw = maxWeight - k * Math.abs(x - target);
        // 将原始值截断到 [minWeight, maxWeight] 区间内
        return Math.max(minWeight, Math.min(maxWeight, raw));
    }

    /**
     * 指数衰减：单调递减，在 x=0 处权重最大，随 x 增加而衰减。
     * @param x 当前值
     * @param minWeight 权重最小值
     * @param maxWeight 权重最大值
     * @param k 衰减系数（越大衰减越快）
     */
    public static double exponential(double x, double minWeight, double maxWeight, double k) {
        // 原始公式：exp(-k * x) 在 0~1 之间
        double norm = Math.exp(-k * x);
        double raw = minWeight + (maxWeight - minWeight) * norm;
        return Math.max(minWeight, Math.min(maxWeight, raw));
    }

    /**
     * 在目标值附近指定范围内权重高，超出范围线性下降。
     * @param x 当前值
     * @param target 目标中心值
     * @param range 范围半径（以 target 为中心，在此范围内权重恒定最高）
     * @param maxWeight 范围内的最大权重
     * @param decaySlope 超出范围后每单位下降的权重
     */
    public static double plateau(double x, double target, double range, double maxWeight, double decaySlope) {
        double deviation = Math.abs(x - target);
        if (deviation <= range) {
            return maxWeight;
        } else {
            return Math.max(0, maxWeight - (deviation - range) * decaySlope);
        }
    }


    /**
     * 高斯分布权重，围绕 target 呈钟形分布。
     * @param x 当前值
     * @param minWeight 权重最小值
     * @param maxWeight 权重最大值
     * @param target 峰值位置
     * @param sigma 标准差（控制曲线宽度）
     */
    public static double gaussian(double x, double minWeight, double maxWeight, double target, double sigma) {
        double exponent = -0.5 * Math.pow((x - target) / sigma, 2);
        double norm = Math.exp(exponent); // 归一化值在 (0, 1] 之间
        return minWeight + (maxWeight - minWeight) * norm;
    }

    /**
     * Sigmoid 函数，偏好较小值（左端权重大）。
     * @param x 当前值
     * @param base 最大权重
     * @param threshold 转折点（大于此值权重迅速下降）
     * @param k 陡峭系数（越大曲线越陡）
     */
    public static double sigmoidLow(double x, double base, double threshold, double k) {
        return base / (1 + Math.exp(k * (x - threshold)));
    }

    /**
     * Sigmoid 函数，偏好较大值（右端权重大）。
     * @param x 当前值
     * @param base 最大权重
     * @param threshold 转折点（小于此值权重迅速下降）
     * @param k 陡峭系数
     */
    public static double sigmoidHigh(double x, double base, double threshold, double k) {
        return base / (1 + Math.exp(k * (threshold - x)));
    }

    /**
     * 幂律衰减，权重随 x 增加按幂次衰减。
     * @param x 当前值
     * @param base 基础权重（x=0时）
     * @param scaleFactor 缩放因子，控制衰减速度
     * @param exponent 衰减指数
     */
    public static double powerLaw(double x, double base, double scaleFactor, double exponent) {
        return base * Math.pow(1 + x / scaleFactor, -exponent);
    }
}