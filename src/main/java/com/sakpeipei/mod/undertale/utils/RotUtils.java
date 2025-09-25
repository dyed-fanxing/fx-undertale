package com.sakpeipei.mod.undertale.utils;


import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.Vec3;

/**
 * 单位向量旋转工具
 */
public class RotUtils {

    // 航偏角度
    public static float yRot(double x,double z) {
        return (float) Math.toDegrees(Math.atan2(x, z));
    }
    // 物理仰俯角度（未对齐mc y轴）
    public static float xRot(double y) {
        return (float) Math.toDegrees(Math.asin(y));
    }


    public static float shootYRot(Vec3 dir){
        return (float)(Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG);
    }
    public static float shootXRot(Vec3 dir){
        return  (float)(Mth.atan2(dir.y, dir.horizontalDistance())  * Mth.RAD_TO_DEG);
    }
    // 航偏角度
    public static float shootYRot(double x,double z) {
        return (float)(Mth.atan2(x, z) * Mth.RAD_TO_DEG);
    }
    // 物理仰俯角度（未对齐mc y轴）
    public static float shootXRot(double y,double d) {
        return (float)(Mth.atan2(y, d)  * Mth.RAD_TO_DEG);
    }

    public static void setLookAtByShootRot(Entity entity, Entity target){
        Vec3 dir = new Vec3(target.getX() - entity.getX(),target.getY(0.5f) - entity.getY(),target.getZ() - entity.getZ());
        entity.setXRot(shootXRot(dir.y,dir.horizontalDistance()));
        entity.setYRot(shootYRot(dir.x,dir.z));
    }

    /**
     * 获取绕 dir向量 旋转zRot，yRot，xRot角度的向量
     * @param dir 向量
     * @param zRot 绕z轴旋转的角度
     * @param yRot 绕y轴旋转的角度
     * @param xRot 绕x轴旋转的角度
     * @return 绕 dir向量 旋转zRot，yRot，xRot角度的向量
     */
    public static Vec3 dirRot(Vec3 dir, float zRot, float yRot, float xRot) {
           return dir.zRot((zRot - 90) * Mth.DEG_TO_RAD)
                .yRot(-yRot * Mth.DEG_TO_RAD)
                .xRot(-xRot * Mth.DEG_TO_RAD);
    }



    public static float dirZRot(float rot) {
      return ( rot - 90) * Mth.DEG_TO_RAD;
    }
    public static float dirYRot(float rot) {
        return -rot * Mth.DEG_TO_RAD;
    }
    public static float dirXRot(float rot) {
        return -rot * Mth.DEG_TO_RAD;
    }

}
