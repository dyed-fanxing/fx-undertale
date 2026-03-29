package com.fanxing.fx_undertale.entity.capability;

import net.minecraft.world.phys.Vec3;

/**
 * 使OBB具有旋转碰撞检测的能力
 */
public interface OBBRotationCollider {
    float getAngularVelocity();
    Vec3 getRotateAxis();
}
