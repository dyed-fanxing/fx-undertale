package com.fanxing.fx_undertale.utils;


import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * 向量旋转工具
 */
public class RotUtils {

    /**
     * 将向量（坐标）vec对齐MC世界Roll翻滚方向
     */
    public static Vec3 zRot(Vec3 vec,float roll) {
        return vec.yRot(roll * Mth.DEG_TO_RAD);
    }
    /**
     * 将向量（坐标）vec对齐MC世界Pitch仰俯方向
     */
    public static Vec3 xRot(Vec3 vec,float pitch) {
        return vec.yRot(-pitch * Mth.DEG_TO_RAD);
    }
    /**
     * 将向量（坐标）vec对齐MC世界Yaw航偏方向
     */
    public static Vec3 yRot(Vec3 vec,float yaw) {
        return vec.yRot(-yaw * Mth.DEG_TO_RAD);
    }

    public static Vec3 getWorldVec3(Vec3 pos,float pitch,float yaw){
        return getWorldVec3((float) pos.x, (float) pos.y, (float) pos.z,pitch,yaw);
    }
    public static Vec3 getWorldVec3(double x, double y, double z, float pitch, float yaw) {
        return getWorldVec3((float) x, (float) y, (float) z,pitch,yaw);
    }
    /**
     * 根据 相对向量（坐标）和仰俯、航偏 获取世界向量（坐标），先仰俯 后航偏
     * @param x,y,z 相对坐标
     * @param pitch,yaw 仰俯，航偏
     * @return 世界向量
     */
    public static Vec3 getWorldVec3(float x, float y, float z, float pitch, float yaw) {
        float pitchRad = -pitch * Mth.DEG_TO_RAD;
        float yawRad = -yaw * Mth.DEG_TO_RAD;
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);

