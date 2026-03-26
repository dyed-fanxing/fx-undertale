package com.fanxing.fx_undertale.utils.collsion;

import com.fanxing.fx_undertale.common.phys.OBB;
import com.fanxing.fx_undertale.entity.capability.OBBable;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
                hits.add(new EntityHitResultTimed(target, target.position(), hitTime));
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
                    earliestHit = new EntityHitResultTimed(target, target.position(), hitTime);
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
                hits.add(new EntityHitResultTimed(target, target.position(), hitTime));
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
                    earliestHit = new EntityHitResultTimed(target, target.position(), hitTime);
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

    // ==================== 内部核心逻辑 (平移+旋转) ====================

    /**
     * 在复合运动中寻找碰撞时间
     */
    private static float findCollisionInHullCombined(Entity target, OBB startOBB, Vec3 velocity, float totalAngle, Vec3 axis, Vec3 pivot) {
        // 检查 t=0
        if (target instanceof OBBable obbEntity) {
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
            if (target instanceof OBBable obbEntity) {
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
            if (target instanceof OBBable obbEntity) {
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
     *
     * 修正模型：
     * 刚体上任意一点 P 的运动轨迹是螺旋线。
     * P(t) = RotateAround(Pivot, StartPos + Velocity_linear_along_axis * t, Axis, TotalAngle * t) + Velocity_perp * t ?
     *
     * 更通用的游戏物理模型通常简化为：
     * 1. 计算当前时刻的中心位置：CurrentCenter = StartCenter + velocity * t
     * 2. 计算当前时刻的姿态：Rotate startOBB axes by (totalAngle * t) around axis.
     * 3. 但是，如果 pivot 不等于 center，旋转会导致中心偏离。
     *
     * 最准确的“平移+旋转”定义（Screw Motion）：
     * 物体绕着一条空间直线（由 pivot 和 axis 定义）旋转，同时沿着该直线平移。
     * 但用户传入的是 arbitrary velocity (任意方向速度)。
     *
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

        // 2. 再旋转
        float currentAngle = totalAngle * t;
        return translatedOBB.rotateAround(currentAngle, axis, pivot);
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
        if (target instanceof OBBable obbEntity) {
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

    public static AABB buildConservativeSweptHull(OBB obb, float totalAngle, Vec3 axis, Vec3 pivot) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        Vec3[] verts = obb.getVertices();
        int samples = 64;
        if (samples <= 0) samples = 1;
        float step = totalAngle / samples;

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
}