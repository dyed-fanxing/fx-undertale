package com.sakpeipei.undertale.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

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
     * 获取实体在移动方向上的碰撞结果列表
     * @param entity 实体
     * @param filter 过滤器
     * @param blockClip 方块碰撞检测条件
     * @param isCollision 是否包含碰撞检测，即静止时，是否使用碰撞检测
     * @return
     */
    public static List<HitResult> getHitResultsOnMoveVector(Entity entity, Predicate<Entity> filter,ClipContext.Block blockClip,boolean isCollision) {
        return getHitResults(entity, entity.position(), entity.getDeltaMovement(),filter,0.3F, blockClip,isCollision);
    }
    private static List<HitResult> getHitResults(Entity entity,Vec3 from,Vec3 dir,  Predicate<Entity> filter, float pickable, ClipContext.Block blockClip,boolean isCollision) {
        Vec3 to = from.add(dir);
        Level level = entity.level();
        HitResult hitResult = level.clip(new ClipContext(from, to, blockClip, ClipContext.Fluid.NONE, entity));
        if (hitResult.getType() != HitResult.Type.MISS) {
            to = hitResult.getLocation();
        }
        List<HitResult> hitResults = getEntityHitResultsOnMoveVector(entity, from, to, entity.getBoundingBox().expandTowards(dir).inflate(1.0F), filter, pickable,isCollision);
        hitResults.add(hitResult);
        return hitResults;
    }

    public static List<HitResult> getEntityHitResultsOnMoveVector(Entity shooter, Vec3 from, Vec3 to, AABB searchArea, Predicate<Entity> filter, float pickable,boolean isCollision) {
        Level level = shooter.level();
        List<HitResult> results = new ArrayList<>();
        for (Entity entity1 : level.getEntities(shooter, searchArea, filter)) {
            AABB aabb = entity1.getBoundingBox().inflate(pickable);
            if(isCollision){
                // 优先射线交点，没有就使用目标位置
                Vec3 hitPoint = aabb.clip(from, to).orElse(entity1.position());
                if (entity1.getRootVehicle() == shooter.getRootVehicle() && !entity1.canRiderInteract()) {
                    continue;
                }
                results.add(new EntityHitResult(entity1, hitPoint));
            }else{
                Optional<Vec3> hitPos = aabb.clip(from, to);
                if (hitPos.isPresent()) {
                    // 检查骑乘关系（如果需要）
                    if (entity1.getRootVehicle() == shooter.getRootVehicle() && !entity1.canRiderInteract()) {
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
     * 检测胶囊体与AABB是否碰撞
     * @param start 胶囊体起点
     * @param end 胶囊体终点
     * @param r 胶囊体半径
     * @param aabb AABB盒子
     * @return 是否碰撞
     */
    public static boolean capsuleIntersectsAABB(Vec3 start, Vec3 end, float r, AABB aabb) {
        // 1. 找到胶囊体线段上离AABB最近的点
        Vec3 closestOnSegment = getClosestPointOnLine(aabb.getCenter(), start, end);
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
    public static Vec3 getClosestPointOnLine(Vec3 point, Vec3 start, Vec3 end) {
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