        float z1 = z * cosPitch - y * sinPitch;
        return new Vec3(
                x * cosYaw + z1 * sinYaw,
                y * cosPitch + z * sinPitch,
                z1 * cosYaw - x * sinYaw
        );
    }
    public static Vec3 getWorldVec3(Vec3 pos,float roll,float pitch,float yaw){
        return getWorldVec3((float) pos.x, (float) pos.y, (float) pos.z,roll,pitch,yaw);
    }
    /**
     * 根据相对坐标和翻滚、仰俯、航偏获取世界坐标
     * 顺序：先绕 Z 轴翻滚，再绕 X 轴仰俯，最后绕 Y 轴航偏
     * @param x,y,z 相对坐标
     * @param roll 翻滚角（度）
     * @param pitch 仰俯角（度）
     * @param yaw 航偏角（度）
     * @return 世界坐标向量
     */
    public static Vec3 getWorldVec3(float x, float y, float z, float roll, float pitch, float yaw) {
        float rollRad = roll * Mth.DEG_TO_RAD;
        float pitchRad = -pitch * Mth.DEG_TO_RAD;
        float yawRad = -yaw * Mth.DEG_TO_RAD;

        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);
        float cosRoll = Mth.cos(rollRad);
        float sinRoll = Mth.sin(rollRad);

        // 1. 绕 Z 轴旋转 roll
        double x1 = x * cosRoll - y * sinRoll;
        double y1 = x * sinRoll + y * cosRoll;

        // 2. 绕 X 轴旋转 pitch
        double y2 = y1 * cosPitch + (double) z * sinPitch;
        double z2 = -y1 * sinPitch + (double) z * cosPitch;

        // 3. 绕 Y 轴旋转 yaw
        double x3 = x1 * cosYaw + z2 * sinYaw;
        double z3 = -x1 * sinYaw + z2 * cosYaw;
        return new Vec3(x3, y2, z3);
    }



    // 航偏角度，同MC默认lookAt方法计算方式，对齐MC世界坐标Z轴
    public static float yRotD(Vec3 vec) {
        return Mth.wrapDegrees((float)(Mth.atan2(vec.z,vec.x) * Mth.RAD_TO_DEG - 90.0F));
    }
    // 仰俯角度，同MC默认lookAt方法计算方式，对齐MC世界坐标Y轴
    public static float xRotD(Vec3 vec) {
        return Mth.wrapDegrees((float)(-(Mth.atan2(vec.y, vec.horizontalDistance()) * Mth.RAD_TO_DEG)));
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
    public static float yRotR(Vec3 vec) {
        return (float) (Mth.atan2(vec.x,vec.z) - 0.5);
    }
    // 仰俯弧度，同MC默认lookAt方法计算方式，对齐MC世界坐标Y轴
    public static float xRotR(Vec3 vec) {
        return (float)(-Mth.atan2(vec.y, vec.horizontalDistance()));
    }
    // 航偏弧度，同上
    public static float yRotR(double x,double z) {
        return (float) (Mth.atan2(x,z) - 0.5);
    }
    // 仰俯弧度，同上
    public static float xRotR(double y,double hd) {
        return (float)(-Mth.atan2(y, hd));
    }


    /**
     * 实体看向向量方向，不适用于弹射物，弹射物请使用下方的专用方法
     * @param entity 实体，非弹射物
     * @param vec 向量
     */
    public static void lookVec(Entity entity,Vec3 vec) {
        entity.setXRot(Mth.wrapDegrees((float)(-(Mth.atan2(vec.y, vec.horizontalDistance()) * Mth.RAD_TO_DEG))));
        entity.setYRot(Mth.wrapDegrees((float)(Mth.atan2(vec.z,vec.x) * Mth.RAD_TO_DEG - 90.0F)));
    }


    /*
        !!!由于原版的弹射物shoot方法所调用的设置旋转的逻辑与Entity实体的逻辑不同
        导致弹射物的视线方向和运动方向不一样，下方的方法是使用shoot里的设置旋转的逻辑
        以同步Entity的lookAt方法名，方便设置弹射物的方向
    */
    public static float shootYRot(Vec3 vec){
        return (float)(Mth.atan2(vec.x, vec.z) * Mth.RAD_TO_DEG);
    }
    public static float shootXRot(Vec3 vec){
        return  (float)(Mth.atan2(vec.y, vec.horizontalDistance())  * Mth.RAD_TO_DEG);
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
        Vec3 vec = new Vec3(target.getX() - entity.getX(),target.getEyeY() - entity.getY(),target.getZ() - entity.getZ());
        entity.absRotateTo(shootXRot(vec.y,vec.horizontalDistance()),shootYRot(vec.x,vec.z));
    }

    public static void lookAtShoot(Entity entity, Entity target){
        lookVecShoot(entity, new Vec3(target.getX() - entity.getX(),target.getY() - entity.getY(),target.getZ() - entity.getZ()));
    }
    public static void lookAtShoot(Entity entity, Vec3 targetPos){
        lookVecShoot(entity,new Vec3(targetPos.x - entity.getX(),targetPos.y - entity.getY(),targetPos.z - entity.getZ()));
    }
    public static void lookAtShoot(Entity entity, double x,double y,double z){
        lookVecShoot(entity,new Vec3(x - entity.getX(),y - entity.getY(),z - entity.getZ()));
    }
    public static void lookAtBodyShoot(Entity entity, Entity target){
        lookVecShoot(entity, new Vec3(target.getX() - entity.getX(),target.getY(0.5f) - entity.getY(0.5f),target.getZ() - entity.getZ()));
    }
    public static void lookAtBodyShoot(Entity entity, Vec3 targetPos){
        lookVecShoot(entity,new Vec3(targetPos.x - entity.getX(),targetPos.y - entity.getY(0.5f),targetPos.z - entity.getZ()));
    }
    public static void lookAtBodyShoot(Entity entity, double x,double y,double z){
        lookVecShoot(entity,new Vec3(x - entity.getX(),y - entity.getY(0.5f),z - entity.getZ()));
    }
    public static void lookAtEyeShoot(Entity entity, Entity target){
        lookVecShoot(entity,new Vec3(target.getX() - entity.getX(),target.getEyeY() - entity.getEyeY(),target.getZ() - entity.getZ()));
    }
    public static void lookAtEyeShoot(Entity entity, Vec3 targetPos){
        lookVecShoot(entity,new Vec3(targetPos.x - entity.getX(),targetPos.y - entity.getEyeY(),targetPos.z - entity.getZ()));
    }
    public static void lookAtEyeShoot(Entity entity, double x,double y,double z){
        lookVecShoot(entity,new Vec3(x - entity.getX(),y - entity.getEyeY(),z - entity.getZ()));
    }
    /**
     * 弹射物看向矢量方向
     * @param entity 弹射物，不可用于实体
     * @param vec 矢量，常用于射击方向，即将要运动的方向
     */
    public static void lookVecShoot(Entity entity,Vec3 vec){
        entity.setXRot((float)(Mth.atan2(vec.y, vec.horizontalDistance())  * Mth.RAD_TO_DEG));
        entity.setYRot((float)(Mth.atan2(vec.x, vec.z) * Mth.RAD_TO_DEG));
    }


    /**
     * 返回从from向量旋转到to向量的四元数，常用于渲染矩阵旋转
     */
    public static Quaternionf rotation(Vec3 from, Vec3 to) {
        return new Quaternionf().fromAxisAngleRad(from.cross(to).toVector3f(), (float) Math.acos(from.dot(to)));
    }

    /**
     * 将一个向量绕指定轴，旋转一定角度
     * @param vec 向量
     * @param axis 轴
     * @param angleRad 角度
     */
    public static Vec3 rotate(Vec3 vec, Vec3 axis, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double dot = vec.dot(axis);
        // 平行分量
        Vec3 parallel = axis.scale(dot);
        // 垂直分量
        Vec3 perpendicular = vec.subtract(parallel);
        // 叉积分量（用于旋转）
        Vec3 cross = axis.cross(perpendicular);
        // 旋转后的垂直分量
        Vec3 rotatedPerp = perpendicular.scale(cos).add(cross.scale(sin));
        return parallel.add(rotatedPerp);
    }
}
