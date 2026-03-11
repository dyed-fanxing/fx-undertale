package com.sakpeipei.undertale.entity;

public interface Mountable {
    boolean isMountable();
    void setMountable(boolean mountable);

    /**
     * 是否应该双击按键 下车（从乘坐的实体上脱离）
     */
    default boolean shouldDismountOnDoubleKey(){
        return false;
    }
}
