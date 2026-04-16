package com.fanxing.fx_undertale.utils.collsion;

import com.fanxing.fx_undertale.common.phys.OBB;
import com.fanxing.fx_undertale.entity.capability.OBBHolder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * 连续碰撞检测工具类 (Continuous Collision Detection)
 * 支持 OBB 的纯平移、纯旋转、平移+旋转三种运动形式的碰撞检测。
 */
public class OBBCCDUtils {
    private static final Logger log = LoggerFactory.getLogger(OBBCCDUtils.class);
    
    public static int MAX_STEPS = 64;
    public static final int HULL_STEPS = 60;
    public static final int BISECTION_ITERATIONS = 20;

    // ============================================================
    // 1. 纯平移检测（原有方法，性能最优）
    // ============================================================

    public static List<EntityHitResult> getEntityHitResultsOnlyOnMove(OBB obb, Vec3 velocity, Entity exclude, Predicate<Entity> filter) {
        if (velocity.lengthSqr() < Mth.EPSILON) return Collections.emptyList();

        List<EntityHitResult> results = new ArrayList<>();
        OBB extendedOBB = obb.expandTowards(velocity);
        AABB searchArea = extendedOBB.getBoundingAABB().inflate(0.5);
        List<Entity> candidates = exclude.level().getEntities(exclude, searchArea, filter);
        for (Entity e : candidates) {
            if (extendedOBB.intersects(e.getBoundingBox())) {
                results.add(new EntityHitResult(e, e.getBoundingBox().getCenter()));
            }
        }
        results.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(obb.getCenter())));
        return results;
    }

    public static boolean willHitEntityOnlyOnMove(OBB obb, Vec3 velocity, Entity target) {
        OBB extendedOBB = obb.expandTowards(velocity);
        return extendedOBB.intersects(target.getBoundingBox());
    }

    @Nullable
    public static EntityHitResult getEntityHitResultOnlyOnMove(OBB obb, Vec3 velocity, Level level, Entity exclude, Predicate<Entity> filter) {
        if (velocity.lengthSqr() < Mth.EPSILON) return null;
        OBB extendedOBB = obb.expandTowards(velocity);
        AABB searchArea = extendedOBB.getBoundingAABB().inflate(0.5);
        List<Entity> candidates = level.getEntities(exclude, searchArea, filter);
        for (Entity e : candidates) {
            if (extendedOBB.intersects(e.getBoundingBox())) {
                return new EntityHitResult(e, e.getBoundingBox().getCenter());
            }
        }
        return null;
    }




    /**
     * 获取OBB实体仅旋转的情况下的实体碰撞结果集
     */
    public static List<EntityHitResultTimed> getEntityHitResultsOnlyOnRotation(OBB startOBB, float angularVelocity, Vec3 rotationAxis, Vec3 rotationPivot, Level level, @Nullable Entity exclude, Predicate<Entity> filter) {
//        System.out.println("CCD: angularVelocity=" + angularVelocity + " rad, rotationAxis=" + rotationAxis + ", pivot=" + rotationPivot);
        if (Mth.abs(angularVelocity) < Mth.EPSILON) return Collections.emptyList();
        Vec3 axis = rotationAxis.normalize();
        if (axis.lengthSqr() < 0.9f) return Collections.emptyList();

        AABB sweptHull = buildConservativeSweptHull(startOBB, angularVelocity, axis, rotationPivot);

        List<Entity> candidates = level.getEntities(exclude, sweptHull, filter);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<EntityHitResultTimed> hits = new ArrayList<>();

        for (Entity target : candidates) {
            if (!sweptHull.intersects(target.getBoundingBox())) continue;

            float hitTime = findCollisionInHull(target, startOBB, angularVelocity, axis, rotationPivot);

            if (hitTime >= 0.0f && hitTime <= 1.0f) {
                // 计算碰撞时刻的 OBB
                OBB collisionOBB = calculateOBBAtTime(startOBB, Vec3.ZERO, angularVelocity, axis, rotationPivot, hitTime);
                // 计算精确碰撞点（使用 SAT 精确计算）
                Vec3 collisionPoint = calculateCollisionPointPrecise(collisionOBB, target.getBoundingBox());
                // 如果没有精确碰撞点，使用目标位置作为后备
                if (collisionPoint == null) {
                    collisionPoint = target.position();
                }
                hits.add(new EntityHitResultTimed(target, collisionPoint, hitTime));
            }
        }

        hits.sort(Comparator.comparingDouble(h -> h.time));
        return hits;
    }
    /**
     * 默认以 OBB 的几何中心 (center) 为旋转锚点
     */
    public static List<EntityHitResultTimed> getEntityHitResultsOnlyOnRotation(OBB startOBB, float angularVelocity, Vec3 rotationAxis, Level level, @Nullable Entity exclude, Predicate<Entity> filter) {
        return getEntityHitResultsOnlyOnRotation(startOBB, angularVelocity, rotationAxis, startOBB.getCenter(), level, exclude, filter);
    }


    /**
     * 直接使用角速度向量：方向=旋转轴，大小=角速度（弧度）
     */
    public static List<EntityHitResultTimed> getEntityHitResultsOnlyOnRotation(OBB startOBB,Vector3f angularVelocityVector,Vec3 rotationPivot,Level level,@Nullable Entity exclude,Predicate<Entity> filter) {
        if (angularVelocityVector.lengthSquared() < Mth.EPSILON*Mth.EPSILON) return Collections.emptyList();

        AABB sweptHull = buildConservativeSweptHull(startOBB, angularVelocityVector, rotationPivot);

        List<Entity> candidates = level.getEntities(exclude, sweptHull, filter);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<EntityHitResultTimed> hits = new ArrayList<>();

        for (Entity target : candidates) {
            if (!sweptHull.intersects(target.getBoundingBox())) continue;

            float hitTime = findCollisionInHull(target, startOBB, angularVelocityVector, rotationPivot);

            if (hitTime >= 0.0f && hitTime <= 1.0f) {
                // 计算碰撞时刻的 OBB
                OBB collisionOBB = calculateOBBAtTime(startOBB, Vec3.ZERO, angularVelocityVector, rotationPivot, hitTime);
                // 计算精确碰撞点（使用 SAT 精确计算）
                Vec3 collisionPoint = calculateCollisionPointPrecise(collisionOBB, target.getBoundingBox());
                // 如果没有精确碰撞点，使用目标位置作为后备
                if (collisionPoint == null) {
                    collisionPoint = target.position();
                }
                hits.add(new EntityHitResultTimed(target, collisionPoint, hitTime));
            }
        }

        hits.sort(Comparator.comparingDouble(h -> h.time));
        return hits;
    }
    /**
     * 【新方法】默认以 OBB 的几何中心 (center) 为旋转锚点
     */
    public static List<EntityHitResultTimed> getEntityHitResultsOnlyOnRotation(OBB startOBB,Vector3f angularVelocityVector,Level level,@Nullable Entity exclude,Predicate<Entity> filter) {
        return getEntityHitResultsOnlyOnRotation(startOBB, angularVelocityVector, startOBB.getCenter(), level, exclude, filter);
    }

    /**
     * 获取OBB实体仅旋转情况下的【第一个】碰撞结果
     */
    @Nullable
    public static EntityHitResultTimed getEntityHitResultOnlyOnRotation(OBB startOBB, float angularVelocity, Vec3 rotationAxis, Vec3 rotationPivot, Level level, @Nullable Entity exclude, Predicate<Entity> filter) {
        if (Mth.abs(angularVelocity) < Mth.EPSILON) return null;
        Vec3 axis = rotationAxis.normalize();
        if (axis.lengthSqr() < 0.9f) return null;

        AABB sweptHull = buildConservativeSweptHull(startOBB, angularVelocity, axis, rotationPivot);

        List<Entity> candidates = level.getEntities(exclude, sweptHull, filter);
        if (candidates.isEmpty()) {
            return null;
        }

        EntityHitResultTimed earliestHit = null;
        float minTime = 1.000001f;

        for (Entity target : candidates) {
            if (!sweptHull.intersects(target.getBoundingBox())) continue;
            float hitTime = findCollisionInHull(target, startOBB, angularVelocity, axis, rotationPivot);
            if (hitTime >= 0.0f && hitTime <= 1.0f) {
                if (hitTime < minTime) {
                    minTime = hitTime;
                    // 计算碰撞时刻的 OBB
                                    OBB collisionOBB = calculateOBBAtTime(startOBB, Vec3.ZERO, angularVelocity, axis, rotationPivot, hitTime);
                                    // 计算精确碰撞点（使用 SAT 精确计算）
                                    Vec3 collisionPoint = calculateCollisionPointPrecise(collisionOBB, target.getBoundingBox());                    // 如果没有精确碰撞点，使用目标位置作为后备
                    if (collisionPoint == null) {
                        collisionPoint = target.position();
                    }
                    earliestHit = new EntityHitResultTimed(target, collisionPoint, hitTime);
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
     */
    @Nullable
    public static EntityHitResultTimed getEntityHitResultOnlyOnRotation(OBB startOBB, float angularVelocity, Vec3 rotationAxis, Level level, @Nullable Entity exclude, Predicate<Entity> filter) {
        return getEntityHitResultOnlyOnRotation(startOBB, angularVelocity, rotationAxis, startOBB.getCenter(), level, exclude, filter);
    }

    /**
     * 【新方法】直接接受角速度向量，不分解成轴和角度
     */
    @Nullable
    public static EntityHitResultTimed getEntityHitResultOnlyOnRotation(OBB startOBB,Vector3f angularVelocityVector,Vec3 rotationPivot,Level level,@Nullable Entity exclude,Predicate<Entity> filter) {
        if (angularVelocityVector.lengthSquared() < Mth.EPSILON * Mth.EPSILON) return null;

        AABB sweptHull = buildConservativeSweptHull(startOBB, angularVelocityVector, rotationPivot);

        List<Entity> candidates = level.getEntities(exclude, sweptHull, filter);
        if (candidates.isEmpty()) {
            return null;
        }

        EntityHitResultTimed earliestHit = null;
        float minTime = 1.000001f;

        for (Entity target : candidates) {
            if (!sweptHull.intersects(target.getBoundingBox())) continue;

            float hitTime = findCollisionInHull(target, startOBB, angularVelocityVector, rotationPivot);

            if (hitTime >= 0.0f && hitTime <= 1.0f) {
                if (hitTime < minTime) {
                    minTime = hitTime;
                    // 计算碰撞时刻的 OBB
                    OBB collisionOBB = calculateOBBAtTime(startOBB, Vec3.ZERO, angularVelocityVector, rotationPivot, hitTime);
                    // 计算精确碰撞点（使用 SAT 精确计算）
                    Vec3 collisionPoint = calculateCollisionPointPrecise(collisionOBB, target.getBoundingBox());
                    // 如果没有精确碰撞点，使用目标位置作为后备
                    if (collisionPoint == null) {
                        collisionPoint = target.position();
                    }
                    earliestHit = new EntityHitResultTimed(target, collisionPoint, hitTime);
                    if (hitTime == 0.0f) {
                        return earliestHit;
                    }
                }
            }
        }

        return earliestHit;
    }

    /**
     * 【新方法】默认以 OBB 的几何中心 (center) 为旋转锚点
     */
    @Nullable
    public static EntityHitResultTimed getEntityHitResultOnlyOnRotation(
            OBB startOBB,
            Vector3f angularVelocityVector,
            Level level,
            @Nullable Entity exclude,
            Predicate<Entity> filter) {
        return getEntityHitResultOnlyOnRotation(startOBB, angularVelocityVector, startOBB.getCenter(), level, exclude, filter);
    }

    // ============================================================
    // 3. 平移 + 旋转检测 (Screw Motion / Helical Motion)
    // ============================================================
    /**
     * 【完整列表】获取 OBB 在 平移+旋转 复合运动下的所有碰撞结果
     *
     * @param startOBB        起始 OBB
     * @param velocity        线性速度向量 (表示从 t=0 到 t=1 的总位移)
     * @param angularVelocity 角速度 (弧度，表示从 t=0 到 t=1 的总旋转角度)
     * @param rotationAxis    旋转轴
     * @param rotationPivot   旋转锚点 (世界坐标)
     * @param level           维度
     * @param exclude         排除实体
     * @param filter          过滤器
     * @return 按碰撞时间排序的结果列表
     */
    public static List<EntityHitResultTimed> getEntityHitResults(
            OBB startOBB,
            Vec3 velocity,
            float angularVelocity,
            Vec3 rotationAxis,
            Vec3 rotationPivot,
            Level level,
            @Nullable Entity exclude,
            Predicate<Entity> filter) {

        if (velocity.lengthSqr() < Mth.EPSILON && Mth.abs(angularVelocity) < Mth.EPSILON) return Collections.emptyList();

        Vec3 axis = rotationAxis.normalize();
        if (axis.lengthSqr() < 0.9f) {
            // 如果轴无效，退化为纯平移
            if (velocity.lengthSqr() > Mth.EPSILON) {
                // 这里需要转换一下返回类型，或者简单处理
                // 为了保持类型一致，我们手动调用一次纯平移逻辑并包装
                List<EntityHitResult> moveHits = getEntityHitResultsOnlyOnMove(startOBB, velocity, exclude, filter);
                return moveHits.stream()
                        .map(h -> new EntityHitResultTimed(h.getEntity(), h.getLocation(), 0.0f)) // 纯平移无法精确计算时间比例，暂定为0或需额外计算
                        .toList();
            }
            return Collections.emptyList();
        }

        // 1. 构建保守扫掠盒 (Broad Phase)
        AABB sweptHull = buildConservativeSweptHullCombined(startOBB, velocity, angularVelocity, axis, rotationPivot);

        List<Entity> candidates = level.getEntities(exclude, sweptHull, filter);
        if (candidates.isEmpty()) return Collections.emptyList();

        List<EntityHitResultTimed> hits = new ArrayList<>();

        for (Entity target : candidates) {
            if (!sweptHull.intersects(target.getBoundingBox())) continue;

            float hitTime = findCollisionInHullCombined(target, startOBB, velocity, angularVelocity, axis, rotationPivot);

            if (hitTime >= 0.0f && hitTime <= 1.0f) {
                // 计算碰撞时刻的 OBB
                OBB collisionOBB = calculateOBBAtTime(startOBB, velocity, angularVelocity, axis, rotationPivot, hitTime);
                // 计算精确碰撞点（使用 SAT 精确计算）
                Vec3 collisionPoint = calculateCollisionPointPrecise(collisionOBB, target.getBoundingBox());
                // 如果没有精确碰撞点，使用目标位置作为后备
                if (collisionPoint == null) {
                    collisionPoint = target.position();
                }
                hits.add(new EntityHitResultTimed(target, collisionPoint, hitTime));
            }
        }

        hits.sort(Comparator.comparingDouble(h -> h.time));
        return hits;
    }

    /**
     * 【完整列表】重载：默认以 OBB 中心为旋转锚点
     */
    public static List<EntityHitResultTimed> getEntityHitResults(
            OBB startOBB,
            Vec3 velocity,
            float angularVelocity,
            Vec3 rotationAxis,
            Level level,
            @Nullable Entity exclude,
            Predicate<Entity> filter) {
        return getEntityHitResults(startOBB, velocity, angularVelocity, rotationAxis, startOBB.getCenter(), level, exclude, filter);
    }

    /**
     * 【新方法】直接接受角速度向量，不分解成轴和角度
     */
    public static List<EntityHitResultTimed> getEntityHitResults(
            OBB startOBB,
            Vec3 velocity,
            Vector3f angularVelocityVector,
            Vec3 rotationPivot,
            Level level,
            @Nullable Entity exclude,
            Predicate<Entity> filter) {

        if (velocity.lengthSqr() < Mth.EPSILON && angularVelocityVector.lengthSquared() < Mth.EPSILON * Mth.EPSILON) return Collections.emptyList();

        AABB sweptHull = buildConservativeSweptHullCombinedByAngularVelocity(startOBB, velocity, angularVelocityVector, rotationPivot);

        List<Entity> candidates = level.getEntities(exclude, sweptHull, filter);
        if (candidates.isEmpty()) return Collections.emptyList();

        List<EntityHitResultTimed> hits = new ArrayList<>();

        for (Entity target : candidates) {
            if (!sweptHull.intersects(target.getBoundingBox())) continue;

            float hitTime = findCollisionCombinedByAngularVelocity(target, startOBB, velocity, angularVelocityVector, rotationPivot);

            if (hitTime >= 0.0f && hitTime <= 1.0f) {
                // 计算碰撞时刻的 OBB
                OBB collisionOBB = calculateOBBAtTime(startOBB, velocity, angularVelocityVector, rotationPivot, hitTime);
                // 计算精确碰撞点（使用 SAT 精确计算）
                Vec3 collisionPoint = calculateCollisionPointPrecise(collisionOBB, target.getBoundingBox());
                // 如果没有精确碰撞点，使用目标位置作为后备
                if (collisionPoint == null) {
                    collisionPoint = target.position();
                }
                hits.add(new EntityHitResultTimed(target, collisionPoint, hitTime));
            }
        }

        hits.sort(Comparator.comparingDouble(h -> h.time));
        return hits;
    }

    /**
     * 【新方法】默认以 OBB 中心为旋转锚点
     */
    public static List<EntityHitResultTimed> getEntityHitResults(
            OBB startOBB,
            Vec3 velocity,
            Vector3f angularVelocityVector,
            Level level,
            @Nullable Entity exclude,
            Predicate<Entity> filter) {
        return getEntityHitResults(startOBB, velocity, angularVelocityVector, startOBB.getCenter(), level, exclude, filter);
    }

    /**
     * 【单个结果】获取 OBB 在 平移+旋转 复合运动下的第一个碰撞结果
     *
     * @return 最早发生的碰撞，若无则返回 null
     */
    @Nullable
    public static EntityHitResultTimed getEntityHitResult(
            OBB startOBB,
            Vec3 velocity,
            float angularVelocity,
            Vec3 rotationAxis,
            Vec3 rotationPivot,
            Level level,
            @Nullable Entity exclude,
            Predicate<Entity> filter) {
        if (velocity.lengthSqr() < Mth.EPSILON && Mth.abs(angularVelocity) < Mth.EPSILON) return null;

        Vec3 axis = rotationAxis.normalize();
        if (axis.lengthSqr() < 0.9f) {
            if (velocity.lengthSqr() > Mth.EPSILON) {
                EntityHitResult moveHit = getEntityHitResultOnlyOnMove(startOBB, velocity, level, exclude, filter);
                if (moveHit != null) return new EntityHitResultTimed(moveHit.getEntity(), moveHit.getLocation(), 0.0f);
            }
            return null;
        }

        AABB sweptHull = buildConservativeSweptHullCombined(startOBB, velocity, angularVelocity, axis, rotationPivot);

        List<Entity> candidates = level.getEntities(exclude, sweptHull, filter);
        if (candidates.isEmpty()) return null;

        EntityHitResultTimed earliestHit = null;
        float minTime = 1.000001f;

        for (Entity target : candidates) {
            if (!sweptHull.intersects(target.getBoundingBox())) continue;

            float hitTime = findCollisionInHullCombined(target, startOBB, velocity, angularVelocity, axis, rotationPivot);

            if (hitTime >= 0.0f && hitTime <= 1.0f) {
                if (hitTime < minTime) {
                    minTime = hitTime;
                    // 计算碰撞时刻的 OBB
                                    OBB collisionOBB = calculateOBBAtTime(startOBB, velocity, angularVelocity, axis, rotationPivot, hitTime);
                                    // 计算精确碰撞点（使用 SAT 精确计算）
                                    Vec3 collisionPoint = calculateCollisionPointPrecise(collisionOBB, target.getBoundingBox());                    // 如果没有精确碰撞点，使用目标位置作为后备
                    if (collisionPoint == null) {
                        collisionPoint = target.position();
                    }
                    earliestHit = new EntityHitResultTimed(target, collisionPoint, hitTime);
                    if (hitTime == 0.0f) return earliestHit;
                }
            }
        }

        return earliestHit;
    }

    /**
     * 【单个结果】重载：默认以 OBB 中心为旋转锚点
     */
    @Nullable
    public static EntityHitResultTimed getEntityHitResult(
            OBB startOBB,
            Vec3 velocity,
            float angularVelocity,
            Vec3 rotationAxis,
            Level level,
            @Nullable Entity exclude,
            Predicate<Entity> filter) {
        return getEntityHitResult(startOBB, velocity, angularVelocity, rotationAxis, startOBB.getCenter(), level, exclude, filter);
    }

    /**
     * 【新方法】直接接受角速度向量，不分解成轴和角度
     */
    @Nullable
    public static EntityHitResultTimed getEntityHitResult(
            OBB startOBB,
            Vec3 velocity,
            Vector3f angularVelocityVector,
            Vec3 rotationPivot,
            Level level,
            @Nullable Entity exclude,
            Predicate<Entity> filter) {

        if (velocity.lengthSqr() < Mth.EPSILON && angularVelocityVector.lengthSquared() < Mth.EPSILON * Mth.EPSILON) return null;

        AABB sweptHull = buildConservativeSweptHullCombinedByAngularVelocity(startOBB, velocity, angularVelocityVector, rotationPivot);

        List<Entity> candidates = level.getEntities(exclude, sweptHull, filter);
        if (candidates.isEmpty()) return null;

        EntityHitResultTimed earliestHit = null;
        float minTime = 1.000001f;

        for (Entity target : candidates) {
            if (!sweptHull.intersects(target.getBoundingBox())) continue;

            float hitTime = findCollisionCombinedByAngularVelocity(target, startOBB, velocity, angularVelocityVector, rotationPivot);

            if (hitTime >= 0.0f && hitTime <= 1.0f) {
                if (hitTime < minTime) {
                    minTime = hitTime;
                    // 计算碰撞时刻的 OBB
                    OBB collisionOBB = calculateOBBAtTime(startOBB, velocity, angularVelocityVector, rotationPivot, hitTime);
                    // 计算精确碰撞点（使用 SAT 精确计算）
                    Vec3 collisionPoint = calculateCollisionPointPrecise(collisionOBB, target.getBoundingBox());
                    // 如果没有精确碰撞点，使用目标位置作为后备
                    if (collisionPoint == null) {
                        collisionPoint = target.position();
                    }
                    earliestHit = new EntityHitResultTimed(target, collisionPoint, hitTime);
                    if (hitTime == 0.0f) return earliestHit;
                }
            }
        }

        return earliestHit;
    }

    /**
     * 【新方法】默认以 OBB 中心为旋转锚点
     */
    @Nullable
    public static EntityHitResultTimed getEntityHitResult(
            OBB startOBB,
            Vec3 velocity,
            Vector3f angularVelocityVector,
            Level level,
            @Nullable Entity exclude,
            Predicate<Entity> filter) {
        return getEntityHitResult(startOBB, velocity, angularVelocityVector, startOBB.getCenter(), level, exclude, filter);
    }

    // ==================== 内部核心逻辑 (平移+旋转) ====================

    /**
     * 在复合运动中寻找碰撞时间
     */
    private static float findCollisionInHullCombined(Entity target, OBB startOBB, Vec3 velocity, float totalAngle, Vec3 axis, Vec3 pivot) {
        // 检查 t=0
        if (target instanceof OBBHolder obbEntity) {
            OBB targetOBB = obbEntity.getOBB();
            if (targetOBB != null && startOBB.intersects(targetOBB)) return 0.0f;
        } else {
            if (startOBB.intersects(target.getBoundingBox())) return 0.0f;
        }

        // 线性扫描
        int steps = HULL_STEPS;
        float tStart = -1.0f;

        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            OBB currentOBB = calculateOBBAtTime(startOBB, velocity, totalAngle, axis, pivot, t);

            boolean hit = false;
            if (target instanceof OBBHolder obbEntity) {
                OBB targetOBB = obbEntity.getOBB();
                if (targetOBB != null) hit = currentOBB.intersects(targetOBB);
            } else {
                hit = currentOBB.intersects(target.getBoundingBox());
            }

            if (hit) {
                tStart = (float) (i - 1) / steps;
                break;
            }
        }

        if (tStart < 0) return -1.0f;

        // 二分查找
        float tEnd = Math.min(tStart + 1.0f / steps, 1.0f);
        for (int i = 0; i < BISECTION_ITERATIONS; i++) {
            float tMid = (tStart + tEnd) * 0.5f;
            OBB midOBB = calculateOBBAtTime(startOBB, velocity, totalAngle, axis, pivot, tMid);

            boolean hit = false;
            if (target instanceof OBBHolder obbEntity) {
                OBB targetOBB = obbEntity.getOBB();
                if (targetOBB != null) hit = midOBB.intersects(targetOBB);
            } else {
                hit = midOBB.intersects(target.getBoundingBox());
            }

            if (hit) {
                tEnd = tMid;
            } else {
                tStart = tMid;
            }
        }
        return tEnd;
    }

    /**
     * 计算时间 t 时的 OBB 状态
     * 逻辑：先平移，再绕轴旋转
     * 注意：这里的旋转是相对于初始 Pivot 的。
     * 公式：Pos(t) = Rotate(Pivot, StartPos + Velocity*t, Angle(t))
     * 但更直观的理解是：OBB 作为一个刚体，其中心随时间平移，同时自身绕轴旋转。
     * 为了简化，我们假设：
     * 1. 中心点移动：Center(t) = StartCenter + Velocity * t
     * 2. 姿态旋转：Rotation(t) = StartRotation + Axis * (TotalAngle * t)
     * 3. 但是，如果旋转轴不是穿过中心的，单纯的 "中心平移 + 自转" 是不对的，应该是 "绕固定轴螺旋运动"。

     * 修正模型：
     * 刚体上任意一点 P 的运动轨迹是螺旋线。
     * P(t) = RotateAround(Pivot, StartPos + Velocity_linear_along_axis * t, Axis, TotalAngle * t) + Velocity_perp * t ?

     * 更通用的游戏物理模型通常简化为：
     * 1. 计算当前时刻的中心位置：CurrentCenter = StartCenter + velocity * t
     * 2. 计算当前时刻的姿态：Rotate startOBB axes by (totalAngle * t) around axis.
     * 3. 但是，如果 pivot 不等于 center，旋转会导致中心偏离。

     * 最准确的“平移+旋转”定义（Screw Motion）：
     * 物体绕着一条空间直线（由 pivot 和 axis 定义）旋转，同时沿着该直线平移。
     * 但用户传入的是 arbitrary velocity (任意方向速度)。

     * 我们采用最常用的近似模型（适用于大多数游戏攻击判定）：
     * 在时间 t：
     * 1. 临时 OBB = startOBB 平移 (velocity * t)
     * 2. 最终 OBB = 临时 OBB 绕 (pivot, axis) 旋转 (totalAngle * t)
     * 这样既包含了位移带来的轨迹，也包含了旋转带来的扫掠。
     */
    private static OBB calculateOBBAtTime(OBB startOBB, Vec3 velocity, float totalAngle, Vec3 axis, Vec3 pivot, float t) {
        // 1. 先平移
        Vec3 translatedCenter = startOBB.getCenter().add(velocity.scale(t));
        OBB translatedOBB = new OBB(translatedCenter, startOBB.xHalfSize, startOBB.yHalfSize, startOBB.zHalfSize, startOBB.forward, startOBB.up);
        // 2. 再旋转（角速度向量已按时间缩放：angularVelocity = axis * totalAngle * t）
        Vec3 angularVelVec3 = axis.scale(totalAngle * t);
        return translatedOBB.rotateByAngularVelocity(angularVelVec3.toVector3f(), pivot);
    }

    /**
     * 构建复合运动的保守扫掠盒
     * 策略：取 t=0, t=0.5, t=1 三个时刻的 OBB 的并集，并适当膨胀
     */
    private static AABB buildConservativeSweptHullCombined(OBB startOBB, Vec3 velocity, float totalAngle, Vec3 axis, Vec3 pivot) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        // 采样关键点：起点、中间点、终点
        float[] times = {0.0f, 0.5f, 1.0f};

        for (float t : times) {
            OBB currentOBB = calculateOBBAtTime(startOBB, velocity, totalAngle, axis, pivot, t);
            AABB box = currentOBB.getBoundingAABB();
            if (box.minX < minX) minX = (float) box.minX;
            if (box.minY < minY) minY = (float) box.minY;
            if (box.minZ < minZ) minZ = (float) box.minZ;
            if (box.maxX > maxX) maxX = (float) box.maxX;
            if (box.maxY > maxY) maxY = (float) box.maxY;
            if (box.maxZ > maxZ) maxZ = (float) box.maxZ;
        }

        // 额外膨胀以覆盖采样点之间的空隙
        float epsilon = 0.5f + Math.max((float) velocity.length(), Math.abs(totalAngle) * 2.0f);
        // 上面的 epsilon 估算比较粗糙，为了安全起见，可以直接加一个固定值或者基于速度大小的值
        // 更保守的做法：直接计算起点和终点的最大距离

        return new AABB(minX - 0.5f, minY - 0.5f, minZ - 0.5f, maxX + 0.5f, maxY + 0.5f, maxZ + 0.5f);
    }

    // ============================================================
    // 4. 辅助方法 (纯旋转的扫掠盒构建，保留供参考)
    // ============================================================
    public static float findCollisionInHull(Entity target, OBB startOBB, float totalAngle, Vec3 axis, Vec3 pivot) {
        if (target instanceof OBBHolder obbEntity) {
            OBB targetOBB = obbEntity.getOBB();
            if (targetOBB != null) {
                if (startOBB.intersects(targetOBB)) return 0.0f;
                return binarySearchCollision(startOBB, targetOBB, totalAngle, axis, pivot);
            }
        }
        AABB targetBox = target.getBoundingBox();
        if (startOBB.intersects(targetBox)) return 0.0f;
        return binarySearchCollision(startOBB, targetBox, totalAngle, axis, pivot);
    }

    public static float binarySearchCollision(OBB startOBB, OBB targetOBB, float totalAngle, Vec3 axis, Vec3 pivot) {
        int steps = HULL_STEPS;
        float tStart = -1.0f;
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
        float tEnd = Math.min(tStart + 1.0f / steps, 1.0f);
        for (int i = 0; i < BISECTION_ITERATIONS; i++) {
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

    public static float binarySearchCollision(OBB startOBB, AABB targetBox, float totalAngle, Vec3 axis, Vec3 pivot) {
        int steps = HULL_STEPS;
        float tStart = -1.0f;
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
        float tEnd = Math.min(tStart + 1.0f / steps, 1.0f);
        for (int i = 0; i < BISECTION_ITERATIONS; i++) {
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

    public static AABB buildConservativeSweptHull(OBB obb, float totalRadian, Vec3 axis, Vec3 pivot) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        Vec3[] verts = obb.getVertices();
        int samples = 64;
        float step = totalRadian / samples;

        float ax = (float) axis.x, ay = (float) axis.y, az = (float) axis.z;
        float px = (float) pivot.x, py = (float) pivot.y, pz = (float) pivot.z;

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
        return new AABB(minX - epsilon, minY - epsilon, minZ - epsilon, maxX + epsilon, maxY + epsilon, maxZ + epsilon);
    }

    // ==================== 使用角速度向量的辅助方法 ====================

    /**
     * 使用角速度向量查找碰撞时间
     */
    private static float findCollisionInHull(Entity target, OBB startOBB, Vector3f angularVelocityVector, Vec3 pivot) {
        // 检查 t=0
        if (target instanceof OBBHolder obbEntity) {
            OBB targetOBB = obbEntity.getOBB();
            if (targetOBB != null && startOBB.intersects(targetOBB)) return 0.0f;
        } else {
            if (startOBB.intersects(target.getBoundingBox())) return 0.0f;
        }

        // 线性扫描
        int steps = HULL_STEPS;
        float tStart = -1.0f;

        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            // 直接使用角速度向量，不分解
            Vector3f scaledAngularVel = new Vector3f(angularVelocityVector).mul(t);
            OBB currentOBB = startOBB.rotateByAngularVelocity(scaledAngularVel, pivot);

            boolean hit = false;
            if (target instanceof OBBHolder obbEntity) {
                OBB targetOBB = obbEntity.getOBB();
                if (targetOBB != null) hit = currentOBB.intersects(targetOBB);
            } else {
                hit = currentOBB.intersects(target.getBoundingBox());
            }

            if (hit) {
                tStart = (float) (i - 1) / steps;
                break;
            }
        }

        if (tStart < 0) return -1.0f;

        // 二分查找
        float tEnd = Math.min(tStart + 1.0f / steps, 1.0f);
        for (int i = 0; i < BISECTION_ITERATIONS; i++) {
            float tMid = (tStart + tEnd) * 0.5f;
            Vector3f scaledAngularVel = new Vector3f(angularVelocityVector).mul(tMid);
            OBB midOBB = startOBB.rotateByAngularVelocity(scaledAngularVel, pivot);

            boolean hit = false;
            if (target instanceof OBBHolder obbEntity) {
                OBB targetOBB = obbEntity.getOBB();
                if (targetOBB != null) hit = midOBB.intersects(targetOBB);
            } else {
                hit = midOBB.intersects(target.getBoundingBox());
            }

            if (hit) {
                tEnd = tMid;
            } else {
                tStart = tMid;
            }
        }
        return tEnd;
    }

    /**
     * 使用角速度向量构建保守扫掠盒
     */
    private static AABB buildConservativeSweptHull(OBB startOBB, Vector3f angularVelocityVector, Vec3 pivot) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        // 采样关键点：起点、中间点、终点
        float[] times = {0.0f, 0.5f, 1.0f};

        for (float t : times) {
            Vector3f scaledAngularVel = new Vector3f(angularVelocityVector).mul(t);
            OBB currentOBB = startOBB.rotateByAngularVelocity(scaledAngularVel, pivot);
            AABB box = currentOBB.getBoundingAABB();
            if (box.minX < minX) minX = (float) box.minX;
            if (box.minY < minY) minY = (float) box.minY;
            if (box.minZ < minZ) minZ = (float) box.minZ;
            if (box.maxX > maxX) maxX = (float) box.maxX;
            if (box.maxY > maxY) maxY = (float) box.maxY;
            if (box.maxZ > maxZ) maxZ = (float) box.maxZ;
        }

        // 膨胀以覆盖采样点之间的空隙
        float maxAngularVel = angularVelocityVector.length();
        float epsilon = 0.5f + Math.abs(maxAngularVel) * 2.0f;

        return new AABB(minX - epsilon, minY - epsilon, minZ - epsilon, maxX + epsilon, maxY + epsilon, maxZ + epsilon);
    }

    /**
     * 平移+旋转：使用角速度向量查找碰撞时间
     */
    private static float findCollisionCombinedByAngularVelocity(Entity target, OBB startOBB, Vec3 velocity, Vector3f angularVelocityVector, Vec3 pivot) {
        // 检查 t=0
        if (target instanceof OBBHolder obbEntity) {
            OBB targetOBB = obbEntity.getOBB();
            if (targetOBB != null && startOBB.intersects(targetOBB)) return 0.0f;
        } else {
            if (startOBB.intersects(target.getBoundingBox())) return 0.0f;
        }

        // 线性扫描
        int steps = HULL_STEPS;
        float tStart = -1.0f;

        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            OBB currentOBB = calculateOBBAtTime(startOBB, velocity, angularVelocityVector, pivot, t);

            boolean hit = false;
            if (target instanceof OBBHolder obbEntity) {
                OBB targetOBB = obbEntity.getOBB();
                if (targetOBB != null) hit = currentOBB.intersects(targetOBB);
            } else {
                hit = currentOBB.intersects(target.getBoundingBox());
            }

            if (hit) {
                tStart = (float) (i - 1) / steps;
                break;
            }
        }

        if (tStart < 0) return -1.0f;

        // 二分查找
        float tEnd = Math.min(tStart + 1.0f / steps, 1.0f);
        for (int i = 0; i < BISECTION_ITERATIONS; i++) {
            float tMid = (tStart + tEnd) * 0.5f;
            OBB midOBB = calculateOBBAtTime(startOBB, velocity, angularVelocityVector, pivot, tMid);

            boolean hit = false;
            if (target instanceof OBBHolder obbEntity) {
                OBB targetOBB = obbEntity.getOBB();
                if (targetOBB != null) hit = midOBB.intersects(targetOBB);
            } else {
                hit = midOBB.intersects(target.getBoundingBox());
            }

            if (hit) {
                tEnd = tMid;
            } else {
                tStart = tMid;
            }
        }
        return tEnd;
    }

    /**
     * 平移+旋转：使用角速度向量计算时间 t 时的 OBB
     */
    private static OBB calculateOBBAtTime(OBB startOBB, Vec3 velocity, Vector3f angularVelocityVector, Vec3 pivot, float t) {
        // 1. 先平移
        Vec3 translatedCenter = startOBB.getCenter().add(velocity.scale(t));
        OBB translatedOBB = new OBB(translatedCenter, startOBB.xHalfSize, startOBB.yHalfSize, startOBB.zHalfSize, startOBB.forward, startOBB.up);

        // 2. 再旋转（角速度向量已按时间缩放）
        Vector3f scaledAngularVel = new Vector3f(angularVelocityVector).mul(t);
        return translatedOBB.rotateByAngularVelocity(scaledAngularVel, pivot);
    }

    /**
     * 平移+旋转：使用角速度向量构建保守扫掠盒
     */
    private static AABB buildConservativeSweptHullCombinedByAngularVelocity(OBB startOBB, Vec3 velocity, Vector3f angularVelocityVector, Vec3 pivot) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        // 采样关键点：起点、中间点、终点
        float[] times = {0.0f, 0.5f, 1.0f};

        for (float t : times) {
            OBB currentOBB = calculateOBBAtTime(startOBB, velocity, angularVelocityVector, pivot, t);
            AABB box = currentOBB.getBoundingAABB();
            if (box.minX < minX) minX = (float) box.minX;
            if (box.minY < minY) minY = (float) box.minY;
            if (box.minZ < minZ) minZ = (float) box.minZ;
            if (box.maxX > maxX) maxX = (float) box.maxX;
            if (box.maxY > maxY) maxY = (float) box.maxY;
            if (box.maxZ > maxZ) maxZ = (float) box.maxZ;
        }

        // 额外膨胀以覆盖采样点之间的空隙
        float epsilon = 0.5f + Math.max((float) velocity.length(), angularVelocityVector.length() * 2.0f);

        return new AABB(minX - epsilon, minY - epsilon, minZ - epsilon, maxX + epsilon, maxY + epsilon, maxZ + epsilon);
    }

    // ============================================================
    // 5. 精确碰撞点计算
    // ============================================================

    /**
     * 计算 OBB 和 AABB 的精确碰撞点
     * @param obb 碰撞时的 OBB
     * @param aabb 目标实体的 AABB
     * @return 精确碰撞点位置（如果没有碰撞则返回 null）
     */
    @Nullable
    public static Vec3 calculateCollisionPoint(OBB obb, AABB aabb) {
        // 找出 OBB 上所有点中，距离 AABB 边界最近的点
        // 这个点最可能是实际碰撞点

        List<Vec3> candidatePoints = new ArrayList<>();

        // 1. 添加 OBB 的 8 个顶点
        Vec3[] obbVertices = obb.getVertices();
        for (Vec3 vertex : obbVertices) {
            candidatePoints.add(vertex);
        }

        // 2. 添加 OBB 的 6 个面中心
        // 面1: X 正方向
        candidatePoints.add(obb.getCenter().add(obb.right.scale(obb.xHalfSize)));
        // 面2: X 负方向
        candidatePoints.add(obb.getCenter().subtract(obb.right.scale(obb.xHalfSize)));
        // 面3: Y 正方向（上）
        candidatePoints.add(obb.getCenter().add(obb.up.scale(obb.yHalfSize)));
        // 面4: Y 负方向（下）
        candidatePoints.add(obb.getCenter().subtract(obb.up.scale(obb.yHalfSize)));
        // 面5: Z 正方向（前）
        candidatePoints.add(obb.getCenter().add(obb.forward.scale(obb.zHalfSize)));
        // 面6: Z 负方向（后）
        candidatePoints.add(obb.getCenter().subtract(obb.forward.scale(obb.zHalfSize)));

        // 3. 添加 OBB 的 12 条边的中点
        // 4 条边连接上下面 (Y 轴)
        candidatePoints.add(obb.getCenter().add(obb.right.scale(obb.xHalfSize)).add(obb.up.scale(obb.yHalfSize)));
        candidatePoints.add(obb.getCenter().subtract(obb.right.scale(obb.xHalfSize)).add(obb.up.scale(obb.yHalfSize)));
        candidatePoints.add(obb.getCenter().add(obb.right.scale(obb.xHalfSize)).subtract(obb.up.scale(obb.yHalfSize)));
        candidatePoints.add(obb.getCenter().subtract(obb.right.scale(obb.xHalfSize)).subtract(obb.up.scale(obb.yHalfSize)));
        candidatePoints.add(obb.getCenter().add(obb.forward.scale(obb.zHalfSize)).add(obb.up.scale(obb.yHalfSize)));
        candidatePoints.add(obb.getCenter().subtract(obb.forward.scale(obb.zHalfSize)).add(obb.up.scale(obb.yHalfSize)));
        candidatePoints.add(obb.getCenter().add(obb.forward.scale(obb.zHalfSize)).subtract(obb.up.scale(obb.yHalfSize)));
        candidatePoints.add(obb.getCenter().subtract(obb.forward.scale(obb.zHalfSize)).subtract(obb.up.scale(obb.yHalfSize)));

        // 4 条边连接前后面 (Z 轴)
        candidatePoints.add(obb.getCenter().add(obb.forward.scale(obb.zHalfSize)).add(obb.right.scale(obb.xHalfSize)));
        candidatePoints.add(obb.getCenter().subtract(obb.forward.scale(obb.zHalfSize)).add(obb.right.scale(obb.xHalfSize)));
        candidatePoints.add(obb.getCenter().add(obb.forward.scale(obb.zHalfSize)).subtract(obb.right.scale(obb.xHalfSize)));
        candidatePoints.add(obb.getCenter().subtract(obb.forward.scale(obb.zHalfSize)).subtract(obb.right.scale(obb.xHalfSize)));
        candidatePoints.add(obb.getCenter().add(obb.forward.scale(obb.zHalfSize)).add(obb.up.scale(obb.yHalfSize)));
        candidatePoints.add(obb.getCenter().subtract(obb.forward.scale(obb.zHalfSize)).add(obb.up.scale(obb.yHalfSize)));
        candidatePoints.add(obb.getCenter().add(obb.forward.scale(obb.zHalfSize)).subtract(obb.up.scale(obb.yHalfSize)));
        candidatePoints.add(obb.getCenter().subtract(obb.forward.scale(obb.zHalfSize)).subtract(obb.up.scale(obb.yHalfSize)));

        // 4 条边连接左右面 (X 轴)
        candidatePoints.add(obb.getCenter().add(obb.right.scale(obb.xHalfSize)).add(obb.forward.scale(obb.zHalfSize)));
        candidatePoints.add(obb.getCenter().subtract(obb.right.scale(obb.xHalfSize)).add(obb.forward.scale(obb.zHalfSize)));
        candidatePoints.add(obb.getCenter().add(obb.right.scale(obb.xHalfSize)).subtract(obb.forward.scale(obb.zHalfSize)));
        candidatePoints.add(obb.getCenter().subtract(obb.right.scale(obb.xHalfSize)).subtract(obb.forward.scale(obb.zHalfSize)));

        // 计算每个点到 AABB 边界的距离，找出最近的点
        Vec3 closestPoint = null;
        double minDistanceToBoundary = Double.POSITIVE_INFINITY;

        for (Vec3 point : candidatePoints) {
            double distanceToBoundary = getDistanceToAABBBoundary(point, aabb);
            if (distanceToBoundary < minDistanceToBoundary) {
                minDistanceToBoundary = distanceToBoundary;
                closestPoint = point;
            }
        }

        if (closestPoint != null) {
            log.debug("OBB 上最接近 AABB 边界的点：{}，距离边界：{}", closestPoint, minDistanceToBoundary);
            return closestPoint;
        }

        return null;
    }

    /**
     * 计算点到 AABB 边界的距离
     * 如果点在 AABB 内部，距离为负数（表示到最近的边界的距离）
     * 如果点在 AABB 外部，距离为正数（表示到最近的边界的距离）
     */
    private static double getDistanceToAABBBoundary(Vec3 point, AABB aabb) {
        double dx = 0.0;
        if (point.x < aabb.minX) {
            dx = aabb.minX - point.x;
        } else if (point.x > aabb.maxX) {
            dx = point.x - aabb.maxX;
        }

        double dy = 0.0;
        if (point.y < aabb.minY) {
            dy = aabb.minY - point.y;
        } else if (point.y > aabb.maxY) {
            dy = point.y - aabb.maxY;
        }

        double dz = 0.0;
        if (point.z < aabb.minZ) {
            dz = aabb.minZ - point.z;
        } else if (point.z > aabb.maxZ) {
            dz = point.z - aabb.maxZ;
        }

        // 计算到 AABB 边界的距离
        // 如果点在 AABB 内部（所有 dx, dy, dz 都 <= 0），找到最小距离的绝对值
        // 如果点在 AABB 外部，计算到最近边界的距离
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 如果点在 AABB 内部，返回负数表示距离边界的距离
        if (point.x >= aabb.minX && point.x <= aabb.maxX &&
            point.y >= aabb.minY && point.y <= aabb.maxY &&
            point.z >= aabb.minZ && point.z <= aabb.maxZ) {
            // 点在内部，找到到最近边界的距离
            double minDist = Math.min(Math.min(aabb.minX - point.x, point.x - aabb.maxX),
                                     Math.min(aabb.minY - point.y, point.y - aabb.maxY));
            minDist = Math.min(minDist, Math.min(aabb.minZ - point.z, point.z - aabb.maxZ));
            return Math.abs(minDist);
        }

        return distance;
    }

    /**
     * 检查点是否在 AABB 内
     */
    private static boolean isPointInsideAABB(Vec3 point, AABB aabb) {
        return point.x >= aabb.minX && point.x <= aabb.maxX &&
               point.y >= aabb.minY && point.y <= aabb.maxY &&
               point.z >= aabb.minZ && point.z <= aabb.maxZ;
    }

    /**
     * 检查点是否在 OBB 内
     */
    private static boolean isPointInsideOBB(Vec3 point, OBB obb) {
        // 将点转换到 OBB 的局部坐标系
        Vec3 relativePoint = point.subtract(obb.getCenter());

        // 投影到 OBB 的三个轴上
        float x = (float) relativePoint.dot(obb.right);
        float y = (float) relativePoint.dot(obb.up);
        float z = (float) relativePoint.dot(obb.forward);

        // 检查是否在 OBB 的尺寸范围内
        return Math.abs(x) <= obb.xHalfSize &&
               Math.abs(y) <= obb.yHalfSize &&
               Math.abs(z) <= obb.zHalfSize;
    }

    /**
     * 获取 AABB 的 8 个顶点
     */
    private static Vec3[] getAABBVertices(AABB aabb) {
        return new Vec3[]{
            new Vec3(aabb.minX, aabb.minY, aabb.minZ),
            new Vec3(aabb.maxX, aabb.minY, aabb.minZ),
            new Vec3(aabb.maxX, aabb.minY, aabb.maxZ),
            new Vec3(aabb.minX, aabb.minY, aabb.maxZ),
            new Vec3(aabb.minX, aabb.maxY, aabb.minZ),
            new Vec3(aabb.maxX, aabb.maxY, aabb.minZ),
            new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ),
            new Vec3(aabb.minX, aabb.maxY, aabb.maxZ)
        };
    }

    /**
     * [SAT版本] 计算 OBB 和 AABB 的精确碰撞点
     * 使用分离轴定理找出真正的接触点
     */
    public static Vec3 calculateCollisionPointPrecise(OBB obb, AABB aabb) {
        // 收集所有可能的接触特征
        List<Vec3> contactFeatures = new ArrayList<>();
        
        // 1. OBB的顶点
        Vec3[] obbVertices = obb.getVertices();
        for (Vec3 vertex : obbVertices) {
            if (isPointInsideAABB(vertex, aabb)) {
                contactFeatures.add(vertex);
            }
        }
        
        // 2. AABB的顶点
        Vec3[] aabbVertices = getAABBVertices(aabb);
        for (Vec3 vertex : aabbVertices) {
            if (isPointInsideOBB(vertex, obb)) {
                contactFeatures.add(vertex);
            }
        }
        
        // 3. 边面交点
        contactFeatures.addAll(getEdgeFaceIntersections(obb, aabb));
        
        // 4. 选择穿透最深的点
        Vec3 bestPoint = null;
        double maxPenetration = Double.NEGATIVE_INFINITY;
        
        for (Vec3 point : contactFeatures) {
            double penetration = getPenetrationDepth(point, obb, aabb);
            if (penetration > maxPenetration) {
                maxPenetration = penetration;
                bestPoint = point;
            }
        }
        
        if (bestPoint == null) {
            bestPoint = obb.getCenter();
        }
        
//        log.debug("SAT碰撞点计算：特征数={}, 选择点={}, 穿透深度={}", contactFeatures.size(), bestPoint, maxPenetration);
        return bestPoint;
    }
    
    /**
     * 获取边与面的交点
     */
    private static List<Vec3> getEdgeFaceIntersections(OBB obb, AABB aabb) {
        List<Vec3> intersections = new ArrayList<>();
        
        // AABB的边与OBB的面
        Vec3[] v = getAABBVertices(aabb);
        Vec3[][] aabbEdges = {
            {v[0], v[1]}, {v[1], v[2]}, {v[2], v[3]}, {v[3], v[0]},
            {v[4], v[5]}, {v[5], v[6]}, {v[6], v[7]}, {v[7], v[4]},
            {v[0], v[4]}, {v[1], v[5]}, {v[2], v[6]}, {v[3], v[7]}
        };
        
        Vec3 center = obb.getCenter();
        Vec3[][] obbFaces = {
            {center.add(obb.right.scale(obb.xHalfSize)), obb.right},
            {center.subtract(obb.right.scale(obb.xHalfSize)), obb.right.reverse()},
            {center.add(obb.up.scale(obb.yHalfSize)), obb.up},
            {center.subtract(obb.up.scale(obb.yHalfSize)), obb.up.reverse()},
            {center.add(obb.forward.scale(obb.zHalfSize)), obb.forward},
            {center.subtract(obb.forward.scale(obb.zHalfSize)), obb.forward.reverse()}
        };
        
        for (Vec3[] edge : aabbEdges) {
            for (Vec3[] face : obbFaces) {
                Vec3 intersection = linePlaneIntersection(edge[0], edge[1], face[0], face[1]);
                if (intersection != null && isPointInsideOBB(intersection, obb)) {
                    intersections.add(intersection);
                }
            }
        }
        
        // OBB的边与AABB的面
        Vec3[] ov = obb.getVertices();
        Vec3[][] obbEdges = {
            {ov[0], ov[1]}, {ov[1], ov[2]}, {ov[2], ov[3]}, {ov[3], ov[0]},
            {ov[4], ov[5]}, {ov[5], ov[6]}, {ov[6], ov[7]}, {ov[7], ov[4]},
            {ov[0], ov[4]}, {ov[1], ov[5]}, {ov[2], ov[6]}, {ov[3], ov[7]}
        };
        
        Vec3[][] aabbFaces = {
            {new Vec3(aabb.maxX, aabb.minY, aabb.minZ), new Vec3(1, 0, 0)},
            {new Vec3(aabb.minX, aabb.minY, aabb.minZ), new Vec3(-1, 0, 0)},
            {new Vec3(aabb.minX, aabb.maxY, aabb.minZ), new Vec3(0, 1, 0)},
            {new Vec3(aabb.minX, aabb.minY, aabb.minZ), new Vec3(0, -1, 0)},
            {new Vec3(aabb.minX, aabb.minY, aabb.maxZ), new Vec3(0, 0, 1)},
            {new Vec3(aabb.minX, aabb.minY, aabb.minZ), new Vec3(0, 0, -1)}
        };
        
        for (Vec3[] edge : obbEdges) {
            for (Vec3[] face : aabbFaces) {
                Vec3 intersection = linePlaneIntersection(edge[0], edge[1], face[0], face[1]);
                if (intersection != null && isPointInsideAABB(intersection, aabb)) {
                    intersections.add(intersection);
                }
            }
        }
        
        return intersections;
    }
    
    /**
     * 线段与平面的交点
     */
    private static Vec3 linePlaneIntersection(Vec3 lineStart, Vec3 lineEnd, Vec3 planePoint, Vec3 planeNormal) {
        Vec3 lineDir = lineEnd.subtract(lineStart);
        double denominator = lineDir.dot(planeNormal);
        
        if (Math.abs(denominator) < 1e-10) {
            return null;
        }
        
        double t = planePoint.subtract(lineStart).dot(planeNormal) / denominator;
        
        if (t < 0 || t > 1) {
            return null;
        }
        
        return lineStart.add(lineDir.scale(t));
    }
    
    /**
     * 计算穿透深度
     */
    private static double getPenetrationDepth(Vec3 point, OBB obb, AABB aabb) {
        return -getDistanceToAABBBoundary(point, aabb);
    }
}