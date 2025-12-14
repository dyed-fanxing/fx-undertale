package com.sakpeipei.mod.undertale.entity.common.anim;

import net.minecraft.sounds.SoundEvent;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 多次判定攻击
 */
public class MultiTimingAnim<T> implements AnimType<T> {
    protected final byte id; // 动画ID
    protected int duration; // 施法/动画/动作 持续Tick
    protected final int cd; // 攻击后摇CD
    protected T action; // 行动
    protected SoundEvent soundEvent;
    protected final int[] hitTicks;

    protected MultiTimingAnim(byte id, int duration, int[] hitTicks, int cd, T action) {
        this.id = id;
        this.duration = duration;
        this.hitTicks = hitTicks;
        this.cd = cd;
        this.action = action;

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

    @Override
    public byte getId() {
        return id;
    }

    @Override
    public T getAction() {
        return action;
    }

    public void setAction(T action) {
        this.action = action;
    }

    @Override
    public SoundEvent getSoundEvent() {
        return soundEvent;
    }

    public void setSoundEvent(SoundEvent soundEvent) {
        this.soundEvent = soundEvent;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public int getCd() {
        return cd;
    }

    @Override
    public void addDuration(int increment) {
        this.duration += increment;
    }

    public int[] getHitTicks() {
        return hitTicks;
    }
}