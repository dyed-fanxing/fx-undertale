package com.sakpeipei.undertale.entity;

import net.minecraft.world.entity.Entity;

/**
 * @author yujinbao
 * @since 2026/1/16 15:53
 * @param entity 实体
 * @param delayTick 延迟结束的Tick点
 */

public record DelayEntity(Entity entity, int delayTick) {
    public boolean tick(int tick){
        if(delayTick == tick){
            entity.level().addFreshEntity(entity);
            return true;
        }
        return false;
    }
}
