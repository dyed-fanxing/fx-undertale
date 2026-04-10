package com.fanxing.fx_undertale.common.phys.motion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * 圆周运动模型：每帧直接返回切向速度，保证正圆轨迹
 * 支持动态改变角速度，不会变成椭圆
 */
public class CircularMotionModel extends PhysicsMotionModel {
    private float radius;
    private float angularSpeed; // 弧度/tick

    public CircularMotionModel(float radius, float angularSpeed) {
        this.radius = radius;
        this.angularSpeed = angularSpeed;
    }

    public CircularMotionModel(CompoundTag tag) {
        this.radius = tag.getFloat("radius");
        this.angularSpeed = tag.getFloat("angularSpeed");
    }

    public CircularMotionModel(RegistryFriendlyByteBuf buf) {
        this.radius = buf.readFloat();
        this.angularSpeed = buf.readFloat();
    }

    // 允许运行时改变角速度
    public void setAngularSpeed(float angularSpeed) {
        this.angularSpeed = angularSpeed;
    }

    @Override
    public Vec3 update(Vec3 currentPos, Vec3 currentVel, Vec3 targetPos, int ticks) {
        if (targetPos == null) return currentVel;
        // 从圆心指向实体的向量
        Vec3 radial = currentPos.subtract(targetPos);
        double r = radial.length();
        if (r < 1e-4) {
            // 在圆心时，给一个默认切向速度（沿X轴）
            return new Vec3(radius * angularSpeed, 0, 0);
        }
        // 径向单位向量
        Vec3 radialDir = radial.scale(1.0 / r);
        // 切向方向（逆时针）：取 (-radialDir.z, 0, radialDir.x)
        Vec3 tangentDir = new Vec3(-radialDir.z, 0, radialDir.x).normalize();
        // 期望速度大小 = ω * R
        double speed = radius * angularSpeed;
        return tangentDir.scale(speed);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("radius", radius);
        tag.putFloat("angularSpeed", angularSpeed);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(radius);
        buf.writeFloat(angularSpeed);
    }

    static {
        PhysicsMotionModel.register(CircularMotionModel.class);
    }
}