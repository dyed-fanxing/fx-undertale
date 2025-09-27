package com.sakpeipei.mod.undertale.utils;

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
public class ProjectileUtils {
    /**
     * 获取实体在移动方向上的碰撞结果列表
     * @param entity 实体
     * @param filter 过滤器
     * @param blockClip 方块碰撞检测条件
     * @return
     */
    public static List<HitResult> getEntityHitResultsOnMoveVector(Entity entity, Predicate<Entity> filter,ClipContext.Block blockClip) {
        return getHitResults(entity, entity.position(), entity.getDeltaMovement(),filter,0.3F, blockClip);
    }
    private static List<HitResult> getHitResults(Entity entity,Vec3 from,Vec3 dir,  Predicate<Entity> filter, float pickable, ClipContext.Block blockClip) {
        Vec3 to = from.add(dir);
        Level level = entity.level();
        HitResult hitResult = level.clip(new ClipContext(from, to, blockClip, ClipContext.Fluid.NONE, entity));
        if (hitResult.getType() != HitResult.Type.MISS) {
            to = hitResult.getLocation();
        }
        List<HitResult> hitResults = getEntityHitResultsOnMoveVector(entity, from, to, entity.getBoundingBox().expandTowards(dir).inflate(1.0F), filter, pickable);
        hitResults.add(hitResult);
        return hitResults;
    }

    public static List<HitResult> getEntityHitResultsOnMoveVector(Entity shooter, Vec3 from, Vec3 to, AABB searchArea, Predicate<Entity> filter, float pickable) {
        Level level = shooter.level();
        List<HitResult> results = new ArrayList<>();

        for (Entity entity1 : level.getEntities(shooter, searchArea, filter)) {
            AABB aabb = entity1.getBoundingBox().inflate(pickable);
            Optional<Vec3> hitPos = aabb.clip(from, to);
            if (hitPos.isPresent()) {
                // 检查骑乘关系（如果需要）
                if (entity1.getRootVehicle() == shooter.getRootVehicle() && !entity1.canRiderInteract()) {
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
