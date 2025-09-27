package com.sakpeipei.mod.undertale.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Predicate;

public class RayUtils {
    /**
     * 获取实体视线上最近目标实体的射线检测结果
     * @param length 射线的长度
     */
    public static EntityHitResult getLastestEntityHitResultOnViewVector(Level level, Entity entity, Predicate<Entity> condition,float length) {
        return getLastestEntityHitResultOnViewVector(level, entity, condition, 0, length);
    }
    /**
     * 获取实体缩放视线上最近目标实体的射线检测结果
     * @param inflate 射线的缩放范围
     * @param length 射线的长度
     */
    public static EntityHitResult getLastestEntityHitResultOnViewVector(Level level, Entity entity, Predicate<Entity> condition, float inflate,float length) {
        Vec3 start = entity.getEyePosition();
        Vec3 end = start.add(entity.getLookAngle().scale(length));
        double max = Double.MAX_VALUE;
        Entity target = null;
        for (Entity target1 : level.getEntities(entity, new AABB(start, end), condition)) {
            AABB aabb = target1.getBoundingBox().inflate(target1.getPickRadius()).inflate(inflate);
            Optional<Vec3> optional = aabb.clip(start, end);
            if (optional.isPresent()) {
                double cur = entity.distanceToSqr(optional.get());
                if (cur < max) {
                    target = target1;
                    max = cur;
                }
            }
        }
        return target == null ? null : new EntityHitResult(target);
    }



}
