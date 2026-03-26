package com.fanxing.fx_undertale.common.phys.motion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 螺旋简谐运动模型 - 最终修正版 (向量叉乘法)
 *
 * 原理：
 * 1. 计算相对向量 R = currentPos - targetPos
 * 2. 计算切线速度 V_tangent = cross(R, UpVector) * angularSpeed
 * 3. 如果需要半径变化，再叠加径向速度。
 */
public class SpiralHarmonicMotionModel extends PhysicsMotionModel {

    private final float amplitude;       // 基础半径
    private final float radialFreq;      // 径向频率 (半径伸缩快慢)
    private final float rotationFreq;    // 旋转频率 (转圈快慢)
    private final float nonLinearity;
    private final float speedMultiplier;

    private float phase;
    private float initialRadius = -1f; // 记录初始距离，用于保持相对比例

    public SpiralHarmonicMotionModel(float amp, float wr, float wt, float nonLin, float speedMult) {
        this.amplitude = amp;
        this.radialFreq = wr;
        this.rotationFreq = wt;
        this.nonLinearity = nonLin;
        this.speedMultiplier = speedMult;
        this.phase = 0.0f;
    }

    public SpiralHarmonicMotionModel(CompoundTag tag) {
        this.amplitude = tag.getFloat("amp");
        this.radialFreq = tag.getFloat("wr");
        this.rotationFreq = tag.getFloat("wt");
        this.nonLinearity = tag.getFloat("nonLin");
        this.speedMultiplier = tag.getFloat("speedMult");
        this.phase = tag.getFloat("phase");
        this.initialRadius = tag.getFloat("initR");
    }

    public SpiralHarmonicMotionModel(RegistryFriendlyByteBuf buf) {
        this.amplitude = buf.readFloat();
        this.radialFreq = buf.readFloat();
        this.rotationFreq = buf.readFloat();
        this.nonLinearity = buf.readFloat();
        this.speedMultiplier = buf.readFloat();
        this.phase = 0.0f;
        this.initialRadius = -1f;
    }

    @Override
    public Vec3 update(Vec3 currentPos, Vec3 currentVel,@Nullable Vec3 targetLastPos, @Nullable Vec3 targetPos, double deltaTime) {
        if (targetPos == null) return Vec3.ZERO;

        // 1. 推进相位
        phase += speedMultiplier * 0.05f;

        // 2. 计算相对位置向量 (从目标指向物体)
        Vec3 relPos = currentPos.subtract(targetPos);
        double distXZ = Math.sqrt(relPos.x * relPos.x + relPos.z * relPos.z);

        // 如果距离太近，防止除以0，给个微小默认值并稍微推开一点
        if (distXZ < 0.1) {
            // 如果刚开始或者重合了，强制在 X 轴方向生成一个初始半径
            if (initialRadius < 0) initialRadius = amplitude;
            relPos = new Vec3(initialRadius, 0, 0);
            distXZ = initialRadius;
        } else {
            if (initialRadius < 0) initialRadius = (float) distXZ;
        }

        // === 核心算法：分解速度 ===

        // A. 旋转速度 (切线方向)
        // 二维向量 (x, z) 逆时针旋转90度 变成 (-z, x)
        // 速度大小 = 角速度 * 当前半径
        float angleSpeed = rotationFreq * speedMultiplier;
        float tangentVx = (float) (-relPos.z / distXZ * angleSpeed * distXZ); // 简化后就是 -relPos.z * angleSpeed
        float tangentVz = (float) (relPos.x / distXZ * angleSpeed * distXZ);  // 简化后就是 relPos.x * angleSpeed

        // 修正：上面的 distXZ 约掉了，其实就是：
        tangentVx = (float) (-relPos.z * angleSpeed);
        tangentVz = (float) (relPos.x * angleSpeed);

        // B. 径向速度 (简谐伸缩)
        // 你的原公式：R(t) = A * cos(wr * t)
        // 径向速度 V_r = dR/dt = -A * wr * sin(wr * t)
        // 注意：这里的径向速度是相对于“理想半径”的变化率。
        // 为了稳定，我们计算“目标半径”和“当前半径”的差值，或者直接应用公式速度。
        // 这里采用直接应用公式速度，叠加在当前的径向方向上。

        float targetRadius = amplitude * Mth.cos(radialFreq * phase);
        float radialSpeedMag = -amplitude * radialFreq * Mth.sin(radialFreq * phase);

        // 归一化径向向量
        float normX = (float) (relPos.x / distXZ);
        float normZ = (float) (relPos.z / distXZ);

        float radialVx = normX * radialSpeedMag;
        float radialVz = normZ * radialSpeedMag;

        // C. 非线性因子 (保留你的逻辑)
        float factor = calculateSpeedFactor(Mth.abs(Mth.cos(radialFreq * phase)));

        // 合成螺旋速度 (XZ平面)
        float spiralVx = (tangentVx + radialVx) * factor;
        float spiralVz = (tangentVz + radialVz) * factor;

        // 3. 前进速度 (整体飞向目标)
        // 如果你的螺旋运动本身半径在变小 (radialFreq 导致)，其实不需要额外的 forwardVel
        // 但如果需要整体移动，加上这个：
        Vec3 forwardVel = Vec3.ZERO;
        double distSqr = relPos.lengthSqr();

        // 只有当距离较远时才施加前进力，避免在目标点附近抽搐
        if (distSqr > 1.0) {
            // 这里的 speedMultiplier 控制靠近速度
            Vec3 dirToTarget = targetPos.subtract(currentPos).normalize();
            forwardVel = dirToTarget.scale(speedMultiplier * 1.5);
        }

        // 4. 返回最终速度
        return new Vec3(
                forwardVel.x + spiralVx,
                currentVel.y, // Y轴保持原有或根据需求修改
                forwardVel.z + spiralVz
        );
    }

    private float calculateSpeedFactor(float distNorm) {
        float alpha = nonLinearity * 0.96f;
        float harmonicFactor = Mth.sqrt(Math.max(0f, 1f - distNorm * distNorm));
        return (1f - alpha) + alpha * harmonicFactor;
    }


    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("amp", amplitude);
        tag.putFloat("wr", radialFreq);
        tag.putFloat("wt", rotationFreq);
        tag.putFloat("nonLin", nonLinearity);
        tag.putFloat("speedMult", speedMultiplier);
        tag.putFloat("phase", phase);
        tag.putFloat("initR", initialRadius);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeFloat(amplitude);
        buf.writeFloat(radialFreq);
        buf.writeFloat(rotationFreq);
        buf.writeFloat(nonLinearity);
        buf.writeFloat(speedMultiplier);
    }

    @Override
    protected void writeSyncData(FriendlyByteBuf buf) {
        buf.writeFloat(phase);
        buf.writeFloat(initialRadius);
    }

    @Override
    protected void readSyncData(FriendlyByteBuf buf) {
        this.phase = buf.readFloat();
        this.initialRadius = buf.readFloat();
    }

    @Override
    protected String getType() {
        return "SpiralHarmonic";
    }
    static {
        register("SpiralHarmonic",SpiralHarmonicMotionModel::new,SpiralHarmonicMotionModel::new);
    }
}