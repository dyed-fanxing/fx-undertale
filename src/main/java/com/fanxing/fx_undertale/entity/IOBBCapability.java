package com.fanxing.fx_undertale.entity;

import com.fanxing.fx_undertale.common.phys.OBB;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * 需要OBB碰撞的实体，不用于LivingEntity
 */
public interface IOBBCapability {
    @NotNull
    OBB getOBB();

    /**
     * 重写这个方法，以获取OBB的下的AABB渲染剔除碰撞箱
     */
    @NotNull
    AABB getBoundingBoxForCulling();
}
