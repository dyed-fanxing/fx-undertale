package com.sakpeipei.mod.undertale.entity.summon;

import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

public interface IGasterBlaster {
    Level level();
    @Nullable
    UUID getOwnerUUID();
    @Nullable
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

