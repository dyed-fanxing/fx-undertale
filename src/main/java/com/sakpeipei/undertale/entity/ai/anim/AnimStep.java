package com.sakpeipei.undertale.entity.ai.anim;

import net.minecraft.world.entity.LivingEntity;

import java.util.function.ToIntFunction;

/**
 * @author Sakqiongzi
 * @since 2025-12-14 17:53
 * 判定步骤
 */
public record AnimStep(Byte id, int[] hitTicks, ToIntFunction<LivingEntity> action, int duration){
    public AnimStep(Byte id,int hitTick, ToIntFunction<LivingEntity> action,int duration) {
        this(id, new int[]{hitTick},action,duration);
    }
    public AnimStep(int hitTick, ToIntFunction<LivingEntity> action,int duration) {
        this(null, new int[]{hitTick},action,duration);
    }
    public AnimStep(int[] hitTicks, ToIntFunction<LivingEntity> action,int duration) {
        this(null, hitTicks,action,duration);
    }

    public boolean shouldHitAt(int currentTick) {
        for (int hitTick : hitTicks) {
            if(hitTick == currentTick){
                return true;
            }
        }
        return false;
    }
}
