package com.sakpeipei.undertale.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface EntityExtension {
    /**
     * 获取相对于实体局部坐标的世界空间位置
     * @param localOffset 实体局部坐标系中的偏移量
     * @return 世界空间中的绝对位置
     */
    Vec3 undertale$getRelativePosition(Vec3 localOffset);

    /**
     * 获取相对于实体眼睛局部坐标的世界空间位置
     * @param localOffset 相对于眼睛的局部偏移
     * @return 世界空间中的绝对位置
     */
    default Vec3 getRelativeEyePosition(Vec3 localOffset){
        return undertale$getRelativePosition(localOffset.add(0,((Entity)this).getEyeHeight(),0));
    }
}
