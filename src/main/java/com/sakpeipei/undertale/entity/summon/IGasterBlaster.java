package com.sakpeipei.undertale.entity.summon;

import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.UUID;

public interface IGasterBlaster extends TraceableEntity{

    Level level();
    @Nullable
    UUID getOwnerUUID();
    @Override
    LivingEntity getOwner();
    void setOwner(LivingEntity owner) ;

    float getSize();
    float getMonthHeight();
    void checkHit();

    /**
     * 光束攻击起点，即嘴（炮口位置）
     */
    default Vec3 getStart(){
        return ((Entity) this).position().add(0,getMonthHeight(),0);
    }
    Vec3 getEnd();
    /**
     * 能否攻击目标
     */
    default boolean canHitTarget(Entity target) {
        return target.isAlive() && target != getOwner();
    }

    /**
     * 是否正在开火
     */
    boolean isFire();
}

