package com.sakpeipei.undertale.common.anim;

import net.minecraft.sounds.SoundEvent;

import java.util.List;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 单次动画
 */
public class SingleAnim<T>{
    final byte id;   // 动画id
    int animTick;  // 触发动画Tick
    int[] hitTicks;   // 判定Ticks
    protected int duration; // 施法/动画/动作 持续Tick
    protected final int cd; // 攻击后摇CD
    final List<T> actions;      // 执行什么动作



    public SingleAnim(byte id, int hitTick, int duration, int cd, T action) {
        this(id,new int[]{hitTick},duration,cd,List.of(action));
    }

    public SingleAnim(byte id,int[] hitTicks, int duration, int cd, List<T> actions) {
        this.id = id;
        this.hitTicks = hitTicks;
        this.duration = duration;
        this.cd = cd;
        this.actions = actions;
    }

    // 在该tick是否判定
    public boolean shouldHitAt(int currentTick) {
        for (int hitTick : hitTicks) {
            if(hitTick == currentTick){
                return true;
            }
        }
        return false;
    }
    // 在该tick是否播放音效
    public boolean shouldPlaySoundAt(int animTick) {
        return false;
    }

    public void addHitTick(int increment) {
        for (int i = 0; i < hitTicks.length; i++) {
            hitTicks[i] += increment;
        }
    }

    public void addDuration(int increment) {
        this.duration += increment;
    }


    public byte getId() {
        return id;
    }

    public int[] getHitTicks() {
        return hitTicks;
    }

    public SoundEvent getSoundEvent() {
        return null;
    }
    public List<T> getActions() {
        return actions;
    }

    public int getDuration() {
        return duration;
    }
    public int getCd() {
        return cd;
    }
}