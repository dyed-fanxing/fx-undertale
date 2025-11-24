package com.sakpeipei.mod.undertale.entity.common;

import net.minecraft.sounds.SoundEvent;

/**
 * @author yujinbao
 * @since 2025/11/21 15:11
 */
public abstract class AnimType<T> implements IAnimType<T> {

    protected final byte id; // 动画ID
    protected final int duration; // 施法/动画/动作 持续Tick
    protected boolean triggerAnim; // 是否触发动画
    protected final int cd; // 攻击后摇CD
    protected T action; // 行动
    protected SoundEvent soundEvent;

    protected AnimType(byte id, int duration, boolean triggerAnim, int cd, T action) {
        this.id = id;
        this.duration = duration;
        this.triggerAnim = triggerAnim;
        this.cd = cd;
        this.action = action;
    }

    public AnimType(byte id, int duration, int cd, T action) {
        this(id, duration, false, cd, action);
    }


    protected AnimType(byte id, int duration, boolean triggerAnim, int cd) {
        this.id = id;
        this.duration = duration;
        this.triggerAnim = triggerAnim;
        this.cd = cd;
        this.action = null;
    }
    protected AnimType(byte id, int duration, int cd) {
        this(id,duration,true,cd);
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
    public boolean isTriggerAnim() {
        return triggerAnim;
    }

    public void setTriggerAnim(boolean triggerAnim) {
        this.triggerAnim = triggerAnim;
    }

    @Override
    public int getCd() {
        return cd;
    }
}
