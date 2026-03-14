package com.fanxing.fx_undertale.entity;

import com.fanxing.fx_undertale.common.phys.OBB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * 需要OBB碰撞的实体，不用于LivingEntity
 */
public interface IOBB<T extends Entity> extends ISelf<T> {

    default void updateOBB(){
        T self = getSelf();
        setOBB(OBB.fromEntity(self));
    }

    OBB getOBB();
    void setOBB(OBB obb);

    /**
     * 重写这个方法，以获取OBB的下的AABB渲染剔除碰撞箱
     */
    default AABB getBoundingBoxForCulling(){
        OBB obb = getOBB();
        T self = getSelf();
        return obb == null? self.getBoundingBoxForCulling():obb.getBoundingAABB();
    }
}
