package com.fanxing.fx_undertale.entity.capability;

import com.fanxing.fx_undertale.common.phys.OBB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * 需要OBB碰撞的实体，不用于LivingEntity
 */
public interface OBBable{

    void updateOBB();
    OBB getOBB();
    /**
     * 重写这个方法，以获取OBB的下的AABB渲染剔除碰撞箱
     */
    AABB getBoundingBoxForCulling();
}
