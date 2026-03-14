package com.fanxing.fx_undertale.common.phys.motion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 振荡模型：恒定速度，越过目标时反转方向。
 * 速度大小固定，运动始终沿直线，通过距离比较实现来回穿透。
 */
public class OscillationMotionModel extends AbstractPhysicsMotionModel {
    private float speed;               // 恒定速度大小
    private double lastDist = Double.MAX_VALUE; // 上一帧距离（用于检测越过）

    public OscillationMotionModel(float speed) {
        this.speed = speed;
    }

    public OscillationMotionModel(CompoundTag tag) {
        this.speed = tag.getFloat("speed");
    }

    public OscillationMotionModel(RegistryFriendlyByteBuf buf) {
        this.speed = buf.readFloat();
    }

    @Override
    public Vec3 update(Vec3 currentPos, Vec3 currentVel, @Nullable Vec3 targetPos, @Nullable Vec3 targetVel, double deltaTime) {
        if (targetPos == null) return currentVel;

        double dist = currentPos.distanceTo(targetPos);

        Vec3 newVel;
        if (lastDist == Double.MAX_VALUE) {
            // 第一帧：直接指向目标
            newVel = targetPos.subtract(currentPos).normalize().scale(speed);
        } else if (dist > lastDist) {
            // 正在远离目标（已越过），反转当前速度
            if (currentVel.lengthSqr() < 1e-6) {
                // 速度为零时，指向目标
                newVel = targetPos.subtract(currentPos).normalize().scale(speed);
            } else {
                // 反转方向，保持大小
                newVel = currentVel.scale(-1).normalize().scale(speed);
            }
        } else {
            // 靠近目标，保持指向目标
            newVel = targetPos.subtract(currentPos).normalize().scale(speed);
        }

        // 更新上一帧距离
        lastDist = dist;
        return newVel;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("speed", speed);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(speed);
    }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }


    @Override
    protected String getType() {
        return "oscillation";
    }

    static {
        register("oscillation",OscillationMotionModel::new,OscillationMotionModel::new);
    }
}