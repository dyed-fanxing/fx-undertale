package com.sakpeipei.mod.undertale.entity.common;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 多次判定攻击
 */
public class MultiTimingAnim<T> extends AnimType<T> {
    protected final int[] hitTicks;

    public MultiTimingAnim(byte id,int duration, boolean triggerAnim,  int[] hitTicks, int cd, T actions) {
        super(id,duration,triggerAnim,cd, actions);
        this.hitTicks = hitTicks;

    }

    public MultiTimingAnim(byte id, int duration,  int[] hitTicks, int cd,T actions) {
        this(id,duration,true,hitTicks,cd,actions);
    }

    @Override
    public boolean shouldHitAt(int currentTick) {
        for (int hitTick : hitTicks) {
            if(hitTick == currentTick){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldPlaySoundAt(int currentTick) {
        return false;
    }

    public int[] getHitTicks() {
        return hitTicks;
    }
}