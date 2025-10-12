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
    // 航偏角度，同MC默认lookAt方法计算方式，对齐MC世界坐标Z轴
    public static float yRotD(Vec3 vec3) {
        return Mth.wrapDegrees((float)(Mth.atan2(vec3.x,vec3.z) * Mth.RAD_TO_DEG - 90.0F));
    }
    // 仰俯角度，同MC默认lookAt方法计算方式，对齐MC世界坐标Y轴
    public static float xRotD(Vec3 vec3) {
        return Mth.wrapDegrees((float)(-(Mth.atan2(vec3.y, vec3.horizontalDistance()) * Mth.RAD_TO_DEG)));
    }
    // 航偏角度，同上
    public static float yRotD(double x,double z) {
        return Mth.wrapDegrees((float)(Mth.atan2(x, z) * Mth.RAD_TO_DEG - 90.0F));
    }
    // 仰俯角度，同上
    public static float xRotD(double y,double hd) {
        return Mth.wrapDegrees((float)(-(Mth.atan2(y, hd) * Mth.RAD_TO_DEG)));
    }

    // 航偏弧度，同MC默认lookAt方法计算方式，对齐MC世界坐标Z轴
    public static float yRotR(Vec3 vec3) {
        return (float) (Mth.atan2(vec3.x,vec3.z) - 0.5);
    }
    // 仰俯弧度，同MC默认lookAt方法计算方式，对齐MC世界坐标Y轴
    public static float xRotR(Vec3 vec3) {
        return (float)(-Mth.atan2(vec3.y, vec3.horizontalDistance()));
    }
    // 航偏弧度，同上
    public static float yRotR(double x,double z) {
        return (float) (Mth.atan2(x,z) - 0.5);
    }
    // 仰俯弧度，同上
    public static float xRotR(double y,double hd) {
        return (float)(-Mth.atan2(y, hd));
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

    public static void absRotateByShoot(Entity entity, Entity target){
        Vec3 dir = new Vec3(target.getX() - entity.getX(),target.getEyeY() - entity.getY(),target.getZ() - entity.getZ());
        entity.absRotateTo(shootXRot(dir.y,dir.horizontalDistance()),shootYRot(dir.x,dir.z));
    }
    public static void lookAtByShoot(Entity entity, Entity target){
        Vec3 dir = new Vec3(target.getX() - entity.getX(),target.getEyeY() - entity.getY(),target.getZ() - entity.getZ());
        entity.setXRot(shootXRot(dir.y,dir.horizontalDistance()));
        entity.setYRot(shootYRot(dir.x,dir.z));
    }
    public static void lookAtByTowardsMovement(Entity entity, Entity target){
        Vec3 dir = new Vec3(target.getX() - entity.getX(),target.getEyeY() - entity.getY(),target.getZ() - entity.getZ());
        entity.setXRot((float)(Mth.atan2(dir.y, dir.horizontalDistance()) * Mth.RAD_TO_DEG));
        entity.setYRot((float)(Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG));
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
