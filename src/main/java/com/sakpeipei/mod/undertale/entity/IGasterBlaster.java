package com.sakpeipei.mod.undertale.entity;

import net.minecraft.world.entity.*;

public interface IGasterBlaster{
    LivingEntity getOwner();
    void setOwner(LivingEntity owner) ;
    float getLength() ;
    float getWidth() ;
    void checkHit();
    /**
     * 能否攻击目标
     */
    default boolean canHitTarget(Entity target) {
        return target.isAlive() && target != getOwner();
    }
}

