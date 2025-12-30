package com.sakpeipei.undertale.entity.projectile;

import net.minecraft.world.phys.Vec3;

/**
 * @author Sakqiongzi
 * @since 2025-10-14 20:50
 */
public interface ProjectileDeflection {
    net.minecraft.world.entity.projectile.ProjectileDeflection MIRROR_DEFLECT = (projectile, entity, random) -> {
        if (entity != null) {
            Vec3 motion = projectile.getDeltaMovement();
            // 使用实体的面向作为镜面法线
            Vec3 normal = entity.getLookAngle().normalize();
            // 计算镜面反射
            double dotProduct = motion.dot(normal);
            Vec3 reflection = motion.subtract(normal.scale(2 * dotProduct));
            // 添加一些随机偏移，模拟不完美的反射表面
            Vec3 randomOffset = new Vec3(
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1
            );
            Vec3 finalMotion = reflection.add(randomOffset).normalize()
                    .scale(motion.length() * 0.5);
            projectile.setDeltaMovement(finalMotion);
            projectile.hasImpulse = true;
        }
    };
}
