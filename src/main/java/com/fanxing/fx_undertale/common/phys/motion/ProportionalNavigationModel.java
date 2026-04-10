package com.fanxing.fx_undertale.common.phys.motion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 比例导引模型：每 tick 将速度方向以固定最大转向速率转向目标方向。
 * 速度大小恒定，无阻尼。
 */
public class ProportionalNavigationModel extends PhysicsMotionModel {
    private static final Logger log = LoggerFactory.getLogger(ProportionalNavigationModel.class);

    private float turnRate;   // 每 tick 最大转向角度（弧度）
    private float speed;       // 恒定速度大小

    public ProportionalNavigationModel(float turnRate, float speed) {
        this.turnRate = turnRate;
        this.speed = speed;
    }

    public ProportionalNavigationModel(CompoundTag tag) {
        this.turnRate = tag.getFloat("turnRate");
        this.speed = tag.getFloat("speed");
    }

    public ProportionalNavigationModel(RegistryFriendlyByteBuf buf) {
        this.turnRate = buf.readFloat();
        this.speed = buf.readFloat();
    }

    @Override
    public Vec3 update(Vec3 currentPos, Vec3 currentVel, @Nullable Vec3 targetPos, int ticks) {
        if (targetPos == null) return currentVel;

        Vec3 toTarget = targetPos.subtract(currentPos);
        double dist = toTarget.length();

        // 当距离非常小时，直接对准目标，保持当前速度大小
        if (dist < 1e-4) {
            double currentSpeed = currentVel.length();
            if (currentSpeed < 1e-6) {
                // 如果速度几乎为零，则用默认速度指向目标
                return toTarget.normalize().scale(speed);
            } else {
                return toTarget.normalize().scale(currentSpeed);
            }
        }

        Vec3 targetDir = toTarget.normalize();

        // 获取当前速度大小和方向
        double currentSpeed = currentVel.length();
        Vec3 currentDir;
        if (currentSpeed < 1e-6) {
            currentDir = targetDir;
            currentSpeed = speed; // 速度为零时，使用默认速度
        } else {
            currentDir = currentVel.scale(1.0 / currentSpeed);
        }

        // 计算夹角
        double dot = currentDir.dot(targetDir);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angle = Math.acos(dot);

        // 实际转向角度（不超过最大转向）
        double turn = Math.min(angle, turnRate);
        if (turn < 1e-6) {
            return targetDir.scale(currentSpeed);
        }

        // 计算旋转轴
        Vec3 axis = currentDir.cross(targetDir);
        if (axis.lengthSqr() < 1e-6) {
            axis = new Vec3(1, 0, 0);
        } else {
            axis = axis.normalize();
        }

        // 旋转当前方向
        Vec3 newDir = rotateVector(currentDir, turn, axis);
        return newDir.scale(currentSpeed);
    }

    // 向量绕轴旋转工具
    private Vec3 rotateVector(Vec3 v, double angle, Vec3 axis) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double oneMinusCos = 1 - cos;
        double x = axis.x, y = axis.y, z = axis.z;
        double dot = v.x * x + v.y * y + v.z * z;
        Vec3 cross = v.cross(axis);
        return new Vec3(
                v.x * cos + cross.x * sin + x * dot * oneMinusCos,
                v.y * cos + cross.y * sin + y * dot * oneMinusCos,
                v.z * cos + cross.z * sin + z * dot * oneMinusCos
        );
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("turnRate", turnRate);
        tag.putFloat("speed", speed);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(turnRate);
        buf.writeFloat(speed);
    }

    public float getTurnRate() { return turnRate; }
    public void setTurnRate(float turnRate) { this.turnRate = turnRate; }
    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }



}