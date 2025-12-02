package com.sakpeipei.mod.undertale.entity.common;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 单次判定攻击
 */
public class OnceTimingAnim<T> extends AbstractAnimType<T> {
    protected final int hitTick;

    public OnceTimingAnim(byte id,int duration, boolean triggerAnim, int hitTick, int cd, T action) {
        super(id,duration,triggerAnim,cd, action);
        this.hitTick = hitTick;
    }

    public OnceTimingAnim(byte id, int duration, int hitTick, int cd,T action) {
        this(id,duration,true,hitTick,cd,action);
    }

    @Override
    public boolean shouldHitAt(int currentTick) {
        return this.hitTick == currentTick;
    }

    @Override
    public boolean shouldPlaySoundAt(int currentTick) {
        return this.hitTick - 4 == currentTick;

    }


}