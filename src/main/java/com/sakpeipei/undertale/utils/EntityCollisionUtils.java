package com.sakpeipei.undertale.utils;

import com.sakpeipei.undertale.common.phys.OBB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class EntityCollisionUtils {

    private static final Logger log = LoggerFactory.getLogger(ProjectileUtils.class);

    /**
     * 获取弹射物在移动方向上的碰撞结果列表，默认自身范围，无额外扩大碰撞检测
     */
    public static List<HitResult> getHitResultsOnMoveVector(Entity entity, Predicate<Entity> filter, ClipContext.Block blockClip, boolean allowStaticHit) {
        Vec3 from = entity.getBoundingBox().getCenter();
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 to = from.add(velocity);
        BlockHitResult blockHitResult = entity.level().clip(new ClipContext(from, to, blockClip, ClipContext.Fluid.NONE, entity));
        if(blockHitResult.getType() != HitResult.Type.MISS){
            Vec3 location = blockHitResult.getLocation();
            to = new Vec3(location.x, to.y, location.z);
        }
        float halfWidth = entity.getBbWidth() * 0.5f;
        List<HitResult> hitResults = new ArrayList<>(getEntityHitResults(entity,from,to,halfWidth,entity.getBbHeight()*0.5f,halfWidth,entity.getBoundingBox().expandTowards(velocity), filter, allowStaticHit));
        hitResults.add(blockHitResult);
        return hitResults;
    }

    /**
     * 获取弹射物移动向量上的实体碰撞检测结果列表
     */
    public static List<EntityHitResult> getEntityHitResultsOnMoveVector(Entity entity, Predicate<Entity> filter, boolean allowStaticHit){
        float halfWidth = entity.getBbWidth() * 0.5f;
        Vec3 from = entity.getBoundingBox().getCenter();
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 to = from.add(velocity);
        return getEntityHitResults(entity, from, to, halfWidth,entity.getBbHeight() * 0.5f,halfWidth,entity.getBoundingBox().expandTowards(velocity), filter, allowStaticHit);
    }
    /**
     * 获取弹射物移动向量上的实体碰撞检测结果列表
     */
    public static List<EntityHitResult> getEntityHitResults(Entity entity, Vec3 from, Vec3 to,Predicate<Entity> filter,boolean allowStaticHit){
        float halfWidth = entity.getBbWidth() * 0.5f;
        return getEntityHitResults(entity, from, to, halfWidth,entity.getBbHeight() * 0.5f,halfWidth,entity.getBoundingBox().expandTowards(entity.getDeltaMovement()), filter, allowStaticHit);
    }

    /**
     * 获取弹射物移动向量上的实体碰撞检测结果列表
     */
    public static List<EntityHitResult> getEntityHitResults(Entity entity, Vec3 from, Vec3 to, AABB searchArea, Predicate<Entity> filter, boolean allowStaticHit){
        float halfWidth = entity.getBbWidth() * 0.5f;
        return getEntityHitResults(entity, from, to, halfWidth,entity.getBbHeight() * 0.5f,halfWidth,searchArea, filter, allowStaticHit);
    }

    /**
     * 获取弹射物在线段之间的实体碰撞检测结果列表
     * 原理是起点from和终点to的线段和目标AABB碰撞箱扩大后的碰撞箱是否碰撞，是射线和AABB碰撞箱的检测
     *
     * @param entity 实体
     * @param from 射线起点
     * @param to 射线终点
     * @param inflateX,inflateY,inflateZ 扩大目标碰撞箱的检测范围，即实体自身碰撞箱一半
     * @param searchArea 粗略筛选实体的AABB碰撞箱
     * @param filter 过滤器
     * @param allowStaticHit 是否允许静态碰撞，即实体静止不同时是否检测碰撞
     */
    public static List<EntityHitResult> getEntityHitResults(Entity entity, Vec3 from, Vec3 to, double inflateX,double inflateY,double inflateZ, AABB searchArea, Predicate<Entity> filter,boolean allowStaticHit) {
        Level level = entity.level();
        List<EntityHitResult> results = new ArrayList<>();
        List<Entity> entities = level.getEntities(entity, searchArea, filter);
        for (Entity entity1 : entities) {
            AABB aabb = entity1.getBoundingBox().inflate(inflateX,inflateY,inflateZ);
            if(allowStaticHit){
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




    // ============== OBB实体碰撞检测（新增） ==============
    /**
     * 获取OBB在移动过程中会碰撞的实体列表
     * 原理：将OBB沿速度方向扩展，检测扩展后的OBB与实体的碰撞
     */
    public static List<EntityHitResult> getEntityHitResults(Entity entity,OBB obb,Vec3 velocity,Predicate<Entity> filter) {
        List<EntityHitResult> results = new ArrayList<>();

        // 1. 获取沿速度方向扩展的OBB
        OBB extendedOBB = obb.expandTowards(velocity);
        // 2. 获取扩展OBB的搜索范围（稍微扩大一点）
        AABB searchArea = extendedOBB.getBoundingAABB().inflate(0.5);
        // 3. 搜索范围内的所有实体
        List<Entity> potentialEntities = entity.level().getEntities(entity, searchArea, filter);
        // 4. 检查每个实体是否会与扩展OBB碰撞
        for (Entity entity1 : potentialEntities) {
            if (extendedOBB.intersects(entity1.getBoundingBox())) {
                // 计算近似碰撞点（实体中心）
                Vec3 hitPoint = entity1.getBoundingBox().getCenter();
                results.add(new EntityHitResult(entity1, hitPoint));
            }
        }
        // 5. 按距离排序（从近到远）
        results.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(obb.getCenter())));
        return results;
    }

    /**
     * 快速检测：OBB是否会与指定实体碰撞
     */
    public static boolean willHitEntity(OBB obb,Vec3 velocity,Entity entity) {
        // 获取扩展OBB
        OBB extendedOBB = obb.expandTowards(velocity);
        // 检查扩展OBB是否与实体AABB相交
        return extendedOBB.intersects(entity.getBoundingBox());
    }


    // ============== 高级碰撞检测（带碰撞时间） ==============

    /**
     * 获取碰撞时间估计（0.0-1.0之间）
     * 返回-1表示不会碰撞
     */
    public static double estimateCollisionTime(
            OBB obb,
            Vec3 velocity,
            Entity entity) {

        // 如果已经相交，碰撞时间=0
        if (obb.intersects(entity.getBoundingBox())) {
            return 0.0;
        }

        // 如果扩展后也不相交，肯定不会碰撞
        if (!willHitEntity(obb, velocity, entity)) {
            return -1.0;
        }

        // 简单估算：基于中心点距离和速度方向
        Vec3 obbCenter = obb.getCenter();
        AABB entityBox = entity.getBoundingBox();
        Vec3 entityCenter = entityBox.getCenter();

        // 从OBB中心指向实体中心的向量
        Vec3 toEntity = entityCenter.subtract(obbCenter);

        // 速度长度
        double speed = velocity.length();
        if (speed < 1e-7) {
            return -1.0;
        }

        // 速度方向
        Vec3 velocityDir = velocity.normalize();

        // 实体中心在速度方向上的投影
        double projection = toEntity.dot(velocityDir);

        // 如果投影为负，实体在速度反方向
        if (projection < 0) {
            return -1.0;
        }
        // 计算沿速度方向的距离
        // 估算碰撞时间
        double estimatedTime = projection / speed;

        // 确保在合理范围内
        if (estimatedTime < 0 || estimatedTime > 1.0) {
            return -1.0;
        }

        return Math.min(estimatedTime, 1.0);
    }

    /**
     * 获取带碰撞时间的实体碰撞结果
     */
    public static List<TimedEntityHit> getTimedEntityCollisions(Entity entity,OBB obb,Vec3 velocity,Predicate<Entity> filter) {
        List<TimedEntityHit> results = new ArrayList<>();
        // 先获取所有可能碰撞的实体
        List<EntityHitResult> hits = getEntityHitResults(entity, obb, velocity, filter);
        for (EntityHitResult hit : hits) {
            double collisionTime = estimateCollisionTime(obb, velocity, hit.getEntity());
            if (collisionTime >= 0) {
                results.add(new TimedEntityHit(hit.getEntity(), hit.getLocation(), collisionTime));
            }
        }

        // 按碰撞时间排序（从早到晚）
        results.sort(Comparator.comparingDouble(TimedEntityHit::collisionTime));

        return results;
    }

    // ============== 辅助类 ==============

    /**
     * 带碰撞时间的实体命中结果
     *
     * @param collisionTime 0.0-1.0
     */
        public record TimedEntityHit(Entity entity, Vec3 hitPoint, double collisionTime) {
            public TimedEntityHit(Entity entity, Vec3 hitPoint, double collisionTime) {
                this.entity = entity;
                this.hitPoint = hitPoint;
                this.collisionTime = Math.max(0, Math.min(1, collisionTime));
            }

            public Vec3 getPositionAtTime() {
                return entity.position().add(entity.getDeltaMovement().scale(collisionTime));
            }
        }

    // ============== 实用工具方法 ==============

    /**
     * 获取OBB沿移动路径的保守AABB（用于实体搜索优化）
     */
    public static AABB getConservativeSearchArea(OBB obb, Vec3 velocity) {
        // 获取扩展OBB的AABB，并适当扩大
        OBB extendedOBB = obb.expandTowards(velocity);
        return extendedOBB.getBoundingAABB().inflate(1.0);
    }

    /**
     * 批量检测：OBB是否会与实体列表中的任何一个碰撞
     */
    public static boolean willHitAnyEntity(
            OBB obb,
            Vec3 velocity,
            List<Entity> entities) {

        OBB extendedOBB = obb.expandTowards(velocity);

        for (Entity entity : entities) {
            if (extendedOBB.intersects(entity.getBoundingBox())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取第一个会碰撞的实体（性能优化版）
     */
    public static EntityHitResult getFirstEntityHit(
            Level level,
            Entity exclude,
            OBB obb,
            Vec3 velocity,
            Predicate<Entity> filter) {

        AABB searchArea = getConservativeSearchArea(obb, velocity);
        List<Entity> entities = level.getEntities(exclude, searchArea, filter);

        OBB extendedOBB = obb.expandTowards(velocity);

        for (Entity entity : entities) {
            if (extendedOBB.intersects(entity.getBoundingBox())) {
                return new EntityHitResult(entity, entity.getBoundingBox().getCenter());
            }
        }

        return null;
    }
}