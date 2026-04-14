package com.fanxing.fx_undertale.entity.component;

import com.fanxing.fx_undertale.utils.collsion.CollisionDetectionUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * 椭球体防护罩组件。
 * 可附加到任意实体上，在该实体的 tick() 方法中调用 tick() 即可自动拦截进入椭球区域的弹射物。
 */
public class EllipsoidProjectileShield {
    private final Entity holder;
    private final double a, b, c;
    private double searchRadius = 64.0;  // 搜索半径，可配置，默认64格

    public EllipsoidProjectileShield(Entity holder, double a, double b, double c) {
        this.holder = holder;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /**
     * 设置搜索半径（用于获取周围弹射物的 AABB 范围）。
     * 半径越大，检测范围越广，能捕获更远距离的弹射物，但性能开销略增。
     * @param radius 半径（格）
     */
    public void searchRadius(double radius) {
        this.searchRadius = radius;
    }

    /**
     * 每 tick 调用一次（放在宿主实体的 tick() 方法中）
     * @return 第一个检测到的弹射物与椭球的碰撞结果，如果没有则返回 null
     */
    @Nullable
    public EntityHitResult tick(Predicate<? super Projectile> validFilter) {
        Level level = holder.level();
        if (level.isClientSide) return null;

        Vec3 center = holder.position();
        AABB searchBox = new AABB(
                center.x - searchRadius, center.y - searchRadius, center.z - searchRadius,
                center.x + searchRadius, center.y + searchRadius, center.z + searchRadius
        );
        var projectiles = level.getEntitiesOfClass(Projectile.class, searchBox,validFilter);

        for (Projectile proj : projectiles) {
            Vec3 curPos = proj.position();
            // 1. 如果弹射物已经在椭球内部，立即拦截
            Vec3 vel = proj.getDeltaMovement();
            if (CollisionDetectionUtils.isPointInsideEllipsoid(curPos, center, a, b, c) && vel.lengthSqr() <= Mth.EPSILON) return new EntityHitResult(proj, curPos);
            Vec3 nextPos = curPos.add(vel);
            // 3. 检测从当前位置到预测位置的线段是否与椭球相交
            Vec3 hit = CollisionDetectionUtils.getSegmentEllipsoidIntersection(curPos, nextPos, center, a, b, c);
            if (hit != null) return new EntityHitResult(proj, hit);
        }
        return null;
    }
}