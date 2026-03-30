package com.fanxing.fx_undertale.common.phys;

import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * @author FanXing
 * @since 2025-10-14 20:50
 */
public interface CollisionDeflection {
    net.minecraft.world.entity.projectile.ProjectileDeflection MIRROR_DEFLECT = (projectile, entity, random) -> {
        if (entity != null) {
            Vec3 deltaMovement = projectile.getDeltaMovement();
            // 使用实体的面向作为镜面法线
            Vec3 normal = entity.getLookAngle().normalize();
            // 计算镜面反射
            double dotProduct = deltaMovement.dot(normal);
            Vec3 reflection = deltaMovement.subtract(normal.scale(2 * dotProduct));
            // 添加一些随机偏移，模拟不完美的反射表面
            Vec3 randomOffset = new Vec3(
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1
            );
            projectile.setDeltaMovement(reflection.add(randomOffset).normalize().scale(deltaMovement.length()));
            projectile.hasImpulse = true;
        }
    };

    static void mirrorDeflect(Entity entity, Vec3 normal, RandomSource random) {
        Vec3 motion = entity.getDeltaMovement();
        // 计算镜面反射
        double dotProduct = motion.dot(normal);
        Vec3 reflection = motion.subtract(normal.scale(2 * dotProduct));
        // 添加一些随机偏移，模拟不完美的反射表面
        Vec3 randomOffset = new Vec3(
                (random.nextDouble() - 0.5) * 0.1,
                (random.nextDouble() - 0.5) * 0.1,
                (random.nextDouble() - 0.5) * 0.1
        );
        entity.setDeltaMovement(reflection.add(randomOffset).normalize().scale(motion.length()));
    }
    // 碰撞
    static void mirrorDeflect(Entity entity, Vec3i normal,RandomSource random) {
        mirrorDeflect(entity,new Vec3(normal.getX(), normal.getY(), normal.getZ()),random);
    }


    /**
     * 使实体在碰撞表面滑动，可独立控制法向和切向的速度保留
     * @param entity             碰撞实体
     * @param normal             表面法线（单位向量）
     * @param random             随机源
     * @param tangentialFriction 切向速度保留系数（0~1，越大滑动越远）
     * @param normalRetention    法向速度保留系数（0 表示法向速度消失，负值表示反弹，正值表示继续向前）
     */
    static void slideDeflect(Entity entity, Vec3 normal, RandomSource random,
                             float tangentialFriction, float normalRetention) {
        Vec3 motion = entity.getDeltaMovement();
        double dot = motion.dot(normal);
        Vec3 normalComponent = normal.scale(dot);       // 法向分量
        Vec3 tangential = motion.subtract(normalComponent); // 切向分量

        // 应用摩擦和法向保留
        Vec3 newTangential = tangential.scale(tangentialFriction);
        Vec3 newNormal = normalComponent.scale(normalRetention);
        Vec3 newMotion = newTangential.add(newNormal);

        // 添加随机扰动（模拟粗糙表面）
        Vec3 randomOffset = new Vec3(
                (random.nextDouble() - 0.5) * 0.1,
                (random.nextDouble() - 0.5) * 0.1,
                (random.nextDouble() - 0.5) * 0.1
        );
        newMotion = newMotion.add(randomOffset);

        // 设置新速度（如果接近零则置零）
        if (newMotion.lengthSqr() < 1e-6) {
            entity.setDeltaMovement(Vec3.ZERO);
        } else {
            entity.setDeltaMovement(newMotion);
        }
    }

    // 重载方法，接受 Vec3i 法线
    static void slideDeflect(Entity entity, Vec3i normal, RandomSource random,
                             float tangentialFriction, float normalRetention) {
        slideDeflect(entity, new Vec3(normal.getX(), normal.getY(), normal.getZ()),
                random, tangentialFriction, normalRetention);
    }

}
