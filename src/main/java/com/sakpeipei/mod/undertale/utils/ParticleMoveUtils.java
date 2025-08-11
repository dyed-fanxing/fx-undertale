package com.sakpeipei.mod.undertale.utils;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class ParticleMoveUtils {
    private static final Logger log = LogManager.getLogger(ParticleMoveUtils.class);

    public static void ballIn(Level level, int tick,float radius, ParticleOptions particleOptions,double x,double y,double z) {
        ballIn(level,tick,radius,() -> particleOptions,x,y,z);
    }
    /**
     * 生成向中心点聚合的的粒子
     * @param x,y,z     中心点坐标
     * @param tick      时间
     * @param radius    半径
     * @param particleSupplier 粒子
     */
    public static void ballIn(Level level, int tick, float radius, Supplier<ParticleOptions> particleSupplier,double x,double y,double z) {
        RandomSource random = level.random;
        // 使用球面均匀分布方法（Y轴为主轴）
        double theta = Math.acos(2 * random.nextDouble() - 1); // θ ∈ [0, π]（相对于Y轴的角度）
        double phi = 2 * Math.PI * random.nextDouble();        // φ ∈ [0, 2π]（绕Y轴的旋转角度）
        // 计算球面坐标（Y轴上下，XZ平面）
        double xOffset = radius * Math.sin(theta) * Math.cos(phi); // X方向
        double yOffset = radius * Math.cos(theta);                 // Y方向（主轴）
        double zOffset = radius * Math.sin(theta) * Math.sin(phi); // Z方向
        // 方向指向中心（可选）
        Vec3 dir = new Vec3(-xOffset, -yOffset, -zOffset).normalize().scale(radius / tick);
        // 生成粒子（速度初始为0或指向中心）
        level.addParticle(
                particleSupplier.get(),
                x + xOffset,
                y + 1.5 + yOffset,
                z + zOffset,
                dir.x, dir.y, dir.z
        );
    }


    public static void circularOut(Level level,int count, float radius, ParticleOptions particleOptions,
                                   double x,double y,double z,float yRot,float xRot,float dirZ) {
        circularOut(level,count,radius,() -> particleOptions,x,y,z,yRot,xRot,dirZ);
    }
    /**
     * 环形爆炸：中心点向外
     */
    public static void circularOut(Level level,int count, float radius, Supplier<ParticleOptions> particleSupplier,
                                   double x,double y,double z,float yRot,float xRot,float dirZ) {
        double angle = 0;
        double unit = 360.0 / count;
        for (int i = 0; i < count; i++){
            angle += unit;
            double rad = angle * Mth.DEG_TO_RAD;
            double cos = radius * Math.cos(rad);
            double sin = radius * Math.sin(rad);
            Vec3 dir = new Vec3(cos, sin, dirZ).yRot(RotUtils.dirYRot(yRot)).xRot(RotUtils.dirXRot(xRot)).normalize().scale(0.5);
            level.addParticle(particleSupplier.get(), x, y,  z, dir.x, dir.y, dir.z);
        }
    }
}
