package com.fanxing.fx_undertale.utils;

import net.minecraft.world.phys.Vec3;
import java.util.function.Function;

public class ParametricCurveUtils {

    public static Function<Float, Vec3> reverse(Function<Float, Vec3> curve) {
        return t -> curve.apply(1 - t);
    }

    /** 圆形，半径 1 */
    public static Function<Float, Vec3> circle() {
        return t -> {
            float angle = t * 2 * (float) Math.PI;
            float r = t;  // 半径从 0 到 1
            return new Vec3(r * Math.cos(angle), 0, r * Math.sin(angle));
        };
    }

    /** 螺旋线，半径 1，圈数可调 */
    public static Function<Float, Vec3> spiral(float turns) {
        return t -> {
            float r = t;
            float angle = t * 2 * (float) Math.PI * turns;
            return new Vec3(r * Math.cos(angle), 0, r * Math.sin(angle));
        };
    }

    /** 星形，半径 1 */
    public static Function<Float, Vec3> star(int points, float depth) {
        return t -> {
            float angle = t * 2 * (float) Math.PI;
            float r = t;
            float offset = depth * (float) Math.cos(points * angle);
            float finalR = r * (1 - depth + offset);
            return new Vec3(finalR * Math.cos(angle), 0, finalR * Math.sin(angle));
        };
    }

    /** 花瓣，半径 1 */
    public static Function<Float, Vec3> flower(int petals) {
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