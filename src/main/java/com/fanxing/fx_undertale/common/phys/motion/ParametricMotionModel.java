package com.fanxing.fx_undertale.common.phys.motion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象参数方程运动模型
 * 子类需实现参数方程及其导数
 */
public abstract class ParametricMotionModel extends PhysicsMotionModel {
    private static final Logger log = LoggerFactory.getLogger(ParametricMotionModel.class);

    // ========== 控制参数（初始化后不变，只需生成时同步）==========
    private double stiffnessVel = 0.3;        // 速度比例增益
    private double stiffnessPos = 0.2;         // 位置比例增益（未使用）
    private double posCorrectionDamping = 0.995f; // 阻尼（未使用）

    public ParametricMotionModel() {
    }

    public ParametricMotionModel(CompoundTag tag) {
        this.stiffnessVel = tag.getDouble("stiffnessVel");
        this.stiffnessPos = tag.getDouble("stiffnessPos");
        this.posCorrectionDamping = tag.getDouble("posCorrectionDamping");
    }

    public ParametricMotionModel(RegistryFriendlyByteBuf buf) {
        this.stiffnessVel = buf.readDouble();
        this.stiffnessPos = buf.readDouble();
        this.posCorrectionDamping = buf.readDouble();
    }

    // ========== 子类必须实现的抽象方法 ==========
    protected abstract double getTheta(double time);
    protected abstract Vec3 getPosition(double theta);
    protected abstract Vec3 getDerivative(double theta);

    @Override
    public Vec3 update(Vec3 currentPos, Vec3 currentVel, @Nullable Vec3 targetPos,int ticks) {
        // 忽略传入的 targetVel，只用 targetPos 和参数方程
        double theta = getTheta(ticks);
        Vec3 relPos = getPosition(theta);

        // 如果没有目标，按参数方程运动
        if (targetPos == null) {
            return relPos.subtract(currentPos); // 直接移向相对位置（很少用）
        }

        // 期望位置 = 目标位置 + 相对偏移
        Vec3 desiredPos = targetPos.add(relPos);

        // 指向期望位置的向量
        Vec3 toTarget = desiredPos.subtract(currentPos);
        double distance = toTarget.length();

        // 如果很近，直接减速停靠
        if (distance < 0.1) {
            return currentVel.scale(0.9); // 减速
        }

        // ===== 可调参数 =====
        double strength = 0.5;   // 拉力强度（减小到0.5，降低过冲）
        double damping = 0.98;    // 阻尼系数（增大到0.98，增强稳定性）
        // ===================

        // 加速度 = 方向 * 强度 * 距离（距离越远拉力越大）
        Vec3 acceleration = toTarget.normalize().scale(strength * distance);

        // 新速度 = (当前速度 + 加速度) * 阻尼
        Vec3 newVel = currentVel.add(acceleration).scale(damping);

        // 限制最大速度（可选）
        double maxSpeed = 2.0;
        if (newVel.lengthSqr() > maxSpeed * maxSpeed) {
            newVel = newVel.normalize().scale(maxSpeed);
        }

        return newVel;
    }
    protected boolean enablePositionCorrection() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("stiffnessVel", stiffnessVel);
        tag.putDouble("stiffnessPos", stiffnessPos);
        tag.putDouble("posCorrectionDamping", posCorrectionDamping);
    }

    // ========== 网络同步 ==========
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeDouble(stiffnessVel);
        buf.writeDouble(stiffnessPos);
        buf.writeDouble(posCorrectionDamping);
    }


    // ========== 参数设置 ==========
    public void setStiffnessVel(double stiffnessVel) {
        this.stiffnessVel = stiffnessVel;
    }

    public void setStiffnessPos(double stiffnessPos) {
        this.stiffnessPos = stiffnessPos;
    }

    public void setDamping(double posCorrectionDamping) {
        this.posCorrectionDamping = posCorrectionDamping;
    }

}