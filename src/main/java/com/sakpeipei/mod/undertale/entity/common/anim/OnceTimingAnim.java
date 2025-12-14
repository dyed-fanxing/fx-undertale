package com.sakpeipei.mod.undertale.entity.common.anim;

import net.minecraft.sounds.SoundEvent;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 单次判定攻击
 */
public class OnceTimingAnim<T> implements AnimType<T> {
    protected final int hitTick;
    protected final byte id; // 动画ID
    protected int duration; // 施法/动画/动作 持续Tick
    protected final int cd; // 攻击后摇CD
    protected T action; // 行动
    protected SoundEvent soundEvent;

    public OnceTimingAnim(byte id, int duration, int hitTick, int cd, T action) {
        this.id = id;
        this.duration = duration;
        this.hitTick = hitTick;
        this.cd = cd;
        this.action = action;
    }


    @Override
    public boolean shouldHitAt(int currentTick) {
        return this.hitTick == currentTick;
    }

    @Override
    public boolean shouldPlaySoundAt(int currentTick) {
        return this.hitTick - 4 == currentTick;

    }

    @Override
    public void addDuration(int increment) {
        this.duration += increment;
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
}