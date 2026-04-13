package com.fanxing.fx_undertale.utils;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.function.Function;

public class Curve3DUtils {

    /**
     * 正弦曲线运动（例如上下起伏或左右摆动）
     * @param t        当前时间参数（可以是 tickCount 或 进度 0..1）
     * @param center   运动中心点（绝对坐标）
     * @param amplitudeX X 轴振幅
     * @param amplitudeY Y 轴振幅
     * @param amplitudeZ Z 轴振幅
     * @param frequency 角频率（弧度每 tick，例如 0.1 表示约 62 刻一个周期）
     * @param phase     初始相位偏移
     * @return 当前时刻的绝对位置
     */
    public static Vec3 sine(double t, Vec3 center,
                            double amplitudeX, double amplitudeY, double amplitudeZ,
                            double frequency, double phase) {
        double x = center.x + amplitudeX * Math.sin(frequency * t + phase);
        double y = center.y + amplitudeY * Math.sin(frequency * t + phase);
        double z = center.z + amplitudeZ * Math.sin(frequency * t + phase);
        return new Vec3(x, y, z);
    }

    /**
     * 圆周运动（绕指定轴）
     * @param t          时间参数
     * @param center     圆心绝对坐标
     * @param axis       旋转轴（X, Y 或 Z）
     * @param radius1    第一个轴向上的半径（例如绕 Y 轴时，对应 X 轴半径）
     * @param radius2    第二个轴向上的半径（例如绕 Y 轴时，对应 Z 轴半径）
     * @param speed      角速度（弧度每 tick）
     * @param phase      起始角度偏移
     * @return 当前时刻的绝对位置
     */
    public static Vec3 circle(double t, Vec3 center, Direction.Axis axis,
                              double radius1, double radius2,
                              double speed, double phase) {
        double angle = speed * t + phase;
        double dx = radius1 * Math.cos(angle);
        double dy = radius2 * Math.sin(angle);
        return switch (axis) {
            // 绕 X 轴旋转：Y 和 Z 变化，X 固定
            case X -> new Vec3(center.x, center.y + dx, center.z + dy);
            // 绕 Y 轴旋转：X 和 Z 变化，Y 固定
            case Y -> new Vec3(center.x + dx, center.y, center.z + dy);
            // 绕 Z 轴旋转：X 和 Y 变化，Z 固定
            case Z -> new Vec3(center.x + dx, center.y + dy, center.z);
        };
    }


    /**
     * 三角波直线往返运动（沿任意方向，恒速，循环），返回绝对位置
     * @param t         时间参数（单位：tick）
     * @param center    运动中心点
     * @param direction 运动方向向量（不需要归一化）
     * @param amplitude 运动幅度（从 center - amplitude*dir 到 center + amplitude*dir）
     * @param speed     角速度（周期 T = 2π / speed）
     * @param phase     起始相位（0 表示从 center 开始向正方向运动）
     * @return 当前时刻的绝对位置
     */
    public static Vec3 triangle(double t, Vec3 center, Vec3 direction,
                                double amplitude, double speed, double phase) {
        Vec3 dir = direction.normalize();
        double angle = speed * t + phase;
        double norm = (angle % (2 * Math.PI)) / (2 * Math.PI);
        double value01 = norm < 0.5 ? norm * 2 : 2 - norm * 2;
        double offset = (value01 * 2 - 1) * amplitude;
        return center.add(dir.x * offset, dir.y * offset, dir.z * offset);
    }

    /**
     * 三角波直线往返运动（沿任意方向，恒速，循环），返回绝对位置
     * @param t         时间参数（单位：tick）
     * @param center    运动中心点
     * @param axis      轴
     * @param amplitude 运动幅度（从 center - amplitude*dir 到 center + amplitude*dir）
     * @param speed     角速度（周期 T = 2π / speed）
     * @param phase     起始相位（0 表示从 center 开始向正方向运动）
     * @return 当前时刻的绝对位置
     */
    public static Vec3 triangle(double t, Vec3 center, Direction.Axis axis,
                                double amplitude, double speed, double phase) {
        double angle = speed * t + phase;
        double norm = (angle % (2 * Math.PI)) / (2 * Math.PI);
        double value01 = norm < 0.5 ? norm * 2 : 2 - norm * 2;
        double offset = (value01 * 2 - 1) * amplitude;
        // 根据运动轴，构建绝对位置
        return switch (axis) {
            case X -> new Vec3(center.x + offset, center.y, center.z);
            case Y -> new Vec3(center.x, center.y + offset, center.z);
            case Z -> new Vec3(center.x, center.y, center.z + offset);
        };
    }









    public static Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        float x = 0.5f * ((2f * p1.x) +
                        (-p0.x + p2.x) * t +
                        (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 +
                        (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
        float y = 0.5f * ((2 * p1.y) +
                        (-p0.y + p2.y) * t +
                        (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
                        (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
        float z = 0.5f * ((2 * p1.z) +
                        (-p0.z + p2.z) * t +
                        (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 +
                        (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);
        return new Vector3f(x, y, z);
    }
}
