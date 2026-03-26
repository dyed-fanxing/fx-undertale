package com.fanxing.fx_undertale.utils.collsion;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author FanXing
 * @since 2025-09-27 13:29
 */
public class CollisionDetectionUtils {


    private static final Logger log = LoggerFactory.getLogger(CollisionDetectionUtils.class);


    /**
     * 检测胶囊体与AABB是否碰撞（胶囊体的定义是圆柱的轴线和两个半球R）
     * 检测的范围是start到end的圆柱范围，以及两端两个半球的范围
     * @param start 胶囊体起点
     * @param end 胶囊体终点
     * @param r 胶囊体半径
     * @param aabb AABB盒子
     * @return 是否碰撞
     */
    public static boolean capsuleIntersectsAABB(Vec3 start, Vec3 end, float r, AABB aabb) {
        // 1. 找到胶囊体线段上离AABB最近的点
        Vec3 closestOnSegment = getClosestPointOnLineSegment(aabb.getCenter(), start, end);
        // 2. 找到AABB上离这个点最近的点
        // 3. 计算两点之间的距离平方 <= 距离平方小于等于半径平方则碰撞为ture，否则 false
        return aabb.distanceToSqr(closestOnSegment) <= r * r;
    }

    /**
     * 获取线段上离给定点最近的点
     * @param point 目标点
     * @param start 线段起点
     * @param end 线段终点
     * @return 线段上最近的点
     */
    public static Vec3 getClosestPointOnLineSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 line = end.subtract(start);
        double lineSqr = line.lengthSqr();
        // 线段退化为点
        if (lineSqr < 1e-6) {
            return start;
        }

        // 计算投影比例
        double ratio = point.subtract(start).dot(line) / lineSqr;
        // 夹紧到[0,1]范围内
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        return start.add(line.scale(ratio));
    }
}
