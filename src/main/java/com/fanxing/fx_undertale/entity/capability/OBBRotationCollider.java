package com.fanxing.fx_undertale.entity.capability;

import org.joml.Vector3f;

/**
 * 使OBB具有旋转碰撞检测的能力
 */
public interface OBBRotationCollider {
    /**
     * 角速度（弧度单位）
     */
    Vector3f getAngularVelocity();
    void setAngularVelocity(Vector3f velocity);
}
