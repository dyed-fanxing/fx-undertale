package com.fanxing.fx_undertale.utils.collsion;

import com.fanxing.fx_undertale.common.phys.OBB;
import com.fanxing.fx_undertale.entity.capability.OBBable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class OBBRotationCollider {

    public static final int HULL_STEPS = 60;
    public static final int BISECTION_ITERATIONS = 20;
    /**
     * 获取OBB实体仅旋转的情况下的实体碰撞结果集
     * @param startOBB 实体的OBB
     * @param angularVelocity 角速度（弧度）
     * @param rotationAxis 旋转轴
     * @param rotationPivot 旋转锚点
     * @param level 维度
     * @param exclude 排除的实体（一般是自身）
     * @param filter 实体过滤器
     */
    public static List<EntityHitResultTimed> getEntityHitResultsOnlyOnRotation(OBB startOBB,float angularVelocity,Vec3 rotationAxis,Vec3 rotationPivot,Level level,@Nullable Entity exclude,Predicate<Entity> filter) {
        Vec3 axis = rotationAxis.normalize();
        if (axis.lengthSqr() < 0.9f) return Collections.emptyList();

        AABB sweptHull = buildConservativeSweptHull(startOBB, angularVelocity, axis, rotationPivot);

        List<Entity> candidates = level.getEntities(exclude, sweptHull, filter);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<EntityHitResultTimed> hits = new ArrayList<>();

        for (Entity target : candidates) {
            // 快速拒绝：扫掠盒与目标当前包围盒不相交
            // 注意：这里用 target.getBoundingBox() 做 Broad Phase 是合理的，因为目标通常也在移动或静止，其当前盒是保守估计的一部分
            if (!sweptHull.intersects(target.getBoundingBox())) continue;

            float hitTime = findCollisionInHull(target, startOBB, angularVelocity, axis, rotationPivot);

            if (hitTime >= 0.0f && hitTime <= 1.0f) {
                hits.add(new EntityHitResultTimed(target, target.position(), hitTime));
            }
        }

        hits.sort(Comparator.comparingDouble(h -> h.time));
        return hits;
    }
    /**
     * 默认以 OBB 的几何中心 (center) 为旋转锚点
     */
    public static List<EntityHitResultTimed> getEntityHitResultsOnlyOnRotation(OBB startOBB,float angularVelocity,Vec3 rotationAxis,Level level,@Nullable Entity exclude,Predicate<Entity> filter) {
        return getEntityHitResultsOnlyOnRotation(startOBB,angularVelocity,rotationAxis,startOBB.getCenter(),level,exclude,filter);
    }
    /**
     * 获取OBB实体仅旋转情况下的【第一个】碰撞结果
     * @return 返回时间最早的碰撞结果，如果没有碰撞则返回 null
     */
    @Nullable
    public static EntityHitResultTimed getEntityHitResultOnlyOnRotation(OBB startOBB,float angularVelocity,Vec3 rotationAxis,Vec3 rotationPivot,Level level,@Nullable Entity exclude,Predicate<Entity> filter) {
        Vec3 axis = rotationAxis.normalize();
        if (axis.lengthSqr() < 0.9f) return null;

        AABB sweptHull = buildConservativeSweptHull(startOBB, angularVelocity, axis, rotationPivot);

        List<Entity> candidates = level.getEntities(exclude, sweptHull, filter);
        if (candidates.isEmpty()) {
            return null;
        }

        EntityHitResultTimed earliestHit = null;
        float minTime = 1.000001f; // 初始化为略大于 1.0 的值

        for (Entity target : candidates) {
            // 快速拒绝
            if (!sweptHull.intersects(target.getBoundingBox())) continue;
            float hitTime = findCollisionInHull(target, startOBB, angularVelocity, axis, rotationPivot);
            // 检查是否有效且比当前记录的最早时间更早
            if (hitTime >= 0.0f && hitTime <= 1.0f) {
                if (hitTime < minTime) {
                    minTime = hitTime;
                    earliestHit = new EntityHitResultTimed(target, target.position(), hitTime);
                    // 优化：如果找到了 t=0 的碰撞，这绝对是最近的，直接返回，无需继续查找
                    if (hitTime == 0.0f) {
                        return earliestHit;
                    }
                }
            }
        }
        return earliestHit;
    }
    /**
     * 默认以 OBB 的几何中心 (center) 为旋转锚点
     * @return 返回时间最早的碰撞结果，如果没有碰撞则返回 null
     */
    @Nullable
    public static EntityHitResultTimed getEntityHitResultOnlyOnRotation(OBB startOBB,float angularVelocity,Vec3 rotationAxis,Level level,@Nullable Entity exclude,Predicate<Entity> filter) {
        return getEntityHitResultOnlyOnRotation(startOBB,angularVelocity,rotationAxis,startOBB.getCenter(),level,exclude,filter);
    }

    public static float findCollisionInHull(Entity target, OBB startOBB, float totalAngle, Vec3 axis, Vec3 pivot) {
        // === 分支 1: 目标是 OBB ===
        if (target instanceof OBBable obbEntity) {
            OBB targetOBB = obbEntity.getOBB();
            if (targetOBB != null) {
                // 检查 t=0
                if (startOBB.intersects(targetOBB)) return 0.0f;

                return binarySearchCollision(startOBB, targetOBB, totalAngle, axis, pivot);
            }
        }

        // === 分支 2: 目标是普通 AABB ===
        AABB targetBox = target.getBoundingBox();

        // 检查 t=0
        if (startOBB.intersects(targetBox)) return 0.0f;

        return binarySearchCollision(startOBB, targetBox, totalAngle, axis, pivot);
    }

    /**
     * 通用二分查找逻辑 (针对 OBB 目标)
     */
    public static float binarySearchCollision(OBB startOBB, OBB targetOBB, float totalAngle, Vec3 axis, Vec3 pivot) {
        int steps = HULL_STEPS;
        float tStart = -1.0f;

        // 1. 线性扫描寻找碰撞区间
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            float angle = totalAngle * t;
            OBB currentOBB = startOBB.rotateAround(angle, axis, pivot);
            if (currentOBB.intersects(targetOBB)) {
                tStart = (float) (i - 1) / steps;
                break;
            }
        }

        if (tStart < 0) return -1.0f;

        // 2. 二分查找精确化
        float tEnd = Math.min(tStart + 1.0f / steps, 1.0f);
        for (int i = 0; i < BISECTION_ITERATIONS; i++) { // 【修复】这里必须用常量，不能用 tStart
            float tMid = (tStart + tEnd) * 0.5f;
            float angleMid = totalAngle * tMid;
            OBB midOBB = startOBB.rotateAround(angleMid, axis, pivot);
            if (midOBB.intersects(targetOBB)) {
                tEnd = tMid;
            } else {
                tStart = tMid;
            }
        }
        return tEnd;
    }

    /**
     * 通用二分查找逻辑 (针对 AABB 目标)
     */
    public static float binarySearchCollision(OBB startOBB, AABB targetBox, float totalAngle, Vec3 axis, Vec3 pivot) {
        int steps = HULL_STEPS;
        float tStart = -1.0f;

        // 1. 线性扫描
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            float angle = totalAngle * t;
            OBB currentOBB = startOBB.rotateAround(angle, axis, pivot);
            if (currentOBB.intersects(targetBox)) {
                tStart = (float) (i - 1) / steps;
                break;
            }
        }

        if (tStart < 0) return -1.0f;

        // 2. 二分查找精确化
        float tEnd = Math.min(tStart + 1.0f / steps, 1.0f);
        for (int i = 0; i < BISECTION_ITERATIONS; i++) { // 【修复】同上
            float tMid = (tStart + tEnd) * 0.5f;
            float angleMid = totalAngle * tMid;
            OBB midOBB = startOBB.rotateAround(angleMid, axis, pivot);
            if (midOBB.intersects(targetBox)) {
                tEnd = tMid;
            } else {
                tStart = tMid;
            }
        }
        return tEnd;
    }

    /**
     * 构建保守扫掠包围盒 (全 float 版本)
     */
    public static AABB buildConservativeSweptHull(OBB obb, float totalAngle, Vec3 axis, Vec3 pivot) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        Vec3[] verts = obb.getVertices();
        int samples = 64;
        float step = totalAngle / samples;

        float ax = (float) axis.x;
        float ay = (float) axis.y;
        float az = (float) axis.z;
        float px = (float) pivot.x;
        float py = (float) pivot.y;
        float pz = (float) pivot.z;

        for (int i = 0; i <= samples; i++) {
            float t = i * step;
            float cosT = Mth.cos(t);
            float sinT = Mth.sin(t);
            float oneMinusCos = 1.0f - cosT;

            for (Vec3 v : verts) {
                float dx = (float) v.x - px;
                float dy = (float) v.y - py;
                float dz = (float) v.z - pz;

                float crossX = ay * dz - az * dy;
                float crossY = az * dx - ax * dz;
                float crossZ = ax * dy - ay * dx;
                float dot = ax * dx + ay * dy + az * dz;

                float rx = dx * cosT + crossX * sinT + ax * dot * oneMinusCos;
                float ry = dy * cosT + crossY * sinT + ay * dot * oneMinusCos;
                float rz = dz * cosT + crossZ * sinT + az * dot * oneMinusCos;

                float finalX = px + rx;
                float finalY = py + ry;
                float finalZ = pz + rz;

                if (finalX < minX) minX = finalX;
                if (finalX > maxX) maxX = finalX;
                if (finalY < minY) minY = finalY;
                if (finalY > maxY) maxY = finalY;
                if (finalZ < minZ) minZ = finalZ;
                if (finalZ > maxZ) maxZ = finalZ;
            }
        }

        float epsilon = 0.001f;
        return new AABB(
                minX - epsilon, minY - epsilon, minZ - epsilon,
                maxX + epsilon, maxY + epsilon, maxZ + epsilon
        );
    }
}