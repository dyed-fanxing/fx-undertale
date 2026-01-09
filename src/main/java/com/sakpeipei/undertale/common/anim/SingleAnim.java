package com.sakpeipei.undertale.common.anim;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.*;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 单次动画
 */
public class SingleAnim<T>{
    final byte id;   // 动画id
    int animTick;  // 触发动画Tick
    int[] hitTicks;   // 判定Ticks
    protected int length; // 动画时长 施法/动画/动作
    protected final int cd; // 攻击后摇CD
    final List<T> actions;      // 执行什么动作
    IntFunction<Integer> speedModifier; // 动画速度修改器：可修改动画时长，修改动画判定时机

    public SingleAnim(byte id, int hitTick, int length, int cd, T action) {
        this(id,new int[]{hitTick},length,cd,List.of(action),null);
    }
    public SingleAnim(byte id, int hitTick, int length, int cd, T action,IntFunction<Integer> speedModifier) {
        this(id,new int[]{hitTick},length,cd,List.of(action),speedModifier);
    }

    public SingleAnim(byte id,int[] hitTicks, int length, int cd, List<T> actions,IntFunction<Integer> speedModifier) {
        this.id = id;
        this.hitTicks = hitTicks;
        this.length = length;
        this.cd = cd;
        this.actions = actions;
        this.speedModifier = speedModifier;
    }

    // 在该tick是否判定
    public boolean shouldHitAt(int currentTick) {
        for (int hitTick : hitTicks) {
            if(speedModifier.apply(hitTick) == currentTick){
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

    public void addLength(int increment) {
        this.length += increment;
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

    public int getLength() {
        return length;
    }
    public int getCd() {
        return cd;
    }
}