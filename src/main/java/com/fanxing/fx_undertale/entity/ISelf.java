package com.fanxing.fx_undertale.entity;

import net.minecraft.world.entity.Entity;

public interface ISelf<T extends Entity> {
    /**
     * 获取自身实体引用
     * 实现类需要返回 this
     */
    @SuppressWarnings("unchecked")
    default T getSelf() {
        return (T) this;
    }
}
