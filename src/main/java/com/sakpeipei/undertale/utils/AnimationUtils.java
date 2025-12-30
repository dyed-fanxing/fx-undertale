package com.sakpeipei.undertale.utils;

/**
 * 动画计算工具类
 * 提供静态方法处理各种动画效果
 */
public final class AnimationUtils {

    /**
     * 标准动画计算
     * @param start 起始值
     * @param end 结束值
     * @param currentTick 当前tick计数（从0开始）
     * @param duration 总持续tick数
     * @param partialTicks 部分刻进度[0,1)
     * @param easing 缓动类型
     * @return 当前动画值
     */
    public static float calculate(float start, float end, int currentTick, int duration, float partialTicks, EasingType easing) {
        float progress = Math.min(1f, (currentTick + partialTicks) / duration);
        return interpolate(start, end, applyEasing(progress, easing));
    }

    /**
     * 从from过渡到to，可用于1-Tick瞬时动画插值或其他
     * @param partialTicks 部分刻进度
     * @return 插值结果
     */
    public static float interpolateInstant(float from, float to, float partialTicks) {
        return from + (to - from) * partialTicks;
    }

    /**
     * 进度计算
     * @param currentTick 当前tick
     * @param duration 总tick数
     * @param partialTicks 部分刻
     * @param easing 缓动类型
     * @return 标准化进度[0,1]
     */
    public static float calculateProgress(int currentTick, int duration, float partialTicks, EasingType easing) {
        float progress = Math.min(1f, (currentTick + partialTicks) / duration);
        return applyEasing(progress, easing);
    }

    /**
     * 基础插值
     * @param start
     * @param end
     * @param progress
     * @return
     */
    public static float interpolate(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    // 缓动函数应用
    public static float applyEasing(float progress, EasingType type) {
        return switch (type) {
            case EASE_IN -> progress * progress;
            case EASE_OUT -> 1 - (1 - progress) * (1 - progress);
            case EASE_IN_OUT -> progress < 0.5f ? 2 * progress * progress : 1 - (float) Math.pow(-2 * progress + 2, 2) / 2;
            default -> progress; // LINEAR
        };
    }

    public enum EasingType {
        LINEAR,       // 线性
        EASE_IN,      // 缓入
        EASE_OUT,     // 缓出
        EASE_IN_OUT   // 缓入缓出
    }
}