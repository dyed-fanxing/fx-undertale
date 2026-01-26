package com.sakpeipei.undertale.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author Sakqiongzi
 * @since 2025-09-27 13:29
 */
public class CollisionDetectionUtils {




    /**
     * 获取实体在移动方向上的碰撞结果列表，默认自身实体范围，无额外扩大碰撞检测
     */
//    public static List<HitResult> getHitResultsOnMoveVector(Entity entity, Predicate<Entity> filter,ClipContext.Block blockClip,boolean hasStatic) {
//        return getHitResults(entity, entity.getBoundingBox().getCenter(), entity.getDeltaMovement(),filter,0.3F, blockClip,hasStatic);
//    }
    public static List<HitResult> getHitResultsOnMoveVector(Entity entity, Predicate<Entity> filter,boolean hasStatic) {
        return getHitResults(entity, entity.getBoundingBox().getCenter(), entity.getDeltaMovement(),filter,0.3F, ClipContext.Block.COLLIDER,hasStatic);
    }
    /**
     * pickable默认0.3f
     */
    public static List<HitResult> getHitResultsOnMoveVector(Entity entity, Predicate<Entity> filter,ClipContext.Block blockClip,boolean hasStatic) {
        return getHitResults(entity, entity.getBoundingBox().getCenter(), entity.getDeltaMovement(),filter,0.3F, blockClip,hasStatic);
    }
    /**
     * 获取实体在移动方向上的碰撞结果列表，只能用于正方体碰撞箱的弹射物
     * 原理是起点在移动方向上扩大运动向量的线段和目标碰撞箱扩大pickable大小的碰撞检测，是射线和碰撞箱的检测
     */
    public static List<HitResult> getHitResultsOnMoveVector(Entity entity, Predicate<Entity> filter,float pickable,ClipContext.Block blockClip,boolean hasStatic) {
        return getHitResults(entity, entity.getBoundingBox().getCenter(), entity.getDeltaMovement(),filter,0.3F, blockClip,hasStatic);
    }

    /**
     * 获取实体在from起点到dir向量线段之间的碰撞检测结果列表（包含方块碰撞和实体碰撞）
     */
    private static List<HitResult> getHitResults(Entity entity,Vec3 from,Vec3 dir,  Predicate<Entity> filter, ClipContext.Block blockClip,boolean hasStatic) {
        Vec3 to = from.add(dir);
        Level level = entity.level();
        HitResult hitResult = level.clip(new ClipContext(from, to, blockClip, ClipContext.Fluid.NONE, entity));
        if (hitResult.getType() != HitResult.Type.MISS) {
            to = hitResult.getLocation();
        }
        List<HitResult> hitResults = getEntityHitResults(entity, from, to, entity.getBoundingBox().expandTowards(dir), filter,hasStatic);
        hitResults.add(hitResult);
        return hitResults;
    }

    /**
     * 获取实体在from起点到dir向量线段之间的碰撞检测结果列表（包含方块碰撞和实体碰撞）
     * @param dir 向量
     * @param blockClip 方块判定
     * @param pickable 扩大目标碰撞箱三个坐标轴
     */
    private static List<HitResult> getHitResults(Entity entity,Vec3 from,Vec3 dir,  Predicate<Entity> filter, float pickable, ClipContext.Block blockClip,boolean hasStatic) {
        Vec3 to = from.add(dir);
        Level level = entity.level();
        HitResult hitResult = level.clip(new ClipContext(from, to, blockClip, ClipContext.Fluid.NONE, entity));
        if (hitResult.getType() != HitResult.Type.MISS) {
            to = hitResult.getLocation();
        }
        List<HitResult> hitResults = getEntityHitResults(entity, from, to, entity.getBoundingBox().expandTowards(dir), filter, pickable,hasStatic);
        hitResults.add(hitResult);
        return hitResults;
    }
    /**
     * 获取实体在from到to两点线段之间的实体碰撞检测结果列表
     */
    public static List<HitResult> getEntityHitResults(Entity entity, Vec3 from, Vec3 to, AABB searchArea, Predicate<Entity> filter,boolean hasStatic) {
        float halfWidth = entity.getBbWidth() * 0.5f;
        return getEntityHitResults(entity, from, to, halfWidth,entity.getBbHeight() * 0.5f,halfWidth,searchArea, filter, hasStatic);
    }

    /**
     * 获取实体在from到to两点线段之间的实体碰撞检测结果列表
     * @param pickable 扩大目标碰撞箱三个坐标轴
     */
    public static List<HitResult> getEntityHitResults(Entity entity, Vec3 from, Vec3 to, AABB searchArea, Predicate<Entity> filter, float pickable,boolean hasStatic) {
        return getEntityHitResults(entity, from, to, pickable,pickable,pickable,searchArea, filter, hasStatic);
    }

    /**
     * 获取实体在线段之间的实体碰撞检测结果列表
     * 原理是起点from和终点to的线段和目标AABB碰撞箱扩大后的碰撞箱是否碰撞，是射线和AABB碰撞箱的检测
     *
     * @param entity 实体
     * @param from 射线起点
     * @param to 射线终点
     * @param inflateX,inflateY,inflateZ 扩大目标碰撞箱的检测范围，即实体自身碰撞箱一半
     * @param searchArea 粗略筛选实体的AABB碰撞箱
     * @param filter 过滤器
     * @param hasStatic 是否检测静态碰撞，即实体静止不同时是否检测
     */
    public static List<HitResult> getEntityHitResults(Entity entity, Vec3 from, Vec3 to, double inflateX,double inflateY,double inflateZ, AABB searchArea, Predicate<Entity> filter,boolean hasStatic) {
        Level level = entity.level();
        List<HitResult> results = new ArrayList<>();
        for (Entity entity1 : level.getEntities(entity, searchArea, filter)) {
            AABB aabb = entity1.getBoundingBox().inflate(inflateX,inflateY,inflateZ);
            if(hasStatic){
                // 优先射线交点，没有就使用目标位置
                Vec3 hitPoint = aabb.clip(from, to).orElse(entity1.position());
                if (entity1.getRootVehicle() == entity.getRootVehicle() && !entity1.canRiderInteract()) {
                    continue;
                }
                results.add(new EntityHitResult(entity1, hitPoint));
            }else{
                Optional<Vec3> hitPos = aabb.clip(from, to);
                if (hitPos.isPresent()) {
                    // 检查骑乘关系（如果需要）
                    if (entity1.getRootVehicle() == entity.getRootVehicle() && !entity1.canRiderInteract()) {
                        continue;  // 跳过不能交互的同乘实体
                    }
                    results.add(new EntityHitResult(entity1, hitPos.get()));
                }
            }
        }
        // 按距离排序（近到远）
        results.sort(Comparator.comparingDouble(a -> from.distanceToSqr(a.getLocation())));
        return results;
    }




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
