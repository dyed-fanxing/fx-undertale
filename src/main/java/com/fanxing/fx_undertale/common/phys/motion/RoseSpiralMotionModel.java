package com.fanxing.fx_undertale.common.phys.motion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 玫瑰螺旋曲线运动模型（极坐标法，稳定收敛）
 * 物体以螺旋轨迹向目标点运动，最终停止。
 */
public class RoseSpiralMotionModel extends PhysicsMotionModel {

    private float radialSpeed;      // 径向速度系数 (1/tick)
    private float angularSpeed;     // 角速度 (弧度/tick)
    private float inertia = 0.5f;   // 惯性系数 (0~1)

    public RoseSpiralMotionModel() {
        this(1.0f, 1.0f);
    }

    public RoseSpiralMotionModel(float radialSpeed, float angularSpeed) {
        this.radialSpeed = radialSpeed;
        this.angularSpeed = angularSpeed;
    }

    public RoseSpiralMotionModel(CompoundTag tag) {
        this.radialSpeed = tag.getFloat("radialSpeed");
        this.angularSpeed = tag.getFloat("angularSpeed");
        this.inertia = tag.getFloat("inertia");
    }

    public RoseSpiralMotionModel(RegistryFriendlyByteBuf buf) {
        this.radialSpeed = buf.readFloat();
        this.angularSpeed = buf.readFloat();
        this.inertia = buf.readFloat();
    }

    @Override
    public Vec3 update(Vec3 currentPos, Vec3 currentVel,@Nullable Vec3 targetPos,int ticks) {
        if (targetPos == null) return currentVel;

        Vec3 r = targetPos.subtract(currentPos);
        double distance = r.length();
        if (distance < 1e-6) return Vec3.ZERO;

        // 水平距离与方向角
        double dx = r.x, dz = r.z;
        double horDist = Math.sqrt(dx * dx + dz * dz);
        double angle = Math.atan2(dz, dx);

        // 速度合成
        double radialHor = radialSpeed * horDist;          // 径向水平分量
        double tangential = angularSpeed * horDist;        // 切向水平分量
        double angleTan = angle + Math.PI / 2;             // 切向方向（逆时针）

        double vx = radialHor * Math.cos(angle) + tangential * Math.cos(angleTan);
        double vz = radialHor * Math.sin(angle) + tangential * Math.sin(angleTan);
        double vy = radialSpeed * r.y;                     // 垂直速度

        Vec3 desiredVel = new Vec3(vx, vy, vz);
        return desiredVel.scale(1 - inertia).add(currentVel.scale(inertia));
    }

    // ---------- 序列化 ----------
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("radialSpeed", radialSpeed);
        tag.putFloat("angularSpeed", angularSpeed);
        tag.putFloat("inertia", inertia);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(radialSpeed);
        buf.writeFloat(angularSpeed);
        buf.writeFloat(inertia);
    }

}