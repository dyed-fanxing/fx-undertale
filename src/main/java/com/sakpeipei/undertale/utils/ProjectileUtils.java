package com.sakpeipei.undertale.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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

public class ProjectileUtils {

    private static final Logger log = LoggerFactory.getLogger(ProjectileUtils.class);

    /**
     * 获取弹射物在移动方向上的碰撞结果列表，默认自身范围，无额外扩大碰撞检测
     */
    public static List<HitResult> getHitResultsOnMoveVector(Entity entity, Predicate<Entity> filter, ClipContext.Block blockClip) {
        Vec3 from = entity.getBoundingBox().getCenter();
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 to = from.add(velocity);
        BlockHitResult blockHitResult = entity.level().clip(new ClipContext(from, to, blockClip, ClipContext.Fluid.NONE, entity));
        if(blockHitResult.getType() != HitResult.Type.MISS){
            Vec3 location = blockHitResult.getLocation();
            to = new Vec3(location.x, to.y, location.z);
        }
        float halfWidth = entity.getBbWidth() * 0.5f;
        List<HitResult> hitResults = new ArrayList<>(getEntityHitResults(entity,from,to,halfWidth,entity.getBbHeight()*0.5f,halfWidth,entity.getBoundingBox().expandTowards(velocity), filter));
        hitResults.add(blockHitResult);
        return hitResults;
    }

    /**
     * 获取弹射物移动向量上的实体碰撞检测结果列表
     */
    public static List<EntityHitResult> getEntityHitResultsOnMoveVector(Entity entity,Predicate<Entity> filter){
        float halfWidth = entity.getBbWidth() * 0.5f;
        Vec3 from = entity.getBoundingBox().getCenter();
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 to = from.add(velocity);
        return getEntityHitResults(entity, from, to, halfWidth,entity.getBbHeight() * 0.5f,halfWidth,entity.getBoundingBox().expandTowards(velocity), filter);
    }
    /**
     * 获取弹射物移动向量上的实体碰撞检测结果列表
     */
    public static List<EntityHitResult> getEntityHitResults(Entity entity, Vec3 from, Vec3 to,Predicate<Entity> filter){
        float halfWidth = entity.getBbWidth() * 0.5f;
        return getEntityHitResults(entity, from, to, halfWidth,entity.getBbHeight() * 0.5f,halfWidth,entity.getBoundingBox().expandTowards(entity.getDeltaMovement()), filter);
    }

    /**
     * 获取弹射物移动向量上的实体碰撞检测结果列表
     */
    public static List<EntityHitResult> getEntityHitResults(Entity entity, Vec3 from, Vec3 to, AABB searchArea,Predicate<Entity> filter){
        float halfWidth = entity.getBbWidth() * 0.5f;
        return getEntityHitResults(entity, from, to, halfWidth,entity.getBbHeight() * 0.5f,halfWidth,searchArea, filter);
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
     */
    public static List<EntityHitResult> getEntityHitResults(Entity entity, Vec3 from, Vec3 to, double inflateX,double inflateY,double inflateZ, AABB searchArea, Predicate<Entity> filter) {
        Level level = entity.level();
        List<EntityHitResult> results = new ArrayList<>();
        List<Entity> entities = level.getEntities(entity, searchArea, filter);
        for (Entity entity1 : entities) {
            AABB aabb = entity1.getBoundingBox().inflate(inflateX,inflateY,inflateZ);
            Optional<Vec3> hitPos = aabb.clip(from, to);
            if (hitPos.isPresent()) {
                // 检查骑乘关系（如果需要）
                if (entity1.getRootVehicle() == entity.getRootVehicle() && !entity1.canRiderInteract()) {
                    continue;  // 跳过不能交互的同乘实体
                }
                results.add(new EntityHitResult(entity1, hitPos.get()));
            }
        }
        // 按距离排序（近到远）
        results.sort(Comparator.comparingDouble(a -> from.distanceToSqr(a.getLocation())));
        return results;
    }


}