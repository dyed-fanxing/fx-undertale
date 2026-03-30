package com.fanxing.fx_undertale.entity.capability;

import org.joml.Quaternionf;

/**
 * 使用四元数代替欧拉角进行旋转的实体能力接口
 * 用于避免欧拉角万向节死锁问题，实现自由旋转
 */
public interface QuaternionRotatable {

    /**
     * 获取插值后的四元数（用于渲染和插值计算）
     * @param partialTick 插值系数 (0.0 到 1.0)
     * @return 插值后的四元数
     */
    Quaternionf getLerpOrientation(float partialTick);

    /**
     * 获取当前姿态的四元数
     * @return 当前姿态的四元数
     */
    Quaternionf getOrientation();
}